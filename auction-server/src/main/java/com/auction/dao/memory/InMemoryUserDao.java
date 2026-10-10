package com.auction.dao.memory;

import com.auction.dao.UserDao;
import com.auction.model.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserDao implements UserDao {
    private final Map<String, User> database = new ConcurrentHashMap<>();
    private final Map<String, String> passwords = new ConcurrentHashMap<>();
    private final Map<String, String> recoveryCodes = new ConcurrentHashMap<>();

    @Override
    public void save(User user) { database.put(user.getId(), user); }

    @Override
    public void save(User user, String passwordHash) {
        save(user);
        passwords.put(user.getId(), passwordHash);
    }

    @Override
    public void save(User user, String passwordHash, String recoveryHash) {
        save(user, passwordHash);
        if (recoveryHash != null) { recoveryCodes.put(user.getId(), recoveryHash); }
    }

    @Override
    public User findById(String id) { return id == null ? null : database.get(id); }

    @Override
    public User findByEmail(String email) {
        return database.values().stream().filter(u -> u.getEmail().equals(email)).findFirst().orElse(null);
    }

    @Override
    public String findPasswordHashByEmail(String email) {
        User user = findByEmail(email);
        return user == null ? null : passwords.get(user.getId());
    }

    @Override
    public void updatePasswordHash(String email, String hash) {
        User user = findByEmail(email);
        if (user != null) { passwords.put(user.getId(), hash); }
    }

    @Override
    public String findRecoveryCodeHashByEmail(String email) {
        User user = findByEmail(email);
        return user == null ? null : recoveryCodes.get(user.getId());
    }

    @Override
    public synchronized boolean resetPasswordWithRecoveryCode(String email, String expectedHash, String passwordHash) {
        User user = findByEmail(email);
        if (user == null || expectedHash == null || !expectedHash.equals(recoveryCodes.get(user.getId()))) { return false; }
        passwords.put(user.getId(), passwordHash);
        recoveryCodes.remove(user.getId());
        return true;
    }

    @Override
    public List<User> findAll() { return new ArrayList<>(database.values()); }
}
