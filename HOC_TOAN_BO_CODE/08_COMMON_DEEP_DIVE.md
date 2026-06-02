# 08 - Common deep dive: học kỹ domain, protocol, utility

File này dùng khi bạn muốn hiểu sâu `auction-common`, đặc biệt là các class có nhiều logic như `Auction`, `DtoMapper`, `PasswordHasher`, `ImageStorage`.

---

## 1. Entity.java

### Vai trò

`Entity` là lớp cha cho object có định danh. Trong dự án, `User`, `Item`, `Auction` đều là entity.

### Vì sao implements Serializable?

Vì một số object có thể được truyền qua Java Socket bằng `ObjectOutputStream`. Nếu object không serializable, khi ghi qua socket sẽ lỗi.

### Cách trả lời

```text
Entity gom logic id chung và cho phép object domain có thể serialize khi truyền qua mạng.
```

---

## 2. User.java, Admin.java, Seller.java, Bidder.java

## User.java

### Vai trò

`User` là lớp cha trừu tượng cho người dùng. Nó chứa thông tin chung như `username`, `email` và các thông tin profile.

### Ai tạo User?

```text
AuthService.registerSeller/registerBidder/registerAdmin
DbMappers khi đọc từ database
DtoMapper khi dựng lại từ UserDto
```

### Ai dùng User?

```text
SceneNavigator dùng để phân màn hình theo role.
Controller dùng để hiển thị tên/avatar/profile.
Service dùng để kiểm tra seller/bidder/admin.
```

## Admin.java

Admin là user quản trị. Sau login, nếu object là `Admin`, `SceneNavigator` mở `admin-view.fxml`.

## Seller.java

Seller là user bán hàng. Seller tạo item và auction.

## Bidder.java

Bidder là user đặt giá. `BidService.placeBid()` yêu cầu bidder không null và phải là bidder hợp lệ.

### Câu hỏi hay gặp

```text
Hỏi: Vì sao dùng subclass Admin/Seller/Bidder thay vì chỉ lưu role string?
Trả lời: Dùng kế thừa giúp thể hiện rõ mô hình OOP và có thể mở rộng hành vi riêng cho từng loại user.
Hiện role vẫn có thể lưu trong database, nhưng trong code object cụ thể giúp SceneNavigator và service xử lý rõ hơn.
```

---

## 3. Item.java và các subclass

## Item.java

### Vai trò

`Item` là vật phẩm đem ra đấu giá. Một `Auction` luôn gắn với một `Item`.

### Dữ liệu quan trọng

```text
id
name
description
startingPrice
imagePath / imagePaths
```

### Chỗ dễ bị hỏi

```text
startingPrice là giá khởi điểm của item.
currentPrice của auction ban đầu lấy từ startingPrice, sau đó thay đổi theo bid.
```

## Art, Electronics, Vehicle, Other

Các subclass này đại diện loại vật phẩm. Hiện có thể chưa thêm nhiều field riêng, nhưng chúng chứng minh tính mở rộng OOP.

### Câu trả lời

```text
Hiện các subclass item chủ yếu để phân loại và thể hiện khả năng mở rộng.
Sau này Art có thể thêm tác giả, Vehicle thêm biển số, Electronics thêm bảo hành.
```

---

## 4. ItemFactory.java

### Vai trò

`ItemFactory` nhận `ItemType` rồi tạo đúng object con.

### Ai gọi?

```text
SellerService.createItem()
```

### Vì sao cần Factory?

Nếu không có factory, code tạo item sẽ rải ở controller/service bằng nhiều `if-else`. Factory gom logic tạo object vào một chỗ.

### Khi thêm loại item mới

```text
1. Thêm enum trong ItemType.
2. Thêm subclass nếu cần.
3. Sửa ItemFactory để tạo subclass mới.
4. Sửa UI nếu cần hiển thị lựa chọn mới.
```

---

## 5. Auction.java - file domain quan trọng nhất

`Auction` là trái tim của dự án. Nếu hiểu file này, bạn hiểu phần lõi đấu giá.

## Các field quan trọng

```text
item:
  Vật phẩm được đấu giá.

seller:
  Người tạo phiên.

currentPrice:
  Giá hiện tại. Ban đầu bằng item.startingPrice.

status:
  Trạng thái phiên: OPEN/RUNNING/FINISHED/PAID/CANCELED.

bids:
  Danh sách BidTransaction.

winner:
  Bidder đang thắng.

bidObservers:
  Observer của riêng phiên này.

globalBidObservers:
  Observer toàn cục, nghe bid từ mọi auction.

stateLock:
  ReentrantLock bảo vệ trạng thái nội bộ.

endTime:
  Thời điểm phiên kết thúc.
```

## Constructor

