package com.auction.service;

import com.auction.dao.memory.InMemoryAuctionDao;
import com.auction.dao.memory.InMemoryAutoBidDao;
import com.auction.dao.memory.InMemoryUserDao;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoBidPriorityQueueTest {

    private InMemoryAuctionDao auctionDao;
    private InMemoryAutoBidDao autoBidDao;
    private InMemoryUserDao userDao;
    private BidService bidService;

    private Bidder bidderA;
    private Bidder bidderB;
    private Bidder bidderC;
    private Auction auction;

    @BeforeEach
    void setUp() {
        auctionDao = new InMemoryAuctionDao();
        autoBidDao = new InMemoryAutoBidDao();
        userDao = new InMemoryUserDao();
        bidService = new BidService(auctionDao, autoBidDao, userDao);

        // Tạo người dùng
        Seller seller = new Seller("S001", "seller", "seller@test.com");
        bidderA = new Bidder("B001", "bidderA", "a@test.com");
        bidderB = new Bidder("B002", "bidderB", "b@test.com");
        bidderC = new Bidder("B003", "bidderC", "c@test.com");

        userDao.save(seller);
        userDao.save(bidderA);
        userDao.save(bidderB);
        userDao.save(bidderC);

        // Tạo phiên đấu giá với giá khởi điểm là $300
        Item item = new Item("I001", "Tranh sơn mài", "Tranh sơn mài truyền thống", 300.0);
        auction = new Auction("A001", item, seller);
        auctionDao.save(auction);
    }

    @Test
    void testAutoBiddingPriorityQueueWar() {
        // Cấu hình Auto-Bid cho 3 bidder:
        // Bidder A: Max $500, Increment $10
        // Bidder B: Max $600, Increment $15
        // Bidder C: Max $550, Increment $20
        autoBidDao.save(new AutoBidConfig(auction.getId(), bidderA.getId(), 500.0, 10.0));
        autoBidDao.save(new AutoBidConfig(auction.getId(), bidderB.getId(), 600.0, 15.0));
        autoBidDao.save(new AutoBidConfig(auction.getId(), bidderC.getId(), 550.0, 20.0));

        auction.start();

        // Đặt một lượt bid thủ công trị giá $310 bởi Bidder A để khởi động cuộc chiến Auto-Bid
        bidService.placeBid(auction.getId(), bidderA, 310.0);

        // Xem giá trị cuối cùng sau khi cuộc chiến tự động đấu giá kết thúc
        double finalPrice = auction.getCurrentPrice();
        System.out.println("Giá cuối cùng của cuộc đấu giá tự động: $" + finalPrice);
        System.out.println("Người thắng cuộc: " + (auction.getWinner() != null ? auction.getWinner().getUsername() : "Không có"));

        // Người thắng cuộc phải là bidder B vì max bid của họ là cao nhất ($600).
        assertEquals(bidderB.getId(), auction.getWinner().getId());

        // Giá cuối cùng phải là $535.0 (khi Bidder C đạt giới hạn $550 và không thể bid tiếp ở mức $555)
        assertEquals(535.0, finalPrice);
        
        // Xem số lượng lượt bid được tạo ra tự động
        assertTrue(auction.getBids().size() > 5);
    }
}
