package com.auction.protocol;

import com.auction.exception.AuthenticationException;
import com.auction.exception.AuthorizationException;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.ValidationException;

public final class ProtocolErrors {
    private ProtocolErrors() {
    }

    public static ErrorCode codeFor(AuctionException exception) {
        if (exception instanceof AuthenticationException) { return ErrorCode.AUTHENTICATION; }
        if (exception instanceof AuthorizationException) { return ErrorCode.AUTHORIZATION; }
        if (exception instanceof ValidationException) { return ErrorCode.VALIDATION; }
        if (exception instanceof InvalidBidException) { return ErrorCode.INVALID_BID; }
        if (exception instanceof AuctionClosedException) { return ErrorCode.AUCTION_CLOSED; }
        return ErrorCode.AUCTION;
    }

    public static AuctionException toException(AuctionResponse response) {
        String message = response.getMessage() == null ? "Không thể xử lý yêu cầu" : response.getMessage();
        ErrorCode code = response.getErrorCode();
        if (code == null) { return new AuctionException(message); }
        return switch (code) {
            case AUTHENTICATION -> new AuthenticationException(message);
            case AUTHORIZATION -> new AuthorizationException(message);
            case VALIDATION -> new ValidationException(message);
            case INVALID_BID -> new InvalidBidException(message);
            case AUCTION_CLOSED -> new AuctionClosedException(message);
            default -> new AuctionException(message);
        };
    }
}
