package com.auction.app;

import com.auction.controller.AuctionController;
import com.auction.controller.AuthController;
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

    private void setScene(Parent root, String title) {
        Scene scene = new Scene(root, 980, 620);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
}
