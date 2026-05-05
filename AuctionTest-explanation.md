# Giải thích `AuctionTest`

File test được giải thích: [AuctionTest.java](D:/BaitaplonTest/src/test/java/com/auction/model/auction/AuctionTest.java)

## Mục đích chung

`AuctionTest` dùng để kiểm tra logic nghiệp vụ trong model `Auction`, gồm:
- trạng thái ban đầu
- chuyển trạng thái đúng
- chuyển trạng thái sai
- quy tắc đặt giá

## Giải thích từng test case

### `testConstructorInitialState`

Kiểm tra sau khi tạo `Auction` mới thì:
- `id` đúng
- `item` đúng
- `seller` đúng
- `currentPrice` bằng giá khởi điểm của item
- `status` mặc định là `CREATED`
- `isOpen()` là `false`
- danh sách bid rỗng
- `winner` chưa có

Ý nghĩa: xác nhận constructor khởi tạo object đúng trạng thái ban đầu.

### `testStartAuction`

Gọi `auction.start()` rồi kiểm tra:
- `status` chuyển sang `OPEN`
- `isOpen()` trả về `true`

Ý nghĩa: xác nhận auction có thể được mở từ trạng thái `CREATED`.

### `testFinishAuction`

Gọi:
- `auction.start()`
- `auction.finish()`

Sau đó kiểm tra:
- `status` là `FINISHED`
- `isOpen()` là `false`

Ý nghĩa: xác nhận auction đang mở có thể kết thúc đúng.

### `testCancelAuctionFromCreated`

Gọi `auction.cancel()` ngay khi auction còn ở `CREATED`, sau đó kiểm tra:
- `status` là `CANCELED`
- `isOpen()` là `false`

Ý nghĩa: xác nhận auction có thể hủy trước khi mở.

### `testCancelAuctionFromOpen`

Gọi:
- `auction.start()`
- `auction.cancel()`

Sau đó kiểm tra:
- `status` là `CANCELED`
- `isOpen()` là `false`

Ý nghĩa: xác nhận auction có thể hủy khi đang mở.

### `testMarkPaidFromFinished`

Gọi:
- `auction.start()`
- `auction.finish()`
- `auction.markPaid()`

Sau đó kiểm tra:
- `status` là `PAID`
- `isOpen()` là `false`

Ý nghĩa: xác nhận chỉ sau khi kết thúc thì auction mới được đánh dấu đã thanh toán.

### `testStartAuctionFromWrongStatusThrowsException`

Gọi `auction.start()` 2 lần.

Lần đầu:
- chuyển từ `CREATED` sang `OPEN`

Lần hai:
- kỳ vọng ném `AuctionException`

Ý nghĩa: xác nhận không được mở lại auction khi nó đã đang ở trạng thái khác `CREATED`.

### `testFinishAuctionFromWrongStatusThrowsException`

Không gọi `start()` trước, mà gọi thẳng `auction.finish()`.

Kỳ vọng:
- ném `AuctionException`

Ý nghĩa: xác nhận không được kết thúc auction khi nó chưa được mở.

### `testMarkPaidFromWrongStatusThrowsException`

Gọi:
- `auction.start()`

Sau đó gọi:
- `auction.markPaid()`

Kỳ vọng:
- ném `AuctionException`

Ý nghĩa: xác nhận không được đánh dấu đã thanh toán khi auction chưa ở trạng thái `FINISHED`.

### `testAddBidWhenAuctionIsOpen`

Gọi:
- `auction.start()`
- tạo `BidTransaction` hợp lệ với giá `1200.0`
- `auction.addBid(bid)`

Sau đó kiểm tra:
- số lượng bids là `1`
- `currentPrice` cập nhật thành `1200.0`
- `winner` là bidder vừa đặt giá

Ý nghĩa: xác nhận luồng đặt giá hợp lệ hoạt động đúng.

### `testAddMultipleValidBids`

Gọi:
- `auction.start()`
- thêm bid `1200.0`
- thêm bid `1500.0`

Sau đó kiểm tra:
- có `2` bid
- `currentPrice` là `1500.0`
- `winner` là bidder của bid cao nhất cuối cùng

Ý nghĩa: xác nhận auction cập nhật người đang dẫn đầu và giá hiện tại sau mỗi bid hợp lệ.

### `testAddBidWhenAuctionIsClosedThrowsException`

Không gọi `start()`, tạo bid rồi gọi `auction.addBid(bid)`.

Kỳ vọng:
- ném `AuctionClosedException`

Ý nghĩa: xác nhận auction chỉ nhận bid khi đang `OPEN`.

### `testAddNullBidThrowsException`

Gọi:
- `auction.start()`
- `auction.addBid(null)`

Kỳ vọng:
- ném `InvalidBidException`

Ý nghĩa: xác nhận input `null` là không hợp lệ.

### `testAddLowerBidThrowsException`

Gọi:
- `auction.start()`
- tạo bid có giá `900.0`

Kỳ vọng:
- ném `InvalidBidException`

Ý nghĩa: xác nhận không cho bid thấp hơn giá hiện tại.

### `testAddEqualBidThrowsException`

Gọi:
- `auction.start()`
- tạo bid có giá `1000.0`, bằng giá hiện tại ban đầu

Kỳ vọng:
- ném `InvalidBidException`

Ý nghĩa: xác nhận bid phải lớn hơn giá hiện tại, không được bằng.

### `testToString`

Gọi `auction.toString()` và kiểm tra:
- kết quả không null
- có chứa chữ `Auction`
- có chứa `A001`
- có chứa chữ `status`

Ý nghĩa: xác nhận `toString()` trả về chuỗi có thông tin cơ bản để debug.

## Tóm tắt nhóm test

Có thể chia các test thành 4 nhóm:
- Khởi tạo object: `testConstructorInitialState`
- Chuyển trạng thái hợp lệ: `testStartAuction`, `testFinishAuction`, `testCancelAuctionFromCreated`, `testCancelAuctionFromOpen`, `testMarkPaidFromFinished`
- Chuyển trạng thái sai: `testStartAuctionFromWrongStatusThrowsException`, `testFinishAuctionFromWrongStatusThrowsException`, `testMarkPaidFromWrongStatusThrowsException`
- Quy tắc bid: các test liên quan đến `addBid`

## Ghi nhớ nhanh

Flow trạng thái đang được test:

```text
CREATED -> OPEN -> FINISHED -> PAID
CREATED -> CANCELED
OPEN -> CANCELED
```

Rule bid đang được test:
- chỉ bid khi `OPEN`
- bid không được null
- bid phải lớn hơn `currentPrice`
