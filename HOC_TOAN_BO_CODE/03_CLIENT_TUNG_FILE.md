# 03 - auction-client: giải thích từng file

`auction-client` là ứng dụng JavaFX. Nó hiển thị giao diện, bắt sự kiện người dùng và gọi server thông qua Gateway. Controller không nên tự xử lý nghiệp vụ nặng; nghiệp vụ thật nằm ở server.

## Luồng client tổng quát

```text
Main
  -> AppContext
  -> SceneNavigator
  -> FXML + Controller
  -> ViewModel hoặc Gateway
  -> LocalAuctionClientGateway / SocketAuctionClientGateway
```

---

## Entry và app context

### Main.java

```text
Tầng: JavaFX entry point.
Nhiệm vụ: Khởi động app client.
Ai gọi: JavaFX runtime.
Gọi ai: AppContext, SceneNavigator.showLogin().
```

Cần hiểu:

```text
Nếu chạy không có --socket -> LOCAL mode.
Nếu chạy --socket host port -> SOCKET mode.
```

### AppContext.java

```text
Tầng: Composition root phía client.
Nhiệm vụ: Tạo gateway phù hợp với chế độ chạy.
Ai gọi: Main, test.
Gọi ai: ServerContext/AuctionServerFacade ở local mode, SocketAuctionClientGateway ở socket mode.
```

Hai mode:

```text
LOCAL:
  new ServerContext("jdbc:sqlite:auction-system.db")
  new AuctionServerFacade(serverContext)
  new LocalAuctionClientGateway(facade)

SOCKET:
  new SocketAuctionClientGateway(host, port)
  socketGateway.connect()
```

### SceneNavigator.java

```text
Tầng: Navigation/UI infrastructure.
Nhiệm vụ: Load FXML, gắn controller, chuyển màn hình.
Ai gọi: Controller, Main.
Gọi ai: FXMLLoader, controller.init().
```

Cần hiểu:

```text
Sau login, SceneNavigator kiểm tra user là Seller/Admin/Bidder để mở màn hình phù hợp.
```

---

## client gateway

### AuctionClientGateway.java

```text
Tầng: Gateway interface.
Nhiệm vụ: Định nghĩa các thao tác client có thể gọi.
Ai dùng: Controller, ViewModel.
Implementation: LocalAuctionClientGateway, SocketAuctionClientGateway.
```

Câu trả lời:

```text
Nhờ interface này, controller không cần biết đang gọi server local hay socket.
```

### LocalAuctionClientGateway.java

```text
Tầng: Gateway local.
Nhiệm vụ: Gọi AuctionServerFacade trực tiếp trong cùng process.
Ai gọi: Controller/ViewModel qua AuctionClientGateway.
Gọi ai: AuctionServerFacade.
```

Cần hiểu:

```text
Local mode tiện để chạy một máy, không cần mở server socket riêng.
```

### SocketAuctionClientGateway.java

```text
Tầng: Gateway socket.
Nhiệm vụ: Đóng gói thao tác thành AuctionRequest, gửi qua TCP socket, nhận AuctionResponse.
Ai gọi: Controller/ViewModel qua AuctionClientGateway.
Gọi ai: AuctionSocketServer qua mạng.
Tính năng nâng cao: reconnect, ObjectOutputStream reset, realtime listener socket.
```

Các phần phải hiểu:

```text
connect()                  -> mở socket chính.
send(AuctionRequest)       -> gửi request và đọc response.
startListeningForUpdates() -> mở socket phụ subscribe realtime.
login/register/placeBid... -> tạo AuctionRequest tương ứng.
```

### ClientEventManager.java

```text
Tầng: Realtime UI event manager.
Nhiệm vụ: Giữ danh sách listener UI cần reload khi có AuctionEvent.
Ai gọi: SocketAuctionClientGateway khi nhận event; các controller khi init.
```

Cần hiểu:

```text
ClientEventManager.fireUpdate() gọi các Runnable đã đăng ký.
CopyOnWriteArrayList giúp an toàn khi danh sách listener thay đổi trong lúc đang duyệt.
```

---

## presentation / ViewModel

### LoginViewModel.java

```text
Tầng: ViewModel.
Nhiệm vụ: Xử lý logic nhẹ cho login/register trước khi trả kết quả cho controller.
Ai gọi: AuthController, RegisterController.
Gọi ai: AuctionClientGateway.
```

### AuctionListViewModel.java

