package com.auction.factory;

import com.auction.enums.ItemType;
import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;

public class ItemFactory {
    public static Item createItem(ItemType type, String id, String name, String description, double startingPrice) {
        if (type == null) {
            throw new IllegalArgumentException("Item type must not be null");
        }

        switch (type) {
            case ART:
                return new Art(id, name, description, startingPrice);
            case ELECTRONICS:
                return new Electronics(id, name, description, startingPrice);
            case VEHICLE:
                return new Vehicle(id, name, description, startingPrice);
            default:
                throw new IllegalArgumentException("Unsupported item type: " + type);
        }
    }

    public static Item createItem(String type, String id, String name, String description, double startingPrice) {
        return createItem(ItemType.fromString(type), id, name, description, startingPrice);
    }
}
