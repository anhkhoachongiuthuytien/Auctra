package com.auction.fxml;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class FxmlLoadTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("JavaFX toolkit did not start");
        }
    }

    @Test
    void dashboardFxmlFilesLoad() throws Exception {
        load("/fxml/admin-view.fxml");
        load("/fxml/auction-list-view.fxml");
        load("/fxml/seller-view.fxml");
        load("/fxml/profile-view.fxml");
    }

    private static void load(String path) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                URL url = FxmlLoadTest.class.getResource(path);
                assertNotNull(url, path + " not found");
                FXMLLoader loader = new FXMLLoader(url);
                loader.setCharset(StandardCharsets.UTF_8);
                loader.load();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail(path + " load timed out");
        }
        Throwable error = failure.get();
        if (error != null) {
            throw new AssertionError(path + " failed to load", error);
        }
    }
}
