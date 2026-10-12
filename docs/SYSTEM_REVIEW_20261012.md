# Đánh giá và chạy thử hệ thống — 12/10/2026

## Kết luận

Mã nguồn đã triển khai Spring Boot, Thymeleaf, Bootstrap, JPA, SQL Server, WebSocket, JWT và Cloudinary; nghiệp vụ có nhiều chi nhánh, tồn kho riêng và phân quyền theo chi nhánh. Lỗi SQL Server đã được sửa: đúng instance SQLEXPRESS, migration thật lên V4 và website chạy thành công với RentalShop. Mail API/Cloudinary/JWT production vẫn cần cấu hình và xác minh riêng; chưa thể xác nhận 100% mọi dịch vụ ngoài.

Lượt đánh giá ban đầu không sửa database gốc. Trong lượt sửa SQL Server tiếp theo, người dùng đã chọn instance và đồng ý baseline/migrate RentalShop sau khi backup/thử trên bản sao. Không reset database, không gửi email/upload ảnh/thu tiền thật, không commit/push hoặc mở browser/link ngoài. Chi tiết: [sửa và xác minh SQL Server](SQLSERVER_REPAIR_20261012.md).

## Cấu trúc hiện tại

228 lớp Java; 45 controller; 39 entity; 39 repository; 71 template; 52 lớp kiểm thử Java.

| Thư mục | Vai trò |
|---|---|
| `src/main/java/com/finalproject/config`, `security` | Cấu hình, đăng nhập/session/JWT, WebSocket và kiểm tra quyền |
| `controller/admin`, `controller/manager`, `controller/customer`, `controller/auth` | Màn hình và API theo nhóm người dùng |
| `entity`, `repository`, `service`, `service/impl`, `dto`, `utils` | Dữ liệu JPA, truy vấn, nghiệp vụ, DTO và tiện ích |
| `src/main/resources/templates`, `static` | Thymeleaf, Bootstrap, CSS và JavaScript |
| `src/main/resources/db/migration`, `src/main/java/com/finalproject/db/migration` | Migration Flyway cho SQL Server |
| `src/test/java`, `src/test/js` | Kiểm thử backend, HTTP/WebSocket và hành vi JavaScript |
| `scripts`, `docs` | Script chạy JDK/Maven/Node và hướng dẫn/báo cáo |
| `sql`, `database.sql` | SQL lịch sử; script reset không dùng để nâng cấp database thật |
| `target`, `.tools`, `upload` | Sản phẩm build, runtime portable và ảnh cục bộ |

## Đã sửa trong lượt kiểm tra này

1. Bộ lọc Hôm nay / 7 ngày / 30 ngày / Từ ngày–Đến ngày dùng chung cho dashboard admin, dashboard cửa hàng và báo cáo doanh thu theo đơn/sản phẩm. Mặc định 7 ngày gồm hôm nay; bao gồm trọn ngày bắt đầu/kết thúc theo UTC+7.
2. Tổng số, biểu đồ, sản phẩm bán chạy và đơn gần đây được lọc cùng khoảng ngày và đúng chi nhánh. Người dùng/cửa hàng/sản phẩm mới dùng ngày tạo; đơn và doanh thu dùng ngày tạo đơn. Biểu đồ không còn bị giới hạn 1.000 đơn; danh sách xếp hạng vẫn giới hạn và phân trang.
3. Giữ khoảng ngày khi đổi tiêu chí, biểu đồ, chế độ báo cáo sản phẩm/đơn hoặc chuyển trang. Kiểm tra thiếu ngày, ngày đảo ngược, định dạng sai ở cả giao diện và backend. Khoảng dài gom tháng/năm nhưng không kéo rộng giới hạn dữ liệu.
4. Bỏ dữ liệu doanh thu giả dự phòng trong JavaScript, bổ sung trạng thái không có doanh thu. Ghi rõ điểm xếp hạng là điểm tích lũy hiện tại của các tài khoản/cửa hàng tạo trong kỳ.
5. Trang analytics kiểm tra quyền quản lý chi nhánh giống dashboard; nhân viên membership STAFF không được xem báo cáo tài chính dành cho quản lý.
6. Bỏ in/log mã OTP và nội dung lỗi SMTP có thể chứa mã. Không báo đã gửi OTP khi SMTP thiếu cấu hình/gửi thất bại. Đăng ký thất bại được phép thử lại; gửi lại thất bại giữ mã đã gửi trước đó. Đổi mật khẩu gửi OTP thất bại trả HTTP 503 và xóa riêng bản OTP chưa gửi, không xóa lịch sử đã có.

Các yêu cầu hủy đơn trước khi giao và hiển thị tồn kho ở trang chủ đã được thực hiện ở lượt trước; regression test vẫn kiểm tra chúng.

## Cách chạy thử

```powershell
.\scripts\maven.ps1 package
.\scripts\test-js.ps1
```

Test dùng H2 riêng trong chế độ SQL Server; `SystemHttpSmokeTests` khởi động server HTTP thật ở cổng ngẫu nhiên. Kiểm tra 25 lượt mở các trang chính: trang chủ, danh sách/chi tiết sản phẩm, cửa hàng, đăng nhập/đăng ký/quên mật khẩu, giỏ hàng/hồ sơ/đơn của khách, trang quản lý và admin. Có kiểm tra tồn kho trang chủ, file JS, đăng nhập session, 401/403/302 và dữ liệu bộ lọc ngày.

