package com.auction.db;

import com.auction.enums.ItemType;
import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

public final class DbMappers {
    private DbMappers() {
    }

    public static String detectRole(User user) {
        if (user instanceof Seller) {
            return "SELLER";
        }
        if (user instanceof Bidder) {
            return "BIDDER";
        }
        if (user instanceof Admin) {
            return "ADMIN";
        }
        throw new IllegalArgumentException("Unsupported user type: " + user.getClass().getName());
    }

    public static User createUser(String role, String id, String username, String email) {
        return switch (role.toUpperCase()) {
            case "SELLER" -> new Seller(id, username, email);
            case "BIDDER" -> new Bidder(id, username, email);
            case "ADMIN" -> new Admin(id, username, email);
            default -> throw new IllegalArgumentException("Unsupported role: " + role);
        };
    }

    public static String detectItemType(Item item) {
        if (item instanceof Art) {
            return ItemType.ART.name();
        }
        if (item instanceof Electronics) {
            return ItemType.ELECTRONICS.name();
        }
        if (item instanceof Vehicle) {
            return ItemType.VEHICLE.name();
        }
        throw new IllegalArgumentException("Unsupported item type: " + item.getClass().getName());
    }

    public static Item createItem(String type, String id, String name, String description, double startingPrice) {
        return createItem(type, id, name, description, startingPrice, null);
    }

    public static Item createItem(String type, String id, String name, String description,
                                  double startingPrice, String imagePath) {
        Item item = switch (ItemType.fromString(type)) {
            case ART -> new Art(id, name, description, startingPrice);
            case ELECTRONICS -> new Electronics(id, name, description, startingPrice);
            case VEHICLE -> new Vehicle(id, name, description, startingPrice);
        };
        item.setImagePath(imagePath);
        return item;
    }
}
