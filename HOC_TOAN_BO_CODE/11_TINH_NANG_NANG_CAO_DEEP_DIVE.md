# 11 - Tính năng nâng cao deep dive

File này giải thích chi tiết các tính năng nâng cao, theo kiểu có thể nói lại khi bị hỏi xoáy.

---

## 1. Realtime Dual-Socket

### Vấn đề

Nếu chỉ dùng request/response:

```text
Bidder A đặt giá.
Server lưu giá mới.
Bidder B/Seller/Admin sẽ không biết ngay trừ khi tự bấm refresh.
```

### Giải pháp

Client mở thêm socket phụ để nghe event.

```text
Socket chính:
  Client gửi request -> Server trả response.

Socket phụ:
  Client subscribe -> Server push AuctionEvent khi có thay đổi.
```

### Luồng subscribe

```text
SocketAuctionClientGateway.startListeningForUpdates()
  -> new Socket(host, port)
  -> gửi AuctionRequest(RequestType.SUBSCRIBE_UPDATES)
```

Server:

```text
AuctionSocketServer.handleClient()
  -> nếu request.getType() == SUBSCRIBE_UPDATES
  -> BroadcastManager.addClient(out)
  -> giữ socket mở
```

### Luồng broadcast

Khi có bid:

```text
AuctionSocketServer case PLACE_BID
  -> facade.placeBid()
  -> BroadcastManager.broadcast(new AuctionEvent("NEW_BID", auctionId))
```

Client:

```text
listener thread đọc AuctionEvent
  -> Platform.runLater(ClientEventManager::fireUpdate)
  -> AuctionDetailController.reloadAuction()
```

### Vì sao không dùng polling?

Polling là client cứ vài giây gọi server một lần. Cách đó tốn request và có độ trễ. Broadcast realtime giúp server chủ động báo khi thật sự có thay đổi.

### Vì sao cần Platform.runLater?

Socket listener chạy ở thread nền. JavaFX UI chỉ được cập nhật trên JavaFX Application Thread. Vì vậy phải đưa việc reload UI vào `Platform.runLater`.

---

## 2. Xử lý đồng thời khi đặt giá

### Vấn đề race condition

Giả sử currentPrice = 100.

```text
Thread A đọc currentPrice = 100, muốn bid 120.
Thread B đọc currentPrice = 100, muốn bid 110.
Nếu không khóa, cả hai đều tưởng hợp lệ.
Kết quả lưu cuối có thể làm winner/currentPrice sai.
```

### Cách dự án xử lý

Ở `BidService.placeBid()`:

```text
synchronized (auction) {
  kiểm tra trạng thái
  kiểm tra amount > currentPrice
  auction.addBid()
  runAutoBiddingEngine()
  auctionDao.save()
}
```

Ở `Auction.addBid()`:

```text
stateLock.lock()
try {
  kiểm tra RUNNING
  kiểm tra bid hợp lệ
  cập nhật bids/currentPrice/winner
  anti-sniping
} finally {
  stateLock.unlock()
}
```

### Vì sao cần cả hai?

```text
synchronized ở service bảo vệ quy trình nghiệp vụ dài: check -> add -> auto-bid -> save.
ReentrantLock ở domain bảo vệ trạng thái nội bộ Auction nếu có code khác gọi addBid.
```

### Test chứng minh

```text
ConcurrentBidTest
BidServiceTest
AuctionTest
```

---

## 3. Auto-Bid PriorityQueue

### Ý tưởng nghiệp vụ

Bidder không muốn ngồi canh đấu giá. Họ đặt:

```text
maxPrice: giá tối đa chịu trả
increment: mỗi lần tự tăng bao nhiêu
```

Ví dụ:

```text
Current price = 100.
Bidder A auto-bid max 200, increment 10.
Bidder B đặt giá 120.
Hệ thống có thể tự đặt cho A lên 130 nếu A còn đủ maxPrice.
```

### Lưu cấu hình

UI:

```text
AuctionDetailController.handleToggleAutoBid()
  -> gateway.registerAutoBid(auctionId, bidderId, maxPrice, increment)
```

