package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.model.user.User;
import com.auction.presentation.LoginViewModel;
import com.auction.util.UiEffects;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class AuthController {
    @FXML private StackPane rootPane;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private AppContext appContext;
    private SceneNavigator navigator;
    private LoginViewModel viewModel;

    public void init(AppContext appContext, SceneNavigator navigator) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.viewModel = new LoginViewModel(appContext.getGateway());
        if (messageLabel != null) {
            messageLabel.setText("");
        }
    }

    @FXML
    private void handleLogin() {
        LoginViewModel.LoginResult result = viewModel.login(emailField.getText(), passwordField.getText());
        showMessage(result);
        if (!result.success()) {
            UiEffects.showToast(rootPane, result.message(), UiEffects.ToastType.ERROR, 2400);
            return;
        }
        try {
            User user = result.user();
            UiEffects.showToast(rootPane, "Đăng nhập thành công", UiEffects.ToastType.SUCCESS, 1200);
            navigator.showHome(user);
        } catch (IOException ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void goToRegister() throws IOException {
        navigator.showRegister();
    }

    @FXML
    private void goToForgotPassword() throws IOException {
        navigator.showForgotPassword();
    }

    private void showMessage(LoginViewModel.LoginResult result) {
        messageLabel.setText(result.message());
        messageLabel.getStyleClass().removeAll("error-label", "success-label");
        messageLabel.getStyleClass().add(result.success() ? "success-label" : "error-label");
    }
}
