package com.auction.util;

import com.auction.client.AuctionClientGateway;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class ItemImageMigrationHelper {
    private ItemImageMigrationHelper() {
    }

    public static void migrateLocalImagePaths(List<Auction> auctions, AuctionClientGateway gateway) {
        if (auctions == null || gateway == null) {
            return;
        }
        for (Auction auction : auctions) {
            migrateAuction(auction, gateway);
        }
    }

    private static void migrateAuction(Auction auction, AuctionClientGateway gateway) {
        if (auction == null || auction.getItem() == null) {
            return;
        }
        Item item = auction.getItem();
        List<String> imageReferences = item.getImagePaths();
        if (imageReferences.isEmpty()) {
            return;
        }

        boolean changed = false;
        List<String> migratedReferences = new ArrayList<>();
        for (String reference : imageReferences) {
            String migrated = migrateReference(reference);
            migratedReferences.add(migrated);
            changed = changed || !migrated.equals(reference);
        }

        if (!changed) {
            return;
        }

        String migratedImagePath = String.join(";", migratedReferences);
        try {
            gateway.updateItemImagePath(item.getId(), migratedImagePath);
            item.setImagePath(migratedImagePath);
        } catch (RuntimeException e) {
            // Migration is best-effort; old local-path records should not block the UI.
        }
    }

    private static String migrateReference(String reference) {
        if (reference == null || reference.isBlank() || ImageStorage.isInlineImage(reference)) {
            return reference;
        }
        try {
            if (ImageStorage.exists(reference)) {
                return ImageStorage.toPortableReference(Paths.get(reference));
            }
        } catch (IOException | InvalidPathException e) {
            return reference;
        }
        return reference;
    }
}
