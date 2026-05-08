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
        // Database chỉ lưu chuỗi role, nên trước khi save phải chuyển subtype Java về text tương ứng.
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
        // Khi load từ DB lên, chuỗi role quyết định object Java nào cần được dựng lại.
        return switch (role.toUpperCase()) {
            case "SELLER" -> new Seller(id, username, email);
            case "BIDDER" -> new Bidder(id, username, email);
            case "ADMIN" -> new Admin(id, username, email);
            default -> throw new IllegalArgumentException("Unsupported role: " + role);
        };
    }

    public static String detectItemType(Item item) {
        // Tương tự role, loại item cũng phải được chuyển về text để lưu trong một cột đơn giản.
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
        // Dùng type từ DB để dựng đúng subclass của Item thay vì luôn trả về Item cơ bản.
        return switch (ItemType.fromString(type)) {
            case ART -> new Art(id, name, description, startingPrice);
            case ELECTRONICS -> new Electronics(id, name, description, startingPrice);
            case VEHICLE -> new Vehicle(id, name, description, startingPrice);
        };
    }
}
