package com.auction.client;

import com.auction.model.auction.Auction;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.protocol.AuctionDto;
import com.auction.protocol.AuctionRequest;
import com.auction.protocol.AuctionResponse;
import com.auction.protocol.DtoMapper;
import com.auction.protocol.ProtocolErrors;
import com.auction.protocol.RequestType;
import com.auction.protocol.UserDto;
import com.auction.util.ItemImageMigrationHelper;
import java.util.ArrayList;
import java.util.List;

/** Shared encoding and error handling keep LOCAL and SOCKET behavior identical. */
public abstract class AbstractAuctionClientGateway implements AuctionClientGateway {
    protected volatile String sessionToken;

    protected abstract AuctionResponse exchange(AuctionRequest request);

    protected void sessionChanged() { }

    protected synchronized AuctionResponse send(AuctionRequest request) {
        request.put("sessionToken", sessionToken);
        AuctionResponse response;
        try {
            response = exchange(request);
        } catch (com.auction.exception.AuctionException exception) {
            if (request.getType() == RequestType.LOGIN) { sessionToken = null; sessionChanged(); }
            throw exception;
        }
        if (!response.isSuccess()) {
            if (request.getType() == RequestType.LOGIN) { sessionToken = null; sessionChanged(); }
            throw ProtocolErrors.toException(response);
        }
        if (response.getSessionToken() != null) {
            sessionToken = response.getSessionToken();
            sessionChanged();
        }
        return response;
    }

    @Override
    public synchronized void logout() {
        try {
            if (sessionToken != null) { send(new AuctionRequest(RequestType.LOGOUT)); }
        } finally {
            sessionToken = null;
            sessionChanged();
        }
    }

    @Override
    public User login(String email, String password) {
        AuctionRequest req = new AuctionRequest(RequestType.LOGIN)
                .put("email", email)
                .put("password", password);
        AuctionResponse resp = send(req);
        User user = DtoMapper.toUser((UserDto) resp.getData());
        user.setRecoveryCode(resp.getRecoveryCode());
        return user;
    }

    @Override
    public User register(String username, String email, String password, String role) {
        AuctionRequest req = new AuctionRequest(RequestType.REGISTER)
                .put("username", username)
                .put("email", email)
                .put("password", password)
                .put("role", role);
        AuctionResponse resp = send(req);
        User user = DtoMapper.toUser((UserDto) resp.getData());
        user.setRecoveryCode(resp.getRecoveryCode());
        return user;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getAvailableRegistrationRoles() {
        AuctionRequest req = new AuctionRequest(RequestType.GET_REGISTRATION_ROLES);
        AuctionResponse resp = send(req);
        return (List<String>) resp.getData();
    }

    @Override
    public void resetPassword(String email, String recoveryCode, String newPassword) {
        AuctionRequest req = new AuctionRequest(RequestType.RESET_PASSWORD)
                .put("email", email)
                .put("recoveryCode", recoveryCode)
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
        ItemImageMigrationHelper.migrateLocalImagePaths(auctions, this);
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
        ItemImageMigrationHelper.migrateLocalImagePaths(auctions, this);
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
        return createAuctionForSeller(seller, itemType, name, description, startingPrice, imagePath, 5);
    }

    @Override
    public Auction createAuctionForSeller(Seller seller, String itemType, String name,
                                          String description, double startingPrice, String imagePath, int durationMinutes) {
        AuctionRequest req = new AuctionRequest(RequestType.CREATE_AUCTION)
                .put("sellerId", seller.getId())
                .put("sellerName", seller.getUsername())
                .put("sellerEmail", seller.getEmail())
                .put("itemType", itemType)
                .put("itemName", name)
                .put("itemDescription", description)
                .put("startingPrice", String.valueOf(startingPrice))
                .put("imagePath", imagePath)
                .put("durationMinutes", String.valueOf(durationMinutes));
        AuctionResponse resp = send(req);
        return DtoMapper.toAuction((AuctionDto) resp.getData());
    }

    @Override
    public void updateItemImagePath(String itemId, String imagePath) {
        AuctionRequest req = new AuctionRequest(RequestType.UPDATE_ITEM_IMAGE)
                .put("itemId", itemId)
                .put("imagePath", imagePath);
        send(req);
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
        User user = DtoMapper.toUser((UserDto) resp.getData());
        user.setRecoveryCode(resp.getRecoveryCode());
        return user;
    }
}
