package com.auction.service;

import com.auction.dao.memory.InMemoryAuctionDao;
import com.auction.dao.memory.InMemoryAutoBidDao;
import com.auction.dao.memory.InMemoryUserDao;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.ValidationException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoBidRegressionTest {
    private final InMemoryAuctionDao auctions = new InMemoryAuctionDao();
    private final InMemoryAutoBidDao configs = new InMemoryAutoBidDao();
    private final InMemoryUserDao users = new InMemoryUserDao();
    private final BidService service = new BidService(auctions, configs, users);
    private final Bidder first = new Bidder("first", "first", "first@test.local");
    private final Bidder second = new Bidder("second", "second", "second@test.local");
    private Auction auction;

    @BeforeEach
    void setUp() {
        users.save(first);
        users.save(second);
        auction = new Auction("regression", new Item("item", "item", "desc", 100),
                new Seller("seller", "seller", "seller@test.local"));
        auction.start();
        auctions.save(auction);
    }

    @Test
    void activationImmediatelyBidsAndDoesNotBidAgainstItself() {
        service.registerAutoBid(auction.getId(), first.getId(), 200, 10);
        assertEquals(110, auction.getCurrentPrice());
        assertEquals(first.getId(), auction.getWinner().getId());
        service.registerAutoBid(auction.getId(), first.getId(), 300, 10);
        assertEquals(110, auction.getCurrentPrice());
        assertEquals(1, auction.getBids().size());
    }

    @Test
    void outbidTriggersAutomaticResponseWithinMaximumAndCancelStopsIt() {
        service.registerAutoBid(auction.getId(), first.getId(), 200, 10);
        service.placeBid(auction.getId(), second, 120);
        assertEquals(130, auction.getCurrentPrice());
        assertEquals(first.getId(), auction.getWinner().getId());
        service.cancelAutoBid(auction.getId(), first.getId());
        service.placeBid(auction.getId(), second, 140);
        assertEquals(second.getId(), auction.getWinner().getId());
        assertEquals(140, auction.getCurrentPrice());
        assertNull(configs.find(auction.getId(), first.getId()));
    }

    @Test
    void twoAutomaticBiddersSettleWithoutExceedingTheirLimits() {
        service.registerAutoBid(auction.getId(), first.getId(), 200, 10);
        service.registerAutoBid(auction.getId(), second.getId(), 250, 10);
        assertEquals(second.getId(), auction.getWinner().getId());
        // The second bidder reaches 200; the first cannot answer with its required 210 step.
        assertEquals(200, auction.getCurrentPrice());
        assertTrue(auction.getBids().stream().allMatch(b -> b.getAmount() <=
                (b.getBidder().getId().equals(first.getId()) ? 200 : 250)));
    }

    @Test
    void invalidNonFiniteAndNonPositiveConfigurationsAreRejectedWithoutSideEffects() {
        for (double bad : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(ValidationException.class, () -> service.registerAutoBid(auction.getId(), first.getId(), 200, bad));
            assertThrows(ValidationException.class, () -> service.registerAutoBid(auction.getId(), first.getId(), bad, 10));
        }
        assertThrows(ValidationException.class, () -> service.registerAutoBid(auction.getId(), first.getId(), 105, 10));
        assertNull(configs.find(auction.getId(), first.getId()));
        assertEquals(100, auction.getCurrentPrice());
    }

    @Test
    void decimalStepsStayWithinTheAdvertisedCeiling() {
        service.registerAutoBid(auction.getId(), first.getId(), 100.3, 0.1);
        service.placeBid(auction.getId(), second, 100.2);
        assertEquals(100.3, auction.getCurrentPrice());
        assertEquals(first.getId(), auction.getWinner().getId());
    }

    @Test
    void badLegacyConfigDoesNotBreakManualBidding() {
        configs.save(new AutoBidConfig(auction.getId(), first.getId(), 200, 0));
        service.placeBid(auction.getId(), second, 120);
        assertEquals(120, auction.getCurrentPrice());
    }

    @Test
    void oversizedAutomaticWarFailsPromptlyBeforeChangingTheAuctionOrConfiguration() {
        service.registerAutoBid(auction.getId(), first.getId(), 1000000, 0.001);
        double before = auction.getCurrentPrice();
        int historyBefore = auction.getBids().size();
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () ->
                assertThrows(ValidationException.class, () -> service.registerAutoBid(auction.getId(), second.getId(), 1000000, 0.001)));
        assertEquals(before, auction.getCurrentPrice());
        assertEquals(historyBefore, auction.getBids().size());
        assertNull(configs.find(auction.getId(), second.getId()));
    }

    @Test
    void expiredAuctionRejectsManualAndAutomaticBidsBeforeSchedulerRuns() {
        auction.setEndTime(LocalDateTime.now().minusSeconds(1));
        assertThrows(AuctionClosedException.class, () -> service.placeBid(auction.getId(), first, 120));
        assertThrows(AuctionClosedException.class, () -> service.registerAutoBid(auction.getId(), first.getId(), 200, 10));
        assertTrue(auction.getBids().isEmpty());
        assertNull(configs.find(auction.getId(), first.getId()));
    }
}
