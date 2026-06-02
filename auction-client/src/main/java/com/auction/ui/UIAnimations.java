package com.auction.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public final class UIAnimations {
    private UIAnimations() {
    }

    public static void fadeIn(Node node, double durationMs) {
        FadeTransition transition = new FadeTransition(Duration.millis(durationMs), node);
        transition.setFromValue(0);
        transition.setToValue(1);
        transition.play();
    }

    public static void slideUpFadeIn(Node node) {
        node.setOpacity(0);
        node.setTranslateY(18);
        FadeTransition fade = new FadeTransition(Duration.millis(260), node);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), node);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    public static void slideInFromRight(Node node, double panelWidth) {
        node.setOpacity(0);
        node.setTranslateX(panelWidth + 20);
        FadeTransition fade = new FadeTransition(Duration.millis(180), node);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(280), node);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    public static void slideOutToRight(Node node, double panelWidth, Runnable onFinish) {
        FadeTransition fade = new FadeTransition(Duration.millis(180), node);
        fade.setToValue(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(240), node);
        slide.setToX(panelWidth + 20);
        ParallelTransition transition = new ParallelTransition(fade, slide);
        transition.setOnFinished(event -> {
            if (onFinish != null) {
                onFinish.run();
            }
        });
        transition.play();
    }

    public static void pulsePrice(Label priceLabel) {
        ScaleTransition up = new ScaleTransition(Duration.millis(120), priceLabel);
        up.setToX(1.06);
        up.setToY(1.06);
        ScaleTransition down = new ScaleTransition(Duration.millis(160), priceLabel);
        down.setToX(1);
        down.setToY(1);
        up.setOnFinished(event -> down.play());
        up.play();
    }

    public static void shakeField(TextField field) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(45), field);
        shake.setFromX(0);
        shake.setByX(8);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(event -> field.setTranslateX(0));
        shake.play();
    }

    public static void successBounce(Node node) {
        ScaleTransition bounce = new ScaleTransition(Duration.millis(220), node);
        bounce.setFromX(1);
        bounce.setFromY(1);
        bounce.setToX(1.05);
        bounce.setToY(1.05);
        bounce.setAutoReverse(true);
        bounce.setCycleCount(2);
        bounce.play();
    }

    public static void rotateOnce(Node node) {
        RotateTransition rotate = new RotateTransition(Duration.millis(360), node);
        rotate.setByAngle(360);
        rotate.play();
    }

    public static void resizeRegion(Region region, double targetWidth) {
        region.setMinWidth(targetWidth);
        region.setPrefWidth(targetWidth);
        region.setMaxWidth(targetWidth);
    }
}
