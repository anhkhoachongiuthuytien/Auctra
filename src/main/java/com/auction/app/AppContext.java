package com.auction.app;

import com.auction.dao.sqlite.SqliteAuctionDao;
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

public class AppContext {
    private final DatabaseManager databaseManager;
    private final SqliteUserDao userDao;
    private final SqliteItemDao itemDao;
    private final SqliteAuctionDao auctionDao;
    private final AuthService authService;
    private final SellerService sellerService;
    private final AuctionService auctionService;
    private final BidService bidService;

    public AppContext() {
        this.databaseManager = new DatabaseManager("jdbc:sqlite:auction-system.db");
        this.databaseManager.initializeSchema();

        // Tại đây app chọn implementation SQLite thật thay cho các DAO in-memory.
        this.userDao = new SqliteUserDao(databaseManager);
        this.itemDao = new SqliteItemDao(databaseManager);
        this.auctionDao = new SqliteAuctionDao(databaseManager, itemDao, userDao);
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
        // Chỉ seed khi database chưa có auction nào để tránh nhân bản dữ liệu demo qua mỗi lần chạy app.
        if (!auctionService.listAuctions().isEmpty()) {
            return;
        }

        ensureDemoUsers();

        Seller seller = (Seller) authService.login("seller@auction.local");
        Bidder bidder = (Bidder) authService.login("bidder@auction.local");

        Item laptop = sellerService.createItem("Electronics", "Gaming Laptop", "RTX laptop for concurrent bidding demo", 1500.0);
        Item car = sellerService.createItem("Vehicle", "Used Sedan", "Auction state machine sample", 8000.0);
        Item art = sellerService.createItem("Art", "Landscape Painting", "Observer pattern sample item", 500.0);

        // Global observer giúp demo ngay cơ chế observer mà không cần đăng ký thủ công ở từng màn hình.
        Auction.addGlobalObserver(new ConsoleBidObserver());

        Auction laptopAuction = auctionService.createAuction(laptop, seller);
        Auction carAuction = auctionService.createAuction(car, seller);
        Auction artAuction = auctionService.createAuction(art, seller);

        auctionService.startAuction(laptopAuction.getId());
        auctionService.startAuction(artAuction.getId());

        bidService.placeBid(laptopAuction.getId(), bidder, 1700.0);

        auctionService.startAuction(carAuction.getId());
        auctionService.finishAuction(carAuction.getId());
        auctionService.markAuctionPaid(carAuction.getId());
    }

    private void ensureDemoUsers() {
        // Kiểm tra theo email trước khi tạo để seedData có thể chạy lặp lại mà không làm trùng user.
        if (!authService.emailExists("seller@auction.local")) {
            authService.registerSeller("seller_demo", "seller@auction.local");
        }
        if (!authService.emailExists("bidder@auction.local")) {
            authService.registerBidder("bidder_demo", "bidder@auction.local");
        }
        if (!authService.emailExists("admin@auction.local")) {
            authService.registerAdmin("admin_demo", "admin@auction.local");
        }
    }
}
