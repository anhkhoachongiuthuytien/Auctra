package com.auction.dao;
import com.auction.model.item.Item;
import java.util.List;

public interface ItemDao {
    void save(Item item);
    Item findById(String id);
    List<Item> findAll();
    void delete(String id);
}