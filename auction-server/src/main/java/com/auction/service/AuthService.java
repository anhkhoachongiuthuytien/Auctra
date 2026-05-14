package com.auction.service;

import com.auction.dao.UserDao;
import com.auction.exception.AuthenticationException;
import com.auction.exception.ValidationException;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.util.IdGenerator;
import com.auction.util.PasswordHasher;

public class AuthService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public Seller registerSeller(String username, String email, String password) {
        validateUserInput(username, email, password);
        validateEmailNotExists(email);
        Seller seller = new Seller(IdGenerator.generateId(), username, email);
        userDao.save(seller, PasswordHasher.hash(password));
        return seller;
    }

    public Bidder registerBidder(String username, String email, String password) {
        validateUserInput(username, email, password);
        validateEmailNotExists(email);
        Bidder bidder = new Bidder(IdGenerator.generateId(), username, email);
        userDao.save(bidder, PasswordHasher.hash(password));
        return bidder;
    }

    public Admin registerAdmin(String username, String email, String password) {
        validateUserInput(username, email, password);
        validateEmailNotExists(email);
        Admin admin = new Admin(IdGenerator.generateId(), username, email);
        userDao.save(admin, PasswordHasher.hash(password));
        return admin;
    }

    public User login(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email không được để trống");
        }
        validatePassword(password);
        User user = userDao.findByEmail(email);
        if (user == null) {
            throw new AuthenticationException("Email chưa được đăng ký");
        }
        String passwordHash = userDao.findPasswordHashByEmail(email);
        if (!PasswordHasher.matches(password, passwordHash)) {
            throw new AuthenticationException("Mật khẩu không đúng");
        }
        return user;
    }

    public boolean emailExists(String email) {
        return userDao.findByEmail(email) != null;
    }

    public void ensurePassword(String email, String password) {
        if (!emailExists(email)) {
            throw new AuthenticationException("Email chưa được đăng ký");
        }
        if (!hasPassword(email)) {
            validatePassword(password);
            userDao.updatePasswordHash(email, PasswordHasher.hash(password));
        }
    }

    /**
     * Đặt lại mật khẩu cho tài khoản. Vì app local không có email service,
     * user phải cung cấp đúng username của chính tài khoản đó để chứng minh quyền sở hữu.
     */
    public void resetPassword(String email, String username, String newPassword) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email không được để trống");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Tên đăng nhập không được để trống");
        }
        validatePassword(newPassword);

        User user = userDao.findByEmail(email);
        if (user == null) {
            throw new AuthenticationException("Email chưa được đăng ký");
        }
        if (!user.getUsername().equals(username.trim())) {
            throw new AuthenticationException("Tên đăng nhập không khớp với email này");
        }
        userDao.updatePasswordHash(email, PasswordHasher.hash(newPassword));
    }

    public boolean hasPassword(String email) {
        String passwordHash = userDao.findPasswordHashByEmail(email);
        return passwordHash != null && !passwordHash.isBlank();
    }

    private void validateEmailNotExists(String email) {
        if (emailExists(email)) {
            throw new AuthenticationException("Email đã tồn tại");
        }
    }

    private void validateUserInput(String username, String email, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Tên đăng nhập không được để trống");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email không được để trống");
        }
        validatePassword(password);
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Mật khẩu không được để trống");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Mật khẩu phải có ít nhất 8 ký tự");
        }
    }
}
