package com.auction.factory;

import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
public class ItemFactory {
    public static Item createItem(String type, String id, String name, String description, double startingPrice) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Loại Item không được để trống!");
        }
        if (type.equalsIgnoreCase("Art")) {
            return new Art(id, name, description, startingPrice);
        }
        else if (type.equalsIgnoreCase("Electronics")) {
            return new Electronics(id, name, description, startingPrice);
        }
        else if (type.equalsIgnoreCase("Vehicle")) {
            return new Vehicle(id, name, description, startingPrice);
        }
        else {
            throw new IllegalArgumentException("Loại Item không hỗ trợ trong hệ thống: " + type);
        }
    }
}