# BAO CAO DATABASE

## 1. Muc tieu

Bo sung tang luu tru du lieu that cho he thong dau gia de thay the cach luu tam bang bo nho. Phan nay phuc vu muc tieu cham tien do: co thiet ke database, co lop ket noi giua ung dung va database, va mo ta duoc luong `client -> service -> database`.

## 2. Lua chon cong nghe

- Database duoc chon: `SQLite`
- Thu vien JDBC: `org.xerial:sqlite-jdbc:3.50.1.0`

Ly do chon SQLite:

- khong can cai dat server database rieng
- de demo va de nop bai tien do
- phu hop voi cau truc Java hien tai dung DAO
- co the nang cap len MySQL/PostgreSQL sau ma khong can doi service layer qua nhieu

## 3. Cau truc database

Schema duoc dat tai [schema.sql](D:/BaitaplonTest/src/main/resources/db/schema.sql).

### Bang `users`

- `id`: khoa chinh
- `username`: ten hien thi
- `email`: duy nhat
- `role`: vai tro `SELLER`, `BIDDER`, `ADMIN`

### Bang `items`

- `id`: khoa chinh
- `name`: ten vat pham
- `description`: mo ta
- `starting_price`: gia khoi diem
- `type`: loai vat pham `ART`, `VEHICLE`, `ELECTRONICS`

### Bang `auctions`

- `id`: khoa chinh
- `item_id`: khoa ngoai sang `items`
- `seller_id`: khoa ngoai sang `users`
- `current_price`: gia hien tai
- `status`: trang thai phien dau gia
- `winner_id`: khoa ngoai sang `users`, cho phep `NULL`

### Bang `bids`

- `id`: khoa chinh tu tang
- `auction_id`: khoa ngoai sang `auctions`
- `bidder_id`: khoa ngoai sang `users`
- `amount`: muc gia dat
- `bid_time`: thoi diem dat gia

## 4. Tang ket noi database

Da bo sung cac thanh phan sau:

- [DatabaseManager.java](D:/BaitaplonTest/src/main/java/com/auction/db/DatabaseManager.java): tao ket noi JDBC, bat foreign key, khoi tao schema
- [DbMappers.java](D:/BaitaplonTest/src/main/java/com/auction/db/DbMappers.java): map du lieu giua object Java va gia tri luu trong bang
- [SqliteUserDao.java](D:/BaitaplonTest/src/main/java/com/auction/dao/sqlite/SqliteUserDao.java)
- [SqliteItemDao.java](D:/BaitaplonTest/src/main/java/com/auction/dao/sqlite/SqliteItemDao.java)
- [SqliteAuctionDao.java](D:/BaitaplonTest/src/main/java/com/auction/dao/sqlite/SqliteAuctionDao.java)

Cac DAO nay giu nguyen interface hien co, nen service layer khong bi thay doi lon. Day la diem quan trong de giu code de mo rong va de chuyen doi giua `in-memory` va `database-backed` implementation.

## 5. Thay doi trong kien truc he thong

Kien truc hien tai duoc tach thanh 3 tang:

1. `Client`
   - JavaFX GUI
   - nguoi dung thao tac tren man hinh `Login` va `Auction List`

2. `Server / Business layer`
   - `AuthService`, `AuctionService`, `BidService`, `SellerService`
   - xu ly nghiep vu, validation, exception, concurrency

3. `Database layer`
   - `DatabaseManager`
   - cac `Sqlite*Dao`
   - schema va du lieu SQLite

Luong xu ly tong quat:

`JavaFX Controller -> ViewModel -> Service -> DAO -> SQLite`

Phan khoi tao trung tam duoc cau hinh trong [AppContext.java](D:/BaitaplonTest/src/main/java/com/auction/app/AppContext.java). File nay hien dang:

- tao `DatabaseManager("jdbc:sqlite:auction-system.db")`
- khoi tao schema neu chua ton tai
- gan `SqliteUserDao`, `SqliteItemDao`, `SqliteAuctionDao` vao cac service
- seed du lieu demo neu database chua co auction

## 6. Du lieu demo

He thong hien tai tu dong tao cac tai khoan va auction mau khi database trong:

- `seller@auction.local`
- `bidder@auction.local`
- `admin@auction.local`

Du lieu nay giup demo nhanh cac luong:

- dang nhap
- xem danh sach auction
- dat gia
- ket thuc phien
- luu lai lich su bid sau khi tat app

## 7. Dieu chinh model va service de ho tro persistence

De luu va phuc hoi du lieu day du, da bo sung:

- [Auction.java](D:/BaitaplonTest/src/main/java/com/auction/model/auction/Auction.java)
  - them `restoreState(...)` de nap lai trang thai, winner va lich su bid tu database
  - them `global observer` de giu co che notify khi app tai lai doi tuong auction
- [BidTransaction.java](D:/BaitaplonTest/src/main/java/com/auction/model/auction/BidTransaction.java)
  - them constructor co `bidTime` de phuc hoi bid tu du lieu da luu
- [AuctionService.java](D:/BaitaplonTest/src/main/java/com/auction/service/AuctionService.java)
  - sau moi thay doi trang thai se goi `auctionDao.save(...)`
- [BidService.java](D:/BaitaplonTest/src/main/java/com/auction/service/BidService.java)
  - sau khi dat gia hop le se luu lai auction va bids xuong database

## 8. Kiem thu va xac minh

Da bo sung integration test:

- [SqlitePersistenceTest.java](D:/BaitaplonTest/src/test/java/com/auction/db/SqlitePersistenceTest.java)

Noi dung test:

- tao database tam
- khoi tao schema
- luu seller, bidder, item, auction
- start auction
- dat bid
- load lai auction tu SQLite
- kiem tra gia hien tai, winner va lich su bid

Ket qua xac minh:

- lenh `mvn test` da chay thanh cong
- tong cong `60 tests`
- `0 failures`, `0 errors`

## 9. Danh gia hien trang

Phan database hien da dat muc tieu tien do:

- co schema ro rang
- co tang DAO su dung database that
- co luong ket noi tu GUI den database
- co test xac minh persistence

Nhung van con cac huong mo rong tiep theo:

- bo sung migration/versioning cho schema
- tach server thanh process rieng neu can dung mo hinh client-server that
- them polling hoac socket de ho tro realtime update
- them man hinh tao item, tao auction va chi tiet auction day du hon

## 10. Ket luan

Project da duoc nang cap tu mo hinh `in-memory` sang mo hinh co `SQLite database`. Cau truc hien tai phu hop de demo tien do bai tap lon, dong thoi giu duoc kha nang mo rong cho cac tuan sau ma khong can viet lai toan bo service layer.
