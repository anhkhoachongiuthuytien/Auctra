# UI Upgrade Review — BidMaster Auctions

Tài liệu này ghi lại toàn bộ thay đổi giao diện trong đợt nâng cấp từ UI cũ (tham chiếu `image_9.png`, `image_10.png`) sang thiết kế mục tiêu "BidMaster Auctions" (tham chiếu `image_11.png`).

---

## 1. Tổng quan

| Hạng mục | Trước | Sau |
|---|---|---|
| Thương hiệu | "Auction System" — plain label | "BidMaster Auctions" + logo "BM" gradient + subtitle |
| Nền | Linear gradient nhạt | Flat `#f3f5f9` hiện đại hơn |
| Bố cục | Padding đều, không phân vùng | App bar + card (shadow nâng lên) |
| Nút Refresh / Logout / Finish | Hàng nút ở chân trang | Chuyển vào App Bar dạng icon button |
| User menu | Không có | Avatar tròn hiển thị chữ cái đầu |
| Bảng dữ liệu | Bảng trần, header xám | Card có shadow, hover row, row selected highlight xanh |
| Status | Plain text | Badge bo tròn mã màu (OPEN / RUNNING / FINISHED / PAID / CANCELED) |
| Tìm kiếm / Lọc | Không có | Filter tabs (All / Active / Finished) + Search field |
| Thông báo | Label tĩnh ở cuối trang | Toast slide-in góc phải, tự ẩn |
| Loading | Không có | Spinner overlay mờ nền khi refresh |
| Xác nhận | Không có | Modal dialog có backdrop mờ, nút "Xác nhận" đỏ cho hành động nguy hiểm |
| Chuyển scene | Cắt cứng | Fade transition 220ms |
| Co giãn | Fixed 980×620 | 1180×760 mặc định, min 960×620, table `VBox.vgrow=ALWAYS` |

---

## 2. Danh sách file thay đổi

### Files mới

| File | Mục đích |
|---|---|
| `src/main/java/com/auction/util/UiEffects.java` | Helper tái sử dụng cho toast, loading overlay, confirm dialog, fade transition |
| `UI_UPGRADE_REVIEW.md` | Tài liệu review này |

### Files cập nhật

| File | Loại thay đổi |
|---|---|
| `src/main/resources/css/app.css` | Viết lại toàn bộ theme |
| `src/main/resources/fxml/login-view.fxml` | Bọc trong `StackPane` root, thêm brand header, form nằm trong card |
| `src/main/resources/fxml/auction-list-view.fxml` | Bọc trong `StackPane`, thêm app bar, filter tabs, search, action column |
| `src/main/resources/fxml/admin-view.fxml` | Bọc trong `StackPane`, thêm app bar, actions gom vào header của từng card |
| `src/main/resources/fxml/seller-view.fxml` | Tương tự admin-view — form tạo auction nằm trong card riêng |
| `src/main/java/com/auction/controller/AuctionController.java` | Filter tabs, search, status badge, price format, action column, toast/loading/confirm |
| `src/main/java/com/auction/controller/AuthController.java` | Thêm `rootPane`, toast cho login/register |
| `src/main/java/com/auction/controller/AdminController.java` | Thêm `rootPane`, toast, confirm dialog khi cancel, loading overlay khi refresh |
| `src/main/java/com/auction/controller/SellerController.java` | Tương tự — toast, confirm, loading |
| `src/main/java/com/auction/app/SceneNavigator.java` | Fade-in transition, min-size, size mặc định lớn hơn, title có brand |

---

## 3. CSS — điểm nổi bật

### App Bar
- Nền trắng, border dưới nhạt, padding `14 28 14 28`
- Logo `38×38` gradient xanh `#3b82f6 → #1d4ed8`, chữ "BM"

### Card
```css
.card {
    -fx-background-color: white;
    -fx-background-radius: 14;
    -fx-padding: 24;
    -fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.08), 20, 0.1, 0, 4);
}
```

