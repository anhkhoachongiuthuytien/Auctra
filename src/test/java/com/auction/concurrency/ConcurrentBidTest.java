package com.auction.concurrency;

import com.auction.dao.memory.InMemoryAuctionDao;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.service.BidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentBidTest {

    private InMemoryAuctionDao auctionDao;
    private BidService bidService;
    private Auction auction;
    private Bidder bidder1;
    private Bidder bidder2;

    @BeforeEach
    void setUp() {
        auctionDao = new InMemoryAuctionDao();
        bidService = new BidService(auctionDao);

        Item item = new Item("I001", "Laptop", "Gaming laptop", 1000.0);
        Seller seller = new Seller("S001", "sellerA", "seller@gmail.com");
        bidder1 = new Bidder("B001", "bidderA", "a@gmail.com");
        bidder2 = new Bidder("B002", "bidderB", "b@gmail.com");

        auction = new Auction("A001", item, seller);
        auction.start();
        auctionDao.save(auction);
    }

    @Test
    void testTwoThreadsPlaceBidsConcurrently() throws InterruptedException {
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch startGate = new CountDownLatch(1);

        Thread thread1 = new Thread(() -> {
            try {
                startGate.await();
                bidService.placeBid(auction.getId(), bidder1, 1200.0);
            } catch (Throwable t) {
                errors.add(t);
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                startGate.await();
                bidService.placeBid(auction.getId(), bidder2, 1500.0);
            } catch (Throwable t) {
                errors.add(t);
            }
        });

        thread1.start();
        thread2.start();
        startGate.countDown();

        thread1.join();
        thread2.join();

        assertEquals(1500.0, auction.getCurrentPrice());
        assertEquals(bidder2, auction.getWinner());
        assertTrue(auction.getBids().size() >= 1);
        assertTrue(errors.isEmpty() || errors.size() == 1);
    }
}
