package com.auction.client;

import com.auction.model.auction.Auction;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.protocol.AuctionDto;
import com.auction.protocol.AuctionRequest;
import com.auction.protocol.AuctionResponse;
import com.auction.protocol.DtoMapper;
import com.auction.protocol.RequestType;
import com.auction.protocol.UserDto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Gateway phía client giao tiếp với server qua Java Socket.
 * Thay thế LocalAuctionClientGateway khi chạy ở chế độ mạng.
 *
 * Duy trì một persistent connection tới server.
 * Nếu kết nối bị mất, tự động reconnect.
 */
public class SocketAuctionClientGateway implements AuctionClientGateway {
    private static final int DEFAULT_PORT = 9999;
    private static final String DEFAULT_HOST = "localhost";

    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread listenerThread;

    public SocketAuctionClientGateway() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public SocketAuctionClientGateway(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Kết nối tới server. Nếu đã kết nối rồi thì bỏ qua.
     */
    public void connect() {
        if (socket != null && !socket.isClosed()) {
            return;
        }
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("[Client] Đã kết nối tới server " + host + ":" + port);
            
            // Khởi động luồng lắng nghe Real-time nếu chưa chạy
            if (listenerThread == null || !listenerThread.isAlive()) {
                startListeningForUpdates();
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể kết nối tới server: " + e.getMessage(), e);
        }
    }

    private void startListeningForUpdates() {
        listenerThread = new Thread(() -> {
            try {
                Socket eventSocket = new Socket(host, port);
                ObjectOutputStream eventOut = new ObjectOutputStream(eventSocket.getOutputStream());
                eventOut.flush();
                ObjectInputStream eventIn = new ObjectInputStream(eventSocket.getInputStream());
                
                eventOut.writeObject(new AuctionRequest(RequestType.SUBSCRIBE_UPDATES));
                eventOut.flush();
                
                System.out.println("[Client] Đã kết nối luồng Real-time update (Push Notifications).");
                while (true) {
                    Object obj = eventIn.readObject();
                    if (obj instanceof com.auction.protocol.AuctionEvent) {
                        System.out.println("[Client] Bắt được sự kiện: " + ((com.auction.protocol.AuctionEvent)obj).getEventType());
                        javafx.application.Platform.runLater(ClientEventManager::fireUpdate);
                    }
                }
            } catch (Exception e) {
                System.err.println("[Client] Mất kết nối luồng Real-time: " + e.getMessage());
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Ngắt kết nối.
     */
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // ignore
        }
        socket = null;
        out = null;
        in = null;
    }

    /**
     * Gửi request và nhận response từ server.
     */
    private AuctionResponse send(AuctionRequest request) {
        connect();
        try {
            out.writeObject(request);
            out.flush();
            out.reset();
            AuctionResponse response = (AuctionResponse) in.readObject();
            if (!response.isSuccess()) {
                throw new RuntimeException(response.getMessage());
            }
            return response;
        } catch (IOException | ClassNotFoundException e) {
            // Kết nối bị mất — reset và retry 1 lần
            disconnect();
            connect();
            try {
                out.writeObject(request);
                out.flush();
                out.reset();
                AuctionResponse response = (AuctionResponse) in.readObject();
                if (!response.isSuccess()) {
                    throw new RuntimeException(response.getMessage());
                }
                return response;
            } catch (IOException | ClassNotFoundException e2) {
                throw new RuntimeException("Lỗi giao tiếp với server: " + e2.getMessage(), e2);
            }
        }
    }

    @Override
    public User login(String email, String password) {
        AuctionRequest req = new AuctionRequest(RequestType.LOGIN)
                .put("email", email)
                .put("password", password);
        AuctionResponse resp = send(req);
        return DtoMapper.toUser((UserDto) resp.getData());
    }

    @Override
    public User register(String username, String email, String password, String role) {
        AuctionRequest req = new AuctionRequest(RequestType.REGISTER)
                .put("username", username)
                .put("email", email)
                .put("password", password)
                .put("role", role);
        AuctionResponse resp = send(req);
        return DtoMapper.toUser((UserDto) resp.getData());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getAvailableRegistrationRoles() {
        AuctionRequest req = new AuctionRequest(RequestType.GET_REGISTRATION_ROLES);
        AuctionResponse resp = send(req);
        return (List<String>) resp.getData();
    }

    @Override
    public void resetPassword(String email, String username, String newPassword) {
        AuctionRequest req = new AuctionRequest(RequestType.RESET_PASSWORD)
                .put("email", email)
                .put("username", username)
                .put("newPassword", newPassword);
        send(req);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Auction> listAuctions() {
        AuctionRequest req = new AuctionRequest(RequestType.LIST_AUCTIONS);
        AuctionResponse resp = send(req);
        List<AuctionDto> dtos = (List<AuctionDto>) resp.getData();
        List<Auction> auctions = new ArrayList<>();
        for (AuctionDto dto : dtos) {
            auctions.add(DtoMapper.toAuction(dto));
        }
        return auctions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Auction> listAuctionsForSeller(String sellerId) {
        AuctionRequest req = new AuctionRequest(RequestType.LIST_AUCTIONS_FOR_SELLER)
                .put("sellerId", sellerId);
        AuctionResponse resp = send(req);
        List<AuctionDto> dtos = (List<AuctionDto>) resp.getData();
        List<Auction> auctions = new ArrayList<>();
        for (AuctionDto dto : dtos) {
            auctions.add(DtoMapper.toAuction(dto));
        }
        return auctions;
    }

    @Override
    public Auction createAuctionForSeller(Seller seller, String itemType, String name,
                                          String description, double startingPrice) {
        return createAuctionForSeller(seller, itemType, name, description, startingPrice, null);
    }

    @Override
    public Auction createAuctionForSeller(Seller seller, String itemType, String name,
                                          String description, double startingPrice, String imagePath) {
        AuctionRequest req = new AuctionRequest(RequestType.CREATE_AUCTION)
                .put("sellerId", seller.getId())
                .put("sellerName", seller.getUsername())
                .put("sellerEmail", seller.getEmail())
                .put("itemType", itemType)
                .put("itemName", name)
                .put("itemDescription", description)
                .put("startingPrice", String.valueOf(startingPrice))
                .put("imagePath", imagePath);
        AuctionResponse resp = send(req);
        return DtoMapper.toAuction((AuctionDto) resp.getData());
    }

    @Override
    public void startAuction(String auctionId) {
        AuctionRequest req = new AuctionRequest(RequestType.START_AUCTION)
                .put("auctionId", auctionId);
        send(req);
    }

    @Override
    public void finishAuction(String auctionId) {
        AuctionRequest req = new AuctionRequest(RequestType.FINISH_AUCTION)
                .put("auctionId", auctionId);
        send(req);
    }

    @Override
    public void cancelAuction(String auctionId) {
        AuctionRequest req = new AuctionRequest(RequestType.CANCEL_AUCTION)
                .put("auctionId", auctionId);
        send(req);
    }

    @Override
    public void markAuctionPaid(String auctionId) {
        AuctionRequest req = new AuctionRequest(RequestType.MARK_AUCTION_PAID)
                .put("auctionId", auctionId);
        send(req);
    }

    @Override
    public void placeBid(String auctionId, Bidder bidder, double amount) {
        AuctionRequest req = new AuctionRequest(RequestType.PLACE_BID)
                .put("auctionId", auctionId)
                .put("bidderId", bidder.getId())
                .put("bidderName", bidder.getUsername())
                .put("bidderEmail", bidder.getEmail())
                .put("amount", String.valueOf(amount));
        send(req);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<User> listUsers() {
        AuctionRequest req = new AuctionRequest(RequestType.LIST_USERS);
        AuctionResponse resp = send(req);
        List<UserDto> dtos = (List<UserDto>) resp.getData();
        List<User> users = new ArrayList<>();
        for (UserDto dto : dtos) {
            users.add(DtoMapper.toUser(dto));
        }
        return users;
    }

    @Override
    public void registerAutoBid(String auctionId, String bidderId, double maxPrice, double increment) {
        AuctionRequest req = new AuctionRequest(RequestType.REGISTER_AUTO_BID)
                .put("auctionId", auctionId)
                .put("bidderId", bidderId)
                .put("maxPrice", String.valueOf(maxPrice))
                .put("increment", String.valueOf(increment));
        send(req);
    }

    @Override
    public void cancelAutoBid(String auctionId, String bidderId) {
        AuctionRequest req = new AuctionRequest(RequestType.CANCEL_AUTO_BID)
                .put("auctionId", auctionId)
                .put("bidderId", bidderId);
        send(req);
    }

    @Override
    public com.auction.model.auction.AutoBidConfig getAutoBid(String auctionId, String bidderId) {
        AuctionRequest req = new AuctionRequest(RequestType.GET_AUTO_BID_STATUS)
                .put("auctionId", auctionId)
                .put("bidderId", bidderId);
        AuctionResponse resp = send(req);
        return (com.auction.model.auction.AutoBidConfig) resp.getData();
    }

    @Override
    public User updateUser(String userId, String username, String email) {
        return updateUser(userId, username, email, null, null, null, null, null, null);
    }

    @Override
    public User updateUser(String userId, String username, String email,
                           String shippingAddress, String phoneNumber,
                           String storeName, String storeDescription,
                           String department) {
        return updateUser(userId, username, email, shippingAddress, phoneNumber, storeName, storeDescription, department, null);
    }

    @Override
    public User updateUser(String userId, String username, String email,
                           String shippingAddress, String phoneNumber,
                           String storeName, String storeDescription,
                           String department, String avatarPath) {
        AuctionRequest req = new AuctionRequest(RequestType.UPDATE_USER)
                .put("userId", userId)
                .put("username", username)
                .put("email", email)
                .put("shippingAddress", shippingAddress)
                .put("phoneNumber", phoneNumber)
                .put("storeName", storeName)
                .put("storeDescription", storeDescription)
                .put("department", department)
                .put("avatarPath", avatarPath);
        AuctionResponse resp = send(req);
        return DtoMapper.toUser((UserDto) resp.getData());
    }
}
