package com.auction.model.auction;

import com.auction.enums.AuctionStatus;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionException;
import com.auction.exception.InvalidBidException;
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
    private AuctionStatus status;
    private List<BidTransaction> bids;
    private Bidder winner;

    public Auction() {
        this.bids = new ArrayList<>();
        this.status = AuctionStatus.CREATED;
        this.winner = null;
    }

    public Auction(String id, Item item, Seller seller) {
        super(id);
        this.item = item;
        this.seller = seller;
        this.currentPrice = item.getStartingPrice();
        this.status = AuctionStatus.CREATED;
        this.bids = new ArrayList<>();
        this.winner = null;
    }

    public void start() {
        if (this.status == AuctionStatus.CREATED){
            this.status = AuctionStatus.OPEN;
        }
        else{
            throw new AuctionException("Cannot start auction from status: " + status);
        }
    }

    public void finish() {
        if (this.status == AuctionStatus.OPEN){
            this.status = AuctionStatus.FINISHED;
        }
        else{
            throw new AuctionException("Cannot finish auction from status: " + status);
        }
    }

    public void cancel(){
        if (this.status == AuctionStatus.CREATED || this.status == AuctionStatus.OPEN){
            this.status = AuctionStatus.CANCELED;
        }
        else{
            throw new AuctionException("Cannot cancel auction from status: " + status);
        }
    }
    public void markPaid(){
        if (this.status == AuctionStatus.FINISHED){
            this.status = AuctionStatus.PAID;
        }
        else{
            throw new AuctionException("Cannot mark auction as paid from status: " + status);
        }
    }


    public void addBid(BidTransaction bid) {
        if (this.status != AuctionStatus.OPEN) {
            throw new AuctionClosedException("Auction is not open for bidding");
        }
        if (bid == null) {
            throw new InvalidBidException("Bid cannot be null");
        }
        if (bid.getAmount() <= this.currentPrice) {
            throw new InvalidBidException("Bid amount must be higher than current price");
        }

        this.bids.add(bid);
        this.currentPrice = bid.getAmount();
        this.winner = bid.getBidder();
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
        return this.status == AuctionStatus.OPEN;
    }
    public AuctionStatus getStatus(){
        return this.status;
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
                ", status=" + status +
                ", bids=" + bids +
                ", winner=" + winner +
                '}';
    }
}
