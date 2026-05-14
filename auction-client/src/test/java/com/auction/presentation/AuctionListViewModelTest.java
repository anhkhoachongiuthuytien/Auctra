package com.auction.presentation;

import com.auction.client.AuctionClientGateway;
import com.auction.dao.memory.InMemoryAuctionDao;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.service.AuctionService;
import com.auction.service.BidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionListViewModelTest {
    private AuctionListViewModel viewModel;
    private Auction auction;
    private Bidder bidder;

    @BeforeEach
    void setUp() {
        InMemoryAuctionDao auctionDao = new InMemoryAuctionDao();
        AuctionService auctionService = new AuctionService(auctionDao);
        BidService bidService = new BidService(auctionDao);
        viewModel = new AuctionListViewModel(new TestGateway(auctionService, bidService));

        Item item = new Item("I001", "Laptop", "Gaming laptop", 1000.0);
        Seller seller = new Seller("S001", "seller", "seller@test.com");
        bidder = new Bidder("B001", "bidder", "bidder@test.com");

        auction = new Auction("A001", item, seller);
        auction.start();
        auctionDao.save(auction);
    }

    // Khi bidder gửi số tiền thấp hơn giá hiện tại, thông báo phía client phải thân thiện với người dùng.
    @Test
    void testPlaceBidBelowCurrentPriceReturnsFriendlyMessage() {
        AuctionListViewModel.ActionResult result = viewModel.placeBid(bidder, auction, "900");

        assertFalse(result.success());
        assertTrue(result.message().contains("hiện tại: 1000.00"));
    }

    private static final class TestGateway implements AuctionClientGateway {
        private final AuctionService auctionService;
        private final BidService bidService;

        private TestGateway(AuctionService auctionService, BidService bidService) {
            this.auctionService = auctionService;
            this.bidService = bidService;
        }

        @Override
        public User login(String email, String password) {
            throw new UnsupportedOperationException();
        }

        @Override
        public User register(String username, String email, String password, String role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> getAvailableRegistrationRoles() {
            return List.of("Bidder", "Seller");
        }

        @Override
        public void resetPassword(String email, String username, String newPassword) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Auction> listAuctions() {
            return auctionService.listAuctions();
        }

        @Override
        public List<Auction> listAuctionsForSeller(String sellerId) {
            return listAuctions().stream()
                    .filter(auction -> auction.getSeller().getId().equals(sellerId))
                    .toList();
        }

        @Override
        public Auction createAuctionForSeller(Seller seller, String itemType, String name, String description, double startingPrice) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Auction createAuctionForSeller(Seller seller, String itemType, String name, String description,
                                              double startingPrice, String imagePath) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startAuction(String auctionId) {
            auctionService.startAuction(auctionId);
        }

        @Override
        public void finishAuction(String auctionId) {
            auctionService.finishAuction(auctionId);
        }

        @Override
        public void cancelAuction(String auctionId) {
            auctionService.cancelAuction(auctionId);
        }

        @Override
        public void markAuctionPaid(String auctionId) {
            auctionService.markAuctionPaid(auctionId);
        }

        @Override
        public void placeBid(String auctionId, Bidder bidder, double amount) {
            bidService.placeBid(auctionId, bidder, amount);
        }

        @Override
        public List<User> listUsers() {
            throw new UnsupportedOperationException();
        }
    }
}
