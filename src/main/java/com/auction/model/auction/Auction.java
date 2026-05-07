package com.auction.model.auction;

import com.auction.enums.AuctionStatus;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionException;
import com.auction.exception.InvalidBidException;
import com.auction.model.base.Entity;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.observer.BidEvent;
import com.auction.observer.BidObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    private final Item item;
    private final Seller seller;
    private double currentPrice;
    private AuctionStatus status;
    private final List<BidTransaction> bids;
    private Bidder winner;
    private final List<BidObserver> bidObservers;
    private final ReentrantLock stateLock;

    public Auction() {
        this(null, null, null);
    }

    public Auction(String id, Item item, Seller seller) {
        super(id);
        this.item = item;
        this.seller = seller;
        this.currentPrice = item == null ? 0.0 : item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
        this.bids = new ArrayList<>();
        this.winner = null;
        this.bidObservers = new CopyOnWriteArrayList<>();
        this.stateLock = new ReentrantLock();
    }

    public void start() {
        stateLock.lock();
        try {
            if (status == AuctionStatus.OPEN) {
                status = AuctionStatus.RUNNING;
                return;
            }
            throw new AuctionException("Cannot start auction from status: " + status);
        } finally {
            stateLock.unlock();
        }
    }

    public void finish() {
        stateLock.lock();
        try {
            if (status == AuctionStatus.RUNNING) {
                status = AuctionStatus.FINISHED;
                return;
            }
            throw new AuctionException("Cannot finish auction from status: " + status);
        } finally {
            stateLock.unlock();
        }
    }

    public void cancel() {
        stateLock.lock();
        try {
            if (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING || status == AuctionStatus.FINISHED) {
                status = AuctionStatus.CANCELED;
                return;
            }
            throw new AuctionException("Cannot cancel auction from status: " + status);
        } finally {
            stateLock.unlock();
        }
    }

    public void markPaid() {
        stateLock.lock();
        try {
            if (status == AuctionStatus.FINISHED) {
                status = AuctionStatus.PAID;
                return;
            }
            throw new AuctionException("Cannot mark auction as paid from status: " + status);
        } finally {
            stateLock.unlock();
        }
    }

    public void addObserver(BidObserver observer) {
        if (observer != null) {
            bidObservers.add(observer);
        }
    }

    public void removeObserver(BidObserver observer) {
        bidObservers.remove(observer);
    }

    public void addBid(BidTransaction bid) {
        stateLock.lock();
        try {
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Auction is not open for bidding");
            }
            if (bid == null) {
                throw new InvalidBidException("Bid cannot be null");
            }
            if (bid.getAmount() <= currentPrice) {
                throw new InvalidBidException("Bid amount must be higher than current price");
            }

            bids.add(bid);
            currentPrice = bid.getAmount();
            winner = bid.getBidder();
        } finally {
            stateLock.unlock();
        }

        notifyBidPlaced(bid);
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
        return status == AuctionStatus.RUNNING;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public List<BidTransaction> getBids() {
        return bids;
    }

    public Bidder getWinner() {
        return winner;
    }

    public void setWinner(Bidder winner) {
        this.winner = winner;
    }

    private void notifyBidPlaced(BidTransaction bid) {
        BidEvent event = new BidEvent(this, bid, currentPrice, winner);
        for (BidObserver observer : bidObservers) {
            observer.onBidPlaced(event);
        }
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
