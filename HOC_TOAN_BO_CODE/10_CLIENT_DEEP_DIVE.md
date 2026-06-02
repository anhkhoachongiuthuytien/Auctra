# 10 - Client deep dive: học kỹ JavaFX, controller, gateway

File này giải thích sâu `auction-client`. Mục tiêu là biết mỗi nút bấm trên UI đi qua controller nào, gọi gateway nào và chạm service nào.

---

## 1. Main.java

### start(Stage primaryStage)

Luồng:

```text
1. AppContext appContext = createAppContext().
2. SceneNavigator navigator = new SceneNavigator(primaryStage, appContext).
3. navigator.showLogin().
```

### createAppContext()

Luồng:

```text
1. Đọc raw args JavaFX.
2. Nếu có --socket:
   - lấy host/port.
   - return new AppContext(host, port).
3. Nếu không:
   - return new AppContext().
```

### Câu trả lời

```text
Main quyết định chế độ chạy ban đầu: local hay socket. Sau đó giao việc chuyển màn hình cho SceneNavigator.
```

---

## 2. AppContext.java

### AppContext() - local mode

Luồng:

```text
1. new ServerContext("jdbc:sqlite:auction-system.db").
2. new AuctionServerFacade(serverContext).
3. gateway = new LocalAuctionClientGateway(serverFacade).
4. start AuctionExpiryScheduler.
```

Ý nghĩa:

```text
Client và server chạy chung process.
Không cần mở cổng socket.
```

### AppContext(String host, int port) - socket mode

Luồng:

```text
1. new SocketAuctionClientGateway(host, port).
2. socketGateway.connect().
3. gateway = socketGateway.
```

Ý nghĩa:

```text
Client gọi server qua TCP socket.
ServerMain phải chạy trước.
```

### getGateway()

Controller dùng method này để gọi mọi nghiệp vụ.

---

## 3. SceneNavigator.java

### createLoader(fxmlPath)

Tạo `FXMLLoader` trỏ tới file FXML trong resources.

### showLogin()

```text
1. Load login-view.fxml.
2. Lấy AuthController.
3. controller.init(appContext, this).
4. setScene().
```

### showRegister()

Load `register-view.fxml`, init `RegisterController`.

### showForgotPassword()

Load `forgot-password-view.fxml`, init `ForgotPasswordController`.

### showAuctionList(User user)

Load `auction-list-view.fxml`, init `AuctionController`.

### showAuctionDetail(Auction auction, User user)

Load `auction-detail-view.fxml`, init `AuctionDetailController`.

### showHome(User user)

Logic phân quyền:

```text
Nếu user instanceof Seller -> showSellerDashboard().
Nếu user instanceof Admin  -> showAdminDashboard().
Ngược lại                 -> showAuctionList().
```

### showSellerDashboard(Seller seller)

Load `seller-view.fxml`, init `SellerController`.

### showAdminDashboard(Admin admin)

Load `admin-view.fxml`, init `AdminController`.

### showProfile(User user)

Load `profile-view.fxml`, init `ProfileController`.

### setScene(root, title)

Gắn root vào Scene/Stage, set title, có thể thêm hiệu ứng/stylesheet.

---

## 4. AuctionClientGateway.java

Interface này là hợp đồng giữa UI và server.

Các nhóm method:

```text
Auth:
  login, register, resetPassword, getAvailableRegistrationRoles

Auction:
  listAuctions, listAuctionsForSeller, createAuctionForSeller
  startAuction, finishAuction, cancelAuction, markAuctionPaid

Bid:
  placeBid

Admin/User:
  listUsers, updateUser

Auto-Bid:
  registerAutoBid, cancelAutoBid, getAutoBid
```

Câu trả lời:

```text
Controller chỉ phụ thuộc interface Gateway nên không biết local hay socket.
```

---

## 5. LocalAuctionClientGateway.java

### Vai trò

Gọi `AuctionServerFacade` trực tiếp.

### Ví dụ

```text
login(email, password)
  -> serverFacade.login(email, password)

placeBid(auctionId, bidder, amount)
  -> serverFacade.placeBid(...)
  -> fireLocalUpdate()
```

### fireLocalUpdate()

Vì local mode không có socket broadcast, gateway chủ động gọi `ClientEventManager.fireUpdate()` để UI vẫn cập nhật.

### Câu trả lời

```text
Local gateway giúp test/chạy app một máy nhưng controller vẫn dùng cùng interface như socket.
```

---

## 6. SocketAuctionClientGateway.java

### connect()

Luồng:

```text
1. Nếu socket đang mở thì return.
2. new Socket(host, port).
3. Tạo ObjectOutputStream và flush trước.
4. Tạo ObjectInputStream.
5. startListeningForUpdates() nếu listener chưa chạy.
```

