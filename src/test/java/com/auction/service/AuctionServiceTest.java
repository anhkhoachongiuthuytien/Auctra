package com.auction.service;

import com.auction.dao.memory.InMemoryAuctionDao;
import com.auction.enums.AuctionStatus;
import com.auction.exception.AuctionException;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionServiceTest {

    private InMemoryAuctionDao auctionDao;
    private AuctionService auctionService;
    private Item item;
    private Seller seller;

    @BeforeEach
    void setUp() {
        auctionDao = new InMemoryAuctionDao();
        auctionService = new AuctionService(auctionDao);
        item = new Item("I001", "Laptop", "Gaming laptop", 1000.0);
        seller = new Seller("S001", "sellerA", "seller@gmail.com");
    }

    @Test
    void testCreateAuction() {
        Auction auction = auctionService.createAuction(item, seller);

        assertNotNull(auction);
        assertNotNull(auction.getId());
        assertEquals(item, auction.getItem());
        assertEquals(seller, auction.getSeller());
        assertEquals(AuctionStatus.CREATED, auction.getStatus());
        assertEquals(auction, auctionDao.findById(auction.getId()));
    }

    @Test
    void testStartAuction() {
        Auction auction = auctionService.createAuction(item, seller);

        auctionService.startAuction(auction.getId());

        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    void testFinishAuction() {
        Auction auction = auctionService.createAuction(item, seller);
        auctionService.startAuction(auction.getId());

        auctionService.finishAuction(auction.getId());

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    @Test
    void testCancelAuction() {
        Auction auction = auctionService.createAuction(item, seller);

        auctionService.cancelAuction(auction.getId());

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    void testMarkAuctionPaid() {
        Auction auction = auctionService.createAuction(item, seller);
        auctionService.startAuction(auction.getId());
        auctionService.finishAuction(auction.getId());

        auctionService.markAuctionPaid(auction.getId());

        assertEquals(AuctionStatus.PAID, auction.getStatus());
    }

    @Test
    void testStartAuctionWhenAuctionNotFoundThrowsException() {
        assertThrows(AuctionException.class, () -> auctionService.startAuction("INVALID_ID"));
    }

    @Test
    void testFinishAuctionWhenAuctionNotFoundThrowsException() {
        assertThrows(AuctionException.class, () -> auctionService.finishAuction("INVALID_ID"));
    }

    @Test
    void testCancelAuctionWhenAuctionNotFoundThrowsException() {
        assertThrows(AuctionException.class, () -> auctionService.cancelAuction("INVALID_ID"));
    }

    @Test
    void testMarkAuctionPaidWhenAuctionNotFoundThrowsException() {
        assertThrows(AuctionException.class, () -> auctionService.markAuctionPaid("INVALID_ID"));
    }

    @Test
    void testFinishAuctionFromWrongStateThrowsException() {
        Auction auction = auctionService.createAuction(item, seller);

        assertThrows(AuctionException.class, () -> auctionService.finishAuction(auction.getId()));
    }

    @Test
    void testMarkAuctionPaidFromWrongStateThrowsException() {
        Auction auction = auctionService.createAuction(item, seller);

        assertThrows(AuctionException.class, () -> auctionService.markAuctionPaid(auction.getId()));
    }
}
