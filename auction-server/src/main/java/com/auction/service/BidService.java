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
            throw new ValidationException("Mã phiên đấu giá không được để trống");
        }
        if (bidder == null) {
            throw new ValidationException("Bidder không được để trống");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new ValidationException("Số tiền đặt giá phải lớn hơn 0");
        }
        Auction auction = auctionDao.findById(auctionId);
        if (auction == null) {
            throw new AuctionException("Không tìm thấy cuộc đấu giá");
        }
        // Khóa theo từng object auction để hai thread không cùng kiểm tra và cập nhật
        // currentPrice trên cùng một phiên đấu giá tại cùng thời điểm.
        synchronized (auction) {
            if (!auction.isOpen()) {
                throw new AuctionClosedException("Cuộc đấu giá đã đóng");
            }
            if (amount <= auction.getCurrentPrice()) {
                throw new InvalidBidException("Giá đặt phải cao hơn giá hiện tại");
            }
            BidTransaction bid = new BidTransaction(bidder, amount);
            auction.addBid(bid);
            auctionDao.save(auction);
        }
    }
}
