package com.auction.manager;

import com.auction.model.auction.Auction;
import com.auction.model.item.Art;
import com.auction.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

    private AuctionManager manager;

    @BeforeEach
    void setUp() {
        manager = AuctionManager.getInstance();
        // Clear active auctions between tests by removing all currently stored
        List<Auction> current = manager.getActiveAuctions();
        for (Auction a : current) {
            manager.removeActiveAuction(a);
        }
    }

    @Test
    void testSingleton() {
        AuctionManager m1 = AuctionManager.getInstance();
        AuctionManager m2 = AuctionManager.getInstance();
        assertSame(m1, m2);
    }

    @Test
    void testAddAndRemoveAuction() {
        Art item = new Art("item1", "Item 1", "Desc", 10.0);
        Seller seller = new Seller("s1", "seller", "email");
        Auction auction = new Auction("auc1", item, seller);

        assertTrue(manager.getActiveAuctions().isEmpty());

        manager.addActiveAuction(auction);
        assertEquals(1, manager.getActiveAuctions().size());
        assertEquals(auction, manager.getActiveAuctions().get(0));

        // Add duplicate should not change size
        manager.addActiveAuction(auction);
        assertEquals(1, manager.getActiveAuctions().size());

        // Add null should not change size
        manager.addActiveAuction(null);
        assertEquals(1, manager.getActiveAuctions().size());

        manager.removeActiveAuction(auction);
        assertTrue(manager.getActiveAuctions().isEmpty());
    }
}
