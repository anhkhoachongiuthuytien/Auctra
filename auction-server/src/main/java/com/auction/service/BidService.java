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

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class BidService {
    private final AuctionDao auctionDao;
    private final AutoBidDao autoBidDao;
    private final UserDao userDao;

    public BidService(AuctionDao auctionDao) {
        this(auctionDao, null, null);
    }

    public BidService(AuctionDao auctionDao, AutoBidDao autoBidDao, UserDao userDao) {
        this.auctionDao = auctionDao;
        this.autoBidDao = autoBidDao;
        this.userDao = userDao;
    }

    public void placeBid(String auctionId, Bidder bidder, double amount) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException("Mã phiên đấu giá không được để trống");
        }
        if (bidder == null) {
            throw new ValidationException("Bidder không được để trống");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new ValidationException("Số tiền đặt giá phải lớn hơn 0");
        }
        Auction auction = auctionDao.findById(auctionId);
        if (auction == null) {
            throw new AuctionException("Không tìm thấy cuộc đấu giá");
        }
        // Khóa theo từng object auction để hai thread không cùng kiểm tra và cập nhật
        // currentPrice trên cùng một phiên đấu giá tại cùng thời điểm.
        synchronized (auction) {
            if (!auction.isOpen()) {
                throw new AuctionClosedException("Cuộc đấu giá đã đóng");
            }
            if (amount <= auction.getCurrentPrice()) {
                throw new InvalidBidException("Giá đặt phải cao hơn giá hiện tại");
            }
            BidTransaction bid = new BidTransaction(bidder, amount);
            auction.addBid(bid);

            // Chạy động cơ tự động đấu giá (Auto-Bidding Engine)
            if (autoBidDao != null && userDao != null) {
                runAutoBiddingEngine(auction);
            }

            auctionDao.save(auction);
        }
    }

    /**
     * Tài liệu học tập: Giải thuật Đấu giá tự động sử dụng PriorityQueue
     * 1. Duyệt qua tất cả các cấu hình Auto-Bid đã đăng ký của phiên này.
     * 2. Sử dụng PriorityQueue để sắp xếp các lượt đặt giá tự động tiềm năng theo mức giá tăng dần.
     *    Người nào cần nâng giá ở mức thấp nhất tiếp theo sẽ được ưu tiên đấu giá trước.
     * 3. Khi một bidder outbid bidder khác, bidder cũ sẽ được re-insert vào hàng đợi để tiếp tục đấu giá
     *    cho đến khi vượt quá maxPrice của họ.
     * 4. Vòng lặp liên tục đẩy giá lên và kích hoạt Anti-Sniping cho đến khi không còn ai đủ ngân sách.
     */
    private void runAutoBiddingEngine(Auction auction) {
        List<AutoBidConfig> configs = autoBidDao.getAutoBidsForAuction(auction.getId());
        if (configs.isEmpty()) return;

        PriorityQueue<PendingAutoBid> pq = new PriorityQueue<>();
        String winnerId = auction.getWinner() != null ? auction.getWinner().getId() : "";
        double currentPrice = auction.getCurrentPrice();

        // 1. Khởi động PQ với tất cả các cấu hình Auto-Bid (trừ người thắng hiện tại)
        for (AutoBidConfig config : configs) {
            if (!config.getBidderId().equals(winnerId)) {
                double nextBid = currentPrice + config.getIncrement();
                if (config.getMaxPrice() >= nextBid) {
                    pq.add(new PendingAutoBid(config.getBidderId(), config.getMaxPrice(), config.getIncrement(), nextBid));
                }
            }
        }

        while (!pq.isEmpty()) {
            PendingAutoBid bidder = pq.poll();
            currentPrice = auction.getCurrentPrice();
            winnerId = auction.getWinner() != null ? auction.getWinner().getId() : "";

            if (!bidder.bidderId.equals(winnerId)) {
                double bidAmount = Math.max(bidder.nextBidAmount, currentPrice + bidder.increment);
                if (bidder.maxBid >= bidAmount) {
                    User user = userDao.findById(bidder.bidderId);
                    if (user instanceof Bidder) {
                        String oldWinnerId = winnerId;
                        
                        // Đặt giá mới
                        BidTransaction autoBid = new BidTransaction((Bidder) user, bidAmount);
                        auction.addBid(autoBid);

                        // Khi người thắng cũ bị outbid, ta phải thêm họ lại vào PQ (nếu họ có cấu hình Auto-Bid)
                        if (oldWinnerId != null && !oldWinnerId.isEmpty()) {
                            AutoBidConfig oldWinnerConfig = null;
                            for (AutoBidConfig c : configs) {
                                if (c.getBidderId().equals(oldWinnerId)) {
                                    oldWinnerConfig = c;
                                    break;
                                }
                            }
                            if (oldWinnerConfig != null) {
                                double nextBidForOldWinner = auction.getCurrentPrice() + oldWinnerConfig.getIncrement();
                                if (oldWinnerConfig.getMaxPrice() >= nextBidForOldWinner) {
                                    pq.add(new PendingAutoBid(oldWinnerConfig.getBidderId(), 
                                            oldWinnerConfig.getMaxPrice(), 
                                            oldWinnerConfig.getIncrement(), 
                                            nextBidForOldWinner));
                                }
                            }
                        }

                        // Đồng bộ lại tất cả giá trị tiếp theo của các đối thủ khác đang chờ trong hàng đợi
                        List<PendingAutoBid> temp = new ArrayList<>();
                        while (!pq.isEmpty()) {
                            PendingAutoBid p = pq.poll();
                            double adjustedNextBid = Math.max(p.nextBidAmount, auction.getCurrentPrice() + p.increment);
                            if (p.maxBid >= adjustedNextBid) {
                                p.nextBidAmount = adjustedNextBid;
                                temp.add(p);
                            }
                        }
                        pq.addAll(temp);
                    }
                }
            }
        }
    }

    private static class PendingAutoBid implements Comparable<PendingAutoBid> {
        final String bidderId;
        final double maxBid;
        final double increment;
        double nextBidAmount;

        PendingAutoBid(String bidderId, double maxBid, double increment, double nextBidAmount) {
            this.bidderId = bidderId;
            this.maxBid = maxBid;
            this.increment = increment;
            this.nextBidAmount = nextBidAmount;
        }

        @Override
        public int compareTo(PendingAutoBid o) {
            return Double.compare(this.nextBidAmount, o.nextBidAmount);
        }
    }
}
