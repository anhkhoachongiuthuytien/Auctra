package com.auction.dao;

import com.auction.model.auction.AutoBidConfig;
import java.util.List;

/**
 * Trừu tượng hóa các thao tác cơ sở dữ liệu đối với cấu hình Auto-Bid.
 */
public interface AutoBidDao {
    void save(AutoBidConfig config);
    List<AutoBidConfig> getAutoBidsForAuction(String auctionId);
    AutoBidConfig find(String auctionId, String bidderId);
    void delete(String auctionId, String bidderId);
}
