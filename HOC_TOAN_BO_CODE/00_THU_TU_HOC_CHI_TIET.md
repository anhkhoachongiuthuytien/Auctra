# 00 - Thứ tự học chi tiết: từ file nào đến file nào

File này trả lời đúng câu hỏi: **học theo thứ tự nào, từ file nào đến file nào, đối với mỗi phần của dự án**.

Nguyên tắc:

```text
Không học theo thứ tự thư mục.
Học theo thứ tự phụ thuộc:
Model -> Service -> DAO -> Socket/Gateway -> Controller -> Tính năng nâng cao.
```

---

## 0. Đọc tài liệu trong thư mục này theo thứ tự nào?

Trước khi mở code, đọc tài liệu trong `HOC_TOAN_BO_CODE` theo thứ tự:

```text
0. 00_THU_TU_HOC_CHI_TIET.md
1. README.md
2. 01_COMMON_TUNG_FILE.md
3. 08_COMMON_DEEP_DIVE.md
4. 02_SERVER_TUNG_FILE.md
5. 09_SERVER_DEEP_DIVE.md
6. 04_DATABASE_DAO.md
7. 05_SOCKET_REALTIME_PROTOCOL.md
8. 03_CLIENT_TUNG_FILE.md
9. 10_CLIENT_DEEP_DIVE.md
10. 06_TINH_NANG_NANG_CAO.md
11. 11_TINH_NANG_NANG_CAO_DEEP_DIVE.md
12. 07_LUONG_NGHIEP_VU_DE_DOC_CODE.md
```

Lý do:

```text
Đọc common trước để hiểu object.
Đọc server sau để hiểu nghiệp vụ.
Đọc database để biết dữ liệu lưu thế nào.
Đọc socket để biết client-server nói chuyện thế nào.
Đọc client cuối vì UI gọi xuống các tầng dưới.
Đọc nâng cao sau khi đã hiểu lõi.
Cuối cùng đọc luồng nghiệp vụ để ráp toàn bộ.
```

---

## 1. Phần common: học từ file nào đến file nào?

Thư mục:

```text
auction-common/src/main/java/com/auction
```

Học theo thứ tự này:

### Bước 1: Nền tảng entity

```text
1. model/base/Entity.java
```

Mục tiêu:

```text
Hiểu mọi model chính đều có id và Serializable.
```

### Bước 2: Enum trước để hiểu trạng thái/loại

```text
2. enums/AuctionStatus.java
3. enums/ItemType.java
4. enums/UserRole.java
```

Mục tiêu:

```text
Hiểu trạng thái auction, loại item, role user.
```

### Bước 3: User model

```text
5. model/user/User.java
6. model/user/Admin.java
7. model/user/Seller.java
8. model/user/Bidder.java
```

Mục tiêu:

```text
Hiểu phân quyền bằng kế thừa: Admin, Seller, Bidder.
```

### Bước 4: Item model

```text
9. model/item/Item.java
10. model/item/Art.java
11. model/item/Electronics.java
12. model/item/Vehicle.java
13. model/item/Other.java
14. factory/ItemFactory.java
```

Mục tiêu:

```text
Hiểu vật phẩm đấu giá và Factory Pattern.
```

### Bước 5: Auction model

```text
15. model/auction/BidTransaction.java
16. model/auction/AutoBidConfig.java
17. model/auction/Auction.java
```

Mục tiêu:

```text
Đây là lõi. Phải hiểu addBid(), start(), finish(), cancel(), markPaid(), anti-sniping.
```

### Bước 6: Exception

```text
18. exception/AuctionException.java
19. exception/AuthenticationException.java
20. exception/AuthorizationException.java
21. exception/ValidationException.java
22. exception/InvalidBidException.java
23. exception/AuctionClosedException.java
```

Mục tiêu:

```text
Biết service/model ném lỗi gì khi dữ liệu sai hoặc thao tác sai trạng thái.
```

### Bước 7: DAO interface

```text
24. dao/UserDao.java
25. dao/ItemDao.java
26. dao/AuctionDao.java
27. dao/AutoBidDao.java
```

Mục tiêu:

```text
Hiểu common chỉ định nghĩa interface, còn implementation nằm ở server.
```

