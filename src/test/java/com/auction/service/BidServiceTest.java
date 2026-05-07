package com.auction.service;

import com.auction.dao.memory.InMemoryAuctionDao;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.ValidationException;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BidServiceTest {

    private InMemoryAuctionDao auctionDao;
    private BidService bidService;
    private Bidder bidder1;
    private Bidder bidder2;
    private Auction auction;

    @BeforeEach
    void setUp() {
        auctionDao = new InMemoryAuctionDao();
        bidService = new BidService(auctionDao);

        Item item = new Item("I001", "Laptop", "Gaming laptop", 1000.0);
        Seller seller = new Seller("S001", "sellerA", "seller@gmail.com");
        bidder1 = new Bidder("B001", "bidderA", "a@gmail.com");
        bidder2 = new Bidder("B002", "bidderB", "b@gmail.com");

        auction = new Auction("A001", item, seller);
        auctionDao.save(auction);
    }

    @Test
    void testPlaceValidBid() {
        auction.start();

        bidService.placeBid(auction.getId(), bidder1, 1200.0);

        assertEquals(1, auction.getBids().size());
        assertEquals(1200.0, auction.getCurrentPrice());
        assertEquals(bidder1, auction.getWinner());
    }

    @Test
    void testPlaceBidWhenAuctionNotFoundThrowsException() {
        assertThrows(AuctionException.class, () ->
                bidService.placeBid("INVALID_ID", bidder1, 1200.0));
    }

    @Test
    void testPlaceBidWhenAuctionClosedThrowsException() {
        assertThrows(AuctionClosedException.class, () ->
                bidService.placeBid(auction.getId(), bidder1, 1200.0));
    }

    @Test
    void testPlaceLowerBidThrowsException() {
        auction.start();

        assertThrows(InvalidBidException.class, () ->
                bidService.placeBid(auction.getId(), bidder1, 900.0));
    }

    @Test
    void testPlaceEqualBidThrowsException() {
        auction.start();

        assertThrows(InvalidBidException.class, () ->
                bidService.placeBid(auction.getId(), bidder1, 1000.0));
    }

    @Test
    void testPlaceMultipleValidBids() {
        auction.start();

        bidService.placeBid(auction.getId(), bidder1, 1200.0);
        bidService.placeBid(auction.getId(), bidder2, 1500.0);

        assertEquals(2, auction.getBids().size());
        assertEquals(1500.0, auction.getCurrentPrice());
        assertEquals(bidder2, auction.getWinner());
    }

    @Test
    void testPlaceBidWithNullBidderThrowsValidationException() {
        auction.start();

        assertThrows(ValidationException.class, () ->
                bidService.placeBid(auction.getId(), null, 1200.0));
    }

    @Test
    void testPlaceBidWithInvalidAmountThrowsValidationException() {
        auction.start();

        assertThrows(ValidationException.class, () ->
                bidService.placeBid(auction.getId(), bidder1, 0));
    }
}
