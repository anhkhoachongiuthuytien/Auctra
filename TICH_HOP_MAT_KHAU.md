# Tích Hợp Mật Khẩu Vào Dự Án

## Mục tiêu

Bổ sung xác thực bằng mật khẩu cho hệ thống thay vì chỉ login bằng email như trước.  
Yêu cầu của phần này là:

- không lưu mật khẩu gốc trong database
- có thể đăng ký và đăng nhập bằng `email + password`
- dữ liệu demo cũ vẫn dùng tiếp được
- không làm hỏng build, test và Checkstyle hiện có

## Những gì đã thay đổi

### 0. Từ demo login sang auth flow thật

Trước đây:
- app chỉ có màn `login`
- người dùng chủ yếu dùng các tài khoản demo seed sẵn
- chưa có luồng tạo tài khoản thật trên giao diện

Hiện tại:
- màn [login-view.fxml](/D:/BaitaplonTest/src/main/resources/fxml/login-view.fxml) đã có cả `Login` và `Register`
- người dùng có thể tự tạo tài khoản mới ngay trên app
- role được hỗ trợ khi đăng ký:
  - `Bidder`
  - `Seller`

Luồng mới:

1. nhập `username`, `email`, `password`, `confirm password`
2. chọn loại tài khoản
3. bấm `Create Account`
4. hệ thống lưu user mới xuống SQLite
5. điều hướng ngay sang dashboard phù hợp theo role

### 1. Thay đổi schema database

Đã thêm cột `password_hash` vào bảng `users`.

File:
- [schema.sql](/D:/BaitaplonTest/src/main/resources/db/schema.sql)

Thiết kế hiện tại:

```sql
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    role TEXT NOT NULL,
    password_hash TEXT NOT NULL DEFAULT ''
);
```

Lý do dùng `DEFAULT ''`:
- để các luồng cũ vẫn có thể `save(user)` mà chưa truyền password hash
- giúp tránh lỗi với các test/persistence flow không đi qua `AuthService`

### 2. Kỹ thuật hash mật khẩu

Đã thêm utility:
- [PasswordHasher.java](/D:/BaitaplonTest/src/main/java/com/auction/util/PasswordHasher.java)

Thuật toán sử dụng:
- `PBKDF2WithHmacSHA256`

Lý do chọn:
- có sẵn trong JDK, không cần thêm thư viện ngoài
- tốt hơn nhiều so với lưu plain text
- phù hợp cho đồ án và dễ giải thích trong vấn đáp

Thông số kỹ thuật:
- `salt` ngẫu nhiên: `16 bytes`
- số vòng lặp: `65536`
- độ dài key: `256 bits`

Định dạng chuỗi lưu xuống DB:

```text
iterations:base64(salt):base64(hash)
```

Ví dụ ý tưởng:

```text
65536:QmFzZTY0U2FsdA==:QmFzZTY0SGFzaA==
```

Điều này cho phép:
- lưu cả cấu hình cần thiết cùng với hash
- verify password về sau mà không cần giữ mật khẩu gốc

### 3. Cách verify password

Khi user login:

1. nhập `email` và `password`
2. `AuthService` tìm user theo email
3. đọc `password_hash` từ DB
4. tách `iterations`, `salt`, `hash`
5. hash lại mật khẩu vừa nhập bằng cùng cấu hình
6. so sánh hash mới với hash đã lưu

Nếu trùng:
- login thành công

Nếu không trùng:
- báo `Incorrect password`

## Các file đã sửa

### Tầng service

- [AuthService.java](/D:/BaitaplonTest/src/main/java/com/auction/service/AuthService.java)

Đã thay đổi:
- `registerSeller(username, email, password)`
- `registerBidder(username, email, password)`
- `registerAdmin(username, email, password)`
- `login(email, password)`

Đã thêm:
- validate password không rỗng
- validate password tối thiểu `8 ký tự`
- `ensurePassword(email, password)` để bootstrap mật khẩu cho dữ liệu demo cũ
- `hasPassword(email)` để kiểm tra user đã có password hash hay chưa

