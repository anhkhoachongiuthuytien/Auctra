package com.auction.dao;

import com.auction.dao.memory.InMemoryUserDao;
import com.auction.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InMemoryUserDaoTest {

    private InMemoryUserDao userDao;
    private Seller seller;

    @BeforeEach
    void setUp() {
        userDao = new InMemoryUserDao();
        seller = new Seller("S001", "seller", "seller@test.com");
        userDao.save(seller);
    }

    // Đảm bảo tìm theo id trả về đúng user đã được lưu.
    @Test
    void testFindById() {
        assertEquals(seller, userDao.findById("S001"));
    }

    // Đảm bảo truy vấn theo email hoạt động đúng với dữ liệu hiện có.
    @Test
    void testFindByEmail() {
        assertEquals(seller, userDao.findByEmail("seller@test.com"));
    }

    // Kiểm tra danh sách người dùng phản ánh đúng số phần tử được lưu.
    @Test
    void testFindAll() {
        assertEquals(1, userDao.findAll().size());
    }

    // Khi email không tồn tại, DAO phải trả về null thay vì ném lỗi.
    @Test
    void testFindMissingUserReturnsNull() {
        assertNull(userDao.findByEmail("missing@test.com"));
    }
}
