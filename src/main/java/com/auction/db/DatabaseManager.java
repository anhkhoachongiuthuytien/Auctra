package com.auction.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
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
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public void initializeSchema() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : loadSchemaSql().split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }

    private String loadSchemaSql() {
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
