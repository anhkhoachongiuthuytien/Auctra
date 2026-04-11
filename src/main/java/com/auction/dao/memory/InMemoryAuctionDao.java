package com.auction.dao;
import com.auction.dao.AuctionDao;
import java.util.HashMap;
import com.auction.model.auction.Auction;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class InMemoryAuctionDaoTest implements AuctionDao{
    HashMap<String, Auction> database = new HashMap<>();
    public void save(Auction auction){
        database.put(auction.getId(), auction);
    }
    public Auction findById(String id){
        return database.get(id);
    }
    public List<Auction> findAll(){
        return new ArrayList<>(database.values());
    }
    public void delete(String id){
        database.remove(id);
    }
}package com.auction.dao;
import com.auction.dao.AuctionDao;
import java.util.HashMap;
import com.auction.model.auction.Auction;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class InMemoryAuctionDaoTest implements AuctionDao{
    HashMap<String, Auction> database = new HashMap<>();
    public void save(Auction auction){
        database.put(auction.getId(), auction);
    }
    public Auction findById(String id){
        return database.get(id);
    }
    public List<Auction> findAll(){
        return new ArrayList<>(database.values());
    }
    public void delete(String id){
        database.remove(id);
    }
}