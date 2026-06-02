# 06 - Tính năng nâng cao của dự án

File này gom những phần dễ được hỏi xoáy. Đây là các điểm chứng minh dự án không chỉ CRUD đơn giản.

## Danh sách tính năng nâng cao

```text
1. Dual-Socket realtime
2. Thread safety khi đặt giá đồng thời
3. Auto-Bid bằng PriorityQueue
4. Anti-Sniping
5. Scheduler tự kết thúc phiên
6. Password hashing PBKDF2
7. DAO + SQLite + migration nhẹ
8. DTO/protocol/serialization
9. UI nâng cao: countdown, chart, toast, theme, image
```

---

## 1. Dual-Socket realtime

Files:

```text
auction-client/src/main/java/com/auction/client/SocketAuctionClientGateway.java
auction-client/src/main/java/com/auction/client/ClientEventManager.java
auction-server/src/main/java/com/auction/server/AuctionSocketServer.java
auction-server/src/main/java/com/auction/server/BroadcastManager.java
auction-common/src/main/java/com/auction/protocol/AuctionEvent.java
```

Cách hoạt động:

```text
Client có socket chính để gửi request/nhận response.
Client có socket phụ để nghe event realtime.
Server giữ danh sách client realtime trong BroadcastManager.
Khi có thay đổi, server gửi AuctionEvent về các client.
Client nhận event và reload UI.
```

Câu trả lời:

```text
Realtime không phải polling liên tục. Server chủ động push event về client qua socket phụ.
```

---

## 2. Thread safety khi đặt giá đồng thời

Files:

```text
auction-server/src/main/java/com/auction/service/BidService.java
auction-common/src/main/java/com/auction/model/auction/Auction.java
auction-server/src/test/java/com/auction/concurrency/ConcurrentBidTest.java
```

Vấn đề:

```text
Hai bidder có thể đặt giá gần như cùng lúc.
Nếu cả hai cùng đọc currentPrice cũ, cả hai có thể tưởng giá của mình hợp lệ.
Kết quả có thể sai currentPrice hoặc winner.
```

Cách xử lý:

```text
BidService dùng synchronized(auction) để khóa theo từng phiên.
Auction.addBid() dùng ReentrantLock để bảo vệ currentPrice, winner, status, bids.
```

Câu trả lời:

```text
BidService khóa ở tầng service để bảo vệ toàn bộ quy trình đặt giá và lưu DAO.
Auction dùng ReentrantLock để bảo vệ trạng thái nội bộ của chính object Auction.
```

---

## 3. Auto-Bid bằng PriorityQueue

Files:

```text
auction-common/src/main/java/com/auction/model/auction/AutoBidConfig.java
auction-common/src/main/java/com/auction/dao/AutoBidDao.java
auction-server/src/main/java/com/auction/dao/sqlite/SqliteAutoBidDao.java
auction-server/src/main/java/com/auction/service/BidService.java
auction-client/src/main/java/com/auction/controller/AuctionDetailController.java
auction-server/src/test/java/com/auction/service/AutoBidPriorityQueueTest.java
```

Khái niệm:

```text
Bidder đặt maxPrice và increment.
Hệ thống tự nâng giá cho bidder khi cần, nhưng không vượt maxPrice.
```

Luồng:

```text
1. Bidder bật Auto-Bid trên AuctionDetailController.
2. Gateway gửi REGISTER_AUTO_BID.
3. Server lưu AutoBidConfig qua AutoBidDao.
4. Khi có bid mới, BidService.runAutoBiddingEngine() chạy.
5. PriorityQueue tính bidder nào cần đặt giá tiếp theo.
6. Auction.addBid() được gọi cho các bid tự động hợp lệ.
```

Vì sao dùng PriorityQueue:

```text
PriorityQueue giúp lấy lượt auto-bid có nextBidAmount thấp nhất trước.
Nó mô phỏng quá trình tăng giá từng bước, không nhảy thẳng lên maxPrice.
```

Câu trả lời:

```text
Auto-Bid là tính năng tự động đặt giá theo maxPrice và increment.
BidService dùng PriorityQueue để xử lý thứ tự các lượt nâng giá tự động.
```

---

## 4. Anti-Sniping

Files:

```text
auction-common/src/main/java/com/auction/model/auction/Auction.java
auction-server/src/test/java/com/auction/service/AntiSnipingTest.java
```

Vấn đề:

```text
Người chơi có thể chờ sát giờ kết thúc rồi đặt giá,
khiến người khác không còn thời gian phản ứng.
```

