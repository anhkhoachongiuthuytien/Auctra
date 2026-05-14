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
    private double currentPrice;
    private AuctionStatus status;
    private final List<BidTransaction> bids;
    private Bidder winner;
    private final List<BidObserver> bidObservers;
    // stateLock bảo vệ các thay đổi trạng thái và currentPrice bên trong cùng một auction.
    private final ReentrantLock stateLock;

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
    }

    public void start() {
        stateLock.lock();
        try {
            if (status == AuctionStatus.OPEN) {
                status = AuctionStatus.RUNNING;
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
        stateLock.lock();
        try {
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá chưa mở để đặt giá");
            }
            if (bid == null) {
                throw new InvalidBidException("Lượt đặt giá không được để trống");
            }
            if (bid.getAmount() <= currentPrice) {
                throw new InvalidBidException("Số tiền đặt giá phải cao hơn giá hiện tại");
            }

            bids.add(bid);
            currentPrice = bid.getAmount();
            winner = bid.getBidder();
        } finally {
            stateLock.unlock();
        }

        // Notify được đặt ngoài vùng lock để tránh giữ khóa trong lúc observer chạy code riêng.
        notifyBidPlaced(bid);
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
        return bids;
    }

    public Bidder getWinner() {
        return winner;
    }

    public void setWinner(Bidder winner) {
        this.winner = winner;
    }

    public void restoreState(AuctionStatus status, double currentPrice, Bidder winner, List<BidTransaction> restoredBids) {
        // Hàm này chỉ dùng khi dựng lại aggregate từ database, không dùng trong luồng bid thông thường.
        this.status = status;
        this.currentPrice = currentPrice;
        this.winner = winner;
        this.bids.clear();
        this.bids.addAll(restoredBids);
    }

    private void notifyBidPlaced(BidTransaction bid) {
        BidEvent event = new BidEvent(this, bid, currentPrice, winner);
        for (BidObserver observer : bidObservers) {
            observer.onBidPlaced(event);
        }
        for (BidObserver observer : globalBidObservers) {
            observer.onBidPlaced(event);
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
