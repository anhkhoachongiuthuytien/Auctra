# 🌐 Hướng dẫn Java Socket & Lập trình mạng trong Auctra

> Tài liệu vừa dạy lý thuyết vừa giải thích code thực tế.

---

## Mục lục

1. [Lập trình mạng là gì?](#1-lập-trình-mạng-là-gì)
2. [Mô hình Client-Server](#2-mô-hình-client-server)
3. [TCP Socket trong Java](#3-tcp-socket-trong-java)
4. [Kiến trúc Socket trong Auctra](#4-kiến-trúc-socket-trong-auctra)
5. [Server Socket — Lắng nghe kết nối](#5-server-socket)
6. [Client Socket — Kết nối tới server](#6-client-socket)
7. [Protocol — Giao thức giao tiếp](#7-protocol)
8. [Serialization — Truyền object qua mạng](#8-serialization)
9. [Thread Pool — Xử lý nhiều client](#9-thread-pool)
10. [DTO Pattern — Truyền dữ liệu an toàn](#10-dto-pattern)
11. [Gateway Pattern — Ẩn chi tiết mạng](#11-gateway-pattern)
12. [Reconnect & Error Handling](#12-reconnect--error-handling)
13. [Cách chạy & triển khai](#13-cách-chạy--triển-khai)

---

## 1. Lập trình mạng là gì?

Lập trình mạng (Network Programming) là viết chương trình có khả năng **giao tiếp giữa các máy tính** qua mạng (LAN, Internet).

Trong Auctra:
- **Server** chạy trên 1 máy, quản lý database
- **Client** chạy trên máy khác, hiển thị giao diện
- Hai bên giao tiếp qua **TCP Socket**

```
┌──────────────┐    TCP Socket     ┌──────────────┐
│   Client A   │◄─────────────────►│              │
│  (Bidder)    │    Port 9999      │   Server     │
└──────────────┘                   │              │
                                   │  Database    │
┌──────────────┐    TCP Socket     │  Services    │
│   Client B   │◄─────────────────►│  DAO Layer   │
│  (Seller)    │    Port 9999      │              │
└──────────────┘                   └──────────────┘
```

---

## 2. Mô hình Client-Server

### Khái niệm

| Thuật ngữ | Ý nghĩa | Trong Auctra |
|-----------|---------|-------------|
| **Server** | Máy cung cấp dịch vụ, luôn chạy chờ | `auction-server` module |
| **Client** | Máy sử dụng dịch vụ, kết nối tới server | `auction-client` module |
| **Port** | "Cổng" để phân biệt dịch vụ trên cùng 1 IP | `9999` |
| **IP Address** | Địa chỉ máy trên mạng | `localhost`, `192.168.1.x` |
| **Protocol** | Quy tắc giao tiếp giữa 2 bên | Request/Response objects |

### Flow tổng quan

```
1. Server khởi động → mở cổng 9999 → chờ client
2. Client khởi động → kết nối tới IP:9999
3. Client gửi Request → Server nhận → xử lý → trả Response
4. Lặp lại bước 3 cho mỗi thao tác
5. Client đóng kết nối khi thoát
```

---

## 3. TCP Socket trong Java

### TCP vs UDP

| | TCP | UDP |
|--|-----|-----|
| Kết nối | Có (connection-oriented) | Không |
| Tin cậy | Đảm bảo nhận đủ, đúng thứ tự | Có thể mất gói |
| Tốc độ | Chậm hơn | Nhanh hơn |
| Use case | Web, chat, đấu giá | Game, video streaming |

> Auctra dùng **TCP** vì cần đảm bảo mỗi lệnh đặt giá đều được server nhận chính xác.

### Hai class chính

```java
// PHÍA SERVER — lắng nghe kết nối
java.net.ServerSocket  // Mở cổng, chờ client kết nối
// Ví dụ: new ServerSocket(9999)

// PHÍA CLIENT — kết nối tới server
java.net.Socket        // Tạo kết nối TCP tới server
// Ví dụ: new Socket("192.168.1.5", 9999)
```

### Ví dụ đơn giản nhất

```java
// === SERVER ===
ServerSocket serverSocket = new ServerSocket(9999);  // Mở cổng
Socket client = serverSocket.accept();               // Chờ (block) cho đến khi có client
InputStream in = client.getInputStream();             // Đọc dữ liệu từ client
OutputStream out = client.getOutputStream();          // Gửi dữ liệu cho client

// === CLIENT ===
Socket socket = new Socket("localhost", 9999);        // Kết nối
OutputStream out = socket.getOutputStream();          // Gửi dữ liệu cho server
InputStream in = socket.getInputStream();             // Đọc dữ liệu từ server
```

---

## 4. Kiến trúc Socket trong Auctra

```
auction-common (shared)
├── protocol/
│   ├── RequestType.java      ← Enum: LOGIN, PLACE_BID, LIST_AUCTIONS...
│   ├── AuctionRequest.java   ← Object gửi từ Client → Server
│   ├── AuctionResponse.java  ← Object gửi từ Server → Client
│   ├── UserDto.java          ← Data Transfer Object cho User
│   ├── AuctionDto.java       ← DTO cho Auction
│   ├── BidDto.java           ← DTO cho Bid
│   └── DtoMapper.java        ← Chuyển đổi Model ↔ DTO

auction-server
├── server/
│   ├── ServerMain.java          ← Entry point: java -jar auction-server.jar
│   ├── AuctionSocketServer.java ← TCP Server, quản lý connections
│   ├── AuctionServerFacade.java ← Facade gom tất cả business logic
│   └── ServerContext.java       ← Khởi tạo DB, Services, DAOs

auction-client
├── client/
│   ├── AuctionClientGateway.java        ← Interface (abstract)
│   ├── LocalAuctionClientGateway.java   ← Gọi trực tiếp (cùng máy)
│   └── SocketAuctionClientGateway.java  ← Gọi qua TCP socket (khác máy)
```

---

## 5. Server Socket

File: `AuctionSocketServer.java`

### Khởi tạo

```java
public class AuctionSocketServer {
    private final int port;                      // Cổng lắng nghe
    private final AuctionServerFacade facade;     // Business logic
    private final ExecutorService threadPool;      // Pool thread xử lý client
    private volatile boolean running;              // Flag dừng server
    private ServerSocket serverSocket;

    public AuctionSocketServer(AuctionServerFacade facade, int port) {
        this.facade = facade;
        this.port = port;
        // Tạo pool 10 threads → hỗ trợ tối đa 10 client đồng thời
        this.threadPool = Executors.newFixedThreadPool(10);
    }
}
```

### Vòng lặp accept

```java
public void start() throws IOException {
    serverSocket = new ServerSocket(port);       // Mở cổng 9999
    running = true;
    System.out.println("[Server] Đang chạy tại cổng " + port);

    while (running) {                            // Vòng lặp vô hạn
        Socket clientSocket = serverSocket.accept();  // BLOCK — chờ client
        System.out.println("[Server] Client kết nối: "
            + clientSocket.getRemoteSocketAddress());

        // Giao cho thread pool xử lý, không block main thread
        threadPool.submit(() -> handleClient(clientSocket));
    }
}
```

> **`accept()` là blocking call:** Thread dừng lại tại đây cho đến khi có client mới kết nối.

### Xử lý 1 client

```java
private void handleClient(Socket socket) {
    try (
        // ObjectInputStream/ObjectOutputStream — đọc/ghi Java object
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
    ) {
        while (true) {  // Vòng lặp xử lý nhiều request trên cùng connection
            AuctionRequest request;
            try {
                request = (AuctionRequest) in.readObject();  // Đọc request
            } catch (Exception e) {
                break;  // Client ngắt kết nối → thoát vòng lặp
            }

            AuctionResponse response = processRequest(request);  // Xử lý
            out.writeObject(response);  // Gửi response
            out.flush();                // Đẩy data ra network
            out.reset();                // Reset cache để gửi object mới
        }
    } catch (IOException e) {
        System.err.println("[Server] Lỗi: " + e.getMessage());
    } finally {
        socket.close();
        System.out.println("[Server] Client đã ngắt kết nối.");
    }
}
```

### Xử lý request (switch-case)

```java
private AuctionResponse processRequest(AuctionRequest request) {
    try {
        switch (request.getType()) {
            case LOGIN:
                User user = facade.login(
                    request.get("email"),
                    request.get("password")
                );
                return AuctionResponse.ok(DtoMapper.toDto(user));

            case LIST_AUCTIONS:
                List<Auction> auctions = facade.listAuctions();
                List<AuctionDto> dtos = auctions.stream()
                    .map(DtoMapper::toDto).toList();
                return AuctionResponse.ok(dtos);

            case PLACE_BID:
                facade.placeBid(
                    request.get("auctionId"),
                    new Bidder(request.get("bidderId"), ...),
                    request.getDouble("amount")
                );
                return AuctionResponse.ok();

            // ... các case khác
            default:
                return AuctionResponse.error("Không hỗ trợ: " + request.getType());
        }
    } catch (Exception e) {
        return AuctionResponse.error(e.getMessage());
    }
}
```

### Shutdown hook

```java
// Trong ServerMain.java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    System.out.println("Đang tắt server...");
    socketServer.stop();  // Đóng ServerSocket, shutdown thread pool
}));
```

> Khi nhấn `Ctrl+C`, JVM gọi shutdown hook → server đóng sạch sẽ.

---

## 6. Client Socket

File: `SocketAuctionClientGateway.java`

### Kết nối persistent

```java
public class SocketAuctionClientGateway implements AuctionClientGateway {
    private final String host;   // IP server
    private final int port;      // Port server
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public void connect() {
        if (socket != null && !socket.isClosed()) return;  // Đã kết nối rồi

        socket = new Socket(host, port);                    // TCP handshake
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();  // Quan trọng! Gửi header để bên kia khởi tạo ObjectInputStream
        in = new ObjectInputStream(socket.getInputStream());
    }
}
```

> **Tại sao `out.flush()` ngay sau khi tạo?**
> `ObjectOutputStream` gửi 1 header khi khởi tạo. Bên server cần nhận header này
> trước khi tạo `ObjectInputStream`. Nếu không flush → **deadlock** (cả 2 bên đều chờ).

### Gửi request & nhận response

```java
private AuctionResponse send(AuctionRequest request) {
    connect();  // Đảm bảo đã kết nối

    try {
        out.writeObject(request);   // Serialize request → gửi qua mạng
        out.flush();
        out.reset();                // Xóa cache để lần sau gửi object mới

        // Đọc response (BLOCK cho đến khi server trả về)
        AuctionResponse response = (AuctionResponse) in.readObject();

        if (!response.isSuccess()) {
            throw new RuntimeException(response.getMessage());
        }
        return response;

    } catch (IOException | ClassNotFoundException e) {
        // Kết nối bị mất → reconnect và retry 1 lần
        disconnect();
        connect();
        // ... retry logic
    }
}
```

### Sử dụng Gateway (ví dụ login)

```java
@Override
public User login(String email, String password) {
    // 1. Tạo request object
    AuctionRequest req = new AuctionRequest(RequestType.LOGIN)
        .put("email", email)
        .put("password", password);

    // 2. Gửi qua socket & nhận response
    AuctionResponse resp = send(req);

    // 3. Chuyển DTO → Model
    return DtoMapper.toUser((UserDto) resp.getData());
}
```

---

## 7. Protocol — Giao thức giao tiếp

### Request Type (Enum)

```java
public enum RequestType {
    LOGIN,                    // Đăng nhập
    REGISTER,                 // Đăng ký
    GET_REGISTRATION_ROLES,   // Lấy danh sách role
    RESET_PASSWORD,           // Đổi mật khẩu
    LIST_AUCTIONS,            // Danh sách đấu giá
    LIST_AUCTIONS_FOR_SELLER, // Đấu giá của seller
    CREATE_AUCTION,           // Tạo phiên mới
    START_AUCTION,            // Bắt đầu đấu giá
    FINISH_AUCTION,           // Kết thúc
    CANCEL_AUCTION,           // Hủy
    MARK_AUCTION_PAID,        // Đánh dấu đã thanh toán
    PLACE_BID,                // Đặt giá
    LIST_USERS                // Admin: danh sách user
}
```

### Request Object

```java
public class AuctionRequest implements Serializable {
    private final RequestType type;           // Loại request
    private final Map<String, String> params; // Tham số key-value

    // Builder pattern: .put().put()...
    public AuctionRequest put(String key, String value) {
        params.put(key, value);
        return this;  // ← Cho phép chaining
    }
}
```

### Response Object

```java
public class AuctionResponse implements Serializable {
    private final boolean success;   // Thành công hay lỗi
    private final String message;    // Thông báo lỗi (nếu có)
    private final Object data;       // Dữ liệu trả về (UserDto, List<AuctionDto>...)

    // Factory methods
    public static AuctionResponse ok(Object data) { ... }
    public static AuctionResponse ok() { ... }
    public static AuctionResponse error(String message) { ... }
}
```

### Flow hoàn chỉnh 1 request

```
Client                          Network                         Server
  │                                │                               │
  │ AuctionRequest(PLACE_BID)      │                               │
  │ {auctionId, bidderId, amount}  │                               │
  │──── writeObject() ────────────►│──── readObject() ────────────►│
  │                                │                               │
  │                                │        processRequest()       │
  │                                │        facade.placeBid()      │
  │                                │        → Service → DAO → DB   │
  │                                │                               │
  │                                │◄──── writeObject() ──────────│
  │◄──── readObject() ────────────│  AuctionResponse.ok()          │
  │                                │                               │
  │ Hiển thị "Đặt giá thành công" │                               │
```

---

## 8. Serialization — Truyền object qua mạng

### Serialization là gì?

Chuyển Java object → byte stream để truyền qua mạng (hoặc lưu file).

```java
// Tất cả class truyền qua socket phải implement Serializable
public class AuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L;  // Version control
    // ...
}
```

### ObjectOutputStream / ObjectInputStream

```java
// GỬI object
ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
out.writeObject(request);   // Java tự serialize object → bytes
out.flush();

// NHẬN object
ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
AuctionRequest req = (AuctionRequest) in.readObject();  // bytes → object
```

### `out.reset()` — Tại sao cần?

```java
out.writeObject(response);
out.flush();
out.reset();  // ← QUAN TRỌNG!
```

> `ObjectOutputStream` cache các object đã gửi. Nếu gửi cùng object lần 2,
> nó chỉ gửi reference thay vì data mới. `reset()` xóa cache để lần sau
> gửi data mới nhất.

---

## 9. Thread Pool — Xử lý nhiều client

### Vấn đề

Nếu server chỉ có 1 thread → chỉ phục vụ 1 client → các client khác phải chờ.

### Giải pháp: ExecutorService

```java
// Tạo pool 10 threads
ExecutorService threadPool = Executors.newFixedThreadPool(10);

// Mỗi client kết nối → giao cho 1 thread trong pool
while (running) {
    Socket client = serverSocket.accept();
    threadPool.submit(() -> handleClient(client));
    //                  ↑ Lambda chạy trên thread riêng
}
```

### Sơ đồ xử lý

```
Main Thread (accept loop)
    │
    ├── Client A kết nối → Thread 1 xử lý
    ├── Client B kết nối → Thread 2 xử lý
    ├── Client C kết nối → Thread 3 xử lý
    │   ...
    └── Client J kết nối → Thread 10 xử lý
        Client K kết nối → CHỜ thread rảnh
```

---

## 10. DTO Pattern — Truyền dữ liệu an toàn

### Vấn đề

Model objects (Auction, User) có thể chứa logic phức tạp, circular reference,
hoặc không implement Serializable. Không nên gửi trực tiếp qua mạng.

### Giải pháp: Data Transfer Object

```java
// DTO — chỉ chứa data, implement Serializable
public class UserDto implements Serializable {
    private String id;
    private String username;
    private String email;
    private String role;
    // getters, setters
}

// Mapper — chuyển đổi Model ↔ DTO
public class DtoMapper {
    public static UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getClass().getSimpleName());
        return dto;
    }

    public static User toUser(UserDto dto) {
        return switch (dto.getRole()) {
            case "Admin"  -> new Admin(dto.getId(), dto.getUsername(), dto.getEmail());
            case "Seller" -> new Seller(dto.getId(), dto.getUsername(), dto.getEmail());
            default       -> new Bidder(dto.getId(), dto.getUsername(), dto.getEmail());
        };
    }
}
```

---

## 11. Gateway Pattern — Ẩn chi tiết mạng

### Interface chung

```java
public interface AuctionClientGateway {
    User login(String email, String password);
    List<Auction> listAuctions();
    void placeBid(String auctionId, Bidder bidder, double amount);
    // ... 13 methods tổng cộng
}
```

### 2 Implementation

```java
// Chế độ LOCAL — chạy cùng máy, gọi trực tiếp
public class LocalAuctionClientGateway implements AuctionClientGateway {
    public User login(String email, String password) {
        return authService.login(email, password);  // Gọi thẳng
    }
}

// Chế độ SOCKET — chạy khác máy, gọi qua mạng
public class SocketAuctionClientGateway implements AuctionClientGateway {
    public User login(String email, String password) {
        AuctionRequest req = new AuctionRequest(RequestType.LOGIN)
            .put("email", email).put("password", password);
        AuctionResponse resp = send(req);  // Gửi qua TCP
        return DtoMapper.toUser((UserDto) resp.getData());
    }
}
```

### UI không biết đang chạy ở chế độ nào

```java
// Controller chỉ gọi qua interface
AuctionClientGateway gateway = appContext.getGateway();
User user = gateway.login(email, password);
// Không cần biết đang LOCAL hay SOCKET!
```

---

## 12. Reconnect & Error Handling

```java
// SocketAuctionClientGateway.send()
private AuctionResponse send(AuctionRequest request) {
    connect();
    try {
        out.writeObject(request);
        // ...
    } catch (IOException e) {
        // Kết nối bị mất!
        disconnect();    // Đóng socket cũ
        connect();       // Tạo socket mới

        // Retry 1 lần
        out.writeObject(request);
        // ...
    }
}
```

---

## 13. Cách chạy & triển khai

### Chế độ LOCAL (đơn giản)

```bash
cd auction-client
mvn javafx:run
# Client tự kết nối database trong cùng máy
```

### Chế độ SOCKET (2 máy)

**Máy A (Server):**
```bash
java -jar auction-server.jar
# Output: [Server] Đang chạy tại cổng 9999
```

**Máy B (Client):**
```bash
cd auction-client
mvn javafx:run "-Djavafx.args=--socket 192.168.1.100 9999"
#                                       ▲ IP máy A    ▲ Port
```

### Sơ đồ triển khai

```
Máy A (Server)                        Máy B (Client)
┌─────────────────────┐               ┌─────────────────────┐
│ auction-server.jar  │               │ auction-client      │
│ ┌─────────────────┐ │    TCP/IP     │ ┌─────────────────┐ │
│ │ SocketServer    │◄├──────────────►┤ │ SocketGateway   │ │
│ │ port 9999       │ │  192.168.x.x  │ │                 │ │
│ └────────┬────────┘ │               │ └────────┬────────┘ │
│ ┌────────▼────────┐ │               │ ┌────────▼────────┐ │
│ │ Facade→Service  │ │               │ │ Controller      │ │
│ │ →DAO→SQLite     │ │               │ │ →ViewModel→UI   │ │
│ └─────────────────┘ │               │ └─────────────────┘ │
│ auction-system.db   │               │ (không có DB)       │
└─────────────────────┘               └─────────────────────┘
```

---

## Tổng kết

| Khái niệm | Class trong Auctra | Vai trò |
|-----------|-------------------|---------|
| ServerSocket | `AuctionSocketServer` | Lắng nghe cổng 9999 |
| Socket | `SocketAuctionClientGateway` | Kết nối TCP |
| Serialization | `AuctionRequest/Response` | Truyền object qua mạng |
| Thread Pool | `Executors.newFixedThreadPool(10)` | 10 client đồng thời |
| Protocol | `RequestType` enum | 13 loại request |
| DTO | `UserDto, AuctionDto, BidDto` | Data transfer objects |
| Gateway | `AuctionClientGateway` interface | Ẩn LOCAL/SOCKET |
| Facade | `AuctionServerFacade` | Gom business logic |
