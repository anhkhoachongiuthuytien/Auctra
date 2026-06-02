# Sổ tay bảo vệ BTL Auctra

Mục tiêu của file này là giúp trả lời khi thầy hỏi bất kỳ phần nào trong code. Không học thuộc từng dòng. Hãy học theo 4 câu hỏi:

1. File này thuộc tầng nào?
2. Nó chịu trách nhiệm gì?
3. Nó gọi tới file nào tiếp theo?
4. Nếu lỗi thì lỗi nghiệp vụ nào có thể xảy ra?

## 1. Câu trả lời tổng quan 30 giây

Dự án là hệ thống đấu giá trực tuyến Auctra, viết bằng Java 17, JavaFX, SQLite và Java Socket.

Kiến trúc chia 3 module:

- `auction-common`: chứa model, enum, exception, DAO interface, protocol DTO/request/response. Đây là phần dùng chung giữa client và server.
- `auction-server`: chứa service nghiệp vụ, DAO cài đặt SQLite/in-memory, server socket, facade và khởi tạo dữ liệu demo.
- `auction-client`: chứa UI JavaFX, FXML, controller, ViewModel, gateway kết nối server local hoặc socket.

Câu nói khi thầy hỏi kiến trúc:

> Em tách project thành 3 module để common chứa domain dùng chung, server xử lý nghiệp vụ và dữ liệu, client chỉ hiển thị UI và gọi qua gateway. Nhờ vậy client có thể chạy local cùng process hoặc socket qua mạng mà không đổi controller.

## 2. Sơ đồ luồng tổng quát

```text
FXML View
  -> Controller
  -> ViewModel hoặc Gateway
  -> AuctionClientGateway
  -> LocalAuctionClientGateway hoặc SocketAuctionClientGateway
  -> AuctionServerFacade
  -> Service
  -> DAO
  -> SQLite / in-memory
```

Ví dụ đặt giá:

```text
auction-detail-view.fxml
  -> AuctionDetailController.handlePlaceBid()
  -> AuctionListViewModel.placeBid()
  -> AuctionClientGateway.placeBid()
  -> LocalAuctionClientGateway hoặc SocketAuctionClientGateway
  -> AuctionServerFacade.placeBid()
  -> BidService.placeBid()
  -> Auction.addBid()
  -> AuctionDao.save()
```

## 3. Cách trả lời khi bị hỏi một file bất kỳ

Mẫu trả lời:

> File này thuộc tầng `<tầng>`. Nhiệm vụ chính là `<trách nhiệm>`. Nó nhận dữ liệu từ `<nguồn>`, kiểm tra `<điều kiện>`, rồi gọi `<file/service tiếp theo>`. Nếu có lỗi thì ném/hiển thị `<exception/message>`.

Ví dụ với `BidService.java`:

> File này thuộc tầng service ở server. Nó xử lý nghiệp vụ đặt giá. Nó kiểm tra auctionId, bidder, amount, tìm Auction qua AuctionDao, sau đó khóa auction để tránh hai người đặt giá đồng thời làm sai currentPrice. Nếu giá hợp lệ thì tạo BidTransaction, gọi Auction.addBid và lưu lại qua auctionDao.save.

## 4. Module auction-common

### 4.1 Vai trò

`auction-common` là lõi domain dùng chung. Nếu thầy hỏi tại sao không để model trong client hoặc server:

> Vì cả client và server đều cần hiểu User, Auction, DTO, RequestType. Tách common giúp tránh duplicate class và đảm bảo protocol hai bên thống nhất.

### 4.2 Model chính

#### `Entity`

File: `auction-common/src/main/java/com/auction/model/base/Entity.java`

- Lớp cha trừu tượng cho các entity có `id`.
- Implements `Serializable`, nên object có thể đi qua socket.

Trả lời:

> Entity gom thuộc tính id dùng chung cho User, Item, Auction và hỗ trợ serialize khi truyền qua network.

#### `User`, `Bidder`, `Seller`, `Admin`

Files:

- `model/user/User.java`
- `model/user/Bidder.java`
- `model/user/Seller.java`
- `model/user/Admin.java`