### Bước 8: Observer

```text
28. observer/BidEvent.java
29. observer/BidObserver.java
```

Mục tiêu:

```text
Hiểu Auction phát event khi có bid mới.
```

### Bước 9: Protocol socket

```text
30. protocol/RequestType.java
31. protocol/AuctionRequest.java
32. protocol/AuctionResponse.java
33. protocol/AuctionEvent.java
34. protocol/UserDto.java
35. protocol/BidDto.java
36. protocol/AuctionDto.java
37. protocol/DtoMapper.java
```

Mục tiêu:

```text
Hiểu client-server truyền dữ liệu qua Request/Response/Event và DTO.
```

### Bước 10: Utility

```text
38. util/IdGenerator.java
39. util/PasswordHasher.java
40. util/ImageStorage.java
41. manager/AuctionManager.java
```

Mục tiêu:

```text
Hiểu tạo id, bảo mật mật khẩu, lưu ảnh, manager phụ.
```

### File common cần học kỹ nhất

```text
1. Auction.java
2. BidTransaction.java
3. AutoBidConfig.java
4. RequestType.java
5. AuctionRequest.java
6. AuctionResponse.java
7. AuctionEvent.java
8. DtoMapper.java
9. PasswordHasher.java
```

---

## 2. Phần server: học từ file nào đến file nào?

Thư mục:

```text
auction-server/src/main/java/com/auction
```

Học theo thứ tự này:

### Bước 1: Nhìn bản đồ server trước

```text
1. server/ServerContext.java
```

Mục tiêu:

```text
Biết server tạo DatabaseManager, DAO và Service nào.
Chỉ đọc lướt lần đầu, chưa cần hiểu sâu.
```

### Bước 2: Database nền

```text
2. ../resources/db/schema.sql
3. db/DatabaseManager.java
4. db/DbMappers.java
```

Mục tiêu:

```text
Biết bảng nào lưu gì và Java map dữ liệu DB thành object thế nào.
```

### Bước 3: DAO SQLite

```text
5. dao/sqlite/SqliteUserDao.java
6. dao/sqlite/SqliteItemDao.java
7. dao/sqlite/SqliteAuctionDao.java
8. dao/sqlite/SqliteAutoBidDao.java
```

Mục tiêu:

```text
Hiểu cách lưu/đọc user, item, auction, bids, auto-bid.
SqliteAuctionDao là file khó nhất trong nhóm DAO.
```

### Bước 4: DAO in-memory để hiểu test

```text
9. dao/memory/InMemoryUserDao.java
10. dao/memory/InMemoryItemDao.java
11. dao/memory/InMemoryAuctionDao.java
12. dao/memory/InMemoryAutoBidDao.java
```

Mục tiêu:

```text
Hiểu test service không cần SQLite thật.
```

### Bước 5: Service nghiệp vụ

```text
13. service/AuthService.java
14. service/UserService.java
15. service/SellerService.java
16. service/AuctionService.java
17. service/BidService.java
```

Mục tiêu:

```text
Hiểu đăng nhập, profile, tạo item/auction, đổi trạng thái, đặt giá, auto-bid.
BidService là file quan trọng nhất server.
```

### Bước 6: Quay lại ServerContext đọc kỹ

```text
18. server/ServerContext.java
```

Mục tiêu:

```text
Lúc này đọc lại sẽ hiểu dependency thật: service nào dùng DAO nào.
```

### Bước 7: Facade

```text
19. server/AuctionServerFacade.java
```

Mục tiêu:

```text
Hiểu gateway/socket không gọi service trực tiếp mà gọi qua facade.
```

### Bước 8: Socket server và realtime

```text
20. server/BroadcastManager.java
21. server/AuctionSocketServer.java
22. server/AuctionExpiryScheduler.java
23. server/ServerMain.java
```

Mục tiêu:

```text
Hiểu server socket, thread pool, request/response, broadcast realtime, scheduler hết hạn.
```

### Bước 9: Observer phụ

```text
24. observer/ConsoleBidObserver.java
```

Mục tiêu:

```text
Hiểu observer in log khi có bid mới, không phải luồng chính.
```

### File server cần học kỹ nhất

