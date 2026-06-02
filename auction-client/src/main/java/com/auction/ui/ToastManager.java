package com.auction.ui;

import com.auction.util.UiEffects;
import javafx.scene.layout.StackPane;

public final class ToastManager {
    private static final ToastManager INSTANCE = new ToastManager();

    private ToastManager() {
    }

    public static ToastManager getInstance() {
        return INSTANCE;
    }

    public void info(StackPane root, String message) {
        show(root, message, UiEffects.ToastType.INFO, 1800);
    }

    public void success(StackPane root, String message) {
        show(root, message, UiEffects.ToastType.SUCCESS, 1800);
    }

    public void error(StackPane root, String message) {
        show(root, message, UiEffects.ToastType.ERROR, 2400);
    }

    public void show(StackPane root, String message, UiEffects.ToastType type, long durationMs) {
        if (root == null || message == null || message.isBlank()) {
            return;
        }
        UiEffects.showToast(root, message, type, durationMs);
    }
}