Ý nghĩa:

- `User` là abstract class có `username`, `email`.
- `Bidder` là người đặt giá.
- `Seller` là người bán/tạo phiên.
- `Admin` là quản trị viên.

OOP cần nói:

> Đây là kế thừa để phân quyền theo role. Khi login xong, SceneNavigator kiểm tra user thuộc Seller/Admin/Bidder để đưa tới màn hình tương ứng.

#### `Item`, `Art`, `Electronics`, `Vehicle`

Files:

- `model/item/Item.java`
- `model/item/Art.java`
- `model/item/Electronics.java`
- `model/item/Vehicle.java`

Ý nghĩa:

- `Item` có `name`, `description`, `startingPrice`, `imagePath`.
- Các lớp con biểu diễn loại vật phẩm.

Trả lời:

> Item là lớp cha cho các loại vật phẩm. Các subclass hiện chưa thêm thuộc tính riêng nhiều, nhưng thể hiện mở rộng OOP: sau này Art có thể thêm tác giả, Vehicle thêm biển số, Electronics thêm bảo hành.

#### `Auction`

File: `model/auction/Auction.java`

Đây là file quan trọng nhất.

Thuộc tính:

- `item`: vật phẩm.
- `seller`: người bán.
- `currentPrice`: giá hiện tại.
- `status`: trạng thái phiên.
- `bids`: lịch sử đặt giá.
- `winner`: người đang thắng.
- `stateLock`: khóa tránh race condition.
- `bidObservers`, `globalBidObservers`: observer khi có bid.

Method cần thuộc:

- `start()`: `OPEN -> RUNNING`.
- `finish()`: `RUNNING -> FINISHED`.
- `cancel()`: hủy khi `OPEN/RUNNING/FINISHED`.
- `markPaid()`: `FINISHED -> PAID`.
- `addBid()`: kiểm tra phiên đang RUNNING, bid không null, amount > currentPrice, cập nhật `currentPrice`, `winner`, thêm vào `bids`.

Trả lời khi thầy hỏi tại sao có lock:

> Vì đặt giá có thể xảy ra đồng thời. Nếu hai thread cùng đọc currentPrice rồi cùng đặt giá, dữ liệu có thể sai. `ReentrantLock` trong Auction bảo vệ thay đổi trạng thái và giá trong cùng một phiên.

Trả lời khi hỏi observer:

> Sau khi addBid thành công, Auction tạo BidEvent và notify observer. Phần này dùng Observer Pattern để tách logic cập nhật/broadcast khỏi logic đặt giá.

#### `BidTransaction`

File: `model/auction/BidTransaction.java`

- Lưu một lượt đặt giá.
- Có `bidder`, `amount`, `bidTime`.

Trả lời:

> Đây không phải phiên đấu giá, mà là một record lịch sử của từng lần đặt giá trong phiên.

### 4.3 Enum

#### `AuctionStatus`

File: `enums/AuctionStatus.java`

Các trạng thái thường có:

- `OPEN`: mới tạo, chưa chạy.
- `RUNNING`: đang đấu giá, bidder được đặt giá.
- `FINISHED`: đã kết thúc, chờ thanh toán.
- `PAID`: đã thanh toán.
- `CANCELED`: bị hủy.

Luồng trạng thái:

```text
OPEN -> RUNNING -> FINISHED -> PAID
OPEN/RUNNING/FINISHED -> CANCELED
```

#### `ItemType`

File: `enums/ItemType.java`

- Dùng để parse loại item từ string.
- Dùng trong `ItemFactory`.

### 4.4 Factory Pattern

File: `factory/ItemFactory.java`

Nhiệm vụ:

- Nhận `ItemType`.
- Tạo đúng subclass: `Art`, `Electronics`, `Vehicle`.

Trả lời:

> Đây là Factory Pattern. Thay vì controller/service phải if-else tạo từng loại item, toàn bộ logic tạo item được gom vào ItemFactory. Khi thêm loại item mới thì sửa factory và enum.

### 4.5 DAO Interface

