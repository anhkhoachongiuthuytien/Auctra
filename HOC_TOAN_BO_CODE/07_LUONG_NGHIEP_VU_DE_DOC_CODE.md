# 07 - Luồng nghiệp vụ để lần code

Muốn hiểu toàn bộ code thì đừng đọc file ngẫu nhiên. Hãy lần theo các luồng nghiệp vụ dưới đây. Mỗi luồng cho biết nên mở file nào theo thứ tự.

---

## 1. Luồng khởi động client local

```text
Main.start()
  -> createAppContext()
  -> new AppContext()
  -> new ServerContext("jdbc:sqlite:auction-system.db")
  -> new AuctionServerFacade(serverContext)
  -> new LocalAuctionClientGateway(serverFacade)
  -> new AuctionExpiryScheduler(...).start()
  -> new SceneNavigator(...)
  -> navigator.showLogin()
```

Ý nghĩa:

```text
Ở local mode, client và server chạy chung process.
Controller vẫn gọi gateway, nhưng gateway gọi facade trực tiếp, không qua socket.
```

---

## 2. Luồng khởi động client socket

```text
Main.start()
  -> createAppContext()
  -> thấy args có --socket
  -> new AppContext(host, port)
  -> new SocketAuctionClientGateway(host, port)
  -> connect()
  -> startListeningForUpdates()
  -> navigator.showLogin()
```

Ý nghĩa:

```text
Ở socket mode, client kết nối tới server riêng.
Socket chính dùng request/response.
Socket phụ nghe realtime event.
```

---

## 3. Luồng khởi động server socket

```text
ServerMain.main()
  -> new ServerContext("jdbc:sqlite:auction-system.db")
  -> new AuctionServerFacade(serverContext)
  -> new AuctionExpiryScheduler(serverContext.getAuctionService()).start()
  -> new AuctionSocketServer(facade, port)
  -> socketServer.start()
```

Ý nghĩa:

```text
Server mở cổng 9999, chờ client kết nối, xử lý request qua thread pool.
```

---

## 4. Luồng đăng nhập

```text
login-view.fxml
  -> AuthController.handleLogin()
  -> LoginViewModel.login(email, password)
  -> AuctionClientGateway.login()
```

Nếu local:

```text
LocalAuctionClientGateway.login()
  -> AuctionServerFacade.login()
```

Nếu socket:

```text
SocketAuctionClientGateway.login()
  -> new AuctionRequest(LOGIN)
  -> send(req)
  -> AuctionSocketServer.processRequest()
  -> case LOGIN
```

Server:

```text
AuctionServerFacade.login()
  -> AuthService.login()
  -> UserDao.findByEmail()
  -> PasswordHasher.matches()
  -> trả User
```

Client sau login:

```text
AuthController
  -> SceneNavigator.showHome(user)
  -> nếu Seller: showSellerDashboard()
  -> nếu Admin: showAdminDashboard()
  -> nếu Bidder: showAuctionList()
```

---

## 5. Luồng đăng ký

```text
register-view.fxml
  -> RegisterController.handleRegister()
  -> LoginViewModel.register()
  -> Gateway.register()
  -> AuctionServerFacade.register()
  -> AuthService.registerSeller/registerBidder()
  -> PasswordHasher.hash()
  -> UserDao.save(user, passwordHash)
```

Nếu socket:

```text
AuctionSocketServer case REGISTER
  -> facade.register()
  -> BroadcastManager.broadcast(USER_REGISTERED)
```

---

## 6. Luồng quên mật khẩu

```text
forgot-password-view.fxml
  -> ForgotPasswordController.handleReset()
  -> Gateway.resetPassword()
  -> AuctionServerFacade.resetPassword()
  -> AuthService.resetPassword()
  -> UserDao.updatePasswordHash(email, PasswordHasher.hash(newPassword))
```

---

## 7. Luồng seller tạo phiên

```text
seller-view.fxml
  -> SellerController.handleCreateAuction()
  -> đọc name, type, description, startingPrice, image
  -> Gateway.createAuctionForSeller()
```

Server:

```text
AuctionServerFacade.createAuctionForSeller()
  -> SellerService.createItem()
  -> ItemFactory.createItem()
  -> ItemDao.save()
  -> SellerService.createAuction()
  -> AuctionDao.save()
```

Nếu socket:

```text
AuctionSocketServer case CREATE_AUCTION
  -> BroadcastManager.broadcast(AUCTION_CREATED)
```

---

## 8. Luồng start auction

```text
SellerController.handleStartAuction()
  -> Gateway.startAuction(auctionId)
  -> AuctionServerFacade.startAuction()
  -> AuctionService.startAuction()
  -> AuctionDao.findById()
  -> Auction.start()
  -> AuctionDao.save()
```

Nếu socket:

```text
AuctionSocketServer case START_AUCTION
  -> BroadcastManager.broadcast(AUCTION_STARTED)
```

---

## 9. Luồng finish auction

