package com.auction.ui;

import javafx.scene.control.Label;

public final class IconFactory {
    private IconFactory() {
    }

    public static Label create(String name) {
        Label label = new Label(labelFor(name));
        label.getStyleClass().add("nav-icon");
        return label;
    }

    private static String labelFor(String name) {
        if (name == null) {
            return "";
        }
        return switch (name) {
            case "auction", "gavel" -> "AU";
            case "account", "profile" -> "AC";
            case "logout" -> "LO";
            case "dashboard" -> "DB";
            case "users" -> "US";
            case "search" -> "SE";
            case "bell", "notification" -> "NO";
            case "refresh" -> "RF";
            case "upload" -> "UP";
            case "delete" -> "DL";
            case "edit" -> "ED";
            default -> name.substring(0, Math.min(2, name.length())).toUpperCase();
        };
    }
}
