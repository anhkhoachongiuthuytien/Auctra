package com.auction.dao.memory;

import com.auction.dao.AuctionDao;
import com.auction.model.auction.Auction;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InMemoryAuctionDao implements AuctionDao {
    private final Map<String, Auction> database = new HashMap<>();

    public void save(Auction auction) {
        database.put(auction.getId(), auction);
    }

    public Auction findById(String id) {
        return database.get(id);
    }

    public List<Auction> findAll() {
        return new ArrayList<>(database.values());
    }

    public void delete(String id) {
        database.remove(id);
    }
}
