package com.auction.service;
import com.auction.model.user.User;
import com.auction.dao.UserDao;

public class UserService {
    private UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }
    public void register(User user) {
        userDao.save(user);
    }
    public User findById(String id){
        return userDao.findById(id) ;
    }
}