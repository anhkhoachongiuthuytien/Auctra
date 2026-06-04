package com.auction.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public final class ItemImageHelper {
    private ItemImageHelper() {
    }

    public static Image load(String reference, double requestedWidth, double requestedHeight) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        try {
            if (ImageStorage.isInlineImage(reference)) {
                byte[] bytes = ImageStorage.decodeInlineImage(reference);
                return new Image(new ByteArrayInputStream(bytes), requestedWidth, requestedHeight, true, true);
            }
            if (ImageStorage.exists(reference)) {
                return new Image(new File(reference).toURI().toString(), requestedWidth, requestedHeight, true, true);
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
        return null;
    }

    public static ImageView createImageView(String reference, double fitWidth, double fitHeight) {
        Image image = load(reference, fitWidth, fitHeight);
        if (image == null || image.isError()) {
            return null;
        }
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(fitWidth);
        imageView.setFitHeight(fitHeight);
        imageView.setPreserveRatio(true);
        return imageView;
    }
}
