package com.auction.model.auction;

import com.auction.model.base.Entity;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;

import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private Item item;
    private Seller seller;
    private double currentPrice;
    private boolean open;
    private List<BidTransaction> bids;
    private Bidder winner;

    public Auction() {
        this.bids = new ArrayList<>();
        this.open = false;
        this.winner = null;
    }

    public Auction(String id, Item item, Seller seller) {
        super(id);
        this.item = item;
        this.seller = seller;
        this.currentPrice = item.getStartingPrice();
        this.open = false;
        this.bids = new ArrayList<>();
        this.winner = null;
    }

    public void openAuction() {
        this.open = true;
    }

    public void closeAuction() {
        this.open = false;
    }

    public void addBid(BidTransaction bid) {
        if (bid != null) {
            this.bids.add(bid);
            this.currentPrice = bid.getAmount();
            this.winner = bid.getBidder();
        }
    }

    public Item getItem() {
        return item;
    }


    public Seller getSeller() {
        return seller;
    }


    public double getCurrentPrice() {
        return currentPrice;
    }


    public boolean isOpen() {
        return open;
    }


    public List<BidTransaction> getBids() {
        return bids;
    }


    public Bidder getWinner() {
        return this.winner;
    }

    public void setWinner(Bidder winner) {
        this.winner = winner;
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id='" + getId() + '\'' +
                ", item=" + item +
                ", seller=" + seller +
                ", currentPrice=" + currentPrice +
                ", open=" + open +
                ", bids=" + bids +
                ", winner=" + winner +
                '}';
    }
}