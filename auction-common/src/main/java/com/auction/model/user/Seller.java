package com.auction.model.user;

public class Seller extends User {
    private String storeName;
    private String storeDescription;

    public Seller() {}

    public Seller(String id, String username, String email) {
        super(id, username, email);
    }

    public Seller(String id, String username, String email, String storeName, String storeDescription) {
        super(id, username, email);
        this.storeName = storeName;
        this.storeDescription = storeDescription;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreDescription() {
        return storeDescription;
    }

    public void setStoreDescription(String storeDescription) {
        this.storeDescription = storeDescription;
    }
}
