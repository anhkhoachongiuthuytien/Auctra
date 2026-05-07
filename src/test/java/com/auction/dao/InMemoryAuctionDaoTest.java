package com.auction.dao;

import com.auction.dao.memory.InMemoryAuctionDao;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InMemoryAuctionDaoTest {

    private InMemoryAuctionDao auctionDao;
    private Auction auction;

    @BeforeEach
    void setUp() {
        auctionDao = new InMemoryAuctionDao();
        auction = new Auction("A001", new Item("I001", "Laptop", "Gaming laptop", 1000.0),
                new Seller("S001", "seller", "seller@test.com"));
        auctionDao.save(auction);
    }

    @Test
    void testFindById() {
        assertEquals(auction, auctionDao.findById("A001"));
    }

    @Test
    void testFindAll() {
        assertEquals(1, auctionDao.findAll().size());
    }

    @Test
    void testDelete() {
        auctionDao.delete("A001");

        assertNull(auctionDao.findById("A001"));
    }
}
