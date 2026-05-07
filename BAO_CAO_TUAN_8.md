# Bao Cao Tuan 8: Kiem thu, Ngoai le va GUI nang cao

## Muc tieu tuan 8

Theo tieu chi cua tuan 8, he thong can:

- co custom exceptions ro rang
- xu ly ngoai le cho dat gia thap, dau gia khi phien dong, loi du lieu
- co unit test JUnit cho logic chinh
- refactor code theo huong sach hon, giam code smell, ap dung SOLID/MVC ro hon
- tiep tuc hoan thien GUI JavaFX va tach logic khoi controller

## Thay doi da thuc hien

### 1. Bo sung va chuan hoa exception

Da giu cac exception da co:

- [InvalidBidException.java](/D:/BaitaplonTest/src/main/java/com/auction/exception/InvalidBidException.java)
- [AuctionClosedException.java](/D:/BaitaplonTest/src/main/java/com/auction/exception/AuctionClosedException.java)
- [AuthenticationException.java](/D:/BaitaplonTest/src/main/java/com/auction/exception/AuthenticationException.java)

Da bo sung:

- [ValidationException.java](/D:/BaitaplonTest/src/main/java/com/auction/exception/ValidationException.java)

`ValidationException` duoc dung cho cac loi du lieu dau vao:

- email rong
- username rong
- bidder null
- auction id rong
- gia bid <= 0
- item name/description rong
- starting price <= 0

### 2. Tang cuong xu ly ngoai le o service layer

Da cap nhat:

- [AuthService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/AuthService.java)
- [BidService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/BidService.java)
- [SellerService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/SellerService.java)

Noi dung chinh:

- `AuthService` kiem tra username/email rong truoc khi register va login
- `BidService` kiem tra:
  - `auctionId` khong rong
  - `bidder` khong null
  - `amount` hop le va > 0
- `SellerService` kiem tra:
  - `ItemType` khong null
  - `name` khong rong
  - `description` khong rong
  - `startingPrice` > 0

### 3. Refactor GUI theo huong MVC ro hon

Tuan 7 da co FXML va controller co ban, nhung controller van chua tach logic ro rang. Tuan 8 da refactor them mot tang `presentation`.

Da bo sung:

- [LoginViewModel.java](/D:/BaitaplonTest/src/main/java/com/auction/presentation/LoginViewModel.java)
- [AuctionListViewModel.java](/D:/BaitaplonTest/src/main/java/com/auction/presentation/AuctionListViewModel.java)

Controller duoc lam mong hon:

- [AuthController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AuthController.java)
- [AuctionController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AuctionController.java)

Sau refactor:

- controller chu yeu doc input tu view va cap nhat message len UI
- `viewmodel` xu ly login, tai danh sach, dat gia va finish auction
- cac message loi duoc tra ve co cau truc qua `result object`

### 4. Nang cap GUI JavaFX

Da cap nhat:

- [login-view.fxml](/D:/BaitaplonTest/src/main/resources/fxml/login-view.fxml)
- [auction-list-view.fxml](/D:/BaitaplonTest/src/main/resources/fxml/auction-list-view.fxml)

GUI hien tai co:

- login bang email
- hien message loi/ket qua tren UI
- danh sach auction
- nhap `bid amount`
- nut `Place Bid`
- nut `Finish Auction`
- `Refresh`
- `Back to Login`

Hanh vi UI:

- account `Bidder` duoc dat gia
- account khac se bi khoa nut bid
- nut `Finish Auction` duoc dung cho account khong phai bidder
- ngoai le tu service duoc hien thi thanh message tren man hinh

### 5. Mo rong unit test

Da bo sung va hoan thien cac file test:

- [AuthServiceTest.java](/D:/BaitaplonTest/src/test/java/com/auction/service/AuthServiceTest.java)
- [SellerServiceTest.java](/D:/BaitaplonTest/src/test/java/com/auction/service/SellerServiceTest.java)
- [BidServiceTest.java](/D:/BaitaplonTest/src/test/java/com/auction/service/BidServiceTest.java)
- [InMemoryUserDaoTest.java](/D:/BaitaplonTest/src/test/java/com/auction/dao/InMemoryUserDaoTest.java)
- [InMemoryItemDaoTest.java](/D:/BaitaplonTest/src/test/java/com/auction/dao/InMemoryItemDaoTest.java)
- [InMemoryAuctionDaoTest.java](/D:/BaitaplonTest/src/test/java/com/auction/dao/InMemoryAuctionDaoTest.java)

Ngoai ra, cac test tu tuan 7 van duoc giu va tiep tuc pass:

- [AuctionTest.java](/D:/BaitaplonTest/src/test/java/com/auction/model/auction/AuctionTest.java)
- [AuctionServiceTest.java](/D:/BaitaplonTest/src/test/java/com/auction/service/AuctionServiceTest.java)
- [ConcurrentBidTest.java](/D:/BaitaplonTest/src/test/java/com/auction/concurrency/ConcurrentBidTest.java)

### 6. Ket qua kiem thu

Da xac minh bang Maven:

```powershell
mvn test
```

Ket qua:

- `Tests run: 59`
- `Failures: 0`
- `Errors: 0`
- `BUILD SUCCESS`

## Danh gia theo tieu chi tuan 8

1. `Tao custom exceptions`
- Dat

2. `Xu ly ngoai le cho dat gia thap hon hien tai, dau gia khi phien dong, loi du lieu`
- Dat

3. `Viet unit test JUnit cho logic dau gia`
- Dat

4. `Refactor code, loai bo code smell, ap dung SOLID`
- Dat o muc phu hop voi cau truc hien tai

5. `[Tu hoc] Hoan thien GUI JavaFX, ap dung MVC, tach logic khoi Controller, dung FXML`
- Dat o muc tot hon tuan 7

## Gioi han hien tai

- chua co password that, login van theo email
- chua co persistence database that
- `BidController` va `SellerController` van chua duoc mo rong
- `auction detail` va `seller view` van chua phat trien day du

## Tong ket

Sau tuan 8, project da tien mot buoc ro rang ve chat luong code:

- validation va exception ro hon
- test suite day hon
- GUI da co xu ly hanh dong va loi that
- controller da mong hon nhờ tach logic sang `presentation/viewmodel`

Trang thai hien tai phu hop de demo cho noi dung:

- testing
- exception handling
- refactor
- JavaFX MVC co ban
