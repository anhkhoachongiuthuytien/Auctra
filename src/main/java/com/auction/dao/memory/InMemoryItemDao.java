package com.auction.dao;
import com.auction.dao.ItemDdao;
import com.auction.model.item.Item;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class InMemoryItemDaoTest implements ItemDao{
    HashMap<String, Item> database = new HashMap<>();
    public void save(Item item){
        database.put(item.getId(), Item);
    }
    public Item findById(String id){
        return database.get(id);
    }
    public List<Item> findAll(){
        return new ArrayList<>(database.values);
    }
    public void delete(String id){
        database.remove(id);
    }
}