### Status badge
| Status | Background | Text color |
|---|---|---|
| OPEN | `#dbeafe` (blue-100) | `#1d4ed8` |
| RUNNING | `#d1fae5` (green-100) | `#059669` |
| FINISHED | `#fef3c7` (amber-100) | `#b45309` |
| PAID | `#ede9fe` (violet-100) | `#6d28d9` |
| CANCELED | `#fee2e2` (red-100) | `#b91c1c` |

### Button variants
- `.button-primary` — xanh dương (mặc định)
- `.button-success` — xanh lá (tích cực)
- `.button-danger` — đỏ (nguy hiểm)
- `.button-ghost` — viền, nền trong suốt
- `.button-icon` — app bar icon
- `.button-row` — padding nhỏ hơn để dùng trong table cell

### Table
- Header nền `#f8fafc` bo góc trên
- Row hover → `#eff6ff` (blue-50)
- Row selected → `#dbeafe` (blue-100)
- Padding `12px` mỗi cell

---

## 4. `UiEffects.java` — API

```java
// Toast góc trên phải, tự ẩn sau durationMs
UiEffects.showToast(rootPane, "Đã lưu", ToastType.SUCCESS, 1800);

// Loading overlay có spinner, trả về Runnable để gỡ
Runnable hide = UiEffects.showLoadingOverlay(rootPane);
// ... xong việc
hide.run();

// Confirm dialog với backdrop, callback theo kết quả
UiEffects.showConfirmDialog(rootPane,
    "Kết thúc cuộc đấu giá",
    "Bạn có chắc chắn?",
    "Xác nhận",
    true,           // dangerConfirm → nút đỏ
    ok -> {
        if (ok) doSomething();
    });

// Chạy action kèm overlay tối thiểu N ms (cho hiệu ứng loading mượt)
UiEffects.runWithLoading(rootPane, 500,
    this::refreshTable,
    () -> UiEffects.showToast(rootPane, "Đã làm mới", ToastType.INFO, 1600));
```

Điểm cần biết:
- Toàn bộ hiệu ứng hoạt động dựa trên `StackPane` root. Tất cả FXML đã được bọc trong `<StackPane fx:id="rootPane">`.
- Không cần tạo `Stage` hay `Window` phụ — toast / overlay / modal đều nằm trong cùng scene.
- Click ra ngoài backdrop của confirm dialog được coi là huỷ.

---

## 5. Mapping 6 trạng thái UI mục tiêu

| Màn hình | Thao tác | Cách mô phỏng trong code |
|---|---|---|
| 1. Dashboard ban đầu | Load xong, tab Active, search trống | `AuctionController.init()` → `activeFilter = "ACTIVE"`, `searchField` rỗng |
| 2. Refresh | Click ⟳ trên App Bar | `handleRefresh()` → `UiEffects.runWithLoading(500ms)` + toast "Dữ liệu đã làm mới" |
| 3. Chọn auction | Click row | CSS `.table-row-cell:selected` + `setRowFactory` cập nhật `selectedAuctionLabel`; action column tự enable "Place Bid" khi RUNNING |
| 4. Place Bid | Click "Place Bid" trong hàng | `onPlaceBidRow()` → `viewModel.placeBid()` → toast xanh "Đặt thầu thành công: 2100.0" + `refreshTable()` cập nhật giá |
| 5. Finish Auction | Click ⏹ trên App Bar | `handleFinishAuction()` → `UiEffects.showConfirmDialog(..., dangerConfirm=true)` với nút "Xác nhận" đỏ |
| 6. Back to Login | Click ⎋ trên App Bar | `navigator.showLogin()` → `setScene()` có `FadeTransition 0.6→1.0` |

---

## 6. Thay đổi hành vi đáng chú ý

### Nút điều hướng chuyển vào App Bar
Trước: Refresh / Finish / Back to Login nằm ở chân trang dạng `HBox` nút thường.
Sau: Các nút này chuyển thành `button-icon` (transparent, hover nhẹ) trên App Bar, giảm "noise" visual ở chân trang.

### Action column trong bảng
Trước: Chỉ có "Place Bid" ở dưới bảng, dùng cho bất kỳ row nào được chọn.
Sau: Mỗi row có 2 button inline ("Details", "Place Bid"). "Place Bid" tự disable nếu auction không ở trạng thái `RUNNING` hoặc user không phải Bidder.

