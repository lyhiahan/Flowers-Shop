# Rà soát ràng buộc nghiệp vụ ngày 04/10/2026

Phạm vi: xác thực và ô trống, sản phẩm Admin/Manager, chọn hàng và giỏ hàng, checkout, tồn kho đồng thời, voucher, trạng thái đơn, chuyển khoản, MoMo dev và bảo vệ các thao tác thay đổi dữ liệu. Không thêm thư viện.

## Các thay đổi

| Trường hợp | Xử lý ở server |
| --- | --- |
| Hai người mua món cuối | Khóa hàng tồn theo thứ tự product ID, đọc lại tồn sau khi lấy khóa, trừ trong cùng transaction tạo đơn. Giữ `@Version` hiện có. Một người thành công; người còn lại bị từ chối, không tạo đơn hoặc trừ thêm hàng. |
| Gửi lại cùng giỏ đồng thời | Khóa giỏ hiện có bảo đảm chỉ tạo một đơn. |
| Hai đơn hủy đồng thời | Khóa và đọc lại từng hàng tồn trước khi hoàn số lượng; không mất phần hàng hoàn của người khác. |
| Hai người dùng voucher cuối | Khóa voucher hiện có và đọc lại quota sau khi lấy khóa; giao dịch thất bại rollback cả tồn kho và lượt dùng. |
| Giá âm/0/chữ/quá nhiều chữ số | Admin binding báo lỗi ngay ở form; service kiểm tra lại giá dương và tối đa 16 chữ số, 2 số thập phân. Manager trả lỗi đầu vào thay vì lỗi 500. |
| Tên sản phẩm trống/quá dài, tồn kho âm/quá lớn | Từ chối trước khi ghi dữ liệu. Service kiểm tra độc lập với form. |
| Tạo sản phẩm chỉ cho một chi nhánh | Lưu đúng chi nhánh đã chọn và chỉ khởi tạo tồn tại đó; không âm thầm áp dụng toàn hệ thống. |
| Sửa sản phẩm đã xóa hoặc chi nhánh không tồn tại | Từ chối, không biến chi nhánh sai thành sản phẩm toàn hệ thống. |
| Upload lỗi IOException | Rollback thao tác tạo/sửa sản phẩm; không commit một phần do checked exception. |
| Số lượng giỏ âm hoặc bỏ trống | Báo lỗi, giữ nguyên mục giỏ. Giá trị 0 vẫn được dùng để xóa theo hành vi hiện có. |
| Chọn ID ngoài giỏ, chi nhánh sai, sản phẩm đã xóa | Từ chối; không âm thầm thay chi nhánh hoặc bỏ qua ID không thuộc giỏ. |
| Checkout bỏ trống ô bắt buộc | Báo thiếu tên, số điện thoại, địa chỉ giao hàng, ngày nhận hoặc sản phẩm; không tự lấy dữ liệu hồ sơ để tiếp tục. Đến lấy yêu cầu chọn chi nhánh; giao hàng yêu cầu đơn vị vận chuyển. Lời chúc là tùy chọn. |
| Điện thoại sai, nội dung dài vượt cột DB | Từ chối trước khi tạo đơn. |
| Đăng ký bỏ trống | Form và service kiểm tra tên đăng nhập, mật khẩu, họ tên, email, điện thoại, tỉnh/thành phố, quận/huyện. Mật khẩu vượt giới hạn 72 byte UTF-8 của BCrypt bị từ chối. |
| Login bỏ trống | Đăng nhập thất bại, không tạo phiên đăng nhập. |
| Xác nhận chuyển khoản lặp | Khóa đơn, chỉ quản lý chi nhánh đúng được xác nhận; không tạo thêm thông báo khi đã PAID. |
| Giả callback MoMo, IPN sai chữ ký/đơn/tiền, phát lại IPN | Callback chỉ đọc; sandbox kiểm tra HMAC và correlation; IPN lặp không ghi nhận lần hai hoặc hồi sinh đơn hoàn tiền. Mock chỉ có POST của chủ đơn kèm CSRF. |
| GET xóa giỏ hoặc mở lại giao dịch MoMo | Chuyển sang POST có CSRF. Endpoint checkout cũ từ giỏ chuyển tới form checkout chung. |
| Chuyển hướng do người dùng cung cấp | Chỉ chấp nhận các đường dẫn nội bộ tương ứng; không redirect ra website khác. |
| API dùng session | Bỏ ngoại lệ CSRF `/api/**`; chỉ giữ các endpoint auth được chỉ định và IPN bên ngoài. |
| Lỗi nhập chữ, thiếu tham số, JSON hỏng | Trả lỗi đầu vào 400; sai phương thức 405; không lộ thông tin exception nội bộ trên trang lỗi 500. |

## Kiểm thử

