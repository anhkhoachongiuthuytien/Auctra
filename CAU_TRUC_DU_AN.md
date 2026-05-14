# Cấu Trúc Toàn Bộ Dự Án (Project Architecture)

Dự án Auctra là một hệ thống Client-Server phân tán viết bằng Java. Dự án sử dụng mô hình **Multi-module Maven** để phân tách logic và tối ưu hóa tái sử dụng mã (Code Reusability).

## 1. Mô Hình Kiến Trúc (Dual-Socket Architecture)
Hệ thống hoạt động theo mô hình **Dual-Socket Client-Server Architecture**:
*   **Server:** Quản lý kết nối (Thread Pool), xử lý nghiệp vụ cốt lõi, tương tác với Database (SQLite) và broadcast các thay đổi cho Client. Đặc biệt sử dụng `BroadcastManager` để duy trì danh sách các kết nối lắng nghe sự kiện.
*   **Client:** Chạy giao diện JavaFX. Client mở 2 luồng Socket:
    *   **Luồng 1 (Đồng bộ):** Dùng để gửi Request (gọi hàm API) và nhận Response.
    *   **Luồng 2 (Bất đồng bộ):** Chạy ngầm liên tục để lắng nghe Push Notifications từ Server (Real-time update) và cập nhật giao diện thông qua `ClientEventManager`.
*   **Giao thức:** Sử dụng `ObjectInputStream`/`ObjectOutputStream` của Java để truyền tải các gói tin `AuctionRequest`, `AuctionResponse` và `AuctionEvent`.

## 2. Các Module Trong Maven

Dự án được chia làm 3 module độc lập, tương tác với nhau:

### A. `auction-common` (Thành phần dùng chung)
Đây là thư viện lõi, được cả Client và Server import. Chứa các thành phần không mang tính xử lý nặng, chủ yếu là các định nghĩa dữ liệu.
*   **Model:** Định nghĩa các đối tượng nghiệp vụ (User, Bidder, Seller, Admin, Item, Auction, Bid).
*   **Giao thức Mạng (Protocol):** 
    *   `AuctionRequest`, `AuctionResponse`: Dữ liệu gửi/nhận qua Socket.
    *   `RequestType`: Enum quy định các loại API (vd: `LOGIN`, `PLACE_BID`).
    *   `DtoMapper`: Lớp chuyển đổi giữa Model thật và Data Transfer Object (DTO) để gửi qua mạng.
*   **Observer Pattern:** Khai báo các interface `Subject` và `Observer` dùng cho việc cập nhật giá thầu theo thời gian thực.

### B. `auction-server` (Hệ thống Máy chủ)
Đây là bộ não của ứng dụng.
*   **Network Layer:** `AuctionSocketServer` (Lắng nghe cổng TCP, duy trì Thread Pool cho mỗi Client).
*   **Facade Layer:** `AuctionServerFacade` (Chuyển hướng các request mạng tới Service xử lý tương ứng).
*   **Service Layer:** Chứa logic nghiệp vụ cốt lõi (`AuthService`, `AuctionService`, `BidService`, `SellerService`, `UserService`).
*   **DAO Layer (Data Access Object):** Triển khai thao tác Database bằng SQLite (`SqliteAuctionDao`, `SqliteUserDao`, `SqliteItemDao`).
*   **Database:** Quản lý cấu hình, tạo bảng (`DatabaseManager` và `schema.sql`).

### C. `auction-client` (Hệ thống Máy trạm - Giao diện)
Đảm nhận hiển thị giao diện đồ hoạ cho người dùng.
*   **Gateway Layer:** `SocketAuctionClientGateway` (Triển khai mẫu thiết kế Gateway. Thay vì gọi Service, nó đóng gói dữ liệu thành `AuctionRequest`, đẩy qua Socket và chờ `AuctionResponse`).
*   **Controller Layer (JavaFX):** Chứa code điều khiển các nút bấm, điền form (`LoginController`, `AdminController`, `SellerController`, `AuctionController`).
*   **Views (.fxml):** Cấu trúc layout màn hình.
*   **Styles (.css):** Toàn bộ giao diện được thiết kế theo `Google Material Design v2` lưu tại thư mục `css/app.css`.
*   **Navigator:** Lớp `SceneNavigator` quản lý việc chuyển đổi qua lại giữa các màn hình.

## 3. Luồng Dữ Liệu Ví Dụ (Ví dụ tác vụ Login)
1. **Client (UI):** Người dùng nhập Email, Mật khẩu trên `login-view.fxml`.
2. **Client (Controller):** `AuthController` gọi `gateway.login(email, password)`.
3. **Client (Gateway):** `SocketAuctionClientGateway` tạo `AuctionRequest(type=LOGIN)` gửi qua Socket.
4. **Server (Network):** `AuctionSocketServer` nhận Request, đẩy vào `AuctionServerFacade`.
5. **Server (Service):** `AuthService` kiểm tra email, băm mật khẩu và so sánh bằng chuỗi constant time.
6. **Server (DAO):** Truy vấn SQLite để lấy thông tin.
7. **Server (Network):** Trả về `AuctionResponse` thành công.
8. **Client (Gateway):** Nhận Response, trả về đối tượng `User` cho Controller.
9. **Client (Navigator):** Dựa vào role của `User`, chuyển màn hình sang Admin/Seller/Bidder tương ứng.
