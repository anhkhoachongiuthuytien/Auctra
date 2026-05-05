# Giải thích `ConcurrentBidTest`

File test được giải thích: [ConcurrentBidTest.java](D:/BaitaplonTest/src/test/java/com/auction/concurrency/ConcurrentBidTest.java)

## Mục đích chung

`ConcurrentBidTest` dùng để kiểm tra phần xử lý đa luồng trong `BidService`.

Mục tiêu của test này:
- mô phỏng nhiều thread cùng đặt bid vào cùng một auction
- kiểm tra xem phần `synchronized` trong `BidService` có giúp tránh lỗi race condition cơ bản hay không

## Test case hiện có

### `testTwoThreadsPlaceBidsConcurrently`

Test này làm các bước:

1. Tạo `InMemoryAuctionDao`
2. Tạo `BidService`
3. Tạo một `Auction`
4. Mở auction bằng `auction.openAuction()`
5. Lưu auction vào DAO
6. Tạo 2 bidder:
   - `bidder1`
   - `bidder2`
7. Tạo 2 thread:
   - thread 1 bid `1200.0`
   - thread 2 bid `1500.0`
8. Chạy cả 2 thread gần như cùng lúc
9. Đợi cả 2 thread hoàn thành bằng `join()`

Sau đó test kiểm tra:
- danh sách `errors` rỗng, tức là không thread nào ném lỗi bất thường
- số lượng bid trong auction là `2`
- `currentPrice` cuối cùng là `1500.0`
- `winner` là `bidder2`

## Ý nghĩa của test này

Test này muốn xác nhận rằng:
- khi nhiều thread cùng vào `placeBid(...)`
- hệ thống vẫn cập nhật dữ liệu theo cách nhất quán
- bid cao hơn cuối cùng sẽ trở thành giá hiện tại
- không bị lỗi kiểu 2 thread cùng sửa dữ liệu theo cách làm hỏng trạng thái auction

## Vì sao test này liên quan đến `synchronized`

Trong [BidService.java](D:/BaitaplonTest/src/main/java/com/auction/service/BidService.java), phần đặt bid đang được bảo vệ bằng:

```java
synchronized (auction) {
    ...
}
```

Ý nghĩa:
- tại cùng một thời điểm, chỉ một thread được xử lý phần kiểm tra và cập nhật của auction đó
- tránh trường hợp 2 thread cùng đọc một `currentPrice` cũ rồi cùng update sai

## Điều test này chưa kiểm tra sâu

Test hiện tại là case cơ bản nhất. Nó chưa kiểm tra:
- rất nhiều thread cùng bid một lúc
- thread nào thắng nếu 2 thread bid mức gần nhau trong timing phức tạp hơn
- hành vi khi một thread bid không hợp lệ còn thread khác bid hợp lệ

Nhưng nó đủ để chứng minh:
- phần đồng bộ cơ bản đã được thêm vào
- hệ thống có thể xử lý concurrency ở mức đầu tiên

## Ghi nhớ nhanh

Flow đang được test:

```text
2 thread
-> cùng gọi placeBid(...)
-> BidService khóa theo auction
-> từng thread lần lượt cập nhật bid
-> auction giữ currentPrice và winner cuối cùng đúng
```

## Tóm tắt

`ConcurrentBidTest` là bước tiếp theo sau `BidServiceTest`:
- `BidServiceTest` kiểm tra logic đơn luồng
- `ConcurrentBidTest` kiểm tra logic đó khi có nhiều thread cùng chạy
