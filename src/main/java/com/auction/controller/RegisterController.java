package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.presentation.LoginViewModel;
import com.auction.util.UiEffects;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
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
            UiEffects.showToast(rootPane, result.message(), UiEffects.ToastType.ERROR, 2400);
            return;
        }
        try {
            UiEffects.showToast(rootPane, "Tạo tài khoản thành công", UiEffects.ToastType.SUCCESS, 1200);
            navigator.showHome(result.user());
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
