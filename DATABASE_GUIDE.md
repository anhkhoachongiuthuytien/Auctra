# 📚 Hướng dẫn Database trong Auctra — SQLite + JDBC + DAO Pattern

> Tài liệu này **vừa dạy lý thuyết vừa giải thích code thực tế** trong dự án Auctra.
> Mỗi khái niệm đều có code minh họa trực tiếp từ project.

---

## Mục lục

1. [Tổng quan Database](#1-tổng-quan-database)
2. [SQLite là gì?](#2-sqlite-là-gì)
3. [JDBC — Cầu nối Java ↔ Database](#3-jdbc--cầu-nối-java--database)
4. [Schema — Thiết kế bảng dữ liệu](#4-schema--thiết-kế-bảng-dữ-liệu)
5. [DatabaseManager — Quản lý kết nối](#5-databasemanager--quản-lý-kết-nối)
6. [DAO Pattern — Tách biệt logic truy vấn](#6-dao-pattern--tách-biệt-logic-truy-vấn)
7. [CRUD Operations chi tiết](#7-crud-operations-chi-tiết)
8. [Transaction — Đảm bảo tính toàn vẹn](#8-transaction--đảm-bảo-tính-toàn-vẹn)
9. [Foreign Key & Cascade Delete](#9-foreign-key--cascade-delete)
10. [Migration — Nâng cấp schema an toàn](#10-migration--nâng-cấp-schema-an-toàn)
11. [Bảo mật — Password Hashing](#11-bảo-mật--password-hashing)

---

## 1. Tổng quan Database

### Database là gì?

Database (cơ sở dữ liệu) là nơi **lưu trữ dữ liệu có cấu trúc** để ứng dụng có thể đọc, ghi, sửa, xóa (CRUD).

Trong Auctra, database lưu:
- **Users** — tài khoản người dùng (Admin, Seller, Bidder)
- **Items** — vật phẩm đem đấu giá
- **Auctions** — phiên đấu giá
- **Bids** — lịch sử đặt giá

### Kiến trúc tổng thể

```
┌─────────────────────────────────────────────────┐
│                 auction-client                   │
│  (JavaFX UI — không truy cập DB trực tiếp)      │
└───────────────────────┬─────────────────────────┘
                        │ Gateway Interface
┌───────────────────────▼─────────────────────────┐
│                 auction-server                   │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ Service  │──│   DAO    │──│ DatabaseManager│  │
│  │  Layer   │  │ (SQLite) │  │   (JDBC)       │  │
│  └──────────┘  └──────────┘  └───────┬───────┘  │
└──────────────────────────────────────┼──────────┘
                                       │
                              ┌────────▼────────┐
                              │  auction-system  │
                              │      .db         │
                              │   (SQLite file)  │
                              └─────────────────┘
```

> **Nguyên tắc quan trọng:** Client KHÔNG BAO GIỜ truy cập database trực tiếp.
> Mọi thao tác DB đều qua Server → Service → DAO → DatabaseManager.

---

## 2. SQLite là gì?

### Đặc điểm

| Đặc điểm | SQLite | MySQL/PostgreSQL |
|-----------|--------|------------------|
| Kiểu | **Embedded** (nhúng) | Client-Server |
| Cài đặt | Không cần cài | Cần cài server riêng |
| File | 1 file `.db` duy nhất | Nhiều file phức tạp |
| Phù hợp | Desktop app, prototype | Web app lớn |
| Hiệu năng | Tốt cho < 100 user đồng thời | Hàng nghìn user |

### Tại sao Auctra dùng SQLite?

1. **Zero-config** — không cần cài đặt database server
2. **Portable** — chỉ 1 file `auction-system.db`, copy đi đâu cũng chạy
3. **Đủ mạnh** — cho ứng dụng desktop với vài chục user đồng thời

### Dependency Maven

```xml
<!-- Trong auction-server/pom.xml -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.50.1.0</version>
</dependency>
```

> **Thư viện `sqlite-jdbc`** tự mang theo engine SQLite bên trong file JAR.
> Java chỉ cần import thư viện là có thể dùng SQLite mà không cần cài gì thêm trên máy.

---

## 3. JDBC — Cầu nối Java ↔ Database

### JDBC là gì?

**Java Database Connectivity (JDBC)** là API tiêu chuẩn của Java để giao tiếp với database.
Nó cung cấp một bộ interface thống nhất — dù bạn dùng SQLite, MySQL, hay PostgreSQL, cách viết code gần như giống nhau.

### Các class chính trong JDBC

```
┌───────────────────────────────────────────────┐
│              java.sql Package                  │
│                                                │
│  DriverManager ──► Connection ──► Statement     │
│                                      │          │
│                                      ▼          │
│                                  ResultSet      │
│                                                │
│  PreparedStatement (an toàn hơn Statement)     │
└───────────────────────────────────────────────┘
```

| Class | Vai trò | Ví dụ trong Auctra |
|-------|---------|-------------------|
| `DriverManager` | Tạo connection từ JDBC URL | `DriverManager.getConnection(jdbcUrl)` |
| `Connection` | Đại diện 1 phiên làm việc với DB | Mỗi thao tác DAO mở 1 connection |
| `PreparedStatement` | Câu SQL có tham số `?` (chống SQL injection) | `"SELECT * FROM users WHERE email = ?"` |
| `ResultSet` | Kết quả trả về từ câu SELECT | Duyệt từng row bằng `resultSet.next()` |

### JDBC URL trong Auctra

```java
// Trong ServerContext.java
String jdbcUrl = "jdbc:sqlite:auction-system.db";
//                 ▲        ▲        ▲
//                 │        │        └── Tên file database
//                 │        └── Loại database
//                 └── Protocol JDBC
```

### Ví dụ flow thao tác database

```java
// 1. Mở connection
Connection connection = DriverManager.getConnection("jdbc:sqlite:auction-system.db");

// 2. Tạo PreparedStatement (câu SQL có tham số ?)
PreparedStatement stmt = connection.prepareStatement(
    "SELECT id, username, email FROM users WHERE email = ?"
);

// 3. Gán giá trị cho tham số ? (index bắt đầu từ 1)
stmt.setString(1, "seller@auction.local");

// 4. Thực thi và đọc kết quả
ResultSet rs = stmt.executeQuery();
while (rs.next()) {
    String id = rs.getString("id");       // Lấy theo tên cột
    String name = rs.getString("username");
    String email = rs.getString("email");
}

// 5. Đóng tất cả (quan trọng! tránh memory leak)
rs.close();
stmt.close();
connection.close();
```

### Try-with-resources (cách viết sạch)

Trong Auctra, tất cả DAO đều dùng **try-with-resources** để tự động đóng resource:

```java
// Từ SqliteUserDao.java — findById
try (Connection connection = databaseManager.getConnection();             // Tự đóng
     PreparedStatement statement = connection.prepareStatement(sql)) {    // Tự đóng
    statement.setString(1, id);
    try (ResultSet resultSet = statement.executeQuery()) {               // Tự đóng
        if (resultSet.next()) {
            return DbMappers.createUser(
                resultSet.getString("role"),
                resultSet.getString("id"),
                resultSet.getString("username"),
                resultSet.getString("email")
            );
        }
        return null;
    }
} catch (SQLException e) {
    throw new IllegalStateException("Failed to query user", e);
}
```

> **Tại sao dùng try-with-resources?**
> - Nếu quên `close()`, connection bị rò rỉ (leak), sau vài lần gọi sẽ hết resource
> - `try (...)` đảm bảo **luôn đóng** kể cả khi exception xảy ra

---

## 4. Schema — Thiết kế bảng dữ liệu

### File `schema.sql`

```
📁 auction-server/src/main/resources/db/schema.sql
```

### Sơ đồ quan hệ các bảng (ERD)

```
┌──────────────────┐         ┌──────────────────┐
│      users       │         │      items       │
├──────────────────┤         ├──────────────────┤
│ id       TEXT PK │         │ id       TEXT PK │
│ username TEXT    │         │ name     TEXT    │
│ email    TEXT UQ │         │ description TEXT │
│ role     TEXT    │         │ starting_price   │
│ password_hash    │         │ type     TEXT    │
└────────┬─────────┘         │ image_path TEXT  │
         │                   └────────┬─────────┘
         │ FK: seller_id              │ FK: item_id
         │ FK: winner_id              │
         ▼                            ▼
┌──────────────────────────────────────┐
│             auctions                  │
├──────────────────────────────────────┤
│ id            TEXT PK                 │
│ item_id       TEXT FK → items.id      │
│ seller_id     TEXT FK → users.id      │
│ current_price REAL                    │
│ status        TEXT                    │
│ winner_id     TEXT FK → users.id      │
└─────────────────┬────────────────────┘
                  │ FK: auction_id (ON DELETE CASCADE)
                  ▼
┌──────────────────────────────────────┐
│               bids                    │
├──────────────────────────────────────┤
│ id         INTEGER PK AUTOINCREMENT   │
│ auction_id TEXT FK → auctions.id      │
│ bidder_id  TEXT FK → users.id         │
│ amount     REAL                       │
│ bid_time   TEXT                       │
└──────────────────────────────────────┘
```

### Giải thích từng bảng

#### Bảng `users`
```sql
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,              -- UUID duy nhất
    username TEXT NOT NULL,            -- Tên hiển thị
    email TEXT NOT NULL UNIQUE,        -- Email đăng nhập (UNIQUE = không trùng)
    role TEXT NOT NULL,                -- "Admin", "Seller", "Bidder"
    password_hash TEXT NOT NULL DEFAULT ''  -- Mật khẩu đã hash (không lưu plaintext!)
);
```

**Điểm quan trọng:**
- `PRIMARY KEY` — mỗi user có 1 `id` duy nhất
- `UNIQUE` trên `email` — không cho phép 2 tài khoản cùng email
- `password_hash` — **KHÔNG BAO GIỜ lưu mật khẩu gốc** vào database

#### Bảng `items`
```sql
CREATE TABLE IF NOT EXISTS items (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,                -- Tên sản phẩm
    description TEXT NOT NULL,         -- Mô tả chi tiết
    starting_price REAL NOT NULL,      -- Giá khởi điểm (kiểu số thực)
    type TEXT NOT NULL,                -- "Art", "Electronics", "Vehicle"
    image_path TEXT                    -- Đường dẫn ảnh (nullable)
);
```

#### Bảng `auctions`
```sql
CREATE TABLE IF NOT EXISTS auctions (
    id TEXT PRIMARY KEY,
    item_id TEXT NOT NULL,
    seller_id TEXT NOT NULL,
    current_price REAL NOT NULL,       -- Giá cao nhất hiện tại
    status TEXT NOT NULL,              -- OPEN, RUNNING, FINISHED, PAID, CANCELED
    winner_id TEXT,                    -- Người thắng (nullable — chưa có khi mới tạo)
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (winner_id) REFERENCES users(id)
);
```

#### Bảng `bids`
```sql
CREATE TABLE IF NOT EXISTS bids (
    id INTEGER PRIMARY KEY AUTOINCREMENT,  -- Tự tăng
    auction_id TEXT NOT NULL,
    bidder_id TEXT NOT NULL,
    amount REAL NOT NULL,
    bid_time TEXT NOT NULL,                 -- Lưu dạng ISO string
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);
```

**`ON DELETE CASCADE`** — khi xóa auction, tất cả bids của auction đó cũng tự xóa theo.

---

## 5. DatabaseManager — Quản lý kết nối

File: `auction-server/.../db/DatabaseManager.java`

### Chức năng

| Method | Mô tả |
|--------|-------|
| `getConnection()` | Tạo connection mới tới SQLite file |
| `initializeSchema()` | Đọc `schema.sql` và tạo bảng nếu chưa có |
| `ensureUsersPasswordHashColumn()` | Migration — thêm cột nếu thiếu |

### Code giải thích

```java
public Connection getConnection() throws SQLException {
    // Mỗi lần gọi tạo 1 connection MỚI tới file .db
    Connection connection = DriverManager.getConnection(jdbcUrl);

    // SQLite mặc định TẮT foreign key check!
    // Phải bật thủ công trên MỖI connection
    try (Statement statement = connection.createStatement()) {
        statement.execute("PRAGMA foreign_keys = ON");
    }
    return connection;
}
```

> **⚠️ Gotcha SQLite:** Foreign key constraints bị TẮT mặc định.
> Nếu không chạy `PRAGMA foreign_keys = ON`, bạn có thể INSERT dữ liệu
> vi phạm quan hệ mà SQLite không báo lỗi!

### Khởi tạo schema

```java
public void initializeSchema() {
    // 1. Đọc file schema.sql từ resources
    String schemaSql = loadSchemaSql();

    // 2. Tách thành từng câu lệnh theo dấu ;
    for (String sql : schemaSql.split(";")) {
        String trimmed = sql.trim();
        if (!trimmed.isEmpty()) {
            statement.execute(trimmed);  // Chạy CREATE TABLE IF NOT EXISTS
        }
    }

    // 3. Migration: thêm cột mới nếu cần
    ensureUsersPasswordHashColumn(connection);
    ensureItemsImagePathColumn(connection);
}
```

---

## 6. DAO Pattern — Tách biệt logic truy vấn

### DAO là gì?

**Data Access Object (DAO)** là design pattern tách biệt:
- **Business logic** (cách hệ thống hoạt động)
- **Data access logic** (cách đọc/ghi database)

### Cấu trúc trong Auctra

```
auction-common (interface)          auction-server (implementation)
├── dao/                            ├── dao/sqlite/
│   ├── UserDao.java       ◄────── │   ├── SqliteUserDao.java
│   ├── ItemDao.java       ◄────── │   ├── SqliteItemDao.java
│   └── AuctionDao.java    ◄────── │   └── SqliteAuctionDao.java
```

### Interface (trong auction-common)

```java
public interface UserDao {
    void save(User user);                       // Create/Update
    void save(User user, String passwordHash);  // Create with password
    User findById(String id);                   // Read by ID
    User findByEmail(String email);             // Read by Email
    List<User> findAll();                       // Read all
    String findPasswordHashByEmail(String email);
    void updatePasswordHash(String email, String hash);
}
```

### Implementation (trong auction-server)

```java
public class SqliteUserDao implements UserDao {
    private final DatabaseManager databaseManager;

    // Inject DatabaseManager qua constructor
    public SqliteUserDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public User findByEmail(String email) {
        return findSingle(
            "SELECT id, username, email, role FROM users WHERE email = ?",
            email
        );
    }
    // ... implementation chi tiết phía dưới
}
```

### Tại sao tách Interface & Implementation?

1. **Dễ thay database:** Muốn chuyển sang MySQL? Chỉ cần viết `MysqlUserDao implements UserDao`
2. **Dễ test:** Mock `UserDao` interface trong unit test
3. **Clean architecture:** Service layer chỉ biết interface, không biết SQLite

---

## 7. CRUD Operations chi tiết

### CREATE — Thêm mới (INSERT)

```java
// SqliteUserDao.save() — Upsert pattern
String sql = """
    INSERT INTO users(id, username, email, role, password_hash)
    VALUES (?, ?, ?, ?, ?)
    ON CONFLICT(id) DO UPDATE SET
        username = excluded.username,
        email = excluded.email,
        role = excluded.role,
        password_hash = excluded.password_hash
    """;
```

> **Upsert = INSERT + UPDATE:** Nếu `id` đã tồn tại → UPDATE thay vì lỗi.
> `ON CONFLICT(id) DO UPDATE` là cú pháp đặc biệt của SQLite.

### READ — Đọc dữ liệu (SELECT)

```java
// Đọc 1 record
public User findById(String id) {
    String sql = "SELECT id, username, email, role FROM users WHERE id = ?";
    // ... PreparedStatement, setString, executeQuery
}

// Đọc nhiều records
public List<User> findAll() {
    String sql = "SELECT id, username, email, role FROM users ORDER BY username";
    List<User> users = new ArrayList<>();
    // ... duyệt ResultSet bằng while(resultSet.next())
}
```

### UPDATE — Cập nhật

```java
// SqliteUserDao.updatePasswordHash()
String sql = "UPDATE users SET password_hash = ? WHERE email = ?";
statement.setString(1, passwordHash);  // ? thứ 1 = giá trị mới
statement.setString(2, email);         // ? thứ 2 = điều kiện WHERE
statement.executeUpdate();
```

### DELETE — Xóa

```java
// SqliteAuctionDao.delete()
String sql = "DELETE FROM auctions WHERE id = ?";
statement.setString(1, id);
statement.executeUpdate();
// Bids liên quan tự xóa nhờ ON DELETE CASCADE
```

---

## 8. Transaction — Đảm bảo tính toàn vẹn

### Vấn đề

Khi lưu 1 Auction, ta cần ghi cả auction VÀ danh sách bids.
Nếu ghi auction thành công nhưng ghi bids bị lỗi → **dữ liệu mất đồng bộ!**

### Giải pháp: Transaction

```java
// SqliteAuctionDao.save() — Transaction example
try (Connection connection = databaseManager.getConnection()) {
    connection.setAutoCommit(false);  // ← BẮT ĐẦU transaction

    try {
        // Bước 1: Upsert auction
        try (PreparedStatement stmt = connection.prepareStatement(auctionSql)) {
            // ... set params, executeUpdate
        }

        // Bước 2: Xóa bids cũ
        try (PreparedStatement deleteBids = connection.prepareStatement(
                "DELETE FROM bids WHERE auction_id = ?")) {
            deleteBids.setString(1, auction.getId());
            deleteBids.executeUpdate();
        }

        // Bước 3: Ghi bids mới (batch insert)
        try (PreparedStatement insertBid = connection.prepareStatement(insertBidSql)) {
            for (BidTransaction bid : auction.getBids()) {
                insertBid.setString(1, auction.getId());
                insertBid.setString(2, bid.getBidder().getId());
                insertBid.setDouble(3, bid.getAmount());
                insertBid.setString(4, bid.getBidTime().toString());
                insertBid.addBatch();   // ← Thêm vào batch
            }
            insertBid.executeBatch();   // ← Chạy tất cả cùng lúc
        }

        connection.commit();  // ← TẤT CẢ thành công → COMMIT
    } catch (Exception e) {
        connection.rollback();  // ← Có lỗi → ROLLBACK (hủy hết)
        throw e;
    }
}
```

### Nguyên tắc ACID

| Tính chất | Ý nghĩa | Trong Auctra |
|-----------|---------|--------------|
| **Atomicity** | Tất cả hoặc không gì cả | Auction + Bids cùng commit/rollback |
| **Consistency** | Dữ liệu luôn hợp lệ | Foreign key constraints |
| **Isolation** | Transaction không ảnh hưởng nhau | SQLite lock toàn file |
| **Durability** | Sau commit, dữ liệu không mất | Ghi xuống file .db |

### Batch Insert — Tối ưu hiệu năng

```java
// Thay vì:
for (BidTransaction bid : bids) {
    statement.setXxx(...);
    statement.executeUpdate();  // ← N lần execute = chậm
}

// Dùng batch:
for (BidTransaction bid : bids) {
    statement.setXxx(...);
    statement.addBatch();       // ← Chỉ thêm vào queue
}
statement.executeBatch();       // ← 1 lần execute duy nhất = nhanh
```

---

## 9. Foreign Key & Cascade Delete

### Foreign Key là gì?

Foreign Key (khóa ngoại) là ràng buộc đảm bảo **dữ liệu tham chiếu phải tồn tại**.

```sql
-- Ví dụ: auction phải có seller hợp lệ
FOREIGN KEY (seller_id) REFERENCES users(id)
-- → Không thể INSERT auction với seller_id không có trong bảng users
```

### Cascade Delete

```sql
FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
```

Khi xóa 1 auction → **tất cả bids của auction đó tự động bị xóa theo**.

Nếu không có CASCADE, bạn phải tự xóa bids trước rồi mới xóa auction được.

---

## 10. Migration — Nâng cấp schema an toàn

Khi cần thêm cột mới vào bảng đã tồn tại (ví dụ: thêm `image_path` cho items):

```java
// DatabaseManager.ensureItemsImagePathColumn()
private void ensureItemsImagePathColumn(Connection connection) throws SQLException {
    // 1. Kiểm tra cột đã tồn tại chưa
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery("PRAGMA table_info(items)")) {
        while (rs.next()) {
            if ("image_path".equalsIgnoreCase(rs.getString("name"))) {
                return;  // Đã có rồi → bỏ qua
            }
        }
    }

    // 2. Thêm cột mới nếu chưa có
    try (Statement stmt = connection.createStatement()) {
        stmt.execute("ALTER TABLE items ADD COLUMN image_path TEXT");
    }
}
```

> **`PRAGMA table_info()`** là lệnh đặc biệt của SQLite để xem cấu trúc bảng.
> Đây là cách thực hiện migration đơn giản cho SQLite.

---

## 11. Bảo mật — Password Hashing (chi tiết)

### Tại sao KHÔNG BAO GIỜ lưu mật khẩu gốc?

```
❌ Database bị hack → attacker thấy: password_hash = "demo12345"
   → Attacker biết ngay mật khẩu → truy cập tài khoản

✅ Database bị hack → attacker thấy: password_hash = "65536:a8F3k....:x9Bm2...."
   → Attacker KHÔNG THỂ đảo ngược → tài khoản vẫn an toàn
```

### Thuật toán: PBKDF2WithHmacSHA256

Auctra dùng **PBKDF2** (Password-Based Key Derivation Function 2) — một thuật toán hash mật khẩu chuẩn công nghiệp.

File: `auction-common/.../util/PasswordHasher.java`

#### Các hằng số cấu hình

```java
public final class PasswordHasher {
    private static final int ITERATIONS  = 65_536;  // Lặp 65536 lần (chống brute-force)
    private static final int KEY_LENGTH  = 256;     // Độ dài hash output: 256 bit
    private static final int SALT_LENGTH = 16;      // Salt ngẫu nhiên: 16 bytes
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
}
```

| Tham số | Giá trị | Ý nghĩa |
|---------|---------|---------|
| `ITERATIONS` | 65,536 | Số lần lặp hash. Càng nhiều → càng chậm brute-force. 65K là mức an toàn |
| `KEY_LENGTH` | 256 bit | Độ dài hash output. 256 bit = gần như không thể đoán |
| `SALT_LENGTH` | 16 bytes | Dữ liệu ngẫu nhiên thêm vào mật khẩu trước khi hash |
| `ALGORITHM` | PBKDF2+HMAC+SHA256 | Thuật toán kết hợp: PBKDF2 dùng HMAC-SHA256 bên trong |

### Salt là gì? Tại sao cần Salt?

**Salt** là chuỗi bytes ngẫu nhiên, được **tạo mới mỗi lần** hash mật khẩu.

```
Không có salt:
  hash("demo12345") → "abc123..."     ← Ai dùng "demo12345" đều ra giống nhau!
  → Attacker dùng bảng tra cứu (rainbow table) để dò ngược

Có salt (16 bytes ngẫu nhiên):
  hash("demo12345" + salt_1) → "x9Bm2..."   ← User A
  hash("demo12345" + salt_2) → "k3Hn7..."   ← User B
  → Cùng mật khẩu nhưng hash khác nhau! Rainbow table vô dụng.
```

### Hàm `hash()` — Tạo hash khi đăng ký

```java
public static String hash(String rawPassword) {
    // Bước 1: Tạo salt ngẫu nhiên (16 bytes)
    byte[] salt = new byte[SALT_LENGTH];
    new SecureRandom().nextBytes(salt);
    //  ↑ SecureRandom = bộ sinh số ngẫu nhiên an toàn (không đoán được)

    // Bước 2: Hash mật khẩu với salt
    byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
    //                    ↑ mật khẩu gốc      ↑ salt   ↑ 65536 lần  ↑ 256 bit

    // Bước 3: Ghép thành chuỗi lưu vào database
    return ITERATIONS                                  // "65536"
            + ":"                                      // ":"
            + Base64.getEncoder().encodeToString(salt)  // "a8F3kQ2p..."
            + ":"                                      // ":"
            + Base64.getEncoder().encodeToString(hash); // "x9Bm2nH4..."
}
// Kết quả: "65536:a8F3kQ2p...:x9Bm2nH4..."
```

#### Format lưu trong database

```
65536:a8F3kQ2pLmN0xYz=:x9Bm2nH4qR7wKfJ3sD5tG=
  ↑         ↑                    ↑
  │         │                    └── Hash result (Base64)
  │         └── Salt ngẫu nhiên (Base64)
  └── Số lần lặp (iterations)
```

### Hàm `matches()` — Kiểm tra khi đăng nhập

```java
public static boolean matches(String rawPassword, String storedHash) {
    // Bước 1: Kiểm tra hash có hợp lệ không
    if (storedHash == null || storedHash.isBlank()) {
        return false;
    }

    // Bước 2: Tách chuỗi "65536:salt_base64:hash_base64" thành 3 phần
    String[] parts = storedHash.split(":");
    if (parts.length != 3) {
        throw new IllegalStateException("Stored password hash has invalid format");
    }

    int iterations     = Integer.parseInt(parts[0]);          // 65536
    byte[] salt         = Base64.getDecoder().decode(parts[1]); // salt gốc
    byte[] expectedHash = Base64.getDecoder().decode(parts[2]); // hash đã lưu

    // Bước 3: Hash mật khẩu user vừa nhập với CÙNG salt và iterations
    byte[] actualHash = pbkdf2(
        rawPassword.toCharArray(),  // Mật khẩu user nhập
        salt,                       // Salt lấy từ database (KHÔNG tạo mới!)
        iterations,                 // Số lần lặp giống lúc đăng ký
        expectedHash.length * 8     // Cùng độ dài output
    );

    // Bước 4: So sánh an toàn (constant-time)
    return constantTimeEquals(expectedHash, actualHash);
}
```

### Constant-Time Comparison — Chống timing attack

```java
private static boolean constantTimeEquals(byte[] left, byte[] right) {
    if (left.length != right.length) {
        return false;
    }

    int result = 0;
    for (int i = 0; i < left.length; i++) {
        result |= left[i] ^ right[i];  // XOR từng byte
    }
    return result == 0;  // result == 0 ↔ tất cả bytes đều giống nhau
}
```

**Tại sao không dùng `Arrays.equals()`?**

```java
// ❌ Arrays.equals() — KHÔNG AN TOÀN
// Dừng ngay khi gặp byte khác → attacker đo thời gian để đoán từng byte
if (left[0] != right[0]) return false;  // Sai byte đầu → trả về nhanh
if (left[0] == right[0] && left[1] != right[1]) return false;  // Sai byte 2 → chậm hơn
// → Attacker biết byte đầu đúng rồi, chỉ cần đoán byte tiếp

// ✅ constantTimeEquals() — AN TOÀN
// LUÔN duyệt HẾT tất cả bytes, dù đúng hay sai
// → Thời gian chạy giống nhau → attacker không đoán được gì
```

### Flow hoàn chỉnh: Đăng ký → Đăng nhập

```
                    ĐĂNG KÝ (Register)
                    ═══════════════════

User nhập: password = "demo12345"
    │
    ▼
SecureRandom → salt = [random 16 bytes]
    │
    ▼
PBKDF2("demo12345", salt, 65536 lần, 256 bit) → hash = [32 bytes]
    │
    ▼
Ghép: "65536" + ":" + Base64(salt) + ":" + Base64(hash)
    │
    ▼
Lưu vào DB: password_hash = "65536:a8F3kQ2p...:x9Bm2nH4..."


                    ĐĂNG NHẬP (Login)
                    ═════════════════

User nhập: password = "demo12345"
    │
    ▼
Đọc DB: storedHash = "65536:a8F3kQ2p...:x9Bm2nH4..."
    │
    ▼
Tách: iterations=65536, salt=a8F3kQ2p..., expectedHash=x9Bm2nH4...
    │
    ▼
PBKDF2("demo12345", salt_từ_DB, 65536 lần, 256 bit) → actualHash
    │
    ▼
constantTimeEquals(expectedHash, actualHash)
    │
    ├── true  → Login thành công ✅
    └── false → "Mật khẩu không đúng" ❌
```

### AuthService — Logic xác thực đầy đủ

File: `auction-server/.../service/AuthService.java`

#### Đăng ký (Register)

```java
public Seller registerSeller(String username, String email, String password) {
    validateUserInput(username, email, password);  // Kiểm tra input
    validateEmailNotExists(email);                  // Email chưa có trong DB

    Seller seller = new Seller(IdGenerator.generateId(), username, email);
    userDao.save(seller, PasswordHasher.hash(password));
    //                    ↑ Hash mật khẩu rồi mới lưu vào DB
    return seller;
}
```

#### Đăng nhập (Login)

```java
public User login(String email, String password) {
    // 1. Validate input
    if (email == null || email.trim().isEmpty()) {
        throw new ValidationException("Email không được để trống");
    }
    validatePassword(password);

    // 2. Tìm user theo email
    User user = userDao.findByEmail(email);
    if (user == null) {
        throw new AuthenticationException("Email chưa được đăng ký");
    }

    // 3. Lấy hash từ DB và so sánh
    String passwordHash = userDao.findPasswordHashByEmail(email);
    if (!PasswordHasher.matches(password, passwordHash)) {
        throw new AuthenticationException("Mật khẩu không đúng");
    }

    return user;  // Login thành công
}
```

#### Đổi mật khẩu (Reset Password)

```java
public void resetPassword(String email, String username, String newPassword) {
    // Vì app không có email service → user chứng minh quyền sở hữu
    // bằng cách cung cấp đúng username khớp với email

    validatePassword(newPassword);  // Kiểm tra >= 8 ký tự

    User user = userDao.findByEmail(email);
    if (user == null) {
        throw new AuthenticationException("Email chưa được đăng ký");
    }
    if (!user.getUsername().equals(username.trim())) {
        throw new AuthenticationException("Tên đăng nhập không khớp với email này");
    }

    // Hash mật khẩu mới và cập nhật vào DB
    userDao.updatePasswordHash(email, PasswordHasher.hash(newPassword));
}
```

### Validation — Quy tắc kiểm tra mật khẩu

```java
private static final int MIN_PASSWORD_LENGTH = 8;

private void validatePassword(String password) {
    if (password == null || password.isBlank()) {
        throw new ValidationException("Mật khẩu không được để trống");
    }
    if (password.length() < MIN_PASSWORD_LENGTH) {
        throw new ValidationException("Mật khẩu phải có ít nhất 8 ký tự");
    }
}
```

### `ensurePassword()` — Đảm bảo tài khoản demo có mật khẩu

```java
// Khi seed dữ liệu demo, nếu tài khoản đã tồn tại nhưng chưa có password:
public void ensurePassword(String email, String password) {
    if (!emailExists(email)) {
        throw new AuthenticationException("Email chưa được đăng ký");
    }
    if (!hasPassword(email)) {             // Chưa có password_hash
        validatePassword(password);
        userDao.updatePasswordHash(email, PasswordHasher.hash(password));
    }
    // Nếu đã có password → không ghi đè (giữ nguyên password cũ)
}
```

### Tóm tắt bảo mật mật khẩu

| Bước | Phương thức | Mục đích |
|------|------------|---------|
| Hash | `PasswordHasher.hash()` | Chuyển plaintext → hash không đảo ngược |
| Salt | `SecureRandom` 16 bytes | Mỗi user có salt riêng, chống rainbow table |
| Iterations | 65,536 lần PBKDF2 | Làm chậm brute-force (mỗi lần thử ~100ms) |
| So sánh | `constantTimeEquals()` | Chống timing attack |
| Validate | `validatePassword()` | Đảm bảo >= 8 ký tự |
| Lưu trữ | `password_hash` column | Format: `iterations:salt_b64:hash_b64` |

---

## Tổng kết

| Khái niệm | Trong Auctra | File chính |
|-----------|-------------|------------|
| Database Engine | SQLite (embedded) | `auction-system.db` |
| JDBC Driver | `sqlite-jdbc` | `pom.xml` |
| Connection | `DatabaseManager.getConnection()` | `DatabaseManager.java` |
| Schema | 4 bảng (users, items, auctions, bids) | `schema.sql` |
| DAO Pattern | Interface (common) + Impl (server) | `*Dao.java` |
| Transaction | `setAutoCommit(false)` + `commit()` | `SqliteAuctionDao.save()` |
| Migration | `PRAGMA table_info` + `ALTER TABLE` | `DatabaseManager.java` |
| Password Hash | PBKDF2WithHmacSHA256 + Salt | `PasswordHasher.java` |
| Auth Logic | Login, Register, Reset | `AuthService.java` |
