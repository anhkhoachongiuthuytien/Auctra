# Báo Cáo Database

## 1. Mục tiêu

Bổ sung tầng lưu trữ dữ liệu thật cho hệ thống đấu giá để thay thế cách lưu tạm bằng bộ nhớ. Phần này phục vụ mục tiêu chấm tiến độ: có thiết kế database, có lớp kết nối giữa ứng dụng và database, và mô tả được luồng `client -> service -> database`.

## 2. Lựa chọn công nghệ

- Database được chọn: `SQLite`
- Thư viện JDBC: `org.xerial:sqlite-jdbc:3.50.1.0`

Lý do chọn SQLite:

- không cần cài đặt server database riêng
- dễ demo và dễ nộp bài tiến độ
- phù hợp với cấu trúc Java hiện tại dùng DAO
- có thể nâng cấp lên MySQL/PostgreSQL sau mà không cần đổi service layer quá nhiều

## 3. Cấu trúc database

Schema được đặt tại [schema.sql](/D:/BaitaplonTest/src/main/resources/db/schema.sql).

### Bảng `users`

- `id`: khóa chính
- `username`: tên hiển thị
- `email`: duy nhất
- `role`: vai trò `SELLER`, `BIDDER`, `ADMIN`

### Bảng `items`

- `id`: khóa chính
- `name`: tên vật phẩm
- `description`: mô tả
- `starting_price`: giá khởi điểm
- `type`: loại vật phẩm `ART`, `VEHICLE`, `ELECTRONICS`

### Bảng `auctions`

- `id`: khóa chính
- `item_id`: khóa ngoại sang `items`
- `seller_id`: khóa ngoại sang `users`
- `current_price`: giá hiện tại
- `status`: trạng thái phiên đấu giá
- `winner_id`: khóa ngoại sang `users`, cho phép `NULL`

### Bảng `bids`

- `id`: khóa chính tự tăng
- `auction_id`: khóa ngoại sang `auctions`
- `bidder_id`: khóa ngoại sang `users`
- `amount`: mức giá đặt
- `bid_time`: thời điểm đặt giá

## 4. Tầng kết nối database

Đã bổ sung các thành phần sau:

- [DatabaseManager.java](/D:/BaitaplonTest/src/main/java/com/auction/db/DatabaseManager.java): tạo kết nối JDBC, bật foreign key, khởi tạo schema
- [DbMappers.java](/D:/BaitaplonTest/src/main/java/com/auction/db/DbMappers.java): map dữ liệu giữa object Java và giá trị lưu trong bảng
- [SqliteUserDao.java](/D:/BaitaplonTest/src/main/java/com/auction/dao/sqlite/SqliteUserDao.java)
- [SqliteItemDao.java](/D:/BaitaplonTest/src/main/java/com/auction/dao/sqlite/SqliteItemDao.java)
- [SqliteAuctionDao.java](/D:/BaitaplonTest/src/main/java/com/auction/dao/sqlite/SqliteAuctionDao.java)

Các DAO này giữ nguyên interface hiện có, nên service layer không bị thay đổi lớn. Đây là điểm quan trọng để giữ code dễ mở rộng và dễ chuyển đổi giữa `in-memory` và `database-backed` implementation.

## 5. Thay đổi trong kiến trúc hệ thống

Kiến trúc hiện tại được tách thành 3 tầng:

1. `Client`
   - JavaFX GUI
   - người dùng thao tác trên màn hình `Login` và `Auction List`

2. `Server / Business layer`
   - `AuthService`, `AuctionService`, `BidService`, `SellerService`
   - xử lý nghiệp vụ, validation, exception, concurrency

3. `Database layer`
   - `DatabaseManager`
   - các `Sqlite*Dao`
   - schema và dữ liệu SQLite

Luồng xử lý tổng quát:

`JavaFX Controller -> ViewModel -> Service -> DAO -> SQLite`

Phần khởi tạo trung tâm được cấu hình trong [AppContext.java](/D:/BaitaplonTest/src/main/java/com/auction/app/AppContext.java). File này hiện đang:

- tạo `DatabaseManager("jdbc:sqlite:auction-system.db")`
- khởi tạo schema nếu chưa tồn tại
- gán `SqliteUserDao`, `SqliteItemDao`, `SqliteAuctionDao` vào các service
- seed dữ liệu demo nếu database chưa có auction

## 6. Dữ liệu demo

Hệ thống hiện tại tự động tạo các tài khoản và auction mẫu khi database trống:

- `seller@auction.local`
- `bidder@auction.local`
- `admin@auction.local`

Dữ liệu này giúp demo nhanh các luồng:

- đăng nhập
- xem danh sách auction
- đặt giá
- kết thúc phiên
- lưu lại lịch sử bid sau khi tắt app

## 7. Điều chỉnh model và service để hỗ trợ persistence

Để lưu và phục hồi dữ liệu đầy đủ, đã bổ sung:

- [Auction.java](/D:/BaitaplonTest/src/main/java/com/auction/model/auction/Auction.java)
  - thêm `restoreState(...)` để nạp lại trạng thái, winner và lịch sử bid từ database
  - thêm `global observer` để giữ cơ chế notify khi app tải lại đối tượng auction
- [BidTransaction.java](/D:/BaitaplonTest/src/main/java/com/auction/model/auction/BidTransaction.java)
  - thêm constructor có `bidTime` để phục hồi bid từ dữ liệu đã lưu
- [AuctionService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/AuctionService.java)
  - sau mỗi thay đổi trạng thái sẽ gọi `auctionDao.save(...)`
- [BidService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/BidService.java)
  - sau khi đặt giá hợp lệ sẽ lưu lại auction và bids xuống database

## 8. Kiểm thử và xác minh

Đã bổ sung integration test:

- [SqlitePersistenceTest.java](/D:/BaitaplonTest/src/test/java/com/auction/db/SqlitePersistenceTest.java)

Nội dung test:

- tạo database tạm
- khởi tạo schema
- lưu seller, bidder, item, auction
- start auction
- đặt bid
- load lại auction từ SQLite
- kiểm tra giá hiện tại, winner và lịch sử bid

Kết quả xác minh:

- lệnh `mvn test` đã chạy thành công
- tổng cộng `60 tests`
- `0 failures`, `0 errors`

## 9. Đánh giá hiện trạng

Phần database hiện đã đạt mục tiêu tiến độ:

- có schema rõ ràng
- có tầng DAO sử dụng database thật
- có luồng kết nối từ GUI đến database
- có test xác minh persistence

Nhưng vẫn còn các hướng mở rộng tiếp theo:

- bổ sung migration/versioning cho schema
- tách server thành process riêng nếu cần dùng mô hình client-server thật
- thêm polling hoặc socket để hỗ trợ realtime update
- thêm màn hình tạo item, tạo auction và chi tiết auction đầy đủ hơn

## 10. Kết luận

Project đã được nâng cấp từ mô hình `in-memory` sang mô hình có `SQLite database`. Cấu trúc hiện tại phù hợp để demo tiến độ bài tập lớn, đồng thời giữ được khả năng mở rộng cho các tuần sau mà không cần viết lại toàn bộ service layer.
