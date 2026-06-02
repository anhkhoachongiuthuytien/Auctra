# 02 - auction-server: giải thích từng file

`auction-server` là bộ não của hệ thống. Nó xử lý nghiệp vụ, database, socket, realtime broadcast, scheduler tự kết thúc phiên và seed dữ liệu demo.

## Luồng server tổng quát

```text
ServerMain
  -> ServerContext
  -> AuctionServerFacade
  -> Service
  -> DAO
  -> DatabaseManager / SQLite
```

Nếu chạy socket:

```text
Client socket
  -> AuctionSocketServer
  -> AuctionServerFacade
  -> Service
  -> DAO
```

---

## server package

### ServerMain.java

```text
Tầng: Entry point của server socket.
Nhiệm vụ: Khởi động server độc lập.
Ai chạy: java -jar auction-server/target/auction-server.jar.
Gọi ai: ServerContext, AuctionServerFacade, AuctionExpiryScheduler, AuctionSocketServer.
```

Cần hiểu:

```text
Khi demo 2 máy hoặc socket mode, phải chạy ServerMain trước.
```

### ServerContext.java

```text
Tầng: Composition root của server.
Nhiệm vụ: Khởi tạo toàn bộ dependency server.
Tạo: DatabaseManager, Sqlite DAO, Service, demo data.
Ai gọi: ServerMain, AppContext local mode.
```

Các việc chính:

```text
1. Tạo DatabaseManager bằng JDBC URL.
2. initializeSchema().
3. Tạo SqliteUserDao, SqliteItemDao, SqliteAuctionDao, SqliteAutoBidDao.
4. Tạo AuthService, AuctionService, SellerService, BidService, UserService.
5. Seed tài khoản demo và auction demo.
```

Câu trả lời:

```text
ServerContext là nơi lắp ráp hệ thống server. Nhìn vào file này sẽ biết service nào dùng DAO nào.
```

### AuctionServerFacade.java

```text
Tầng: Facade Pattern.
Nhiệm vụ: Là cổng truy cập duy nhất từ network/local gateway vào service.
Ai gọi: LocalAuctionClientGateway, AuctionSocketServer.
Gọi ai: AuthService, SellerService, AuctionService, BidService, UserService, AutoBidDao.
```

Ví dụ:

```text
facade.login()       -> AuthService.login()
facade.placeBid()    -> BidService.placeBid()
facade.startAuction()-> AuctionService.startAuction()
```

Câu trả lời:

```text
Facade giúp AuctionSocketServer không phải biết chi tiết từng service. Network layer chỉ gọi facade.
```

### AuctionSocketServer.java

```text
Tầng: Network layer.
Nhiệm vụ: TCP server nhận AuctionRequest, xử lý và trả AuctionResponse.
Ai gọi: ServerMain.
Gọi ai: AuctionServerFacade, BroadcastManager, DtoMapper.
Tính năng nâng cao: thread pool, persistent connection, realtime subscribe.
```

Các phần phải hiểu:

```text
start()          -> mở ServerSocket, accept client, đưa vào thread pool.
handleClient()   -> đọc nhiều request trên cùng socket.
processRequest() -> switch theo RequestType.
SUBSCRIBE_UPDATES-> đăng ký client vào BroadcastManager.
```

Khi có thao tác làm đổi dữ liệu:

```text
REGISTER          -> broadcast USER_REGISTERED
CREATE_AUCTION    -> broadcast AUCTION_CREATED
START_AUCTION     -> broadcast AUCTION_STARTED
FINISH_AUCTION    -> broadcast AUCTION_FINISHED
PLACE_BID         -> broadcast NEW_BID
UPDATE_USER       -> broadcast USER_UPDATED
```

### BroadcastManager.java

```text
Tầng: Realtime infrastructure.
Nhiệm vụ: Giữ danh sách ObjectOutputStream của các client đã subscribe.
Ai gọi: AuctionSocketServer, AuctionExpiryScheduler.
Gửi gì: AuctionEvent.
```

Cần hiểu:

