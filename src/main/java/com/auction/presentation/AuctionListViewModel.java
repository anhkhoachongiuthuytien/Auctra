package com.auction.presentation;

import com.auction.exception.AuctionException;
import com.auction.model.auction.Auction;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.service.AuctionService;
import com.auction.service.BidService;

import java.util.List;

public class AuctionListViewModel {
    private final AuctionService auctionService;
    private final BidService bidService;

    public AuctionListViewModel(AuctionService auctionService, BidService bidService) {
        this.auctionService = auctionService;
        this.bidService = bidService;
    }

    public List<Auction> loadAuctions() {
        return auctionService.listAuctions();
    }

    public String getWelcomeMessage(User user) {
        return "Logged in as: " + user.getUsername() + " (" + user.getEmail() + ")";
    }

    public String getSummaryMessage(List<Auction> auctions) {
        return "Loaded " + auctions.size() + " auctions. Running auctions can receive concurrent bids.";
    }

    public ActionResult placeBid(User currentUser, Auction auction, String amountText) {
        if (!(currentUser instanceof Bidder bidder)) {
            return ActionResult.failure("Only bidder accounts can place bids.");
        }
        if (auction == null) {
            return ActionResult.failure("Please select an auction first.");
        }

        try {
            // UI gửi amount dưới dạng text, nên ViewModel chịu trách nhiệm parse trước khi vào service.
            double amount = Double.parseDouble(amountText);
            bidService.placeBid(auction.getId(), bidder, amount);
            return ActionResult.success("Bid placed successfully.");
        } catch (NumberFormatException ex) {
            return ActionResult.failure("Bid amount must be a valid number.");
        } catch (AuctionException ex) {
            // Service ném exception nghiệp vụ, còn ViewModel đổi chúng thành message thân thiện cho UI.
            return ActionResult.failure(ex.getMessage());
        }
    }

    public ActionResult finishAuction(Auction auction) {
        if (auction == null) {
            return ActionResult.failure("Please select an auction first.");
        }

        try {
            auctionService.finishAuction(auction.getId());
            return ActionResult.success("Auction finished successfully.");
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
