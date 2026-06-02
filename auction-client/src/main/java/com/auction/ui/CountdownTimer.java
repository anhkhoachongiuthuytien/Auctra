package com.auction.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;

public final class CountdownTimer {
    private final Label label;
    private final LocalDateTime endTime;
    private Timeline timeline;

    public CountdownTimer(Label label, LocalDateTime endTime) {
        this.label = label;
        this.endTime = endTime;
        label.getStyleClass().add("countdown-timer");
    }

    public void start() {
        stop();
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> update()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        update();
        timeline.play();
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private void update() {
        long seconds = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
        label.getStyleClass().removeAll("countdown-timer-warning", "countdown-timer-danger");
        if (seconds <= 0) {
            label.setText("Đã kết thúc");
            label.getStyleClass().add("countdown-timer-danger");
            stop();
            return;
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        label.setText(String.format("%02d:%02d:%02d", hours, minutes, secs));
        if (seconds < 60) {
            label.getStyleClass().add("countdown-timer-danger");
        } else if (seconds < 300) {
            label.getStyleClass().add("countdown-timer-warning");
        }
    }
}