Files:

- `dao/UserDao.java`
- `dao/ItemDao.java`
- `dao/AuctionDao.java`

Ý nghĩa:

> Common chỉ định nghĩa interface DAO, còn server quyết định dùng SQLite hay in-memory. Đây là Dependency Inversion: service phụ thuộc abstraction, không phụ thuộc trực tiếp SQLite.

### 4.6 Protocol socket

Files:

- `protocol/AuctionRequest.java`
- `protocol/AuctionResponse.java`
- `protocol/RequestType.java`
- `protocol/AuctionDto.java`
- `protocol/UserDto.java`
- `protocol/BidDto.java`
- `protocol/DtoMapper.java`

Trả lời:

> Khi chạy socket, client không gọi service trực tiếp. Nó gửi AuctionRequest có RequestType và params. Server xử lý rồi trả AuctionResponse. DTO dùng để truyền dữ liệu qua socket ổn định hơn thay vì phụ thuộc object domain trực tiếp.

## 5. Module auction-server

### 5.1 `ServerContext`

File: `auction-server/src/main/java/com/auction/server/ServerContext.java`

Nhiệm vụ:

- Khởi tạo `DatabaseManager`.
- Tạo DAO SQLite.
- Tạo các service.
- Seed dữ liệu demo.

Demo accounts:

- `seller@auction.local`
- `bidder@auction.local`
- `admin@auction.local`
- Password: `demo12345`

Trả lời:

> ServerContext là composition root phía server. Nó wiring các dependency như DatabaseManager, DAO, Service và seed dữ liệu ban đầu.

### 5.2 `AuctionServerFacade`

File: `server/AuctionServerFacade.java`

Nhiệm vụ:

- Là API trung gian cho client gọi.
- Gom các method như `login`, `register`, `listAuctions`, `createAuctionForSeller`, `placeBid`.

Trả lời:

> Facade Pattern. Client không cần biết bên trong có AuthService, SellerService, BidService. Client chỉ gọi facade qua gateway.

### 5.3 Service nghiệp vụ

#### `AuthService`

File: `service/AuthService.java`

Nhiệm vụ:

- Đăng ký Seller/Bidder/Admin.
- Login.
- Reset password.
- Kiểm tra email tồn tại.
- Hash password bằng `PasswordHasher`.

Điều kiện validate:

- Username không rỗng.
- Email không rỗng.
- Password không rỗng và >= 8 ký tự.
- Email không trùng.
- Login kiểm tra password hash.

Trả lời:

> AuthService không lưu password plain text, mà lưu hash. Khi login thì lấy hash từ UserDao và dùng PasswordHasher.matches để so sánh.

#### `SellerService`

File: `service/SellerService.java`

Nhiệm vụ:

- Tạo item.
- Tạo auction cho seller.

Validate:

- Type không null.
- Name/description không rỗng.
- Starting price > 0.
- Seller/item không null khi tạo auction.

Luồng:

```text
SellerController
  -> gateway.createAuctionForSeller()
  -> AuctionServerFacade
  -> SellerService.createItem()
  -> ItemFactory.createItem()
  -> itemDao.save()
  -> SellerService.createAuction()
  -> auctionDao.save()
```

#### `AuctionService`

File: `service/AuctionService.java`

Nhiệm vụ:

- Tạo auction.
- Start/finish/cancel/mark paid.
- List auctions.
- Get auction by id.

Trả lời:

> AuctionService quản lý vòng đời phiên đấu giá. Logic chuyển trạng thái thật nằm trong Auction model; service lấy object từ DAO, gọi method domain rồi save lại.

#### `BidService`

File: `service/BidService.java`

Đây là file cần thuộc nhất.

Nhiệm vụ:

- Đặt giá cho auction.

Validate:

- `auctionId` không rỗng.
- `bidder` không null.
- `amount` là số hợp lệ và > 0.
- Auction tồn tại.
- Auction đang mở.
- Amount > currentPrice.

Concurrency:

- `synchronized (auction)` để tránh 2 thread cùng đặt giá trên cùng auction.
- Trong `Auction.addBid()` còn có `ReentrantLock`.

