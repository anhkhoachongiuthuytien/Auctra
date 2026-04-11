package com.auction.service;

import com.auction.dao.AuctionDao;
import com.auction.exception.AuctionException;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Seller;
import com.auction.util.IdGenerator;

public class AuctionService {
    private final AuctionDao auctionDao;

    public AuctionService(AuctionDao auctionDao) {
        this.auctionDao = auctionDao;
    }

    public Auction createAuction(Item item, Seller seller) {
        Auction auction = new Auction(IdGenerator.generateId(), item, seller);
        auctionDao.save(auction);
        return auction;
    }

    public void openAuction(String auctionId) {
        Auction auction = auctionDao.findById(auctionId);
        if (auction == null) throw new AuctionException("Auction not found");
        auction.openAuction();
    }

    public void closeAuction(String auctionId) {
        Auction auction = auctionDao.findById(auctionId);
        if (auction == null) throw new AuctionException("Auction not found");
        auction.closeAuction();
    }
}