```text
Tầng: ViewModel.
Nhiệm vụ: Xử lý logic nhẹ cho danh sách auction và đặt giá.
Ai gọi: AuctionDetailController.
Gọi ai: AuctionClientGateway.placeBid().
```

Cần hiểu:

```text
ViewModel giúp controller bớt chứa logic validate/format.
```

---

## controller

### AuthController.java

```text
Tầng: JavaFX controller.
FXML: login-view.fxml.
Nhiệm vụ: Bắt sự kiện đăng nhập.
Ai gọi: JavaFX khi bấm nút login.
Gọi ai: LoginViewModel, SceneNavigator.
```

Luồng:

```text
handleLogin()
  -> viewModel.login(email, password)
  -> nếu thành công navigator.showHome(user)
```

### RegisterController.java

```text
Tầng: JavaFX controller.
FXML: register-view.fxml.
Nhiệm vụ: Đăng ký tài khoản Bidder/Seller.
Gọi ai: LoginViewModel.register(), SceneNavigator.
```

### ForgotPasswordController.java

```text
Tầng: JavaFX controller.
FXML: forgot-password-view.fxml.
Nhiệm vụ: Reset password theo email/username/password mới.
Gọi ai: AuctionClientGateway.resetPassword().
```

### AuctionController.java

```text
Tầng: JavaFX controller.
FXML: auction-list-view.fxml.
Nhiệm vụ: Hiển thị danh sách phiên cho bidder/người dùng.
Gọi ai: Gateway.listAuctions(), SceneNavigator.showAuctionDetail().
```

Cần hiểu:

```text
Nó tạo card auction, filter theo trạng thái và mở màn chi tiết khi người dùng chọn phiên.
```

### AuctionDetailController.java

```text
Tầng: JavaFX controller quan trọng.
FXML: auction-detail-view.fxml.
Nhiệm vụ: Hiển thị chi tiết phiên, đặt giá, auto-bid, countdown, chart, lịch sử bid.
Gọi ai: AuctionListViewModel, Gateway, ClientEventManager.
Tính năng nâng cao: realtime reload, countdown, auto-bid UI, price chart.
```

Các method quan trọng:

```text
init()                -> nhận appContext/navigator/user/auction.
renderAuction()       -> vẽ lại toàn bộ dữ liệu phiên.
handlePlaceBid()      -> đặt giá thường.
handleToggleAutoBid() -> bật/tắt auto-bid.
startCountdown()      -> cập nhật thời gian còn lại.
reloadAuction()       -> load lại auction từ gateway khi có realtime event.
```

### SellerController.java

```text
Tầng: JavaFX controller.
FXML: seller-view.fxml.
Nhiệm vụ: Seller tạo vật phẩm, tạo phiên, start/finish phiên.
Gọi ai: Gateway.createAuctionForSeller(), startAuction(), finishAuction().
```

Luồng tạo phiên:

```text
handleCreateAuction()
  -> lấy dữ liệu form
  -> lưu ảnh nếu có
  -> gateway.createAuctionForSeller()
  -> refresh danh sách phiên của seller
```

### AdminController.java

```text
Tầng: JavaFX controller.
FXML: admin-view.fxml.
Nhiệm vụ: Dashboard admin, danh sách users, danh sách auctions, cancel/mark paid.
Gọi ai: Gateway.listUsers(), listAuctions(), cancelAuction(), markAuctionPaid().
```

### ProfileController.java

```text
Tầng: JavaFX controller.
FXML: profile-view.fxml.
Nhiệm vụ: Hiển thị và cập nhật thông tin cá nhân/avatar.
Gọi ai: Gateway.updateUser(), ImageStorage/UserImageHelper.
```

### BidController.java

```text
Tầng: JavaFX controller phụ.
Nhiệm vụ: Xử lý thao tác bid nếu có màn/flow riêng.
Điểm cần hiểu: Luồng bid chính hiện nằm ở AuctionDetailController + AuctionListViewModel.
```

---

## ui package

### ThemeManager.java

```text
Tầng: UI utility.
Nhiệm vụ: Đổi theme light/dark cho Scene.
Ai gọi: Các controller khi bấm toggle theme.
```

### UIAnimations.java

```text
Tầng: UI utility.
Nhiệm vụ: Hiệu ứng shake, pulse, bounce, fade...
Ai gọi: Controller khi input lỗi/thành công/cập nhật giá.
```

### ToastManager.java

```text
Tầng: UI utility.
Nhiệm vụ: Hiển thị toast message.
Ai gọi: UiEffects/controller.
```

