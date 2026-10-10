package com.auction.util;

import com.auction.app.*;
import com.auction.client.*;
import com.auction.controller.*;
import com.auction.model.auction.Auction;
import com.auction.model.user.*;
import com.auction.server.*;
import com.auction.ui.ResponsiveManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;

public final class ScreenshotGenerator {
    public static Path defaultOutput() {
        return Files.exists(Path.of("src", "main", "resources", "fxml"))
                ? Path.of("target", "ui-screenshots")
                : Path.of("auction-client", "target", "ui-screenshots");
    }
    public static void main(String[] args) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        Platform.startup(() -> {
            try { generate(args.length == 0 ? defaultOutput() : Path.of(args[0])); } catch (Throwable t) { t.printStackTrace(); }
            finally { done.countDown(); }
        });
        done.await(60, TimeUnit.SECONDS);
        Platform.exit();
    }
    public static void generate(Path dir) throws Exception {
        Files.createDirectories(dir);
        Path db = Files.createTempFile("auctra-ui-", ".db");
        LocalAuctionClientGateway gateway = new LocalAuctionClientGateway(new AuctionServerFacade(new ServerContext("jdbc:sqlite:" + db)));
        AppContext app = new AppContext(gateway);
        Seller seller = (Seller) gateway.login("seller@auction.local", "demo12345");
        Auction auction = gateway.createAuctionForSeller(seller, "Electronics", "Máy ảnh Fujifilm X100VI", "Máy ảnh nhỏ gọn, cảm biến 40 MP. Kèm hộp, pin và dây đeo. Người bán cung cấp tình trạng và phụ kiện trong mô tả.", 1200, null, 60);
        gateway.startAuction(auction.getId());
        Bidder bidder = (Bidder) gateway.login("bidder@auction.local", "demo12345");
        gateway.placeBid(auction.getId(), bidder, 1250);
        String id = auction.getId();
        Stage stage = new Stage();
        for (String name : new String[]{"login", "register", "forgot-password", "auction-list", "auction-detail", "seller", "admin", "profile"}) {
            ClientEventManager.clearListeners();
            User actor = switch (name) {
                case "seller" -> gateway.login("seller@auction.local", "demo12345");
                case "admin" -> gateway.login("admin@auction.local", "demo12345");
                default -> gateway.login("bidder@auction.local", "demo12345");
            };
            FXMLLoader loader = new FXMLLoader(ScreenshotGenerator.class.getResource("/fxml/" + name + "-view.fxml"));
            Parent root = loader.load();
            SceneNavigator nav = new SceneNavigator(stage, app);
            Object controller = loader.getController();
            if (controller instanceof AuthController c) c.init(app, nav);
            if (controller instanceof RegisterController c) c.init(app, nav);
            if (controller instanceof ForgotPasswordController c) c.init(app, nav);
            if (controller instanceof AuctionController c) c.init(app, nav, actor);
            if (controller instanceof AuctionDetailController c) c.init(app, nav, actor, gateway.listAuctions().stream().filter(a -> a.getId().equals(id)).findFirst().orElseThrow());
            if (controller instanceof SellerController c) c.init(app, nav, (Seller) actor);
            if (controller instanceof AdminController c) c.init(app, nav, (Admin) actor);
            if (controller instanceof ProfileController c) c.init(app, nav, actor);
            for (int width : new int[]{1180, 800}) {
                for (String theme : new String[]{"light", "dark"}) {
                    root.getStyleClass().removeAll("light", "dark"); root.getStyleClass().add(theme);
                    Scene scene = new Scene(root, width, width == 800 ? 600 : 760);
                    scene.getStylesheets().add(ScreenshotGenerator.class.getResource("/css/app.css").toExternalForm());
                    stage.setScene(scene);
                    root.resize(width, width == 800 ? 600 : 760);
                    ResponsiveManager.apply(scene);
                    root.applyCss(); root.layout();
                    WritableImage image = root.snapshot(null, null);
                    BufferedImage png = new BufferedImage((int)image.getWidth(), (int)image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    for (int y=0;y<png.getHeight();y++) for(int x=0;x<png.getWidth();x++) png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
                    ImageIO.write(png, "png", dir.resolve(name + "-" + width + "-" + theme + ".png").toFile());
                    System.out.println(name + " " + width + " " + theme);
                    stage.setScene(null);
                    scene.setRoot(new javafx.scene.layout.StackPane());
                }
            }
        }
        stage.close(); gateway.close(); ClientEventManager.clearListeners();
        Files.deleteIfExists(db);
    }
}
