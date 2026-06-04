package com.auction.fxml;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.client.AuctionClientGateway;
import com.auction.enums.AuctionStatus;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.fail;

class SceneNavigatorSmokeTest {
    private static final Bidder BIDDER = new Bidder("B001", "bidder_demo", "bidder@auction.local");
    private static final Seller SELLER = new Seller("S001", "seller_demo", "seller@auction.local");
    private static final Admin ADMIN = new Admin("A001", "admin_demo", "admin@auction.local");

    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("JavaFX toolkit did not start");
        }
    }

    @Test
    void homeRoutesLoadForEveryRole() throws Exception {
        runOnFx(() -> {
            AppContext context = new AppContext(new MockGateway());
            Stage stage = new Stage();
            try {
                SceneNavigator navigator = new SceneNavigator(stage, context);
                navigator.showHome(BIDDER);
                navigator.showHome(SELLER);
                navigator.showHome(ADMIN);
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void profileRoutesLoadForEveryRole() throws Exception {
        runOnFx(() -> {
            AppContext context = new AppContext(new MockGateway());
            Stage stage = new Stage();
            try {
                SceneNavigator navigator = new SceneNavigator(stage, context);
                navigator.showProfile(BIDDER);
                navigator.showProfile(SELLER);
                navigator.showProfile(ADMIN);
            } finally {
                stage.close();
            }
        });
    }

    private static void runOnFx(ThrowingRunnable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(15, TimeUnit.SECONDS)) {
            fail("JavaFX action timed out");
        }
        Throwable error = failure.get();
        if (error != null) {
            throw new AssertionError("Scene route failed", error);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class MockGateway implements AuctionClientGateway {
        private final List<User> users = new ArrayList<>(List.of(BIDDER, SELLER, ADMIN));
        private final List<Auction> auctions = new ArrayList<>();

        private MockGateway() {
            Item laptop = new Item("I001", "Laptop demo", "Máy demo cho smoke test", 1500.0);
            Auction running = new Auction("AU001", laptop, SELLER);
            running.restoreState(AuctionStatus.RUNNING, 1700.0, null, new ArrayList<>(), LocalDateTime.now().plusHours(2));
            auctions.add(running);
        }

        @Override
        public User login(String email, String password) {
            return users.stream()
                    .filter(user -> user.getEmail().equals(email))
                    .findFirst()
                    .orElseThrow();
        }

        @Override
        public User register(String username, String email, String password, String role) {
            return new Bidder("BNEW", username, email);
        }

        @Override
        public List<String> getAvailableRegistrationRoles() {
            return List.of("Bidder", "Seller");
        }

        @Override
        public void resetPassword(String email, String username, String newPassword) {
        }

        @Override
        public List<Auction> listAuctions() {
            return auctions;
        }

        @Override
        public List<Auction> listAuctionsForSeller(String sellerId) {
            return auctions.stream()
                    .filter(auction -> auction.getSeller().getId().equals(sellerId))
                    .toList();
        }

        @Override
        public Auction createAuctionForSeller(Seller seller, String itemType, String name, String description, double startingPrice) {
            return createAuctionForSeller(seller, itemType, name, description, startingPrice, null);
        }

        @Override
        public Auction createAuctionForSeller(Seller seller, String itemType, String name, String description, double startingPrice, String imagePath) {
            return createAuctionForSeller(seller, itemType, name, description, startingPrice, imagePath, 5);
        }

        @Override
        public Auction createAuctionForSeller(Seller seller, String itemType, String name, String description, double startingPrice, String imagePath, int durationMinutes) {
            Item item = new Item("IN", name, description, startingPrice);
            item.setImagePath(imagePath);
            Auction auction = new Auction("AUN", item, seller);
            auction.setDurationMinutes(durationMinutes);
            auctions.add(auction);
            return auction;
        }

        @Override public void startAuction(String auctionId) { }
        @Override public void finishAuction(String auctionId) { }
        @Override public void cancelAuction(String auctionId) { }
        @Override public void markAuctionPaid(String auctionId) { }
        @Override public void placeBid(String auctionId, Bidder bidder, double amount) { }

        @Override
        public List<User> listUsers() {
            return users;
        }

        @Override public void registerAutoBid(String auctionId, String bidderId, double maxPrice, double increment) { }
        @Override public void cancelAutoBid(String auctionId, String bidderId) { }
        @Override public com.auction.model.auction.AutoBidConfig getAutoBid(String auctionId, String bidderId) { return null; }

        @Override
        public User updateUser(String userId, String username, String email) {
            return updateUser(userId, username, email, null, null, null, null, null, null);
        }

        @Override
        public User updateUser(String userId, String username, String email, String shippingAddress, String phoneNumber,
                               String storeName, String storeDescription, String department) {
            return updateUser(userId, username, email, shippingAddress, phoneNumber, storeName, storeDescription, department, null);
        }

        @Override
        public User updateUser(String userId, String username, String email, String shippingAddress, String phoneNumber,
                               String storeName, String storeDescription, String department, String avatarPath) {
            if (userId.equals(SELLER.getId())) {
                return new Seller(userId, username, email, storeName, storeDescription);
            }
            if (userId.equals(ADMIN.getId())) {
                return new Admin(userId, username, email, department);
            }
            return new Bidder(userId, username, email, shippingAddress, phoneNumber);
        }
    }
}
