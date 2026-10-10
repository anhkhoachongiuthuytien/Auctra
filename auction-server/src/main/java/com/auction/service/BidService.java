package com.auction.service;

import com.auction.dao.AuctionDao;
import com.auction.dao.AutoBidDao;
import com.auction.dao.UserDao;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.ValidationException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.auction.BidTransaction;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.util.AuctionLocks;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class BidService {
    private static final int MAX_AUTO_ROUNDS = 10000;
    private final AuctionDao auctionDao;
    private final AutoBidDao autoBidDao;
    private final UserDao userDao;

    public BidService(AuctionDao auctionDao) { this(auctionDao, null, null); }

    public BidService(AuctionDao auctionDao, AutoBidDao autoBidDao, UserDao userDao) {
        this.auctionDao = auctionDao;
        this.autoBidDao = autoBidDao;
        this.userDao = userDao;
    }

    public void placeBid(String auctionId, Bidder bidder, double amount) {
        if (bidder == null) { throw new ValidationException("Bidder không được để trống"); }
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new ValidationException("Số tiền đặt giá phải hữu hạn và lớn hơn 0");
        }
        synchronized (AuctionLocks.forId(auctionId)) {
            Auction auction = requireOpenAuction(auctionId);
            if (amount <= auction.getCurrentPrice()) {
                throw new InvalidBidException("Giá đặt phải cao hơn giá hiện tại");
            }
            Bidder actualBidder = userDao == null ? bidder : requireBidder(bidder.getId());
            List<BidTransaction> bids = new ArrayList<>();
            bids.add(new BidTransaction(actualBidder, amount));
            bids.addAll(planAutoBids(amount, actualBidder.getId(), configsFor(auctionId)));
            auction.addBids(bids);
            auctionDao.save(auction);
        }
    }

    public void registerAutoBid(String auctionId, String bidderId, double maxPrice, double increment) {
        requireAutoBidSupport();
        validateConfig(maxPrice, increment);
        synchronized (AuctionLocks.forId(auctionId)) {
            Auction auction = requireOpenAuction(auctionId);
            requireBidder(bidderId);
            double minimum = nextAmount(auction.getCurrentPrice(), increment);
            if (!Double.isFinite(minimum) || minimum <= auction.getCurrentPrice() || maxPrice < minimum) {
                throw new ValidationException("Giá tối đa phải đủ cho giá hiện tại cộng bước nâng");
            }
            AutoBidConfig config = new AutoBidConfig(auctionId, bidderId, maxPrice, increment);
            List<AutoBidConfig> configs = new ArrayList<>(configsFor(auctionId));
            configs.removeIf(c -> c.getBidderId().equals(bidderId));
            configs.add(config);
            List<BidTransaction> planned = planAutoBids(auction.getCurrentPrice(), winnerId(auction), configs);
            // Plan before storing anything so invalid/huge configurations cannot partially bid.
            AutoBidConfig previous = autoBidDao.find(auctionId, bidderId);
            autoBidDao.save(config);
            try {
                if (!planned.isEmpty()) {
                    auction.addBids(planned);
                    auctionDao.save(auction);
                }
            } catch (RuntimeException exception) {
                if (previous == null) { autoBidDao.delete(auctionId, bidderId); }
                else { autoBidDao.save(previous); }
                throw exception;
            }
        }
    }

    public void cancelAutoBid(String auctionId, String bidderId) {
        requireAutoBidSupport();
        synchronized (AuctionLocks.forId(auctionId)) {
            requireBidder(bidderId);
            autoBidDao.delete(auctionId, bidderId);
        }
    }

    private Auction requireOpenAuction(String id) {
        Auction auction = auctionDao.findById(id);
        if (auction == null) { throw new AuctionException("Không tìm thấy cuộc đấu giá"); }
        if (!auction.isOpen() || auction.getEndTime() == null
                || !LocalDateTime.now().isBefore(auction.getEndTime())) {
            throw new AuctionClosedException("Cuộc đấu giá chưa mở hoặc đã hết thời gian");
        }
        return auction;
    }

    private Bidder requireBidder(String id) {
        User user = id == null ? null : userDao.findById(id);
        if (!(user instanceof Bidder bidder)) {
            throw new ValidationException("Không tìm thấy tài khoản người mua hợp lệ");
        }
        return bidder;
    }

    private void requireAutoBidSupport() {
        if (autoBidDao == null || userDao == null) { throw new AuctionException("Auto-bid không khả dụng"); }
    }

    private List<AutoBidConfig> configsFor(String id) {
        return autoBidDao == null || userDao == null ? List.of() : autoBidDao.getAutoBidsForAuction(id);
    }

    private static void validateConfig(double max, double step) {
        if (!Double.isFinite(max) || max <= 0 || !Double.isFinite(step) || step <= 0) {
            throw new ValidationException("Giá tối đa và bước nâng phải hữu hạn và lớn hơn 0");
        }
    }

    private static double nextAmount(double price, double step) {
        return BigDecimal.valueOf(price).add(BigDecimal.valueOf(step)).doubleValue();
    }

    private static String winnerId(Auction auction) {
        return auction.getWinner() == null ? "" : auction.getWinner().getId();
    }

    /** Preserve increment-based bidding, with deterministic ties and no mutation while planning. */
    private List<BidTransaction> planAutoBids(double price, String winner, List<AutoBidConfig> configs) {
        Map<String, AutoBidConfig> eligible = new HashMap<>();
        Map<String, Bidder> bidders = new HashMap<>();
        for (AutoBidConfig config : configs) {
            // Old databases may contain invalid configurations; they must not poison manual bids.
            if (!Double.isFinite(config.getMaxPrice()) || config.getMaxPrice() <= 0
                    || !Double.isFinite(config.getIncrement()) || config.getIncrement() <= 0) { continue; }
            User user = userDao.findById(config.getBidderId());
            if (user instanceof Bidder bidder) {
                eligible.put(config.getBidderId(), config);
                bidders.put(config.getBidderId(), bidder);
            }
        }
        List<BidTransaction> planned = new ArrayList<>();
        PriorityQueue<PendingBid> queue = new PriorityQueue<>(Comparator.comparingDouble(PendingBid::amount)
                .thenComparing(PendingBid::bidderId));
        while (true) {
            queue.clear();
            for (AutoBidConfig config : eligible.values()) {
                if (config.getBidderId().equals(winner)) { continue; }
                double next = nextAmount(price, config.getIncrement());
                if (Double.isFinite(next) && next > price && next <= config.getMaxPrice()) {
                    queue.add(new PendingBid(config.getBidderId(), next));
                }
            }
            if (queue.isEmpty()) { return planned; }
            if (planned.size() >= MAX_AUTO_ROUNDS) {
                throw new ValidationException("Auto-bid cần quá nhiều lượt; hãy tăng bước nâng giá");
            }
            PendingBid next = queue.remove();
            planned.add(new BidTransaction(bidders.get(next.bidderId()), next.amount()));
            winner = next.bidderId();
            price = next.amount();
        }
    }

    private record PendingBid(String bidderId, double amount) { }
}
