package com.auction.protocol;

import java.io.Serializable;

/**
 * Sự kiện được broadcast từ Server về tất cả Client
 * để thông báo có cập nhật (Real-time update).
 */
public class AuctionEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String eventType;

    public AuctionEvent(String eventType) {
        this.eventType = eventType;
    }

    public String getEventType() {
        return eventType;
    }
}
