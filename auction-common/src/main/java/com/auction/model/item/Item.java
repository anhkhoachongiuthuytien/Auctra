package com.auction.model.item;
import com.auction.model.base.Entity;

public class Item extends Entity {
    private String name;
    private String description;
    private double startingPrice;
    private String imagePath; // Portable image references separated by semicolon.

    public Item() {}

    public Item(String id, String name, String  description, double  startingPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description=description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public java.util.List<String> getImagePaths() {
        if (imagePath == null || imagePath.isBlank()) {
            return java.util.Collections.emptyList();
        }
        String[] parts = imagePath.split(";");
        java.util.List<String> list = new java.util.ArrayList<>();
        for (String p : parts) {
            if (!p.isBlank()) {
                list.add(p);
            }
        }
        return list;
    }

    public void setImagePaths(java.util.List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            this.imagePath = "";
        } else {
            this.imagePath = String.join(";", paths);
        }
    }
}
