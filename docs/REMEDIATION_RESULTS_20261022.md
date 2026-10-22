# Kết quả khắc phục hệ thống — 22/10/2026

Mốc rà soát ban đầu: commit merge `ca2b9a7` ngày 21/10. Các thay đổi dưới đây đang ở working tree, chưa commit/push. Người dùng đã yêu cầu tiếp tục triển khai và xác nhận toàn bộ dữ liệu liên quan là demo.

## Kết quả thay đổi

| Vấn đề | Kết quả |
| --- | --- |
| F01 — trùng phiên bản V11 | Khôi phục đúng file V11 gốc từ checkout đã lưu, giữ checksum `306107251`; chuyển migration mới sang V12–V15. Database mới và bản sao V11 migrate bằng Flyway chuẩn, không repair lịch sử. `RentalShop` hiện ở V15. |
| F02 — giỏ có khóa NULL bị tăng số lượng | V15 giữ nguyên các dòng thiếu khóa để đối soát riêng; chỉ gộp dòng có đủ cart/product/store. Kiểm thử SQL Server chạy hai lần giữ tổng 5 đơn vị và bảo toàn thông tin dòng. |
| F03 — thanh toán mô phỏng trong prod | Prod mặc định tắt MoMo, ngân hàng dùng manual; cấu hình mock/sandbox không phù hợp bị từ chối khi khởi động. Service thanh toán và tạo đơn kiểm tra MoMo trước khi thay đổi dữ liệu. |
| F04 — XSS tên cửa hàng trong giỏ | Modal tạo text node cho tên cửa hàng; có kiểm thử payload HTML bằng DOM giả lập. |
| F05 — phiên cũ sau đổi/reset mật khẩu | Principal giữ dấu vân tay của credential qua quá trình eraseCredentials; filter đối chiếu ở request tiếp theo. Kiểm thử đăng nhập hai phiên, đổi mật khẩu và reset qua OTP. Phiên form-login cũ không có dấu vân tay phải đăng nhập lại. |
| F06 — dữ liệu nhạy cảm trong thông báo | WebSocket phát DTO chỉ chứa các trường thông báo cần thiết sau commit; password/OTP của User bị chặn serialization. |
| F07 — đơn demo thiếu chứng từ | Seeder tạo receipt cho đơn mẫu mới. Đối soát riêng 15 đơn cũ bằng receipt mô phỏng `DEMO-RECONCILE-{orderId}`, có bảng audit. Đây là chứng từ demo, không chứng minh giao dịch tiền thật. |
| F08 — kho lệch chi nhánh | Cách ly 22 dòng bằng cờ không bán/đã xóa, lưu snapshot audit và giữ nguyên 212 đơn vị. Không tự gán sang chi nhánh khác. |

Cải tiến kèm theo: phân trang tìm người dùng ở database và giới hạn kích thước trang; dùng Clock của ứng dụng cho AuthService/voucher; lưu BCrypt thay mật khẩu rõ trong đăng ký chờ xác thực; bỏ khóa toàn cục quanh đăng ký/gửi mail ban đầu; chuẩn hóa cấu hình SQL Server và script chạy cục bộ; thêm workflow Java/JavaScript CI. Workflow chưa chạy trên GitHub vì chưa push.

## Dữ liệu và khả năng khôi phục

Đã diễn tập trên `CodexWebPreview_20261021_2210` trước khi áp dụng vào `RentalShop`. Cả hai backup COPY_ONLY có CHECKSUM đều đã được `RESTORE VERIFYONLY`:

- `C:\Program Files\Microsoft SQL Server\MSSQL17.MSSQLSERVER\MSSQL\Backup\RentalShop_before_remediation_20261021_234132.bak` — trước migration.
- `C:\Program Files\Microsoft SQL Server\MSSQL17.MSSQLSERVER\MSSQL\Backup\RentalShop_before_demo_reconciliation_20261021_234149.bak` — trước đối soát demo.

Script `scripts/reconcile-confirmed-demo.ps1` mặc định diễn tập/rollback, chỉ Apply khi chỉ định rõ; Apply tự sao lưu và xác minh trước. SQL có transaction, kiểm tra trạng thái đầu vào và hậu điều kiện. Audit được lưu trong `dbo.demo_payment_reconciliation_20261021` và `dbo.demo_inventory_reconciliation_20261021`. Script rollback nằm tại `scripts/sql/rollback-demo-reconciliation-20261021.sql`, có chặn nếu dữ liệu đã thay đổi sau đối soát; đã thử bằng transaction rollback trên bản sao.

