package com.auction.app;

import com.auction.controller.AuctionController;
import com.auction.controller.AdminController;
import com.auction.controller.AuthController;
import com.auction.controller.ForgotPasswordController;
import com.auction.controller.ProfileController;
import com.auction.controller.RegisterController;
import com.auction.controller.SellerController;
import com.auction.model.user.Admin;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

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
        setScene(root, "Auctra • Đăng nhập");
    }

    public void showRegister() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register-view.fxml"));
        Parent root = loader.load();
        RegisterController controller = loader.getController();
        controller.init(appContext, this);
        setScene(root, "Auctra • Đăng ký");
    }

    public void showForgotPassword() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forgot-password-view.fxml"));
        Parent root = loader.load();
        ForgotPasswordController controller = loader.getController();
        controller.init(appContext, this);
        setScene(root, "Auctra • Đặt lại mật khẩu");
    }

    public void showAuctionList(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction-list-view.fxml"));
        Parent root = loader.load();
        AuctionController controller = loader.getController();
        controller.init(appContext, this, user);
        setScene(root, "Auctra • Phiên đấu giá");
    }

    public void showAuctionDetail(com.auction.model.auction.Auction auction, User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction-detail-view.fxml"));
        Parent root = loader.load();
        com.auction.controller.AuctionDetailController controller = loader.getController();
        controller.init(appContext, this, user, auction);
        setScene(root, "Auctra • " + auction.getItem().getName());
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
        setScene(root, "Auctra • Người bán");
    }

    public void showAdminDashboard(Admin admin) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin-view.fxml"));
        Parent root = loader.load();
        AdminController controller = loader.getController();
        controller.init(appContext, this, admin);
        setScene(root, "Auctra • Quản trị");
    }

    public void showProfile(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile-view.fxml"));
        Parent root = loader.load();
        ProfileController controller = loader.getController();
        controller.init(appContext, this, user);
        setScene(root, "Auctra • Tài khoản");
    }

    /**
     * Chuyển đến màn hình "phiên của tôi" theo role:
     * - Seller → seller dashboard (các phiên của seller)
     * - Admin → admin dashboard
     * - Bidder → auction list chung (chưa có My Bids riêng)
     */
    public void showMyAuctions(User user) throws IOException {
        if (user instanceof Seller seller) {
            showSellerDashboard(seller);
        } else if (user instanceof Admin admin) {
            showAdminDashboard(admin);
        } else {
            showAuctionList(user);
        }
    }

    private void setScene(Parent root, String title) {
        double width = 1180;
        double height = 760;

        // Tự động thu nhỏ cửa sổ nếu là màn hình Auth (Login/Register/Forgot)
        if (title.contains("Đăng nhập") || title.contains("Đăng ký") || title.contains("Đặt lại mật khẩu")) {
            width = 460;
            height = 680;
        }

        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        
        stage.setTitle(title);
        stage.setScene(scene);
        
        // Cấu hình linh hoạt
        if (width < 600) {
            stage.setResizable(false); // Không cho resize khi đang ở Login
        } else {
            stage.setResizable(true);
            stage.setMinWidth(1000);
            stage.setMinHeight(700);
        }
        
        stage.centerOnScreen();
        stage.show();

        FadeTransition fade = new FadeTransition(Duration.millis(220), root);
        fade.setFromValue(0.6);
        fade.setToValue(1.0);
        fade.play();
    }
}
