package com.auction.service;

import com.auction.dao.AuctionDao;
import com.auction.exception.AuctionException;
import com.auction.manager.AuctionManager;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Seller;
import com.auction.util.IdGenerator;

import java.util.List;

public class AuctionService {
    private final AuctionDao auctionDao;

    public AuctionService(AuctionDao auctionDao) {
        this.auctionDao = auctionDao;
    }

    public Auction createAuction(Item item, Seller seller) {
        Auction auction = new Auction(IdGenerator.generateId(), item, seller);
        auctionDao.save(auction);
        AuctionManager.getInstance().addActiveAuction(auction);
        return auction;
    }

    public void startAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);
        auction.start();
    }

    public void finishAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);
        auction.finish();
    }

    public void cancelAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);
        auction.cancel();
        AuctionManager.getInstance().removeActiveAuction(auction);
    }

    public void markAuctionPaid(String auctionId) {
        Auction auction = getAuctionById(auctionId);
        auction.markPaid();
        AuctionManager.getInstance().removeActiveAuction(auction);
    }

    public List<Auction> listAuctions() {
        return auctionDao.findAll();
    }

    public Auction getAuctionById(String auctionId) {
        Auction auction = auctionDao.findById(auctionId);
        if (auction == null) {
            throw new AuctionException("Auction not found");
        }
        return auction;
    }
}