```text
Auction()
  Constructor rỗng, dùng khi framework/test/mapper cần tạo object.

Auction(String id, Item item, Seller seller)
  Tạo auction mới.
  currentPrice = item.startingPrice nếu có item.
  status = OPEN.
  endTime = now + 5 phút.
```

## start()

```text
Input: không có.
Điều kiện: status phải là OPEN.
Kết quả: status chuyển thành RUNNING.
Lỗi: nếu status không phải OPEN thì ném AuctionException.
```

Ý nghĩa:

```text
Phiên mới tạo chưa cho bid ngay. Seller/admin phải start để chuyển sang RUNNING.
```

## finish()

```text
Input: không có.
Điều kiện: status phải là RUNNING.
Kết quả: status chuyển thành FINISHED.
Lỗi: nếu chưa chạy hoặc đã kết thúc thì ném AuctionException.
```

Ý nghĩa:

```text
FINISHED nghĩa là phiên đã kết thúc nhưng chưa chắc đã thanh toán.
```

## cancel()

```text
Điều kiện: status là OPEN/RUNNING/FINISHED.
Kết quả: status chuyển thành CANCELED.
Lỗi: nếu trạng thái không cho hủy.
```

## markPaid()

```text
Điều kiện: status phải là FINISHED.
Kết quả: status chuyển thành PAID.
Lỗi: nếu phiên chưa kết thúc.
```

Ý nghĩa:

```text
PAID là trạng thái sau khi admin xác nhận thanh toán.
```

## addBid(BidTransaction bid)

Đây là method quan trọng nhất trong domain.

### Luồng xử lý

```text
1. stateLock.lock().
2. Kiểm tra status == RUNNING.
3. Kiểm tra bid != null.
4. Kiểm tra bid.amount > currentPrice.
5. bids.add(bid).
6. currentPrice = bid.amount.
7. winner = bid.bidder.
8. checkAndApplyAntiSniping().
9. unlock.
10. notifyBidPlaced(bid) ở ngoài lock.
```

### Vì sao notify nằm ngoài lock?

Nếu observer chạy code chậm hoặc gọi ngược vào auction, giữ lock trong lúc notify có thể gây block lâu hoặc deadlock. Vì vậy cập nhật state xong thì unlock, sau đó mới notify.

### Lỗi có thể xảy ra

```text
AuctionClosedException:
  Phiên chưa RUNNING hoặc đã đóng.

InvalidBidException:
  Bid null hoặc amount <= currentPrice.
```

### Câu trả lời khi bị hỏi

```text
Auction.addBid() bảo vệ rule cốt lõi: chỉ phiên RUNNING mới nhận bid,
giá mới phải lớn hơn giá hiện tại, sau bid phải cập nhật currentPrice, winner và lịch sử.
```

## checkAndApplyAntiSniping()

### Logic

```text
Nếu auction đang RUNNING
và endTime không null
và hiện tại nằm trong 60 giây cuối
thì endTime = endTime + 60 giây.
```

### Mục đích

Chống kiểu đặt giá sát giờ khiến người khác không kịp phản ứng.

## restoreState()

### Vai trò

DAO dùng method này khi đọc auction từ database. Vì constructor mặc định tạo trạng thái ban đầu, còn dữ liệu DB có status/currentPrice/winner/bids/endTime riêng, nên phải restore lại.

### Câu trả lời

```text
restoreState chỉ dùng khi dựng lại aggregate từ database, không dùng trong luồng đặt giá bình thường.
```

## Observer trong Auction

### addObserver()

Thêm observer cho riêng auction.

### addGlobalObserver()

Thêm observer nghe mọi auction.

### notifyBidPlaced()

Tạo `BidEvent`, gọi `onBidPlaced()` của observer riêng và observer toàn cục.

### Câu trả lời

```text
Observer Pattern giúp tách logic thông báo khỏi logic đặt giá.
Auction chỉ phát event, còn observer quyết định làm gì với event đó.
```

---

## 6. BidTransaction.java

### Vai trò

Đại diện một lần đặt giá.

### Dữ liệu

```text
bidder
amount
bidTime
```

### Ai tạo?

```text
BidService.placeBid() tạo bid thường.
BidService.runAutoBiddingEngine() tạo bid tự động.
SqliteAuctionDao/DbMappers dựng lại bid từ database.
```

### Ai dùng?

```text
Auction.addBid()
AuctionDetailController hiển thị bảng lịch sử bid.
SqliteAuctionDao lưu/đọc bảng bids.
```

---

## 7. AutoBidConfig.java

### Vai trò

Lưu cấu hình đấu giá tự động.

### Dữ liệu

```text
auctionId:
  Phiên áp dụng auto-bid.

bidderId:
  Người bật auto-bid.

maxPrice:
  Giá tối đa bidder chấp nhận.

increment:
  Bước tăng mỗi lần tự động đặt giá.
```

### Ai dùng?

