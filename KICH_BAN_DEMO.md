# Kịch Bản Demo Dự Án Auctra

Dưới đây là quy trình từng bước để bạn demo ứng dụng cho Giảng viên một cách mượt mà và chuyên nghiệp nhất, thể hiện được toàn bộ các tính năng cốt lõi của hệ thống mạng phân tán (Socket) và Giao diện (JavaFX).

---

## 🛠 Chuẩn bị trước khi Demo
1. **Khởi động Server:** Mở terminal, chạy lệnh `java -jar auction-server.jar`. Đảm bảo thấy dòng `[Server] Dang chay tai cong 9999`.
2. **Khởi động 3 Client (3 cửa sổ):** 
   - Mở 3 terminal riêng biệt, chạy `mvn javafx:run` ở thư mục `auction-client` cho mỗi cái để giả lập 3 người dùng khác nhau trên cùng một màn hình (hoặc 2 máy tính nếu có thể).
3. **Sắp xếp cửa sổ màn hình:** Để cửa sổ Admin ở một bên, cửa sổ Seller và Bidder ở bên còn lại để dễ dàng thấy dữ liệu được đồng bộ realtime.

---

## 🎬 Bước 1: Giới thiệu Hệ thống & Đăng nhập (Authentication)

1. **Mở Client 1:** Show giao diện Đăng nhập mang phong cách Google Material Design.
2. **Đăng ký (Register):** 
   - Click "Tạo tài khoản". 
   - Demo việc điền thông tin và chọn vai trò là `Bidder`. Nhấn mạnh tính năng validate (mật khẩu phải 8 ký tự, không được trùng email).
3. **Mã hóa mật khẩu:** 
   - *Nói với thầy:* "Mật khẩu của em khi lưu xuống SQLite đã được băm bằng thuật toán PBKDF2WithHmacSHA256 kèm Salt, đảm bảo chống lại các cuộc tấn công dò mật khẩu (Rainbow Table)."
4. **Đăng nhập (Login):**
   - Đăng nhập lần lượt 3 cửa sổ: 
     - Cửa sổ 1: Đăng nhập quyền **Admin** (`admin@auction.local` / `demo12345`)
     - Cửa sổ 2: Đăng nhập quyền **Seller** (`seller@auction.local` / `demo12345`)
     - Cửa sổ 3: Đăng nhập quyền **Bidder** (`bidder@auction.local` / `demo12345`)

---

## 🎬 Bước 2: Demo Chức năng của Seller (Người Bán)

1. **Trên cửa sổ Seller:**
   - Chuyển sang Tab "Trang người bán".
   - **Tạo vật phẩm mới:** Nhập tên sản phẩm (Ví dụ: "MacBook Pro M3"), chọn loại, mô tả, và đặt giá khởi điểm (Ví dụ: 1000$). Click "Tạo phiên đấu giá".
   - *Điểm nhấn:* Ngay khi tạo xong, sản phẩm sẽ xuất hiện ở bảng phía dưới với trạng thái `OPEN` (Mở).
2. **Bắt đầu Đấu giá:**
   - Chọn sản phẩm vừa tạo trong bảng.
   - Nhấn nút **"▶ Bắt đầu"** (Start). Trạng thái sản phẩm lập tức chuyển sang `RUNNING` (Đang diễn ra).

---

## 🎬 Bước 3: Demo Tính năng Real-time & Đặt giá (Bidder)

1. **Trên cửa sổ Bidder:**
   - Nhấn "Làm mới" hoặc chọn Tab "Đang diễn ra" để thấy chiếc MacBook Pro M3 vừa được Seller mở bán.
   - Click "Chi tiết" để vào phòng đấu giá.
2. **Giao dịch (Bidding):**
   - Đặt giá cao hơn giá khởi điểm (Ví dụ: 1200$). Nhấn "Đặt giá".
   - Hệ thống báo thành công.
3. **Đồng bộ Real-time (Socket & Observer Pattern):**
   - *Nói với thầy:* "Bây giờ thầy nhìn sang cửa sổ của Seller."
   - Chỉ cho thầy thấy màn hình Seller tự động cập nhật "Giá hiện tại" lên 1200$ ngay lập tức mà không cần nhấn F5 (Refresh). 
   - Giải thích: "Khác với mô hình HTTP truyền thống phải nhấn F5, ứng dụng của em sử dụng kiến trúc **Dual-Socket (Real-time Push Notifications)**. Khi Client khởi động, nó mở ngầm 1 luồng kết nối phụ chuyên để lắng nghe sự kiện (`SUBSCRIBE_UPDATES`). Nhờ có class `BroadcastManager` trên Server, bất cứ khi nào có thay đổi, Server sẽ phát sóng (broadcast) thẳng về Client, và em dùng `Platform.runLater` để update giao diện tức thì."

---

## 🎬 Bước 4: Chốt Phiên & Thanh toán (Seller & Admin)

1. **Kết thúc (Finish):**
   - Quay lại cửa sổ Seller. Chọn MacBook Pro M3 và nhấn **"⏹ Kết thúc"**. 
   - Trạng thái chuyển thành `FINISHED`. 
2. **Theo dõi của Admin:**
   - Qua cửa sổ Admin. Click tab "Tổng quan" (Dashboard). 
   - Chỉ cho thầy các thống kê tự động nhảy số (Tổng người dùng, Doanh thu, Số phiên...).
   - Chuyển sang tab "Phiên đấu giá". Tìm MacBook Pro M3.
3. **Hoàn tất Giao dịch:**
   - Giả lập việc người mua đã thanh toán tiền.
   - Admin chọn phiên đấu giá đó, nhấn nút **"Đã TT"** (Mark Paid).
   - Trạng thái chuyển thành `PAID` màu tím. Bảng thống kê "Tổng doanh thu" của Admin sẽ tự động cộng thêm số tiền 1200$ vừa chốt.

---

## 🎓 Bước 5: Tổng kết Kiến trúc Code

*Mở source code lên màn hình (nếu được yêu cầu) và giới thiệu sơ qua:*
1. **Multi-Module Maven:** Chỉ ra 3 thư mục `auction-client`, `auction-server`, `auction-common`. Phân chia rõ ràng để dễ quản lý.
2. **Gateway Pattern:** Mở file `SocketAuctionClientGateway.java` để cho thầy thấy cách giao diện UI không gọi thẳng vào Database mà gói data gửi qua Socket.
3. **Facade Pattern:** Mở file `AuctionServerFacade.java` ở Server để thấy luồng rẽ nhánh xử lý thông minh.
4. **CSS Material Design:** Mở `app.css` để thầy thấy em tự code giao diện đồng bộ chứ không dùng kéo thả cơ bản, thể hiện sự đầu tư chỉn chu.

*Chúc bạn bảo vệ tiến độ dự án thật xuất sắc và đạt điểm cao!*
