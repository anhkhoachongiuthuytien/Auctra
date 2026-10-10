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
    private ErrorCode errorCode;
    private String sessionToken;
    private String recoveryCode;

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

    public static AuctionResponse error(ErrorCode code, String message) {
        AuctionResponse response = error(message);
        response.errorCode = code;
        return response;
    }

    public static AuctionResponse authenticated(UserDto user, String token, String recoveryCode) {
        AuctionResponse response = ok(user);
        response.sessionToken = token;
        response.recoveryCode = recoveryCode;
        return response;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public String getSessionToken() { return sessionToken; }
    public String getRecoveryCode() { return recoveryCode; }

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
