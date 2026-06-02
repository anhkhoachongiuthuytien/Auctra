package com.auction.client;

import com.auction.model.auction.Auction;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.util.List;

public interface AuctionClientGateway {
    User login(String email, String password);

    User register(String username, String email, String password, String role);

    List<String> getAvailableRegistrationRoles();

    void resetPassword(String email, String username, String newPassword);

    List<Auction> listAuctions();

    List<Auction> listAuctionsForSeller(String sellerId);

    Auction createAuctionForSeller(Seller seller, String itemType, String name, String description, double startingPrice);

    Auction createAuctionForSeller(Seller seller, String itemType, String name, String description,
                                   double startingPrice, String imagePath);

    void startAuction(String auctionId);

    void finishAuction(String auctionId);

    void cancelAuction(String auctionId);

    void markAuctionPaid(String auctionId);

    void placeBid(String auctionId, Bidder bidder, double amount);

    List<User> listUsers();

    void registerAutoBid(String auctionId, String bidderId, double maxPrice, double increment);

    void cancelAutoBid(String auctionId, String bidderId);

    com.auction.model.auction.AutoBidConfig getAutoBid(String auctionId, String bidderId);

    User updateUser(String userId, String username, String email);

    User updateUser(String userId, String username, String email,
                    String shippingAddress, String phoneNumber,
                    String storeName, String storeDescription,
                    String department);

    User updateUser(String userId, String username, String email,
                    String shippingAddress, String phoneNumber,
                    String storeName, String storeDescription,
                    String department, String avatarPath);
}