```text
1. ServerContext.java
2. AuctionServerFacade.java
3. AuthService.java
4. SellerService.java
5. AuctionService.java
6. BidService.java
7. DatabaseManager.java
8. SqliteAuctionDao.java
9. AuctionSocketServer.java
10. BroadcastManager.java
11. AuctionExpiryScheduler.java
```

---

## 3. Phần database/DAO: học từ file nào đến file nào?

Học riêng phần database theo thứ tự:

```text
1. auction-server/src/main/resources/db/schema.sql
2. auction-server/src/main/java/com/auction/db/DatabaseManager.java
3. auction-common/src/main/java/com/auction/dao/UserDao.java
4. auction-common/src/main/java/com/auction/dao/ItemDao.java
5. auction-common/src/main/java/com/auction/dao/AuctionDao.java
6. auction-common/src/main/java/com/auction/dao/AutoBidDao.java
7. auction-server/src/main/java/com/auction/db/DbMappers.java
8. auction-server/src/main/java/com/auction/dao/sqlite/SqliteUserDao.java
9. auction-server/src/main/java/com/auction/dao/sqlite/SqliteItemDao.java
10. auction-server/src/main/java/com/auction/dao/sqlite/SqliteAuctionDao.java
11. auction-server/src/main/java/com/auction/dao/sqlite/SqliteAutoBidDao.java
```

Mục tiêu:

```text
Biết mỗi bảng lưu gì.
Biết DAO interface khác DAO implementation.
Biết service không viết SQL.
Biết SqliteAuctionDao phải dựng lại Auction phức tạp thế nào.
```

---

## 4. Phần socket/protocol/realtime: học từ file nào đến file nào?

Học theo thứ tự:

### Bước 1: Protocol ở common

```text
1. auction-common/src/main/java/com/auction/protocol/RequestType.java
2. auction-common/src/main/java/com/auction/protocol/AuctionRequest.java
3. auction-common/src/main/java/com/auction/protocol/AuctionResponse.java
4. auction-common/src/main/java/com/auction/protocol/AuctionEvent.java
5. auction-common/src/main/java/com/auction/protocol/UserDto.java
6. auction-common/src/main/java/com/auction/protocol/BidDto.java
7. auction-common/src/main/java/com/auction/protocol/AuctionDto.java
8. auction-common/src/main/java/com/auction/protocol/DtoMapper.java
```

### Bước 2: Gateway phía client

```text
9. auction-client/src/main/java/com/auction/client/AuctionClientGateway.java
10. auction-client/src/main/java/com/auction/client/SocketAuctionClientGateway.java
```

### Bước 3: Server nhận request

```text
11. auction-server/src/main/java/com/auction/server/AuctionSocketServer.java
12. auction-server/src/main/java/com/auction/server/AuctionServerFacade.java
```

### Bước 4: Realtime

```text
13. auction-server/src/main/java/com/auction/server/BroadcastManager.java
14. auction-client/src/main/java/com/auction/client/ClientEventManager.java
15. auction-client/src/main/java/com/auction/controller/AuctionDetailController.java
```

Mục tiêu:

```text
Hiểu request/response qua socket chính.
Hiểu event realtime qua socket phụ.
Hiểu Platform.runLater và reload UI.
```

---

## 5. Phần client/UI: học từ file nào đến file nào?

Thư mục:

```text
auction-client/src/main/java/com/auction
```

Học theo thứ tự này:

### Bước 1: App entry và navigation

```text
1. Main.java
2. app/AppContext.java
3. app/SceneNavigator.java
```

Mục tiêu:

```text
Hiểu app khởi động, chọn local/socket, chuyển màn hình theo role.
```

### Bước 2: Gateway

```text
4. client/AuctionClientGateway.java
5. client/LocalAuctionClientGateway.java
6. client/SocketAuctionClientGateway.java
7. client/ClientEventManager.java
```

Mục tiêu:

```text
Hiểu controller gọi server qua gateway.
```

### Bước 3: ViewModel

```text
8. presentation/LoginViewModel.java
9. presentation/AuctionListViewModel.java
```

Mục tiêu:

```text
Hiểu logic nhẹ giữa controller và gateway.
```

### Bước 4: Controller theo thứ tự nghiệp vụ

