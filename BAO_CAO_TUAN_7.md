# Báo Cáo Tuần 7: Đa Luồng Và Song Song

## Mục tiêu tuần 7

Theo tiêu chí của bài giảng tuần này, hệ thống cần:

- triển khai `Observer Pattern` để notify khi có bid mới
- có logic nghiệp vụ tạo phiên đấu giá, đặt giá và kiểm tra hợp lệ
- có logic chuyển trạng thái `OPEN -> RUNNING -> FINISHED -> PAID/CANCELED`
- xử lý `concurrent bidding`, tránh race condition và lost update
- sử dụng `synchronized` và `ReentrantLock` cho các thao tác critical
- tự học JavaFX để có các màn hình cơ bản như `Login` và `Danh sách`

## Thay đổi đã thực hiện

### 1. Observer Pattern cho bid mới

Đã bổ sung package [src/main/java/com/auction/observer](/D:/BaitaplonTest/src/main/java/com/auction/observer) gồm:

- [BidObserver.java](/D:/BaitaplonTest/src/main/java/com/auction/observer/BidObserver.java)
- [BidEvent.java](/D:/BaitaplonTest/src/main/java/com/auction/observer/BidEvent.java)
- [ConsoleBidObserver.java](/D:/BaitaplonTest/src/main/java/com/auction/observer/ConsoleBidObserver.java)

Trong [Auction.java](/D:/BaitaplonTest/src/main/java/com/auction/model/auction/Auction.java), mỗi auction hiện có:

- danh sách observer
- `addObserver(...)`
- `removeObserver(...)`
- `notifyBidPlaced(...)`

Khi có bid hợp lệ mới, auction sẽ tạo `BidEvent` và notify cho observer.

### 2. Hoàn thiện logic nghiệp vụ đấu giá

Đã giữ và chỉnh lại các service chính:

- [AuctionService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/AuctionService.java)
- [BidService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/BidService.java)
- [SellerService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/SellerService.java)
- [ItemFactory.java](/D:/BaitaplonTest/src/main/java/com/auction/factory/ItemFactory.java)
- [ItemType.java](/D:/BaitaplonTest/src/main/java/com/auction/enums/ItemType.java)

Kết quả:

- tạo item theo `ART`, `ELECTRONICS`, `VEHICLE`
- tạo auction và lưu vào `InMemoryAuctionDao`
- đặt giá có validate:
  - auction phải tồn tại
  - auction phải đang cho phép bid
  - giá mới phải cao hơn giá hiện tại

### 3. Chỉnh state machine theo tiêu chí tuần 7

Đã cập nhật [AuctionStatus.java](/D:/BaitaplonTest/src/main/java/com/auction/enums/AuctionStatus.java) thành:

- `OPEN`
- `RUNNING`
- `FINISHED`
- `PAID`
- `CANCELED`

Đã cập nhật [Auction.java](/D:/BaitaplonTest/src/main/java/com/auction/model/auction/Auction.java) để chạy theo flow:

- auction mới tạo ra ở trạng thái `OPEN`
- `start()` chuyển `OPEN -> RUNNING`
- `finish()` chuyển `RUNNING -> FINISHED`
- `markPaid()` chuyển `FINISHED -> PAID`
- `cancel()` cho phép hủy từ `OPEN`, `RUNNING` hoặc `FINISHED`

Bid chỉ được chấp nhận khi auction ở `RUNNING`.

### 4. Concurrent bidding

Đã giữ và nâng cấp phần xử lý bid đồng thời:

- [BidService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/BidService.java) sử dụng `synchronized (auction)` để bảo vệ critical section ở service layer
- [Auction.java](/D:/BaitaplonTest/src/main/java/com/auction/model/auction/Auction.java) sử dụng `ReentrantLock` để bảo vệ state transition và thao tác thêm bid ở model layer

Mục đích:

