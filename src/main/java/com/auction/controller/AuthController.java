package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.model.user.User;
import com.auction.presentation.LoginViewModel;
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
    private LoginViewModel viewModel;

    public void init(AppContext appContext, SceneNavigator navigator) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.viewModel = new LoginViewModel(appContext.getAuthService());
        messageLabel.setText(viewModel.getDemoAccountsMessage());
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        LoginViewModel.LoginResult result = viewModel.login(email);
        messageLabel.setText(result.message());
        if (!result.success()) {
            return;
        }
        try {
            User user = result.user();
            navigator.showAuctionList(user);
        } catch (IOException ex) {
            messageLabel.setText(ex.getMessage());
        }
    }
}
