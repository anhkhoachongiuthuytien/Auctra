package com.auction.ui;

import javafx.scene.Scene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ThemeManager {
    private static final Path THEME_FILE = Paths.get(System.getProperty("user.home"), ".auctionx", "theme.txt");
    private static Theme currentTheme = Theme.LIGHT;

    private ThemeManager() {
    }

    public enum Theme {
        LIGHT("light"),
        DARK("dark");

        private final String styleClass;

        Theme(String styleClass) {
            this.styleClass = styleClass;
        }

        public String styleClass() {
            return styleClass;
        }
    }

    public static void applySavedTheme(Scene scene) {
        apply(scene, readSavedTheme());
    }

    public static void apply(Scene scene, Theme theme) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        scene.getRoot().getStyleClass().removeAll("light", "dark");
        scene.getRoot().getStyleClass().add(theme.styleClass());
        currentTheme = theme;
        writeSavedTheme(theme);
    }

    private static Theme readSavedTheme() {
        try {
            if (Files.exists(THEME_FILE)) {
                String val = Files.readString(THEME_FILE).trim().toLowerCase();
                if ("dark".equals(val)) {
                    return Theme.DARK;
                }
            }
        } catch (Exception ignored) {}
        return Theme.LIGHT;
    }

    private static void writeSavedTheme(Theme theme) {
        try {
            Files.createDirectories(THEME_FILE.getParent());
            Files.writeString(THEME_FILE, theme.styleClass());
        } catch (Exception ignored) {}
    }

    public static void toggle(Scene scene) {
        Theme next = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
        apply(scene, next);
    }
}
