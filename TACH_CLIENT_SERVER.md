# Tách Client Và Server Chuẩn Bị Cho Lập Trình Mạng

## Mục tiêu

Tách dự án khỏi kiểu `JavaFX gọi thẳng Service/DAO` để chuẩn bị cho bước tiếp theo là tích hợp giao tiếp mạng.

Sau thay đổi này, kiến trúc mới là:

```text
JavaFX Client -> Client Gateway -> Server Facade -> Service -> DAO -> SQLite
```

## Vấn đề của kiến trúc cũ

Trước đây:
- `Controller` và `ViewModel` lấy trực tiếp `AuthService`, `AuctionService`, `BidService`, `SellerService`, `UserService`
- `AppContext` vừa là chỗ dựng UI, vừa là chỗ dựng backend
- nếu sau này muốn thay bằng `Socket client` hoặc `REST client`, sẽ phải sửa rất nhiều `Controller/ViewModel`

Tức là:
- phần client và backend chưa có ranh giới rõ
- UI phụ thuộc quá chặt vào implementation backend nội bộ

## Kiến trúc mới

### 1. Server side

Đã thêm:
- [ServerContext.java](/D:/BaitaplonTest/src/main/java/com/auction/server/ServerContext.java)
- [AuctionServerFacade.java](/D:/BaitaplonTest/src/main/java/com/auction/server/AuctionServerFacade.java)

`ServerContext` chịu trách nhiệm:
- khởi tạo `DatabaseManager`
- khởi tạo `DAO`
- khởi tạo `Service`
- seed dữ liệu demo

`AuctionServerFacade` là mặt ngoài của server:
- gom các nghiệp vụ mà client cần gọi
- che đi chi tiết `Service/DAO`
- đóng vai trò giống một API nội bộ

Các hàm server facade hiện có:
- `login`
- `register`
- `listAuctions`
- `listAuctionsForSeller`
- `createAuctionForSeller`
- `startAuction`
- `finishAuction`
- `cancelAuction`
- `markAuctionPaid`
- `placeBid`
- `listUsers`

### 2. Client side

Đã thêm:
- [AuctionClientGateway.java](/D:/BaitaplonTest/src/main/java/com/auction/client/AuctionClientGateway.java)
- [LocalAuctionClientGateway.java](/D:/BaitaplonTest/src/main/java/com/auction/client/LocalAuctionClientGateway.java)

`AuctionClientGateway` là contract phía client:
- UI chỉ biết gọi gateway
- không biết backend thật đang là local call hay network call

`LocalAuctionClientGateway` là bản triển khai hiện tại:
- gọi trực tiếp vào `AuctionServerFacade`
- dùng để giữ app chạy ngay bây giờ
- sau này có thể thay bằng:
  - `SocketAuctionClientGateway`
  - `RestAuctionClientGateway`

### 3. AppContext mới

Đã sửa:
- [AppContext.java](/D:/BaitaplonTest/src/main/java/com/auction/app/AppContext.java)

Vai trò mới:
- chỉ làm `composition root`
- dựng `ServerContext`
- dựng `AuctionServerFacade`
- dựng `LocalAuctionClientGateway`
- expose duy nhất:

```java
getGateway()
```

Tức là client không còn lấy thẳng service từ `AppContext` nữa.

## Những file client đã được refactor

### ViewModel

- [LoginViewModel.java](/D:/BaitaplonTest/src/main/java/com/auction/presentation/LoginViewModel.java)
- [AuctionListViewModel.java](/D:/BaitaplonTest/src/main/java/com/auction/presentation/AuctionListViewModel.java)

Trước đây:
- gọi trực tiếp `AuthService`, `AuctionService`, `BidService`

Hiện tại:
- chỉ gọi `AuctionClientGateway`

### Controller

- [AuthController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AuthController.java)
- [AuctionController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AuctionController.java)
- [SellerController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/SellerController.java)
- [AdminController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AdminController.java)

Các controller này không còn gọi service trực tiếp qua `AppContext` nữa.

Ví dụ:
- `SellerController` không còn gọi `getSellerService()` / `getAuctionService()`
- `AdminController` không còn gọi `getUserService()` / `getAuctionService()`
- tất cả đi qua `appContext.getGateway()`

## Lợi ích của cách tách này

### 1. Chuẩn bị tốt cho lập trình mạng

Sau này nếu muốn dùng socket:
- giữ nguyên `Controller`
- giữ nguyên `ViewModel`
- giữ nguyên `FXML`
- chỉ cần thay `LocalAuctionClientGateway` bằng một gateway mới biết gửi request qua mạng

### 2. Giảm coupling

Client:
- không biết backend dùng SQLite, DAO hay service nào bên dưới

Server:
- có thể thay đổi cách xử lý bên trong mà không phải sửa UI

### 3. Dễ mở rộng thành multi-process

Hiện tại vẫn là:
- một process JavaFX
- nhưng có ranh giới client/server rõ hơn

Bước tiếp theo có thể là:
- tách `ServerContext + AuctionServerFacade` sang app server riêng
- client giữ `AuctionClientGateway`

## Những gì vẫn chưa làm

Hiện tại đây là bước tách kiến trúc, **chưa phải network programming thật**.

Chưa có:
- `Socket`
- `ServerSocket`
- `REST API`
- `JSON request/response`
- `DTO` tách riêng giữa client và server
- `realtime push`

Tức là:
- **đã chuẩn bị ranh giới**
- **chưa truyền dữ liệu qua mạng**

## Gợi ý bước tiếp theo

Nếu tiếp tục tích hợp lập trình mạng, nên làm theo thứ tự:

1. tạo `SocketAuctionClientGateway`
2. tạo `SocketAuctionServer`
3. định nghĩa request/response object
4. serialize dữ liệu bằng JSON
5. để `AuctionClientGateway` gọi server qua socket thay vì local call

Khi đó:
- `LocalAuctionClientGateway` chỉ là bản mock/local
- `SocketAuctionClientGateway` sẽ là bản production cho bài lập trình mạng

## Kết quả kiểm tra

Đã chạy lại:

```bash
mvn test
mvn verify
```

Kết quả:
- `65 tests`
- `0 failures`
- `0 Checkstyle violations`
- `BUILD SUCCESS`

## Kết luận ngắn

Sau thay đổi này, dự án đã được tách thành hai phía rõ hơn:

- `client`: JavaFX UI + controller + viewmodel + client gateway
- `server`: facade + service + dao + sqlite

Hiện tại client và server vẫn chạy cùng tiến trình, nhưng đã có ranh giới kỹ thuật đủ tốt để chuẩn bị thay thế bằng giao tiếp mạng ở bước tiếp theo.
