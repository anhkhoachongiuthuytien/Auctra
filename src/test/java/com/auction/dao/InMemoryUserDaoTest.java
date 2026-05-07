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

    @Test
    void testFindById() {
        assertEquals(seller, userDao.findById("S001"));
    }

    @Test
    void testFindByEmail() {
        assertEquals(seller, userDao.findByEmail("seller@test.com"));
    }

    @Test
    void testFindAll() {
        assertEquals(1, userDao.findAll().size());
    }

    @Test
    void testFindMissingUserReturnsNull() {
        assertNull(userDao.findByEmail("missing@test.com"));
    }
}
