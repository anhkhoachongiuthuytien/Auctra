package com.auction.manager;

import com.auction.model.auction.Auction;
import java.util.ArrayList;
import java.util.List;

public class AuctionManager {
    private static AuctionManager instance;
    private List<Auction> activeAuctions;
    private AuctionManager() {
        activeAuctions = new ArrayList<>();
    }
    public static synchronized AuctionManager getInstance() {
        // Nếu chưa có ai tạo thì mới tạo mới
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }
    public void addActiveAuction(Auction auction) {
        if (auction != null && !activeAuctions.contains(auction)) {
            activeAuctions.add(auction);
            System.out.println("AuctionManager: Đã thêm phiên đấu giá [" + auction.getId() + "] vào hệ thống.");
        }
    }
    public void removeActiveAuction(Auction auction) {
        if (activeAuctions.contains(auction)) {
            activeAuctions.remove(auction);
            System.out.println("AuctionManager: Đã xóa/kết thúc phiên đấu giá [" + auction.getId() + "].");
        }
    }
    public List<Auction> getActiveAuctions() {
        return activeAuctions;
    }
}