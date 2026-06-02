# 09 - Server deep dive: học kỹ server, service, DAO, socket

File này giải thích sâu `auction-server` theo method và luồng gọi. Đây là phần cần học kỹ nhất nếu muốn hiểu nghiệp vụ thật.

---

## 1. ServerMain.java

### Vai trò

`ServerMain` là entry point khi chạy server riêng bằng jar.

### Luồng chạy

```text
main(args)
  -> đọc port nếu có
  -> tạo ServerContext
  -> tạo AuctionServerFacade
  -> tạo AuctionExpiryScheduler và start()
  -> tạo AuctionSocketServer
  -> socketServer.start()
```

### Khi nào dùng?

```text
Chạy socket mode, đặc biệt khi demo client-server hoặc 2 máy.
```

### Nếu server không chạy?

Client socket sẽ lỗi không kết nối được ở `SocketAuctionClientGateway.connect()`.

---

## 2. ServerContext.java

### Vai trò

Đây là nơi lắp ráp dependency server.

### Constructor ServerContext(String jdbcUrl)

Luồng:

```text
1. new DatabaseManager(jdbcUrl)
2. databaseManager.initializeSchema()
3. new SqliteUserDao(databaseManager)
4. new SqliteItemDao(databaseManager)
5. new SqliteAuctionDao(databaseManager, itemDao, userDao)
6. new SqliteAutoBidDao(databaseManager)
7. new AuthService(userDao)
8. new AuctionService(auctionDao)
9. new SellerService(itemDao, auctionDao)
10. new BidService(auctionDao, autoBidDao, userDao)
11. new UserService(userDao)
12. seedData()
```

### seedData()

Nhiệm vụ:

```text
Tạo tài khoản demo nếu chưa có.
Tạo dữ liệu auction demo nếu database đang trống.
```

### ensureDemoUsers()

Tạo:

```text
seller@auction.local / demo12345
bidder@auction.local / demo12345
admin@auction.local  / demo12345
```

Nếu user đã tồn tại nhưng chưa có password hash, `AuthService.ensurePassword()` sẽ bổ sung.

### Câu trả lời

```text
ServerContext cho biết toàn bộ server được ráp như thế nào. Nhìn vào đây sẽ biết service nào dùng DAO nào.
```

---

## 3. AuctionServerFacade.java

### Vai trò

Facade là cổng gọi service.

### login(email, password)

```text
Gọi AuthService.login().
Trả User nếu đúng mật khẩu.
```

### register(username, email, password, role)

```text
Nếu role là Seller -> AuthService.registerSeller().
Nếu role là Bidder -> AuthService.registerBidder().
Role Admin không được đăng ký từ UI thường.
```

### listAuctions()

Gọi `AuctionService.listAuctions()`.

### listAuctionsForSeller(sellerId)

Lọc danh sách auction theo sellerId.

### createAuctionForSeller(...)

Luồng:

```text
1. Chuyển itemType string thành ItemType.
2. SellerService.createItem().
3. SellerService.createAuction().
4. Trả Auction.
```

### startAuction/finishAuction/cancelAuction/markAuctionPaid

Chỉ chuyển tiếp sang `AuctionService`.

### placeBid(auctionId, bidder, amount)

Chuyển tiếp sang `BidService.placeBid()`.

### registerAutoBid/cancelAutoBid/getAutoBid

Làm việc trực tiếp với `AutoBidDao`.

### updateUser(...)

Chuyển tiếp sang `UserService.updateUser()`.

### Vì sao facade quan trọng?

```text
AuctionSocketServer chỉ cần biết facade, không cần giữ 5 service khác nhau.
LocalAuctionClientGateway cũng dùng cùng facade nên local/socket thống nhất.
```

---

## 4. AuthService.java

### registerSeller/registerBidder/registerAdmin

Luồng:

```text
1. validateUserInput(username, email, password).
2. validateEmailNotExists(email).
3. Tạo Seller/Bidder/Admin.
4. userDao.save(user, PasswordHasher.hash(password)).
5. Trả user.
```

