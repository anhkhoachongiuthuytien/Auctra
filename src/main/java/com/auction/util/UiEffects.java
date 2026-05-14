package com.auction.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Tập hợp các hiệu ứng UI tái sử dụng: toast, loading overlay, confirm dialog, fade transition.
 * Tất cả đều hoạt động trên một StackPane root để đè lên content chính.
 */
public final class UiEffects {

    private UiEffects() { }

    public enum ToastType { INFO, SUCCESS, ERROR }

    /**
     * Hiện một toast ở góc trên phải của StackPane root, tự fade out sau durationMs.
     */
    public static void showToast(StackPane root, String message, ToastType type, long durationMs) {
        Label toast = new Label(message);
        toast.getStyleClass().add("toast");
        switch (type) {
            case SUCCESS -> toast.getStyleClass().add("toast-success");
            case ERROR   -> toast.getStyleClass().add("toast-error");
            default      -> toast.getStyleClass().add("toast-info");
        }
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        StackPane.setMargin(toast, new javafx.geometry.Insets(24, 28, 0, 0));

        root.getChildren().add(toast);

        // Fade in → pause → fade out → remove
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.millis(durationMs));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(fadeIn, pause, fadeOut);
        seq.setOnFinished(e -> root.getChildren().remove(toast));
        seq.play();
    }

    /**
     * Hiện loading overlay đè lên StackPane root. Trả về Runnable để gỡ overlay.
     */
    public static Runnable showLoadingOverlay(StackPane root) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("loading-overlay");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.getStyleClass().add("spinner");
        spinner.setPrefSize(64, 64);

        overlay.getChildren().add(spinner);
        root.getChildren().add(overlay);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        return () -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), overlay);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> root.getChildren().remove(overlay));
            fadeOut.play();
        };
    }

    /**
     * Hiện confirm dialog với backdrop mờ. Gọi onConfirm.accept(true/false) theo lựa chọn.
     */
    public static void showConfirmDialog(StackPane root,
                                         String title,
                                         String message,
                                         String confirmText,
                                         boolean dangerConfirm,
                                         Consumer<Boolean> onResult) {
        StackPane backdrop = new StackPane();
        backdrop.getStyleClass().add("modal-backdrop");

        VBox card = new VBox(16);
        card.getStyleClass().add("modal-card");
        card.setAlignment(Pos.TOP_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("modal-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("modal-message");
        messageLabel.setWrapText(true);

        Button cancelBtn = new Button("Hủy");
        cancelBtn.getStyleClass().add("button-ghost");

        Button confirmBtn = new Button(confirmText);
        confirmBtn.getStyleClass().add(dangerConfirm ? "button-danger" : "button-primary");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox buttonRow = new HBox(10, spacer, cancelBtn, confirmBtn);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(titleLabel, messageLabel, buttonRow);
        backdrop.getChildren().add(card);
        root.getChildren().add(backdrop);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), backdrop);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        Runnable close = () -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), backdrop);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> root.getChildren().remove(backdrop));
            fadeOut.play();
        };

        cancelBtn.setOnAction(e -> {
            close.run();
            onResult.accept(false);
        });
        confirmBtn.setOnAction(e -> {
            close.run();
            onResult.accept(true);
        });
        backdrop.setOnMouseClicked(e -> {
            if (e.getTarget() == backdrop) {
                close.run();
                onResult.accept(false);
            }
        });
    }

    /**
     * Tự động xoá nội dung của label sau `delayMs` ms. Dùng để "tự dọn" status message.
     */
    public static void autoHideLabel(javafx.scene.control.Label label, long delayMs) {
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(e -> {
            label.setText("");
            label.getStyleClass().removeAll("error-label", "success-label");
        });
        pause.play();
    }

    /**
     * Chạy một action "giả lập async" với spinner overlay (cho hiệu ứng loading).
     */
    public static void runWithLoading(StackPane root, long minShowMs, Runnable action, Runnable onDone) {
        Runnable hide = showLoadingOverlay(root);
        PauseTransition pause = new PauseTransition(Duration.millis(minShowMs));
        pause.setOnFinished(e -> {
            action.run();
            hide.run();
            if (onDone != null) Platform.runLater(onDone);
        });
        pause.play();
    }
}

