# Mô tả giao diện hiện tại

Tài liệu này mô tả hiện trạng giao diện JavaFX của ứng dụng Auctra, dựa trên các file FXML trong `auction-client/src/main/resources/fxml` và stylesheet `auction-client/src/main/resources/css/app.css`.

## 1. Phong cách giao diện chung

Ứng dụng sử dụng **giao diện trắng gọn (Clean White Theme)** và bảng màu chuẩn của **Google** (Google Blue `#1a73e8`, Red `#ea4335`, Yellow `#fbbc05`, Green `#34a853`) dựa trên logo biểu đồ Google mới của ứng dụng.

Các thành phần giao diện chính:

- **Logo ứng dụng**: Được cài đặt trực tiếp từ hình ảnh biểu đồ Google mới (`/images/logo.jpg`), hiển thị đồng bộ trên thanh điều hướng và màn hình đăng nhập.
- **Thanh điều hướng trên cùng**: Nền trắng, chứa logo ảnh, tiêu đề thương hiệu Auctra và cụm thông tin tài khoản người dùng.
- **Sidebar bên trái**: Nền trắng sạch, viền phải mỏng màu xám, các nút điều hướng sử dụng màu chữ xám đậm, đổi sang nền xanh nhạt (`#e8f0fe`) và viền chỉ báo màu xanh Google Blue (`#1a73e8`) khi được kích hoạt hoặc hover.
- **Card nội dung**: Nền trắng (`#ffffff`), bo góc lớn (18px), có viền mảnh và đổ bóng mờ rất nhẹ theo phong cách Material Design của Google.
- **Bảng dữ liệu (TableView)**: Header dùng màu nền xám nhạt (`#f1f3f4`), dòng dữ liệu phẳng sạch sẽ, có màu nền hover xám nhạt và hàng được chọn có màu nền xanh nhạt cùng viền xanh dương nổi bật.
- **Nút bấm**: Nút chính dùng màu xanh Google Blue (`#1a73e8`); nút thành công dùng xanh lá (`#34a853`); nút cảnh báo/hủy dùng màu đỏ (`#ea4335`); các nút phụ dùng dạng viền mảnh hoặc chữ trơn (ghost button).
- **Phông chữ hiển thị**: Toàn bộ hệ thống được chuyển từ font Georgia sang **Segoe UI / Roboto**, giải quyết triệt để lỗi hiển thị tiếng Việt có dấu.

CSS chính nằm ở:

- `auction-client/src/main/resources/css/app.css`


## 2. Màn hình đăng nhập

File:

- `auction-client/src/main/resources/fxml/login-view.fxml`

Bố cục gồm 2 cột:

- Cột trái là panel thương hiệu.
- Cột phải là form đăng nhập.

Panel thương hiệu bên trái có nền gradient sáng, logo chữ "Auctra" nhiều màu, slogan "Nền tảng đấu giá trực tuyến", các chip tính năng như `Real-time`, `An toàn`, `Nhiều người`, và cụm thống kê mẫu như số người dùng, số phiên đấu giá, tỷ lệ hài lòng.

Form bên phải nằm trong card trắng, gồm:

- Logo Auctra nhỏ.
- Tiêu đề "Đăng nhập".
- Mô tả ngắn.
- Ô nhập email.
- Ô nhập mật khẩu.
- Link "Quên mật khẩu?".
- Checkbox "Ghi nhớ đăng nhập".
- Vùng hiển thị thông báo.
- Link tạo tài khoản.
- Nút "Tiếp theo".

Phía dưới form có footer nhỏ gồm ngôn ngữ, trợ giúp và điều khoản.

## 3. Màn hình đăng ký

File:

- `auction-client/src/main/resources/fxml/register-view.fxml`

Màn hình đăng ký dùng cùng phong cách với màn đăng nhập: bố cục 2 cột, panel thương hiệu bên trái và card form bên phải.

Panel trái hiển thị logo Auctra, dòng "Tạo tài khoản mới" và các chip như `Miễn phí`, `Bảo mật`, `Nhanh chóng`.

Form đăng ký gồm:

- Tên hiển thị.
- Email.
- Mật khẩu.
- Xác nhận mật khẩu.
- Gợi ý yêu cầu mật khẩu.
- ComboBox chọn loại tài khoản.
- Vùng thông báo.
- Link quay lại đăng nhập.
- Nút "Tiếp theo".

## 4. Màn hình quên mật khẩu

File:

- `auction-client/src/main/resources/fxml/forgot-password-view.fxml`

Màn hình này tiếp tục dùng layout 2 cột của nhóm xác thực.

Panel trái hiển thị thương hiệu Auctra và thông điệp khôi phục tài khoản. Form bên phải gồm:

- Tiêu đề "Khôi phục tài khoản".
- Mô tả yêu cầu nhập email và tên đăng nhập để xác thực.
- Ô email.
- Ô tên đăng nhập.
- Ô mật khẩu mới.
- Ô xác nhận mật khẩu.
- Vùng thông báo.
- Link quay lại đăng nhập.
- Nút "Tiếp theo".

## 5. Màn hình danh sách phiên đấu giá

