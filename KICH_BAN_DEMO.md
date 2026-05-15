# 🚀 Quy Trình Thao Tác Demo Auctra (Chỉ gồm các bước thực hiện)

Đây là checklist các bước thao tác trên máy tính để show toàn bộ tính năng dự án.

---

### 🛠 Bước 1: Chuẩn bị hệ thống
1.  **Chạy Server:** Mở terminal tại gốc dự án -> `java -jar auction-server/target/auction-server.jar`.
2.  **Chạy 3 Client:** Mở 3 terminal mới tại `auction-client` -> Chạy lệnh:
    `mvn javafx:run "-Djavafx.args=--socket localhost 9999"`
3.  **Sắp xếp:** Để 1 cửa sổ bên trái, 2 cửa sổ còn lại xếp chồng bên phải.

---

### 🔑 Bước 2: Đăng nhập & Phân quyền
1.  **Cửa sổ 1 (Admin):** Đăng nhập `admin@auction.local` / `demo12345`.
2.  **Cửa sổ 2 (Seller):** Đăng nhập `seller@auction.local` / `demo12345`.
3.  **Cửa sổ 3 (Bidder):** Đăng nhập `bidder@auction.local` / `demo12345`.

---

### 📦 Bước 3: Quy trình Đấu giá (Show Real-time)
1.  **Tại máy Seller:**
    *   Vào tab **Trang người bán**.
    *   Nhấn **Tạo phiên đấu giá** -> Nhập tên sản phẩm, giá khởi điểm (VD: 500$).
    *   Trong bảng bên dưới, chọn sản phẩm vừa tạo -> Nhấn **▶ Bắt đầu**.
2.  **Tại máy Bidder:**
    *   Nhấn **Làm mới** hoặc vào tab **Đang diễn ra**.
    *   Nhấn **Chi tiết** vào sản phẩm vừa hiện ra.
    *   Nhập giá cao hơn (VD: 700$) -> Nhấn **Đặt giá**.
3.  **Quan sát sự đồng bộ:**
    *   Nhìn sang máy **Seller** và **Admin**: Giá 700$ tự động cập nhật ngay lập tức mà không cần bấm gì.

---

### 📊 Bước 4: Quản lý & Kết thúc
1.  **Tại máy Seller:** Chọn sản phẩm -> Nhấn **⏹ Kết thúc**.
2.  **Tại máy Admin:**
    *   Vào tab **Tổng quan**: Chỉ cho thầy xem các con số thống kê (Tổng người dùng, Doanh thu) tự nhảy.
    *   Vào tab **Phiên đấu giá**: Chọn phiên vừa kết thúc -> Nhấn **Đã TT** (Đã thanh toán).
3.  **Kiểm tra lại Dashboard:** Số tiền doanh thu của Admin sẽ tự động cộng thêm số tiền vừa chốt.

---

### 🛠 Bước 5: Show Kỹ thuật (Nếu được hỏi)
1.  **Database:** Mở công cụ **DB Browser for SQLite** -> Mở file `.db` -> Show bảng `users` đã mã hóa mật khẩu.
2.  **Code:** Mở thư mục dự án, chỉ vào 3 module `client`, `server`, `common`.
3.  **Test:** Mở terminal chạy `mvn test` để show **61 tests** chạy tự động thành công.
4.  **CI/CD:** Mở trình duyệt vào tab **Actions** trên GitHub để show lịch sử build tự động.
