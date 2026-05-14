# Hướng Dẫn Kiến Trúc Real-Time (Dual-Socket)

Trong dự án Auctra, để đạt được hiệu ứng dữ liệu nhảy liên tục mà người dùng không cần phải nhấn F5 (Làm mới), hệ thống đã được nâng cấp lên kiến trúc **Dual-Socket (2 luồng kết nối song song)**. 

Tài liệu này sẽ giải thích chi tiết cơ chế hoạt động để bạn tự tin giải thích với Giảng viên.

---

## 1. Vấn đề của mô hình cũ (Request-Reply)
Bình thường, Socket hoạt động theo kiểu **Đồng bộ (Synchronous)**:
1. Client gửi `AuctionRequest` (Ví dụ: Đặt giá).
2. Server nhận, xử lý, và trả về `AuctionResponse` (Thành công).
3. Luồng (Thread) kết thúc giao tiếp. Các Client khác (như Seller) **không hề biết** có sự thay đổi này trừ khi họ chủ động gửi một Request `LIST_AUCTIONS` để xin lại dữ liệu mới.

## 2. Giải pháp Dual-Socket
Thay vì đập bỏ code cũ (gây rủi ro lỗi phần mềm rất cao), chúng ta mở thêm **một ống nước thứ 2** chuyên dùng để "nghe ngóng":

### Phía Client (`SocketAuctionClientGateway.java`)
- Khi Client vừa kết nối, nó sẽ chạy một luồng ngầm (`Thread listenerThread`).
- Luồng này mở một `Socket` mới đến Server và gửi đi thông điệp đặc biệt: `RequestType.SUBSCRIBE_UPDATES`.
- Sau đó, luồng này treo (block) ở vòng lặp vô tận `eventIn.readObject()`, chỉ chực chờ Server ném dữ liệu xuống.

### Phía Server (`BroadcastManager.java` & `AuctionSocketServer.java`)
- Khi Server nhận được thông điệp `SUBSCRIBE_UPDATES`, nó biết rằng: *"À, luồng Socket này không dùng để gửi lệnh thông thường, nó dùng để nghe thông báo"*.
- Server đưa cái `ObjectOutputStream` này vào danh sách quản lý tập trung trong class `BroadcastManager` (là một List Thread-safe).
- Bất cứ khi nào có một lệnh làm **thay đổi dữ liệu** (Tạo phiên, Đặt giá, Bắt đầu, Kết thúc, Thanh toán), Server sẽ gọi lệnh:
  `BroadcastManager.broadcast(new AuctionEvent("NEW_BID"));`
- Hàm `broadcast` sẽ chạy qua toàn bộ danh sách ống nước phụ của các Client đang online và ném `AuctionEvent` xuống.

### Cập nhật Giao diện (`ClientEventManager.java`)
- Khi cái luồng ngầm của Client tóm được `AuctionEvent` từ Server, nó không thể tự mình chọc thẳng vào Giao diện (JavaFX nghiêm cấm Thread ngoài can thiệp UI).
- Nên nó gọi qua `ClientEventManager.fireUpdate()`.
- Các Controller (Admin, Seller, Bidder) đều đã đăng ký lắng nghe sự kiện này bằng hàm `Platform.runLater(this::refreshData);`. Nghĩa là: *Báo cho luồng giao diện chính biết, rảnh rỗi thì load lại bảng dữ liệu đi.*

## 3. Câu hỏi phòng thủ khi bị hỏi xoáy
**Hỏi:** Tại sao không dùng 1 Socket duy nhất cho cả Gửi lệnh và Nhận Push Notification?
**Đáp:** *"Thưa thầy, vì giao thức ObjectInputStream/ObjectOutputStream trong Java làm việc đồng bộ chặn (blocking). Nếu dùng 1 Socket, khi Client gọi `in.readObject()` chờ kết quả Đăng nhập, nó có thể vô tình đọc nhầm gói tin `AuctionEvent` bay tới cùng lúc, làm sụp đổ toàn bộ logic chuyển kiểu dữ liệu. Việc tách 2 Socket giúp em phân tách rạch ròi luồng Gửi-Nhận Chủ Động và luồng Lắng Nghe Bị Động, an toàn và dễ scale hơn rất nhiều."*
