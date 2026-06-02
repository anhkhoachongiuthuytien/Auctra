package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.presentation.LoginViewModel;
import com.auction.ui.UIAnimations;
import com.auction.util.UiEffects;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class RegisterController {
    @FXML private StackPane rootPane;
    @FXML private TextField registerUsernameField;
    @FXML private TextField registerEmailField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private ProgressBar passwordStrengthBar;
    @FXML private Label passwordStrengthLabel;
    @FXML private Label messageLabel;

    private AppContext appContext;
    private SceneNavigator navigator;
    private LoginViewModel viewModel;

    public void init(AppContext appContext, SceneNavigator navigator) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.viewModel = new LoginViewModel(appContext.getGateway());
        roleComboBox.getItems().setAll(viewModel.getAvailableRegistrationRoles());
        roleComboBox.getSelectionModel().select("Bidder");
        registerPasswordField.textProperty().addListener((obs, oldValue, newValue) -> updatePasswordStrength(newValue));
        updatePasswordStrength("");
        if (messageLabel != null) {
            messageLabel.setText("");
        }
    }

    @FXML
    private void handleRegister() {
        String email = registerEmailField.getText();
        String password = registerPasswordField.getText();
        LoginViewModel.LoginResult result = viewModel.register(
                registerUsernameField.getText(),
                email,
                password,
                confirmPasswordField.getText(),
                roleComboBox.getValue()
        );
        showMessage(result);
        if (!result.success()) {
            UIAnimations.shakeField(registerPasswordField);
            UiEffects.showToast(rootPane, result.message(), UiEffects.ToastType.ERROR, 2400);
            return;
        }
        try {
            UiEffects.showToast(rootPane, "Tạo tài khoản thành công", UiEffects.ToastType.SUCCESS, 1200);
            navigator.showHome(result.user());
        } catch (IOException ex) {
            showNavigationError("Không mở được màn hình sau đăng ký", ex);
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

    private void showNavigationError(String message, IOException ex) {
        messageLabel.setText(message + ". Chi tiết đã được ghi ở console.");
        messageLabel.getStyleClass().removeAll("info-label", "success-label");
        if (!messageLabel.getStyleClass().contains("error-label")) {
            messageLabel.getStyleClass().add("error-label");
        }
        UiEffects.showToast(rootPane, message, UiEffects.ToastType.ERROR, 2600);
        ex.printStackTrace();
    }

    private void updatePasswordStrength(String password) {
        if (passwordStrengthBar == null || passwordStrengthLabel == null) {
            return;
        }
        int length = password == null ? 0 : password.length();
        double progress;
        String text;
        if (length == 0) {
            progress = 0;
            text = "Chưa nhập";
        } else if (length < 6) {
            progress = 0.25;
            text = "Yếu";
        } else if (length < 10) {
            progress = 0.55;
            text = "Trung bình";
        } else {
            progress = 0.9;
            text = "Mạnh";
        }
        passwordStrengthBar.setProgress(progress);
        passwordStrengthLabel.setText(text);
    }

    @FXML
    private void handleToggleTheme() {
        if (rootPane != null && rootPane.getScene() != null) {
            com.auction.ui.ThemeManager.toggle(rootPane.getScene());
        }
    }
}