Trả lời:

> BidService kiểm tra đầu vào và khóa auction theo object để chống race condition. Sau đó tạo BidTransaction, gọi auction.addBid và lưu lại auction qua DAO.

### 5.4 DAO SQLite

Files:

- `dao/sqlite/SqliteUserDao.java`
- `dao/sqlite/SqliteItemDao.java`
- `dao/sqlite/SqliteAuctionDao.java`

Vai trò:

- Cài đặt interface DAO bằng SQLite.
- Chuyển dữ liệu giữa SQL row và model.

#### `DatabaseManager`

File: `db/DatabaseManager.java`

Nhiệm vụ:

- Tạo connection từ JDBC URL.
- Bật foreign key cho SQLite.
- Đọc `schema.sql`.
- Tự thêm cột nếu database cũ thiếu `password_hash` hoặc `image_path`.

Trả lời:

> DatabaseManager giúp app tự khởi tạo schema khi chạy lần đầu. Nó cũng xử lý migration nhỏ để database cũ không bị lỗi khi thêm cột mới.

#### `schema.sql`

File: `auction-server/src/main/resources/db/schema.sql`

Bảng:

- `users`: id, username, email, role, password_hash.
- `items`: id, name, description, starting_price, type, image_path.
- `auctions`: id, item_id, seller_id, current_price, status, winner_id.
- `bids`: auction_id, bidder_id, amount, bid_time.

Trả lời:

> Database tách users/items/auctions/bids để chuẩn hóa dữ liệu. Auction tham chiếu item, seller, winner; bids tham chiếu auction và bidder.

### 5.5 Socket server

Files:

- `server/ServerMain.java`
- `server/AuctionSocketServer.java`
- `server/BroadcastManager.java`

Vai trò:

- `ServerMain`: entry point server.
- `AuctionSocketServer`: nhận request socket từ client, dispatch theo RequestType.
- `BroadcastManager`: gửi event realtime cho client subscribe.

Trả lời:

> Socket mode dùng ObjectInputStream/ObjectOutputStream. Client gửi AuctionRequest, server trả AuctionResponse. Ngoài request-response còn có luồng subscribe update để client tự refresh khi có event.

## 6. Module auction-client

### 6.1 Entry và điều hướng

#### `Main`

File: `auction-client/src/main/java/com/auction/Main.java`

Nhiệm vụ:

- Entry JavaFX.
- Tạo `AppContext`.
- Tạo `SceneNavigator`.
- Mở màn login.

Có 2 chế độ:

- LOCAL mặc định.
- SOCKET nếu chạy với `--socket [host] [port]`.

#### `AppContext`

File: `app/AppContext.java`

Nhiệm vụ:

- Tạo gateway.
- LOCAL: tạo `ServerContext` + `AuctionServerFacade` + `LocalAuctionClientGateway`.
- SOCKET: tạo `SocketAuctionClientGateway`.

Trả lời:

> AppContext giúp controller không cần biết app đang chạy local hay socket. Controller chỉ gọi AuctionClientGateway.

#### `SceneNavigator`

File: `app/SceneNavigator.java`

Nhiệm vụ:

- Load FXML.
- Lấy controller.
- Gọi `init(...)`.
- Đổi scene/root.
- Điều hướng theo role trong `showHome(User user)`.

Trả lời:

> SceneNavigator gom logic chuyển màn hình. Sau login, nếu user là Seller thì vào seller dashboard, Admin thì vào admin dashboard, còn lại là danh sách đấu giá.

### 6.2 Gateway Pattern

#### `AuctionClientGateway`

File: `client/AuctionClientGateway.java`

Vai trò:

- Interface cho mọi thao tác client cần: login, register, list auctions, place bid...

#### `LocalAuctionClientGateway`

File: `client/LocalAuctionClientGateway.java`

- Gọi trực tiếp `AuctionServerFacade`.
- Không qua network.
- Dùng khi chạy demo local.

#### `SocketAuctionClientGateway`

File: `client/SocketAuctionClientGateway.java`

