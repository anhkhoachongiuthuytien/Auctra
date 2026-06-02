# 01 - auction-common: giải thích từng file

`auction-common` là module dùng chung giữa client và server. Nó không nên chứa logic UI hoặc logic database cụ thể. Nó định nghĩa "ngôn ngữ chung" của hệ thống: model, enum, exception, DAO interface, protocol, DTO và utility.

## Vai trò tổng thể

```text
Client cần hiểu User, Auction, Item, DTO, RequestType.
Server cũng cần hiểu các class đó.
Vì vậy các class dùng chung được đưa vào auction-common để tránh duplicate code.
```

---

## model/base

### Entity.java

```text
Tầng: Domain model nền tảng.
Nhiệm vụ: Lớp cha abstract cho các entity có id.
Ai dùng: User, Item, Auction.
Điểm cần hiểu: Implements Serializable để object có thể truyền qua socket.
```

---

## model/user

### User.java

```text
Tầng: Domain model.
Nhiệm vụ: Lớp cha cho mọi loại người dùng.
Thuộc tính chính: id, username, email, thông tin profile.
Ai dùng: AuthService, UserDao, SceneNavigator, các controller.
Điểm cần hiểu: User là abstract class, role cụ thể nằm ở subclass.
```

### Admin.java

```text
Tầng: Domain model.
Nhiệm vụ: Đại diện quản trị viên.
Ai dùng: SceneNavigator để mở admin-view; AdminController để quản lý hệ thống.
```

### Seller.java

```text
Tầng: Domain model.
Nhiệm vụ: Đại diện người bán.
Ai dùng: SellerService, SellerController, Auction.
Điểm cần hiểu: Seller là người tạo item và phiên đấu giá.
```

### Bidder.java

```text
Tầng: Domain model.
Nhiệm vụ: Đại diện người đặt giá.
Ai dùng: BidService, Auction.addBid(), AuctionDetailController.
Điểm cần hiểu: Chỉ Bidder mới được đặt giá.
```

---

## model/item

### Item.java

```text
Tầng: Domain model.
Nhiệm vụ: Lớp cha cho vật phẩm đấu giá.
Thuộc tính chính: name, description, startingPrice, imagePath/imagePaths.
Ai dùng: Auction, SellerService, ItemDao, FXML/UI.
```

### Art.java

```text
Tầng: Domain model.
Nhiệm vụ: Loại vật phẩm nghệ thuật.
Ai tạo: ItemFactory.
```

### Electronics.java

```text
Tầng: Domain model.
Nhiệm vụ: Loại vật phẩm điện tử.
Ai tạo: ItemFactory.
```

### Vehicle.java

```text
Tầng: Domain model.
Nhiệm vụ: Loại vật phẩm phương tiện.
Ai tạo: ItemFactory.
```

### Other.java

```text
Tầng: Domain model.
Nhiệm vụ: Loại vật phẩm khác.
Ai tạo: ItemFactory.
```

---

## factory

### ItemFactory.java

```text
Tầng: Domain helper / Factory Pattern.
Nhiệm vụ: Nhận ItemType và tạo đúng subclass Item.
Ai gọi: SellerService khi seller tạo vật phẩm.
Gọi ai: Art, Electronics, Vehicle, Other.
Điểm cần hiểu: Nếu thêm loại vật phẩm mới, sửa ItemType và ItemFactory.
```

---

## model/auction

### Auction.java

```text
Tầng: Domain model cốt lõi.
Nhiệm vụ: Quản lý một phiên đấu giá.
Thuộc tính chính: item, seller, currentPrice, status, bids, winner, endTime.
Ai gọi: AuctionService, BidService, DAO khi restore dữ liệu.
Gọi ai: BidEvent/BidObserver khi có bid mới.
Tính năng nâng cao: ReentrantLock, observer, anti-sniping.
```

Các method phải hiểu:

```text
start()       -> chỉ cho OPEN chuyển sang RUNNING.
finish()      -> chỉ cho RUNNING chuyển sang FINISHED.
cancel()      -> hủy phiên nếu còn cho phép.
markPaid()    -> FINISHED chuyển sang PAID.
addBid()      -> kiểm tra phiên RUNNING, bid hợp lệ, amount > currentPrice.
restoreState()-> dựng lại Auction từ database.
```

