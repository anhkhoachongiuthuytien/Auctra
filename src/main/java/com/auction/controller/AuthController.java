package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.exception.AuthenticationException;
import com.auction.model.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class AuthController {
    @FXML
    private TextField emailField;

    @FXML
    private Label messageLabel;

    private AppContext appContext;
    private SceneNavigator navigator;

    public void init(AppContext appContext, SceneNavigator navigator) {
        this.appContext = appContext;
        this.navigator = navigator;
        messageLabel.setText("Demo accounts: seller@auction.local, bidder@auction.local, admin@auction.local");
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isEmpty()) {
            messageLabel.setText("Please enter an email.");
            return;
        }

        try {
            User user = appContext.getAuthService().login(email);
            navigator.showAuctionList(user);
        } catch (AuthenticationException | IOException ex) {
            messageLabel.setText(ex.getMessage());
        }
    }
}
