package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.presentation.LoginViewModel;
import com.auction.util.UiEffects;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class ForgotPasswordController {
    @FXML private StackPane rootPane;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
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
        // Enter ở ô cuối → submit
        confirmPasswordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleReset();
            }
        });
    }

    @FXML
    private void handleReset() {
        LoginViewModel.LoginResult result = viewModel.resetPassword(
                emailField.getText(),
                usernameField.getText(),
                newPasswordField.getText(),
                confirmPasswordField.getText()
        );
        showMessage(result);
        if (!result.success()) {
            UiEffects.showToast(rootPane, result.message(), UiEffects.ToastType.ERROR, 2400);
            return;
        }
        try {
            UiEffects.showToast(rootPane, "Đặt lại mật khẩu thành công",
                    UiEffects.ToastType.SUCCESS, 1800);
            navigator.showLogin();
        } catch (IOException ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void goToLogin() throws IOException {
        navigator.showLogin();
    }

    private void showMessage(LoginViewModel.LoginResult result) {
        messageLabel.setText(result.message());
        messageLabel.getStyleClass().removeAll("error-label", "success-label");
        messageLabel.getStyleClass().add(result.success() ? "success-label" : "error-label");
    }
}
