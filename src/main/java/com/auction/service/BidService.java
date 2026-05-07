package com.auction.service;

import com.auction.dao.AuctionDao;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.ValidationException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.BidTransaction;
import com.auction.model.user.Bidder;

public class BidService {
    private final AuctionDao auctionDao;

    public BidService(AuctionDao auctionDao) {
        this.auctionDao = auctionDao;
    }

    public void placeBid(String auctionId, Bidder bidder, double amount) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException("Auction id must not be empty");
        }
        if (bidder == null) {
            throw new ValidationException("Bidder must not be null");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new ValidationException("Bid amount must be greater than 0");
        }
        Auction auction = auctionDao.findById(auctionId);
        if (auction == null) {
            throw new AuctionException("Auction not found");
        }
        synchronized (auction) {
            if (!auction.isOpen()) {
                throw new AuctionClosedException("Auction is closed");
            }
            if (amount <= auction.getCurrentPrice()) {
                throw new InvalidBidException("Bid must be higher");
            }
            BidTransaction bid = new BidTransaction(bidder, amount);
            auction.addBid(bid);
            auctionDao.save(auction);
        }
    }
}
