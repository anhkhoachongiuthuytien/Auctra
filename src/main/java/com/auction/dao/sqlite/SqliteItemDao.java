package com.auction.dao.sqlite;

import com.auction.dao.ItemDao;
import com.auction.db.DatabaseManager;
import com.auction.db.DbMappers;
import com.auction.model.item.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqliteItemDao implements ItemDao {
    private final DatabaseManager databaseManager;

    public SqliteItemDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(Item item) {
        // Upsert item để cùng một câu SQL xử lý cả thêm mới lẫn cập nhật vật phẩm.
        String sql = """
                INSERT INTO items(id, name, description, starting_price, type)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    name = excluded.name,
                    description = excluded.description,
                    starting_price = excluded.starting_price,
                    type = excluded.type
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, item.getId());
            statement.setString(2, item.getName());
            statement.setString(3, item.getDescription());
            statement.setDouble(4, item.getStartingPrice());
            statement.setString(5, DbMappers.detectItemType(item));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save item", e);
        }
    }

    @Override
    public Item findById(String id) {
        // Tìm một item theo id để dựng lại auction hoặc mở chi tiết vật phẩm.
        String sql = "SELECT id, name, description, starting_price, type FROM items WHERE id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return DbMappers.createItem(
                            resultSet.getString("type"),
                            resultSet.getString("id"),
                            resultSet.getString("name"),
                            resultSet.getString("description"),
                            resultSet.getDouble("starting_price")
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find item", e);
        }
    }

    @Override
    public List<Item> findAll() {
        // Lấy toàn bộ item, sắp xếp theo tên để danh sách hiển thị dễ đọc hơn.
        String sql = "SELECT id, name, description, starting_price, type FROM items ORDER BY name";
        List<Item> items = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                items.add(DbMappers.createItem(
                        resultSet.getString("type"),
                        resultSet.getString("id"),
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        resultSet.getDouble("starting_price")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list items", e);
        }

        return items;
    }

    @Override
    public void delete(String id) {
        // Xóa item theo id khi vật phẩm không còn cần lưu trong hệ thống.
        String sql = "DELETE FROM items WHERE id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete item", e);
        }
    }
}