```text
clients là synchronizedList.
broadcast() synchronized khi duyệt danh sách để an toàn thread.
Client hỏng/mất kết nối sẽ bị loại khỏi danh sách.
```

### AuctionExpiryScheduler.java

```text
Tầng: Background job.
Nhiệm vụ: Tự động kết thúc các auction đã hết giờ.
Ai gọi: ServerMain và AppContext local mode.
Gọi ai: AuctionService.finishAuction(), BroadcastManager.broadcast().
```

Cách chạy:

```text
Mỗi 10 giây:
  -> listAuctions()
  -> tìm auction RUNNING có now > endTime
  -> finishAuction()
  -> broadcast AUCTION_FINISHED
```

Điểm nâng cao:

```text
Scheduler dùng daemon thread nên app có thể tắt sạch.
Trong local mode, nó cố gọi ClientEventManager qua reflection để UI cập nhật.
```

---

## service package

### AuthService.java

```text
Tầng: Business service.
Nhiệm vụ: Đăng ký, đăng nhập, reset password, đảm bảo password demo.
Ai gọi: AuctionServerFacade, ServerContext seedData.
Gọi ai: UserDao, PasswordHasher.
```

Các method chính:

```text
registerSeller()
registerBidder()
registerAdmin()
login()
ensurePassword()
resetPassword()
```

Cần hiểu:

```text
AuthService không lưu password gốc.
Nó gọi PasswordHasher.hash() khi đăng ký và PasswordHasher.matches() khi đăng nhập.
```

### UserService.java

```text
Tầng: Business service.
Nhiệm vụ: Quản lý thông tin user/profile.
Ai gọi: AuctionServerFacade.
Gọi ai: UserDao.
```

Cần hiểu:

```text
Dùng cho admin list users và profile update.
```

### SellerService.java

```text
Tầng: Business service.
Nhiệm vụ: Tạo item và tạo auction cho seller.
Ai gọi: AuctionServerFacade.
Gọi ai: ItemFactory, ItemDao, AuctionDao.
```

Luồng:

```text
createItem()    -> ItemFactory tạo đúng loại item -> itemDao.save()
createAuction() -> new Auction(item, seller) -> auctionDao.save()
```

### AuctionService.java

```text
Tầng: Business service.
Nhiệm vụ: Quản lý vòng đời phiên đấu giá.
Ai gọi: AuctionServerFacade, AuctionExpiryScheduler.
Gọi ai: AuctionDao, Auction model.
```

Các method chính:

```text
createAuction()
startAuction()
finishAuction()
cancelAuction()
markAuctionPaid()
listAuctions()
```

Cần hiểu:

```text
AuctionService tìm auction, gọi method trạng thái trên Auction, rồi save lại.
```

### BidService.java

```text
Tầng: Business service quan trọng nhất.
Nhiệm vụ: Xử lý đặt giá và auto-bid.
Ai gọi: AuctionServerFacade.
Gọi ai: AuctionDao, Auction.addBid(), AutoBidDao, UserDao.
Tính năng nâng cao: synchronized, auto-bid PriorityQueue, anti-sniping gián tiếp qua Auction.addBid().
```

Luồng placeBid:

```text
1. Validate auctionId, bidder, amount.
2. auctionDao.findById(auctionId).
3. synchronized(auction).
4. Kiểm tra auction đang RUNNING.
5. Kiểm tra amount > currentPrice.
6. Tạo BidTransaction.
7. auction.addBid(bid).
8. Nếu có autoBidDao/userDao thì runAutoBiddingEngine().
9. auctionDao.save(auction).
```

Câu trả lời:

```text
BidService xử lý nghiệp vụ bên ngoài, còn Auction.addBid() bảo vệ rule bên trong object Auction.
```

---

## db package

### DatabaseManager.java

```text
Tầng: Database infrastructure.
Nhiệm vụ: Tạo connection, bật foreign key, initialize schema, migration nhẹ.
Ai gọi: ServerContext, DAO.
```

Cần hiểu:

```text
getConnection() bật PRAGMA foreign_keys = ON.
initializeSchema() load db/schema.sql.
Nếu thiếu cột mới thì ALTER TABLE thêm cột.
```

