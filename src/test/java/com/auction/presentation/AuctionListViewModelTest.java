package com.auction.presentation;

import com.auction.dao.memory.InMemoryAuctionDao;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.service.AuctionService;
import com.auction.service.BidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionListViewModelTest {

    private AuctionListViewModel viewModel;
    private Auction auction;
    private Bidder bidder;

    @BeforeEach
    void setUp() {
        InMemoryAuctionDao auctionDao = new InMemoryAuctionDao();
        viewModel = new AuctionListViewModel(new AuctionService(auctionDao), new BidService(auctionDao));

        Item item = new Item("I001", "Laptop", "Gaming laptop", 1000.0);
        Seller seller = new Seller("S001", "seller", "seller@test.com");
        bidder = new Bidder("B001", "bidder", "bidder@test.com");

        auction = new Auction("A001", item, seller);
        auction.start();
        auctionDao.save(auction);
    }

    // Khi người dùng bid thấp hơn giá hiện tại, ViewModel phải trả về message rõ current price để UI hiển thị.
    @Test
    void testPlaceBidBelowCurrentPriceReturnsFriendlyMessage() {
        AuctionListViewModel.ActionResult result = viewModel.placeBid(bidder, auction, "900");

        assertFalse(result.success());
        assertTrue(result.message().contains("current: 1000.00"));
    }
}
