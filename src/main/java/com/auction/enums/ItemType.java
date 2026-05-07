package com.auction.enums;

public enum ItemType {
    ART("Art"),
    ELECTRONICS("Electronics"),
    VEHICLE("Vehicle");

    private final String displayName;

    ItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ItemType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Item type must not be empty");
        }

        for (ItemType type : values()) {
            if (type.name().equalsIgnoreCase(value) || type.displayName.equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unsupported item type: " + value);
    }
}
