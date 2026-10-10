package com.auction.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;

public final class ResponsiveManager {

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
        apply(scene);
        Platform.runLater(() -> apply(scene));
    }

    public static void apply(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        boolean compact = scene.getWidth() < 1000;
        scene.getRoot().getStyleClass().remove("compact");
        if (compact) scene.getRoot().getStyleClass().add("compact");
        Node sidebar = scene.getRoot().lookup(".admin-sidebar");
        if (sidebar instanceof Region region) {
            // Preserve readable navigation labels at compact widths.
            region.setPrefWidth(compact ? 176 : 192);
            region.setMinWidth(compact ? 176 : 192);
            region.setMaxWidth(compact ? 176 : 192);
        }
        Node brand = scene.getRoot().lookup(".auth-brand-panel");
        if (brand != null) {
            brand.setVisible(!compact);
            brand.setManaged(!compact);
        }
    }
}