```text
10. controller/AuthController.java
11. controller/RegisterController.java
12. controller/ForgotPasswordController.java
13. controller/AuctionController.java
14. controller/AuctionDetailController.java
15. controller/SellerController.java
16. controller/AdminController.java
17. controller/ProfileController.java
18. controller/BidController.java
```

Mục tiêu:

```text
Hiểu login/register, xem phiên, đặt giá, seller tạo phiên, admin quản lý, profile.
AuctionDetailController là file khó nhất client.
```

### Bước 5: UI helper

```text
19. ui/ThemeManager.java
20. ui/UIAnimations.java
21. ui/ToastManager.java
22. ui/BadgeFactory.java
23. ui/IconFactory.java
24. ui/FloatingFieldHelper.java
25. ui/CountdownTimer.java
26. ui/ResponsiveManager.java
27. ui/SkeletonPane.java
```

### Bước 6: Client util

```text
28. util/UiEffects.java
29. util/UserImageHelper.java
30. util/ScreenshotGenerator.java
```

### Bước 7: FXML theo controller

```text
31. resources/fxml/login-view.fxml
32. resources/fxml/register-view.fxml
33. resources/fxml/forgot-password-view.fxml
34. resources/fxml/auction-list-view.fxml
35. resources/fxml/auction-detail-view.fxml
36. resources/fxml/seller-view.fxml
37. resources/fxml/admin-view.fxml
38. resources/fxml/profile-view.fxml
```

### Bước 8: CSS

```text
39. resources/css/tokens.css
40. resources/css/base.css
41. resources/css/app.css
42. resources/css/themes/light.css
43. resources/css/themes/dark.css
44. resources/css/components/*.css
45. resources/css/screens/*.css
```

Mục tiêu:

```text
Hiểu style được chia theo token, theme, component, screen.
```

---

## 6. Phần tính năng nâng cao: học từ file nào đến file nào?

### Tính năng 1: Realtime dual-socket

```text
1. auction-common/src/main/java/com/auction/protocol/AuctionEvent.java
2. auction-common/src/main/java/com/auction/protocol/RequestType.java
3. auction-client/src/main/java/com/auction/client/SocketAuctionClientGateway.java
4. auction-server/src/main/java/com/auction/server/AuctionSocketServer.java
5. auction-server/src/main/java/com/auction/server/BroadcastManager.java
6. auction-client/src/main/java/com/auction/client/ClientEventManager.java
7. auction-client/src/main/java/com/auction/controller/AuctionDetailController.java
```

### Tính năng 2: Đặt giá đồng thời / thread safety

```text
1. auction-server/src/main/java/com/auction/service/BidService.java
2. auction-common/src/main/java/com/auction/model/auction/Auction.java
3. auction-server/src/test/java/com/auction/concurrency/ConcurrentBidTest.java
4. auction-server/src/test/java/com/auction/service/BidServiceTest.java
```

### Tính năng 3: Auto-Bid

```text
1. auction-common/src/main/java/com/auction/model/auction/AutoBidConfig.java
2. auction-common/src/main/java/com/auction/dao/AutoBidDao.java
3. auction-server/src/main/java/com/auction/dao/sqlite/SqliteAutoBidDao.java
4. auction-server/src/main/java/com/auction/service/BidService.java
5. auction-client/src/main/java/com/auction/controller/AuctionDetailController.java
6. auction-server/src/test/java/com/auction/service/AutoBidPriorityQueueTest.java
```

### Tính năng 4: Anti-Sniping

```text
1. auction-common/src/main/java/com/auction/model/auction/Auction.java
2. auction-server/src/test/java/com/auction/service/AntiSnipingTest.java
```

### Tính năng 5: Scheduler tự kết thúc phiên

```text
1. auction-common/src/main/java/com/auction/model/auction/Auction.java
2. auction-server/src/main/java/com/auction/service/AuctionService.java
3. auction-server/src/main/java/com/auction/server/AuctionExpiryScheduler.java
4. auction-server/src/main/java/com/auction/server/BroadcastManager.java
5. auction-client/src/main/java/com/auction/client/ClientEventManager.java
```

### Tính năng 6: Bảo mật mật khẩu

