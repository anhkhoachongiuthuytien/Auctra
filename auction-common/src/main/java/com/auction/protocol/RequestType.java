package com.auction.protocol;

/**
 * Liệt kê tất cả các loại yêu cầu mà client có thể gửi tới server.
 */
public enum RequestType {
    LOGIN,
    REGISTER,
    GET_REGISTRATION_ROLES,
    RESET_PASSWORD,
    LIST_AUCTIONS,
    LIST_AUCTIONS_FOR_SELLER,
    CREATE_AUCTION,
    START_AUCTION,
    FINISH_AUCTION,
    CANCEL_AUCTION,
    MARK_AUCTION_PAID,
    PLACE_BID,
    LIST_USERS,
    SUBSCRIBE_UPDATES
}
