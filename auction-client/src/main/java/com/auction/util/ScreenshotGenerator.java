package com.auction.util;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.client.AuctionClientGateway;
import com.auction.controller.*;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScreenshotGenerator extends Application {
    private AppContext appContext;
    private SceneNavigator navigator;
    private Stage dummyStage;

    private static class MockAuctionClientGateway implements AuctionClientGateway {
        @Override public User login(String email, String password) { return null; }
        @Override public User register(String username, String email, String password, String role) { return null; }
        @Override public List<String> getAvailableRegistrationRoles() { return List.of("Bidder", "Seller", "Admin"); }
        @Override public void resetPassword(String email, String username, String newPassword) {}
        @Override public List<Auction> listAuctions() { return new ArrayList<>(); }
        @Override public List<Auction> listAuctionsForSeller(String sellerId) { return new ArrayList<>(); }
        @Override public Auction createAuctionForSeller(Seller seller, String itemType, String name, String description, double startingPrice) { return null; }
        @Override public Auction createAuctionForSeller(Seller seller, String itemType, String name, String description, double startingPrice, String imagePath) { return null; }
        @Override public Auction createAuctionForSeller(Seller seller, String itemType, String name, String description, double startingPrice, String imagePath, int durationMinutes) { return null; }
        @Override public void startAuction(String auctionId) {}
        @Override public void finishAuction(String auctionId) {}
        @Override public void cancelAuction(String auctionId) {}
        @Override public void markAuctionPaid(String auctionId) {}
        @Override public void placeBid(String auctionId, Bidder bidder, double amount) {}
        @Override public List<User> listUsers() { return new ArrayList<>(); }
        @Override public void registerAutoBid(String auctionId, String bidderId, double maxPrice, double increment) {}
        @Override public void cancelAutoBid(String auctionId, String bidderId) {}
        @Override public com.auction.model.auction.AutoBidConfig getAutoBid(String auctionId, String bidderId) { return null; }
        @Override public User updateUser(String userId, String username, String email) { return null; }
        @Override public User updateUser(String userId, String username, String email, String shippingAddress, String phoneNumber, String storeName, String storeDescription, String department) { return null; }
        @Override public User updateUser(String userId, String username, String email, String shippingAddress, String phoneNumber, String storeName, String storeDescription, String department, String avatarPath) { return null; }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.dummyStage = primaryStage;
        // Sử dụng Mock Gateway để không bao giờ bị khóa file SQLite hoặc bị nghẽn mạng!
        this.appContext = new AppContext(new MockAuctionClientGateway());
        this.navigator = new SceneNavigator(primaryStage, appContext);

        // Chuẩn bị thực thể giả lập
        Bidder bidder = new Bidder("U001", "Nguyễn Văn A", "bidder@example.com");
        Seller seller = new Seller("U002", "Cửa hàng Hitech", "seller@example.com");
        Admin admin = new Admin("U003", "Trần Quản Trị", "admin@example.com");

        Item item = new Item("I001", "iPhone 15 Pro Max 256GB", "Màu Titan Tự nhiên, nguyên seal, chính hãng VN/A.", 1200.0);
        Auction auction = new Auction("A001", item, seller);
        auction.restoreState(com.auction.enums.AuctionStatus.RUNNING, 1250.0, null, new ArrayList<>(), LocalDateTime.now().plusDays(2));

        String outputDir = java.nio.file.Paths.get("auction-client", "target", "ui-screenshots")
                .toAbsolutePath() + File.separator;

        // Tạo thư mục nếu chưa tồn tại
        new File(outputDir).mkdirs();

        // 1. Màn hình đăng nhập
        captureView("/fxml/login-view.fxml", (loader) -> {
            AuthController c = loader.getController();
            c.init(appContext, navigator);
        }, outputDir + "login_view.png");

        // 2. Màn hình đăng ký
        captureView("/fxml/register-view.fxml", (loader) -> {
            RegisterController c = loader.getController();
            c.init(appContext, navigator);
        }, outputDir + "register_view.png");

        // 3. Màn hình quên mật khẩu
        captureView("/fxml/forgot-password-view.fxml", (loader) -> {
            ForgotPasswordController c = loader.getController();
            c.init(appContext, navigator);
        }, outputDir + "forgot_password_view.png");

        // 4. Màn hình danh sách đấu giá
        captureView("/fxml/auction-list-view.fxml", (loader) -> {
            AuctionController c = loader.getController();
            c.init(appContext, navigator, bidder);

            // Ghi đè danh sách đấu giá bằng dữ liệu giả lập chất lượng cao
            Field field = AuctionController.class.getDeclaredField("masterList");
            field.setAccessible(true);
            ObservableList<Auction> masterList = (ObservableList<Auction>) field.get(c);
            
            Auction a1 = new Auction("A001", new Item("I001", "iPhone 15 Pro Max 256GB", "Nguyên seal, chính hãng VN/A", 1200.0), seller);
            a1.restoreState(com.auction.enums.AuctionStatus.RUNNING, 1250.0, null, new ArrayList<>(), LocalDateTime.now().plusHours(5));
            
            Auction a2 = new Auction("A002", new Item("I002", "MacBook Pro M3 Max 16\"", "Space Black, 36GB RAM, 1TB SSD", 2500.0), seller);
            a2.restoreState(com.auction.enums.AuctionStatus.RUNNING, 2700.0, null, new ArrayList<>(), LocalDateTime.now().plusHours(14));
            
            Auction a3 = new Auction("A003", new Item("I003", "Tranh sơn dầu Mùa Thu Vàng", "Kích thước 80x120cm, vẽ tay bởi họa sĩ nổi tiếng", 800.0), seller);
            a3.restoreState(com.auction.enums.AuctionStatus.OPEN, 850.0, null, new ArrayList<>(), LocalDateTime.now().plusDays(2));

            masterList.setAll(a1, a2, a3);
            // Kích hoạt filter vẽ lại lưới bằng reflection
            Method method = AuctionController.class.getDeclaredMethod("handleFilterActive");
            method.setAccessible(true);
            method.invoke(c);
        }, outputDir + "auction_list_view.png");

        // 5. Màn hình chi tiết đấu giá
        captureView("/fxml/auction-detail-view.fxml", (loader) -> {
            AuctionDetailController c = loader.getController();
            c.init(appContext, navigator, bidder, auction);
        }, outputDir + "auction_detail_view.png");

        // 6. Màn hình tài khoản cá nhân
        captureView("/fxml/profile-view.fxml", (loader) -> {
            ProfileController c = loader.getController();
            c.init(appContext, navigator, bidder);
        }, outputDir + "profile_view.png");

        // 7. Màn hình người bán
        captureView("/fxml/seller-view.fxml", (loader) -> {
            SellerController c = loader.getController();
            c.init(appContext, navigator, seller);

            // Ghi đè danh sách đấu giá của người bán bằng dữ liệu giả lập
            Platform.runLater(() -> {
                try {
                    Auction s1 = new Auction("A001", new Item("I001", "iPhone 15 Pro Max 256GB", "Nguyên seal", 1200.0), seller);
                    s1.restoreState(com.auction.enums.AuctionStatus.RUNNING, 1250.0, null, new ArrayList<>(), LocalDateTime.now().plusHours(5));

                    Auction s2 = new Auction("A004", new Item("I004", "iPad Pro M4 11\"", "256GB Wifi, màu Bạc", 900.0), seller);
                    s2.restoreState(com.auction.enums.AuctionStatus.OPEN, 950.0, null, new ArrayList<>(), LocalDateTime.now().plusDays(4));

                    List<Auction> list = List.of(s1, s2);
                    // Dùng reflection gọi private method renderGrid
                    Method method = SellerController.class.getDeclaredMethod("renderGrid", List.class);
                    method.setAccessible(true);
                    method.invoke(c, list);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }, outputDir + "seller_view.png");

        // 8. Màn hình quản trị
        captureView("/fxml/admin-view.fxml", (loader) -> {
            AdminController c = loader.getController();
            c.init(appContext, navigator, admin);

            // Ghi đè dữ liệu userMasterList và auctionMasterList bằng reflection
            Field usersField = AdminController.class.getDeclaredField("userMasterList");
            usersField.setAccessible(true);
            ObservableList<User> userList = (ObservableList<User>) usersField.get(c);
            userList.setAll(bidder, seller, admin);

            Field auctionsField = AdminController.class.getDeclaredField("auctionMasterList");
            auctionsField.setAccessible(true);
            ObservableList<Auction> auctionList = (ObservableList<Auction>) auctionsField.get(c);

            Auction ad1 = new Auction("A001", new Item("I001", "iPhone 15 Pro Max 256GB", "Nguyên seal", 1200.0), seller);
            ad1.restoreState(com.auction.enums.AuctionStatus.RUNNING, 1250.0, null, new ArrayList<>(), LocalDateTime.now().plusHours(5));

            Auction ad2 = new Auction("A002", new Item("I002", "MacBook Pro M3 Max", "36GB RAM", 2500.0), seller);
            ad2.restoreState(com.auction.enums.AuctionStatus.RUNNING, 2700.0, null, new ArrayList<>(), LocalDateTime.now().plusHours(14));

            auctionList.setAll(ad1, ad2);
            
            Platform.runLater(() -> {
                try {
                    Method method = AdminController.class.getDeclaredMethod("handleShowDashboard");
                    method.setAccessible(true);
                    method.invoke(c);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }, outputDir + "admin_view.png");

        System.out.println("[SUCCESS] Đã tạo thành công ảnh chụp toàn bộ giao diện của ứng dụng!");
        Platform.exit();
        System.exit(0);
    }

    private interface ControllerInitializer {
        void init(FXMLLoader loader) throws Exception;
    }

    private void captureView(String fxmlPath, ControllerInitializer initializer, String outputPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setCharset(StandardCharsets.UTF_8);
            Parent root = loader.load();

            initializer.init(loader);

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            dummyStage.setScene(scene);
            dummyStage.show();

            root.applyCss();
            root.layout();

            WritableImage fxImage = scene.snapshot(null);
            BufferedImage bufImage = toBufferedImage(fxImage);
            ImageIO.write(bufImage, "png", new File(outputPath));
            System.out.println("Đã chụp & lưu: " + outputPath);

        } catch (Exception e) {
            System.err.println("Lỗi khi tải/chụp màn hình " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static BufferedImage toBufferedImage(javafx.scene.image.Image fxImage) {
        int width = (int) fxImage.getWidth();
        int height = (int) fxImage.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        javafx.scene.image.PixelReader pixelReader = fxImage.getPixelReader();
        int[] buffer = new int[width * height];
        pixelReader.getPixels(0, 0, width, height,
                javafx.scene.image.PixelFormat.getIntArgbInstance(),
                buffer, 0, width);
        image.setRGB(0, 0, width, height, buffer, 0, width);
        return image;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
