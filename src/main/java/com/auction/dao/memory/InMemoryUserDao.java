package com.auction.dao.memory;

import com.auction.dao.UserDao;
import com.auction.model.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InMemoryUserDao implements UserDao {
    private final HashMap<String, User> database = new HashMap<>();

    public void save(User user) {
        database.put(user.getId(), user);
    }

    public User findById(String id) {
        return database.get(id);
    }

    public User findByEmail(String email) {
        for (User user : database.values()) {
            if (user.getEmail().equals(email)) {
                return user;
            }
        }
        return null;
    }

    public List<User> findAll() {
        return new ArrayList<>(database.values());
    }
}
