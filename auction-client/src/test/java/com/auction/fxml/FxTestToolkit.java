package com.auction.fxml;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps one toolkit alive for all UI test classes in the Surefire JVM. */
final class FxTestToolkit {
    private FxTestToolkit() { }

    static void start() throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(1);
        Runnable initialize = () -> {
            Platform.setImplicitExit(false);
            ready.countDown();
        };
        try {
            Platform.startup(initialize);
        } catch (IllegalStateException alreadyStarted) {
            // Confirm the existing event thread is alive, rather than treating
            // "already initialized" as proof that the toolkit is still running.
            Platform.runLater(initialize);
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS), "JavaFX event thread did not become ready");
    }
}