- Kết nối TCP tới server.
- Gửi `AuctionRequest`.
- Nhận `AuctionResponse`.
- Có reconnect một lần nếu lỗi.
- Có thread riêng subscribe realtime update.

Trả lời:

> Đây là Gateway Pattern. Controller/ViewModel không cần biết backend local hay socket. Muốn đổi chế độ chỉ thay implementation gateway.

### 6.3 ViewModel

#### `LoginViewModel`

File: `presentation/LoginViewModel.java`

Nhiệm vụ:

- Bọc logic login/register/reset password cho UI.
- Trả `LoginResult` gồm success/user/message.
- Kiểm tra confirm password trước khi gọi gateway.

Trả lời:

> ViewModel giúp controller không phải chứa quá nhiều logic xử lý kết quả. Controller chỉ lấy result rồi hiển thị toast hoặc chuyển màn.

#### `AuctionListViewModel`

File: `presentation/AuctionListViewModel.java`

Nhiệm vụ:

- Load auctions.
- Format welcome/summary message.
- Xử lý place bid ở tầng trình bày.
- Kiểm tra user phải là Bidder.
- Parse amount từ text.

### 6.4 Controller và màn hình

#### `AuthController`

FXML: `login-view.fxml`

Nhiệm vụ:

- Đọc email/password.
- Gọi `LoginViewModel.login`.
- Nếu thành công: `navigator.showHome(user)`.
- Nếu lỗi: hiển thị message/toast và shake field.

#### `RegisterController`

FXML: `register-view.fxml`

Nhiệm vụ:

- Đăng ký user.
- Load role từ gateway.
- Kiểm tra password confirm qua ViewModel.
- Có password strength UI.

#### `ForgotPasswordController`

FXML: `forgot-password-view.fxml`

Nhiệm vụ:

- Reset password bằng email + username + password mới.
- Khi thành công quay lại login.

#### `AuctionController`

FXML: `auction-list-view.fxml`

Nhiệm vụ:

- Hiển thị danh sách đấu giá.
- Filter ALL/ACTIVE/FINISHED.
- Search theo item/seller.
- Double click mở chi tiết.
- Nút action đặt giá/chi tiết.

Luồng load:

```text
init()
  -> setupUserBadge()
  -> configureTable()
  -> configureFilterAndSearch()
  -> refreshTable()
```

#### `AuctionDetailController`

FXML: `auction-detail-view.fxml`

Nhiệm vụ:

- Hiển thị ảnh, item, seller, price, winner, bid history.
- Cho bidder đặt giá.
- Nút +10/+100/+1000.
- Refresh lại auction.

Method cần thuộc:

- `renderAuction(Auction auction)`: đổ dữ liệu lên UI.
- `handlePlaceBid()`: gọi ViewModel placeBid.
- `findAuction()`: tìm lại auction từ gateway theo id.

#### `SellerController`

FXML: `seller-view.fxml`

Nhiệm vụ:

- Seller tạo item + auction.
- Chọn/kéo-thả ảnh.
- Start/finish auction.
- Xem detail.
- Hiển thị bảng các auction của seller.
- Tính stat cards.

Luồng tạo phiên:

```text
handleCreateAuction()
  -> copy ảnh vào ImageStorage nếu có
  -> parse startingPrice
  -> gateway.createAuctionForSeller(...)
  -> clear form
  -> refreshAuctions()
```

#### `AdminController`

FXML: `admin-view.fxml`

Nhiệm vụ:

- Dashboard thống kê.
- Xem users.
- Xem auctions.
- Search global.
- Cancel auction.
- Mark auction paid.

Luồng admin hủy phiên:

```text
handleCancelAuction()
  -> lấy selected auction
  -> showConfirmDialog
  -> gateway.cancelAuction(id)
  -> refreshData()
```

#### `ProfileController`

FXML: `profile-view.fxml`

Nhiệm vụ:

- Hiển thị avatar, username, role, email, user id.
- Điều hướng về auctions.
- Edit profile hiện đang là chức năng đang phát triển.

Bug đã sửa:

- Email trong card thông tin được gắn vào `emailValueLabel`.

### 6.5 UI utility

Files:

- `util/UiEffects.java`: toast, loading overlay, confirm dialog.
- `ui/UIAnimations.java`: fade, slide, pulse, shake, bounce.
- `ui/ThemeManager.java`: light/dark theme class.
- `ui/ResponsiveManager.java`: collapse sidebar khi width nhỏ.
- `ui/ToastManager.java`: wrapper cho toast.
- `ui/BadgeFactory.java`: apply class badge theo AuctionStatus.
- `ui/IconFactory.java`: tạo label icon dạng text ổn định.

Trả lời:

> UI utility được tách riêng để controller không phải tự viết animation/toast/dialog lặp lại.

## 7. Các design pattern cần nói được

### 7.1 MVC/MVVM nhẹ

- FXML = View.
- Controller = xử lý event UI.
- ViewModel = logic trình bày.
- Service = logic nghiệp vụ server.

### 7.2 DAO Pattern

- Interface trong common.
- SQLite/in-memory implementation trong server.
- Service phụ thuộc DAO interface.

### 7.3 Factory Pattern

- `ItemFactory` tạo subclass item.

### 7.4 Facade Pattern

- `AuctionServerFacade` che giấu các service bên trong server.

### 7.5 Gateway Pattern

- `AuctionClientGateway` che giấu local/socket.

### 7.6 Observer Pattern

- `BidObserver`, `BidEvent`, `Auction.addObserver`, `Auction.addGlobalObserver`.
- Dùng khi có bid mới.

### 7.7 Singleton

- `AuctionManager.getInstance()`.
- Quản lý auction active.

## 8. Các câu thầy dễ hỏi và câu trả lời

### Hỏi: Tại sao phải chia 3 module?

Trả lời:

> Vì common chứa model/protocol dùng chung, server xử lý nghiệp vụ/database, client xử lý UI. Tách như vậy giảm phụ thuộc, dễ test và cho phép client chạy local hoặc socket.

### Hỏi: Đặt giá được kiểm tra ở đâu?

Trả lời:

> Có nhiều lớp kiểm tra. UI/ViewModel parse số và kiểm tra user là Bidder. BidService kiểm tra auctionId, bidder, amount, auction tồn tại. Auction.addBid kiểm tra phiên đang RUNNING và amount > currentPrice. Đây là defense-in-depth, tránh chỉ tin vào UI.

### Hỏi: Vì sao vừa `synchronized` trong BidService vừa có `ReentrantLock` trong Auction?

Trả lời:

> `synchronized (auction)` bảo vệ toàn bộ thao tác service gồm kiểm tra, tạo bid và save. `ReentrantLock` trong Auction bảo vệ trạng thái nội bộ nếu Auction được gọi từ nơi khác. Có hơi dư, nhưng tăng an toàn cho concurrent bid.

### Hỏi: Password lưu ở đâu?

Trả lời:

> Password không lưu plain text. AuthService gọi PasswordHasher.hash rồi UserDao lưu `password_hash`. Khi login thì so sánh bằng PasswordHasher.matches.

### Hỏi: Role được phân màn hình thế nào?

Trả lời:

> Sau login, AuthController gọi SceneNavigator.showHome(user). Trong showHome, nếu user là Seller thì mở seller dashboard, Admin thì mở admin dashboard, còn Bidder thì mở auction list.

### Hỏi: Local mode khác socket mode thế nào?

Trả lời:

> Local mode chạy server facade cùng process, không qua mạng. Socket mode dùng SocketAuctionClientGateway gửi AuctionRequest tới AuctionSocketServer. Controller không đổi vì đều gọi qua AuctionClientGateway.

### Hỏi: SQLite schema gồm gì?

Trả lời:

> Có users, items, auctions, bids. Auction tham chiếu item, seller, winner. Bids tham chiếu auction và bidder. SQLite bật foreign key để đảm bảo quan hệ.

### Hỏi: Nếu thầy hỏi file FXML?

Trả lời:

> FXML chỉ mô tả layout và fx:id. Event onAction trỏ tới method trong controller. Ví dụ `onAction="#handlePlaceBid"` trong auction-detail gọi method handlePlaceBid của AuctionDetailController.

### Hỏi: Nếu thầy hỏi CSS mới?

Trả lời:

> CSS được tách thành tokens, base, components, screens, themes. `app.css` import tất cả. Tokens chứa màu/font chung; components chứa button/card/table/form; screens chứa style riêng từng màn.

### Hỏi: Admin hủy phiên ở đâu?

Trả lời:

> AdminController.handleCancelAuction lấy auction đang chọn, hiện confirm dialog, nếu xác nhận thì gọi gateway.cancelAuction, qua facade tới AuctionService.cancelAuction, trong domain Auction.cancel đổi status sang CANCELED rồi save DAO.

### Hỏi: Seller tạo phiên ở đâu?

Trả lời:

> SellerController.handleCreateAuction lấy dữ liệu form, copy ảnh nếu có, parse giá, gọi gateway.createAuctionForSeller. ServerFacade gọi SellerService tạo item bằng ItemFactory rồi tạo Auction và lưu.

### Hỏi: Realtime update hoạt động thế nào?

Trả lời:

> SocketAuctionClientGateway mở thêm listenerThread subscribe update. Khi nhận AuctionEvent thì gọi Platform.runLater(ClientEventManager::fireUpdate). Controller đã đăng ký listener để refresh lại dữ liệu trên JavaFX thread.

### Hỏi: Tại sao phải dùng Platform.runLater?

Trả lời:

> JavaFX UI chỉ được cập nhật trên JavaFX Application Thread. Listener socket chạy background thread, nên phải dùng Platform.runLater để cập nhật UI an toàn.

## 9. Nếu bị hỏi theo file, tra nhanh ở đây

### Common

| File | Trả lời nhanh |
|---|---|
| `Entity` | Lớp cha có id, Serializable |
| `User` | Lớp cha user, có username/email |
| `Bidder/Seller/Admin` | Role cụ thể để phân quyền |
| `Item` | Vật phẩm đấu giá, có name/description/startingPrice/imagePath |
| `Art/Electronics/Vehicle` | Subclass của Item |
| `Auction` | Aggregate chính, quản lý trạng thái, giá, bid, winner |
| `BidTransaction` | Một lần đặt giá |
| `ItemFactory` | Tạo item theo ItemType |
| `AuctionStatus` | Trạng thái phiên |
| `AuctionRequest/Response` | Gói request/response qua socket |
| `DtoMapper` | Chuyển domain object sang DTO và ngược lại |

### Server

| File | Trả lời nhanh |
|---|---|
| `ServerContext` | Wiring DAO/service/database, seed demo |
| `AuctionServerFacade` | API đơn giản cho client gọi |
| `AuthService` | Login/register/reset password |
| `SellerService` | Tạo item và auction |
| `AuctionService` | Quản lý vòng đời auction |
| `BidService` | Đặt giá, validate, chống concurrency |
| `UserService` | Lấy danh sách user |
| `DatabaseManager` | Kết nối SQLite và init schema |
| `SqliteUserDao` | CRUD user/password hash |
| `SqliteItemDao` | CRUD item |
| `SqliteAuctionDao` | CRUD auction và bids |
| `AuctionSocketServer` | Nhận request socket |
| `BroadcastManager` | Gửi event realtime |

### Client

| File | Trả lời nhanh |
|---|---|
| `Main` | Entry JavaFX |
| `AppContext` | Chọn local/socket gateway |
| `SceneNavigator` | Load FXML, chuyển màn |
| `AuctionClientGateway` | Interface thao tác client |
| `LocalAuctionClientGateway` | Gọi facade trực tiếp |
| `SocketAuctionClientGateway` | Gọi server qua socket |
| `LoginViewModel` | Logic login/register/reset cho UI |
| `AuctionListViewModel` | Load auction, place bid |
| `AuthController` | Login screen |
| `RegisterController` | Register screen |
| `ForgotPasswordController` | Reset password screen |
| `AuctionController` | List/filter/search auction |
| `AuctionDetailController` | Xem chi tiết và đặt giá |
| `SellerController` | Seller dashboard |
| `AdminController` | Admin dashboard |
| `ProfileController` | Profile screen |
| `UiEffects` | Toast/loading/confirm dialog |
| `UIAnimations` | Animation dùng chung |

