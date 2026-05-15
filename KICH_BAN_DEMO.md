# 📑 Kịch Bản Demo Auctra Chi Tiết (Tuần 10 - Final Polish)

Tài liệu này hướng dẫn chi tiết từng click chuột để bạn trình diễn dự án một cách hoàn hảo nhất.

---

## 🛠 GIAI ĐOẠN 0: CHUẨN BỊ (5 phút trước khi demo)
1.  **Xóa dữ liệu cũ (Nếu muốn làm mới từ đầu):** Xóa file `auction-system.db` trong thư mục `auction-server`.
2.  **Build toàn bộ:** Mở terminal tại `d:\BaitaplonTest` chạy `mvn clean install -DskipTests`.
3.  **Mở Server:** Chạy `java -jar auction-server/target/auction-server.jar`.
4.  **Mở 3 Client (3 cửa sổ riêng):** Tại `auction-client` chạy `mvn javafx:run "-Djavafx.args=--socket localhost 9999"`.
    *   *Sắp xếp:* Admin bên trái, Seller bên phải trên, Bidder bên phải dưới.

---

## 🎭 GIAI ĐOẠN 1: ĐĂNG KÝ & ĐĂNG NHẬP (Show giao diện & Auth)
1.  **Tại máy Bidder (Cửa sổ 3):**
    *   Nhấn **Tạo tài khoản**. 
    *   Nhập Email mới (VD: `test@gmail.com`), chọn vai trò **Bidder**, mật khẩu `12345678`.
    *   Nhấn **Tiếp theo**. Thông báo "Đăng ký thành công".
2.  **Thử nghiệm Quên mật khẩu:**
    *   Nhấn **Quên mật khẩu?** tại màn hình đăng nhập.
    *   Nhập Email và tên người dùng vừa tạo -> Nhập mật khẩu mới.
    *   Nhấn **Tiếp theo** -> Quay lại đăng nhập bằng mật khẩu mới.
3.  **Đăng nhập chính thức:** Đăng nhập 3 quyền (Admin, Seller, Bidder) vào 3 cửa sổ như đã sắp xếp.

---

## 🎭 GIAI ĐOẠN 2: QUY TRÌNH NGƯỜI BÁN (Seller Workflow)
1.  **Tại máy Seller:**
    *   Vào tab **Trang người bán** (Sidebar trái).
    *   Nhấn nút **Tạo phiên đấu giá** (Nút xanh dương có icon +).
    *   Điền: Tên = `iPhone 15 Pro`, Loại = `Electronics`, Giá = `1000`, Mô tả = `Máy mới 100%`.
    *   Nhấn **Tạo**. Quan sát bảng phía dưới: Item hiện lên với trạng thái `OPEN` (Màu xanh dương).
2.  **Kích hoạt phiên:**
    *   Chọn dòng `iPhone 15 Pro` trong bảng.
    *   Nhấn nút **▶ Bắt đầu** ở phía trên bảng.
    *   Trạng thái chuyển sang `RUNNING` (Màu xanh lá).

---

## 🎭 GIAI ĐOẠN 3: ĐẤU GIÁ THỜI GIAN THỰC (Real-time Showcase)
1.  **Tại máy Bidder:**
    *   Vào tab **Đang diễn ra** -> Nhấn **Làm mới** (nếu chưa thấy).
    *   Thấy `iPhone 15 Pro` -> Nhấn nút **Chi tiết**.
2.  **Thực hiện đặt giá:**
    *   Nhập giá `1200` vào ô nhập liệu -> Nhấn **Đặt giá**.
3.  **QUAN SÁT REAL-TIME (Cực kỳ quan trọng):**
    *   **Máy Seller:** Giá hiện tại tự nhảy lên 1200$ ngay lập tức.
    *   **Máy Admin:** (Nếu đang mở tab Phiên đấu giá) Giá cũng tự nhảy lên 1200$.
    *   **Mô tả kỹ thuật:** *Giải thích đây là nhờ cơ chế Push Notifications của Dual-Socket, không cần F5.*

---

## 🎭 GIAI ĐOẠN 4: KẾT THÚC & QUẢN TRỊ (Admin & Closing)
1.  **Tại máy Seller:** Chọn sản phẩm -> Nhấn **⏹ Kết thúc**. Trạng thái thành `FINISHED` (Màu vàng).
2.  **Tại máy Admin:**
    *   Vào tab **Tổng quan** (Dashboard): Show các thẻ thống kê dọc (Tổng người dùng tăng, Phiên đang diễn ra giảm).
    *   Vào tab **Phiên đấu giá**: Chọn iPhone 15 Pro -> Nhấn **Đã TT** (Đã thanh toán).
    *   Trạng thái thành `PAID` (Màu tím).
3.  **Kiểm tra doanh thu:** Quay lại tab **Tổng quan**, thẻ **Tổng doanh thu** đã tự động cộng thêm 1200$.

---

## 🎭 GIAI ĐOẠN 5: SHOW CHI TIẾT KỸ THUẬT (Dành cho câu hỏi khó)
1.  **Xem Database:** Mở **DB Browser for SQLite** -> Mở file `auction-system.db` -> Show bảng `users` (Chỉ cho thầy thấy cột `password_hash` đã được mã hóa không thể đọc được).
2.  **Chất lượng Code:** 
    *   Mở Terminal -> Chạy `mvn test`. Show kết quả **61 tests passed**.
    *   Mở trình duyệt -> Vào GitHub -> Tab **Actions**. Show lịch sử build tự động (Tick xanh).
3.  **Cấu trúc dự án:** Show 3 module `common`, `server`, `client` trong IDE để chứng minh kiến trúc chuyên nghiệp.

---

**Ghi chú:** Luôn giữ bình tĩnh, nếu máy lag, hãy giải thích đó là do cơ chế mạng đang xử lý đồng bộ dữ liệu lớn. Chúc bạn thành công!
