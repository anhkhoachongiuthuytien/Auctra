package com.auction.concurrency;

import com.auction.dao.sqlite.SqliteAuctionDao;
import com.auction.dao.sqlite.SqliteItemDao;
import com.auction.dao.sqlite.SqliteUserDao;
import com.auction.db.DatabaseManager;
import com.auction.enums.AuctionStatus;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionException;
import com.auction.exception.InvalidBidException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Electronics;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.service.AuctionService;
import com.auction.service.BidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteConcurrentBidTest {
    @TempDir Path directory;
    private SqliteAuctionDao auctions;
    private AuctionService auctionService;
    private BidService bids;
    private final Bidder bidder = new Bidder("bidder", "bidder", "bidder@test.local");

    @BeforeEach
    void setUp() {
        DatabaseManager database = new DatabaseManager("jdbc:sqlite:" + directory.resolve("concurrency.db"));
        database.initializeSchema();
        SqliteUserDao users = new SqliteUserDao(database);
        SqliteItemDao items = new SqliteItemDao(database);
        auctions = new SqliteAuctionDao(database, items, users);
        auctionService = new AuctionService(auctions);
        bids = new BidService(auctions);
        Seller seller = new Seller("seller", "seller", "seller@test.local");
        Electronics item = new Electronics("item", "item", "description", 1000);
        users.save(seller);
        users.save(bidder);
        items.save(item);
        Auction auction = new Auction("auction", item, seller);
        auction.start();
        auctions.save(auction);
    }

    @Test
    void simultaneousSqliteBidsKeepTheHighestPriceAndEveryAcceptedBid() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int i = 1; i <= 30; i++) {
                double amount = 1000 + i * 10;
                results.add(pool.submit(() -> {
                    start.await();
                    try { bids.placeBid("auction", bidder, amount); return true; }
                    catch (InvalidBidException expected) { return false; }
                }));
            }
            start.countDown();
            int accepted = 0;
            for (Future<Boolean> result : results) { if (result.get(10, TimeUnit.SECONDS)) { accepted++; } }
            Auction stored = auctions.findById("auction");
            assertEquals(1300, stored.getCurrentPrice());
            assertEquals(accepted, stored.getBids().size());
            for (int i = 1; i < stored.getBids().size(); i++) {
                assertTrue(stored.getBids().get(i).getAmount() > stored.getBids().get(i - 1).getAmount());
            }
        } finally { pool.shutdownNow(); }
    }

    @Test
    void staleSnapshotCannotLowerThePriceOrEraseHistory() {
        Auction stale = auctions.findById("auction");
        bids.placeBid("auction", bidder, 1500);
        stale.addBid(new BidTransaction(bidder, 1200));
        assertThrows(AuctionException.class, () -> auctions.save(stale));
        Auction stored = auctions.findById("auction");
        assertEquals(1500, stored.getCurrentPrice());
        assertEquals(1, stored.getBids().size());
        assertEquals(1500, stored.getBids().get(0).getAmount());
    }

    @Test
    void bidAndFinishCannotReopenTheAuction() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> bid = pool.submit(() -> {
                try { start.await(); bids.placeBid("auction", bidder, 1200); }
                catch (AuctionClosedException expected) { }
                catch (InterruptedException exception) { throw new RuntimeException(exception); }
            });
            Future<?> finish = pool.submit(() -> {
                try { start.await(); auctionService.finishAuction("auction"); }
                catch (InterruptedException exception) { throw new RuntimeException(exception); }
            });
            start.countDown();
            bid.get(10, TimeUnit.SECONDS);
            finish.get(10, TimeUnit.SECONDS);
            assertEquals(AuctionStatus.FINISHED, auctions.findById("auction").getStatus());
            assertThrows(AuctionClosedException.class, () -> bids.placeBid("auction", bidder, 1600));
        } finally { pool.shutdownNow(); }
    }

    @Test
    void schedulerRechecksTheLatestExtendedDeadline() {
        Auction auction = auctions.findById("auction");
        auction.setEndTime(LocalDateTime.now().plusSeconds(20));
        auctions.save(auction);
        LocalDateTime oldExpiry = auction.getEndTime().plusSeconds(1);
        bids.placeBid("auction", bidder, 1200);
        assertFalse(auctionService.finishIfExpired("auction", oldExpiry));
        assertEquals(AuctionStatus.RUNNING, auctions.findById("auction").getStatus());
    }
}
