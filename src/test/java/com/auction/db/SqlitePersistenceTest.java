package com.auction.db;

import com.auction.dao.sqlite.SqliteAuctionDao;
import com.auction.dao.sqlite.SqliteItemDao;
import com.auction.dao.sqlite.SqliteUserDao;
import com.auction.enums.AuctionStatus;
import com.auction.model.auction.Auction;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SqlitePersistenceTest {

    private Path databasePath;
    private DatabaseManager databaseManager;
    private SqliteUserDao userDao;
    private SqliteItemDao itemDao;
    private SqliteAuctionDao auctionDao;

    @BeforeEach
    void setUp() throws IOException {
        databasePath = Files.createTempFile("auction-system-test-", ".db");
        databaseManager = new DatabaseManager("jdbc:sqlite:" + databasePath.toAbsolutePath());
        databaseManager.initializeSchema();
        userDao = new SqliteUserDao(databaseManager);
        itemDao = new SqliteItemDao(databaseManager);
        auctionDao = new SqliteAuctionDao(databaseManager, itemDao, userDao);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(databasePath);
    }

    // Kiểm tra toàn bộ aggregate Auction có thể lưu xuống SQLite rồi load lại đầy đủ trạng thái.
    @Test
    void testPersistAndReloadAuctionAggregate() {
        Seller seller = new Seller("S001", "seller", "seller@test.com");
        Bidder bidder = new Bidder("B001", "bidder", "bidder@test.com");
        Item item = new Electronics("I001", "Laptop", "Gaming laptop", 1000.0);

        userDao.save(seller);
        userDao.save(bidder);
        itemDao.save(item);

        Auction auction = new Auction("A001", item, seller);
        auction.start();
        auction.addBid(new BidTransaction(bidder, 1200.0));
        auctionDao.save(auction);

        Auction reloaded = auctionDao.findById("A001");

        assertNotNull(reloaded);
        assertEquals(AuctionStatus.RUNNING, reloaded.getStatus());
        assertEquals(1200.0, reloaded.getCurrentPrice());
        assertEquals("bidder@test.com", reloaded.getWinner().getEmail());
        assertEquals(1, reloaded.getBids().size());
    }
}
