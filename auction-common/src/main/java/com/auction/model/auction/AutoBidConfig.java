package com.auction.model.auction;

import java.io.Serializable;

/**
 * Cấu hình tự động đấu giá của một bidder cho một phiên đấu giá cụ thể.
 * Lưu trữ giới hạn tối đa và bước giá tự động.
 */
public class AutoBidConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String auctionId;
    private final String bidderId;
    private final double maxPrice;
    private final double increment;

    public AutoBidConfig(String auctionId, String bidderId, double maxPrice, double increment) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxPrice = maxPrice;
        this.increment = increment;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public double getIncrement() {
        return increment;
    }

    @Override
    public String toString() {
        return "AutoBidConfig{" +
                "auctionId='" + auctionId + '\'' +
                ", bidderId='" + bidderId + '\'' +
                ", maxPrice=" + maxPrice +
                ", increment=" + increment +
                '}';
    }
}
