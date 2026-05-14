package com.auction.protocol;

import java.io.Serializable;

/**
 * DTO đại diện cho một lượt đặt giá khi truyền qua mạng.
 */
public class BidDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String bidderId;
    private String bidderName;
    private double amount;
    private String bidTime;

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getBidTime() {
        return bidTime;
    }

    public void setBidTime(String bidTime) {
        this.bidTime = bidTime;
    }
}
