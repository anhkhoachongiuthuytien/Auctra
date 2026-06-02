package com.auction.ui;

import javafx.scene.layout.VBox;

public class SkeletonPane extends VBox {
    public SkeletonPane() {
        this(5);
    }

    public SkeletonPane(int rows) {
        setSpacing(10);
        getStyleClass().add("skeleton-pane");
        for (int i = 0; i < rows; i++) {
            javafx.scene.layout.Region row = new javafx.scene.layout.Region();
            row.getStyleClass().add("skeleton-row");
            row.setMaxWidth(Double.MAX_VALUE);
            getChildren().add(row);
        }
    }
}