### BadgeFactory.java

```text
Tầng: UI factory.
Nhiệm vụ: Tạo badge trạng thái/role.
Ai gọi: Controller khi render UI.
```

### IconFactory.java

```text
Tầng: UI factory.
Nhiệm vụ: Tạo icon dùng trong nút/sidebar.
Ai gọi: Controller/UI helper.
```

### FloatingFieldHelper.java

```text
Tầng: UI helper.
Nhiệm vụ: Hỗ trợ text field kiểu floating label.
Ai gọi: Controller hoặc init UI.
```

### CountdownTimer.java

```text
Tầng: UI helper.
Nhiệm vụ: Hỗ trợ đếm ngược.
Ai dùng: Màn chi tiết auction hoặc UI liên quan thời gian.
```

### ResponsiveManager.java

```text
Tầng: UI helper.
Nhiệm vụ: Điều chỉnh responsive layout theo kích thước màn.
Ai gọi: Controller khi scene/window thay đổi.
```

### SkeletonPane.java

```text
Tầng: UI component.
Nhiệm vụ: Hiển thị loading skeleton.
Ai gọi: Controller/UiEffects khi tải dữ liệu.
```

---

## util package client

### UiEffects.java

```text
Tầng: Client utility.
Nhiệm vụ: Gom helper hiển thị toast/loading/effect.
Ai gọi: Controller.
```

### UserImageHelper.java

```text
Tầng: Client utility.
Nhiệm vụ: Xử lý ảnh/avatar người dùng.
Ai gọi: ProfileController và UI render user.
```

### ScreenshotGenerator.java

```text
Tầng: Dev/test utility.
Nhiệm vụ: Chạy JavaFX để capture screenshot các màn hình.
Ai dùng: Khi cần kiểm tra UI hoặc tạo ảnh minh họa.
```

---

## FXML resources

### login-view.fxml

```text
Màn hình: Đăng nhập.
Controller: AuthController.
Nghiệp vụ: login, chuyển sang register/forgot password.
```

### register-view.fxml

```text
Màn hình: Đăng ký.
Controller: RegisterController.
Nghiệp vụ: tạo tài khoản Bidder/Seller.
```

### forgot-password-view.fxml

```text
Màn hình: Quên mật khẩu.
Controller: ForgotPasswordController.
Nghiệp vụ: reset password.
```

### auction-list-view.fxml

```text
Màn hình: Danh sách phiên đấu giá.
Controller: AuctionController.
Nghiệp vụ: xem/filter/mở chi tiết auction.
```

### auction-detail-view.fxml

```text
Màn hình: Chi tiết phiên đấu giá.
Controller: AuctionDetailController.
Nghiệp vụ: đặt giá, auto-bid, xem chart, countdown, lịch sử bid.
```

### seller-view.fxml

```text
Màn hình: Seller dashboard.
Controller: SellerController.
Nghiệp vụ: tạo item/auction, start/finish auction.
```

### admin-view.fxml

```text
Màn hình: Admin dashboard.
Controller: AdminController.
Nghiệp vụ: quản lý users, auctions, cancel, mark paid.
```

### profile-view.fxml

```text
Màn hình: Hồ sơ cá nhân.
Controller: ProfileController.
Nghiệp vụ: cập nhật profile/avatar.
```

---

## CSS resources

### app.css

```text
File CSS tổng.
Nhiệm vụ: Import/tập hợp style chính cho app.
```

### base.css

```text
Nền tảng CSS chung: root, layout, control mặc định.
```

### tokens.css

```text
Design tokens: màu, spacing, radius, shadow, font size.
```

### themes/light.css và themes/dark.css

```text
Định nghĩa biến màu cho light/dark theme.
ThemeManager sẽ đổi theme tương ứng.
```

### screens/*.css

```text
Style riêng cho từng màn hình:
auth.css, auction.css, seller.css, admin.css, profile.css.
```

### components/*.css

```text
Style cho component tái sử dụng:
button, badge, card, table, form, toast, sidebar, countdown, avatar, chip...
```

---

## Tóm tắt client cần nhớ

```text
Main khởi động app.
AppContext quyết định local/socket.
SceneNavigator chuyển màn hình.
Controller bắt sự kiện UI.
ViewModel xử lý logic nhẹ.
Gateway gọi server.
SocketAuctionClientGateway là cầu nối mạng.
ClientEventManager giúp UI reload khi có realtime event.
```
