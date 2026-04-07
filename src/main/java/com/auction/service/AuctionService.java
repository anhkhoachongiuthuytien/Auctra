package com.auction.service;

import com.auction.dao.AuctionDao;
import com.auction.exception.AuctionException;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Seller;

public class AuctionService {
    private AuctionDao auctionDao;
    public AuctionService(AuctionDao autionDao){
        this.auctionDao = autionDao;
    }
    public Auction createdAuction(Item item, Seller seller) {
        Auction auction = new Auction(Id.Generator.generateId(), item, seller);
        auctionDao.save(auction);
        return auction;
    }
    public void openAution(String auctionId){
        Auction auction = auctionDao.fingById(auctionId);
        if (auction == null) throw new AuctionException("Auction not found");
        auction.openAuction();
    }
    public void closeAution(String auctionId){
        Auction auction = auctionDao.findById(auctionId);
        if (auction == null) throw new AuctionException("Auction not found");
        auction.closeAuction();
    }
}