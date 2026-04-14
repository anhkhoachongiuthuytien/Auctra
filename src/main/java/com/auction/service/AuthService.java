package com.auction.service;

import com.auction.dao.UserDao;
import com.auction.exception.AuthenticationException;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.util.IdGenerator;

public class AuthService {
    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public Seller registerSeller(String username, String email) {
        validateEmailNotExists(email);
        Seller seller = new Seller(IdGenerator.generateId(), username, email);
        userDao.save(seller);
        return seller;
    }

    public Bidder registerBidder(String username, String email) {
        validateEmailNotExists(email);
        Bidder bidder = new Bidder(IdGenerator.generateId(), username, email);
        userDao.save(bidder);
        return bidder;
    }

    public Admin registerAdmin(String username, String email) {
        validateEmailNotExists(email);
        Admin admin = new Admin(IdGenerator.generateId(), username, email);
        userDao.save(admin);
        return admin;
    }

    public User login(String email) {
        User user = userDao.findByEmail(email);
        if (user == null) {
            throw new AuthenticationException("Email does not exist");
        }
        return user;
    }

    public boolean emailExists(String email) {
        return userDao.findByEmail(email) != null;
    }

    private void validateEmailNotExists(String email) {
        if (emailExists(email)) {
            throw new AuthenticationException("Email already exists");
        }
    }
}