### DbMappers.java

```text
Tầng: Database mapper.
Nhiệm vụ: Chuyển row từ ResultSet thành User/Item/Auction object.
Ai gọi: Sqlite DAO.
```

Khác với DtoMapper:

```text
DbMappers dùng cho database.
DtoMapper dùng cho socket/protocol.
```

---

## dao/sqlite package

### SqliteUserDao.java

```text
Tầng: DAO SQLite.
Nhiệm vụ: CRUD user và password_hash.
Implements: UserDao.
Ai gọi: AuthService, UserService, BidService auto-bid.
```

Cần hiểu:

```text
findByEmail dùng cho login.
save(user, passwordHash) dùng cho register.
updatePasswordHash dùng cho reset password/demo.
updateUser dùng cho profile.
```

### SqliteItemDao.java

```text
Tầng: DAO SQLite.
Nhiệm vụ: Lưu/đọc item.
Implements: ItemDao.
Ai gọi: SellerService, SqliteAuctionDao.
```

### SqliteAuctionDao.java

```text
Tầng: DAO SQLite quan trọng.
Nhiệm vụ: Lưu/đọc auction, kèm item, seller, winner, bids.
Implements: AuctionDao.
Ai gọi: AuctionService, BidService.
Gọi ai: ItemDao, UserDao, DbMappers.
```

Cần hiểu:

```text
Auction là object phức tạp nên khi đọc DB phải dựng lại cả item, seller, winner, bids.
Khi save auction phải lưu trạng thái, currentPrice, winner, endTime và lịch sử bids.
```

### SqliteAutoBidDao.java

```text
Tầng: DAO SQLite.
Nhiệm vụ: Lưu/đọc/xóa cấu hình auto-bid.
Implements: AutoBidDao.
Ai gọi: AuctionServerFacade, BidService.
```

Cần hiểu:

```text
save() dùng UPSERT theo khóa auction_id + bidder_id.
getAutoBidsForAuction() dùng trong BidService.runAutoBiddingEngine().
```

---

## dao/memory package

### InMemoryUserDao.java

```text
Tầng: DAO in-memory.
Nhiệm vụ: Lưu user bằng collection trong RAM.
Ai dùng: Unit test hoặc chế độ không cần SQLite.
```

### InMemoryItemDao.java

```text
Tầng: DAO in-memory.
Nhiệm vụ: Lưu item bằng collection trong RAM.
Ai dùng: Unit test.
```

### InMemoryAuctionDao.java

```text
Tầng: DAO in-memory.
Nhiệm vụ: Lưu auction bằng collection trong RAM.
Ai dùng: Unit test service.
```

### InMemoryAutoBidDao.java

```text
Tầng: DAO in-memory.
Nhiệm vụ: Lưu cấu hình auto-bid bằng collection trong RAM.
Ai dùng: AutoBidPriorityQueueTest hoặc test liên quan.
```

---

## observer package

### ConsoleBidObserver.java

```text
Tầng: Observer implementation.
Nhiệm vụ: In log khi có bid mới.
Ai gọi: Auction thông qua BidObserver.
Mục đích: Minh họa Observer Pattern/debug.
```

---

## resources

### db/schema.sql

```text
Tầng: Database schema.
Nhiệm vụ: Tạo bảng users, items, auctions, bids, auto_bids.
Ai đọc: DatabaseManager.initializeSchema().
```

Các bảng:

```text
users      -> tài khoản, role, password_hash, profile
items      -> vật phẩm
auctions   -> phiên đấu giá
bids       -> lịch sử đặt giá
auto_bids  -> cấu hình đấu giá tự động
```

---

## Tóm tắt server cần nhớ

```text
ServerMain chỉ khởi động.
ServerContext lắp ráp dependency.
AuctionServerFacade là cổng vào service.
Service chứa nghiệp vụ.
DAO chứa SQL.
AuctionSocketServer chứa network protocol.
BroadcastManager chứa realtime subscribers.
AuctionExpiryScheduler tự kết thúc phiên hết hạn.
```
