package com.auction.app;

import com.auction.controller.AuctionController;
import com.auction.controller.AdminController;
import com.auction.controller.AuthController;
import com.auction.controller.SellerController;
import com.auction.model.user.Admin;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneNavigator {
    private final Stage stage;
    private final AppContext appContext;

    public SceneNavigator(Stage stage, AppContext appContext) {
        this.stage = stage;
        this.appContext = appContext;
    }

    public void showLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
        Parent root = loader.load();
        AuthController controller = loader.getController();
        controller.init(appContext, this);
        setScene(root, "Auction System - Login");
    }

    public void showAuctionList(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction-list-view.fxml"));
        Parent root = loader.load();
        AuctionController controller = loader.getController();
        controller.init(appContext, this, user);
        setScene(root, "Auction System - Auction List");
    }

    public void showHome(User user) throws IOException {
        if (user instanceof Seller seller) {
            showSellerDashboard(seller);
            return;
        }
        if (user instanceof Admin admin) {
            showAdminDashboard(admin);
            return;
        }
        showAuctionList(user);
    }

    public void showSellerDashboard(Seller seller) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/seller-view.fxml"));
        Parent root = loader.load();
        SellerController controller = loader.getController();
        controller.init(appContext, this, seller);
        setScene(root, "Auction System - Seller Dashboard");
    }

    public void showAdminDashboard(Admin admin) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin-view.fxml"));
        Parent root = loader.load();
        AdminController controller = loader.getController();
        controller.init(appContext, this, admin);
        setScene(root, "Auction System - Admin Dashboard");
    }

    private void setScene(Parent root, String title) {
        Scene scene = new Scene(root, 980, 620);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
}
