package com.auction.observer;

import com.auction.model.auction.Auction;
import com.auction.model.auction.BidTransaction;
import com.auction.model.user.Bidder;

public class BidEvent {
    private final Auction auction;
    private final BidTransaction bidTransaction;
    private final double latestPrice;
    private final Bidder currentWinner;

    public BidEvent(Auction auction, BidTransaction bidTransaction, double latestPrice, Bidder currentWinner) {
        this.auction = auction;
        this.bidTransaction = bidTransaction;
        this.latestPrice = latestPrice;
        this.currentWinner = currentWinner;
    }

    public Auction getAuction() {
        return auction;
    }

    public BidTransaction getBidTransaction() {
        return bidTransaction;
    }

    public double getLatestPrice() {
        return latestPrice;
    }

    public Bidder getCurrentWinner() {
        return currentWinner;
    }
}
