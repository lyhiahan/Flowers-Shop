# Kết quả dọn tệp và kiểm tra hệ thống ngày 29/10/2026

## Phạm vi

Đối chiếu mã nguồn tại commit `f4b4d9b`, kiểm tra website đang chạy trên localhost và SQL Server local `RentalShop` tại cổng `53735`. Các kiểm thử nghiệp vụ Java dùng database H2 riêng theo profile `test`; không tạo đơn, thanh toán, mở khóa tài khoản hoặc sửa tồn kho trong database local đang sử dụng.

## Những tệp đã dọn

| Tệp hoặc nhóm tệp | Lý do |
|---|---|
| `src/main/resources/templates/admin/product-list.html` | Giao diện sản phẩm cũ không còn được controller hoặc template gọi tới; màn hình hiện hành là `admin/products-manage.html`. |
| `docs/BAO_CAO_WEB_CO_DANH_MUC_TU_VIET_TAT.docx` | Bản Word trung gian; bản đã bổ sung minh chứng Cloudinary và kịch bản thuyết trình được giữ lại. |
| Năm tệp `.tools/manager-choices-*.png` và `.tools/manager-choices-*.html` đã xác định | Ảnh xem thử và HTML sinh tạm từ các lần chỉnh giao diện trước, gồm một tệp HTML rỗng. |
| Hai thư mục `target/start-web-fixture-*` từ lần kiểm tra này | Chỉ chứa cấu hình thử và script Maven giả lập; không phải cấu hình ứng dụng thật. |

Bảy tệp cũ chiếm 46.784.272 byte, tương đương khoảng 46,8 MB, chưa tính các thư mục fixture nhỏ. Script `scripts/test-start-web.ps1` nay kiểm tra đường dẫn và tự dọn thư mục fixture của chính lần chạy trong khối `finally`, kể cả khi kiểm tra thất bại. Giữ nguyên database, tệp cấu hình riêng, ảnh upload, JDK, Node.js, cloudflared, migration và các bản báo cáo cuối đang sử dụng.

## Kiểm tra website và database local

| Nhóm kiểm tra | Kết quả |
|---|---|
| Trang công khai và biểu mẫu xác thực | Đã kiểm tra health, trang chủ, sản phẩm, cửa hàng, thành viên, đăng nhập, đăng ký và quên mật khẩu. |
| ADMIN | Đăng nhập; thống kê, tài khoản, cửa hàng, hạng chi nhánh, hoa hồng, sản phẩm, biểu mẫu sản phẩm, đơn hàng, danh mục, thuộc tính, giao hàng, voucher, cấp bậc khách, giao dịch, hội thoại, nhật ký giám sát và thông báo. |
| MANAGER | Đăng nhập; tổng quan, cửa hàng phụ trách, hồ sơ chi nhánh, sản phẩm, nhân viên, đơn hàng, voucher, ví, doanh thu và thông báo. |
| STAFF | Đăng nhập; danh sách đơn, chuẩn bị hoa, giao nhận và hội thoại. |
| USER | Đăng nhập; hồ sơ, yêu thích, theo dõi, đã xem, giỏ hàng, đơn hàng, ví và thông báo. |
| Phân quyền | USER, MANAGER và STAFF không mở được trang quản trị; ADMIN vào giỏ hàng được điều hướng về trang quản trị. |
| Tổng kiểm tra HTTP local | **55/55 đạt**. Các kiểm tra này xác nhận mã HTTP, trang đích và việc tải trang, không phải kiểm thử thao tác trình duyệt bằng ảnh chụp. |
| SQL local | Không có tồn kho chi nhánh âm, ví khách âm hoặc voucher có số lượt dùng âm/vượt số lượng. Migration gần nhất: V18, trạng thái thành công. |

## Kiểm thử tự động

- JavaScript: **186/186 đạt** trên bản mã mới nhất; có kiểm tra tương tác giao diện, giỏ hàng, dữ liệu tồn kho và gateway QR nội bộ.
- Script khởi động: **12/12 đạt**; chỉ dùng fixture và Maven giả lập, xác nhận cấu hình, bảo toàn biến môi trường và không tự nhập dữ liệu/migration khi chưa chọn; không còn thư mục fixture sau khi chạy.
- Java: **BUILD SUCCESS**; tổng cộng **882 ca**, trong đó **870 đạt**, **12 bỏ qua**, **0 thất bại**, **0 lỗi**. Các ca bỏ qua là những kiểm tra tích hợp cần bật riêng; không được tính là đã đạt. Bộ test bao gồm tạo và hủy đơn, kiểm tra tồn kho, voucher, thanh toán demo, hoàn khoản, phân quyền và HTTP/WebSocket trong môi trường thử.

Lần chạy Java ban đầu phát hiện các ca phân quyền dùng tài khoản mock chưa tồn tại trong database thử, khiến bộ lọc thu hồi phiên chạy trước bước kiểm tra quyền. Đã bổ sung tài khoản USER hoạt động vào đúng ca thử để tiếp tục kiểm tra HTTP 403 tại chức năng được bảo vệ; không đổi sang chấp nhận truy cập. Các ca tài khoản đã bị xóa hoặc vô hiệu hóa được đối chiếu với chuyển hướng trình duyệt `/login?locked=true`, còn yêu cầu AJAX vẫn phải bị từ chối HTTP 403. Các thay đổi này chỉ nằm trong dữ liệu thử và kỳ vọng kiểm thử, không nới quyền của mã ứng dụng.

## Giới hạn kết luận

Luồng tạo đơn, thanh toán, hoàn khoản, kiểm soát tồn kho và hội thoại được kiểm tra bằng bộ kiểm thử trong môi trường riêng. Phiên kiểm tra local chỉ đọc trang và dữ liệu; kết quả này không xác nhận giao dịch ngân hàng thật, thư OTP thật, upload Cloudinary mới hoặc quét QR trên điện thoại qua mạng ngoài. Các dịch vụ đó cần một phiên kiểm tra tích hợp riêng với cấu hình và thiết bị thực tế.

Các thay đổi để lại trong working tree để xem xét; không thực hiện commit hoặc push trong phiên này.
