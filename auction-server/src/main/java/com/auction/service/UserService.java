package com.auction.service;

import com.auction.dao.UserDao;
import com.auction.exception.AuthenticationException;
import com.auction.model.user.User;

import java.util.List;

public class UserService {
    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User getUserById(String id) {
        User user = userDao.findById(id);
        if (user == null) {
            throw new AuthenticationException("Không tìm thấy người dùng");
        }
        return user;
    }

    public User getUserByEmail(String email) {
        User user = userDao.findByEmail(email);
        if (user == null) {
            throw new AuthenticationException("Không tìm thấy người dùng");
        }
        return user;
    }

    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    public boolean existsByEmail(String email) {
        return userDao.findByEmail(email) != null;
    }
}