### login(email, password)

Luồng:

```text
1. userDao.findByEmail(email).
2. Nếu không có user -> AuthenticationException.
3. userDao.getPasswordHash(email).
4. PasswordHasher.matches(password, passwordHash).
5. Nếu sai -> AuthenticationException.
6. Nếu đúng -> trả user.
```

### ensurePassword(email, password)

Dùng khi seed demo. Nếu user demo đã tồn tại nhưng chưa có password hash thì cập nhật.

### resetPassword(email, username, newPassword)

Luồng:

```text
1. Tìm user theo email.
2. Kiểm tra username khớp.
3. validatePassword(newPassword).
4. userDao.updatePasswordHash(email, PasswordHasher.hash(newPassword)).
```

### validateUserInput()

Kiểm tra username/email/password không rỗng và password hợp lệ.

### validatePassword()

Kiểm tra password đủ điều kiện tối thiểu.

### Lỗi có thể xảy ra

```text
Email đã tồn tại.
Sai email/password.
Password rỗng hoặc quá ngắn.
Username không khớp khi reset.
```

---

## 5. SellerService.java

### createItem(String type,...)

Nhận type dạng string, chuyển thành `ItemType`, gọi overload dùng `ItemType`.

### createItem(ItemType type,...)

Tạo item không có ảnh.

### createItem(ItemType type,..., imagePath)

Luồng:

```text
1. Validate type/name/startingPrice.
2. ItemFactory.createItem().
3. Gắn imagePath nếu có.
4. itemDao.save(item).
5. Trả item.
```

### createAuction(Item item, Seller seller)

Luồng:

```text
1. Validate item và seller không null.
2. new Auction(null/id, item, seller).
3. auctionDao.save(auction).
4. Trả auction.
```

### getItemById(itemId)

Chuyển tiếp sang `itemDao.findById()`.

---

## 6. AuctionService.java

### createAuction(item, seller)

Tạo Auction mới và lưu DAO.

### startAuction(auctionId)

Luồng:

```text
1. auctionDao.findById(auctionId).
2. Nếu null -> lỗi.
3. auction.start().
4. auctionDao.save(auction).
```

### finishAuction(auctionId)

Tương tự start, nhưng gọi `auction.finish()`.

### cancelAuction(auctionId)

Gọi `auction.cancel()` rồi save.

### markAuctionPaid(auctionId)

Gọi `auction.markPaid()` rồi save.

### listAuctions()

Gọi `auctionDao.findAll()`.

### getAuctionById(auctionId)

Gọi `auctionDao.findById()`.

### Chỗ hay bị hỏi

```text
AuctionService không tự đổi status trực tiếp.
Nó gọi method trên Auction để rule trạng thái nằm trong domain object.
```

---

## 7. BidService.java

Đây là service quan trọng nhất.

### Constructor

```text
BidService(AuctionDao auctionDao)
  Dùng cho test đơn giản không auto-bid.

BidService(AuctionDao auctionDao, AutoBidDao autoBidDao, UserDao userDao)
  Dùng trong app thật để hỗ trợ auto-bid.
```

### placeBid(auctionId, bidder, amount)

Luồng chi tiết:

```text
1. Kiểm tra auctionId không rỗng.
2. Kiểm tra bidder không null.
3. Kiểm tra amount là số hợp lệ và > 0.
4. auctionDao.findById(auctionId).
5. Nếu không tìm thấy -> AuctionException.
6. synchronized(auction).
7. Kiểm tra auction.isOpen() tức status RUNNING.
8. Kiểm tra amount > auction.currentPrice.
9. Tạo BidTransaction.
10. auction.addBid(bid).
11. Nếu có autoBidDao và userDao -> runAutoBiddingEngine(auction).
12. auctionDao.save(auction).
```

### Vì sao vừa synchronized vừa Auction có ReentrantLock?

```text
synchronized(auction) khóa toàn bộ quy trình service: check giá, addBid, auto-bid, save DAO.
ReentrantLock trong Auction bảo vệ state nội bộ nếu Auction.addBid() bị gọi từ nơi khác.
Hai lớp bảo vệ ở hai tầng khác nhau.
```

