package com.auction.protocol;

import java.io.Serializable;

/**
 * Đối tượng trả về từ server cho client qua socket.
 * Chứa trạng thái success/error và dữ liệu (Object serializable).
 */
public class AuctionResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final Object data;

    private AuctionResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static AuctionResponse ok(Object data) {
        return new AuctionResponse(true, null, data);
    }

    public static AuctionResponse ok() {
        return new AuctionResponse(true, null, null);
    }

    public static AuctionResponse error(String message) {
        return new AuctionResponse(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return "AuctionResponse{success=" + success + ", message='" + message + "', data=" + data + "}";
    }
}
