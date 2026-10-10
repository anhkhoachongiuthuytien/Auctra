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
        saveRegistration(seller, password);
        return seller;
    }

    public Bidder registerBidder(String username, String email, String password) {
        validateUserInput(username, email, password);
        validateEmailNotExists(email);
        Bidder bidder = new Bidder(IdGenerator.generateId(), username, email);
        saveRegistration(bidder, password);
        return bidder;
    }

    public Admin registerAdmin(String username, String email, String password) {
        validateUserInput(username, email, password);
        validateEmailNotExists(email);
        Admin admin = new Admin(IdGenerator.generateId(), username, email);
        saveRegistration(admin, password);
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

    private void saveRegistration(User user, String password) {
        String recoveryCode = java.util.UUID.randomUUID().toString().replace("-", "");
        userDao.save(user, PasswordHasher.hash(password), PasswordHasher.hash(recoveryCode));
        user.setRecoveryCode(recoveryCode);
    }

    /** A recovery code is secret, returned once on registration, and consumed atomically. */
    public void resetPassword(String email, String recoveryCode, String newPassword) {
        if (email == null || email.isBlank()) { throw new ValidationException("Email không được để trống"); }
        if (recoveryCode == null || recoveryCode.isBlank()) {
            throw new ValidationException("Mã khôi phục không được để trống");
        }
        validatePassword(newPassword);
        String recoveryHash = userDao.findRecoveryCodeHashByEmail(email);
        if (!PasswordHasher.matches(recoveryCode.trim(), recoveryHash)
                || !userDao.resetPasswordWithRecoveryCode(email, recoveryHash, PasswordHasher.hash(newPassword))) {
            throw new AuthenticationException("Email hoặc mã khôi phục không đúng, hoặc mã đã được sử dụng");
        }
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
