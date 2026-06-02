package com.auction.presentation;

import com.auction.client.AuctionClientGateway;
import com.auction.dao.memory.InMemoryUserDao;
import com.auction.model.user.User;
import com.auction.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginViewModelTest {
    private static final String PASSWORD = "demo12345";

    private LoginViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new LoginViewModel(new TestGateway(new AuthService(new InMemoryUserDao())));
    }

    // Đăng ký phải tạo tài khoản bidder thật thay vì chỉ dựa vào tài khoản demo có sẵn.
    @Test
    void testRegisterBidderSuccess() {
        LoginViewModel.LoginResult result = viewModel.register(
                "new_bidder",
                "bidder@custom.test",
                PASSWORD,
                PASSWORD,
                "Bidder"
        );

        assertTrue(result.success());
        User user = result.user();
        assertNotNull(user);
        assertEquals("bidder@custom.test", user.getEmail());
        assertEquals("Bidder", user.getClass().getSimpleName());
    }

    // Đăng ký phải thất bại sớm khi xác nhận mật khẩu không khớp.
    @Test
    void testRegisterWithMismatchedConfirmationFails() {
        LoginViewModel.LoginResult result = viewModel.register(
                "seller_user",
                "seller@custom.test",
                PASSWORD,
                "different123",
                "Seller"
        );

        assertFalse(result.success());
        assertTrue(result.message().contains("không khớp"));
    }

    private static final class TestGateway implements AuctionClientGateway {
        private final AuthService authService;

        private TestGateway(AuthService authService) {
            this.authService = authService;
        }

        @Override
        public User login(String email, String password) {
            return authService.login(email, password);
        }

        @Override
        public User register(String username, String email, String password, String role) {
            if ("Seller".equalsIgnoreCase(role)) {
                return authService.registerSeller(username, email, password);
            }
            return authService.registerBidder(username, email, password);
        }

        @Override
        public List<String> getAvailableRegistrationRoles() {
            return List.of("Bidder", "Seller");
        }

        @Override
        public void resetPassword(String email, String username, String newPassword) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<com.auction.model.auction.Auction> listAuctions() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<com.auction.model.auction.Auction> listAuctionsForSeller(String sellerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.auction.model.auction.Auction createAuctionForSeller(
                com.auction.model.user.Seller seller,
                String itemType,
                String name,
                String description,
                double startingPrice
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.auction.model.auction.Auction createAuctionForSeller(
                com.auction.model.user.Seller seller,
                String itemType,
                String name,
                String description,
                double startingPrice,
                String imagePath
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startAuction(String auctionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void finishAuction(String auctionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cancelAuction(String auctionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markAuctionPaid(String auctionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void placeBid(String auctionId, com.auction.model.user.Bidder bidder, double amount) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<User> listUsers() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void registerAutoBid(String auctionId, String bidderId, double maxPrice, double increment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cancelAutoBid(String auctionId, String bidderId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.auction.model.auction.AutoBidConfig getAutoBid(String auctionId, String bidderId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public User updateUser(String userId, String username, String email) {
            throw new UnsupportedOperationException();
        }

        @Override
        public User updateUser(String userId, String username, String email,
                               String shippingAddress, String phoneNumber,
                               String storeName, String storeDescription,
                               String department) {
            throw new UnsupportedOperationException();
        }

        @Override
        public User updateUser(String userId, String username, String email,
                               String shippingAddress, String phoneNumber,
                               String storeName, String storeDescription,
                               String department, String avatarPath) {
            throw new UnsupportedOperationException();
        }
    }
}
