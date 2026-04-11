package com.auction.service;
import com.auction.dao.UserDao;
import com.auction.model.user.User;

public class AuthService {
    private UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }
    public User login(String username) {
        return userDao.findByEmail(username) ;
    }
}
