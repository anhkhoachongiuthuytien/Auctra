# Ngân Hàng Câu Hỏi Bảo Vệ Tiến Độ Dự Án

Dưới đây là danh sách các câu hỏi Giảng viên có thể hỏi trong lúc bạn demo hệ thống Auctra. Các câu hỏi được chia theo từng mảng kiến thức.

## 1. Mảng Thiết Kế Mạng (Socket & Client-Server)

**Q1: Tại sao em lại tách project thành 3 module (client, server, common) mà không để chung?**
*Trả lời:* Để đảm bảo nguyên lý Separation of Concerns (Tách biệt mối quan tâm). `common` chứa các Model và DTO để cả client/server cùng hiểu. Việc này giúp Client cực kỳ nhẹ vì không chứa logic database, đồng thời Server bảo mật hơn vì không phơi bày source code nghiệp vụ cho Client.

**Q2: Client gửi request cho Server bằng cơ chế nào? Dữ liệu truyền đi có cấu trúc ra sao?**
*Trả lời:* Dùng `Java Socket`. Khi Client thực hiện thao tác, Gateway sẽ bọc dữ liệu thành đối tượng `AuctionRequest` và Serialize (tuần tự hóa) truyền qua `ObjectOutputStream`. Server nhận được sẽ Deserialize lại thành object để đọc loại Request (`RequestType`) và dữ liệu đi kèm.

**Q3: Nếu có 100 người dùng cùng kết nối vào Server thì Server có bị sập hay bị treo không? Em xử lý ra sao?**
*Trả lời:* Em sử dụng `ExecutorService` (Thread Pool) trên Server. Khi có kết nối mới `serverSocket.accept()`, Server sẽ quăng kết nối đó cho một Thread trong pool xử lý. Nhờ vậy Server không bị block và có thể phục vụ nhiều người đồng thời.

**Q4: Hệ thống làm sao để tự động cập nhật giá mới (Real-time) mà người dùng không cần nhấn F5?**
*Trả lời:* Em sử dụng kiến trúc **Dual-Socket**. Khi khởi động, Client mở thêm một luồng Socket thứ 2 chạy ngầm chỉ để nghe sự kiện (Push Notification). Ở Server, em dùng `BroadcastManager` giữ danh sách các luồng nghe này. Khi có ai đặt giá, Server sẽ gọi lệnh `broadcast` bắn tín hiệu `AuctionEvent` về cho tất cả Client.

## 2. Mảng Cơ Sở Dữ Liệu (SQLite & DAO)

**Q4: Tại sao em dùng SQLite mà không dùng MySQL hay SQL Server?**
*Trả lời:* Vì đây là ứng dụng dạng Desktop App. SQLite là database nhúng (embedded database), chỉ cần 1 file `.db`, không cần cài đặt phần mềm server cồng kềnh, giúp việc chạy source code ở máy thầy cô dễ dàng hơn mà không phải cấu hình.

**Q5: Pattern DAO (Data Access Object) em dùng có tác dụng gì?**
*Trả lời:* DAO giúp em tách biệt hoàn toàn các câu lệnh SQL (`INSERT`, `SELECT`) khỏi Business Logic. Khi thao tác ở Service, em chỉ cần gọi `userDao.save(user)` thay vì viết trực tiếp SQL. Nếu sau này muốn chuyển sang MySQL, em chỉ cần viết lại tầng DAO mà không cần sửa Logic Code.

## 3. Mảng Bảo Mật (Security)

**Q6: Trong database, em lưu mật khẩu dạng gì? Lộ database có bị hack tài khoản không?**
*Trả lời:* Em không lưu plain-text (chữ thường), cũng không dùng MD5 vì đã lỗi thời. Em dùng thuật toán **PBKDF2** (Password-Based Key Derivation Function 2) với SHA-256, chạy 65,536 vòng lặp và cộng thêm `Salt` (chuỗi ngẫu nhiên 16 bytes). Kể cả khi hacker lấy được database, việc dò mật khẩu bằng Rainbow Table là gần như không thể.

**Q7: Cụm từ `constantTimeEquals` trong code xác thực của em có ý nghĩa gì?**
*Trả lời:* Nó dùng để chống tấn công **Timing Attack**. Hàm so sánh chuỗi thông thường (như `String.equals()`) sẽ thoát ra ngay khi gặp ký tự sai đầu tiên. Kẻ gian có thể đo thời gian phản hồi để đoán từng chữ của mật khẩu. Hàm `constantTimeEquals` luôn duyệt qua tất cả ký tự dù đúng hay sai, khiến thời gian thực thi là cố định.

## 4. Mảng OOP & Design Patterns

**Q8: Em đã áp dụng những Design Pattern nào trong dự án?**
*Trả lời:* 
1. **Observer Pattern:** Để thông báo sự kiện giá thay đổi (Bidding).
2. **Gateway Pattern:** Lớp `SocketAuctionClientGateway` giúp che giấu đi sự phức tạp của Socket. Controller chỉ gọi `gateway.placeBid()` như gọi một hàm bình thường.
3. **Facade Pattern:** Lớp `AuctionServerFacade` gom nhóm các Service lại thành 1 điểm truy cập duy nhất cho hệ thống Socket.
4. **Data Transfer Object (DTO):** Đóng gói dữ liệu tối giản để truyền qua mạng thay vì gửi nguyên cục Model nặng nề.

**Q9: Việc kế thừa (Inheritance) được áp dụng ở đâu?**
*Trả lời:* Áp dụng ở hệ thống phân quyền User. Lớp cha là `User`. Các lớp con `Bidder`, `Seller`, `Admin` kế thừa `User` và bổ sung thêm các tính chất riêng biệt của từng vai trò.

## 5. Mảng Giao Diện (JavaFX)

**Q10: Làm sao em chuyển đổi màn hình (Navigation) mượt mà được?**
*Trả lời:* Em thiết kế lớp `SceneNavigator`. Mọi màn hình FXML đều được load lại và gắn vào một `Scene` duy nhất (`stage.setScene(...)`). Sau khi gắn, em dùng `FadeTransition` của JavaFX Animation để tạo hiệu ứng mờ dần (fade in), giúp giao diện không bị giật cục.

**Q11: Em sử dụng CSS như thế nào trong JavaFX?**
*Trả lời:* Em dùng file `app.css` cấu trúc theo tiêu chuẩn **Google Material Design**. Mọi Button, Label, TextField trong FXML đều được gán `styleClass` thay vì hardcode màu sắc trực tiếp. Nhờ đó giao diện đảm bảo tính đồng nhất rất cao.
