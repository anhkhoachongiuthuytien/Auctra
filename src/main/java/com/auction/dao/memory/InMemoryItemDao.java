package com.auction.dao.memory;
import com.auction.dao.ItemDao;
import com.auction.model.item.Item;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class InMemoryItemDao implements ItemDao {
    HashMap<String, Item> database = new HashMap<>();
    public void save(Item item){
        database.put(item.getId(), item);
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