### runAutoBiddingEngine(auction)

Mục tiêu:

```text
Sau khi có bid mới, tự động đặt thêm bid cho những bidder đã bật auto-bid.
```

Luồng:

```text
1. autoBidDao.getAutoBidsForAuction(auctionId).
2. Lấy winner hiện tại và currentPrice.
3. Tạo PriorityQueue<PendingAutoBid>.
4. Với mỗi config không phải winner hiện tại:
   - nextBid = currentPrice + increment.
   - nếu maxPrice >= nextBid thì đưa vào queue.
5. Trong khi queue không rỗng:
   - poll bidder có nextBidAmount thấp nhất.
   - tính bidAmount = max(nextBidAmount, currentPrice + increment).
   - nếu maxBid đủ thì tạo BidTransaction tự động.
   - auction.addBid(autoBid).
   - nếu winner cũ cũng có auto-bid và còn đủ maxPrice thì đưa lại vào queue.
   - điều chỉnh lại nextBidAmount của các bidder còn lại.
```

### PendingAutoBid

Class private dùng trong PriorityQueue.

```text
bidderId
maxBid
increment
nextBidAmount
```

`compareTo()` so sánh theo `nextBidAmount`, ai cần bid thấp hơn được xử lý trước.

### Lỗi có thể xảy ra

```text
auctionId rỗng.
bidder null.
amount <= 0 hoặc NaN/Infinity.
auction không tồn tại.
auction không RUNNING.
amount <= currentPrice.
```

---

## 8. AuctionSocketServer.java

### start()

Luồng:

```text
1. new ServerSocket(port).
2. running = true.
3. while running:
   - accept client socket.
   - threadPool.submit(() -> handleClient(clientSocket)).
```

### handleClient(socket)

Luồng:

```text
1. Tạo ObjectInputStream/ObjectOutputStream.
2. while true đọc AuctionRequest.
3. Nếu client ngắt thì remove khỏi BroadcastManager nếu là subscriber.
4. Nếu request là SUBSCRIBE_UPDATES:
   - BroadcastManager.addClient(out).
   - đánh dấu isSubscriber = true.
   - continue.
5. Request thường:
   - processRequest(request).
   - write AuctionResponse.
   - flush/reset.
```

### processRequest(request)

Switch theo `RequestType`.

Ví dụ quan trọng:

```text
LOGIN:
  facade.login()
  return AuctionResponse.ok(UserDto)

REGISTER:
  facade.register()
  broadcast USER_REGISTERED

CREATE_AUCTION:
  dựng Seller từ request
  facade.createAuctionForSeller()
  broadcast AUCTION_CREATED

PLACE_BID:
  dựng Bidder từ request
  facade.placeBid()
  broadcast NEW_BID

REGISTER_AUTO_BID:
  facade.registerAutoBid()
  broadcast NEW_BID

UPDATE_USER:
  facade.updateUser()
  broadcast USER_UPDATED
```

### Vì sao processRequest bắt Exception?

Để lỗi nghiệp vụ không làm sập server thread. Nếu service ném lỗi, server trả `AuctionResponse.error(message)`.

---

## 9. BroadcastManager.java

### clients

Danh sách stream của client đang nghe realtime.

### addClient(out)

Thêm stream vào danh sách.

### removeClient(out)

Xóa stream khi client ngắt.

### broadcast(event)

Luồng:

```text
1. synchronized(clients).
2. Duyệt từng ObjectOutputStream.
3. writeObject(event), flush, reset.
4. Nếu lỗi, đưa vào deadClients.
5. Xóa deadClients khỏi clients.
```

### Chỗ cần nhớ

```text
BroadcastManager không tự biết khi nào dữ liệu đổi.
AuctionSocketServer hoặc AuctionExpiryScheduler gọi broadcast.
```

---

## 10. AuctionExpiryScheduler.java

### start()

