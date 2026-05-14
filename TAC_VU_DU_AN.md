# Các Tác Vụ Của Hệ Thống (Auctra System Features)

Dự án đấu giá trực tuyến Auctra được chia thành 3 vai trò chính. Dưới đây là mô tả chi tiết các tác vụ (use-cases) mà từng người dùng có thể thực hiện trên hệ thống.

## 1. Người Mua (Bidder)
Bidder là những người dùng tham gia vào các phiên đấu giá để trả giá mua các vật phẩm.

*   **Đăng ký/Đăng nhập:** Tạo tài khoản mới với vai trò Bidder.
*   **Xem danh sách đấu giá:** Xem tất cả các vật phẩm đang được mở bán trên nền tảng.
*   **Lọc phiên đấu giá:** Lọc danh sách theo trạng thái (Tất cả, Đang diễn ra, Đã kết thúc).
*   **Tìm kiếm:** Tìm kiếm vật phẩm đấu giá theo tên.
*   **Xem chi tiết phiên đấu giá:** Xem hình ảnh, mô tả, giá khởi điểm, giá hiện tại và lịch sử trả giá.
*   **Đặt giá (Place Bid):** Gửi yêu cầu đặt giá (Bid) cho một vật phẩm đang trong trạng thái `RUNNING`. Hệ thống sẽ tự động kiểm tra xem giá đặt có lớn hơn giá hiện tại hay không.
*   **Cập nhật thời gian thực:** Nhận các thông báo cập nhật giá ngay lập tức qua Socket khi có người khác đặt giá cao hơn.

## 2. Người Bán (Seller)
Seller là người có các vật phẩm muốn mang lên nền tảng để đấu giá.

*   **Đăng ký/Đăng nhập:** Đăng ký tài khoản Seller.
*   **Quản lý phiên đấu giá cá nhân:** Xem danh sách các phiên đấu giá do chính mình tạo ra.
*   **Tạo vật phẩm & Phiên đấu giá mới:**
    *   Điền thông tin: Tên vật phẩm, Loại sản phẩm, Mô tả, Giá khởi điểm.
    *   Tải lên hình ảnh vật phẩm.
*   **Bắt đầu phiên đấu giá (Start):** Chuyển trạng thái của phiên đấu giá từ `OPEN` sang `RUNNING` để người mua có thể bắt đầu đặt giá.
*   **Kết thúc phiên đấu giá (Finish):** Chốt phiên đấu giá. Chuyển trạng thái sang `FINISHED`. Người mua trả giá cao nhất ở thời điểm này sẽ là người chiến thắng.
*   **Xem chi tiết:** Theo dõi lịch sử đặt giá của người mua đối với sản phẩm của mình theo thời gian thực.

## 3. Quản Trị Viên (Admin)
Admin là người có toàn quyền quản lý hệ thống, đảm bảo nền tảng hoạt động trơn tru.

*   **Đăng nhập bảo mật:** Admin sử dụng tài khoản được cấp sẵn (không thể tự đăng ký).
*   **Bảng Điều Khiển (Dashboard):** Xem tổng quan các số liệu thống kê: Tổng số người dùng, Số phiên đấu giá đang chạy, Số phiên đã thanh toán, Tổng doanh thu của hệ thống.
*   **Quản lý Người dùng (User Management):**
    *   Xem danh sách tất cả người dùng trong hệ thống (ID, Username, Email, Role).
    *   Tìm kiếm người dùng theo tên hoặc email.
*   **Quản lý Đấu giá (Auction Management):**
    *   Xem danh sách toàn bộ các phiên đấu giá trên hệ thống.
    *   **Huỷ (Cancel):** Buộc huỷ một phiên đấu giá vi phạm quy định (chuyển trạng thái sang `CANCELED`).
    *   **Xác nhận Thanh toán (Mark Paid):** Cập nhật trạng thái phiên đấu giá thành `PAID` sau khi người chiến thắng đã hoàn tất nghĩa vụ thanh toán.

## 4. Tác Vụ Hệ Thống (System Background Tasks)
*   **Bảo mật mật khẩu:** Hash mật khẩu bằng thuật toán `PBKDF2WithHmacSHA256` trước khi lưu vào SQLite.
*   **Đồng bộ Socket:** Quản lý kết nối Client-Server, quảng bá (broadcast) thay đổi dữ liệu (giá đấu mới) cho toàn bộ Client đang kết nối.
*   **Tự động tạo Schema:** Tự động tạo bảng SQLite và dữ liệu mẫu nếu chạy lần đầu.
