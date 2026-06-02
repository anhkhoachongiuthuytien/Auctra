package com.auction.dao.sqlite;

import com.auction.dao.AutoBidDao;
import com.auction.db.DatabaseManager;
import com.auction.model.auction.AutoBidConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqliteAutoBidDao implements AutoBidDao {
    private final DatabaseManager databaseManager;

    public SqliteAutoBidDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(AutoBidConfig config) {
        String sql = """
                INSERT INTO auto_bids(auction_id, bidder_id, max_price, increment)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(auction_id, bidder_id) DO UPDATE SET
                    max_price = excluded.max_price,
                    increment = excluded.increment
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, config.getAuctionId());
            statement.setString(2, config.getBidderId());
            statement.setDouble(3, config.getMaxPrice());
            statement.setDouble(4, config.getIncrement());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save auto bid config", e);
        }
    }

    @Override
    public List<AutoBidConfig> getAutoBidsForAuction(String auctionId) {
        String sql = "SELECT auction_id, bidder_id, max_price, increment FROM auto_bids WHERE auction_id = ?";
        List<AutoBidConfig> configs = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    configs.add(new AutoBidConfig(
                            resultSet.getString("auction_id"),
                            resultSet.getString("bidder_id"),
                            resultSet.getDouble("max_price"),
                            resultSet.getDouble("increment")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load auto bids for auction", e);
        }

        return configs;
    }

    @Override
    public AutoBidConfig find(String auctionId, String bidderId) {
        String sql = "SELECT auction_id, bidder_id, max_price, increment FROM auto_bids WHERE auction_id = ? AND bidder_id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionId);
            statement.setString(2, bidderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new AutoBidConfig(
                            resultSet.getString("auction_id"),
                            resultSet.getString("bidder_id"),
                            resultSet.getDouble("max_price"),
                            resultSet.getDouble("increment")
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query auto bid config", e);
        }
    }

    @Override
    public void delete(String auctionId, String bidderId) {
        String sql = "DELETE FROM auto_bids WHERE auction_id = ? AND bidder_id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionId);
            statement.setString(2, bidderId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete auto bid config", e);
        }
    }
}
