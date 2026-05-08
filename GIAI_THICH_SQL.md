# Giải Thích Phần SQL

File này tóm tắt các phần có dùng SQL trong project và mỗi phần đang làm công việc gì.

## 1. `schema.sql`

File: [schema.sql](/D:/BaitaplonTest/src/main/resources/db/schema.sql)

Chức năng:

- bật `PRAGMA foreign_keys = ON`
- tạo bảng `users`
- tạo bảng `items`
- tạo bảng `auctions`
- tạo bảng `bids`

Ý nghĩa từng bảng:

- `users`: lưu tài khoản và vai trò (`SELLER`, `BIDDER`, `ADMIN`)
- `items`: lưu vật phẩm đấu giá
- `auctions`: lưu phiên đấu giá, giá hiện tại, trạng thái, người thắng
- `bids`: lưu lịch sử từng lần đặt giá

## 2. `DatabaseManager.java`

File: [DatabaseManager.java](/D:/BaitaplonTest/src/main/java/com/auction/db/DatabaseManager.java)

Chức năng:

- mở kết nối JDBC đến SQLite
- bật khóa ngoại cho mỗi connection
- đọc `schema.sql`
- chạy từng câu lệnh SQL để khởi tạo database

Nói ngắn gọn:

- đây là lớp nền để ứng dụng có thể tự tạo database khi chạy lần đầu

## 3. `SqliteUserDao.java`

File: [SqliteUserDao.java](/D:/BaitaplonTest/src/main/java/com/auction/dao/sqlite/SqliteUserDao.java)

Các câu SQL chính:

- `INSERT ... ON CONFLICT(id) DO UPDATE`
  - lưu user mới hoặc cập nhật user cũ
- `SELECT ... WHERE id = ?`
  - tìm user theo id
- `SELECT ... WHERE email = ?`
  - tìm user theo email
- `SELECT ... ORDER BY username`
  - lấy toàn bộ user

Mục đích:

- phục vụ đăng ký, đăng nhập và tra cứu tài khoản

## 4. `SqliteItemDao.java`

File: [SqliteItemDao.java](/D:/BaitaplonTest/src/main/java/com/auction/dao/sqlite/SqliteItemDao.java)

Các câu SQL chính:

- `INSERT ... ON CONFLICT(id) DO UPDATE`
  - lưu item mới hoặc cập nhật item cũ
- `SELECT ... WHERE id = ?`
  - tìm item theo id
- `SELECT ... ORDER BY name`
  - lấy danh sách item
- `DELETE FROM items WHERE id = ?`
  - xóa item

Mục đích:

- quản lý danh sách vật phẩm đấu giá

## 5. `SqliteAuctionDao.java`

File: [SqliteAuctionDao.java](/D:/BaitaplonTest/src/main/java/com/auction/dao/sqlite/SqliteAuctionDao.java)

Đây là file SQL quan trọng nhất vì nó xử lý cả `auction` và `bid history`.

### 5.1. Lưu auction

SQL:

- `INSERT INTO auctions ... ON CONFLICT(id) DO UPDATE`

Mục đích:

- lưu trạng thái phiên đấu giá hiện tại
- cập nhật `current_price`
- cập nhật `status`
- cập nhật `winner_id`

### 5.2. Xóa bids cũ của auction

SQL:

- `DELETE FROM bids WHERE auction_id = ?`

Mục đích:

- xóa snapshot bid cũ trước khi ghi lại snapshot mới

### 5.3. Ghi lại danh sách bids

SQL:

- `INSERT INTO bids(auction_id, bidder_id, amount, bid_time) VALUES (?, ?, ?, ?)`

Mục đích:

- lưu toàn bộ lịch sử bid hiện tại của auction

### 5.4. Tìm auction theo id

SQL:

- `SELECT id, item_id, seller_id, current_price, status, winner_id FROM auctions WHERE id = ?`

Mục đích:

- lấy một auction cụ thể
- sau đó map thêm item, seller, winner và bids

### 5.5. Lấy toàn bộ auction

SQL:

- `SELECT ... FROM auctions ORDER BY id`

Mục đích:

- hiển thị danh sách auction trên giao diện

### 5.6. Load bid history

SQL:

- `SELECT bidder_id, amount, bid_time FROM bids WHERE auction_id = ? ORDER BY bid_time`

Mục đích:

- khôi phục đúng lịch sử bid theo thứ tự thời gian

## 6. Vì sao `SqliteAuctionDao.save()` dùng transaction?

Lý do:

- nếu chỉ lưu `auctions` mà chưa lưu `bids`, dữ liệu sẽ lệch nhau
- transaction giúp đảm bảo:
  - hoặc lưu hết cả `auction` và `bids`
  - hoặc rollback nếu có lỗi

Nghĩa là dữ liệu luôn nhất quán hơn.

## 7. Vì sao xóa bids rồi insert lại?

Trong project này, `Auction` đang được coi như một `aggregate`.

Khi lưu một auction:

- hệ thống lưu lại toàn bộ trạng thái auction
- đồng thời lưu lại toàn bộ danh sách bids hiện tại

Ưu điểm:

- code đơn giản
- dễ hiểu
- phù hợp với đồ án và demo

Nhược điểm:

- chưa tối ưu cho hệ thống lớn

## 8. Quan hệ dữ liệu quan trọng

- `auctions.item_id -> items.id`
- `auctions.seller_id -> users.id`
- `auctions.winner_id -> users.id`
- `bids.auction_id -> auctions.id`
- `bids.bidder_id -> users.id`

Lưu ý:

- `bids.auction_id` có `ON DELETE CASCADE`
- nghĩa là xóa auction thì bid history của auction đó cũng bị xóa theo

## 9. Cách giải thích ngắn khi thầy hỏi

Có thể trả lời:

> Phần SQL của nhóm em gồm 3 phần chính: schema để tạo bảng, DatabaseManager để mở kết nối và khởi tạo database, và các DAO SQLite để chạy câu lệnh CRUD. Trong đó SqliteAuctionDao là phần quan trọng nhất vì nó vừa lưu trạng thái auction vừa lưu lịch sử bid bằng transaction để dữ liệu nhất quán.
