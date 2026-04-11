package com.auction.dao;
import  com.auction.model.user.User;
import java.util.List;

public interface UserDao {
    void save(User user);
    User findById(String id);
    User findByEmail(string email);
    List<User> findAll();
}