`BusinessRegressionTests`: kiểm tra form rỗng, sai kiểu, các giá trị biên, quyền sở hữu, tồn kho và bản ghi không đổi sau yêu cầu bị từ chối.

`ConcurrencyRegressionTests`: dùng hai luồng với hai transaction thực trên H2 để thử món cuối, cùng giỏ, voucher cuối, hủy đồng thời và rút ví đồng thời. Không phải kiểm thử tải hoặc kết quả thực nghiệm race trên SQL Server production.

`MomoSandboxSecurityTests`: payload giả lập có chữ ký test, kiểm tra IPN hợp lệ, sai chữ ký, sai binding, đơn hủy, replay và giới hạn sandbox. Chưa gọi gateway thật bằng khóa test của nhóm.

Lượt xác minh riêng: 75/75 test đạt (45 nghiệp vụ, 6 đồng thời, 12 MoMo sandbox, 1 cấu hình MoMo, 11 quản lý sản phẩm). Sau khi bổ sung kiểm tra tỉnh/quận đăng ký, chạy lại 45/45 test nghiệp vụ đều đạt. Kiểm tra cú pháp 62 template và toàn bộ JavaScript local không có lỗi.

HTTP local: đăng nhập demo, danh sách đơn và ảnh QR trả 200. Tài khoản demo không có đơn bank/MoMo chưa thanh toán để mở form trên DB thật; form và endpoint trạng thái đã được render/test bằng fixture H2. Computer Use hiện không kết nối được native pipe nên chưa có ảnh chụp trình duyệt mới cho lượt này.

Server đã được khởi động lại với bản sửa mới tại `http://localhost:8080/`. Sau khởi động, 16 trang khách/manager/admin trong `target/check-live-report.ps1` trả HTTP 200, không phát hiện lỗi truy vấn giao dịch; danh sách đơn và ảnh QR cũng trả 200. URL SQL Server được override khi chạy local; không đổi cấu hình kết nối chung của nhóm.

Lần chạy toàn bộ sau merge phát hiện 16 failure về giao diện dashboard/catalog đã không khớp các test hiện có (AdminNewStoresStatistics, AdminNewUsersStatistics, CatalogFilter, StoreManagementRevenue). Các failure này được ghi riêng; không sửa assertion để che lỗi. Các kiểm thử nghiệp vụ mới được chạy và xác minh riêng.

Lượt tổng cuối sau toàn bộ sửa đổi: 176 test, 160 đạt, 16 failure giao diện nêu trên, 0 error. Log: `target/hardening-verified-full.log`. Bộ test không được coi là đạt toàn bộ.

Hướng dẫn cấu hình và giới hạn thanh toán: [PAYMENT_DEV.md](PAYMENT_DEV.md).

## Bổ sung: dropdown chuyển trạng thái đơn nhận tại tiệm

Ảnh 09:22 cho thấy đơn đã thanh toán MoMo nhưng chọn `PREPARING -> DELIVERING` bị từ chối. DB xác nhận đơn #1 là `PICKUP`, đang `PREPARING`, thanh toán `PAID`. Server từ chối bước vận chuyển cho đơn nhận tại tiệm là đúng; dropdown Admin đã đưa ra lựa chọn sai.

Đã dùng chung `OrderWorkflowService.availableTransitions` cho kiểm tra chuyển trạng thái và dropdown Admin/Manager. Đơn nhận tại tiệm chuyển từ Chuẩn bị sang Khách nhận hoa tại quầy - Hoàn tất khi khách nhận hàng. Đơn giao tận nơi đi qua Đang giao, Đã giao rồi Hoàn tất; dropdown không đưa ra bước nhảy hoặc hủy sau bàn giao mà server không chấp nhận. Lỗi cố gửi vận chuyển cho đơn nhận tại tiệm được giải thích bằng tiếng Việt.

Kiểm thử mới render cả hai trang chi tiết, xác minh dropdown, thử yêu cầu sai không làm đổi trạng thái thanh toán và hoàn tất đơn nhận tại tiệm bằng fixture H2. Không tự hoàn tất đơn #1 thật. HTTP trên server xác nhận `/admin/orders-manage/1` trả 200, có COMPLETED và không có DELIVERING trong dropdown.

Các test cũng phát hiện và sửa hai tham chiếu không tồn tại tại trang chi tiết đơn Manager: `product.images` đổi sang `product.imageUrl` có fallback; `detail.price` đổi sang giá chốt `detail.unitPrice` và `detail.subtotal`.

Sau sửa template, 47/47 BusinessRegressionTests đạt, gồm hai test mới cho luồng nhận tại tiệm và giao tận nơi. Các bộ AdminOrders (8), StoreManagementOrder (5), Concurrency (6), MoMoSandboxSecurity (12) trong lượt kiểm tra liên quan đều đạt. Kiểm tra cú pháp 62 template/JavaScript đạt. Không commit/push.
