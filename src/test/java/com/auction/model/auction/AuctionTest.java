package com.auction.model.auction;

import com.auction.enums.AuctionStatus;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionException;
import com.auction.exception.InvalidBidException;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.observer.BidEvent;
import com.auction.observer.BidObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionTest {

    private Item item;
    private Seller seller;
    private Bidder bidder1;
    private Bidder bidder2;
    private Auction auction;

    @BeforeEach
    void setUp() {
        item = new Item("I001", "Laptop", "Gaming laptop", 1000.0);
        seller = new Seller("S001", "sellerA", "seller@gmail.com");
        bidder1 = new Bidder("B001", "bidderA", "a@gmail.com");
        bidder2 = new Bidder("B002", "bidderB", "b@gmail.com");
        auction = new Auction("A001", item, seller);
    }

    @Test
    void testConstructorInitialState() {
        assertEquals("A001", auction.getId());
        assertEquals(item, auction.getItem());
        assertEquals(seller, auction.getSeller());
        assertEquals(1000.0, auction.getCurrentPrice());
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertFalse(auction.isOpen());
        assertTrue(auction.getBids().isEmpty());
        assertEquals(null, auction.getWinner());
    }

    @Test
    void testStartAuctionMovesToRunning() {
        auction.start();

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
        assertTrue(auction.isOpen());
    }

    @Test
    void testFinishAuctionMovesToFinished() {
        auction.start();
        auction.finish();

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertFalse(auction.isOpen());
    }

    @Test
    void testCancelAuctionFromOpen() {
        auction.cancel();

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        assertFalse(auction.isOpen());
    }

    @Test
    void testCancelAuctionFromRunning() {
        auction.start();
        auction.cancel();

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        assertFalse(auction.isOpen());
    }

    @Test
    void testMarkPaidFromFinished() {
        auction.start();
        auction.finish();
        auction.markPaid();

        assertEquals(AuctionStatus.PAID, auction.getStatus());
        assertFalse(auction.isOpen());
    }

    @Test
    void testStartAuctionFromWrongStatusThrowsException() {
        auction.start();

        assertThrows(AuctionException.class, () -> auction.start());
    }

    @Test
    void testFinishAuctionFromWrongStatusThrowsException() {
        assertThrows(AuctionException.class, () -> auction.finish());
    }

    @Test
    void testMarkPaidFromWrongStatusThrowsException() {
        auction.start();

        assertThrows(AuctionException.class, () -> auction.markPaid());
    }

    @Test
    void testAddBidWhenAuctionIsRunning() {
        auction.start();
        BidTransaction bid = new BidTransaction(bidder1, 1200.0);

        auction.addBid(bid);

        assertEquals(1, auction.getBids().size());
        assertEquals(1200.0, auction.getCurrentPrice());
        assertEquals(bidder1, auction.getWinner());
    }

    @Test
    void testAddMultipleValidBids() {
        auction.start();
        BidTransaction bid1 = new BidTransaction(bidder1, 1200.0);
        BidTransaction bid2 = new BidTransaction(bidder2, 1500.0);

        auction.addBid(bid1);
        auction.addBid(bid2);

        assertEquals(2, auction.getBids().size());
        assertEquals(1500.0, auction.getCurrentPrice());
        assertEquals(bidder2, auction.getWinner());
    }

    @Test
    void testAddBidWhenAuctionIsNotRunningThrowsException() {
        BidTransaction bid = new BidTransaction(bidder1, 1200.0);

        assertThrows(AuctionClosedException.class, () -> auction.addBid(bid));
    }

    @Test
    void testAddNullBidThrowsException() {
        auction.start();

        assertThrows(InvalidBidException.class, () -> auction.addBid(null));
    }

    @Test
    void testAddLowerBidThrowsException() {
        auction.start();
        BidTransaction bid = new BidTransaction(bidder1, 900.0);

        assertThrows(InvalidBidException.class, () -> auction.addBid(bid));
    }

    @Test
    void testObserverReceivesNewBidNotification() {
        auction.start();
        CapturingObserver observer = new CapturingObserver();
        auction.addObserver(observer);

        BidTransaction bid = new BidTransaction(bidder1, 1300.0);
        auction.addBid(bid);

        assertNotNull(observer.event);
        assertEquals(auction, observer.event.getAuction());
        assertEquals(1300.0, observer.event.getLatestPrice());
        assertEquals(bidder1, observer.event.getCurrentWinner());
    }

    @Test
    void testToString() {
        String result = auction.toString();

        assertNotNull(result);
        assertTrue(result.contains("Auction"));
        assertTrue(result.contains("A001"));
        assertTrue(result.contains("status"));
    }

    private static class CapturingObserver implements BidObserver {
        private BidEvent event;

        @Override
        public void onBidPlaced(BidEvent event) {
            this.event = event;
        }
    }
}