```text
1. auction-common/src/main/java/com/auction/util/PasswordHasher.java
2. auction-server/src/main/java/com/auction/service/AuthService.java
3. auction-server/src/main/java/com/auction/dao/sqlite/SqliteUserDao.java
4. auction-server/src/test/java/com/auction/service/AuthServiceTest.java
```

### Tính năng 7: Ảnh vật phẩm/avatar

```text
1. auction-common/src/main/java/com/auction/util/ImageStorage.java
2. auction-common/src/main/java/com/auction/model/item/Item.java
3. auction-client/src/main/java/com/auction/controller/SellerController.java
4. auction-client/src/main/java/com/auction/controller/AuctionDetailController.java
5. auction-client/src/main/java/com/auction/controller/ProfileController.java
6. auction-client/src/main/java/com/auction/util/UserImageHelper.java
```

### Tính năng 8: UI nâng cao

```text
1. auction-client/src/main/java/com/auction/controller/AuctionDetailController.java
2. auction-client/src/main/java/com/auction/ui/ThemeManager.java
3. auction-client/src/main/java/com/auction/ui/UIAnimations.java
4. auction-client/src/main/java/com/auction/ui/ToastManager.java
5. auction-client/src/main/java/com/auction/util/UiEffects.java
6. auction-client/src/main/resources/css/tokens.css
7. auction-client/src/main/resources/css/themes/light.css
8. auction-client/src/main/resources/css/themes/dark.css
9. auction-client/src/main/resources/css/components/*.css
10. auction-client/src/main/resources/css/screens/*.css
```

---

## 7. Học theo luồng nghiệp vụ: thứ tự file cụ thể

### Luồng login

```text
1. auction-client/src/main/resources/fxml/login-view.fxml
2. auction-client/src/main/java/com/auction/controller/AuthController.java
3. auction-client/src/main/java/com/auction/presentation/LoginViewModel.java
4. auction-client/src/main/java/com/auction/client/AuctionClientGateway.java
5. auction-client/src/main/java/com/auction/client/LocalAuctionClientGateway.java
6. auction-client/src/main/java/com/auction/client/SocketAuctionClientGateway.java
7. auction-common/src/main/java/com/auction/protocol/RequestType.java
8. auction-server/src/main/java/com/auction/server/AuctionSocketServer.java
9. auction-server/src/main/java/com/auction/server/AuctionServerFacade.java
10. auction-server/src/main/java/com/auction/service/AuthService.java
11. auction-server/src/main/java/com/auction/dao/sqlite/SqliteUserDao.java
12. auction-common/src/main/java/com/auction/util/PasswordHasher.java
13. auction-client/src/main/java/com/auction/app/SceneNavigator.java
```

### Luồng seller tạo phiên

```text
1. auction-client/src/main/resources/fxml/seller-view.fxml
2. auction-client/src/main/java/com/auction/controller/SellerController.java
3. auction-common/src/main/java/com/auction/util/ImageStorage.java
4. auction-client/src/main/java/com/auction/client/AuctionClientGateway.java
5. auction-client/src/main/java/com/auction/client/SocketAuctionClientGateway.java
6. auction-server/src/main/java/com/auction/server/AuctionSocketServer.java
7. auction-server/src/main/java/com/auction/server/AuctionServerFacade.java
8. auction-server/src/main/java/com/auction/service/SellerService.java
9. auction-common/src/main/java/com/auction/factory/ItemFactory.java
10. auction-common/src/main/java/com/auction/model/item/Item.java
11. auction-common/src/main/java/com/auction/model/auction/Auction.java
12. auction-server/src/main/java/com/auction/dao/sqlite/SqliteItemDao.java
13. auction-server/src/main/java/com/auction/dao/sqlite/SqliteAuctionDao.java
14. auction-server/src/main/java/com/auction/server/BroadcastManager.java
```

### Luồng bidder đặt giá

