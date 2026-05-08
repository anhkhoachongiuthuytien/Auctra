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

    // Tạo auction mới phải sinh id, gắn item/seller và lưu xuống DAO.
    @Test
    void testCreateAuction() {
        Auction auction = auctionService.createAuction(item, seller);

        assertNotNull(auction);
        assertNotNull(auction.getId());
        assertEquals(item, auction.getItem());
        assertEquals(seller, auction.getSeller());
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals(auction, auctionDao.findById(auction.getId()));
    }

    // Service phải start được auction tồn tại.
    @Test
    void testStartAuction() {
        Auction auction = auctionService.createAuction(item, seller);

        auctionService.startAuction(auction.getId());

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    // Service phải finish được auction đang chạy.
    @Test
    void testFinishAuction() {
        Auction auction = auctionService.createAuction(item, seller);
        auctionService.startAuction(auction.getId());

        auctionService.finishAuction(auction.getId());

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    // Service phải hủy được auction và cập nhật trạng thái tương ứng.
    @Test
    void testCancelAuction() {
        Auction auction = auctionService.createAuction(item, seller);

        auctionService.cancelAuction(auction.getId());

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    // Chỉ auction đã finish mới được đánh dấu PAID qua service.
    @Test
    void testMarkAuctionPaid() {
        Auction auction = auctionService.createAuction(item, seller);
        auctionService.startAuction(auction.getId());
        auctionService.finishAuction(auction.getId());

        auctionService.markAuctionPaid(auction.getId());

        assertEquals(AuctionStatus.PAID, auction.getStatus());
    }

    // Danh sách auction phải phản ánh đúng dữ liệu mà service đã tạo.
    @Test
    void testListAuctions() {
        auctionService.createAuction(item, seller);

        assertEquals(1, auctionService.listAuctions().size());
    }

    // Start trên id không tồn tại phải báo lỗi thay vì im lặng bỏ qua.
    @Test
    void testStartAuctionWhenAuctionNotFoundThrowsException() {
        assertThrows(AuctionException.class, () -> auctionService.startAuction("INVALID_ID"));
    }

    // Finish trên id không tồn tại phải ném exception rõ ràng.
    @Test
    void testFinishAuctionWhenAuctionNotFoundThrowsException() {
        assertThrows(AuctionException.class, () -> auctionService.finishAuction("INVALID_ID"));
    }

    // Cancel trên id không tồn tại phải trả về lỗi nghiệp vụ.
    @Test
    void testCancelAuctionWhenAuctionNotFoundThrowsException() {
        assertThrows(AuctionException.class, () -> auctionService.cancelAuction("INVALID_ID"));
    }

    // Mark paid trên id không tồn tại phải bị chặn ở service layer.
    @Test
    void testMarkAuctionPaidWhenAuctionNotFoundThrowsException() {
        assertThrows(AuctionException.class, () -> auctionService.markAuctionPaid("INVALID_ID"));
    }

    // Không được finish auction khi nó chưa chuyển sang trạng thái RUNNING.
    @Test
    void testFinishAuctionFromWrongStateThrowsException() {
        Auction auction = auctionService.createAuction(item, seller);

        assertThrows(AuctionException.class, () -> auctionService.finishAuction(auction.getId()));
    }

    // Không được mark paid nếu vòng đời auction chưa đi qua bước FINISHED.
    @Test
    void testMarkAuctionPaidFromWrongStateThrowsException() {
        Auction auction = auctionService.createAuction(item, seller);

        assertThrows(AuctionException.class, () -> auctionService.markAuctionPaid(auction.getId()));
    }
}
