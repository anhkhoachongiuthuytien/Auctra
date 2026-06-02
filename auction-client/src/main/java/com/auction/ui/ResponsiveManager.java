package com.auction.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;

public final class ResponsiveManager {
    private static final double COLLAPSE_WIDTH = 900;
    private static final double SIDEBAR_WIDTH = 240;
    private static final double SIDEBAR_COLLAPSED_WIDTH = 72;

    private ResponsiveManager() {
    }

    public static void attach(Scene scene) {
        if (scene == null) {
            return;
        }
        Object existing = scene.getProperties().get(ResponsiveManager.class);
        if (existing instanceof ChangeListener<?> oldListener) {
            @SuppressWarnings("unchecked")
            ChangeListener<Number> typed = (ChangeListener<Number>) oldListener;
            scene.widthProperty().removeListener(typed);
        }
        ChangeListener<Number> listener = (obs, oldWidth, newWidth) -> apply(scene);
        scene.getProperties().put(ResponsiveManager.class, listener);
        scene.widthProperty().addListener(listener);
        Platform.runLater(() -> apply(scene));
    }

    public static void apply(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        Node sidebar = scene.getRoot().lookup(".admin-sidebar");
        if (!(sidebar instanceof Region region)) {
            return;
        }
        boolean collapsed = scene.getWidth() < COLLAPSE_WIDTH;
        UIAnimations.resizeRegion(region, collapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_WIDTH);
        region.getStyleClass().remove("collapsed");
        if (collapsed) {
            region.getStyleClass().add("collapsed");
        }
    }
}
