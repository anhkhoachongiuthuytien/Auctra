package com.auction.dao.sqlite;

import com.auction.dao.UserDao;
import com.auction.db.DatabaseManager;
import com.auction.db.DbMappers;
import com.auction.model.user.User;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.Admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqliteUserDao implements UserDao {
    private final DatabaseManager databaseManager;

    public SqliteUserDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(User user) {
        // Upsert user without changing the stored password hash.
        String sql = """
                INSERT INTO users(id, username, email, role, shipping_address, phone_number, store_name, store_description, department, avatar_path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    username = excluded.username,
                    email = excluded.email,
                    role = excluded.role,
                    shipping_address = excluded.shipping_address,
                    phone_number = excluded.phone_number,
                    store_name = excluded.store_name,
                    store_description = excluded.store_description,
                    department = excluded.department,
                    avatar_path = excluded.avatar_path
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getId());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getEmail());
            statement.setString(4, DbMappers.detectRole(user));

            if (user instanceof Bidder bidder) {
                statement.setString(5, bidder.getShippingAddress());
                statement.setString(6, bidder.getPhoneNumber());
                statement.setString(7, null);
                statement.setString(8, null);
                statement.setString(9, null);
            } else if (user instanceof Seller seller) {
                statement.setString(5, null);
                statement.setString(6, null);
                statement.setString(7, seller.getStoreName());
                statement.setString(8, seller.getStoreDescription());
                statement.setString(9, null);
            } else if (user instanceof Admin admin) {
                statement.setString(5, null);
                statement.setString(6, null);
                statement.setString(7, null);
                statement.setString(8, null);
                statement.setString(9, admin.getDepartment());
            } else {
                statement.setString(5, null);
                statement.setString(6, null);
                statement.setString(7, null);
                statement.setString(8, null);
                statement.setString(9, null);
            }
            statement.setString(10, user.getAvatarPath());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save user", e);
        }
    }

    @Override
    public void save(User user, String passwordHash) {
         // Upsert user together with a password hash for register and password bootstrap flows.
         String sql = """
                 INSERT INTO users(id, username, email, role, password_hash, shipping_address, phone_number, store_name, store_description, department, avatar_path)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                 ON CONFLICT(id) DO UPDATE SET
                     username = excluded.username,
                     email = excluded.email,
                     role = excluded.role,
                     password_hash = excluded.password_hash,
                     shipping_address = excluded.shipping_address,
                     phone_number = excluded.phone_number,
                     store_name = excluded.store_name,
                     store_description = excluded.store_description,
                     department = excluded.department,
                     avatar_path = excluded.avatar_path
                 """;

         try (Connection connection = databaseManager.getConnection();
              PreparedStatement statement = connection.prepareStatement(sql)) {
             statement.setString(1, user.getId());
             statement.setString(2, user.getUsername());
             statement.setString(3, user.getEmail());
             statement.setString(4, DbMappers.detectRole(user));
             statement.setString(5, passwordHash);

             if (user instanceof Bidder bidder) {
                 statement.setString(6, bidder.getShippingAddress());
                 statement.setString(7, bidder.getPhoneNumber());
                 statement.setString(8, null);
                 statement.setString(9, null);
                 statement.setString(10, null);
             } else if (user instanceof Seller seller) {
                 statement.setString(6, null);
                 statement.setString(7, null);
                 statement.setString(8, seller.getStoreName());
                 statement.setString(9, seller.getStoreDescription());
                 statement.setString(10, null);
             } else if (user instanceof Admin admin) {
                 statement.setString(6, null);
                 statement.setString(7, null);
                 statement.setString(8, null);
                 statement.setString(9, null);
                 statement.setString(10, admin.getDepartment());
             } else {
                 statement.setString(6, null);
                 statement.setString(7, null);
                 statement.setString(8, null);
                 statement.setString(9, null);
                 statement.setString(10, null);
             }
             statement.setString(11, user.getAvatarPath());

             statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save user", e);
        }
    }

    @Override
    public User findById(String id) {
        return findSingle("SELECT id, username, email, role, shipping_address, phone_number, store_name, store_description, department, avatar_path FROM users WHERE id = ?", id);
    }

    @Override
    public User findByEmail(String email) {
        return findSingle("SELECT id, username, email, role, shipping_address, phone_number, store_name, store_description, department, avatar_path FROM users WHERE email = ?", email);
    }

    @Override
    public String findPasswordHashByEmail(String email) {
        String sql = "SELECT password_hash FROM users WHERE email = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("password_hash");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query password hash", e);
        }
    }

    @Override
    public void updatePasswordHash(String email, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE email = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, email);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update password hash", e);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, username, email, role, shipping_address, phone_number, store_name, store_description, department, avatar_path FROM users ORDER BY username";
        List<User> users = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(DbMappers.createUser(
                        resultSet.getString("role"),
                        resultSet.getString("id"),
                        resultSet.getString("username"),
                        resultSet.getString("email"),
                        resultSet.getString("shipping_address"),
                        resultSet.getString("phone_number"),
                        resultSet.getString("store_name"),
                        resultSet.getString("store_description"),
                        resultSet.getString("department"),
                        resultSet.getString("avatar_path")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load users", e);
        }

        return users;
    }

    private User findSingle(String sql, String value) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return DbMappers.createUser(
                            resultSet.getString("role"),
                            resultSet.getString("id"),
                            resultSet.getString("username"),
                            resultSet.getString("email"),
                            resultSet.getString("shipping_address"),
                            resultSet.getString("phone_number"),
                            resultSet.getString("store_name"),
                            resultSet.getString("store_description"),
                            resultSet.getString("department"),
                            resultSet.getString("avatar_path")
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query user", e);
        }
    }
}