Câu giải thích:

```text
Auction chứa rule trạng thái và rule cập nhật giá. BidService xử lý nghiệp vụ bên ngoài,
còn Auction.addBid() bảo vệ tính đúng đắn bên trong từng phiên.
```

### BidTransaction.java

```text
Tầng: Domain model.
Nhiệm vụ: Lưu một lượt đặt giá.
Thuộc tính chính: bidder, amount, bidTime.
Ai dùng: Auction.bids, SqliteAuctionDao, AuctionDetailController.
```

### AutoBidConfig.java

```text
Tầng: Domain model.
Nhiệm vụ: Lưu cấu hình đấu giá tự động của bidder.
Thuộc tính chính: auctionId, bidderId, maxPrice, increment.
Ai dùng: BidService, AutoBidDao, AuctionDetailController.
```

---

## enums

### AuctionStatus.java

```text
Tầng: Enum domain.
Nhiệm vụ: Trạng thái phiên đấu giá.
Giá trị chính: OPEN, RUNNING, FINISHED, PAID, CANCELED.
Ai dùng: Auction, AuctionService, UI badge/filter.
```

Luồng trạng thái:

```text
OPEN -> RUNNING -> FINISHED -> PAID
OPEN/RUNNING/FINISHED -> CANCELED
```

### ItemType.java

```text
Tầng: Enum domain.
Nhiệm vụ: Loại vật phẩm.
Ai dùng: ItemFactory, SellerService, SellerController.
Điểm cần hiểu: Có hàm parse từ string để nhận dữ liệu từ UI/socket.
```

### UserRole.java

```text
Tầng: Enum domain.
Nhiệm vụ: Biểu diễn role người dùng.
Ai dùng: Có thể dùng cho phân quyền/hiển thị role.
```

---

## exception

### AuctionException.java

```text
Tầng: Exception gốc.
Nhiệm vụ: Lớp cha cho lỗi nghiệp vụ đấu giá.
Ai dùng: Service và model ném lỗi.
```

### AuthenticationException.java

```text
Tầng: Exception nghiệp vụ.
Nhiệm vụ: Lỗi xác thực như sai email/password.
Ai dùng: AuthService.
```

### AuthorizationException.java

```text
Tầng: Exception nghiệp vụ.
Nhiệm vụ: Lỗi không đủ quyền.
Ai dùng: Các service/controller nếu cần kiểm tra quyền.
```

### ValidationException.java

```text
Tầng: Exception nghiệp vụ.
Nhiệm vụ: Dữ liệu đầu vào không hợp lệ.
Ai dùng: AuthService, BidService, SellerService.
```

### InvalidBidException.java

```text
Tầng: Exception nghiệp vụ.
Nhiệm vụ: Giá đặt không hợp lệ.
Ai dùng: Auction.addBid(), BidService.
```

### AuctionClosedException.java

```text
Tầng: Exception nghiệp vụ.
Nhiệm vụ: Không thể đặt giá vì phiên chưa mở/đã đóng.
Ai dùng: Auction.addBid(), BidService.
```

---

## dao interface

### UserDao.java

```text
Tầng: DAO abstraction.
Nhiệm vụ: Định nghĩa thao tác lưu/đọc user.
Implementation: SqliteUserDao, InMemoryUserDao.
Ai dùng: AuthService, UserService, BidService auto-bid.
```

### ItemDao.java

```text
Tầng: DAO abstraction.
Nhiệm vụ: Định nghĩa thao tác lưu/đọc item.
Implementation: SqliteItemDao, InMemoryItemDao.
Ai dùng: SellerService, SqliteAuctionDao.
```

### AuctionDao.java

```text
Tầng: DAO abstraction.
Nhiệm vụ: Định nghĩa thao tác lưu/đọc auction.
Implementation: SqliteAuctionDao, InMemoryAuctionDao.
Ai dùng: AuctionService, BidService.
```

### AutoBidDao.java

```text
Tầng: DAO abstraction.
Nhiệm vụ: Định nghĩa thao tác lưu/đọc cấu hình auto-bid.
Implementation: SqliteAutoBidDao, InMemoryAutoBidDao.
Ai dùng: BidService, AuctionServerFacade.
```