Server:

```text
AuctionServerFacade.registerAutoBid()
  -> AutoBidDao.save(new AutoBidConfig(...))
```

DB:

```text
auto_bids(auction_id, bidder_id, max_price, increment)
```

### Khi nào engine chạy?

Trong `BidService.placeBid()`, sau khi bid thường được add:

```text
if (autoBidDao != null && userDao != null) {
  runAutoBiddingEngine(auction);
}
```

### PriorityQueue hoạt động thế nào?

Mỗi config hợp lệ được biến thành `PendingAutoBid`:

```text
bidderId
maxBid
increment
nextBidAmount
```

Queue sắp theo `nextBidAmount` tăng dần.

Luồng:

```text
1. Lấy winner hiện tại.
2. Với mỗi auto-bid config không phải winner:
   nextBid = currentPrice + increment.
   nếu maxPrice >= nextBid thì đưa vào queue.
3. Lấy người có nextBid thấp nhất.
4. Nếu vẫn chưa phải winner và maxBid đủ:
   tạo BidTransaction tự động.
   auction.addBid(autoBid).
5. Nếu winner cũ có auto-bid và còn đủ tiền:
   đưa winner cũ lại vào queue.
6. Điều chỉnh nextBidAmount của các bidder còn lại theo currentPrice mới.
7. Lặp tới khi không còn ai đủ maxPrice.
```

### Vì sao không nhảy thẳng lên maxPrice?

Đấu giá thật thường tăng từng bước. Nếu nhảy thẳng lên maxPrice thì lộ giới hạn tối đa của bidder và làm giá tăng không cần thiết.

---

## 4. Anti-Sniping

### Sniping là gì?

Người chơi đợi sát giờ kết thúc mới đặt giá, khiến người khác không kịp phản ứng.

### Logic trong dự án

Trong `Auction.checkAndApplyAntiSniping()`:

```text
Nếu endTime != null
và status == RUNNING
và now nằm trong 60 giây cuối
thì endTime = endTime + 60 giây.
```

### Khi nào gọi?

Trong `Auction.addBid()` sau khi bid hợp lệ đã cập nhật giá/winner.

### Câu trả lời

```text
Anti-sniping đảm bảo nếu có bid mới sát giờ, phiên được gia hạn để người khác có thời gian phản hồi.
```

---

## 5. Scheduler tự kết thúc phiên

### Vấn đề

Nếu auction có `endTime`, hệ thống cần tự kết thúc khi hết giờ, không thể đợi seller/admin bấm finish.

### Cách xử lý

`AuctionExpiryScheduler` chạy nền:

```text
scheduler.scheduleAtFixedRate(checkExpiredAuctions, 10s, 10s)
```

### checkExpiredAuctions()

```text
1. now = LocalDateTime.now().
2. allAuctions = auctionService.listAuctions().
3. Nếu auction RUNNING và now > auction.endTime:
   - auctionService.finishAuction(auction.id).
   - BroadcastManager.broadcast(AUCTION_FINISHED).
```

### Local mode đặc biệt

Trong local mode không có socket event, scheduler cố gọi `ClientEventManager.fireUpdate()` qua reflection để UI cập nhật.

### Vì sao daemon thread?

Daemon thread không giữ JVM sống mãi khi app đã tắt.

---

## 6. Password hashing

### Vì sao không lưu plain password?

Nếu lộ database, attacker thấy toàn bộ mật khẩu người dùng.

### Dự án lưu gì?

```text
iterations:salt:hash
```

Ví dụ ý nghĩa:

```text
iterations:
  số vòng PBKDF2.

salt:
  chuỗi ngẫu nhiên riêng cho từng password.

hash:
  kết quả PBKDF2.
```

### hash()

```text
1. Tạo salt bằng SecureRandom.
2. Chạy PBKDF2WithHmacSHA256.
3. Base64 encode salt/hash.
4. Ghép thành string.
```

### matches()

```text
1. Split storedHash.
2. Decode salt và expectedHash.
3. Hash rawPassword với salt/iterations cũ.
4. constantTimeEquals(expectedHash, actualHash).
```

