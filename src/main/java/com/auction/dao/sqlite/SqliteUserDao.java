package com.auction.dao.sqlite;

import com.auction.dao.UserDao;
import com.auction.db.DatabaseManager;
import com.auction.db.DbMappers;
import com.auction.model.user.User;

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
        String sql = """
                INSERT INTO users(id, username, email, role)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    username = excluded.username,
                    email = excluded.email,
                    role = excluded.role
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getId());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getEmail());
            statement.setString(4, DbMappers.detectRole(user));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save user", e);
        }
    }

    @Override
    public User findById(String id) {
        return findSingle("SELECT id, username, email, role FROM users WHERE id = ?", id);
    }

    @Override
    public User findByEmail(String email) {
        return findSingle("SELECT id, username, email, role FROM users WHERE email = ?", email);
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, username, email, role FROM users ORDER BY username";
        List<User> users = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(DbMappers.createUser(
                        resultSet.getString("role"),
                        resultSet.getString("id"),
                        resultSet.getString("username"),
                        resultSet.getString("email")
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
                            resultSet.getString("email")
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query user", e);
        }
    }
}
