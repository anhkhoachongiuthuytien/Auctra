package com.auction.dao.sqlite;

import com.auction.dao.AuctionDao;
import com.auction.db.DatabaseManager;
import com.auction.enums.AuctionStatus;
import com.auction.model.auction.Auction;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SqliteAuctionDao implements AuctionDao {
    private final DatabaseManager databaseManager;
    private final SqliteItemDao itemDao;
    private final SqliteUserDao userDao;

    public SqliteAuctionDao(DatabaseManager databaseManager, SqliteItemDao itemDao, SqliteUserDao userDao) {
        this.databaseManager = databaseManager;
        this.itemDao = itemDao;
        this.userDao = userDao;
    }

    @Override
    public void save(Auction auction) {
        String sql = """
                INSERT INTO auctions(id, item_id, seller_id, current_price, status, winner_id)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    item_id = excluded.item_id,
                    seller_id = excluded.seller_id,
                    current_price = excluded.current_price,
                    status = excluded.status,
                    winner_id = excluded.winner_id
                """;

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, auction.getId());
                statement.setString(2, auction.getItem().getId());
                statement.setString(3, auction.getSeller().getId());
                statement.setDouble(4, auction.getCurrentPrice());
                statement.setString(5, auction.getStatus().name());
                statement.setString(6, auction.getWinner() == null ? null : auction.getWinner().getId());
                statement.executeUpdate();
            }

            try (PreparedStatement deleteBids = connection.prepareStatement("DELETE FROM bids WHERE auction_id = ?")) {
                deleteBids.setString(1, auction.getId());
                deleteBids.executeUpdate();
            }

            String insertBidSql = "INSERT INTO bids(auction_id, bidder_id, amount, bid_time) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertBid = connection.prepareStatement(insertBidSql)) {
                for (BidTransaction bid : auction.getBids()) {
                    insertBid.setString(1, auction.getId());
                    insertBid.setString(2, bid.getBidder().getId());
                    insertBid.setDouble(3, bid.getAmount());
                    insertBid.setString(4, bid.getBidTime().toString());
                    insertBid.addBatch();
                }
                insertBid.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save auction", e);
        }
    }

    @Override
    public Auction findById(String id) {
        String sql = "SELECT id, item_id, seller_id, current_price, status, winner_id FROM auctions WHERE id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapAuction(resultSet);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find auction", e);
        }
    }

    @Override
    public List<Auction> findAll() {
        String sql = "SELECT id, item_id, seller_id, current_price, status, winner_id FROM auctions ORDER BY id";
        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                auctions.add(mapAuction(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list auctions", e);
        }

        return auctions;
    }

    @Override
    public void delete(String id) {
        String sql = "DELETE FROM auctions WHERE id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete auction", e);
        }
    }

    private Auction mapAuction(ResultSet resultSet) throws SQLException {
        Item item = itemDao.findById(resultSet.getString("item_id"));
        User sellerUser = userDao.findById(resultSet.getString("seller_id"));
        Seller seller = (Seller) sellerUser;

        Auction auction = new Auction(resultSet.getString("id"), item, seller);
        Bidder winner = null;
        String winnerId = resultSet.getString("winner_id");
        if (winnerId != null) {
            winner = (Bidder) userDao.findById(winnerId);
        }

        List<BidTransaction> bids = loadBids(auction.getId());
        auction.restoreState(
                AuctionStatus.valueOf(resultSet.getString("status")),
                resultSet.getDouble("current_price"),
                winner,
                bids
        );
        return auction;
    }

    private List<BidTransaction> loadBids(String auctionId) {
        String sql = "SELECT bidder_id, amount, bid_time FROM bids WHERE auction_id = ? ORDER BY bid_time";
        List<BidTransaction> bids = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Bidder bidder = (Bidder) userDao.findById(resultSet.getString("bidder_id"));
                    LocalDateTime bidTime = LocalDateTime.parse(resultSet.getString("bid_time"));
                    bids.add(new BidTransaction(bidder, resultSet.getDouble("amount"), bidTime));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load bids", e);
        }

        return bids;
    }
}