```text
AuctionDetailController bật/tắt auto-bid.
AuctionServerFacade lưu/xóa config.
SqliteAutoBidDao đọc/ghi config.
BidService.runAutoBiddingEngine() dùng config để tự bid.
```

---

## 8. Enum deep dive

## AuctionStatus

Các trạng thái:

```text
OPEN:
  Mới tạo, chưa cho đặt giá.

RUNNING:
  Đang đấu giá, bidder có thể đặt giá.

FINISHED:
  Đã kết thúc, chờ thanh toán.

PAID:
  Đã thanh toán.

CANCELED:
  Đã hủy.
```

Luồng:

```text
OPEN -> RUNNING -> FINISHED -> PAID
OPEN/RUNNING/FINISHED -> CANCELED
```

## ItemType

Vai trò:

```text
Parse lựa chọn từ UI/string socket thành loại item.
ItemFactory dùng ItemType để tạo đúng subclass.
```

---

## 9. Protocol deep dive

## RequestType

`RequestType` là bảng API của hệ thống socket. Mỗi enum tương ứng một case trong `AuctionSocketServer.processRequest()`.

Ví dụ:

```text
LOGIN                 -> AuthService.login
REGISTER              -> AuthService.registerSeller/registerBidder
LIST_AUCTIONS          -> AuctionService.listAuctions
CREATE_AUCTION         -> SellerService.createItem + createAuction
PLACE_BID              -> BidService.placeBid
SUBSCRIBE_UPDATES      -> BroadcastManager.addClient
REGISTER_AUTO_BID      -> AutoBidDao.save
UPDATE_USER            -> UserService.updateUser
```

## AuctionRequest

### Vai trò

Client gửi request này qua socket.

### Cấu trúc

```text
type:
  RequestType.

data:
  Map key-value chứa tham số.
```

### Vì sao dùng Map?

Dễ thêm field cho từng request mà không phải tạo class request riêng cho từng API.

### Nhược điểm

Map dùng string key nên nếu gõ sai key sẽ khó phát hiện ở compile-time. Vì vậy key phải thống nhất giữa client và server.

## AuctionResponse

### Vai trò

Server trả kết quả về client.

### Cấu trúc

```text
success:
  true/false.

message:
  thông báo lỗi hoặc mô tả.

data:
  object trả về, thường là DTO/list DTO.
```

## AuctionEvent

### Vai trò

Server chủ động gửi event realtime về client.

### Dữ liệu

```text
eventType
auctionId nếu event liên quan một auction cụ thể
```

## DtoMapper

### Vai trò

Chuyển model sang DTO và DTO về model.

### Vì sao cần DTO?

```text
1. Không gửi dữ liệu nhạy cảm như password_hash.
2. Giảm độ phức tạp khi serialize.
3. Client chỉ cần dữ liệu hiển thị, không cần toàn bộ object nội bộ server.
4. Protocol ổn định hơn khi model thay đổi.
```

---

## 10. Utility deep dive

## PasswordHasher.java

### hash(rawPassword)

```text
1. Tạo salt ngẫu nhiên 16 bytes bằng SecureRandom.
2. Chạy PBKDF2WithHmacSHA256 với 65,536 iterations.
3. Encode salt và hash bằng Base64.
4. Trả string dạng iterations:salt:hash.
```

### matches(rawPassword, storedHash)

```text
1. Tách storedHash thành 3 phần.
2. Decode salt và expectedHash.
3. Hash rawPassword với cùng salt/iterations.
4. So sánh expectedHash và actualHash bằng constantTimeEquals.
```

### constantTimeEquals()

Không thoát sớm khi gặp byte sai. Nó duyệt hết mảng để giảm rủi ro timing attack.

## ImageStorage.java

### Vai trò

Quản lý lưu/kiểm tra ảnh vật phẩm/avatar.

### Ai dùng?

```text
SellerController lưu ảnh item.
ProfileController lưu avatar.
AuctionDetailController load ảnh item.
UserImageHelper hỗ trợ ảnh user.
```

## IdGenerator.java

Tạo id cho entity. Nếu bị hỏi, chỉ cần nói đây là utility để tránh rải logic tạo id trong nhiều class.

---

## Checklist hiểu common

```text
[ ] Giải thích được vì sao cần auction-common.
[ ] Giải thích được User/Seller/Bidder/Admin.
[ ] Giải thích được Item và ItemFactory.
[ ] Giải thích được Auction.start/finish/cancel/markPaid/addBid.
[ ] Giải thích được currentPrice khác startingPrice.
[ ] Giải thích được winner cập nhật ở đâu.
[ ] Giải thích được anti-sniping.
[ ] Giải thích được RequestType/AuctionRequest/AuctionResponse.
[ ] Giải thích được DTO và DtoMapper.
[ ] Giải thích được PasswordHasher.
```
