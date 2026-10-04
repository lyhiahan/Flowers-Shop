# Thanh toán cho môi trường đồ án

## MoMo

Mặc định `momo.mode=mock`: mô phỏng local, không cần khóa MoMo và không chuyển tiền thật. Chọn MoMo khi checkout sẽ mở trang DEV. Chỉ chủ đơn đã đăng nhập mới được POST nút mô phỏng với CSRF hợp lệ. Payment lưu `momo_environment=mock`, mã giao dịch `DEV-MOMO-<id>` và thông báo ghi rõ đây là mô phỏng.

Để thử API MoMo Test, đặt các biến môi trường trước khi chạy ứng dụng:

```text
MOMO_MODE=sandbox
MOMO_PARTNER_CODE=<partner code test của nhóm>
MOMO_ACCESS_KEY=<access key test của nhóm>
MOMO_SECRET_KEY=<secret key test của nhóm>
MOMO_REDIRECT_URL=https://<địa chỉ public>/checkout/momo/callback
MOMO_IPN_URL=https://<địa chỉ public>/checkout/momo/ipn
```

Không đưa khóa vào Git. Sandbox chỉ gọi `https://test-payment.momo.vn/v2/gateway/api/create`; không tự chuyển sang mock khi API lỗi. MoMo cần truy cập được IPN từ bên ngoài, nên localhost chỉ phù hợp với mock. Thanh toán sandbox dùng ứng dụng và tài khoản MoMo Test theo [hướng dẫn MoMo](https://developers.momo.vn/v3/docs/payment/api/wallet/onetime/).

GET callback từ trình duyệt không ghi nhận đã thanh toán. POST IPN phải có HMAC-SHA256 hợp lệ, đúng partner, request, mã lần thanh toán, số tiền, orderInfo và extraData đã lưu. Chỉ `resultCode=0` mới hoàn tất. Mỗi lần tạo URL có bản ghi `momo_payment_attempts` riêng; IPN lặp lại cùng giao dịch không tạo thông báo lần hai và không đổi lại đơn đã hoàn tiền. Chủ đơn khác không được xem hoặc mô phỏng thanh toán đơn của người khác. Cấu hình production bị từ chối trong bản dev này.

Migration bổ sung, không sửa dữ liệu thanh toán cũ: `sql/migrations/20261004_momo_dev.sql`. Project đang dùng Hibernate `ddl-auto=update`; SQL được cung cấp để nhóm có thể nâng schema rõ ràng trên SQL Server khi cần.

## Chuyển khoản ngân hàng

Checkout với `BANK_TRANSFER` chuyển tới `/checkout/bank/<id>`. Có thể mở lại từ danh sách hoặc chi tiết đơn chưa thanh toán.

- Ngân hàng: Techcombank.
- Chủ tài khoản: BUI THANH PHUC.
- Số tài khoản: 3337110941.
- Số tiền lấy từ tổng tiền đơn đã lưu, không nhận số tiền do trình duyệt gửi.
- Nội dung chuyển khoản: `DH<id>`.

Ảnh QR do người dùng cung cấp được lưu nguyên bản tại `static/images/bank-transfer-techcombank.jpg`. CSS chỉ cắt vùng hiển thị QR và giữ khoảng trắng xung quanh, không vẽ lại mã. QR này chứa tài khoản nhận; người mua cần nhập đúng số tiền và nội dung sau khi quét. Không phải QR động điền sẵn số tiền.

Hiện chưa tích hợp SePay hoặc webhook ngân hàng. Quản lý chi nhánh dùng chức năng xác nhận chuyển khoản hiện có sau khi kiểm tra tiền vào tài khoản. Trang hướng dẫn hỏi trạng thái từ server mỗi 10 giây, tối đa 30 phút; chỉ chủ đơn được đọc trạng thái. Xem trang hoặc quét QR không tự đổi thành PAID. Khi đã thanh toán, hủy hoặc hoàn tiền, trang ẩn hướng dẫn chuyển khoản.

Không thêm thư viện hoặc framework cho các thay đổi này.

## Admin xác nhận đã nhận tiền ngân hàng

Admin có nút **Xác nhận đã nhận tiền** tại danh sách `/admin/orders-manage` và phần xác nhận riêng trong chi tiết đơn. Chi tiết hiển thị tổng tiền của đơn và nội dung `DH<id>` để đối chiếu giao dịch trước khi xác nhận. Chỉ đơn `BANK_TRANSFER` chưa thanh toán, chưa hủy và không trong luồng hoàn tiền mới hiện nút.

Form gửi POST có CSRF, yêu cầu quyền Admin. Sau khi xác nhận, thanh toán chuyển sang `PAID` và bản ghi Payment sang `SUCCESS`; trạng thái xử lý đơn giữ nguyên. Xác nhận lặp không tạo thông báo trùng. Form từ chi tiết quay về đúng trang chi tiết và không chấp nhận redirect ra ngoài ứng dụng.

Kiểm thử ngày 04/10/2026: 57 test thuộc BusinessRegressionTests và AdminOrdersUiTests đều đạt, gồm xác nhận từ chi tiết, chặn khách hàng/thiếu CSRF, đơn hủy, xác nhận lặp và trạng thái phía khách. Kiểm tra HTTP chỉ đọc trên localhost cho thấy đơn #6 đã thanh toán nên nút được ẩn; không gửi xác nhận cho đơn thật.
