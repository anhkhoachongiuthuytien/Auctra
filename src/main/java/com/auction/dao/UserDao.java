package com.auction.dao;
import package com.auction.model.user.User;
import java.util.List;

public interface ItemDao {
    void save(User user);
    User findById(String id);
    User findByEmail(string email);
    List<User> findAll();
}