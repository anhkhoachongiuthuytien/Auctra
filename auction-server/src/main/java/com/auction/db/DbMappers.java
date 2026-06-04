package com.auction.db;

import com.auction.enums.ItemType;
import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
import com.auction.model.item.Other;
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
        return createUser(role, id, username, email, null, null, null, null, null, null);
    }

    public static User createUser(String role, String id, String username, String email,
                                  String shippingAddress, String phoneNumber,
                                  String storeName, String storeDescription,
                                  String department) {
        return createUser(role, id, username, email, shippingAddress, phoneNumber, storeName, storeDescription, department, null);
    }

    public static User createUser(String role, String id, String username, String email,
                                  String shippingAddress, String phoneNumber,
                                  String storeName, String storeDescription,
                                  String department, String avatarPath) {
        User user = switch (role.toUpperCase()) {
            case "SELLER" -> new Seller(id, username, email, storeName, storeDescription);
            case "BIDDER" -> new Bidder(id, username, email, shippingAddress, phoneNumber);
            case "ADMIN" -> new Admin(id, username, email, department);
            default -> throw new IllegalArgumentException("Unsupported role: " + role);
        };
        user.setAvatarPath(avatarPath);
        return user;
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
        if (item instanceof Other) {
            return ItemType.OTHER.name();
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
            case OTHER -> new Other(id, name, description, startingPrice);
            default -> new Other(id, name, description, startingPrice); // Dòng này xử lý FASHION và các loại mới sau này
        };
        item.setImagePath(imagePath);
        return item;
    }
}