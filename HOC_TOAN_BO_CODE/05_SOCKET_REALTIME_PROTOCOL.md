# 05 - Socket, protocol và realtime

Phần này giải thích cách client và server giao tiếp qua Java Socket, cách request/response hoạt động, và vì sao app cập nhật realtime không cần bấm refresh.

## Bản đồ socket

```text
auction-common/protocol
  -> RequestType, AuctionRequest, AuctionResponse, AuctionEvent, DTO

auction-client
  -> SocketAuctionClientGateway
  -> ClientEventManager

auction-server
  -> AuctionSocketServer
  -> AuctionServerFacade
  -> BroadcastManager
```

---

## Request/Response socket chính

Luồng bình thường:

```text
Client thao tác UI
  -> Controller gọi Gateway
  -> SocketAuctionClientGateway tạo AuctionRequest
  -> ObjectOutputStream gửi request
  -> AuctionSocketServer đọc request
  -> processRequest switch theo RequestType
  -> gọi AuctionServerFacade
  -> Service xử lý
  -> server trả AuctionResponse
  -> client nhận response
```

Ví dụ đặt giá:

```text
SocketAuctionClientGateway.placeBid()
  -> new AuctionRequest(RequestType.PLACE_BID)
  -> put auctionId, bidderId, bidderName, bidderEmail, amount
  -> send(req)
```

Server:

```text
AuctionSocketServer.processRequest()
  -> case PLACE_BID
  -> tạo Bidder từ request
  -> facade.placeBid()
  -> BroadcastManager.broadcast(new AuctionEvent("NEW_BID", auctionId))
  -> AuctionResponse.ok()
```

---

## Protocol files

### RequestType.java

```text
Nhiệm vụ: Danh sách thao tác client có thể yêu cầu.
Ví dụ: LOGIN, REGISTER, LIST_AUCTIONS, CREATE_AUCTION, PLACE_BID.
```

### AuctionRequest.java

```text
Nhiệm vụ: Object client gửi sang server.
Chứa: RequestType và data map.
```

### AuctionResponse.java

```text
Nhiệm vụ: Object server trả về client.
Chứa: success, message, data.
```

### AuctionEvent.java

```text
Nhiệm vụ: Object server chủ động gửi về client qua realtime socket.
Ví dụ eventType: NEW_BID, AUCTION_STARTED, AUCTION_FINISHED.
```

### DtoMapper + DTO

```text
Nhiệm vụ: Chuyển object domain thành dữ liệu gọn để gửi qua mạng.
Các DTO chính: UserDto, AuctionDto, BidDto.
```

---

## SocketAuctionClientGateway.java

Nhiệm vụ:

```text
Ẩn toàn bộ chi tiết socket khỏi controller.
Controller chỉ gọi gateway.login(), gateway.placeBid(), gateway.listAuctions().
```

Các phần quan trọng:

```text
connect()
  -> mở socket chính tới host:port.

send(AuctionRequest)
  -> ghi object request.
  -> flush/reset ObjectOutputStream.
  -> đọc AuctionResponse.
  -> nếu lỗi kết nối thì reconnect và retry một lần.

startListeningForUpdates()
  -> mở socket thứ hai.
  -> gửi SUBSCRIBE_UPDATES.
  -> loop đọc AuctionEvent.
  -> Platform.runLater(ClientEventManager::fireUpdate).
```

Vì sao dùng `out.reset()`:

```text
ObjectOutputStream có cache object đã gửi.
reset() giúp lần gửi sau không bị dùng lại reference cũ, tránh client/server nhận dữ liệu stale.
```

---

## AuctionSocketServer.java

Nhiệm vụ:

```text
Mở ServerSocket.
Chấp nhận nhiều client.
Dùng thread pool xử lý connection.
Đọc AuctionRequest.
Trả AuctionResponse.
Nhận SUBSCRIBE_UPDATES để đăng ký realtime.
```

Các phần quan trọng:

```text
start()
  -> serverSocket = new ServerSocket(port)
  -> while running accept client
  -> threadPool.submit(handleClient)

handleClient()
  -> tạo ObjectInputStream/ObjectOutputStream
  -> đọc request trong vòng lặp
  -> nếu SUBSCRIBE_UPDATES thì BroadcastManager.addClient(out)
  -> request thường thì processRequest()

processRequest()
  -> switch theo RequestType
  -> gọi facade
  -> trả AuctionResponse
```

---

## BroadcastManager.java

Nhiệm vụ:

```text
Giữ danh sách ObjectOutputStream của client đã subscribe.
Khi server có thay đổi, gửi AuctionEvent cho tất cả client đó.
```

Các method:

```text
addClient(out)
removeClient(out)
broadcast(event)
```

Điểm cần hiểu:

```text
Danh sách clients là synchronizedList.
broadcast synchronized trên clients để duyệt an toàn.
Nếu gửi thất bại, client đó được xem là dead client và bị xóa.
```

---

## ClientEventManager.java

Nhiệm vụ:

```text
Quản lý các listener UI cần chạy khi có realtime event.
```

Luồng:

```text
AuctionDetailController.init()
  -> ClientEventManager.clearListeners()
  -> ClientEventManager.addListener(this::reloadAuction)

Socket listener nhận AuctionEvent
  -> Platform.runLater(ClientEventManager::fireUpdate)
  -> reloadAuction()
```

Vì sao cần `Platform.runLater`:

```text
JavaFX chỉ cho phép cập nhật UI trên JavaFX Application Thread.
Socket listener chạy ở background thread, nên phải dùng Platform.runLater.
```

---

## Dual-Socket là gì?

Trong dự án này, client socket mode dùng 2 kết nối:

```text
Socket 1: request/response
  -> gửi login, listAuctions, placeBid...
  -> nhận kết quả ngay.

Socket 2: realtime listener
  -> subscribe update.
  -> server chủ động push AuctionEvent.
```

Vì sao không dùng 1 socket?

```text
Nếu dùng cùng một socket cho request/response và push event, client dễ bị rối thứ tự đọc:
khi đang chờ response lại có event chen vào.
Tách socket giúp request/response rõ ràng, realtime event cũng rõ ràng.
```

---

## Các event realtime chính

```text
USER_REGISTERED
USER_UPDATED
AUCTION_CREATED
AUCTION_STARTED
AUCTION_FINISHED
AUCTION_CANCELED
AUCTION_PAID
NEW_BID
```

Khi event tới client, UI thường không xử lý chi tiết từng event mà reload dữ liệu mới từ server.

---

## Câu trả lời khi bị hỏi

### Client gửi dữ liệu qua mạng thế nào?

```text
Client tạo AuctionRequest chứa RequestType và dữ liệu, serialize qua ObjectOutputStream.
Server deserialize, switch theo RequestType, gọi Facade/Service rồi trả AuctionResponse.
```

### Realtime hoạt động thế nào?

```text
Client mở socket phụ gửi SUBSCRIBE_UPDATES.
Server lưu stream của client trong BroadcastManager.
Khi có bid/start/finish/cancel, server broadcast AuctionEvent.
Client nhận event ở background thread rồi dùng Platform.runLater để reload UI.
```

### Vì sao dùng DTO?

```text
DTO giúp dữ liệu truyền qua mạng gọn và kiểm soát được.
Client không cần nhận toàn bộ object nội bộ server hoặc dữ liệu nhạy cảm như password_hash.
```
