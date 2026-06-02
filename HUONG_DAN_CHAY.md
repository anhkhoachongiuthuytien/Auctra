# Huong dan chay Auctra

## Yeu cau he thong

- **Java**: 17 tro len
- **Maven**: 3.8 tro len
- **Ket noi mang**: Can khi chay che do Socket (2 may)

---

## 1. Chay giao dien (che do LOCAL)

Day la cach don gian nhat, chay ca client va server cung 1 may.

```powershell
cd d:\BaitaplonTest
mvn install -DskipTests -q
cd auction-client
mvn javafx:run
```

Giao dien se hien ra voi man hinh dang nhap.

### Tai khoan demo

| Vai tro | Email | Mat khau |
|---------|-------|----------|
| Seller | seller@auction.local | demo12345 |
| Bidder | bidder@auction.local | demo12345 |
| Admin | admin@auction.local | demo12345 |

---

## 2. Chay che do Socket (Client-Server tren 2 may)

### Buoc 1: Build toan bo project

```powershell
cd d:\BaitaplonTest
mvn clean install -DskipTests -q
```

### Buoc 2: Chay Server (may A)

```powershell
java -jar d:\BaitaplonTest\auction-server\target\auction-server.jar
```

Ket qua:
```
=== Auctra Auction Server ===
Dang khoi tao database va services...
[Server] Dang chay tai cong 9999
```

Server se lang nghe ket noi tai cong 9999.

### Buoc 3: Chay Client (may B, hoac terminal khac)

```powershell
cd d:\BaitaplonTest\auction-client
mvn javafx:run "-Djavafx.args=--socket <IP_MAY_A> 9999"
```

Vi du: may A co IP `192.168.1.100`:
```powershell
mvn javafx:run "-Djavafx.args=--socket 192.168.1.100 9999"
```

Neu test tren cung 1 may:
```powershell
mvn javafx:run "-Djavafx.args=--socket localhost 9999"
```

> **Luu y PowerShell**: Phai boc tham so trong dau "" nhu tren.

---

## 3. Chay Unit Tests

### Chay tat ca test

```powershell
cd d:\BaitaplonTest
mvn test
```

### Chay test theo module

```powershell
# Test server (DAO, Service, Concurrency)
cd d:\BaitaplonTest\auction-server
mvn test

# Test client (ViewModel)
cd d:\BaitaplonTest\auction-client
mvn test

# Test common (Model)
cd d:\BaitaplonTest\auction-common
mvn test
```

### Chay 1 test cu the

```powershell
cd d:\BaitaplonTest\auction-server
mvn test -Dtest=AuctionServiceTest
mvn test -Dtest=BidServiceTest
mvn test -Dtest=AuthServiceTest
mvn test -Dtest=ConcurrentBidTest
```

### Danh sach test hien co

| Module | Test Class | So luong | Noi dung |
|--------|-----------|---------|----------|
| auction-common | AuctionTest | 16 test | Vong doi auction, bid, observer |
| auction-server | AuctionServiceTest | 12 test | CRUD auction qua service |
| auction-server | BidServiceTest | 8 test | Dat gia, validation |
| auction-server | AuthServiceTest | 10 test | Dang ky, dang nhap, mat khau |
| auction-server | SellerServiceTest | 4 test | Tao item, tao auction |
| auction-server | ConcurrentBidTest | 1 test | Dat gia dong thoi 2 thread |
| auction-server | InMemoryAuctionDaoTest | 3 test | CRUD in-memory |
| auction-server | InMemoryItemDaoTest | 3 test | CRUD in-memory |
| auction-server | InMemoryUserDaoTest | 4 test | CRUD in-memory |
| auction-server | SqlitePersistenceTest | 1 test | Luu/doc SQLite that |
| auction-server | AntiSnipingTest | 2 test | Thoi gian gia han tu dong |
| auction-server | AutoBidPriorityQueueTest | 1 test | Tinh toan thau tu dong |
| auction-client | FxmlLoadTest | 1 test | Kiem tra load file FXML |
| auction-client | SceneNavigatorSmokeTest | 2 test | Kiem tra dieu huong man hinh |
| auction-client | AuctionListViewModelTest | 1 test | ViewModel bid message |
| auction-client | LoginViewModelTest | 2 test | Dang ky, xac nhan MK |

**Tong: 69 tests**

---

## 4. Test Server bang tay

### Kiem tra server co chay khong

1. Mo terminal 1: chay server
2. Mo terminal 2: chay client ket noi

```powershell
# Terminal 1 - Server
java -jar d:\BaitaplonTest\auction-server\target\auction-server.jar

# Terminal 2 - Client
cd d:\BaitaplonTest\auction-client
mvn javafx:run "-Djavafx.args=--socket localhost 9999"
```

3. Khi client ket noi, terminal server se hien:
```
[Server] Client ket noi: /127.0.0.1:xxxxx
```

4. Thu dang nhap voi tai khoan demo
5. Thu tao phien dau gia (login bang seller)
6. Thu dat gia (login bang bidder o terminal khac)

### Kiem tra database

File database SQLite nam tai thu muc chay server:
```
auction-system.db
```

Xem noi dung bang SQLite CLI (neu co cai):
```powershell
sqlite3 auction-system.db
.tables
SELECT * FROM users;
SELECT * FROM auctions;
SELECT * FROM bids;
.quit
```

---

## 5. Cau truc thu muc

```
BaitaplonTest/
  auction-common/    <- Model, DAO interface, Protocol, DTO
  auction-server/    <- Server, SQLite DAO, Services, Database
  auction-client/    <- JavaFX UI, Controllers, Gateway
  DATABASE_GUIDE.md  <- Tai lieu day ve Database
  SOCKET_NETWORK_GUIDE.md  <- Tai lieu day ve Socket
  HUONG_DAN_CHAY.md  <- File nay
```

---

## 6. Xu ly loi thuong gap

| Loi | Nguyen nhan | Cach xu ly |
|-----|------------|------------|
| `Khong the ket noi toi server` | Server chua chay hoac sai IP/port | Kiem tra server dang chay, dung IP |
| `Port 9999 da duoc su dung` | Co process khac dang chiem port | Doi port: `java -jar ... 8888` |
| `BUILD FAILURE` | Chua install module common truoc | Chay `mvn install -DskipTests` tu root |
| `javafx:run` loi | Chua cai JavaFX plugin | Kiem tra pom.xml co javafx-maven-plugin |
| Ma hoa PowerShell | Dau "" bi parse sai | Boc `-Djavafx.args=...` trong `""` |
