package com.auction.manager;

import com.auction.model.auction.Auction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuctionManager {
    private static AuctionManager instance;
    private final List<Auction> activeAuctions;

    private AuctionManager() {
        activeAuctions = Collections.synchronizedList(new ArrayList<>());
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public synchronized void addActiveAuction(Auction auction) {
        if (auction != null && !activeAuctions.contains(auction)) {
            activeAuctions.add(auction);
        }
    }

    public synchronized void removeActiveAuction(Auction auction) {
        activeAuctions.remove(auction);
    }

    public synchronized List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctions);
    }
}
