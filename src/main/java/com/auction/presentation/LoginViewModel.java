package com.auction.presentation;

import com.auction.exception.AuctionException;
import com.auction.model.user.User;
import com.auction.service.AuthService;

public class LoginViewModel {
    private final AuthService authService;

    public LoginViewModel(AuthService authService) {
        this.authService = authService;
    }

    public LoginResult login(String email) {
        try {
            User user = authService.login(email);
            return LoginResult.success(user, "Login successful");
        } catch (AuctionException ex) {
            return LoginResult.failure(ex.getMessage());
        }
    }

    public String getDemoAccountsMessage() {
        return "Demo accounts: seller@auction.local, bidder@auction.local, admin@auction.local";
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
