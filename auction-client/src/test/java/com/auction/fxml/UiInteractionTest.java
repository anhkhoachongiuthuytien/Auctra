package com.auction.fxml;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.client.ClientEventManager;
import com.auction.client.LocalAuctionClientGateway;
import com.auction.controller.*;
import com.auction.enums.AuctionStatus;
import com.auction.model.auction.Auction;
import com.auction.model.user.*;
import com.auction.server.AuctionServerFacade;
import com.auction.server.ServerContext;
import com.auction.ui.ResponsiveManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class UiInteractionTest {
    @TempDir Path directory;

    @BeforeAll static void startToolkit() throws Exception {
        FxTestToolkit.start();
    }

    @Test void allScreensFitDesktopAndCompactWindowsInBothThemes() throws Exception {
        onFx(() -> {
            try (Fixture fixture = new Fixture(directory.resolve("layout.db"))) {
                for (String name : new String[]{"login", "register", "forgot-password", "auction-list", "auction-detail", "seller", "admin", "profile"}) {
                    Parent root = fixture.load(name).getRoot();
                    for (int width : new int[]{1180, 800}) {
                        for (String theme : new String[]{"light", "dark"}) {
                            root.getStyleClass().removeAll("light", "dark");
                            root.getStyleClass().add(theme);
                            Scene scene = render(root, width, width == 800 ? 600 : 760);
                            assertTrue(root.minWidth(-1) <= width, name + " forces a wider window");
                            ScrollPane scroll = (ScrollPane) root.lookup(name.contains("login") || name.equals("register") || name.equals("forgot-password") ? ".auth-scroll" : ".page-scroll");
                            assertNotNull(scroll, name);
                            assertTrue(scroll.getContent().getLayoutBounds().getWidth() <= scroll.getViewportBounds().getWidth() + 1, name + " requires horizontal scrolling");
                            if (name.equals("auction-detail")) {
                                Button place = (Button) root.lookup("#placeBidButton");
                                double bottom = place.localToScene(place.getBoundsInLocal()).getMaxY();
                                assertTrue(bottom <= scene.getHeight(), "Bid action ends at " + bottom + ", viewport " + scene.getHeight() + " at " + width);
                            }
                            scene.setRoot(new javafx.scene.layout.StackPane());
                        }
                    }
                }
            }
        });
    }

    @Test void manualAndAutomaticBidButtonsReachTheRealService() throws Exception {
        onFx(() -> {
            try (Fixture fixture = new Fixture(directory.resolve("bids.db"))) {
                Parent root = fixture.load("auction-detail").getRoot();
                Scene scene = render(root, 1180, 760);
                text(root, "bidAmountField", "1300");
                ((Button) root.lookup("#placeBidButton")).fire();
                assertEquals(1300, fixture.auction().getCurrentPrice());
                text(root, "autoBidMaxField", "1500");
                text(root, "autoBidIncrementField", "10");
                ((Button) root.lookup("#toggleAutoBidButton")).fire();
                assertNotNull(fixture.gateway.getAutoBid(fixture.id, fixture.bidder.getId()));
                assertEquals(1300, fixture.auction().getCurrentPrice(), "Leader must not bid against itself");
                ((Button) root.lookup("#toggleAutoBidButton")).fire();
                assertNull(fixture.gateway.getAutoBid(fixture.id, fixture.bidder.getId()));
                scene.setRoot(new javafx.scene.layout.StackPane());
            }
        });
    }

    @Test void sellerRejectsBadDurationAndPreservesMultilineDescription() throws Exception {
        onFx(() -> {
            try (Fixture fixture = new Fixture(directory.resolve("seller.db"))) {
                Parent root = fixture.load("seller").getRoot();
                Scene scene = render(root, 800, 600);
                TitledPane disclosure = (TitledPane) root.lookup(".create-auction-disclosure");
                disclosure.setExpanded(true);
                text(root, "itemNameField", "Máy ảnh kiểm thử");
                text(root, "startingPriceField", "100");
                text(root, "durationField", "không hợp lệ");
                ((TextArea) root.lookup("#itemDescriptionField")).setText("Dòng một\nDòng hai");
                Button create = root.lookupAll(".button-primary").stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b -> b.getText().equals("Tạo phiên đấu giá")).findFirst().orElseThrow();
                int before = fixture.gateway.listAuctions().size();
                create.fire();
                assertEquals(before, fixture.gateway.listAuctions().size());
                text(root, "durationField", "45");
                create.fire();
                Auction created = fixture.gateway.listAuctions().stream().filter(a -> a.getItem().getName().equals("Máy ảnh kiểm thử")).findFirst().orElseThrow();
                assertEquals(45, created.getDurationMinutes());
                assertEquals("Dòng một\nDòng hai", created.getItem().getDescription());
                scene.setRoot(new javafx.scene.layout.StackPane());
            }
        });
    }

    @Test void adminChartUsesStoredAuctionCounts() throws Exception {
        onFx(() -> {
            try (Fixture fixture = new Fixture(directory.resolve("admin.db"))) {
                FXMLLoader loader = fixture.load("admin");
                BarChart<?, ?> chart = (BarChart<?, ?>) loader.getNamespace().get("opsChart");
                assertEquals(AuctionStatus.values().length, chart.getData().get(0).getData().size());
                long count = chart.getData().get(0).getData().stream().mapToLong(d -> ((Number) d.getYValue()).longValue()).sum();
                assertEquals(fixture.gateway.listAuctions().size(), count);
            }
        });
    }

    private static Scene render(Parent root, int width, int height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(UiInteractionTest.class.getResource("/css/app.css").toExternalForm());
        ResponsiveManager.apply(scene);
        root.resize(width, height); root.applyCss(); root.layout();
        return scene;
    }
    private static void text(Parent root, String id, String value) { ((TextField) root.lookup("#" + id)).setText(value); }
    private static void onFx(ThrowingRunnable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch finished = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { action.run(); } catch (Throwable error) { failure.set(error); } finally { finished.countDown(); }
        });
        assertTrue(finished.await(30, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError("UI regression", failure.get());
    }
    @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }

    private static class Fixture implements AutoCloseable {
        final LocalAuctionClientGateway gateway;
        final AppContext app;
        final Stage stage = new Stage();
        final Bidder bidder;
        final String id;
        Fixture(Path database) {
            gateway = new LocalAuctionClientGateway(new AuctionServerFacade(new ServerContext("jdbc:sqlite:" + database)));
            app = new AppContext(gateway);
            Seller seller = (Seller) gateway.login("seller@auction.local", "demo12345");
            Auction auction = gateway.createAuctionForSeller(seller, "Electronics", "Máy ảnh Fujifilm X100VI", "Tình trạng tốt", 1200, null, 60);
            id = auction.getId(); gateway.startAuction(id);
            bidder = (Bidder) gateway.login("bidder@auction.local", "demo12345");
        }
        Auction auction() { return gateway.listAuctions().stream().filter(a -> a.getId().equals(id)).findFirst().orElseThrow(); }
        FXMLLoader load(String name) throws Exception {
            ClientEventManager.clearListeners();
            User actor = gateway.login(name.equals("seller") ? "seller@auction.local" : name.equals("admin") ? "admin@auction.local" : "bidder@auction.local", "demo12345");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + name + "-view.fxml"));
            loader.load(); SceneNavigator nav = new SceneNavigator(stage, app);
            Object c = loader.getController();
            if (c instanceof AuthController v) v.init(app, nav);
            if (c instanceof RegisterController v) v.init(app, nav);
            if (c instanceof ForgotPasswordController v) v.init(app, nav);
            if (c instanceof AuctionController v) v.init(app, nav, actor);
            if (c instanceof AuctionDetailController v) v.init(app, nav, actor, auction());
            if (c instanceof SellerController v) v.init(app, nav, (Seller) actor);
            if (c instanceof AdminController v) v.init(app, nav, (Admin) actor);
            if (c instanceof ProfileController v) v.init(app, nav, actor);
            return loader;
        }
        @Override public void close() { ClientEventManager.clearListeners(); stage.close(); gateway.close(); }
    }
}
