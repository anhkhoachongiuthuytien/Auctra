# Giải thích `BidServiceTest`

File test được giải thích: [BidServiceTest.java](D:/BaitaplonTest/src/test/java/com/auction/service/BidServiceTest.java)

## Mục đích chung

`BidServiceTest` dùng để kiểm tra logic đặt giá trong `BidService`.

Những gì file test này đang kiểm tra:
- bid hợp lệ
- bid vào auction không tồn tại
- bid khi auction chưa mở
- bid thấp hơn giá hiện tại
- bid bằng giá hiện tại
- nhiều bid hợp lệ liên tiếp

## Giải thích từng test case

### `testPlaceValidBid`

Test này làm các bước:
- mở auction bằng `auction.openAuction()`
- gọi `bidService.placeBid(...)` với giá `1200.0`

Sau đó kiểm tra:
- số lượng bid là `1`
- `currentPrice` đổi thành `1200.0`
- `winner` là `bidder1`

Ý nghĩa: xác nhận một lần bid hợp lệ sẽ cập nhật dữ liệu đúng.

### `testPlaceBidWhenAuctionNotFoundThrowsException`

Test này gọi:
- `bidService.placeBid("INVALID_ID", bidder1, 1200.0)`

Kỳ vọng:
- ném `AuctionException`

Ý nghĩa: xác nhận service không cho bid vào auction không tồn tại.

### `testPlaceBidWhenAuctionClosedThrowsException`

Test này không mở auction trước, rồi gọi:
- `bidService.placeBid(auction.getId(), bidder1, 1200.0)`

Kỳ vọng:
- ném `AuctionClosedException`

Ý nghĩa: xác nhận chỉ khi auction đang mở thì mới được đặt giá.

### `testPlaceLowerBidThrowsException`

Test này:
- mở auction
- gọi bid với giá `900.0`

Kỳ vọng:
- ném `InvalidBidException`

Ý nghĩa: xác nhận không cho bid thấp hơn giá hiện tại.

### `testPlaceEqualBidThrowsException`

Test này:
- mở auction
- gọi bid với giá `1000.0`, đúng bằng giá khởi điểm hiện tại

Kỳ vọng:
- ném `InvalidBidException`

Ý nghĩa: xác nhận bid phải lớn hơn giá hiện tại, không được bằng.

### `testPlaceMultipleValidBids`

Test này:
- mở auction
- đặt bid `1200.0`
- đặt tiếp bid `1500.0`

Sau đó kiểm tra:
- có `2` bid trong danh sách
- `currentPrice` là `1500.0`
- `winner` là `bidder2`

Ý nghĩa: xác nhận khi có nhiều bid hợp lệ liên tiếp, auction sẽ giữ lại giá cao nhất cuối cùng và cập nhật người thắng tạm thời đúng.

## Tóm tắt nhóm test

Có thể chia các test trong file này thành 3 nhóm:

- Bid hợp lệ:
  - `testPlaceValidBid`
  - `testPlaceMultipleValidBids`

- Auction không hợp lệ:
  - `testPlaceBidWhenAuctionNotFoundThrowsException`
  - `testPlaceBidWhenAuctionClosedThrowsException`

- Giá bid không hợp lệ:
  - `testPlaceLowerBidThrowsException`
  - `testPlaceEqualBidThrowsException`

## Ghi nhớ nhanh

Flow đặt bid đang được test:

```text
tìm auction
-> kiểm tra auction có tồn tại
-> kiểm tra auction có đang mở
-> kiểm tra giá bid mới có cao hơn currentPrice
-> tạo BidTransaction
-> cập nhật bids, currentPrice, winner
```

Các exception tương ứng:
- `AuctionException`: auction không tồn tại
- `AuctionClosedException`: auction chưa mở hoặc đã đóng
- `InvalidBidException`: giá bid không hợp lệ
