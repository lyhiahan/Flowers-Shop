# Tự kiểm thử khu vực Nhân viên

Tài khoản chọn nhanh: **Nguyễn Thị Mai Lan · Nhân viên (Staff)**, username `nguyenmailan`, mật khẩu demo `123456`. Nhân viên chỉ làm việc với đơn và tin nhắn của chi nhánh đã phân công.

## Các trang

| Trang | Địa chỉ | Công việc |
|---|---|---|
| Đơn hàng | `/staff/orders` | Tìm theo mã đơn/tên/số điện thoại, lọc trạng thái và hình thức nhận, xem lịch sử và phân trang |
| Chi tiết đơn | `/staff/orders/{id}` | Xem người nhận, điện thoại, địa chỉ, lịch hẹn, lời chúc, mẫu hoa, số lượng, giá trong đơn và thao tác tiếp theo |
| Chuẩn bị hoa | `/staff/orders/preparation` | Các đơn đã xác nhận hoặc đang chuẩn bị hoa |
| Giao & nhận hoa | `/staff/orders/delivery` | Các đơn đang chuẩn bị, bàn giao vận chuyển, đang giao hoặc đã giao chờ hoàn tất |
| Tin nhắn khách hàng | `/staff/chat` | Hộp thư của chi nhánh, đọc lịch sử, trả lời và gửi ảnh; không có chat nội bộ Admin–Manager |

Các trang danh sách mở chi tiết để xử lý; không thay đổi trạng thái chỉ bằng việc mở trang. Khi có cập nhật đơn, thông báo nhắc tải lại hiện trên trang để tránh làm mất phần đang thao tác.

## Chuẩn bị

1. Dùng tài khoản khách tạo đơn tại đúng chi nhánh nhân viên phụ trách. Ghi lại mã đơn.
2. Có ít nhất một đơn **nhận tại quầy + COD**, một đơn **giao tận nơi + COD**, và một đơn **chuyển khoản chưa thanh toán**.
3. Chuẩn bị một đơn ở chi nhánh khác để kiểm tra giới hạn quyền.
4. Chỉ dùng đơn kiểm thử để thực hiện các bước thu COD và hoàn tất; các thao tác này ghi nhận thanh toán/doanh thu của đơn.

## Kịch bản

| Mã | Bạn làm gì | Kết quả đúng |
|---|---|---|
| NV-01 | Đăng nhập nhanh bằng Staff | Vào Bàn làm việc nhân viên, thấy tên mình và đúng chi nhánh; có thanh dọc bên trái với các mục Đơn hàng, Chuẩn bị hoa, Giao & nhận hoa, Tin nhắn khách hàng |
| NV-02 | Tại Đơn hàng, tìm mã như `#HD35`, tên hoặc số điện thoại; thử lọc trạng thái/nhận tại quầy/giao tận nơi | Chỉ hiện các đơn phù hợp thuộc chi nhánh của mình; Xóa lọc trả về danh sách; trang sau/trước giữ bộ lọc |
| NV-03 | Mở chi tiết đơn kiểm thử có lời chúc, ngày giờ giao và nhiều bó hoa | Thấy đúng người nhận, số điện thoại, địa chỉ, lịch hẹn, thiệp, từng mẫu và số lượng. Đơn giá lấy từ lúc đặt, không lấy giá sản phẩm vừa sửa |
| NV-04 | Với đơn mới, bấm Xác nhận đơn → xác nhận hộp thoại | Đơn chuyển Chờ xác nhận → Đã xác nhận; xuất hiện trong Chuẩn bị hoa |
| NV-05 | Bấm Bắt đầu chuẩn bị hoa → xác nhận hộp thoại | Đơn chuyển Đang chuẩn bị hoa. Cảnh báo khách không còn tự hủy được xuất hiện trước khi xác nhận |
| NV-06 | Với đơn nhận tại quầy đang chuẩn bị, mở thao tác Giao hoa tại quầy · Hoàn tất | Không có bước Đang giao. Hộp thoại ghi đúng số tiền COD cần thu. Chưa đánh dấu đã giao/thu đủ tiền thì chưa gửi được; backend cũng chặn nếu bỏ qua biểu mẫu |
| NV-07 | Giao hoa và thu đủ tiền của đơn quầy, đánh dấu xác nhận rồi hoàn tất | Đơn Hoàn tất, thanh toán Đã thanh toán, số tiền còn cần thu bằng 0. Tải lại/gửi lại yêu cầu không tạo thanh toán hoặc doanh thu trùng |
| NV-08 | Với đơn giao tận nơi, sau chuẩn bị chọn Bàn giao vận chuyển · Đang giao → Xác nhận đã giao hoa → Hoàn tất đơn hàng | Đi lần lượt Đang chuẩn bị → Đang giao → Đã giao → Hoàn tất. Khi xác nhận đã giao đơn COD, phải xác nhận đã thu đủ tiền. Đã trả tiền thì bước Hoàn tất không thu thêm |
| NV-09 | Thử gửi lại yêu cầu hoàn tất từ khi đơn còn Chờ xác nhận, hoặc đổi sang Đã giao khi chưa Đang giao | Có thông báo lỗi; trạng thái và thanh toán không đổi |
| NV-10 | Mở đơn chuyển khoản chưa được xác nhận thanh toán | Có hướng dẫn chuyển cho quản lý; chưa có thao tác giao/hoàn tất. Nhân viên không tự xác nhận chuyển khoản. Sau khi quản lý xác nhận, thao tác giao xuất hiện; không yêu cầu thu COD |
| NV-11 | Mở Tin nhắn khách hàng; khách gửi tin từ tài khoản khác đến đúng chi nhánh | Nhân viên thấy hội thoại, đọc lịch sử, trả lời và gửi ảnh được. Tin khách–chi nhánh khác và chat Admin–Manager không được xem/gửi |
| NV-12 | Gõ URL chi tiết của đơn ở chi nhánh khác; thử gửi cập nhật trạng thái của đơn đó | Bị từ chối; không thấy thông tin người nhận và không sửa đơn ngoài chi nhánh |
| NV-13 | Quản lý gỡ nhân viên khỏi chi nhánh trong khi nhân viên còn đăng nhập | Các trang công việc hướng dẫn liên hệ quản lý; không còn xem/xử lý đơn hay chat của chi nhánh cũ |
| NV-14 | Dùng Staff mở các trang nhân sự, sản phẩm, ví, báo cáo hoặc cấu hình của Manager/Admin | Bị từ chối. Từ khu vực Staff cũng không có thao tác hủy đơn từ cửa hàng, hoàn tiền hoặc xác nhận chuyển khoản |

Ghi lại mã đơn, tài khoản, bước thực hiện và kết quả khi phát hiện lỗi. Không gửi mật khẩu, mã OTP, token hoặc khóa cấu hình vào báo cáo lỗi.

Trên màn hình nhỏ, dùng biểu tượng điều hướng để mở sidebar. Kiểm tra đóng bằng nút X, bấm nền tối hoặc phím Escape; trang đang chọn vẫn được đánh dấu.
