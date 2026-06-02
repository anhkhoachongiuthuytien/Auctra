package com.auction.dao.memory;

import com.auction.dao.AutoBidDao;
import com.auction.model.auction.AutoBidConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryAutoBidDao implements AutoBidDao {
    private final Map<String, AutoBidConfig> storage = new HashMap<>();

    private String makeKey(String auctionId, String bidderId) {
        return auctionId + "_" + bidderId;
    }

    @Override
    public void save(AutoBidConfig config) {
        if (config != null) {
            storage.put(makeKey(config.getAuctionId(), config.getBidderId()), config);
        }
    }

    @Override
    public List<AutoBidConfig> getAutoBidsForAuction(String auctionId) {
        List<AutoBidConfig> result = new ArrayList<>();
        for (AutoBidConfig config : storage.values()) {
            if (config.getAuctionId().equals(auctionId)) {
                result.add(config);
            }
        }
        return result;
    }

    @Override
    public AutoBidConfig find(String auctionId, String bidderId) {
        return storage.get(makeKey(auctionId, bidderId));
    }

    @Override
    public void delete(String auctionId, String bidderId) {
        storage.remove(makeKey(auctionId, bidderId));
    }
}
