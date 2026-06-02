# 04 - Database và DAO

Phần này giải thích cách dữ liệu được lưu, đọc và ánh xạ giữa SQLite và object Java.

## Nguyên tắc quan trọng

```text
Client không truy cập database trực tiếp.
Mọi thao tác database đi qua:
Client -> Gateway -> ServerFacade -> Service -> DAO -> SQLite
```

## Vì sao dùng DAO?

```text
DAO tách SQL khỏi nghiệp vụ.
Service chỉ quan tâm "lưu auction", "tìm user", "lấy danh sách auction".
DAO chịu trách nhiệm viết SELECT/INSERT/UPDATE/DELETE.
Nếu sau này đổi SQLite sang MySQL, chủ yếu sửa DAO.
```

---

## schema.sql

File:

```text
auction-server/src/main/resources/db/schema.sql
```

Nhiệm vụ:

```text
Tạo cấu trúc database ban đầu.
Được DatabaseManager đọc khi app khởi động.
```

Các bảng:

```text
users
items
auctions
bids
auto_bids
```

---

## Bảng users

Lưu tài khoản và thông tin profile.

Các dữ liệu quan trọng:

```text
id
username
email
role
password_hash
shipping_address
phone_number
store_name
store_description
department
avatar_path
```

Code liên quan:

```text
User.java
Admin.java / Seller.java / Bidder.java
AuthService.java
UserService.java
SqliteUserDao.java
PasswordHasher.java
```

Nghiệp vụ liên quan:

```text
Đăng ký
Đăng nhập
Quên mật khẩu
Cập nhật profile
Admin xem danh sách user
```

---

## Bảng items

Lưu vật phẩm đem ra đấu giá.

Dữ liệu quan trọng:

```text
id
name
description
starting_price
item_type
image_path
```

Code liên quan:

```text
Item.java
Art/Electronics/Vehicle/Other.java
ItemFactory.java
SellerService.java
SqliteItemDao.java
```

Nghiệp vụ liên quan:

```text
Seller tạo sản phẩm.
Auction hiển thị item.
```

---

## Bảng auctions

Lưu phiên đấu giá.

Dữ liệu quan trọng:

```text
id
item_id
seller_id
current_price
status
winner_id
end_time
```

Code liên quan:

```text
Auction.java
AuctionService.java
BidService.java
SqliteAuctionDao.java
AuctionExpiryScheduler.java
```

Nghiệp vụ liên quan:

```text
Tạo phiên
Bắt đầu phiên
Kết thúc phiên
Hủy phiên
Thanh toán
Tự động hết hạn
```

---

## Bảng bids

Lưu lịch sử từng lượt đặt giá.

Dữ liệu quan trọng:

```text
auction_id
bidder_id
amount
bid_time
```

Code liên quan:

```text
BidTransaction.java
Auction.java
BidService.java
SqliteAuctionDao.java
AuctionDetailController.java
```

Nghiệp vụ liên quan:

```text
Đặt giá
Xem lịch sử bid
Vẽ chart giá
Tính winner/currentPrice
```

---

## Bảng auto_bids

Lưu cấu hình đấu giá tự động.

Dữ liệu quan trọng:

```text
auction_id
bidder_id
max_price
increment
```

Code liên quan:

```text
AutoBidConfig.java
AutoBidDao.java
SqliteAutoBidDao.java
BidService.java
AuctionDetailController.java
```

Nghiệp vụ liên quan:

```text
Bidder bật/tắt auto-bid.
BidService tự động nâng giá theo maxPrice/increment.
```

---

## DatabaseManager.java

File:

```text
auction-server/src/main/java/com/auction/db/DatabaseManager.java
```

Nhiệm vụ:

```text
1. Tạo JDBC connection.
2. Bật PRAGMA foreign_keys = ON.
3. Load schema.sql.
4. Chạy CREATE TABLE nếu chưa có.
5. Migration nhẹ bằng ALTER TABLE khi thiếu cột mới.
```

