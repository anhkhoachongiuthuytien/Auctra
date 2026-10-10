package com.auction.service;

import com.auction.dao.AuctionDao;
import com.auction.exception.AuctionException;
import com.auction.manager.AuctionManager;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Seller;
import com.auction.util.IdGenerator;
import com.auction.util.AuctionLocks;
import java.time.LocalDateTime;

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
        synchronized (AuctionLocks.forId(auctionId)) {
            Auction auction = getAuctionById(auctionId);
            auction.start();
            auctionDao.save(auction);
        }
    }

    public void finishAuction(String auctionId) {
        synchronized (AuctionLocks.forId(auctionId)) {
            Auction auction = getAuctionById(auctionId);
            auction.finish();
            auctionDao.save(auction);
        }
    }

    public void cancelAuction(String auctionId) {
        synchronized (AuctionLocks.forId(auctionId)) {
            Auction auction = getAuctionById(auctionId);
            auction.cancel();
            auctionDao.save(auction);
            AuctionManager.getInstance().removeActiveAuction(auction);
        }
    }

    public void markAuctionPaid(String auctionId) {
        synchronized (AuctionLocks.forId(auctionId)) {
            Auction auction = getAuctionById(auctionId);
            auction.markPaid();
            auctionDao.save(auction);
            AuctionManager.getInstance().removeActiveAuction(auction);
        }
    }

    public boolean finishIfExpired(String auctionId, LocalDateTime now) {
        synchronized (AuctionLocks.forId(auctionId)) {
            Auction auction = getAuctionById(auctionId);
            if (!auction.isOpen() || auction.getEndTime() == null || now.isBefore(auction.getEndTime())) {
                return false;
            }
            auction.finish();
            auctionDao.save(auction);
            return true;
        }
    }

    public List<Auction> listAuctions() {
        return auctionDao.findAll();
    }

    public Auction getAuctionById(String auctionId) {
        Auction auction = auctionDao.findById(auctionId);
        if (auction == null) {
            throw new AuctionException("Không tìm thấy cuộc đấu giá");
        }
        return auction;
    }
}
