package com.auction.service;

import com.auction.dao.memory.InMemoryAuctionDao;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiSnipingTest {

    private InMemoryAuctionDao auctionDao;
    private BidService bidService;
    private Bidder bidder;
    private Auction auction;

    @BeforeEach
    void setUp() {
        auctionDao = new InMemoryAuctionDao();
        bidService = new BidService(auctionDao);

        Seller seller = new Seller("S001", "seller", "seller@test.com");
        bidder = new Bidder("B001", "bidder", "bidder@test.com");

        Item item = new Item("I001", "Laptop", "Laptop gaming", 1000.0);
        auction = new Auction("A001", item, seller);
        auctionDao.save(auction);
    }

    @Test
    void testAntiSnipingTriggered() {
        auction.start();

        // Thiết lập thời gian kết thúc của phiên đấu giá là 30 giây từ bây giờ (dưới 60 giây)
        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(30);
        auction.setEndTime(originalEndTime);

        // Đặt giá
        bidService.placeBid(auction.getId(), bidder, 1200.0);

        // Sau khi đặt giá, thời gian kết thúc phải được gia hạn thêm 60 giây
        LocalDateTime newEndTime = auction.getEndTime();
        
        assertTrue(newEndTime.isAfter(originalEndTime));
        assertEquals(originalEndTime.plusSeconds(60), newEndTime);
    }

    @Test
    void testAntiSnipingNotTriggeredWhenFarFromEnd() {
        auction.start();

        // Thiết lập thời gian kết thúc của phiên đấu giá là 10 phút từ bây giờ (lớn hơn 60 giây)
        LocalDateTime originalEndTime = LocalDateTime.now().plusMinutes(10);
        auction.setEndTime(originalEndTime);

        // Đặt giá
        bidService.placeBid(auction.getId(), bidder, 1200.0);

        // Thời gian kết thúc không đổi vì còn quá xa thời điểm hết hạn
        assertEquals(originalEndTime, auction.getEndTime());
    }
}
