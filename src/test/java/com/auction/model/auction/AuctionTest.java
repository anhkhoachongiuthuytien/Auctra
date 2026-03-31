package com.auction.model.auction;

import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
    void testConstructor() {
        assertEquals("A001", auction.getId());
        assertEquals(item, auction.getItem());
        assertEquals(seller, auction.getSeller());
        assertEquals(1000.0, auction.getCurrentPrice());
        assertFalse(auction.isOpen());
        assertTrue(auction.getBids().isEmpty());
        assertNull(auction.getWinner());
    }

    @Test
    void testOpenCloseAuction() {
        auction.openAuction();
        assertTrue(auction.isOpen());

        auction.closeAuction();
        assertFalse(auction.isOpen());
    }

    @Test
    void testAddBid() {
        BidTransaction bid = new BidTransaction(bidder1, 1200.0);

        auction.addBid(bid);

        assertEquals(1, auction.getBids().size());
        assertEquals(1200.0, auction.getCurrentPrice());
        assertEquals(bidder1, auction.getWinner());
    }

    @Test
    void testMultipleBids() {
        BidTransaction bid1 = new BidTransaction(bidder1, 1200.0);
        BidTransaction bid2 = new BidTransaction(bidder2, 1500.0);

        auction.addBid(bid1);
        auction.addBid(bid2);

        assertEquals(2, auction.getBids().size());
        assertEquals(1500.0, auction.getCurrentPrice());
        assertEquals(bidder2, auction.getWinner());
    }

    @Test
    void testAddNullBid() {
        auction.addBid(null);

        assertEquals(0, auction.getBids().size());
        assertEquals(1000.0, auction.getCurrentPrice());
        assertNull(auction.getWinner());
    }

    @Test
    void testSetWinner() {
        auction.setWinner(bidder1);
        assertEquals(bidder1, auction.getWinner());
    }

    @Test
    void testToString() {
        String result = auction.toString();

        assertNotNull(result);
        assertTrue(result.contains("Auction"));
        assertTrue(result.contains("A001"));
    }
}