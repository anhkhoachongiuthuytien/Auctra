# Auctra - Hệ Thống Đấu Giá Real-time (Client - Server)

Auctra là ứng dụng đấu giá trực tuyến được thiết kế theo kiến trúc Client-Server sử dụng kết nối TCP/IP Socket thời gian thực và giao diện đồ họa JavaFX hiện đại. Bài toán giải quyết việc kết nối đồng bộ và tức thời giữa những người mua (Bidder) tham gia trả giá và những người bán (Seller) quản lý phiên đấu giá dưới sự điều phối của người quản trị (Admin).

---

## 1. Công Nghệ Sử Dụng & Yêu Cầu Hệ Thống

### Công nghệ sử dụng
- **Ngôn ngữ**: Java 17
- **Giao diện Client**: JavaFX 21 + FXML + CSS tùy chỉnh (Auctra Design System)
- **Hệ quản trị cơ sở dữ liệu**: SQLite (sử dụng SQLite JDBC Driver)
- **Quản lý dự án & Build**: Maven
- **Kết nối Real-time**: Custom TCP/IP Socket Protocol + Observer Pattern qua Network
- **Kiểm thử**: JUnit 5 + JaCoCo (Độ bao phủ code)
- **Tích hợp liên tục**: CI/CD qua GitHub Actions

### Môi trường chạy & Yêu cầu cài đặt
- **JDK**: Java Development Kit phiên bản từ 17 trở lên.
- **Maven**: Maven 3.8+ (đã cấu hình biến môi trường `mvn`).
- **Hệ điều hành**: Windows, Linux hoặc macOS (đối với Linux/macOS cần cài đặt sẵn môi trường đồ họa hiển thị cửa sổ JavaFX).

---

## 2. Cấu Trúc Các Module Chính

Dự án được tổ chức theo mô hình Multi-Module Maven sạch sẽ:
```
d:\BaitaplonTest
├── auction-common         # Thư viện dùng chung (Models, DTOs, Enums, Mappers, Encryptors)
├── auction-server         # TCP Socket Server, SQLite Database, Services, DAOs
├── auction-client         # JavaFX UI Controllers, FXML layouts, TCP Socket Client Gateway
└── pom.xml                # Parent POM cấu hình chung và quản lý dependencies
```

---

## 3. Hướng Dẫn Chạy Chương Trình

**khởi động Server trước**, sau đó mới **khởi động Client**.

### Bước 1: Build toàn bộ dự án
Mở terminal tại thư mục gốc của dự án (`d:\BaitaplonTest`) và chạy câu lệnh sau để biên dịch dự án và bỏ qua các bài test:
```bash
mvn clean install -DskipTests
```

### Bước 2: Khởi chạy Server
Server mặc định sẽ khởi động và lắng nghe kết nối tại cổng `9999`.
- **Trên Windows / Linux / macOS (Sử dụng dòng lệnh)**:
  ```bash
  java -jar auction-server/target/auction-server.jar
  ```

### Bước 3: Khởi chạy Client
Client sẽ khởi động và kết nối trực tiếp đến Socket Server tại `localhost:9999`.

- **Trên Windows / Linux / macOS (Sử dụng dòng lệnh)**:
  ```bash
  mvn -pl auction-client javafx:run -Djavafx.args="--socket localhost 9999"
  ```
  *(Lưu ý: Đối với một số shell trên macOS hoặc Linux, bạn có thể cần bọc tham số args trong nháy đơn: `mvn -pl auction-client javafx:run '-Djavafx.args=--socket localhost 9999'`)*

---

## 4. Danh Sách Chức Năng Đã Hoàn Thành

### Chức năng cốt lõi (Bắt buộc)
1. **Thiết kế hướng đối tượng (OOP)**: Triển khai cây kế thừa người dùng (`User` -> `Bidder`, `Seller`, `Admin`), phân tách rõ vai trò và nghiệp vụ.
2. **Quản lý Người dùng & Sản phẩm**: Đăng nhập, đăng ký tài khoản (Seller/Bidder), cập nhật thông tin cá nhân (ảnh đại diện, email, thông tin riêng biệt từng vai trò), tạo sản phẩm đấu giá mới kèm tải lên nhiều hình ảnh.
3. **Đấu giá trực tuyến**: Người bán bắt đầu phiên đấu giá, người mua tham gia trả giá, tự động đóng phiên khi hết giờ và ghi nhận người thắng cuộc.
4. **Xử lý bất đồng bộ & Concurrency an toàn**: Sử dụng `ReentrantLock` bảo vệ tài nguyên `Auction` tránh race condition, lost update khi có nhiều bidder cùng đặt giá trong một phần nghìn giây.
5. **Real-time Push Notifications**: Tự động thông báo cập nhật giá mới tức thời tới tất cả client thông qua cơ chế Server Broadcast Socket.

### Chức năng nâng cao (Tùy chọn)
1. **Đấu giá tự động (Auto-Bidding)**: Người mua có thể cấu hình giá tối đa (`maxBid`) và bước nhảy (`increment`). Hệ thống sử dụng thuật toán `PriorityQueue` để tự động trả giá thay người dùng khi có người đặt giá cao hơn.
2. **Chống bắn tỉa giá (Anti-sniping)**: Tự động gia hạn thêm thời gian đếm ngược (60 giây) nếu có lượt đặt giá hợp lệ xuất hiện trong vòng 60 giây cuối cùng trước khi phiên đấu giá kết thúc.
3. **Thiết lập thời gian tùy chọn**: Cho phép người bán tự đặt số phút kết thúc khi tạo đấu giá và có quyền bấm kết thúc thủ công bất cứ lúc nào khi phiên đang chạy (`RUNNING`).
4. **Trực quan hoá lịch sử (Real-time Line Chart)**: Vẽ biểu đồ đường biến động giá theo thời gian thực tại màn hình chi tiết đấu giá.

---

## 5. Tài Nguyên Đi Kèm
- **Báo cáo chi tiết (PDF)**: https://drive.google.com/file/d/1tyttMCH6bK7X140k84u1ZG6bczcYUWMZ/view?usp=sharing
- **Video Demo sản phẩm**: https://youtu.be/cOjOeHgrtVg