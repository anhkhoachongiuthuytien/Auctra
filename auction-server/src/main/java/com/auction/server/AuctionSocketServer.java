package com.auction.server;

import com.auction.model.auction.Auction;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.protocol.AuctionDto;
import com.auction.protocol.AuctionEvent;
import com.auction.protocol.AuctionRequest;
import com.auction.protocol.AuctionResponse;
import com.auction.protocol.DtoMapper;
import com.auction.protocol.RequestType;
import com.auction.protocol.UserDto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server TCP lắng nghe các AuctionRequest từ client,
 * xử lý qua AuctionServerFacade, và trả AuctionResponse.
 *
 * Mỗi client connection được xử lý trong thread pool riêng.
 */
public class AuctionSocketServer {
    private static final int DEFAULT_PORT = 9999;
    private static final int THREAD_POOL_SIZE = 10;

    private final int port;
    private final AuctionServerFacade facade;
    private final ExecutorService threadPool;
    private volatile boolean running;
    private ServerSocket serverSocket;

    public AuctionSocketServer(AuctionServerFacade facade) {
        this(facade, DEFAULT_PORT);
    }

    public AuctionSocketServer(AuctionServerFacade facade, int port) {
        this.facade = facade;
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    /**
     * Bắt đầu lắng nghe kết nối. Phương thức này block thread hiện tại.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("[Server] Đang chạy tại cổng " + port);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] Client kết nối: " + clientSocket.getRemoteSocketAddress());
                threadPool.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running) {
                    System.err.println("[Server] Lỗi accept: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        threadPool.shutdown();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[Server] Lỗi khi đóng: " + e.getMessage());
        }
        System.out.println("[Server] Đã dừng.");
    }

    /**
     * Xử lý một client connection. Hỗ trợ nhiều request liên tiếp trên cùng connection.
     */
    private void handleClient(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            boolean isSubscriber = false;
            while (true) {
                AuctionRequest request;
                try {
                    request = (AuctionRequest) in.readObject();
                } catch (Exception e) {
                    // Client đã ngắt kết nối
                    if (isSubscriber) {
                        BroadcastManager.removeClient(out);
                    }
                    break;
                }

                if (request.getType() == RequestType.SUBSCRIBE_UPDATES) {
                    BroadcastManager.addClient(out);
                    isSubscriber = true;
                    // Keep listening on this socket for nothing (it just blocks)
                    continue;
                }

                System.out.println("[Server] Nhận request: " + request.getType());
                AuctionResponse response = processRequest(request);
                out.writeObject(response);
                out.flush();
                out.reset();
            }
        } catch (IOException e) {
            System.err.println("[Server] Lỗi xử lý client: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
            System.out.println("[Server] Client đã ngắt kết nối.");
        }
    }

    @SuppressWarnings("unchecked")
    private AuctionResponse processRequest(AuctionRequest request) {
        try {
            switch (request.getType()) {
                case LOGIN: {
                    User user = facade.login(request.get("email"), request.get("password"));
                    return AuctionResponse.ok(DtoMapper.toDto(user));
                }
                case REGISTER: {
                    User user = facade.register(
                            request.get("username"),
                            request.get("email"),
                            request.get("password"),
                            request.get("role")
                    );
                    // Bắn thông báo cho các Client khác biết có người mới đăng ký (dành cho Admin update)
                    BroadcastManager.broadcast(new AuctionEvent("USER_REGISTERED"));
                    return AuctionResponse.ok(DtoMapper.toDto(user));
                }
                case GET_REGISTRATION_ROLES: {
                    List<String> roles = facade.getAvailableRegistrationRoles();
                    return AuctionResponse.ok(new java.util.ArrayList<>(roles));
                }
                case RESET_PASSWORD: {
                    facade.resetPassword(
                            request.get("email"),
                            request.get("username"),
                            request.get("newPassword")
                    );
                    return AuctionResponse.ok();
                }
                case LIST_AUCTIONS: {
                    List<Auction> auctions = facade.listAuctions();
                    List<AuctionDto> dtos = new java.util.ArrayList<>();
                    for (Auction a : auctions) {
                        dtos.add(DtoMapper.toDto(a));
                    }
                    return AuctionResponse.ok(dtos);
                }
                case LIST_AUCTIONS_FOR_SELLER: {
                    List<Auction> auctions = facade.listAuctionsForSeller(request.get("sellerId"));
                    List<AuctionDto> dtos = new java.util.ArrayList<>();
                    for (Auction a : auctions) {
                        dtos.add(DtoMapper.toDto(a));
                    }
                    return AuctionResponse.ok(dtos);
                }
                case CREATE_AUCTION: {
                    Seller seller = new Seller(
                            request.get("sellerId"),
                            request.get("sellerName"),
                            request.get("sellerEmail")
                    );
                    Auction created = facade.createAuctionForSeller(
                            seller,
                            request.get("itemType"),
                            request.get("itemName"),
                            request.get("itemDescription"),
                            request.getDouble("startingPrice"),
                            request.get("imagePath")
                    );
                    BroadcastManager.broadcast(new AuctionEvent("AUCTION_CREATED"));
                    return AuctionResponse.ok(DtoMapper.toDto(created));
                }
                case START_AUCTION: {
                    facade.startAuction(request.get("auctionId"));
                    BroadcastManager.broadcast(new AuctionEvent("AUCTION_STARTED"));
                    return AuctionResponse.ok();
                }
                case FINISH_AUCTION: {
                    facade.finishAuction(request.get("auctionId"));
                    BroadcastManager.broadcast(new AuctionEvent("AUCTION_FINISHED"));
                    return AuctionResponse.ok();
                }
                case CANCEL_AUCTION: {
                    facade.cancelAuction(request.get("auctionId"));
                    BroadcastManager.broadcast(new AuctionEvent("AUCTION_CANCELED"));
                    return AuctionResponse.ok();
                }
                case MARK_AUCTION_PAID: {
                    facade.markAuctionPaid(request.get("auctionId"));
                    BroadcastManager.broadcast(new AuctionEvent("AUCTION_PAID"));
                    return AuctionResponse.ok();
                }
                case PLACE_BID: {
                    Bidder bidder = new Bidder(
                            request.get("bidderId"),
                            request.get("bidderName"),
                            request.get("bidderEmail")
                    );
                    facade.placeBid(request.get("auctionId"), bidder, request.getDouble("amount"));
                    BroadcastManager.broadcast(new AuctionEvent("NEW_BID"));
                    return AuctionResponse.ok();
                }
                case LIST_USERS: {
                    List<User> users = facade.listUsers();
                    List<UserDto> dtos = new java.util.ArrayList<>();
                    for (User u : users) {
                        dtos.add(DtoMapper.toDto(u));
                    }
                    return AuctionResponse.ok(dtos);
                }
                default:
                    return AuctionResponse.error("Loại request không được hỗ trợ: " + request.getType());
            }
        } catch (Exception e) {
            return AuctionResponse.error(e.getMessage());
        }
    }
}
