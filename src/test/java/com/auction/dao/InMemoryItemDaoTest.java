package com.auction.dao;

import com.auction.dao.memory.InMemoryItemDao;
import com.auction.model.item.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InMemoryItemDaoTest {

    private InMemoryItemDao itemDao;
    private Item item;

    @BeforeEach
    void setUp() {
        itemDao = new InMemoryItemDao();
        item = new Item("I001", "Laptop", "Gaming laptop", 1000.0);
        itemDao.save(item);
    }

    @Test
    void testFindById() {
        assertEquals(item, itemDao.findById("I001"));
    }

    @Test
    void testFindAll() {
        assertEquals(1, itemDao.findAll().size());
    }

    @Test
    void testDelete() {
        itemDao.delete("I001");

        assertNull(itemDao.findById("I001"));
    }
}
