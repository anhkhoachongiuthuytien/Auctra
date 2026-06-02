package com.auction.client;

import com.auction.model.auction.Auction;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.server.AuctionServerFacade;
import javafx.application.Platform;

import java.util.List;

public class LocalAuctionClientGateway implements AuctionClientGateway {
    private final AuctionServerFacade serverFacade;

    public LocalAuctionClientGateway(AuctionServerFacade serverFacade) {
        this.serverFacade = serverFacade;
    }

    @Override
    public User login(String email, String password) {
        return serverFacade.login(email, password);
    }

    @Override
    public User register(String username, String email, String password, String role) {
        return serverFacade.register(username, email, password, role);
    }

    @Override
    public List<String> getAvailableRegistrationRoles() {
        return serverFacade.getAvailableRegistrationRoles();
    }

    @Override
    public void resetPassword(String email, String username, String newPassword) {
        serverFacade.resetPassword(email, username, newPassword);
    }

    @Override
    public List<Auction> listAuctions() {
        return serverFacade.listAuctions();
    }

    @Override
    public List<Auction> listAuctionsForSeller(String sellerId) {
        return serverFacade.listAuctionsForSeller(sellerId);
    }

    @Override
    public Auction createAuctionForSeller(Seller seller, String itemType, String name, String description, double startingPrice) {
        Auction auction = serverFacade.createAuctionForSeller(seller, itemType, name, description, startingPrice);
        fireLocalUpdate();
        return auction;
    }

    @Override
    public Auction createAuctionForSeller(Seller seller, String itemType, String name, String description,
                                          double startingPrice, String imagePath) {
        Auction auction = serverFacade.createAuctionForSeller(seller, itemType, name, description, startingPrice, imagePath);
        fireLocalUpdate();
        return auction;
    }

    @Override
    public void startAuction(String auctionId) {
        serverFacade.startAuction(auctionId);
        fireLocalUpdate();
    }

    @Override
    public void finishAuction(String auctionId) {
        serverFacade.finishAuction(auctionId);
        fireLocalUpdate();
    }

    @Override
    public void cancelAuction(String auctionId) {
        serverFacade.cancelAuction(auctionId);
        fireLocalUpdate();
    }

    @Override
    public void markAuctionPaid(String auctionId) {
        serverFacade.markAuctionPaid(auctionId);
        fireLocalUpdate();
    }

    @Override
    public void placeBid(String auctionId, Bidder bidder, double amount) {
        serverFacade.placeBid(auctionId, bidder, amount);
        fireLocalUpdate();
    }

    @Override
    public List<User> listUsers() {
        return serverFacade.listUsers();
    }

    @Override
    public void registerAutoBid(String auctionId, String bidderId, double maxPrice, double increment) {
        serverFacade.registerAutoBid(auctionId, bidderId, maxPrice, increment);
        fireLocalUpdate();
    }

    @Override
    public void cancelAutoBid(String auctionId, String bidderId) {
        serverFacade.cancelAutoBid(auctionId, bidderId);
        fireLocalUpdate();
    }

    @Override
    public com.auction.model.auction.AutoBidConfig getAutoBid(String auctionId, String bidderId) {
        return serverFacade.getAutoBid(auctionId, bidderId);
    }

    @Override
    public User updateUser(String userId, String username, String email) {
        User user = serverFacade.updateUser(userId, username, email);
        fireLocalUpdate();
        return user;
    }

    @Override
    public User updateUser(String userId, String username, String email,
                           String shippingAddress, String phoneNumber,
                           String storeName, String storeDescription,
                           String department) {
        User user = serverFacade.updateUser(userId, username, email,
                shippingAddress, phoneNumber, storeName, storeDescription, department);
        fireLocalUpdate();
        return user;
    }

    @Override
    public User updateUser(String userId, String username, String email,
                           String shippingAddress, String phoneNumber,
                           String storeName, String storeDescription,
                           String department, String avatarPath) {
        User user = serverFacade.updateUser(userId, username, email,
                shippingAddress, phoneNumber, storeName, storeDescription, department, avatarPath);
        fireLocalUpdate();
        return user;
    }

    private void fireLocalUpdate() {
        try {
            if (Platform.isFxApplicationThread()) {
                ClientEventManager.fireUpdate();
            } else {
                Platform.runLater(ClientEventManager::fireUpdate);
            }
        } catch (IllegalStateException ex) {
            ClientEventManager.fireUpdate();
        }
    }
}
