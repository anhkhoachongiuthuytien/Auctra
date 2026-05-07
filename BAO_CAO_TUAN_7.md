# Bao Cao Tuan 7: Da Luong Va Song Song

## Muc tieu tuan 7

Theo tieu chi cua bai giang tuan nay, he thong can:

- trien khai `Observer Pattern` de notify khi co bid moi
- co logic nghiep vu tao phien dau gia, dat gia va kiem tra hop le
- co logic chuyen trang thai `OPEN -> RUNNING -> FINISHED -> PAID/CANCELED`
- xu ly `concurrent bidding`, tranh race condition va lost update
- su dung `synchronized` va `ReentrantLock` cho cac thao tac critical
- tu hoc JavaFX de co cac man hinh co ban nhu `Login` va `Danh sach`

## Thay doi da thuc hien

### 1. Observer Pattern cho bid moi

Da bo sung package [src/main/java/com/auction/observer](/D:/BaitaplonTest/src/main/java/com/auction/observer) gom:

- [BidObserver.java](/D:/BaitaplonTest/src/main/java/com/auction/observer/BidObserver.java)
- [BidEvent.java](/D:/BaitaplonTest/src/main/java/com/auction/observer/BidEvent.java)
- [ConsoleBidObserver.java](/D:/BaitaplonTest/src/main/java/com/auction/observer/ConsoleBidObserver.java)

Trong [Auction.java](/D:/BaitaplonTest/src/main/java/com/auction/model/auction/Auction.java), moi auction hien co:

- danh sach observer
- `addObserver(...)`
- `removeObserver(...)`
- `notifyBidPlaced(...)`

Khi co bid hop le moi, auction se tao `BidEvent` va notify cho observer.

### 2. Hoan thien logic nghiep vu dau gia

Da giu va chinh lai cac service chinh:

- [AuctionService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/AuctionService.java)
- [BidService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/BidService.java)
- [SellerService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/SellerService.java)
- [ItemFactory.java](/D:/BaitaplonTest/src/main/java/com/auction/factory/ItemFactory.java)
- [ItemType.java](/D:/BaitaplonTest/src/main/java/com/auction/enums/ItemType.java)

Ket qua:

- tao item theo `ART`, `ELECTRONICS`, `VEHICLE`
- tao auction va luu vao `InMemoryAuctionDao`
- dat gia co validate:
  - auction phai ton tai
  - auction phai dang cho phep bid
  - gia moi phai cao hon gia hien tai

### 3. Chinh state machine theo tieu chi tuan 7

Da cap nhat [AuctionStatus.java](/D:/BaitaplonTest/src/main/java/com/auction/enums/AuctionStatus.java) thanh:

- `OPEN`
- `RUNNING`
- `FINISHED`
- `PAID`
- `CANCELED`

Da cap nhat [Auction.java](/D:/BaitaplonTest/src/main/java/com/auction/model/auction/Auction.java) de chay theo flow:

- auction moi tao ra o trang thai `OPEN`
- `start()` chuyen `OPEN -> RUNNING`
- `finish()` chuyen `RUNNING -> FINISHED`
- `markPaid()` chuyen `FINISHED -> PAID`
- `cancel()` cho phep huy tu `OPEN`, `RUNNING` hoac `FINISHED`

Bid chi duoc chap nhan khi auction o `RUNNING`.

### 4. Concurrent bidding

Da giu va nang cap phan xu ly bid dong thoi:

- [BidService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/BidService.java) su dung `synchronized (auction)` de bao ve critical section o service layer
- [Auction.java](/D:/BaitaplonTest/src/main/java/com/auction/model/auction/Auction.java) su dung `ReentrantLock` de bao ve state transition va thao tac them bid o model layer

Muc dich:

- tranh hai thread cap nhat gia cung luc ma bo sot update
- dam bao `currentPrice`, `winner`, `bids` duoc cap nhat nhat quan

### 5. synchronized va ReentrantLock

Da dap ung ca hai yeu cau:

- `synchronized`
  - [BidService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/BidService.java)
  - [AuctionManager.java](/D:/BaitaplonTest/src/main/java/com/auction/manager/AuctionManager.java)
- `ReentrantLock`
  - [Auction.java](/D:/BaitaplonTest/src/main/java/com/auction/model/auction/Auction.java)

### 6. JavaFX co ban: Login va Danh sach

Da bo sung wiring JavaFX toi thieu de app khong con dung o mot `Label`.

File moi/chinh:

- [Main.java](/D:/BaitaplonTest/src/main/java/com/auction/Main.java)
- [AppContext.java](/D:/BaitaplonTest/src/main/java/com/auction/app/AppContext.java)
- [SceneNavigator.java](/D:/BaitaplonTest/src/main/java/com/auction/app/SceneNavigator.java)
- [AuthController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AuthController.java)
- [AuctionController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AuctionController.java)
- [login-view.fxml](/D:/BaitaplonTest/src/main/resources/fxml/login-view.fxml)
- [auction-list-view.fxml](/D:/BaitaplonTest/src/main/resources/fxml/auction-list-view.fxml)
- [app.css](/D:/BaitaplonTest/src/main/resources/css/app.css)

Hien tai app co:

- man hinh `Login`
- man hinh `Auction List`
- du lieu seed san de demo
- 3 tai khoan demo:
  - `seller@auction.local`
  - `bidder@auction.local`
  - `admin@auction.local`

## Kiem thu da thuc hien

Da cap nhat va compile lai test cho flow moi:

- [AuctionTest.java](/D:/BaitaplonTest/src/test/java/com/auction/model/auction/AuctionTest.java)
- [AuctionServiceTest.java](/D:/BaitaplonTest/src/test/java/com/auction/service/AuctionServiceTest.java)
- [BidServiceTest.java](/D:/BaitaplonTest/src/test/java/com/auction/service/BidServiceTest.java)
- [ConcurrentBidTest.java](/D:/BaitaplonTest/src/test/java/com/auction/concurrency/ConcurrentBidTest.java)

Ket qua xac minh:

- `javac` compile pass cho `src/main/java`
- `javac` compile pass cho `src/test/java`
- chay JUnit Platform bang runner tam: `35/35 tests successful`

## Danh gia theo tieu chi tuan 7

1. `Observer Pattern de notify khi co bid moi`
- Dat

2. `Code logic nghiep vu: tao phien dau gia, dat gia, kiem tra hop le`
- Dat

3. `Logic chuyen trang thai OPEN -> RUNNING -> FINISHED -> PAID/CANCELED`
- Dat

4. `Xu ly dau gia dong thoi`
- Dat o muc backend core

5. `Su dung synchronized, ReentrantLock`
- Dat

6. `JavaFX co ban: Login, Danh sach`
- Dat o muc toi thieu de demo

## Gioi han hien tai

- chua co persistence that, van dung `in-memory DAO`
- login moi o muc email-based, chua co password va phan quyen day du
- man `auction detail` va `seller view` moi la placeholder
- observer hien dang demo theo dang console notification

## Tong ket

Sau dot thay doi nay, code hien tai da dap ung du bo tieu chi tuan 7 o muc phu hop de review va demo:

- co observer cho bid moi
- co state machine dung yeu cau
- co concurrent bidding voi co che dong bo ro rang
- co su dung ca `synchronized` va `ReentrantLock`
- co giao dien JavaFX co ban cho `Login` va `Auction List`
