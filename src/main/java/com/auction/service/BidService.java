package com.auction.service;

import com.auction.dao.AuctionDao;
import com.auction.model.auction.Auction;
import com.auction.model.auction.BidTransaction;
import com.auction.model.user.Bidder;

public class BidService {
    private AuctionDao auctionDao;
    public BidService(AuctionDao auctionDao) {
        this.auctionDao = auctionDao;
    }
    public void placeBid(String auctionId, Bidder bidder, double amount){
        Auction auction = auctionDao.findById(auctionId);
        if (auction == null) {
            throw new AuctionException("Auction not found");
        }
        if(!auction.isOpen()){
            throw new AuctionClosedException("Auction is closed");
        }
        if (amount<=auction.getCurrentPrice()){
            throw new InvalidBidException("Bid mus be hihger");
        }
        BidTransaction bid = new  BidTransaction(bidder, amount);
        auction.addBid(bid);
    }
}
