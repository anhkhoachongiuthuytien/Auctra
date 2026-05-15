# ✅ Quy Trình Kiểm Tra Cuối Cùng (Pre-flight Checklist)

Hãy thực hiện từng bước này **15 phút trước khi trình bày** với thầy để đảm bảo không có bất kỳ lỗi kỹ thuật nào xảy ra.

---

## 🏁 Bước 1: Dọn dẹp hệ thống (Clean State)
*   [ ] **Xóa Database cũ:** Xóa file `auction-system.db` (trong cả `auction-server` và `auction-client` nếu có). Việc này giúp bạn có một "bảng sạch" để demo từ đầu.
*   [ ] **Đóng các Terminal cũ:** Đảm bảo không còn tiến trình Java nào chạy ngầm chiếm cổng 9999.
    *   *Lệnh kiểm tra:* `netstat -ano | findstr :9999` (Nếu có kết quả, hãy dùng `taskkill` để tắt).

---

## 🏗 Bước 2: Biên dịch & Cài đặt (Build & Install)
*   [ ] **Mở Terminal tại thư mục gốc (`d:\BaitaplonTest`):**
    ```powershell
    mvn clean install -DskipTests
    ```
    *   *Yêu cầu:* Phải hiện **BUILD SUCCESS** cho cả 4 phần (Root, Common, Server, Client). Nếu lỗi ở đây, tuyệt đối không được demo tiếp.

---

## 📡 Bước 3: Kiểm tra kết nối (Connectivity)
*   [ ] **Bật Server:** 
    `java -jar auction-server/target/auction-server.jar`
    *   *Dấu hiệu tốt:* Thấy dòng `[Server] Đang chạy tại cổng 9999`.
*   [ ] **Bật 1 Client để test (Trên cùng máy):**
    `cd auction-client; mvn javafx:run "-Djavafx.args=--socket localhost 9999"`
    *   *Dấu hiệu tốt:* Màn hình đăng nhập hiện lên nhanh chóng, không bị treo.
*   [ ] **(TÙY CHỌN) Bật Client từ máy tính khác (Mạng LAN):**
    *   Trên máy chạy Server, gõ lệnh `ipconfig` để lấy địa chỉ IPv4 (VD: `192.168.1.15`).
    *   Trên máy tính thứ 2 (đã kết nối chung Wifi), chạy lệnh:
        `cd auction-client; mvn javafx:run "-Djavafx.args=--socket 192.168.1.15 9999"`
    *   *Lưu ý:* Phải đảm bảo tường lửa (Firewall) trên máy Server đã cho phép kết nối qua cổng 9999.

---

## 🧪 Bước 4: Kiểm tra tính năng nhanh (Smoke Test)
*   [ ] **Đăng nhập Admin:** Dùng `admin@auction.local` / `demo12345`. Kiểm tra xem tab "Tổng quan" có hiện các thẻ thống kê không.
*   [ ] **Đăng nhập Seller:** Thử tạo nhanh 1 vật phẩm nháp -> Bắt đầu đấu giá.
*   [ ] **Kiểm tra Real-time:** Mở thêm 1 Client Bidder -> Đặt giá thử -> Nhìn máy Seller xem giá có nhảy số ngay lập tức không.

---

## ☁️ Bước 5: Kiểm tra GitHub & CI/CD
*   [ ] **Đẩy code lần cuối:** 
    `git add .`, `git commit -m "Final check before demo"`, `git push origin main`.
*   [ ] **Kiểm tra GitHub Actions:** Lên trang GitHub, vào tab **Actions**, đảm bảo commit cuối cùng có **Dấu tick xanh (✓)**. Điều này chứng minh code của bạn "sạch" và pass mọi bài test tự động.

---

## 🚩 Nếu gặp sự cố đột xuất?
1.  **Lỗi "Port already in use":** Dùng lệnh `taskkill` như tôi đã hướng dẫn để giải phóng cổng.
2.  **Lỗi "Giao diện bị lệch":** Do bạn chưa kéo dãn cửa sổ. Hãy nhớ ở màn hình Dashboard, bạn có thể **Maximize** cửa sổ lên toàn màn hình để show cho đẹp.
3.  **Lỗi "Không thấy sản phẩm":** Nhớ bấm nút **Làm mới** (Refresh) ở góc bảng để nạp dữ liệu từ Server.

**Bình tĩnh - Tự tin - Quyết thắng! 🚀**
