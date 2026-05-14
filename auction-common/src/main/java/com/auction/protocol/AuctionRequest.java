package com.auction.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Đối tượng gửi từ client sang server qua socket.
 * Dùng Java Serialization để truyền qua ObjectOutputStream/ObjectInputStream.
 */
public class AuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final RequestType type;
    private final Map<String, String> params;

    public AuctionRequest(RequestType type) {
        this.type = type;
        this.params = new HashMap<>();
    }

    public RequestType getType() {
        return type;
    }

    public AuctionRequest put(String key, String value) {
        params.put(key, value);
        return this;
    }

    public String get(String key) {
        return params.get(key);
    }

    public double getDouble(String key) {
        String val = params.get(key);
        if (val == null) {
            throw new IllegalArgumentException("Thiếu tham số: " + key);
        }
        return Double.parseDouble(val);
    }

    public Map<String, String> getParams() {
        return params;
    }

    @Override
    public String toString() {
        return "AuctionRequest{type=" + type + ", params=" + params + "}";
    }
}
