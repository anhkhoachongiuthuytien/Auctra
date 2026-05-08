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

    // Xác nhận constructor thiết lập đầy đủ trạng thái mặc định ban đầu của auction.
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

    // Auction phải chuyển từ OPEN sang RUNNING khi được start hợp lệ.
    @Test
    void testStartAuctionMovesToRunning() {
        auction.start();

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
        assertTrue(auction.isOpen());
    }

    // Auction đang chạy phải chuyển sang FINISHED khi kết thúc phiên.
    @Test
    void testFinishAuctionMovesToFinished() {
        auction.start();
        auction.finish();

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertFalse(auction.isOpen());
    }

    // Cho phép hủy auction ngay từ trạng thái OPEN.
    @Test
    void testCancelAuctionFromOpen() {
        auction.cancel();

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        assertFalse(auction.isOpen());
    }

    // Cho phép hủy auction cả khi phiên đã được start.
    @Test
    void testCancelAuctionFromRunning() {
        auction.start();
        auction.cancel();

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        assertFalse(auction.isOpen());
    }

    // Chỉ auction đã finish mới được đánh dấu là đã thanh toán.
    @Test
    void testMarkPaidFromFinished() {
        auction.start();
        auction.finish();
        auction.markPaid();

        assertEquals(AuctionStatus.PAID, auction.getStatus());
        assertFalse(auction.isOpen());
    }

    // Không được start một auction đã ở sai trạng thái vòng đời.
    @Test
    void testStartAuctionFromWrongStatusThrowsException() {
        auction.start();

        assertThrows(AuctionException.class, () -> auction.start());
    }

    // Không được finish auction khi phiên chưa chạy.
    @Test
    void testFinishAuctionFromWrongStatusThrowsException() {
        assertThrows(AuctionException.class, () -> auction.finish());
    }

    // Không được đánh dấu PAID nếu auction chưa hoàn tất.
    @Test
    void testMarkPaidFromWrongStatusThrowsException() {
        auction.start();

        assertThrows(AuctionException.class, () -> auction.markPaid());
    }

    // Một bid hợp lệ phải cập nhật số bid, current price và winner.
    @Test
    void testAddBidWhenAuctionIsRunning() {
        auction.start();
        BidTransaction bid = new BidTransaction(bidder1, 1200.0);

        auction.addBid(bid);

        assertEquals(1, auction.getBids().size());
        assertEquals(1200.0, auction.getCurrentPrice());
        assertEquals(bidder1, auction.getWinner());
    }

    // Nhiều bid hợp lệ liên tiếp phải luôn giữ bidder cuối cùng là người thắng tạm thời.
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

    // Không được nhận bid nếu auction chưa ở trạng thái RUNNING.
    @Test
    void testAddBidWhenAuctionIsNotRunningThrowsException() {
        BidTransaction bid = new BidTransaction(bidder1, 1200.0);

        assertThrows(AuctionClosedException.class, () -> auction.addBid(bid));
    }

    // Domain model phải từ chối bid null để tránh làm hỏng lịch sử đấu giá.
    @Test
    void testAddNullBidThrowsException() {
        auction.start();

        assertThrows(InvalidBidException.class, () -> auction.addBid(null));
    }

    // Giá bid thấp hơn current price phải bị chặn bởi rule nghiệp vụ.
    @Test
    void testAddLowerBidThrowsException() {
        auction.start();
        BidTransaction bid = new BidTransaction(bidder1, 900.0);

        assertThrows(InvalidBidException.class, () -> auction.addBid(bid));
    }

    // Observer phải được notify khi hệ thống ghi nhận một bid mới.
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

    // toString cần chứa thông tin chính để thuận tiện cho debug và log.
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