### Filter + Search
Trước: Hiển thị toàn bộ danh sách, không lọc được.
Sau: 3 filter tabs (All / Active / Finished) + search theo item hoặc seller name. Summary label tự cập nhật `Showing X of Y auctions`.

### Confirm cho hành động nguy hiểm
Trước: Click "Finish Auction" hay "Cancel Auction" → chạy luôn, không xác nhận.
Sau: Mở confirm dialog với button đỏ; click ra ngoài = huỷ.

### Format dữ liệu
- Auction ID hiển thị rút gọn `ab12cd34…` (8 ký tự + `…`) thay vì UUID đầy đủ.
- Giá hiển thị `$2,100.00` thay vì `2100.0`.
- User avatar hiển thị chữ cái đầu của username (viết hoa).

---

## 7. Khả năng co giãn (Responsive)

Các thay đổi đảm bảo UI co giãn khi user resize cửa sổ:

- Stage: `minWidth=960`, `minHeight=620`, mặc định `1180×760`.
- App Bar: dùng `Region HBox.hgrow=ALWAYS` để đẩy user menu sang phải.
- Table: `VBox.vgrow=ALWAYS` → chiếm hết chiều cao còn lại của card.
- Description field trong Seller form: `HBox.hgrow=ALWAYS`.
- Modal / Toast: dùng `StackPane` alignment, tự căn theo kích thước scene hiện tại.

---

## 8. Verify

Build project để xác nhận không lỗi compile:

```bash
mvn -DskipTests compile
# → BUILD SUCCESS
```

Chạy thử:

```bash
mvn clean javafx:run
```

Đăng nhập với tài khoản demo:
- `bidder@auction.local` / `demo12345` — màn hình Auction List (test trạng thái 1-4, 6)
- `admin@auction.local` / `demo12345` — Admin Dashboard (test trạng thái 5 — confirm cancel)
- `seller@auction.local` / `demo12345` — Seller Dashboard

---

## 9. Rủi ro & điểm cần lưu ý

| Rủi ro | Ảnh hưởng | Giảm thiểu |
|---|---|---|
| Test cũ trong `AuctionListViewModelTest` có thể chưa chạy lại | Có thể pass nhưng chưa cover luồng toast/confirm mới | Toast / confirm nằm ở tầng controller (UI), không đụng đến ViewModel → test cũ vẫn pass |
| Emoji trong button text (`⟳`, `⏹`, `⎋`, `🔍`) cần font hỗ trợ | Windows/macOS mặc định render OK, Linux minimal có thể thiếu | Nếu gặp sự cố, thay bằng text thuần hoặc icon font (FontAwesome) |
| File `BidController.java` rỗng | Không dùng, không ảnh hưởng | Có thể xoá sau |
| Table `CellFactory` tạo inline nhiều style class | Nếu badge logic mở rộng, khó maintain | Có thể tách thành `StatusBadgeCell` class riêng trong đợt tiếp theo |
| `UiEffects` dùng `Platform.runLater` trong `runWithLoading` | Đảm bảo `onDone` chạy trên FX thread | Đã xử lý đúng, không cần sửa |

---

## 10. Hướng mở rộng tiếp theo

- Tách `StatusBadgeCell` và `PriceCell` thành class riêng để tái sử dụng giữa 3 controller.
- Thêm Scene Builder-friendly — test mở các FXML mới trong Scene Builder để kiểm tra preview.
- Dark mode — CSS đã tập trung ở `app.css`, chỉ cần thêm `app-dark.css` và toggle theo user preference.
- Real-time bid update — khi tích hợp network (client-server), sử dụng `Platform.runLater` + `UiEffects.showToast` khi nhận event bid từ server.
- Auction detail view hiện là placeholder — có thể làm tiếp trong đợt sau.

---

**Kết luận:** Đợt nâng cấp đã chuyển UI từ plain functional sang modern brand-aligned (BidMaster Auctions) mà không đụng gì đến business logic (service, DAO, model). Build SUCCESS, sẵn sàng demo.
