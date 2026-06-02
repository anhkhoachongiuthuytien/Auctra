package com.auction.model.user;

public class Bidder extends User {
    private String shippingAddress;
    private String phoneNumber;

    public Bidder() {}

    public Bidder(String id, String username, String email) {
        super(id, username, email);
    }

    public Bidder(String id, String username, String email, String shippingAddress, String phoneNumber) {
        super(id, username, email);
        this.shippingAddress = shippingAddress;
        this.phoneNumber = phoneNumber;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
