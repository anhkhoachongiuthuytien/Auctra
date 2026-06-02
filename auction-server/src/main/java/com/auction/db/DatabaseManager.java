package com.auction.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseManager {
    private final String jdbcUrl;

    public DatabaseManager(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            // Bật foreign key trên từng connection để SQLite thật sự kiểm tra quan hệ giữa các bảng.
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public void initializeSchema() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            // Đọc schema.sql rồi chạy từng câu lệnh PRAGMA/CREATE TABLE theo thứ tự.
            for (String sql : loadSchemaSql().split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
            ensureUsersPasswordHashColumn(connection);
            ensureUsersColumn(connection, "shipping_address", "TEXT");
            ensureUsersColumn(connection, "phone_number", "TEXT");
            ensureUsersColumn(connection, "store_name", "TEXT");
            ensureUsersColumn(connection, "store_description", "TEXT");
            ensureUsersColumn(connection, "department", "TEXT");
            ensureUsersColumn(connection, "avatar_path", "TEXT");
            ensureItemsImagePathColumn(connection);
            ensureAuctionsEndTimeColumn(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }

    private void ensureUsersColumn(Connection connection, String columnName, String columnDefinition) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(users)")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return;
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE users ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private void ensureUsersPasswordHashColumn(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(users)")) {
            while (resultSet.next()) {
                if ("password_hash".equalsIgnoreCase(resultSet.getString("name"))) {
                    return;
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE users ADD COLUMN password_hash TEXT NOT NULL DEFAULT ''");
        }
    }

    private void ensureItemsImagePathColumn(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(items)")) {
            while (resultSet.next()) {
                if ("image_path".equalsIgnoreCase(resultSet.getString("name"))) {
                    return;
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE items ADD COLUMN image_path TEXT");
        }
    }

    private void ensureAuctionsEndTimeColumn(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(auctions)")) {
            while (resultSet.next()) {
                if ("end_time".equalsIgnoreCase(resultSet.getString("name"))) {
                    return;
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE auctions ADD COLUMN end_time TEXT");
        }
    }

    private String loadSchemaSql() {
        // Nạp file schema từ resources để ứng dụng có thể tự khởi tạo database khi chạy lần đầu.
        InputStream inputStream = getClass().getResourceAsStream("/db/schema.sql");
        if (inputStream == null) {
            throw new IllegalStateException("Schema resource not found");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load schema resource", e);
        }
    }
}