Cách xử lý:

```text
Trong Auction.addBid(), sau khi bid hợp lệ, gọi checkAndApplyAntiSniping().
Nếu còn dưới 60 giây trước endTime, hệ thống cộng thêm 60 giây.
```

Câu trả lời:

```text
Anti-Sniping giúp phiên đấu giá công bằng hơn bằng cách gia hạn khi có bid sát giờ.
```

---

## 5. Scheduler tự kết thúc phiên

Files:

```text
auction-server/src/main/java/com/auction/server/AuctionExpiryScheduler.java
auction-server/src/main/java/com/auction/service/AuctionService.java
auction-server/src/main/java/com/auction/server/BroadcastManager.java
```

Cách hoạt động:

```text
Scheduler chạy mỗi 10 giây.
Nó lấy danh sách auction.
Nếu auction RUNNING và now > endTime, nó gọi finishAuction().
Sau đó broadcast AUCTION_FINISHED.
```

Điểm hay:

```text
Không cần người dùng bấm kết thúc thủ công.
UI socket client tự cập nhật qua realtime event.
```

---

## 6. Password hashing PBKDF2

Files:

```text
auction-common/src/main/java/com/auction/util/PasswordHasher.java
auction-server/src/main/java/com/auction/service/AuthService.java
auction-server/src/main/java/com/auction/dao/sqlite/SqliteUserDao.java
```

Cách lưu:

```text
Không lưu password gốc.
Lưu dạng: iterations:salt:hash
```

Thông số:

```text
Algorithm: PBKDF2WithHmacSHA256
Iterations: 65,536
Salt length: 16 bytes
Key length: 256 bits
```

Câu trả lời:

```text
PBKDF2 làm việc dò mật khẩu chậm hơn brute-force thường.
Salt giúp hai người cùng password vẫn có hash khác nhau.
constantTimeEquals giảm rủi ro timing attack.
```

---

## 7. DAO + SQLite + migration nhẹ

Files:

```text
auction-server/src/main/resources/db/schema.sql
auction-server/src/main/java/com/auction/db/DatabaseManager.java
auction-server/src/main/java/com/auction/dao/sqlite/*.java
auction-common/src/main/java/com/auction/dao/*.java
```

Điểm nâng cao:

```text
DAO tách SQL khỏi service.
SQLite dễ chạy vì chỉ là một file .db.
DatabaseManager tự tạo schema nếu chưa có.
DatabaseManager kiểm tra thiếu cột và ALTER TABLE thêm cột mới.
PreparedStatement giúp tránh SQL injection.
Foreign key được bật bằng PRAGMA foreign_keys = ON.
```

---

## 8. DTO/protocol/serialization

Files:

```text
AuctionRequest.java
AuctionResponse.java
AuctionEvent.java
RequestType.java
DtoMapper.java
AuctionDto.java
UserDto.java
BidDto.java
```

Điểm nâng cao:

```text
Client-server không gửi string tự do.
Mọi thao tác được chuẩn hóa bằng RequestType.
Data được gói trong AuctionRequest/AuctionResponse.
DTO giúp kiểm soát dữ liệu truyền qua mạng.
ObjectOutputStream/ObjectInputStream serialize object Java qua TCP.
```

---

## 9. UI nâng cao

Files:

```text
auction-client/src/main/java/com/auction/controller/AuctionDetailController.java
auction-client/src/main/java/com/auction/ui/ThemeManager.java
auction-client/src/main/java/com/auction/ui/UIAnimations.java
auction-client/src/main/java/com/auction/ui/ToastManager.java
auction-client/src/main/java/com/auction/util/UiEffects.java
auction-common/src/main/java/com/auction/util/ImageStorage.java
auction-client/src/main/resources/css/**
```

Tính năng:

```text
Countdown thời gian còn lại.
ProgressBar theo thời gian.
LineChart lịch sử giá.
TableView lịch sử bid.
Auto-Bid form.
Toast thông báo.
Dark/light theme.
Ảnh vật phẩm và avatar.
Animation shake/pulse/bounce.
```

---

## Câu tổng kết phần nâng cao

```text
Dự án nâng cao ở chỗ không chỉ CRUD. Nó có realtime dual-socket,
xử lý đồng thời khi nhiều người đặt giá, auto-bid bằng PriorityQueue,
anti-sniping, scheduler tự kết thúc phiên, bảo mật mật khẩu PBKDF2,
DAO SQLite có migration nhẹ và UI JavaFX có countdown/chart/theme/toast.
```
