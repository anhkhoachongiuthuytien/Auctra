package com.auction.protocol;

import java.io.Serializable;

/**
 * Sự kiện được broadcast từ Server về tất cả Client
 * để thông báo có cập nhật (Real-time update).
 */
public class AuctionEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String eventType;
    private final String auctionId;

    public AuctionEvent(String eventType) {
        this(eventType, null);
    }

    public AuctionEvent(String eventType, String auctionId) {
        this.eventType = eventType;
        this.auctionId = auctionId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAuctionId() {
        return auctionId;
    }
}
