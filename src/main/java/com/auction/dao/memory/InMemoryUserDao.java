package com.auction.dao.memory;

import com.auction.dao.UserDao;
import com.auction.model.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryUserDao implements UserDao {
    private final HashMap<String, User> database = new HashMap<>();
    private final Map<String, String> passwordHashesByEmail = new HashMap<>();

    @Override
    public void save(User user) {
        database.put(user.getId(), user);
    }

    @Override
    public void save(User user, String passwordHash) {
        save(user);
        passwordHashesByEmail.put(user.getEmail(), passwordHash);
    }

    @Override
    public User findById(String id) {
        return database.get(id);
    }

    @Override
    public User findByEmail(String email) {
        for (User user : database.values()) {
            if (user.getEmail().equals(email)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public String findPasswordHashByEmail(String email) {
        return passwordHashesByEmail.get(email);
    }

    @Override
    public void updatePasswordHash(String email, String passwordHash) {
        if (findByEmail(email) != null) {
            passwordHashesByEmail.put(email, passwordHash);
        }
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(database.values());
    }
}
