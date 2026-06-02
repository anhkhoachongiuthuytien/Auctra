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

    public User updateUser(String userId, String username, String email) {
        return updateUser(userId, username, email, null, null, null, null, null, null);
    }

    public User updateUser(String userId, String username, String email,
                           String shippingAddress, String phoneNumber,
                           String storeName, String storeDescription,
                           String department) {
        return updateUser(userId, username, email, shippingAddress, phoneNumber, storeName, storeDescription, department, null);
    }

    public User updateUser(String userId, String username, String email,
                           String shippingAddress, String phoneNumber,
                           String storeName, String storeDescription,
                           String department, String avatarPath) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new com.auction.exception.ValidationException("Mã người dùng không được để trống");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new com.auction.exception.ValidationException("Tên đăng nhập không được để trống");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new com.auction.exception.ValidationException("Email không được để trống");
        }
        User existingUser = userDao.findById(userId);
        if (existingUser == null) {
            throw new com.auction.exception.AuthenticationException("Không tìm thấy người dùng");
        }
        // Kiểm tra trùng email nếu email thay đổi
        if (!existingUser.getEmail().equalsIgnoreCase(email)) {
            User userWithEmail = userDao.findByEmail(email);
            if (userWithEmail != null && !userWithEmail.getId().equals(userId)) {
                throw new com.auction.exception.AuthenticationException("Email đã được sử dụng bởi người dùng khác");
            }
        }
        existingUser.setUsername(username.trim());
        existingUser.setEmail(email.trim());
        if (avatarPath != null) {
            existingUser.setAvatarPath(avatarPath);
        }

        if (existingUser instanceof com.auction.model.user.Bidder bidder) {
            bidder.setShippingAddress(shippingAddress != null ? shippingAddress.trim() : null);
            bidder.setPhoneNumber(phoneNumber != null ? phoneNumber.trim() : null);
        } else if (existingUser instanceof com.auction.model.user.Seller seller) {
            seller.setStoreName(storeName != null ? storeName.trim() : null);
            seller.setStoreDescription(storeDescription != null ? storeDescription.trim() : null);
        } else if (existingUser instanceof com.auction.model.user.Admin admin) {
            admin.setDepartment(department != null ? department.trim() : null);
        }

        userDao.save(existingUser);
        return existingUser;
    }
}
