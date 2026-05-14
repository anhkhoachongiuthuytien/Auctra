package com.auction.presentation;

import com.auction.client.AuctionClientGateway;
import com.auction.exception.AuctionException;
import com.auction.exception.InvalidBidException;
import com.auction.model.auction.Auction;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;

import java.util.List;
import java.util.Locale;

public class AuctionListViewModel {
    private final AuctionClientGateway gateway;

    public AuctionListViewModel(AuctionClientGateway gateway) {
        this.gateway = gateway;
    }

    public List<Auction> loadAuctions() {
        return gateway.listAuctions();
    }

    public String getWelcomeMessage(User user) {
        return "Đang đăng nhập: " + user.getUsername() + " (" + user.getEmail() + ")";
    }

    public String getSummaryMessage(List<Auction> auctions) {
        return "Đã tải " + auctions.size() + " cuộc đấu giá. Các phiên đang chạy có thể nhận nhiều lượt đặt giá đồng thời.";
    }

    public ActionResult placeBid(User currentUser, Auction auction, String amountText) {
        if (!(currentUser instanceof Bidder bidder)) {
            return ActionResult.failure("Chỉ tài khoản Bidder mới được đặt giá.");
        }
        if (auction == null) {
            return ActionResult.failure("Vui lòng chọn một cuộc đấu giá trước.");
        }

        try {
            double amount = Double.parseDouble(amountText);
            gateway.placeBid(auction.getId(), bidder, amount);
            return ActionResult.success("Đặt giá thành công.");
        } catch (NumberFormatException ex) {
            return ActionResult.failure("Số tiền đặt giá phải là một số hợp lệ.");
        } catch (InvalidBidException ex) {
            return ActionResult.failure(
                    "Giá đặt phải cao hơn giá hiện tại (hiện tại: "
                            + String.format(Locale.US, "%.2f", auction.getCurrentPrice())
                            + ")."
            );
        } catch (AuctionException ex) {
            return ActionResult.failure(ex.getMessage());
        }
    }

    public ActionResult finishAuction(Auction auction) {
        if (auction == null) {
            return ActionResult.failure("Vui lòng chọn một cuộc đấu giá trước.");
        }

        try {
            gateway.finishAuction(auction.getId());
            return ActionResult.success("Đã kết thúc cuộc đấu giá.");
        } catch (AuctionException ex) {
            return ActionResult.failure(ex.getMessage());
        }
    }

    public record ActionResult(boolean success, String message) {
        public static ActionResult success(String message) {
            return new ActionResult(true, message);
        }

        public static ActionResult failure(String message) {
            return new ActionResult(false, message);
        }
    }
}