Điểm cần giải thích:

```text
SQLite mặc định có thể không enforce foreign key trên connection mới,
nên getConnection() bật PRAGMA foreign_keys = ON mỗi lần tạo connection.
```

Migration nhẹ:

```text
Nếu database cũ thiếu password_hash, avatar_path, image_path, end_time...
DatabaseManager kiểm tra PRAGMA table_info rồi ALTER TABLE thêm cột.
```

---

## DbMappers.java

File:

```text
auction-server/src/main/java/com/auction/db/DbMappers.java
```

Nhiệm vụ:

```text
Chuyển dữ liệu từ ResultSet/database thành object Java.
```

Phân biệt:

```text
DbMappers: mapping database row -> model.
DtoMapper: mapping model <-> DTO để truyền qua socket.
```

---

## SqliteUserDao.java

Nhiệm vụ:

```text
CRUD user.
Lưu password_hash.
Tìm user theo email khi login.
Cập nhật password khi reset.
Cập nhật profile.
```

Ai gọi:

```text
AuthService
UserService
BidService auto-bid
```

Câu trả lời:

```text
AuthService không tự viết SQL. Nó gọi UserDao, implementation thật là SqliteUserDao.
```

---

## SqliteItemDao.java

Nhiệm vụ:

```text
Lưu/đọc item.
Chuyển item_type trong DB thành subclass Item phù hợp.
```

Ai gọi:

```text
SellerService
SqliteAuctionDao
```

---

## SqliteAuctionDao.java

Đây là DAO phức tạp nhất.

Nhiệm vụ:

```text
Lưu/đọc auction.
Khi đọc auction phải dựng lại item, seller, winner, bids.
Khi save phải cập nhật currentPrice, status, winner, endTime và lịch sử bid.
```

Ai gọi:

```text
AuctionService
BidService
AuctionExpiryScheduler gián tiếp qua AuctionService
```

Cần hiểu:

```text
Auction không chỉ là một bảng đơn giản. Nó liên kết items, users và bids.
Vì vậy SqliteAuctionDao phải phối hợp ItemDao/UserDao và DbMappers.
```

---

## SqliteAutoBidDao.java

Nhiệm vụ:

```text
save AutoBidConfig.
find config theo auctionId + bidderId.
getAutoBidsForAuction cho BidService.
delete khi bidder hủy auto-bid.
```

Điểm nâng cao:

```text
save dùng UPSERT:
INSERT ... ON CONFLICT(auction_id, bidder_id) DO UPDATE
```

---

## In-memory DAO

Các file:

```text
InMemoryUserDao.java
InMemoryItemDao.java
InMemoryAuctionDao.java
InMemoryAutoBidDao.java
```

Nhiệm vụ:

```text
Dùng collection trong RAM thay SQLite.
Phục vụ unit test để test service nhanh, không phụ thuộc file DB thật.
```

---

## Luồng database mẫu: đăng nhập

```text
AuthController
  -> LoginViewModel
  -> Gateway.login
  -> AuctionServerFacade.login
  -> AuthService.login
  -> UserDao.findByEmail
  -> SqliteUserDao SELECT users WHERE email = ?
  -> PasswordHasher.matches
```

## Luồng database mẫu: đặt giá

```text
AuctionDetailController
  -> Gateway.placeBid
  -> AuctionServerFacade.placeBid
  -> BidService.placeBid
  -> AuctionDao.findById
  -> Auction.addBid
  -> AuctionDao.save
  -> SqliteAuctionDao UPDATE auctions + save bids
```

## Câu trả lời khi bị hỏi

```text
Database của dự án dùng SQLite vì dễ chạy, không cần cài server DB riêng.
Code không viết SQL trực tiếp trong controller/service mà đi qua DAO.
DatabaseManager tự tạo schema và có migration nhẹ để database cũ vẫn chạy được.
```
