package com.auction.presentation;

import com.auction.client.AuctionClientGateway;
import com.auction.exception.AuctionException;
import com.auction.model.user.User;

import java.util.List;

public class LoginViewModel {
    private final AuctionClientGateway gateway;

    public LoginViewModel(AuctionClientGateway gateway) {
        this.gateway = gateway;
    }

    public LoginResult login(String email, String password) {
        try {
            User user = gateway.login(email, password);
            return LoginResult.success(user, "Đăng nhập thành công");
        } catch (AuctionException ex) {
            return LoginResult.failure(ex.getMessage());
        }
    }

    public LoginResult register(String username, String email, String password, String confirmPassword, String role) {
        if (role == null || role.isBlank()) {
            return LoginResult.failure("Vui lòng chọn loại tài khoản.");
        }
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            return LoginResult.failure("Mật khẩu xác nhận không khớp.");
        }

        try {
            User user = gateway.register(username, email, password, role);
            return LoginResult.success(user, "Đăng ký thành công");
        } catch (AuctionException ex) {
            return LoginResult.failure(ex.getMessage());
        }
    }

    public List<String> getAvailableRegistrationRoles() {
        return gateway.getAvailableRegistrationRoles();
    }

    public String getDemoAccountsMessage() {
        return "Bạn có thể đăng ký tài khoản Bidder/Seller, hoặc dùng mật khẩu demo: demo12345";
    }

    public LoginResult resetPassword(String email, String username, String newPassword, String confirmPassword) {
        if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
            return LoginResult.failure("Mật khẩu xác nhận không khớp.");
        }
        try {
            gateway.resetPassword(email, username, newPassword);
            return LoginResult.success(null, "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.");
        } catch (AuctionException ex) {
            return LoginResult.failure(ex.getMessage());
        }
    }

    public record LoginResult(boolean success, User user, String message) {
        public static LoginResult success(User user, String message) {
            return new LoginResult(true, user, message);
        }

        public static LoginResult failure(String message) {
            return new LoginResult(false, null, message);
        }
    }
}
