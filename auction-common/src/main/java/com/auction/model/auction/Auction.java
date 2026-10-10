package com.auction.model.auction;

import com.auction.enums.AuctionStatus;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionException;
import com.auction.exception.InvalidBidException;
import com.auction.model.base.Entity;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.observer.BidEvent;
import com.auction.observer.BidObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    // Global observers được dùng cho các listener muốn nhận bid event của mọi auction,
    // thay vì phải subscribe thủ công từng instance một.
    private static final List<BidObserver> globalBidObservers = new CopyOnWriteArrayList<>();
    private final Item item;
    private final Seller seller;
    private volatile double currentPrice;
    private volatile AuctionStatus status;
    private final List<BidTransaction> bids;
    private volatile Bidder winner;
    private final List<BidObserver> bidObservers;
    private final ReentrantLock stateLock;
    private volatile java.time.LocalDateTime endTime;
    private int durationMinutes = 5;
    private long version;

    public Auction() {
        this(null, null, null);
    }

    public Auction(String id, Item item, Seller seller) {
        super(id);
        this.item = item;
        this.seller = seller;
        this.currentPrice = item == null ? 0.0 : item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
        this.bids = new ArrayList<>();
        this.winner = null;
        this.bidObservers = new CopyOnWriteArrayList<>();
        this.stateLock = new ReentrantLock();
        this.endTime = java.time.LocalDateTime.now().plusMinutes(5);
        this.durationMinutes = 5;
    }

    public void start() {
        stateLock.lock();
        try {
            if (status == AuctionStatus.OPEN) {
                status = AuctionStatus.RUNNING;
                this.endTime = java.time.LocalDateTime.now().plusMinutes(durationMinutes);
                return;
            }
            throw new AuctionException("Không thể bắt đầu phiên đấu giá khi đang ở trạng thái: " + status);
        } finally {
            stateLock.unlock();
        }
    }

    public void finish() {
        stateLock.lock();
        try {
            if (status == AuctionStatus.RUNNING) {
                status = AuctionStatus.FINISHED;
                return;
            }
            throw new AuctionException("Không thể kết thúc phiên đấu giá khi đang ở trạng thái: " + status);
        } finally {
            stateLock.unlock();
        }
    }

    public void cancel() {
        stateLock.lock();
        try {
            if (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING || status == AuctionStatus.FINISHED) {
                status = AuctionStatus.CANCELED;
                return;
            }
            throw new AuctionException("Không thể huỷ phiên đấu giá khi đang ở trạng thái: " + status);
        } finally {
            stateLock.unlock();
        }
    }

    public void markPaid() {
        stateLock.lock();
        try {
            if (status == AuctionStatus.FINISHED) {
                status = AuctionStatus.PAID;
                return;
            }
            throw new AuctionException("Không thể đánh dấu đã thanh toán khi đang ở trạng thái: " + status);
        } finally {
            stateLock.unlock();
        }
    }

    public void addObserver(BidObserver observer) {
        if (observer != null) {
            bidObservers.add(observer);
        }
    }

    public static void addGlobalObserver(BidObserver observer) {
        if (observer != null && !globalBidObservers.contains(observer)) {
            globalBidObservers.add(observer);
        }
    }

    public void removeObserver(BidObserver observer) {
        bidObservers.remove(observer);
    }

    public void addBid(BidTransaction bid) {
        if (bid == null) {
            throw new InvalidBidException("Lượt đặt giá không được để trống");
        }
        addBids(List.of(bid));
    }

    /** Validate the whole manual/automatic sequence before changing the aggregate. */
    public void addBids(List<BidTransaction> sequence) {
        stateLock.lock();
        try {
            if (status != AuctionStatus.RUNNING || endTime == null
                    || !java.time.LocalDateTime.now().isBefore(endTime)) {
                throw new AuctionClosedException("Phiên đấu giá chưa mở hoặc đã hết thời gian");
            }
            double price = currentPrice;
            for (BidTransaction bid : sequence) {
                if (bid == null || bid.getBidder() == null || !Double.isFinite(bid.getAmount())
                        || bid.getAmount() <= price) {
                    throw new InvalidBidException("Số tiền đặt giá phải hữu hạn và cao hơn giá hiện tại");
                }
                price = bid.getAmount();
            }
            for (BidTransaction bid : sequence) {
                bids.add(bid);
                currentPrice = bid.getAmount();
                winner = bid.getBidder();
                checkAndApplyAntiSniping();
            }
        } finally {
            stateLock.unlock();
        }
        for (BidTransaction bid : sequence) {
            notifyBidPlaced(bid);
        }
    }

    public Item getItem() {
        return item;
    }

    public Seller getSeller() {
        return seller;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public boolean isOpen() {
        return status == AuctionStatus.RUNNING;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public List<BidTransaction> getBids() {
        stateLock.lock();
        try { return List.copyOf(bids); }
        finally { stateLock.unlock(); }
    }

    public Bidder getWinner() {
        return winner;
    }

    public void setWinner(Bidder winner) {
        this.winner = winner;
    }

    public java.time.LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(java.time.LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        if (durationMinutes < 1 || durationMinutes > 10080) {
            throw new com.auction.exception.ValidationException("Thời lượng phải từ 1 đến 10080 phút");
        }
        this.durationMinutes = durationMinutes;
    }

    /**
     * Tài liệu học tập: Giải thuật Chống Bắn Tỉa Giá (Anti-Sniping)
     * Nếu thời gian kết thúc của phiên còn dưới 60 giây và có lượt đặt giá mới hợp lệ,
     * hệ thống tự động gia hạn thêm 60 giây để đảm bảo những người tham gia khác
     * đều có đủ thời gian phản hồi, bảo đảm tính công bằng.
     */
    public void checkAndApplyAntiSniping() {
        if (endTime != null && status == AuctionStatus.RUNNING) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            if (now.isAfter(endTime.minusSeconds(60)) && now.isBefore(endTime)) {
                this.endTime = this.endTime.plusSeconds(60);
                System.out.println("[Anti-Sniping] Phiên " + getId() + " được gia hạn 60 giây. Thời gian kết thúc mới: " + this.endTime);
            }
        }
    }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public void restoreState(AuctionStatus status, double currentPrice, Bidder winner, List<BidTransaction> restoredBids) {
        restoreState(status, currentPrice, winner, restoredBids, java.time.LocalDateTime.now().plusMinutes(5));
    }

    public void restoreState(AuctionStatus status, double currentPrice, Bidder winner, List<BidTransaction> restoredBids, java.time.LocalDateTime endTime) {
        // Hàm này chỉ dùng khi dựng lại aggregate từ database, không dùng trong luồng bid thông thường.
        this.status = status;
        this.currentPrice = currentPrice;
        this.winner = winner;
        this.bids.clear();
        this.bids.addAll(restoredBids);
        this.endTime = endTime;
    }

    private void notifyBidPlaced(BidTransaction bid) {
        BidEvent event = new BidEvent(this, bid, currentPrice, winner);
        for (BidObserver observer : bidObservers) {
            try {
                observer.onBidPlaced(event);
            } catch (RuntimeException exception) {
                System.err.println("Bid observer failed: " + exception.getClass().getSimpleName());
            }
        }
        for (BidObserver observer : globalBidObservers) {
            try {
                observer.onBidPlaced(event);
            } catch (RuntimeException exception) {
                System.err.println("Bid observer failed: " + exception.getClass().getSimpleName());
            }
        }
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id='" + getId() + '\'' +
                ", item=" + item +
                ", seller=" + seller +
                ", currentPrice=" + currentPrice +
                ", status=" + status +
                ", bids=" + bids +
                ", winner=" + winner +
                '}';
    }
}
