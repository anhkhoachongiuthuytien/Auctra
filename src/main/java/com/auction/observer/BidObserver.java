package com.auction.observer;

public interface BidObserver {
    void onBidPlaced(BidEvent event);
}
