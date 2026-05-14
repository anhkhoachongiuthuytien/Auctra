package com.auction.observer;

public class ConsoleBidObserver implements BidObserver {
    @Override
    public void onBidPlaced(BidEvent event) {
        System.out.println(
                "Observer: auction " + event.getAuction().getId()
                        + " received a new bid of "
                        + event.getBidTransaction().getAmount()
                        + " from "
                        + event.getBidTransaction().getBidder().getUsername()
        );
    }
}
