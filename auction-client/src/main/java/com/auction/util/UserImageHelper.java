package com.auction.util;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class UserImageHelper {
    private UserImageHelper() {}

    public static File getAvatarFile(String userId) {
        if (userId == null || userId.isBlank()) return null;
        return Paths.get(System.getProperty("user.home"), ".auctionx", "avatars", userId + ".png").toFile();
    }

    public static void setupAvatar(Label initialsLabel, String userId) {
        setupAvatar(initialsLabel, userId, null);
    }

    public static void setupAvatar(Label initialsLabel, String userId, String avatarPath) {
        if (initialsLabel == null) return;
        javafx.scene.Parent parent = initialsLabel.getParent();
        if (parent instanceof StackPane container) {
            // Remove any existing ImageView from previous configurations
            container.getChildren().removeIf(node -> node instanceof ImageView);

            File file = null;
            if (avatarPath != null && !avatarPath.isBlank()) {
                file = new File(avatarPath);
            }
            if (file == null || !file.exists()) {
                file = getAvatarFile(userId);
            }

            if (file != null && file.exists()) {
                try {
                    ImageView imgView = new ImageView(new Image(file.toURI().toString(), true));
                    
                    // Determine dimensions based on the container size or style classes
                    double width = container.getPrefWidth() > 0 ? container.getPrefWidth() : 42;
                    double height = container.getPrefHeight() > 0 ? container.getPrefHeight() : 42;
                    if (container.getStyleClass().contains("profile-avatar-large")) {
                        width = 96;
                        height = 96;
                    }
                    
                    imgView.setFitWidth(width);
                    imgView.setFitHeight(height);
                    imgView.setPreserveRatio(false);

                    // Round clip
                    Circle clip = new Circle(width / 2, height / 2, width / 2);
                    imgView.setClip(clip);

                    initialsLabel.setVisible(false);
                    container.getChildren().add(imgView);
                } catch (Exception e) {
                    initialsLabel.setVisible(true);
                }
            } else {
                initialsLabel.setVisible(true);
            }
        }
    }
}
