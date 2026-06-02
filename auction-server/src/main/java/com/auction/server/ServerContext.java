package com.auction.server;

import com.auction.dao.sqlite.SqliteAuctionDao;
import com.auction.dao.sqlite.SqliteAutoBidDao;
import com.auction.dao.sqlite.SqliteItemDao;
import com.auction.dao.sqlite.SqliteUserDao;
import com.auction.db.DatabaseManager;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.observer.ConsoleBidObserver;
import com.auction.service.AuctionService;
import com.auction.service.AuthService;
import com.auction.service.BidService;
import com.auction.service.SellerService;
import com.auction.service.UserService;

public class ServerContext {
    private static final String DEMO_PASSWORD = "demo12345";

    private final DatabaseManager databaseManager;
    private final SqliteUserDao userDao;
    private final SqliteItemDao itemDao;
    private final SqliteAuctionDao auctionDao;
    private final SqliteAutoBidDao autoBidDao;
    private final AuthService authService;
    private final SellerService sellerService;
    private final AuctionService auctionService;
    private final BidService bidService;
    private final UserService userService;

    public ServerContext(String jdbcUrl) {
        this.databaseManager = new DatabaseManager(jdbcUrl);
        this.databaseManager.initializeSchema();

        this.userDao = new SqliteUserDao(databaseManager);
        this.itemDao = new SqliteItemDao(databaseManager);
        this.auctionDao = new SqliteAuctionDao(databaseManager, itemDao, userDao);
        this.autoBidDao = new SqliteAutoBidDao(databaseManager);
        this.authService = new AuthService(userDao);
        this.sellerService = new SellerService(itemDao, auctionDao);
        this.auctionService = new AuctionService(auctionDao);
        this.bidService = new BidService(auctionDao, autoBidDao, userDao);
        this.userService = new UserService(userDao);

        seedData();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public SellerService getSellerService() {
        return sellerService;
    }

    public AuctionService getAuctionService() {
        return auctionService;
    }

    public BidService getBidService() {
        return bidService;
    }

    public com.auction.dao.AutoBidDao getAutoBidDao() {
        return autoBidDao;
    }

    public UserService getUserService() {
        return userService;
    }

    private void seedData() {
        ensureDemoUsers();

        if (!auctionService.listAuctions().isEmpty()) {
            return;
        }

        Seller seller = (Seller) authService.login("seller@auction.local", DEMO_PASSWORD);
        Bidder bidder = (Bidder) authService.login("bidder@auction.local", DEMO_PASSWORD);

        Item laptop = sellerService.createItem("Electronics", "Gaming Laptop", "RTX laptop for concurrent bidding demo", 1500.0);
        Item car = sellerService.createItem("Vehicle", "Used Sedan", "Auction state machine sample", 8000.0);
        Item art = sellerService.createItem("Art", "Landscape Painting", "Observer pattern sample item", 500.0);
        Item phone = sellerService.createItem("Electronics", "iPhone 15 Pro Max", "Likenew titanium gray 256GB with original box", 999.0);
        Item watch = sellerService.createItem("Fashion", "Luxury Chronograph", "Mechanical self-winding wrist watch with sapphire crystal", 1200.0);
        Item book = sellerService.createItem("Collectible", "First Edition Novel", "Rare signed copy of classic literature in excellent condition", 300.0);

        Auction.addGlobalObserver(new ConsoleBidObserver());

        Auction laptopAuction = auctionService.createAuction(laptop, seller);
        Auction carAuction = auctionService.createAuction(car, seller);
        Auction artAuction = auctionService.createAuction(art, seller);
        Auction phoneAuction = auctionService.createAuction(phone, seller);
        Auction watchAuction = auctionService.createAuction(watch, seller);
        Auction bookAuction = auctionService.createAuction(book, seller);

        auctionService.startAuction(laptopAuction.getId());
        auctionService.startAuction(artAuction.getId());
        auctionService.startAuction(phoneAuction.getId());
        auctionService.startAuction(watchAuction.getId());

        bidService.placeBid(laptopAuction.getId(), bidder, 1700.0);
        bidService.placeBid(phoneAuction.getId(), bidder, 1050.0);

        auctionService.startAuction(carAuction.getId());
        auctionService.finishAuction(carAuction.getId());
        auctionService.markAuctionPaid(carAuction.getId());
    }

    private void ensureDemoUsers() {
        if (!authService.emailExists("seller@auction.local")) {
            authService.registerSeller("seller_demo", "seller@auction.local", DEMO_PASSWORD);
        }
        authService.ensurePassword("seller@auction.local", DEMO_PASSWORD);

        if (!authService.emailExists("bidder@auction.local")) {
            authService.registerBidder("bidder_demo", "bidder@auction.local", DEMO_PASSWORD);
        }
        authService.ensurePassword("bidder@auction.local", DEMO_PASSWORD);

        if (!authService.emailExists("admin@auction.local")) {
            authService.registerAdmin("admin_demo", "admin@auction.local", DEMO_PASSWORD);
        }
        authService.ensurePassword("admin@auction.local", DEMO_PASSWORD);
    }
}
