package com.auction.ui;

import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;

public final class FloatingFieldHelper {
    private FloatingFieldHelper() {
    }

    public static void bindFilledState(TextInputControl input, Labeled label) {
        if (input == null || label == null) {
            return;
        }
        Runnable update = () -> {
            label.getStyleClass().removeAll("field-label-filled", "field-label-empty");
            label.getStyleClass().add(input.getText() == null || input.getText().isBlank()
                    ? "field-label-empty"
                    : "field-label-filled");
        };
        input.textProperty().addListener((obs, oldValue, newValue) -> update.run());
        update.run();
    }
}
