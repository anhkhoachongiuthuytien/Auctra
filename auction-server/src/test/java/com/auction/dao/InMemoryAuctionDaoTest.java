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

    // Đảm bảo DAO có thể tìm lại đúng auction đã lưu theo id.
    @Test
    void testFindById() {
        assertEquals(auction, auctionDao.findById("A001"));
    }

    // Kiểm tra danh sách trả về phản ánh đúng số bản ghi đang có trong bộ nhớ.
    @Test
    void testFindAll() {
        assertEquals(1, auctionDao.findAll().size());
    }

    // Kiểm tra thao tác xóa thực sự loại bỏ auction khỏi kho lưu trữ in-memory.
    @Test
    void testDelete() {
        auctionDao.delete("A001");

        assertNull(auctionDao.findById("A001"));
    }
}