Đối chiếu chỉ đọc ngày 22/10:

| Chỉ số | Kết quả |
| --- | ---: |
| Schema / migration lỗi | V15 / 0 |
| Đơn / payment | 18 / 16 |
| Đơn PAID thiếu SUCCESS receipt khớp tiền | 0 (trước: 15) |
| Receipt demo mới, có audit | 15 |
| Kho lệch chi nhánh đã cách ly / số lượng giữ lại | 22 / 212 |
| Kho lệch chưa xử lý / sai lệch với snapshot cách ly | 0 / 0 |
| Tổng quantity toàn kho / KPI kho hợp lệ | 1225 / 1013 |
| Ví âm / khóa ngoại không được SQL Server tin cậy | 0 / 0 |

Trong transaction đối soát, checksum đơn hàng và ví được kiểm tra không đổi; không phát sinh chuyển tiền hoặc hoàn tiền. Các dòng kho cách ly vẫn tồn tại để truy vết.

## Kiểm thử

- Java toàn bộ: **665 ca: 653 đạt, 12 bỏ qua, 0 thất bại/lỗi**, `BUILD SUCCESS` lúc 12:00:23 UTC+7 ngày 22/10; log `target/remediation-all-20261022.log`. Các ca bỏ qua cần cấu hình/điều kiện live riêng; SQL Server đã được chạy bổ sung như dòng bên dưới. Fixture thời hạn thanh toán được chuẩn hóa microsecond trước khi lưu H2 để kiểm tra chính xác thời hạn không bị gia hạn, không nới điều kiện nghiệp vụ.
- JavaScript: **121/121 đạt**, log `target/remediation-js-20261022.log`.
- SQL Server live: **9 ca: 8 đạt, 1 bỏ qua**, không lỗi/thất bại; log `target/remediation-sql-live-v4.log`. Đã chạy database mới V1→V15, bản sao hiện tại V11→V15, validate checksum, chạy migrate lần hai và các fixture giỏ/thông báo. Ca baseline database trước V2 cần bộ dữ liệu legacy riêng nên không bật.
- Đã diễn tập script đối soát/rollback, kiểm tra hậu điều kiện trên database gốc và `git diff --check`.

Log/PID/JSON dưới `target/` là artifact cục bộ, có thể bị Maven clean xóa; báo cáo này lưu kết luận để theo dõi cùng mã nguồn.

## Khởi chạy và phạm vi còn lại

Chạy từ PowerShell tại thư mục dự án:

```powershell
.\scripts\run-local.ps1 -DatabaseName RentalShop -Mode Validate
.\scripts\run-local.ps1 -DatabaseName RentalShop -Mode Run
```

Script nạp `.env`, dùng SQL Server `127.0.0.1:1433`, bật Flyway và không tự baseline. Profile dev, chỉ bind `127.0.0.1:8080`, tắt seed/tunnel. Giữ nguyên file `start-web.ps1` có sẵn của người dùng.

Đã khởi động web lúc **12:02:00 UTC+7 ngày 22/10**, Java PID **4016** (launcher PowerShell **11372**), tại **http://localhost:8080**. Log xác nhận catalog `RentalShop/dbo`, Flyway validate thành công và schema V15 đã cập nhật đầy đủ. Không dùng lại bản preview tắt Flyway.

Kiểm tra HTTP sau khởi động: `/`, `/login`, `/products`, `/stores`, `/users` và `/users?page=2147483647&size=2147483647` đều **200**; `/cart` và `/admin/dashboard` đều **302 về `/login`** khi chưa đăng nhập. Đây là kiểm tra HTTP bằng client cục bộ, chưa phải kiểm tra hiển thị bằng trình duyệt. Đối chiếu database sau các request vẫn cho kết quả như bảng trên. Log chạy: `target/web-remediated-20261022.log`; metadata/PID: `target/web-remediated-20261022.json`; kết quả HTTP: `target/web-http-checks-20261022.json`.

Chưa nghiệm thu thủ công toàn bộ UI desktop/mobile, gửi email/Cloudinary thật hoặc cổng thanh toán live. Các phần tối ưu thống kê/OSIV, Clock ở controller còn dùng giờ hệ thống và khóa đồng bộ resend/verify OTP là cải tiến tiếp theo. Các kiểm thử tự động đạt không đồng nghĩa đã sẵn sàng bán hàng thật.