### Tầng DAO

- [UserDao.java](/D:/BaitaplonTest/src/main/java/com/auction/dao/UserDao.java)
- [InMemoryUserDao.java](/D:/BaitaplonTest/src/main/java/com/auction/dao/memory/InMemoryUserDao.java)
- [SqliteUserDao.java](/D:/BaitaplonTest/src/main/java/com/auction/dao/sqlite/SqliteUserDao.java)

Đã bổ sung các hàm:
- `save(User user, String passwordHash)`
- `findPasswordHashByEmail(String email)`
- `updatePasswordHash(String email, String passwordHash)`

Ý nghĩa:
- tầng auth có thể lưu hash lúc register
- tầng login có thể đọc hash để verify
- dữ liệu demo cũ có thể được cập nhật hash mà không cần xóa DB

### Tầng UI / login

- [login-view.fxml](/D:/BaitaplonTest/src/main/resources/fxml/login-view.fxml)
- [AuthController.java](/D:/BaitaplonTest/src/main/java/com/auction/controller/AuthController.java)
- [LoginViewModel.java](/D:/BaitaplonTest/src/main/java/com/auction/presentation/LoginViewModel.java)

Đã thay đổi:
- thêm `PasswordField`
- tách rõ 2 khu vực `Login` và `Register`
- login giờ truyền cả `email` và `password`
- register có:
  - `username`
  - `email`
  - `password`
  - `confirm password`
  - `account type`
- kiểm tra `confirm password` phải khớp
- sau khi đăng ký thành công, user được điều hướng sang đúng màn theo role
- message demo account được cập nhật để người dùng biết password mặc định

### Khởi tạo dữ liệu demo

- [AppContext.java](/D:/BaitaplonTest/src/main/java/com/auction/app/AppContext.java)

Đã thay đổi:
- thêm hằng `DEMO_PASSWORD = "demo12345"`
- khi seed user demo sẽ tạo luôn mật khẩu
- nếu DB cũ đã có user demo nhưng chưa có `password_hash`, hàm `ensurePassword(...)` sẽ tự bổ sung

### Migration DB cũ

- [DatabaseManager.java](/D:/BaitaplonTest/src/main/java/com/auction/db/DatabaseManager.java)

Đã thêm:
- `ensureUsersPasswordHashColumn(...)`

Cách hoạt động:

1. app khởi động
2. chạy `initializeSchema()`
3. kiểm tra `PRAGMA table_info(users)`
4. nếu chưa có cột `password_hash` thì chạy:

```sql
ALTER TABLE users ADD COLUMN password_hash TEXT NOT NULL DEFAULT ''
```

Ý nghĩa:
- DB cũ vẫn mở được
- không bắt buộc người dùng phải xóa file `.db` rồi tạo lại từ đầu

## Tài khoản demo hiện tại

Email:
- `seller@auction.local`
- `bidder@auction.local`
- `admin@auction.local`

Mật khẩu mặc định:
- `demo12345`

## Những điểm bảo mật đã làm đúng

- không lưu plain text password
- mỗi password có `salt` riêng
- hash được tính lại khi login để so khớp
- dữ liệu hash được lưu tách biệt trong DB

## Những gì chưa làm

Hiện tại phần mật khẩu mới ở mức cơ bản, chưa phải auth production đầy đủ.

Chưa có:
- đổi mật khẩu
- quên mật khẩu
- policy mạnh hơn như chữ hoa/số/ký tự đặc biệt
- lock account sau nhiều lần đăng nhập sai
- session/token

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

Sau cập nhật này, dự án đã có xác thực bằng mật khẩu ở mức cơ bản nhưng đúng hướng kỹ thuật:

- login dùng `email + password`
- đã có đăng ký tài khoản thật ngay trên giao diện
- mật khẩu được hash bằng `PBKDF2WithHmacSHA256`
- database cũ được migrate mềm
- tài khoản demo vẫn dùng được ngay
