# Rà soát và sửa frontend — 03/10/2026

Giữ Spring Boot, Thymeleaf, Bootstrap, CSS và JavaScript hiện có. Không thêm dependency, framework hay bước build frontend.

## Đã sửa

- Khôi phục dấu đóng CSS ở trạng thái hover, font Material Symbols, kích thước logo và bố cục sidebar trên màn hình nhỏ.
- Chuyển 7 trang thiếu stylesheet tương ứng sang Bootstrap: hồ sơ, đơn hàng, yêu thích, đã xem, cửa hàng theo dõi, danh sách và chi tiết thành viên.
- Hồ sơ chỉ còn một form cập nhật; ảnh có preview, sổ địa chỉ thêm/sửa/xóa/mặc định, đổi mật khẩu có OTP, tab và modal vai trò dùng Bootstrap. Các trường của vai trò không được chọn bị vô hiệu hóa trước khi gửi.
- Dùng fragment head/header/footer cho các trang sửa; bổ sung viewport cho các template còn thiếu. Loại bỏ footer/thẻ đóng thừa, ID trùng và hai bản đánh giá sản phẩm.
- AJAX theo dõi kiểm tra HTTP, khóa nút khi đang gửi, hiển thị lỗi và không tự gửi lại thao tác toggle khi chưa rõ kết quả. Thông báo đã đọc dùng keepalive khi chuyển trang.
- Checkout dùng `POST /checkout/quote` có CSRF để lấy tiền hàng, phí ship, giảm giá và tổng tiền từ server. Báo giá không trừ kho, không tiêu thụ voucher; đặt hàng luôn tính lại giá hiện tại. Lỗi báo giá ngăn gửi đơn. Địa chỉ mặc định được điền ngay khi mở trang.
- Lưu `shippingFee` và `deliveryUnitId` vào đơn. Báo giá và tạo đơn dùng chung phép tính phí/voucher; voucher miễn ship không bị giới hạn bởi phần lãi của sản phẩm. Nhận tại cửa hàng có phí ship bằng 0.
- Giữ các overload OrderService cũ cho thành viên nhóm: lời gọi không có deliveryUnitId vẫn tương thích. Luồng checkout HTTP mới yêu cầu chọn đơn vị giao hàng khi có đơn vị hoạt động.

## Kiểm tra

```powershell
node scripts/check-frontend.cjs
node scripts/test-checkout-quote.cjs
$env:JAVA_HOME='D:/java/.tools/jdk21/jdk-21.0.12.1+1'
$env:Path=$env:JAVA_HOME+'/bin;'+$env:Path
mvn -B -ntp test
```

- 61 template và JavaScript nội bộ: kiểm tra cú pháp, ID trùng, cân bằng các thẻ cấu trúc và dấu ngoặc CSS.
- 133 test Maven qua, không bỏ qua test. Bổ sung kiểm tra giá báo/giá đơn/phí snapshot, miễn ship, đơn vị ngừng hoạt động, voucher khác chi nhánh, CSRF và render trang có dữ liệu.
- JavaScript checkout: kiểm tra trạng thái chờ, giá từ server, phản hồi cũ đến muộn, báo giá lỗi và ngăn gửi đơn khi chưa có giá hợp lệ.
- HTML được render bởi MockMvc lưu trong `target/frontend-smoke/`, dùng dữ liệu H2 test và tự rollback. Đây không phải screenshot trình duyệt.

## Chạy với database hiện có

Nếu Hibernate schema update bị tắt, áp dụng `sql/migrations/20261003_checkout_shipping.sql` trước khi triển khai. Migration chỉ thêm hai cột, không xóa hay thay thế dữ liệu đơn cũ. Khởi động lại ứng dụng để nạp Java mới nếu tiến trình đang chạy không có hot reload.

Kiểm tra giao diện thực tế ở 375, 768 và 1440 px vẫn cần lượt kiểm thử trình duyệt: menu, bảng, modal, ảnh, OTP, voucher và lỗi mạng. Không coi test render HTML là bằng chứng giao diện đúng ở các kích thước này.

Tên sản phẩm từng bị lưu thành dấu `?` vì `products.name` là `VARCHAR(200)`. Đã thêm mapping `@Nationalized` cho tên/mô tả Product, đổi cột tên thành `NVARCHAR(200)` và áp dụng `sql/migrations/20261003_product_unicode.sql` vào database local đang chạy trên cổng 1433.

Migration khôi phục 33 tên bằng whitelist 27 mẫu có sẵn trong `ProductSeederService.java`: chỉ sửa tên chứa `?` khớp duy nhất với ký tự còn giữ được và giá gốc. Không đổi giá, tồn kho, chi nhánh, ảnh hay đơn hàng. Tên trước/sau được lưu trong `dbo.product_name_unicode_repair_backup`. Script tạo whitelist là `scripts/build-product-unicode-repair.cjs`; không dùng phép thay `?` tùy ý.

Sau sửa, HTTP `/products?size=100` trả về 33 tên có đủ dấu, không còn tên chứa `?`. Thêm test lưu/reload/render tiếng Việt; tổng 134 test Maven qua. Dữ liệu tự nhập không có nguồn gốc đối chiếu sẽ không được tự đoán để sửa.
