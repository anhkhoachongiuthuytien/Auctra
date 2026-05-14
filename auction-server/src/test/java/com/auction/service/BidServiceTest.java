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

    // Bid hợp lệ phải làm tăng current price và cập nhật winner.
    @Test
    void testPlaceValidBid() {
        auction.start();

        bidService.placeBid(auction.getId(), bidder1, 1200.0);

        assertEquals(1, auction.getBids().size());
        assertEquals(1200.0, auction.getCurrentPrice());
        assertEquals(bidder1, auction.getWinner());
    }

    // Service phải báo lỗi nếu client gửi auction id không tồn tại.
    @Test
    void testPlaceBidWhenAuctionNotFoundThrowsException() {
        assertThrows(AuctionException.class, () ->
                bidService.placeBid("INVALID_ID", bidder1, 1200.0));
    }

    // Auction chưa mở cho bidding thì mọi bid đều phải bị từ chối.
    @Test
    void testPlaceBidWhenAuctionClosedThrowsException() {
        assertThrows(AuctionClosedException.class, () ->
                bidService.placeBid(auction.getId(), bidder1, 1200.0));
    }

    // Bid thấp hơn giá hiện tại không được chấp nhận.
    @Test
    void testPlaceLowerBidThrowsException() {
        auction.start();

        assertThrows(InvalidBidException.class, () ->
                bidService.placeBid(auction.getId(), bidder1, 900.0));
    }

    // Bid bằng đúng current price cũng phải bị chặn theo rule "phải cao hơn".
    @Test
    void testPlaceEqualBidThrowsException() {
        auction.start();

        assertThrows(InvalidBidException.class, () ->
                bidService.placeBid(auction.getId(), bidder1, 1000.0));
    }

    // Nhiều bid hợp lệ liên tiếp phải để lại winner cuối cùng với giá cao nhất.
    @Test
    void testPlaceMultipleValidBids() {
        auction.start();

        bidService.placeBid(auction.getId(), bidder1, 1200.0);
        bidService.placeBid(auction.getId(), bidder2, 1500.0);

        assertEquals(2, auction.getBids().size());
        assertEquals(1500.0, auction.getCurrentPrice());
        assertEquals(bidder2, auction.getWinner());
    }

    // Bidder null phải bị chặn trước khi truy cập vào domain model.
    @Test
    void testPlaceBidWithNullBidderThrowsValidationException() {
        auction.start();

        assertThrows(ValidationException.class, () ->
                bidService.placeBid(auction.getId(), null, 1200.0));
    }

    // Amount không hợp lệ như 0 hoặc âm phải fail ở tầng validation.
    @Test
    void testPlaceBidWithInvalidAmountThrowsValidationException() {
        auction.start();

        assertThrows(ValidationException.class, () ->
                bidService.placeBid(auction.getId(), bidder1, 0));
    }
}
