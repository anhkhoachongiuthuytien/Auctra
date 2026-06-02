package com.auction.server;

import com.auction.model.auction.Auction;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.util.List;

public class AuctionServerFacade {
    private static final List<String> REGISTRATION_ROLES = List.of("Bidder", "Seller");

    private final ServerContext serverContext;

    public AuctionServerFacade(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    public User login(String email, String password) {
        return serverContext.getAuthService().login(email, password);
    }

    public User register(String username, String email, String password, String role) {
        if ("Seller".equalsIgnoreCase(role)) {
            return serverContext.getAuthService().registerSeller(username, email, password);
        }
        if ("Bidder".equalsIgnoreCase(role)) {
            return serverContext.getAuthService().registerBidder(username, email, password);
        }
        throw new IllegalArgumentException("Vai trò đăng ký không được hỗ trợ: " + role);
    }

    public List<String> getAvailableRegistrationRoles() {
        return REGISTRATION_ROLES;
    }

    public void resetPassword(String email, String username, String newPassword) {
        serverContext.getAuthService().resetPassword(email, username, newPassword);
    }

    public List<Auction> listAuctions() {
        return serverContext.getAuctionService().listAuctions();
    }

    public List<Auction> listAuctionsForSeller(String sellerId) {
        return listAuctions().stream()
                .filter(auction -> auction.getSeller().getId().equals(sellerId))
                .toList();
    }

    public Auction createAuctionForSeller(Seller seller, String itemType, String name, String description, double startingPrice) {
        return createAuctionForSeller(seller, itemType, name, description, startingPrice, null);
    }

    public Auction createAuctionForSeller(Seller seller, String itemType, String name, String description,
                                          double startingPrice, String imagePath) {
        return serverContext.getSellerService().createAuction(
                serverContext.getSellerService().createItem(
                        com.auction.enums.ItemType.fromString(itemType),
                        name, description, startingPrice, imagePath),
                seller
        );
    }

    public void startAuction(String auctionId) {
        serverContext.getAuctionService().startAuction(auctionId);
    }

    public void finishAuction(String auctionId) {
        serverContext.getAuctionService().finishAuction(auctionId);
    }

    public void cancelAuction(String auctionId) {
        serverContext.getAuctionService().cancelAuction(auctionId);
    }

    public void markAuctionPaid(String auctionId) {
        serverContext.getAuctionService().markAuctionPaid(auctionId);
    }

    public void placeBid(String auctionId, Bidder bidder, double amount) {
        serverContext.getBidService().placeBid(auctionId, bidder, amount);
    }

    public List<User> listUsers() {
        return serverContext.getUserService().getAllUsers();
    }

    public void registerAutoBid(String auctionId, String bidderId, double maxPrice, double increment) {
        serverContext.getAutoBidDao().save(new com.auction.model.auction.AutoBidConfig(auctionId, bidderId, maxPrice, increment));
    }

    public void cancelAutoBid(String auctionId, String bidderId) {
        serverContext.getAutoBidDao().delete(auctionId, bidderId);
    }

    public com.auction.model.auction.AutoBidConfig getAutoBid(String auctionId, String bidderId) {
        return serverContext.getAutoBidDao().find(auctionId, bidderId);
    }

    public User updateUser(String userId, String username, String email) {
        return updateUser(userId, username, email, null, null, null, null, null, null);
    }

    public User updateUser(String userId, String username, String email,
                           String shippingAddress, String phoneNumber,
                           String storeName, String storeDescription,
                           String department) {
        return updateUser(userId, username, email, shippingAddress, phoneNumber, storeName, storeDescription, department, null);
    }

    public User updateUser(String userId, String username, String email,
                           String shippingAddress, String phoneNumber,
                           String storeName, String storeDescription,
                           String department, String avatarPath) {
        return serverContext.getUserService().updateUser(userId, username, email,
                shippingAddress, phoneNumber, storeName, storeDescription, department, avatarPath);
    }
}