```text
1. auction-client/src/main/resources/fxml/auction-detail-view.fxml
2. auction-client/src/main/java/com/auction/controller/AuctionDetailController.java
3. auction-client/src/main/java/com/auction/presentation/AuctionListViewModel.java
4. auction-client/src/main/java/com/auction/client/AuctionClientGateway.java
5. auction-client/src/main/java/com/auction/client/SocketAuctionClientGateway.java
6. auction-common/src/main/java/com/auction/protocol/AuctionRequest.java
7. auction-server/src/main/java/com/auction/server/AuctionSocketServer.java
8. auction-server/src/main/java/com/auction/server/AuctionServerFacade.java
9. auction-server/src/main/java/com/auction/service/BidService.java
10. auction-server/src/main/java/com/auction/dao/sqlite/SqliteAuctionDao.java
11. auction-common/src/main/java/com/auction/model/auction/Auction.java
12. auction-common/src/main/java/com/auction/model/auction/BidTransaction.java
13. auction-server/src/main/java/com/auction/server/BroadcastManager.java
14. auction-client/src/main/java/com/auction/client/ClientEventManager.java
```

### Luồng auto-bid

```text
1. auction-client/src/main/java/com/auction/controller/AuctionDetailController.java
2. auction-client/src/main/java/com/auction/client/AuctionClientGateway.java
3. auction-client/src/main/java/com/auction/client/SocketAuctionClientGateway.java
4. auction-server/src/main/java/com/auction/server/AuctionSocketServer.java
5. auction-server/src/main/java/com/auction/server/AuctionServerFacade.java
6. auction-common/src/main/java/com/auction/model/auction/AutoBidConfig.java
7. auction-server/src/main/java/com/auction/dao/sqlite/SqliteAutoBidDao.java
8. auction-server/src/main/java/com/auction/service/BidService.java
9. auction-common/src/main/java/com/auction/model/auction/Auction.java
10. auction-common/src/main/java/com/auction/model/auction/BidTransaction.java
```

### Luồng admin thanh toán/hủy

```text
1. auction-client/src/main/resources/fxml/admin-view.fxml
2. auction-client/src/main/java/com/auction/controller/AdminController.java
3. auction-client/src/main/java/com/auction/client/AuctionClientGateway.java
4. auction-client/src/main/java/com/auction/client/SocketAuctionClientGateway.java
5. auction-server/src/main/java/com/auction/server/AuctionSocketServer.java
6. auction-server/src/main/java/com/auction/server/AuctionServerFacade.java
7. auction-server/src/main/java/com/auction/service/AuctionService.java
8. auction-common/src/main/java/com/auction/model/auction/Auction.java
9. auction-server/src/main/java/com/auction/dao/sqlite/SqliteAuctionDao.java
10. auction-server/src/main/java/com/auction/server/BroadcastManager.java
```

---

## 8. Nếu chỉ còn ít thời gian thì học file nào trước?

Nếu chỉ còn 6 tiếng:

```text
1. Auction.java
2. BidService.java
3. AuctionServerFacade.java
4. ServerContext.java
5. SocketAuctionClientGateway.java
6. AuctionSocketServer.java
7. BroadcastManager.java
8. AppContext.java
9. SceneNavigator.java
10. AuctionDetailController.java
11. SellerController.java
12. AdminController.java
```

Nếu chỉ còn 2 tiếng:

```text
1. Auction.java
2. BidService.java
3. AuctionServerFacade.java
4. AppContext.java
5. SocketAuctionClientGateway.java
6. AuctionSocketServer.java
7. 07_LUONG_NGHIEP_VU_DE_DOC_CODE.md
```

---

## 9. Checklist sau khi học xong thứ tự này

```text
[ ] Biết common học từ Entity -> Enum -> User -> Item -> Auction -> Protocol.
[ ] Biết server học từ ServerContext -> DB/DAO -> Service -> Facade -> Socket.
[ ] Biết client học từ Main -> AppContext -> SceneNavigator -> Gateway -> Controller.
[ ] Biết database học từ schema -> DatabaseManager -> DAO interface -> SQLite DAO.
[ ] Biết socket học từ RequestType -> Request/Response -> Gateway -> SocketServer -> Broadcast.
[ ] Biết nâng cao học theo từng tính năng, không học rải file.
[ ] Tự lần được luồng login.
[ ] Tự lần được luồng tạo phiên.
[ ] Tự lần được luồng đặt giá.
[ ] Tự lần được luồng realtime.
[ ] Tự lần được luồng auto-bid.
```