- tránh hai thread cập nhật giá cùng lúc mà bỏ sót update
- đảm bảo `currentPrice`, `winner`, `bids` được cập nhật nhất quán

### 5. `synchronized` và `ReentrantLock`

Đã đáp ứng cả hai yêu cầu:

- `synchronized`
  - [BidService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/BidService.java)
  - [AuctionManager.java](/D:/BaitaplonTest/src/main/java/com/auction/manager/AuctionManager.java)
- `ReentrantLock`
  - [Auction.java](/D:/BaitaplonTest/src/main/java/com/auction/model/auction/Auction.java)

### 6. JavaFX cơ bản: Login và Danh sách

Đã bổ sung wiring JavaFX tối thiểu để app không còn dừng ở một `Label`.

File mới/chính:

- [Main.java](/D:/BaitaplonTest/src/main/java/com/auction/Main.java)
- [AppContext.java](/D:/BaitaplonTest/src/main/java/com/auction/app/AppContext.java)
- [SceneNavigator.java](/D:/BaitaplonTest/src/main/java/com/auction/app/SceneNavigator.java)
- [AuthController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AuthController.java)
- [AuctionController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AuctionController.java)
- [login-view.fxml](/D:/BaitaplonTest/src/main/resources/fxml/login-view.fxml)
- [auction-list-view.fxml](/D:/BaitaplonTest/src/main/resources/fxml/auction-list-view.fxml)
- [app.css](/D:/BaitaplonTest/src/main/resources/css/app.css)

Hiện tại app có:

- màn hình `Login`
- màn hình `Auction List`
- dữ liệu seed sẵn để demo
- 3 tài khoản demo:
  - `seller@auction.local`
  - `bidder@auction.local`
  - `admin@auction.local`

## Kiểm thử đã thực hiện

Đã cập nhật và compile lại test cho flow mới:

- [AuctionTest.java](/D:/BaitaplonTest/src/test/java/com/auction/model/auction/AuctionTest.java)
- [AuctionServiceTest.java](/D:/BaitaplonTest/src/test/java/com/auction/service/AuctionServiceTest.java)
- [BidServiceTest.java](/D:/BaitaplonTest/src/test/java/com/auction/service/BidServiceTest.java)
- [ConcurrentBidTest.java](/D:/BaitaplonTest/src/test/java/com/auction/concurrency/ConcurrentBidTest.java)

Kết quả xác minh:

- `javac` compile pass cho `src/main/java`
- `javac` compile pass cho `src/test/java`
- chạy JUnit Platform bằng runner tạm: `35/35 tests successful`

## Đánh giá theo tiêu chí tuần 7

1. `Observer Pattern để notify khi có bid mới`
- Đạt

2. `Code logic nghiệp vụ: tạo phiên đấu giá, đặt giá, kiểm tra hợp lệ`
- Đạt

3. `Logic chuyển trạng thái OPEN -> RUNNING -> FINISHED -> PAID/CANCELED`
- Đạt

4. `Xử lý đấu giá đồng thời`
- Đạt ở mức backend core

5. `Sử dụng synchronized, ReentrantLock`
- Đạt

6. `JavaFX cơ bản: Login, Danh sách`
- Đạt ở mức tối thiểu để demo

## Giới hạn hiện tại

- chưa có persistence thật, vẫn dùng `in-memory DAO`
- login mới ở mức email-based, chưa có password và phân quyền đầy đủ
- màn `auction detail` và `seller view` mới là placeholder
- observer hiện đang demo theo dạng console notification

## Tổng kết

Sau đợt thay đổi này, code hiện tại đã đáp ứng đủ bộ tiêu chí tuần 7 ở mức phù hợp để review và demo:

- có observer cho bid mới
- có state machine đúng yêu cầu
- có concurrent bidding với cơ chế đồng bộ rõ ràng
- có sử dụng cả `synchronized` và `ReentrantLock`
- có giao diện JavaFX cơ bản cho `Login` và `Auction List`
