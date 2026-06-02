# Bộ tài liệu học toàn bộ code Auctra

Thư mục này chia dự án thành nhiều phần nhỏ để học. Mục tiêu là đọc code có hệ thống, hiểu từng file làm gì, ai gọi nó và nó gọi ai.

## Thứ tự đọc đề xuất

```text
0. 00_THU_TU_HOC_CHI_TIET.md
1. 01_COMMON_TUNG_FILE.md
2. 02_SERVER_TUNG_FILE.md
3. 04_DATABASE_DAO.md
4. 05_SOCKET_REALTIME_PROTOCOL.md
5. 03_CLIENT_TUNG_FILE.md
6. 06_TINH_NANG_NANG_CAO.md
7. 07_LUONG_NGHIEP_VU_DE_DOC_CODE.md
```

## Nếu cần học thật chi tiết

Sau khi đọc 7 file trên, đọc tiếp các file deep-dive:

```text
8. 08_COMMON_DEEP_DIVE.md
9. 09_SERVER_DEEP_DIVE.md
10. 10_CLIENT_DEEP_DIVE.md
11. 11_TINH_NANG_NANG_CAO_DEEP_DIVE.md
```

Các file deep-dive giải thích kỹ hơn theo method, input/output, lỗi có thể xảy ra và câu trả lời khi bị hỏi.

## Cách học mỗi file code

Khi mở một file `.java`, luôn tự hỏi:

```text
1. File này thuộc tầng nào?
2. Nó chịu trách nhiệm gì?
3. Ai gọi nó?
4. Nó gọi ai tiếp theo?
5. Nếu lỗi ở đây thì nghiệp vụ nào hỏng?
```

## Bản đồ dự án

```text
auction-common
  -> model, enum, exception, DAO interface, protocol, DTO, utility dùng chung

auction-server
  -> service nghiệp vụ, DAO SQLite/in-memory, database, socket server, scheduler

auction-client
  -> JavaFX app, FXML, controller, gateway, viewmodel, UI helper, CSS
```

Luồng tổng quát:

```text
FXML
  -> Controller
  -> ViewModel hoặc Gateway
  -> AuctionClientGateway
  -> LocalAuctionClientGateway hoặc SocketAuctionClientGateway
  -> AuctionServerFacade
  -> Service
  -> DAO
  -> SQLite / Model
```

## Các file quan trọng nhất

Nếu thời gian rất ít, học sâu các file này trước:

```text
auction-common/src/main/java/com/auction/model/auction/Auction.java
auction-server/src/main/java/com/auction/service/BidService.java
auction-server/src/main/java/com/auction/server/AuctionServerFacade.java
auction-server/src/main/java/com/auction/server/ServerContext.java
auction-client/src/main/java/com/auction/app/AppContext.java
auction-client/src/main/java/com/auction/client/SocketAuctionClientGateway.java
auction-server/src/main/java/com/auction/server/AuctionSocketServer.java
auction-server/src/main/java/com/auction/server/BroadcastManager.java
```

## Câu nói tổng quan

```text
Dự án Auctra tách thành common, server và client. Common chứa domain và protocol dùng chung.
Server xử lý nghiệp vụ, database, socket và realtime. Client chỉ hiển thị UI JavaFX và gọi server qua gateway.
Nhờ Gateway + Facade, controller không cần biết đang chạy local hay socket.
```