### startListeningForUpdates()

Luồng:

```text
1. Tạo thread daemon.
2. Mở socket phụ.
3. Tạo ObjectOutputStream/ObjectInputStream.
4. Gửi AuctionRequest(SUBSCRIBE_UPDATES).
5. while true đọc object.
6. Nếu object là AuctionEvent:
   - Platform.runLater(ClientEventManager::fireUpdate).
```

### disconnect()

Đóng socket chính và reset field.

### send(AuctionRequest request)

Luồng:

```text
1. connect().
2. out.writeObject(request).
3. out.flush().
4. out.reset().
5. Đọc AuctionResponse.
6. Nếu response không success -> throw RuntimeException(message).
7. Nếu lỗi IO/ClassNotFound:
   - disconnect().
   - connect().
   - retry một lần.
```

### Các method nghiệp vụ

Ví dụ:

```text
login()
  -> RequestType.LOGIN
  -> data: email, password
  -> response data: UserDto
  -> DtoMapper.toUser()

listAuctions()
  -> RequestType.LIST_AUCTIONS
  -> response data: List<AuctionDto>
  -> DtoMapper.toAuction()

placeBid()
  -> RequestType.PLACE_BID
  -> data: auctionId, bidderId, bidderName, bidderEmail, amount
```

---

## 7. AuthController.java

### init(appContext, navigator)

Tạo `LoginViewModel` từ gateway và lưu navigator.

### handleLogin()

Luồng:

```text
1. Lấy email/password từ field.
2. viewModel.login().
3. Nếu success:
   - navigator.showHome(user).
4. Nếu fail:
   - showMessage lỗi.
```

### goToRegister()

Chuyển sang màn đăng ký.

### goToForgotPassword()

Chuyển sang màn quên mật khẩu.

### handleToggleTheme()

Gọi `ThemeManager.toggle(scene)`.

---

## 8. RegisterController.java

Nhiệm vụ:

```text
Đăng ký tài khoản mới.
Validate password/confirm password qua LoginViewModel.
Gọi gateway.register().
Nếu thành công chuyển về login hoặc hiển thị thông báo.
```

Điểm UI:

```text
Có password strength indicator.
Nếu input lỗi, dùng UIAnimations.shakeField().
```

---

## 9. ForgotPasswordController.java

Luồng:

```text
handleReset()
  -> lấy email, username, newPassword
  -> gateway.resetPassword()
  -> nếu success quay lại login/thông báo thành công
```

Lỗi thường:

```text
Email không tồn tại.
Username không khớp.
Password mới không hợp lệ.
```

---

## 10. AuctionController.java

### Vai trò

Hiển thị danh sách phiên đấu giá cho bidder/user.

### init()

Nhận appContext, navigator, currentUser. Sau đó load danh sách auction.

### filter

Các handler:

```text
handleFilterAll()
handleFilterActive()
handleFilterFinished()
```

Chúng đổi mode filter rồi render lại grid/list.

### createAuctionCard(Auction a)

Tạo card hiển thị:

```text
tên vật phẩm
giá hiện tại
trạng thái
seller
nút chi tiết
```

### Khi bấm chi tiết

Gọi `navigator.showAuctionDetail(a, currentUser)`.

---

## 11. AuctionDetailController.java

Đây là controller quan trọng nhất phía client.

### init(appContext, navigator, currentUser, auction)

Luồng:

```text
1. Lưu appContext/navigator/currentUser/auctionId.
2. Tạo AuctionListViewModel.
3. configureBidTable().
4. Gắn Enter trong ô bid -> handlePlaceBid().
5. ClientEventManager.clearListeners().
6. ClientEventManager.addListener(this::reloadAuction).
7. startCountdown().
8. updateUserChrome().
9. renderAuction(auction).
```

### configureBidTable()

Setup các cột:

```text
STT
bidder name
amount format tiền
bidTime format ngày giờ
```

### renderAuction(auction)

Vẽ lại toàn bộ UI:

```text
Tên item, loại item, mô tả, seller.
Ảnh và thumbnails.
Status badge.
Current price và starting price.
Winner.
Bid table.
Bid count.
Ẩn/hiện form đặt giá tùy role/status.
Update chart giá.
Kiểm tra Auto-Bid hiện tại.
```

Điều kiện đặt giá:

```text
auction.status == RUNNING
và currentUser là Bidder
```

### updatePriceChart(auction)

Tạo line chart:

```text
Điểm đầu: startingPrice.
Các điểm sau: từng BidTransaction.
Nếu chưa có bid: thêm currentPrice hiện tại.
```

### startCountdown()

Mỗi giây:

```text
1. Nếu auction null -> báo lỗi.
2. Nếu status không RUNNING -> dừng countdown.
3. Tính remaining = endTime - now.
4. Nếu hết giờ -> ẩn bid form, reloadAuction().
5. Nếu còn giờ -> update label và progressBar.
```

### handleToggleAutoBid()

Nếu đang có auto-bid:

```text
gateway.cancelAutoBid(auctionId, currentUser.id)
reloadAuction()
```

Nếu chưa có:

```text
1. Đọc maxPrice và increment.
2. Validate số hợp lệ.
3. Validate increment > 0.
4. Validate maxPrice >= currentPrice + increment.
5. gateway.registerAutoBid().
6. clear field và reloadAuction().
```

### handlePlaceBid()

Luồng:

```text
1. findAuction().
2. viewModel.placeBid(currentUser, auction, bidAmountField.text).
3. Nếu success:
   - toast thành công.
   - bounce button.
   - clear field.
   - reloadAuction().
4. Nếu fail:
   - shake field.
   - toast lỗi.
```

### reloadAuction()

Tìm auction mới nhất từ gateway rồi render lại.

### findAuction()

```text
gateway.listAuctions()
  -> filter theo auctionId
```

### handleBack()

Dừng countdown, clear realtime listener, về home.

---

## 12. SellerController.java

### init()

```text
Lưu context/navigator/currentSeller.
Setup drag-drop ảnh.
Load danh sách auction của seller.
Update stats.
```

### handleChooseImage()

Mở file chooser chọn ảnh.

### handleClearImage()

Xóa danh sách ảnh đang chọn.

### updateImagePreviews()

Render preview ảnh trước khi tạo item.

### setupImageDrop()

Cho kéo-thả ảnh vào vùng chọn ảnh.

### handleCreateAuction()

Luồng:

```text
1. Đọc type/name/description/startingPrice.
2. Validate input.
3. Lưu ảnh nếu có qua ImageStorage.
4. gateway.createAuctionForSeller().
5. Toast thành công.
6. clear form.
7. refreshAuctions().
```

### handleStartAuction()

```text
1. Lấy auction đang chọn.
2. gateway.startAuction(id).
3. refreshAuctions().
```

### handleFinishAuction()

Tương tự start nhưng gọi `finishAuction`.

### createAuctionCard(Auction a)

Tạo card seller có overlay action:

```text
Chi tiết
Bắt đầu
Kết thúc
```

### updateStats()

Tính số phiên, doanh thu, trạng thái... để hiển thị dashboard seller.

---

## 13. AdminController.java

### init()

```text
Lưu context/navigator/currentAdmin.
Setup chart.
Configure search.
Configure user table.
Configure auction table.
refreshData().
```

### handleShowDashboard/Users/Auctions()

Chuyển section trong admin view.

### configureSearch()

Lọc user/auction theo keyword.

### handleCancelAuction()

```text
1. Lấy auction đang chọn.
2. gateway.cancelAuction(id).
3. refreshData().
```

### handleMarkPaid()

```text
1. Lấy auction đang chọn.
2. gateway.markAuctionPaid(id).
3. refreshData().
```

### refreshData()

```text
gateway.listUsers()
gateway.listAuctions()
update table/grid/stats
```

### createAuctionCard()

Tạo card admin có action cancel/paid.

---

## 14. ProfileController.java

Nhiệm vụ:

```text
Hiển thị thông tin user.
Cho sửa username/email/profile theo role.
Cho đổi avatar.
Gọi gateway.updateUser().
```

Điểm cần nhớ:

```text
Profile update đi qua UserService và UserDao, không sửa object client rồi thôi.
```

---

## 15. ViewModel

### LoginViewModel

```text
login():
  gọi gateway.login().
  bắt exception và trả LoginResult.

register():
  kiểm tra password/confirm password.
  gọi gateway.register().
```

### AuctionListViewModel

```text
placeBid():
  kiểm tra currentUser là Bidder.
  parse amountText.
  gọi gateway.placeBid().
  trả ActionResult.
```

---

## Checklist hiểu client

```text
[ ] Main chọn local/socket thế nào?
[ ] AppContext tạo gateway ra sao?
[ ] SceneNavigator phân màn hình theo role thế nào?
[ ] Gateway interface giúp gì?
[ ] Local gateway khác socket gateway ra sao?
[ ] Socket gateway gửi request và nghe realtime ra sao?
[ ] AuthController login ra sao?
[ ] SellerController tạo/start/finish auction ra sao?
[ ] AuctionDetailController đặt giá/auto-bid/countdown ra sao?
[ ] AdminController cancel/paid ra sao?
[ ] ClientEventManager reload UI ra sao?
```