## 10. Cách học trong 1 ngày

### Vòng 1: 60 phút

Học thuộc kiến trúc:

```text
common = model/protocol/interface
server = service/dao/socket/database
client = UI/controller/viewmodel/gateway
```

### Vòng 2: 90 phút

Học 4 luồng:

1. Login.
2. Seller tạo auction.
3. Bidder đặt giá.
4. Admin hủy/paid auction.

### Vòng 3: 60 phút

Mở code và tự nói thành tiếng:

- `Auction.java`
- `BidService.java`
- `SellerService.java`
- `AuthService.java`
- `AppContext.java`
- `SceneNavigator.java`
- `AuctionDetailController.java`

### Vòng 4: 60 phút

Luyện câu hỏi:

- Tại sao chia module?
- Tại sao dùng DAO?
- Tại sao dùng Factory?
- Tại sao dùng Gateway?
- Tại sao cần lock khi đặt giá?
- Database có những bảng nào?
- Socket mode chạy thế nào?

## 11. Chiến thuật khi không nhớ code

Nếu không nhớ dòng code, đừng im. Trả lời theo tầng:

> Em nhớ chức năng này đi từ Controller qua Gateway tới Service. Phần validate nghiệp vụ nằm ở Service và domain model. Em mở file service tương ứng để chỉ rõ điều kiện.

Nếu bị hỏi file lạ:

> File này thuộc nhóm `<common/server/client>`. Em sẽ xác định từ package: nếu package `model/dao/protocol` là common; `service/db/server` là server; `controller/app/client/presentation` là client.

Nếu bị hỏi vì sao có lỗi tiếng Việt mojibake trong terminal:

> Source project dùng UTF-8. PowerShell đôi khi hiển thị sai encoding, nhưng Maven build dùng `project.build.sourceEncoding=UTF-8`, app JavaFX vẫn đọc resource UTF-8.

## 12. Lệnh cần nhớ

Chạy test client:

```powershell
mvn -pl auction-client test
```

Chạy test toàn dự án:

```powershell
mvn test
```

Chạy client JavaFX:

```powershell
mvn -pl auction-client javafx:run
```

Chạy server socket:

```powershell
mvn -pl auction-server package
java -jar auction-server/target/auction-server.jar
```

Chạy client socket:

```powershell
mvn -pl auction-client javafx:run -Djavafx.args="--socket localhost 9999"
```

## 13. Những phần nên nhận là hạn chế nếu thầy hỏi

Nói thẳng, nhưng biến thành hướng phát triển:

- Một số subclass item hiện chưa có thuộc tính riêng nhiều, chủ yếu để thể hiện khả năng mở rộng OOP.
- Profile edit/avatar upload/history tab mới là UI chuẩn bị, backend chưa đầy đủ dữ liệu lịch sử riêng.
- Socket realtime hiện refresh dữ liệu khi có event, chưa phải diff từng dòng cực tối ưu.
- Một số UI nâng cấp dùng text icon thay vì thư viện icon để tránh phụ thuộc download ngoài.

Cách nói:

> Phần này nhóm em thiết kế theo hướng mở rộng. Hiện tại đã có khung UI/kiến trúc, nếu phát triển tiếp sẽ bổ sung dữ liệu backend tương ứng.

## 14. Phần phải thuộc thật kỹ

Nếu chỉ có thời gian học 5 file, học:

1. `Auction.java`
2. `BidService.java`
3. `AuthService.java`
4. `SellerService.java`
5. `SceneNavigator.java`

Nếu chỉ có thời gian học 3 luồng, học:

1. Login.
2. Seller tạo phiên.
3. Bidder đặt giá.

Nếu thầy hỏi bất kỳ phần nào, cố gắng kéo câu trả lời về 3 luồng trên.

