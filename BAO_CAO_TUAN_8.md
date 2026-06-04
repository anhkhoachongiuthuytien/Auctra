# Báo Cáo Tuần 8: Kiểm Thử, Ngoại Lệ Và GUI Nâng Cao

## Mục tiêu tuần 8

Theo tiêu chí của tuần 8, hệ thống cần:

- có custom exceptions rõ ràng
- xử lý ngoại lệ cho đặt giá thấp, đấu giá khi phiên đóng, lỗi dữ liệu
- có unit test JUnit cho logic chính
- refactor code theo hướng sạch hơn, giảm code smell, áp dụng SOLID/MVC rõ hơn
- tiếp tục hoàn thiện GUI JavaFX và tách logic khỏi controller

## Thay đổi đã thực hiện

### 1. Bổ sung và chuẩn hóa exception

Đã giữ các exception đã có:

- [InvalidBidException.java](/D:/BaitaplonTest/src/main/java/com/auction/exception/InvalidBidException.java)
- [AuctionClosedException.java](/D:/BaitaplonTest/src/main/java/com/auction/exception/AuctionClosedException.java)
- [AuthenticationException.java](/D:/BaitaplonTest/src/main/java/com/auction/exception/AuthenticationException.java)

Đã bổ sung:

- [ValidationException.java](/D:/BaitaplonTest/src/main/java/com/auction/exception/ValidationException.java)

`ValidationException` được dùng cho các lỗi dữ liệu đầu vào:

- email rỗng
- username rỗng
- bidder `null`
- auction id rỗng
- giá bid `<= 0`
- item name/description rỗng
- starting price `<= 0`

### 2. Tăng cường xử lý ngoại lệ ở service layer

Đã cập nhật:

- [AuthService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/AuthService.java)
- [BidService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/BidService.java)
- [SellerService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/SellerService.java)

Nội dung chính:


- `AuthService` kiểm tra username/email rỗng trước khi register và login
- `BidService` kiểm tra:
  - `auctionId` không rỗng
  - `bidder` không `null`
  - `amount` hợp lệ và `> 0`
- `SellerService` kiểm tra:
  - `ItemType` không `null`
  - `name` không rỗng
  - `description` không rỗng
  - `startingPrice` `> 0`

### 3. Refactor GUI theo hướng MVC rõ hơn

Tuần 7 đã có FXML và controller cơ bản, nhưng controller vẫn chưa tách logic rõ ràng. Tuần 8 đã refactor thêm một tầng `presentation`.

Đã bổ sung:

- [LoginViewModel.java](/D:/BaitaplonTest/src/main/java/com/auction/presentation/LoginViewModel.java)
- [AuctionListViewModel.java](/D:/BaitaplonTest/src/main/java/com/auction/presentation/AuctionListViewModel.java)

Controller được làm mỏng hơn:

- [AuthController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AuthController.java)
- [AuctionController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AuctionController.java)

Sau refactor:

- controller chủ yếu đọc input từ view và cập nhật message lên UI
- `viewmodel` xử lý login, tải danh sách, đặt giá và finish auction
- các message lỗi được trả về có cấu trúc qua `result object`

### 4. Nâng cấp GUI JavaFX

Đã cập nhật:

- [login-view.fxml](/D:/BaitaplonTest/src/main/resources/fxml/login-view.fxml)
- [auction-list-view.fxml](/D:/BaitaplonTest/src/main/resources/fxml/auction-list-view.fxml)

GUI hiện tại có:

- login bằng email
- hiển thị message lỗi/kết quả trên UI
- danh sách auction
- nhập `bid amount`
- nút `Place Bid`
- nút `Finish Auction`
- `Refresh`
- `Back to Login`

Hành vi UI:

- account `Bidder` được đặt giá
- account khác sẽ bị khóa nút bid
- nút `Finish Auction` được dùng cho account không phải bidder
- ngoại lệ từ service được hiển thị thành message trên màn hình

### 5. Mở rộng unit test

Đã bổ sung và hoàn thiện các file test:

- [AuthServiceTest.java](/D:/BaitaplonTest/src/test/java/com/auction/service/AuthServiceTest.java)
- [SellerServiceTest.java](/D:/BaitaplonTest/src/test/java/com/auction/service/SellerServiceTest.java)
- [BidServiceTest.java](/D:/BaitaplonTest/src/test/java/com/auction/service/BidServiceTest.java)
- [InMemoryUserDaoTest.java](/D:/BaitaplonTest/src/test/java/com/auction/dao/InMemoryUserDaoTest.java)
- [InMemoryItemDaoTest.java](/D:/BaitaplonTest/src/test/java/com/auction/dao/InMemoryItemDaoTest.java)
- [InMemoryAuctionDaoTest.java](/D:/BaitaplonTest/src/test/java/com/auction/dao/InMemoryAuctionDaoTest.java)

Ngoài ra, các test từ tuần 7 vẫn được giữ và tiếp tục pass:

- [AuctionTest.java](/D:/BaitaplonTest/src/test/java/com/auction/model/auction/AuctionTest.java)
- [AuctionServiceTest.java](/D:/BaitaplonTest/src/test/java/com/auction/service/AuctionServiceTest.java)
- [ConcurrentBidTest.java](/D:/BaitaplonTest/src/test/java/com/auction/concurrency/ConcurrentBidTest.java)

### 6. Kết quả kiểm thử

Đã xác minh bằng Maven:

```powershell
mvn test
```

Kết quả:

- `Tests run: 59`
- `Failures: 0`
- `Errors: 0`
- `BUILD SUCCESS`

## Đánh giá theo tiêu chí tuần 8

1. `Tạo custom exceptions`
- Đạt

2. `Xử lý ngoại lệ cho đặt giá thấp hơn hiện tại, đấu giá khi phiên đóng, lỗi dữ liệu`
- Đạt

3. `Viết unit test JUnit cho logic đấu giá`
- Đạt

4. `Refactor code, loại bỏ code smell, áp dụng SOLID`
- Đạt ở mức phù hợp với cấu trúc hiện tại

5. `[Tự học] Hoàn thiện GUI JavaFX, áp dụng MVC, tách logic khỏi Controller, dùng FXML`
- Đạt ở mức tốt hơn tuần 7

## Giới hạn hiện tại

- chưa có password thật, login vẫn theo email
- chưa có persistence database thật
- `BidController` và `SellerController` vẫn chưa được mở rộng
- `auction detail` và `seller view` vẫn chưa phát triển đầy đủ

## Tổng kết

Sau tuần 8, project đã tiến một bước rõ ràng về chất lượng code:

- validation và exception rõ hơn
- test suite dày hơn
- GUI đã có xử lý hành động và lỗi thật
- controller đã mỏng hơn nhờ tách logic sang `presentation/viewmodel`

Trạng thái hiện tại phù hợp để demo cho nội dung:

- testing
- exception handling
- refactor
- JavaFX MVC cơ bản
