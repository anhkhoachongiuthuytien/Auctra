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

    // Đảm bảo item đã lưu có thể được truy xuất lại bằng id.
    @Test
    void testFindById() {
        assertEquals(item, itemDao.findById("I001"));
    }

    // Kiểm tra DAO trả về đầy đủ các item hiện đang được lưu.
    @Test
    void testFindAll() {
        assertEquals(1, itemDao.findAll().size());
    }

    // Kiểm tra thao tác delete làm item biến mất khỏi kết quả truy vấn tiếp theo.
    @Test
    void testDelete() {
        itemDao.delete("I001");

        assertNull(itemDao.findById("I001"));
    }
}