---

## observer

### BidObserver.java

```text
Tầng: Observer Pattern.
Nhiệm vụ: Interface cho object muốn nghe sự kiện bid mới.
Ai gọi: Auction.notifyBidPlaced().
```

### BidEvent.java

```text
Tầng: Event domain.
Nhiệm vụ: Gói thông tin khi có bid mới.
Chứa: auction, bid, currentPrice, winner.
Ai dùng: BidObserver, ConsoleBidObserver.
```

---

## protocol

### RequestType.java

```text
Tầng: Protocol socket.
Nhiệm vụ: Enum các loại request client có thể gửi.
Ví dụ: LOGIN, REGISTER, LIST_AUCTIONS, PLACE_BID, SUBSCRIBE_UPDATES.
Ai dùng: SocketAuctionClientGateway, AuctionSocketServer.
```

### AuctionRequest.java

```text
Tầng: Protocol socket.
Nhiệm vụ: Gói request từ client sang server.
Chứa: RequestType và map dữ liệu.
Ai tạo: SocketAuctionClientGateway.
Ai đọc: AuctionSocketServer.processRequest().
```

### AuctionResponse.java

```text
Tầng: Protocol socket.
Nhiệm vụ: Gói response từ server về client.
Chứa: success, message, data.
Ai tạo: AuctionSocketServer.
Ai đọc: SocketAuctionClientGateway.
```

### AuctionEvent.java

```text
Tầng: Protocol realtime.
Nhiệm vụ: Sự kiện server broadcast về client.
Ví dụ: NEW_BID, AUCTION_CREATED, AUCTION_FINISHED, USER_UPDATED.
Ai tạo: AuctionSocketServer, AuctionExpiryScheduler.
Ai nhận: SocketAuctionClientGateway listener socket.
```

### AuctionDto.java

```text
Tầng: DTO.
Nhiệm vụ: Bản dữ liệu gọn của Auction để truyền qua socket.
Ai tạo: DtoMapper.toDto(Auction).
Ai đọc: DtoMapper.toAuction(AuctionDto).
```

### UserDto.java

```text
Tầng: DTO.
Nhiệm vụ: Bản dữ liệu gọn của User.
Điểm cần hiểu: Không truyền password hash cho client.
```

### BidDto.java

```text
Tầng: DTO.
Nhiệm vụ: Bản dữ liệu gọn của BidTransaction.
Ai dùng: AuctionDto, DtoMapper.
```

### DtoMapper.java

```text
Tầng: Mapping protocol.
Nhiệm vụ: Chuyển Model <-> DTO.
Ai dùng: Socket client và socket server.
Điểm cần hiểu: Giúp protocol ổn định, tránh truyền object nặng/khó kiểm soát.
```

---

## util

### IdGenerator.java

```text
Tầng: Utility.
Nhiệm vụ: Tạo id cho entity.
Ai dùng: Model/service khi tạo object mới.
```

### PasswordHasher.java

```text
Tầng: Security utility.
Nhiệm vụ: Hash và kiểm tra mật khẩu.
Thuật toán: PBKDF2WithHmacSHA256, salt, iterations, constant-time compare.
Ai dùng: AuthService.
```

### ImageStorage.java

```text
Tầng: Utility dùng chung.
Nhiệm vụ: Lưu/kiểm tra đường dẫn ảnh vật phẩm/avatar.
Ai dùng: SellerController, ProfileController, AuctionDetailController.
```

---

## manager

### AuctionManager.java

```text
Tầng: Manager/domain helper.
Nhiệm vụ: Quản lý danh sách auction ở mức in-memory.
Ai dùng: Có thể dùng trong test hoặc phiên bản local đơn giản.
Điểm cần hiểu: Không phải tầng chính khi chạy SQLite/socket.
```

---

## Tóm tắt cần nhớ

```text
File quan trọng nhất của common là Auction.java.
File protocol quan trọng nhất là RequestType, AuctionRequest, AuctionResponse, AuctionEvent.
File security quan trọng nhất là PasswordHasher.java.
DAO trong common chỉ là interface, implementation thật nằm ở auction-server.
```