File:

- `auction-client/src/main/resources/fxml/auction-list-view.fxml`

Đây là màn chính cho người mua (Bidder). Layout dùng `BorderPane` gồm:

- Top bar.
- Sidebar.
- Khu vực nội dung chính.
- Footer.

Top bar có logo Auctra, ô tìm kiếm lớn và cụm avatar người dùng. Sidebar có các mục:

- Phiên đấu giá.
- Tài khoản.
- Đăng xuất.

Nội dung chính gồm breadcrumb, nút làm mới, tiêu đề "Khám phá các phiên đấu giá" và vùng hiển thị lưới thẻ sản phẩm.

Card danh sách có:

- Tiêu đề "Danh sách đấu giá".
- Bộ lọc dạng tab: tất cả, đang diễn ra, đã kết thúc.
- Ô tìm kiếm vật phẩm.
- **Lưới thẻ sản phẩm (Grid Card View)**: Thay thế cho dạng bảng `TableView` cũ. Lưới hiển thị danh sách các ô sản phẩm tự động cuộn dọc (bọc trong `ScrollPane` trong suốt).

Mỗi thẻ sản phẩm (Auction Card) bao gồm:
- **Hộp ảnh sản phẩm (Image Box)**: Hiển thị hình ảnh thực tế của sản phẩm được lấy từ `ImageStorage` trên máy local, hoặc hiển thị emoji placeholder hình hộp quà 📦 nếu sản phẩm chưa có ảnh.
- **Huy hiệu trạng thái (Status Badge)**: Sử dụng các màu sắc trực quan tương ứng với trạng thái phiên:
  - Đang mở (`OPEN` - màu vàng cam)
  - Đang diễn ra (`RUNNING` - màu xanh lá)
  - Đã kết thúc (`FINISHED` - màu xám)
  - Đã thanh toán (`PAID` - màu vàng gold)
  - Đã hủy (`CANCELED` - màu đỏ)
- **Tiêu đề sản phẩm (Item Title)**: Hỗ trợ tự động xuống dòng khi tên quá dài.
- **Giá hiện tại (Current Price)**: Hiển thị giá cao nhất hiện tại ở dạng chữ đậm màu xanh thương hiệu.
- **Người bán (Seller)**: Tên tài khoản của người bán.

*Hành vi tương tác*: Toàn bộ các nút đặt thầu nhanh ngoài trang chủ đã được loại bỏ. Khi người dùng nhấp chuột trực tiếp vào bất kỳ vị trí nào trên thẻ sản phẩm, hệ thống sẽ tự động điều hướng sang **Màn hình chi tiết phiên đấu giá** để thực hiện các thao tác đấu giá tại đó.

## 6. Màn hình chi tiết phiên đấu giá

File:

- `auction-client/src/main/resources/fxml/auction-detail-view.fxml`

Màn chi tiết dùng layout quản trị tương tự danh sách đấu giá, nhưng sidebar chỉ có nút quay lại.

Khu vực chính gồm:

- Breadcrumb: trang chủ, phiên đấu giá, chi tiết.
- Nút làm mới.
- Cụm thông tin sản phẩm và đặt giá.
- Mô tả sản phẩm.
- Lịch sử đặt giá.

Phần trên của nội dung chia 2 cột:

- Cột trái là vùng ảnh sản phẩm lớn, có `ImageView` và placeholder.
- Cột phải là thông tin đấu giá.

Thông tin đấu giá gồm:

- Tên vật phẩm.
- Chip loại sản phẩm.
- Badge trạng thái.
- Số lượt đặt giá.
- Giá hiện tại.
- Giá khởi điểm.
- Người thắng hiện tại.
- Form đặt giá.
- Thông tin người bán và đánh giá.

Form đặt giá gồm:

- Gợi ý đặt giá.
- Ô nhập số tiền.
- Các nút tăng nhanh `+10`, `+100`, `+1,000`.
- Nút "Đặt giá ngay".

Phần dưới gồm card mô tả sản phẩm và bảng lịch sử đặt giá với các cột:

- Số thứ tự.
- Người đặt.
- Số tiền.
- Thời điểm.

## 7. Màn hình người bán

File:

- `auction-client/src/main/resources/fxml/seller-view.fxml`

Màn hình người bán dùng layout dashboard với top bar, sidebar và nội dung chính.

Sidebar có:

- Trang người bán.
- Đăng xuất.

Nội dung chính gồm:

- Breadcrumb "Trang chủ > Người bán".
- Tiêu đề "Trang người bán".
- Lời chào và thông báo hành động.
- Card tạo phiên đấu giá mới.
- Card danh sách phiên đấu giá của người bán.

Card tạo phiên đấu giá mới gồm form 3 hàng:

- Hàng 1: loại sản phẩm và tên vật phẩm.
- Hàng 2: mô tả và giá khởi điểm.
- Hàng 3: chọn ảnh, xem ảnh preview, xoá ảnh, nút tạo phiên đấu giá.

Card danh sách phiên của người bán có:

- Tiêu đề.
- **Lưới thẻ sản phẩm (Grid Card View)**: Hiển thị các ô vuông đại diện cho từng phiên đấu giá của người bán.
  - Khi nhấp chuột vào thẻ sản phẩm: Sẽ xuất hiện menu thao tác (Action Overlay) đè lên trên thẻ.
  - Menu thao tác gồm các nút: Bắt đầu (chỉ hiện khi phiên trạng thái OPEN), Kết thúc (chỉ hiện khi phiên trạng thái RUNNING), Xem chi tiết (để sang màn hình chi tiết phiên đấu giá) và Đóng (để ẩn menu này).

## 8. Màn hình quản trị

File:

- `auction-client/src/main/resources/fxml/admin-view.fxml`

Màn hình quản trị dùng layout dashboard đầy đủ.

Top bar gồm:

- Logo Auctra.
- Nhãn Admin.
- Ô tìm kiếm toàn cục.
- Avatar và tên admin.

Sidebar có menu:

- Tổng quan.
- Người dùng.
- Phiên đấu giá.
- Đăng xuất.

Khu vực tổng quan gồm:

- Breadcrumb.
- Nút làm mới.
- Card chào mừng.
- Các stat card.

Các stat card hiện có:

- Tổng người dùng.
- Đang diễn ra.
- Đã thanh toán.
- Tổng doanh thu.

Mục người dùng gồm:

- Card bảng người dùng.
- Ô tìm kiếm người dùng.
- Bảng với các cột: mã, tên, email, vai trò. Cột tên hiển thị ảnh hồ sơ tròn (nếu đã tải lên) bên cạnh tên người dùng, nếu không có sẽ hiển thị ảnh tròn có chữ cái đầu mặc định.

Mục phiên đấu giá gồm:

- Card bảng phiên đấu giá.
- Ô tìm kiếm.
- **Lưới thẻ sản phẩm (Grid Card View)**: Hiển thị các ô vuông đại diện cho từng phiên đấu giá tương tự như màn hình người mua.
  - Khi nhấp chuột vào thẻ sản phẩm: Sẽ xuất hiện menu thao tác (Action Overlay) đè lên trên thẻ.
  - Menu thao tác gồm các nút: Hủy phiên (chỉ hiện khi phiên trạng thái OPEN hoặc RUNNING), Thanh toán (chỉ hiện khi phiên trạng thái FINISHED), Xem chi tiết (để sang màn hình chi tiết phiên đấu giá) và Đóng (để ẩn menu này).

## 9. Màn hình tài khoản cá nhân

File:

- `auction-client/src/main/resources/fxml/profile-view.fxml`

Màn hình tài khoản dùng layout giống bidder dashboard.

Sidebar gồm:

- Phiên đấu giá.
- Tài khoản.
- Đăng xuất.

Nội dung chính gồm:

- Breadcrumb "Trang chủ > Tài khoản".
- Card hồ sơ.
- Card thông tin tài khoản.

Card hồ sơ hiển thị:

- Avatar lớn: Có dạng hình tròn. Khi người dùng di chuột vào sẽ xuất hiện biểu tượng bàn tay (`cursor="HAND"`). Nhấp vào ảnh để mở FileChooser và chọn tệp ảnh mới tải lên làm ảnh hồ sơ.
- Tên người dùng.
- Chip vai trò.
- Chip `VERIFIED`.
- Email người dùng.
- Nút chỉnh sửa hồ sơ.

Card thông tin tài khoản hiện hiển thị:

- Mã người dùng.
- Email đăng nhập.

Lưu ý: dòng email đăng nhập trong card thông tin tài khoản đang có label giá trị rỗng, trong khi email đã được hiển thị ở card hồ sơ phía trên.

## 10. Các điểm nhận xét về UI hiện tại

Điểm mạnh:

- Các màn hình sau đăng nhập có cấu trúc dashboard thống nhất.
- Theme màu sắc nhất quán, dễ nhận diện thương hiệu.
- Bảng, card, button, chip và badge đã được chuẩn hóa trong CSS.
- Đã chuyển đổi thành công giao diện danh sách phiên đấu giá của Bidder sang dạng lưới thẻ sản phẩm (Grid Card View) hiện đại, bo góc và hiệu ứng chuyển động mượt mà khi di chuột.
- Các luồng chính đã có màn hình riêng: đăng nhập, đăng ký, quên mật khẩu, bidder, seller, admin, profile.

Điểm cần cải thiện:

- Một số màn hình dùng nhiều kích thước cố định, đặc biệt màn chi tiết đấu giá, nên có nguy cơ chật khi cửa sổ nhỏ.
- Nhiều style được viết inline trong FXML, khiến việc chỉnh theme đồng bộ khó hơn.
- Một số class được dùng trong FXML nhưng chưa thấy định nghĩa rõ trong CSS, ví dụ `login-field-label`.
- Màn profile có dòng email đăng nhập chưa gắn dữ liệu.
- CSS đang import Google Fonts qua mạng; nếu chạy offline, font Inter có thể không tải được.
- Một số icon đang dùng emoji/ký tự Unicode; hiển thị có thể khác nhau giữa các hệ điều hành hoặc font.