`ChatSessionIntegrationTests` đăng nhập qua HTTP rồi trao đổi tin nhắn WebSocket thật giữa khách, quản lý và admin. Các nhóm khác kiểm tra đặt hàng, thanh toán mô phỏng, cạnh tranh tồn kho, hủy/hoàn tiền, phân quyền, JWT và upload Cloudinary với mock.

Kết quả lượt cuối lúc 13:52 ngày 12/10/2026: **400 test Java, 0 failures, 0 errors, 1 skipped** (399 pass; Cloudinary live cần credential nên bỏ qua). **13/13 test JavaScript pass**. HTTP/WebSocket thật trên localhost đã chạy thành công; `maven package` báo **BUILD SUCCESS**, tạo `target/finalproject-0.0.1-SNAPSHOT.jar`. Log cuối: `.tools/full-system-verification-final.log`; log chẩn đoán không đưa vào Git. Bản chạy thử đã tự dừng sau test, không để server/database kiểm thử chạy nền.

Cập nhật sau sửa SQL Server: **402 test mặc định, 0 failures/errors, 3 skipped** (399 pass; thêm 2 test SQL Server opt-in). **2/2 test SQL Server live chạy riêng đạt**, kiểm tra V1–V4 trên database rỗng và V2–V4 trên bản sao, validate entity và bảo toàn số dòng. RentalShop gốc đã lên V4 sau khi người dùng đồng ý; website chạy/đọc dữ liệu SQL Server thật thành công. Log mới: `.tools/sqlserver-change-regression.log`, `.tools/sqlserver-live-validation.log`.

Cập nhật sau đổi OTP sang mail API: **435 test, 0 failures/errors, 3 skipped** (432 pass), đóng gói JAR thành công. Mặc định không yêu cầu Gmail App Password, có script nhập/lưu API key mã hóa ngoài code; chưa xác nhận hộp thư thật vì cần credential/người gửi của dịch vụ. Xem [hướng dẫn OTP qua email](OTP_EMAIL_SETUP_20261012.md).

## Cần hoàn tất trước khi nộp/demo bằng môi trường thật

| Ưu tiên | Việc còn cần | Lý do |
|---|---|---|
| Đã hoàn tất local | Kết nối đúng SQLEXPRESS bằng tên instance; dùng `scripts/sqlserver.ps1` | JDBC xác định đúng cổng động 53735; không còn trỏ local vào 1433 |
| Đã hoàn tất local | Backup/verify, thử migration trên SQL Server thật rồi nâng cấp RentalShop được người dùng chấp thuận | Flyway V4 và Hibernate validate thành công; số dòng chính giữ nguyên |
| Bắt buộc | Cấu hình Cloudinary, thử upload/hiển thị/xóa ảnh thật | Test live đang bỏ qua; mock không xác nhận credential hoặc mạng |
| Bắt buộc cho OTP | Cấu hình Brevo API key/sender bằng `scripts/configure-mail.ps1`, thử đăng ký/kích hoạt/quên mật khẩu bằng email nhận được | Không cần Gmail App Password; hiện chưa có mail credential nên chưa xác nhận giao thư thật |
| Bắt buộc khi triển khai | Khóa JWT riêng ổn định, profile prod, HTTPS; thay credential cũ đã từng vào Git | Không dùng khóa tạm hoặc secret đã lộ cho môi trường thật |
| Khi yêu cầu thu tiền thật | Tích hợp/kiểm tra cổng thanh toán thật và đối soát hoàn tiền | MoMo/bank hiện là mô phỏng; hoàn tiền cần được xác nhận thực tế, không coi yêu cầu hoàn là đã hoàn |

Không gửi secret trong chat; đặt chúng trong terminal/IDE hoặc secret manager. README hướng dẫn chạy `spring-boot:run` khi cấu hình đã sẵn sàng.

## Góp ý cải thiện tiếp theo

- Dữ liệu lớn: chuyển thống kê sang aggregate/projection và phân trang database để giảm tải toàn bộ entity/quan hệ; giữ số tổng đầy đủ. Dần thay truy cập lazy trong template bằng DTO trước khi tắt `open-in-view`.
- Triển khai public: bổ sung giới hạn request đăng nhập/OTP theo IP và tài khoản tại reverse proxy, giám sát lỗi mail/payment, backup và thử khôi phục.
- Nhiều server/restart: chuyển đăng ký đang chờ OTP khỏi map trong RAM sang kho lưu có TTL; hiện restart làm mất đăng ký chưa xác thực.
- Bổ sung kiểm thử trình duyệt trên điện thoại/desktop, CDN mất kết nối và tải đồng thời. Lượt này xác nhận render HTTP và JavaScript unit, chưa phải kiểm thử trực quan/E2E trình duyệt hay load test.

Đây là các hạng mục vận hành/chất lượng tiếp theo, không đồng nghĩa đề bắt buộc Redis, Docker, payment thật hoặc dùng đồng thời ba hệ quản trị database.
