package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.model.user.User;
import com.auction.util.UiEffects;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.Locale;

public class ProfileController {
    @FXML private StackPane rootPane;
    @FXML private Label userInitialsLabel;
    @FXML private Label userAvatarBigLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label userIdLabel;
    @FXML private Label navBrandLabel;

    // Nav items (giữ để highlight active)
    @FXML private javafx.scene.control.Button navHome;
    @FXML private javafx.scene.control.Button navAuctions;
    @FXML private javafx.scene.control.Button navMyAuctions;
    @FXML private javafx.scene.control.Button navProfile;

    private AppContext appContext;
    private SceneNavigator navigator;
    private User currentUser;

    public void init(AppContext appContext, SceneNavigator navigator, User currentUser) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.currentUser = currentUser;

        String name = currentUser.getUsername();
        String initial = name == null || name.isBlank() ? "U"
                : name.substring(0, 1).toUpperCase(Locale.ROOT);
        userInitialsLabel.setText(initial);
        userAvatarBigLabel.setText(initial);
        userNameLabel.setText(name);
        userEmailLabel.setText(currentUser.getEmail());
        userRoleLabel.setText(currentUser.getClass().getSimpleName());
        userIdLabel.setText(currentUser.getId());

        // Active highlight cho nav Profile
        navProfile.getStyleClass().removeAll("nav-link");
        navProfile.getStyleClass().add("nav-link-active");
    }

    @FXML private void goToHome()        throws IOException { navigator.showHome(currentUser); }
    @FXML private void goToAuctions()    throws IOException { navigator.showAuctionList(currentUser); }
    @FXML private void goToMyAuctions()  throws IOException { navigator.showMyAuctions(currentUser); }
    @FXML private void goToProfile()     throws IOException { /* đang ở đây */ }

    @FXML
    private void handleLogout() throws IOException {
        navigator.showLogin();
    }

    @FXML
    private void handleEditProfile() {
        UiEffects.showToast(rootPane, "Chức năng chỉnh sửa đang phát triển",
                UiEffects.ToastType.INFO, 1800);
    }
}
