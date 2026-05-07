package com.auction.app;

import com.auction.dao.memory.InMemoryAuctionDao;
import com.auction.dao.memory.InMemoryItemDao;
import com.auction.dao.memory.InMemoryUserDao;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.observer.ConsoleBidObserver;
import com.auction.service.AuctionService;
import com.auction.service.AuthService;
import com.auction.service.BidService;
import com.auction.service.SellerService;

public class AppContext {
    private final InMemoryUserDao userDao;
    private final InMemoryItemDao itemDao;
    private final InMemoryAuctionDao auctionDao;
    private final AuthService authService;
    private final SellerService sellerService;
    private final AuctionService auctionService;
    private final BidService bidService;

    public AppContext() {
        this.userDao = new InMemoryUserDao();
        this.itemDao = new InMemoryItemDao();
        this.auctionDao = new InMemoryAuctionDao();
        this.authService = new AuthService(userDao);
        this.sellerService = new SellerService(itemDao, auctionDao);
        this.auctionService = new AuctionService(auctionDao);
        this.bidService = new BidService(auctionDao);

        seedData();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public AuctionService getAuctionService() {
        return auctionService;
    }

    public BidService getBidService() {
        return bidService;
    }

    private void seedData() {
        Seller seller = authService.registerSeller("seller_demo", "seller@auction.local");
        Bidder bidder = authService.registerBidder("bidder_demo", "bidder@auction.local");
        authService.registerAdmin("admin_demo", "admin@auction.local");

        Item laptop = sellerService.createItem("Electronics", "Gaming Laptop", "RTX laptop for concurrent bidding demo", 1500.0);
        Item car = sellerService.createItem("Vehicle", "Used Sedan", "Auction state machine sample", 8000.0);
        Item art = sellerService.createItem("Art", "Landscape Painting", "Observer pattern sample item", 500.0);

        Auction laptopAuction = auctionService.createAuction(laptop, seller);
        Auction carAuction = auctionService.createAuction(car, seller);
        Auction artAuction = auctionService.createAuction(art, seller);

        laptopAuction.addObserver(new ConsoleBidObserver());
        carAuction.addObserver(new ConsoleBidObserver());
        artAuction.addObserver(new ConsoleBidObserver());

        auctionService.startAuction(laptopAuction.getId());
        auctionService.startAuction(artAuction.getId());

        bidService.placeBid(laptopAuction.getId(), bidder, 1700.0);

        auctionService.startAuction(carAuction.getId());
        auctionService.finishAuction(carAuction.getId());
        auctionService.markAuctionPaid(carAuction.getId());
    }
}
