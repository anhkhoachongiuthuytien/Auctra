# Giải thích `AuctionServiceTest`

File test được giải thích: [AuctionServiceTest.java](D:/BaitaplonTest/src/test/java/com/auction/service/AuctionServiceTest.java)

## Mục đích chung

`AuctionServiceTest` dùng để kiểm tra tầng service của auction hoạt động đúng với model `Auction` và `InMemoryAuctionDao`.

Service này chịu trách nhiệm:
- tạo auction
- bắt đầu auction
- kết thúc auction
- hủy auction
- đánh dấu auction đã thanh toán
- ném lỗi khi không tìm thấy auction

## Giải thích từng test case

### `testCreateAuction`

Test này gọi `auctionService.createAuction(item, seller)` rồi kiểm tra:
- auction được tạo ra không null
- auction có `id`
- `item` và `seller` được gán đúng
- trạng thái ban đầu là `CREATED`
- auction được lưu thật vào `auctionDao`

Ý nghĩa: xác nhận service không chỉ tạo object mà còn lưu object đó vào DAO.

### `testStartAuction`

Test này:
- tạo một auction mới
- gọi `auctionService.startAuction(auction.getId())`

Sau đó kiểm tra:
- trạng thái auction chuyển sang `OPEN`

Ý nghĩa: xác nhận service tìm đúng auction theo id và gọi đúng `auction.start()`.

### `testFinishAuction`

Test này:
- tạo auction
- start auction
- gọi `auctionService.finishAuction(auction.getId())`

Sau đó kiểm tra:
- trạng thái là `FINISHED`

Ý nghĩa: xác nhận service có thể kết thúc một auction đang mở.

### `testCancelAuction`

Test này:
- tạo auction
- gọi `auctionService.cancelAuction(auction.getId())`

Sau đó kiểm tra:
- trạng thái là `CANCELED`

Ý nghĩa: xác nhận service có thể hủy auction từ trạng thái ban đầu `CREATED`.

### `testMarkAuctionPaid`

Test này:
- tạo auction
- start
- finish
- gọi `auctionService.markAuctionPaid(auction.getId())`

Sau đó kiểm tra:
- trạng thái là `PAID`

Ý nghĩa: xác nhận service có thể đưa auction qua flow đầy đủ đến bước thanh toán.

### `testStartAuctionWhenAuctionNotFoundThrowsException`

Test này gọi:
- `auctionService.startAuction("INVALID_ID")`

Kỳ vọng:
- ném `AuctionException`

Ý nghĩa: xác nhận service xử lý đúng trường hợp không tìm thấy auction.

### `testFinishAuctionWhenAuctionNotFoundThrowsException`

Test này gọi:
- `auctionService.finishAuction("INVALID_ID")`

Kỳ vọng:
- ném `AuctionException`

Ý nghĩa: xác nhận service không cho finish một auction không tồn tại.

### `testCancelAuctionWhenAuctionNotFoundThrowsException`

Test này gọi:
- `auctionService.cancelAuction("INVALID_ID")`

Kỳ vọng:
- ném `AuctionException`

Ý nghĩa: xác nhận service không cho hủy một auction không tồn tại.

### `testMarkAuctionPaidWhenAuctionNotFoundThrowsException`

Test này gọi:
- `auctionService.markAuctionPaid("INVALID_ID")`

Kỳ vọng:
- ném `AuctionException`

Ý nghĩa: xác nhận service không cho đánh dấu thanh toán với một auction không tồn tại.

### `testFinishAuctionFromWrongStateThrowsException`

Test này:
- tạo auction mới
- gọi `finishAuction()` ngay, không `start()` trước

Kỳ vọng:
- ném `AuctionException`

Ý nghĩa: xác nhận service vẫn tuân theo rule trạng thái của model `Auction`.

### `testMarkAuctionPaidFromWrongStateThrowsException`

Test này:
- tạo auction mới
- gọi `markAuctionPaid()` ngay

Kỳ vọng:
- ném `AuctionException`

Ý nghĩa: xác nhận service không thể bỏ qua rule rằng auction phải `FINISHED` trước khi chuyển sang `PAID`.

## Tóm tắt nhóm test

Có thể chia các test trong file này thành 3 nhóm:

- Kiểm tra tạo và cập nhật trạng thái đúng:
  - `testCreateAuction`
  - `testStartAuction`
  - `testFinishAuction`
  - `testCancelAuction`
  - `testMarkAuctionPaid`

- Kiểm tra không tìm thấy auction:
  - `testStartAuctionWhenAuctionNotFoundThrowsException`
  - `testFinishAuctionWhenAuctionNotFoundThrowsException`
  - `testCancelAuctionWhenAuctionNotFoundThrowsException`
  - `testMarkAuctionPaidWhenAuctionNotFoundThrowsException`

- Kiểm tra chuyển trạng thái sai:
  - `testFinishAuctionFromWrongStateThrowsException`
  - `testMarkAuctionPaidFromWrongStateThrowsException`

## Ghi nhớ nhanh

Service này làm 2 việc chính:

1. Tìm auction theo `auctionId`
2. Nếu tìm thấy thì gọi đúng method của model `Auction`

Nếu không tìm thấy:
- ném `AuctionException("Auction not found")`

Nếu tìm thấy nhưng trạng thái sai:
- model `Auction` sẽ ném `AuctionException`