Chạy `checkExpiredAuctions()` định kỳ mỗi 10 giây.

### stop()

Shutdown scheduler sạch.

### checkExpiredAuctions()

Luồng:

```text
1. now = LocalDateTime.now().
2. allAuctions = auctionService.listAuctions().
3. Với mỗi auction:
   - status == RUNNING
   - endTime != null
   - now.isAfter(endTime)
4. auctionService.finishAuction(auctionId).
5. BroadcastManager.broadcast(AUCTION_FINISHED).
6. Nếu local mode, cố gọi ClientEventManager.fireUpdate() qua reflection.
```

### Vì sao dùng reflection?

`auction-server` có thể chạy độc lập không có client UI. Nếu import trực tiếp JavaFX/client class sẽ làm server phụ thuộc client. Reflection giúp nếu class tồn tại thì gọi, không thì bỏ qua.

---

## 11. DAO deep dive

### SqliteUserDao

Method cần hiểu:

```text
save(user, passwordHash):
  Lưu user khi đăng ký hoặc bootstrap demo.

findById(id):
  Tìm user theo id.

findByEmail(email):
  Dùng khi login/reset.

findAll():
  Admin xem danh sách user.

getPasswordHash(email):
  AuthService lấy hash để kiểm tra password.

updatePasswordHash(email, hash):
  Reset password hoặc ensure demo password.

updateUser(...):
  Cập nhật profile/avatar.
```

### SqliteItemDao

Method cần hiểu:

```text
save(item):
  Lưu item vào bảng items.

findById(id):
  Dùng khi dựng Auction từ DB.

findAll():
  Lấy danh sách item nếu cần.
```

### SqliteAuctionDao

Method cần hiểu:

```text
save(auction):
  Lưu trạng thái auction.
  Lưu item nếu cần.
  Lưu/currentPrice/status/winner/endTime.
  Lưu bids.

findById(id):
  Đọc auction, dựng lại item/seller/winner/bids.

findAll():
  Đọc tất cả auction.
```

Điểm khó:

```text
Auction là aggregate nên DAO phải restoreState để tái tạo đúng status/currentPrice/winner/bids/endTime.
```

### SqliteAutoBidDao

Method:

```text
save(config):
  UPSERT theo auction_id + bidder_id.

getAutoBidsForAuction(auctionId):
  BidService dùng để chạy auto-bidding engine.

find(auctionId, bidderId):
  UI kiểm tra bidder đã bật auto-bid chưa.

delete(auctionId, bidderId):
  Hủy auto-bid.
```

---

## 12. DatabaseManager deep dive

### getConnection()

```text
DriverManager.getConnection(jdbcUrl)
statement.execute("PRAGMA foreign_keys = ON")
return connection
```

### initializeSchema()

```text
1. getConnection().
2. loadSchemaSql().
3. split theo dấu ;
4. execute từng câu SQL.
5. ensure các cột mới.
```

### ensureUsersColumn()

Kiểm tra `PRAGMA table_info(users)`. Nếu thiếu cột thì `ALTER TABLE users ADD COLUMN`.

### ensureItemsImagePathColumn()

Đảm bảo bảng items có `image_path`.

### ensureAuctionsEndTimeColumn()

Đảm bảo bảng auctions có `end_time`.

### loadSchemaSql()

Đọc `/db/schema.sql` từ resources.

---

## Checklist hiểu server

```text
[ ] ServerMain khởi động server thế nào?
[ ] ServerContext tạo DAO/service nào?
[ ] Facade gọi service nào?
[ ] AuthService login/register/reset ra sao?
[ ] AuctionService đổi trạng thái thế nào?
[ ] BidService.placeBid kiểm tra những gì?
[ ] Auto-bid engine dùng PriorityQueue ra sao?
[ ] AuctionSocketServer xử lý RequestType thế nào?
[ ] BroadcastManager gửi realtime ra sao?
[ ] Scheduler tự kết thúc phiên ra sao?
[ ] DAO SQLite đọc/ghi aggregate Auction ra sao?
```