```text
SellerController.handleFinishAuction()
  -> Gateway.finishAuction(auctionId)
  -> AuctionServerFacade.finishAuction()
  -> AuctionService.finishAuction()
  -> Auction.finish()
  -> AuctionDao.save()
```

Nếu tự động hết giờ:

```text
AuctionExpiryScheduler.checkExpiredAuctions()
  -> AuctionService.finishAuction()
  -> BroadcastManager.broadcast(AUCTION_FINISHED)
```

---

## 10. Luồng admin cancel/mark paid

Cancel:

```text
AdminController.handleCancelAuction()
  -> Gateway.cancelAuction()
  -> AuctionServerFacade.cancelAuction()
  -> AuctionService.cancelAuction()
  -> Auction.cancel()
  -> AuctionDao.save()
```

Mark paid:

```text
AdminController.handleMarkPaid()
  -> Gateway.markAuctionPaid()
  -> AuctionServerFacade.markAuctionPaid()
  -> AuctionService.markAuctionPaid()
  -> Auction.markPaid()
  -> AuctionDao.save()
```

---

## 11. Luồng bidder đặt giá

```text
auction-detail-view.fxml
  -> AuctionDetailController.handlePlaceBid()
  -> AuctionListViewModel.placeBid(currentUser, auction, amountText)
  -> Gateway.placeBid(auctionId, bidder, amount)
```

Server:

```text
AuctionServerFacade.placeBid()
  -> BidService.placeBid()
  -> validate auctionId/bidder/amount
  -> AuctionDao.findById()
  -> synchronized(auction)
  -> kiểm tra auction RUNNING
  -> kiểm tra amount > currentPrice
  -> new BidTransaction(bidder, amount)
  -> Auction.addBid()
  -> runAutoBiddingEngine() nếu có
  -> AuctionDao.save()
```

Nếu socket:

```text
AuctionSocketServer case PLACE_BID
  -> BroadcastManager.broadcast(NEW_BID)
```

Client nhận realtime:

```text
SocketAuctionClientGateway listener
  -> ClientEventManager.fireUpdate()
  -> AuctionDetailController.reloadAuction()
  -> renderAuction()
```

---

## 12. Luồng bật Auto-Bid

```text
AuctionDetailController.handleToggleAutoBid()
  -> đọc maxPrice, increment
  -> validate maxPrice >= currentPrice + increment
  -> Gateway.registerAutoBid()
```

Server:

```text
AuctionServerFacade.registerAutoBid()
  -> AutoBidDao.save(new AutoBidConfig(...))
```

Khi có bid mới:

```text
BidService.placeBid()
  -> runAutoBiddingEngine()
  -> AutoBidDao.getAutoBidsForAuction()
  -> PriorityQueue<PendingAutoBid>
  -> Auction.addBid(autoBid)
```

---

## 13. Luồng hủy Auto-Bid

```text
AuctionDetailController.handleToggleAutoBid()
  -> nếu currentAutoBid != null
  -> Gateway.cancelAutoBid()
  -> AuctionServerFacade.cancelAutoBid()
  -> AutoBidDao.delete(auctionId, bidderId)
```

---

## 14. Luồng realtime tổng quát

Đăng ký realtime:

```text
SocketAuctionClientGateway.startListeningForUpdates()
  -> new Socket(host, port)
  -> gửi AuctionRequest(SUBSCRIBE_UPDATES)
```

Server giữ client:

```text
AuctionSocketServer.handleClient()
  -> nếu request type SUBSCRIBE_UPDATES
  -> BroadcastManager.addClient(out)
```

Khi có thay đổi:

```text
AuctionSocketServer.processRequest()
  -> xử lý nghiệp vụ
  -> BroadcastManager.broadcast(new AuctionEvent(...))
```

Client cập nhật UI:

```text
eventIn.readObject()
  -> nếu AuctionEvent
  -> Platform.runLater(ClientEventManager::fireUpdate)
  -> controller reload
```

---

## 15. Luồng hiển thị profile

```text
SceneNavigator.showProfile(user)
  -> load profile-view.fxml
  -> ProfileController.init()
  -> render thông tin user
```

Cập nhật:

```text
ProfileController.handleEditProfile()
  -> Gateway.updateUser()
  -> AuctionServerFacade.updateUser()
  -> UserService.updateUser()
  -> UserDao.updateUser()
```

Nếu socket:

```text
AuctionSocketServer case UPDATE_USER
  -> BroadcastManager.broadcast(USER_UPDATED)
```

---

## 16. Cách tự kiểm tra đã hiểu chưa

Với mỗi luồng, hãy tự nói thành tiếng:

```text
1. Màn hình nào bắt đầu luồng?
2. Controller method nào chạy?
3. Gateway method nào được gọi?
4. Nếu socket thì RequestType là gì?
5. Server facade gọi service nào?
6. Service gọi DAO/model nào?
7. Có broadcast realtime không?
8. UI reload ở đâu?
```

Nếu trả lời được 8 câu này cho login, tạo phiên, đặt giá, auto-bid và realtime, bạn đã nắm được xương sống toàn bộ code.
