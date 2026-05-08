# Cập Nhật Seller Và Admin

## Mục tiêu

Bổ sung chức năng thực tế cho `Seller` và `Admin` thay cho các màn placeholder trước đó, đồng thời điều hướng người dùng theo đúng `role` sau khi đăng nhập.

## Những gì đã thêm

### 1. Điều hướng theo role sau khi login

- `Bidder` vào màn danh sách auction.
- `Seller` vào `Seller Dashboard`.
- `Admin` vào `Admin Dashboard`.

File liên quan:
- [SceneNavigator.java](/D:/BaitaplonTest/src/main/java/com/auction/app/SceneNavigator.java)
- [AuthController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AuthController.java)

### 2. Chức năng cho Seller

Đã triển khai màn hình seller thật thay cho placeholder.

Seller hiện có thể:
- tạo `Item`
- tạo `Auction` từ item vừa tạo
- xem danh sách auction của chính mình
- `Start Auction`
- `Finish Auction`

File liên quan:
- [SellerController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/SellerController.java)
- [seller-view.fxml](/D:/BaitaplonTest/src/main/resources/fxml/seller-view.fxml)

### 3. Chức năng cho Admin

Đã triển khai màn hình admin mới.

Admin hiện có thể:
- xem toàn bộ `users`
- xem toàn bộ `auctions`
- `Cancel Auction`
- `Mark Paid`
- refresh dữ liệu dashboard

File liên quan:
- [AdminController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AdminController.java)
- [admin-view.fxml](/D:/BaitaplonTest/src/main/resources/fxml/admin-view.fxml)

### 4. Bổ sung service dùng cho dashboard

`AppContext` đã được mở rộng để expose thêm:
- `SellerService`
- `UserService`

File liên quan:
- [AppContext.java](/D:/BaitaplonTest/src/main/java/com/auction/app/AppContext.java)

### 5. Cải thiện thông báo khi đặt bid lỗi

Khi bidder đặt giá thấp hơn hoặc bằng giá hiện tại, UI sẽ hiển thị thông báo rõ ràng hơn và có màu lỗi.

Ví dụ:
- `Bid must be higher than the current price (current: 1000.00).`

File liên quan:
- [AuctionListViewModel.java](/D:/BaitaplonTest/src/main/java/com/auction/presentation/AuctionListViewModel.java)
- [AuctionController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AuctionController.java)
- [app.css](/D:/BaitaplonTest/src/main/resources/css/app.css)

## Kết quả kiểm tra

Đã chạy lại:

```bash
mvn test
mvn verify
```

Kết quả:
- `61 tests`
- `0 failures`
- `0 Checkstyle violations`
- `BUILD SUCCESS`

## Kết luận ngắn

Sau cập nhật này:
- `Seller` không còn là role chỉ tồn tại ở backend mà đã có dashboard thao tác được
- `Admin` đã có dashboard quản lý cơ bản
- login đã điều hướng theo đúng vai trò người dùng
- phần demo của dự án hoàn chỉnh hơn và bám sát yêu cầu vấn đáp/báo cáo hơn
