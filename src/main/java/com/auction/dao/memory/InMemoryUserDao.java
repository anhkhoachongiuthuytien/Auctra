package com.auction.dao;
import com.aution.dao.UserDao;
import com.auction.model.user.User;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class InMemoryUserDao implements UserDao{
    HashMap<String, User> database = new HashMap<>();
    public void save(User user){
        database.put(user.getId(), User);
    }
    public User findById(String id){
        return database.get(id);
    }
    public User findByEmail(String email){
        return database.get(email);
    }
    public List<User> findAll(){
        return new ArrayList<>(database.values());
    }
}