package com.auction.ui;

import com.auction.enums.AuctionStatus;
import javafx.scene.control.Label;

public final class BadgeFactory {
    private BadgeFactory() {
    }

    public static Label create(AuctionStatus status) {
        Label label = new Label(statusText(status));
        applyStatus(label, status);
        return label;
    }

    public static void applyStatus(Label label, AuctionStatus status) {
        label.setText(statusText(status));
        label.getStyleClass().removeAll("badge", "badge-open", "badge-running",
                "badge-finished", "badge-paid", "badge-canceled");
        label.getStyleClass().add("badge");
        if (status == null) {
            label.getStyleClass().add("badge-open");
            return;
        }
        label.getStyleClass().add(switch (status) {
            case OPEN -> "badge-open";
            case RUNNING -> "badge-running";
            case FINISHED -> "badge-finished";
            case PAID -> "badge-paid";
            case CANCELED -> "badge-canceled";
        });
    }

    public static String statusText(AuctionStatus status) {
        if (status == null) return "";
        return switch (status) {
            case OPEN -> "Chưa bắt đầu";
            case RUNNING -> "Đang diễn ra";
            case FINISHED -> "Đã kết thúc";
            case PAID -> "Đã thanh toán";
            case CANCELED -> "Đã hủy";
        };
    }
}
