package com.auction.dao;

import com.auction.model.user.User;

import java.util.List;

public interface UserDao {
    void save(User user);

    void save(User user, String passwordHash);

    User findById(String id);

    User findByEmail(String email);

    String findPasswordHashByEmail(String email);

    void updatePasswordHash(String email, String passwordHash);

    void save(User user, String passwordHash, String recoveryCodeHash);

    String findRecoveryCodeHashByEmail(String email);

    boolean resetPasswordWithRecoveryCode(String email, String expectedRecoveryHash, String passwordHash);

    List<User> findAll();
}
