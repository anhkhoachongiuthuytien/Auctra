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
D:\BaitaplonTest\Auctra
├── auction-common         # Thư viện dùng chung (Models, DTOs, Enums, Mappers, Encryptors)
├── auction-server         # TCP Socket Server, SQLite Database, Services, DAOs
├── auction-client         # JavaFX UI Controllers, FXML layouts, TCP Socket Client Gateway
└── pom.xml                # Parent POM cấu hình chung và quản lý dependencies
```

---

## 3. Hướng Dẫn Chạy Chương Trình
> Luôn khởi chạy **Server trước**, sau đó mới khởi chạy **Client**.

### Bước 1: Biên dịch và đóng gói toàn bộ dự án
Chạy lệnh sau để dọn dẹp, tải các thư viện cần thiết, biên dịch mã nguồn và chạy toàn bộ kiểm thử trước khi cài các module vào Maven local repository:
```bash
mvn clean install
```
*Lưu ý: Lệnh này sẽ tạo ra file đóng gói `auction-server.jar` tại đường dẫn `auction-server/target/auction-server.jar`.*

### Bước 2: Khởi chạy Server
Khởi chạy tiến trình Server để mở cổng kết nối TCP (mặc định cổng 9999):
```bash
java -jar auction-server/target/auction-server.jar
```

### Bước 3: Khởi chạy Client
Mở một cửa sổ terminal mới (vẫn đứng tại thư mục gốc dự án) và chọn một trong các chế độ chạy sau:

*   **Chế độ SOCKET (Kết nối tới Server đang chạy)**:
  *   **Nếu dùng Windows PowerShell**:
      ```powershell
      mvn -pl auction-client javafx:run '-Djavafx.args=--socket localhost 9999'
      ```
      *(Lưu ý: Bắt buộc phải dùng dấu nháy đơn `'` bao quanh tham số để PowerShell không hiểu sai cú pháp).*
  *   **Nếu dùng Windows CMD (Command Prompt)**:
      ```cmd
      mvn -pl auction-client javafx:run -Djavafx.args="--socket localhost 9999"
      ```
  *   **Nếu dùng Linux / macOS (Bash / Zsh)**:
      ```bash
      mvn -pl auction-client javafx:run '-Djavafx.args=--socket localhost 9999'
      ```

*   **Chế độ LOCAL (Chạy offline, tự động tích hợp DB trong tiến trình, không cần bật Server)**:
    ```bash
    mvn -pl auction-client javafx:run
    ```

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

## 6. Bản sửa lỗi đấu giá và hướng dẫn kiểm chứng

Trên Windows, nếu `mvn` đang dùng JRE và báo thiếu compiler, chạy `./build.ps1`.
Script chọn JDK từ lệnh `javac`, chạy test và install các module, rồi khôi phục biến môi trường của terminal.
Sau khi build, chạy `./run-server.ps1` và ở terminal khác chạy `./run-client.ps1`.
Để chạy client trên máy khác dùng `./run-client.ps1 -ServerHost 192.168.1.10 -Port 9999`;
để chạy LOCAL dùng `./run-client.ps1 -Local`. Các script chạy từ thư mục gốc và chọn JDK để tránh lỗi JRE thiếu compiler.

- Auto-bid chạy ngay khi kích hoạt. Khi đang dẫn đầu, người mua không tự nâng giá của mình.
- Auto-bid phản hồi khi bị vượt giá, tuân thủ max/bước giá, và ngừng sau khi hủy hoặc phiên hết hạn.
- Server từ chối bước giá không dương, NaN/Infinity, max không đủ cho bước tiếp theo, tài khoản sai vai trò và phiên đã đóng.
- Các cấu hình cũ không hợp lệ được bỏ qua để không làm hỏng bid thủ công. Vòng đấu tự động vượt 10.000 lượt bị từ chối
  trước khi thay đổi dữ liệu; cần chọn bước nâng lớn hơn.
- Thao tác đọc–kiểm tra–ghi dùng khóa ổn định theo mã phiên; SQLite kiểm tra version để chặn snapshot cũ ghi đè.
  Lịch sử chỉ thêm bid mới, không xóa rồi ghi lại.
- LOCAL và SOCKET dùng chung request handler, session và các exception nghiệp vụ có kiểu rõ ràng.
- Session hết hạn sau 30 phút, bị hủy khi đăng xuất hoặc đổi mật khẩu. Server kiểm tra vai trò và quyền sở hữu.
- Socket hỗ trợ tối đa 128 connection đồng thời (mỗi client thường dùng 2), có timeout và hàng đợi realtime riêng.
  Subscriber chậm bị ngắt thay vì làm chặn các thao tác đặt giá. Không tự gửi lại thao tác ghi khi mất response.
- Client/server của bản sửa phải được build và khởi động lại cùng nhau vì protocol có thêm session và mã lỗi.

### Khôi phục mật khẩu

Khi đăng ký, ứng dụng hiển thị mã khôi phục bí mật một lần; hãy sao chép và cất giữ. Màn hình Quên mật khẩu cần
email và mã này, không dùng username để chứng minh quyền sở hữu. Database chỉ lưu hash, mã chỉ dùng được một lần.

Với tài khoản cũ hoặc đã dùng hết mã, người quản lý có quyền truy cập database server có thể cấp mã mới:

```powershell
java -jar auction-server/target/auction-server.jar --recovery-code email@example.com auction-system.db
```

### Chạy kiểm thử và demo auto-bid

```powershell
mvn clean install
java -jar auction-server/target/auction-server.jar
# Ở terminal khác, cùng thư mục gốc:
mvn -pl auction-client javafx:run '-Djavafx.args=--socket localhost 9999'
```

1. Mở phiên đang RUNNING bằng tài khoản Bidder A; bật Auto-Bid với max cao hơn giá hiện tại cộng bước nâng.
2. Giá tăng ngay một bước. Bật/đổi cấu hình khi A đang dẫn đầu không làm A tự trả thêm.
3. Dùng Bidder B ở client thứ hai trả giá; A phản hồi nếu bước tiếp theo vẫn nằm trong max.
4. Hủy cấu hình của A; bid tiếp theo của B không kích hoạt A nữa.
5. Các test `AutoBidRegressionTest`, `SqliteConcurrentBidTest`, `RequestSecurityTest` và
   `SocketGatewayIntegrationTest` kiểm tra các hành vi trên, phân quyền, recovery và nhiều client.
