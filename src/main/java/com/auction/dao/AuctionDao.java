package com.auction.dao;
import com.auction.model.auction.Auction;
import java.util.List;

public interface AuctionDao {
    void save(Auction auction);
    Auction findById(String id);
    List<Auction> findAll();
    void delete(String id);
}
