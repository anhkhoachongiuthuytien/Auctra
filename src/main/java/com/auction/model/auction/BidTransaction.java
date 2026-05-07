package com.auction.model.auction;
import com.auction.model.user.Bidder;

import java.time.LocalDateTime;

public class BidTransaction {
    private Bidder bidder;
    private double amount;
    private LocalDateTime bidTime;

    public BidTransaction(Bidder bidder, double amount) {
        this(bidder, amount, LocalDateTime.now());
    }

    public BidTransaction(Bidder bidder, double amount, LocalDateTime bidTime) {
        this.bidder = bidder;
        this.amount = amount;
        this.bidTime = bidTime;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }
}