### constantTimeEquals()

So sánh byte-by-byte nhưng không thoát sớm khi gặp sai. Điều này giảm khả năng timing attack.

---

## 7. DAO, transaction và SQLite

### DAO nâng cao ở đâu?

```text
Interface nằm ở auction-common/dao.
Implementation nằm ở auction-server/dao/sqlite.
Test có implementation in-memory.
```

### PreparedStatement

DAO dùng PreparedStatement:

```text
SELECT * FROM users WHERE email = ?
statement.setString(1, email)
```

Tác dụng:

```text
Tránh SQL injection.
Code rõ kiểu dữ liệu.
```

### Foreign key

DatabaseManager bật:

```text
PRAGMA foreign_keys = ON
```

Để SQLite kiểm tra quan hệ:

```text
auction.seller_id -> users.id
auction.item_id -> items.id
bid.auction_id -> auctions.id
bid.bidder_id -> users.id
```

### Migration nhẹ

Nếu DB cũ thiếu cột:

```text
DatabaseManager kiểm tra PRAGMA table_info.
Nếu không thấy cột -> ALTER TABLE ADD COLUMN.
```

Điều này giúp không phải xóa database mỗi khi thêm field mới.

---

## 8. DTO và serialization

### Serialization

Client/server dùng:

```text
ObjectOutputStream.writeObject()
ObjectInputStream.readObject()
```

Object truyền qua mạng phải `Serializable`.

### DTO

DTO giúp:

```text
Không truyền password_hash.
Không truyền object quá phức tạp.
Giữ protocol ổn định.
Tránh phụ thuộc sâu vào object nội bộ.
```

### DtoMapper

Server:

```text
Model -> DTO -> AuctionResponse.data
```

Client:

```text
AuctionResponse.data -> DTO -> Model
```

---

## 9. UI nâng cao

### Countdown

Trong `AuctionDetailController.startCountdown()`:

```text
Mỗi giây tính endTime - now.
Update label.
Update progressBar.
Nếu hết giờ thì ẩn form bid và reloadAuction().
```

### Chart giá

`updatePriceChart()`:

```text
Điểm đầu là startingPrice.
Mỗi bid thêm một điểm theo amount.
```

### Auto-Bid form

UI kiểm tra:

```text
maxPrice không rỗng.
maxPrice là số.
increment > 0.
maxPrice >= currentPrice + increment.
```

### Theme

`ThemeManager.toggle(scene)` đổi CSS theme light/dark.

### Toast/Animation

```text
UiEffects.showToast()
UIAnimations.shakeField()
UIAnimations.successBounce()
UIAnimations.pulsePrice()
```

### Image

Seller chọn/kéo thả ảnh. Ảnh được lưu bằng `ImageStorage`, sau đó đường dẫn gắn vào item để màn chi tiết load lại.

---

## Câu tổng hợp khi bị hỏi "dự án có gì nâng cao?"

```text
Dự án có realtime dual-socket để server push update,
xử lý đồng thời bằng synchronized và ReentrantLock khi đặt giá,
auto-bid bằng PriorityQueue,
anti-sniping gia hạn khi bid sát giờ,
scheduler tự kết thúc phiên hết hạn,
password hashing PBKDF2 có salt và constant-time compare,
DAO SQLite có PreparedStatement, foreign key và migration nhẹ,
DTO/protocol để truyền dữ liệu an toàn,
và UI JavaFX có countdown, chart, theme, toast, image upload.
```

## Checklist nâng cao

```text
[ ] Giải thích được dual-socket.
[ ] Giải thích được vì sao phải Platform.runLater.
[ ] Giải thích được race condition khi bid.
[ ] Giải thích được synchronized và ReentrantLock.
[ ] Giải thích được Auto-Bid PriorityQueue.
[ ] Giải thích được Anti-Sniping.
[ ] Giải thích được Scheduler.
[ ] Giải thích được PasswordHasher.
[ ] Giải thích được DAO/PreparedStatement.
[ ] Giải thích được DTO.
[ ] Giải thích được countdown/chart/theme/toast/image.
```
