package com.auction.server;

import com.auction.enums.AuctionStatus;
import com.auction.model.auction.Auction;
import com.auction.service.AuctionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tác vụ nền tự động kết thúc các phiên đấu giá đã hết thời gian.
 *
 * Scheduler kiểm tra định kỳ (mỗi 10 giây) tất cả auction đang ở trạng thái RUNNING.
 * Nếu endTime của auction đã qua thời điểm hiện tại, scheduler sẽ tự động
 * chuyển trạng thái sang FINISHED và broadcast sự kiện tới các client.
 *
 * Sử dụng daemon thread để JVM có thể tắt sạch khi không còn thread non-daemon.
 */
public class AuctionExpiryScheduler {
    private static final long CHECK_INTERVAL_SECONDS = 10;

    private final AuctionService auctionService;
    private final ScheduledExecutorService scheduler;

    public AuctionExpiryScheduler(AuctionService auctionService) {
        this.auctionService = auctionService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionExpiryScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Bắt đầu chạy scheduler.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(this::checkExpiredAuctions,
                CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        System.out.println("[Scheduler] Đã khởi động kiểm tra hết hạn đấu giá (mỗi "
                + CHECK_INTERVAL_SECONDS + " giây).");
    }

    /**
     * Dừng scheduler sạch sẽ.
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[Scheduler] Đã dừng kiểm tra hết hạn.");
    }

    /**
     * Kiểm tra và tự động kết thúc các auction RUNNING đã hết endTime.
     */
    private void checkExpiredAuctions() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Auction> allAuctions = auctionService.listAuctions();

            for (Auction auction : allAuctions) {
                if (auction.getStatus() == AuctionStatus.RUNNING
                        && auction.getEndTime() != null
                        && now.isAfter(auction.getEndTime())) {
                    try {
                        auctionService.finishAuction(auction.getId());
                        System.out.println("[Scheduler] Tự động kết thúc phiên: "
                                + auction.getId() + " (" + auction.getItem().getName() + ")");
                        
                        // Broadcast sự kiện cho các client Socket đang kết nối
                        BroadcastManager.broadcast(
                                new com.auction.protocol.AuctionEvent("AUCTION_FINISHED", auction.getId()));

                        // Trigger cập nhật giao diện trong chế độ LOCAL (in-process) qua Reflection
                        try {
                            Class<?> eventManagerClass = Class.forName("com.auction.client.ClientEventManager");
                            java.lang.reflect.Method fireMethod = eventManagerClass.getMethod("fireUpdate");
                            
                            // Vì JavaFX chỉ cho phép cập nhật UI từ JavaFX Application Thread
                            try {
                                Class<?> platformClass = Class.forName("javafx.application.Platform");
                                java.lang.reflect.Method runLaterMethod = platformClass.getMethod("runLater", Runnable.class);
                                runLaterMethod.invoke(null, (Runnable) () -> {
                                    try {
                                        fireMethod.invoke(null);
                                    } catch (Exception ignored) {}
                                });
                            } catch (Exception ex) {
                                fireMethod.invoke(null);
                            }
                        } catch (Exception ignored) {
                            // Chạy standalone server không có class ClientEventManager, bỏ qua
                        }
                    } catch (Exception e) {
                        // Auction có thể đã bị finish/cancel bởi ai đó khác, bỏ qua
                        System.err.println("[Scheduler] Lỗi khi kết thúc phiên "
                                + auction.getId() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Scheduler] Lỗi khi kiểm tra hết hạn: " + e.getMessage());
        }
    }
}
