# Chuỗi cửa hàng hoa — Spring Boot

Website bán hoa theo mô hình nhiều chi nhánh. Khách hàng xem sản phẩm, chọn chi nhánh còn hàng, đặt hàng, thanh toán và chat với cửa hàng/admin. Quản lý xử lý tồn kho, đơn hàng, nhân sự, voucher, doanh thu và ví theo quyền tại từng chi nhánh. Admin quản trị toàn chuỗi.

Khách được hủy đơn ở trạng thái chờ xác nhận, đã xác nhận hoặc đang chuẩn bị, không giới hạn một giờ kể từ lúc đặt. Khi đơn bắt đầu giao hoặc đã kết thúc thì không được hủy. Hủy đơn chưa thanh toán trả tồn kho và chuyển sang đã hủy; đơn đã thanh toán chuyển sang chờ cửa hàng hoàn tiền, không tự ghi nhận tiền đã được hoàn.

Trang chủ hiển thị số lượng còn lại ở cả hoa bán chạy và danh sách sản phẩm. Số lượng chỉ tính tại chi nhánh hoạt động và mở cửa, hoặc riêng chi nhánh đang lọc. Hết hàng có nhãn rõ ràng và khóa nút thêm vào giỏ; sản phẩm chung toàn chuỗi được chọn chi nhánh có tồn kho thay vì dùng ID cố định.

Stack: Java 21, Spring Boot 4.1.1, Thymeleaf, Bootstrap 5.3.3, JPA/Hibernate, SQL Server, WebSocket/STOMP, JWT và Cloudinary. SQL Server là lựa chọn trong yêu cầu SQLServer/MySQL/PostgreSQL; không cần dùng đồng thời cả ba.

## Báo cáo theo khoảng ngày

Dashboard admin, dashboard cửa hàng và báo cáo doanh thu theo đơn/sản phẩm đều có **Hôm nay**, **7 ngày**, **30 ngày** và **Từ ngày… đến ngày…**. Mặc định là 7 ngày, gồm hôm nay; chọn khoảng tùy chỉnh rồi bấm **Áp dụng**. Đổi tiêu chí, kiểu biểu đồ hoặc chuyển trang vẫn giữ khoảng ngày.

Khoảng ngày bao gồm toàn bộ ngày bắt đầu và ngày kết thúc theo múi giờ ứng dụng UTC+7. Doanh thu/số đơn/sản phẩm bán dùng ngày tạo đơn; người dùng mới dùng ngày đăng ký, cửa hàng/sản phẩm mới dùng ngày tạo. Doanh thu chỉ tính đơn đủ điều kiện ghi nhận, không tính đơn chưa thanh toán hoặc đã hoàn tiền. Số liệu tổng và biểu đồ dùng toàn bộ dữ liệu trong kỳ, không giới hạn 1.000 bản ghi; bảng danh sách/xếp hạng hiển thị tối đa 1.000 bản ghi và có phân trang. Xếp hạng điểm lọc người dùng/cửa hàng tạo trong kỳ nhưng cột điểm là **điểm tích lũy hiện tại**, không phải lịch sử điểm từng ngày.

Khoảng tùy chỉnh trên 90 ngày gom biểu đồ theo tháng, trên 730 ngày gom theo năm; vẫn chỉ tính dữ liệu trong chính xác khoảng đã chọn. Ngày đảo ngược, thiếu ngày hoặc sai định dạng bị từ chối. Các URL lọc giờ/tháng/năm cũ tiếp tục hoạt động.

## Chạy trên Windows

1. Cài JDK 21, SQL Server 2019+ và bật TCP/IP. Dự án có Maven Wrapper nên không cần cài Maven riêng. Bản JDK 21 portable đã được đặt trong `.tools` trên máy hiện tại; thư mục này không được đưa vào Git.
2. Tạo một database rỗng, ví dụ `RentalShop`, và tài khoản database có quyền tạo bảng/index trong schema `dbo` để chạy migration. Dùng tài khoản riêng cho môi trường production.
3. Xem `.env.example`, đặt biến môi trường trong PowerShell hoặc IDE. Spring Boot **không tự đọc** `.env`.

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlserver://127.0.0.1;instanceName=SQLEXPRESS;databaseName=RentalShop;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true;'
$env:SPRING_DATASOURCE_USERNAME = 'your_database_user'
$env:SPRING_DATASOURCE_PASSWORD = 'your_local_password'
$jwtBytes = [byte[]]::new(32)
[Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$env:CLOUDINARY_CLOUD_NAME = 'your_cloud_name'
$env:CLOUDINARY_API_KEY = 'your_api_key'
$env:CLOUDINARY_API_SECRET = 'your_api_secret'
# Chỉ bật trên database demo: tạo tài khoản/mật khẩu demo và dữ liệu mẫu.
$env:APP_DEMO_DATA_ENABLED = 'true'
.\scripts\maven.ps1 spring-boot:run
```

Mở `http://localhost:8080`. Các biến môi trường trên chỉ tồn tại trong terminal hiện tại. Lưu khóa JWT một lần ở cấu hình riêng nếu cần giữ token hợp lệ qua lần khởi động tiếp theo. Nếu thiếu khóa trong profile `dev`, hệ thống tạo khóa tạm; production yêu cầu khóa Base64 có ít nhất 32 byte.

### SQL Express trên máy hiện tại

Database đã chọn là `RentalShop` trong `SQLEXPRESS` (4 cửa hàng, 14 người dùng, 21 đơn khi kiểm tra). Instance này đang dùng cổng động `53735`, không phải `1433`. Profile dev kết nối bằng `instanceName=SQLEXPRESS`; SQL Server Browser xác định cổng hiện hành, tránh phụ thuộc cổng động sau restart. Instance `SQLEXPRESS01` chứa database khác, không được sửa.

```powershell
# Đọc trạng thái Flyway, không sửa database:
.\scripts\sqlserver.ps1 -Action info
# Chạy website đúng instance/database/profile:
.\scripts\sqlserver.ps1
```

Script dùng mật khẩu từ biến môi trường, hoặc credential mã hóa Windows DPAPI trong `.tools/sqlserver-local.json` trên máy hiện tại; nếu không có/không giải mã được sẽ hỏi mật khẩu bằng SecureString. File này bị Git ignore và chỉ tài khoản Windows đã mã hóa mới giải mã được. Không sao chép credential vào source. Sau khi script kết thúc, các biến môi trường ban đầu được khôi phục. Các action `baseline`/`migrate` luôn yêu cầu gõ lại chính xác tên database; phải có backup trước khi dùng trên database khác.

Nếu chạy từ IDE thay vì script, chọn profile `dev` và cung cấp `SPRING_DATASOURCE_USERNAME`/`SPRING_DATASOURCE_PASSWORD` trong cấu hình chạy; Spring không đọc được file credential DPAPI của script. `SPRING_DATASOURCE_URL` vẫn có thể ghi đè URL mặc định khi dùng máy/instance khác. Production dùng URL và chứng chỉ của môi trường triển khai, không dùng `trustServerCertificate=true` của local.

Tài khoản demo trên **database rỗng**: `admin`, `manager_q1`, `manager_td`, `khachhang`; mật khẩu `123456`. Không bật dữ liệu demo trên production. Nếu database đã có user/store/product, bộ khởi tạo bỏ qua; không tự bổ sung lại sản phẩm đã được người vận hành đổi tên.

## Database hiện có và Flyway

Flyway là nơi quản lý schema ở môi trường chạy. Các bản `V1`–`V4` tạo schema mới hoặc bổ sung bảng/cột/ràng buộc, chuyển role cũ, cài schema chat và ràng buộc đánh giá. File SQL trong `sql/migrations` và `resources/db` là tài liệu lịch sử; không cần chạy chúng lại. Các runner migration cũ chỉ được bật ở profile `test` để giữ kiểm thử H2.

Với **database rỗng**, chạy ứng dụng để Flyway áp dụng từ `V1`. Với **database đã có dữ liệu nhưng chưa có `flyway_schema_history`**, sao lưu và thử trên bản sao trước. Xác nhận URL database và schema `dbo`, sau đó chủ động ghi nhận baseline:

```powershell
.\scripts\maven.ps1 flyway:info
.\scripts\maven.ps1 flyway:baseline
.\scripts\maven.ps1 spring-boot:run
```

Plugin Flyway đọc ba biến `SPRING_DATASOURCE_*` đã đặt ở trên. Baseline ở version 1 không chạy lại schema tạo mới; lần khởi động sau chạy phần nâng cấp `V2`–`V4` trước khi Hibernate đọc entity. Không bật tự baseline để tránh chấp nhận nhầm database. Nếu migration báo dữ liệu trùng/ràng buộc không hợp lệ, cần rà soát dữ liệu được báo; migration không tự xóa lịch sử để vượt lỗi.

`database.sql` là script reset cũ có `DROP TABLE`; không dùng script này để nâng cấp database đang có dữ liệu. `spring.jpa.hibernate.ddl-auto=none`; Hibernate không thay đổi schema khi ứng dụng khởi động.

## Xác thực và phân quyền

Giao diện Thymeleaf và AJAX trong trình duyệt dùng session cùng CSRF. API JWT dùng `Authorization: Bearer <accessToken>` và không lấy quyền từ cookie session. Các request `/api/**` có Bearer header được xử lý qua JWT; request trình duyệt không có Bearer vẫn giữ luồng session hiện có. `/api/auth/me` và `/api/auth/logout` luôn yêu cầu JWT.

| Endpoint | Dữ liệu / hành vi |
|---|---|
| `POST /api/auth/login` | JSON `username`, `password`; username chấp nhận tên đăng nhập hoặc email |
| `POST /api/auth/refresh` | JSON `refreshToken`; xoay refresh token sau mỗi lần dùng |
| `GET /api/auth/me` | Tên đăng nhập và các quyền hiện hành |
| `POST /api/auth/logout` | Thu hồi token family của lần đăng nhập JWT hiện tại |

Access token là JWT HS256, thời hạn 15 phút; refresh token ngẫu nhiên có thời hạn tối đa 7 ngày tính từ lúc đăng nhập. Database chỉ lưu SHA-256 của refresh token. Dùng lại refresh token đã xoay sẽ thu hồi toàn bộ token family. Đăng xuất, khóa/vô hiệu hóa tài khoản và đổi mật khẩu ngăn sử dụng token đã phát hành. JWT được kiểm tra chữ ký, issuer, audience, thời hạn và token type. Quyền được nạp lại từ database mỗi request.

API Bearer hiện dùng được với chat HTTP, thông báo và API tồn kho. Kết nối WebSocket `/ws`, `/ws-chat` của giao diện tiếp tục xác thực bằng session; không truyền JWT trong URL. STOMP kiểm tra quyền gửi/subscribe theo người dùng và chi nhánh. Quyền tổng quát là `ADMIN`, `MANAGER`, `USER`; nhân sự từng cửa hàng được giới hạn thêm bởi ownership/membership.

## Profile và dịch vụ ngoài

`dev`: không cache template, cho phép chứng chỉ SQL Server tự ký tại máy local, có thể bật seed demo rõ ràng. `prod`: cache template, cookie session Secure, tắt demo/DevTools và yêu cầu cấu hình Cloudinary. Triển khai production qua HTTPS; URL SQL Server cần chứng chỉ hợp lệ. Đặt giới hạn request cho endpoint đăng nhập tại reverse proxy khi public website.

OTP mặc định gửi qua Brevo API (`MAIL_PROVIDER=brevo`), không dùng Gmail App Password. Cấu hình người gửi bằng `MAIL_FROM_EMAIL` và `BREVO_API_KEY`; người nhận lấy từ yêu cầu đăng ký/email của tài khoản, không cố định trong code. Chỉ khi chủ động chọn `MAIL_PROVIDER=smtp` mới dùng `SPRING_MAIL_USERNAME`/`SPRING_MAIL_PASSWORD`. MoMo mặc định chạy mô phỏng, không thu tiền thật. Cloudinary sử dụng ba biến `CLOUDINARY_*`. Profile dev có thể lưu ảnh hồ sơ/sản phẩm cục bộ khi chưa cấu hình Cloudinary; gửi ảnh chat yêu cầu Cloudinary. Không còn credential thật trong giá trị mặc định. Các khóa Cloudinary/database từng nằm trong Git cần được thay tại nhà cung cấp/database; việc sửa file hiện tại không xóa lịch sử Git.

OTP/API key không được in ra terminal/log. Ngoài kiểm thử, thiếu cấu hình mail hoặc gửi email thất bại sẽ báo lỗi, không báo thành công giả. Đăng ký gửi mail thất bại được giải phóng để thử lại; gửi lại OTP thất bại giữ mã cũ nếu còn hạn. Profile `test` bật `app.mail.mock-enabled=true` để test không gửi email ra ngoài; production ép tắt mô phỏng mail.

### Cấu hình OTP một lần, không điền App Password vào code

1. Tạo tài khoản Brevo, thiết lập/xác minh địa chỉ hoặc domain gửi theo [hướng dẫn gửi transactional email](https://developers.brevo.com/docs/send-a-transactional-email).
2. Tạo **API key** (không phải SMTP key) theo [hướng dẫn chính thức](https://developers.brevo.com/docs/api-key-authentication).
3. Trên Windows chạy `./scripts/configure-mail.ps1`: nhập email người gửi đã xác minh và API key tại dòng nhập được che. Script lưu key mã hóa Windows DPAPI vào `.tools/mail-local.json`, bị Git ignore; không sửa mã nguồn hay lưu key rõ.
4. Chạy `./scripts/sqlserver.ps1`. Script tự nạp cấu hình mail local khi chưa có biến môi trường ghi đè. Sau đó người dùng đăng ký bằng email của họ, website tự gửi OTP tới email đó; không cần cấu hình lại cho mỗi người nhận.

Nếu chạy từ IDE/server khác, đặt `BREVO_API_KEY`, `MAIL_FROM_EMAIL` và tùy chọn `MAIL_FROM_NAME` trong cấu hình chạy/secret manager. Sender cần được nhà cung cấp chấp nhận, nhưng không bắt khách cung cấp mật khẩu hoặc xác minh email của họ tại Brevo trước khi nhận OTP. Khôi phục/đổi mật khẩu chỉ gửi tới email gắn với tài khoản, không được đổi sang email của người khác trong request.

Hiện chưa có API key/người gửi được cấu hình tại máy kiểm tra, nên đã kiểm chứng payload/luồng/lỗi bằng mock, **chưa xác nhận thư đã tới hộp thư thật**. API trả 201 + messageId nghĩa là đã tiếp nhận gửi; giao thư còn phụ thuộc mailbox tồn tại, spam, giới hạn/tình trạng tài khoản dịch vụ. Không thể gửi mail thật nếu hoàn toàn không cấu hình một dịch vụ gửi có quyền xác thực.

`open-in-view=true` được giữ có chủ đích vì nhiều template hiện truy cập quan hệ JPA lazy khi render. Tắt ngay sẽ gây lỗi màn hình; muốn tắt cần chuyển các màn hình này sang DTO/projection. JWT authority/token service có transaction riêng cho truy cập dữ liệu xác thực.

## Kiểm thử

```powershell
.\scripts\maven.ps1 test
.\scripts\test-js.ps1
.\scripts\maven.ps1 -DskipTests package
```

Java test dùng H2 ở chế độ SQL Server, không ghi database SQL Server đang có. Test WebSocket thực tế cần được phép mở kết nối localhost. Mockito được nạp bằng Java agent để tránh tự attach bị treo. JavaScript test cần Node.js 22; trên máy hiện tại đã có runtime portable trong `.tools`.

Script Maven tương thích Windows PowerShell 5.1 và PowerShell 7: kiểm tra `java -version` bằng process stream riêng để không coi stderr chứa thông tin phiên bản là `NativeCommandError`. Nếu Java mặc định là 26, script bỏ qua và chọn JDK 21 portable của dự án; không cần gỡ Java 26. Xem toolchain bằng `./scripts/maven.ps1 -MavenArguments @('--version')` (không dùng `-v`, có thể bị PowerShell hiểu là Verbose).

Test Cloudinary live chỉ chạy khi chủ động đặt `-Dcloudinary.live=true` và cung cấp credential thật; test tạo và xóa riêng ảnh kiểm thử. SQL Server đã được kiểm tra offline và live trên database rỗng/bản sao; RentalShop trên SQLEXPRESS đã nâng cấp Flyway V4 và chạy website thật thành công sau khi người dùng chấp thuận. Môi trường triển khai khác vẫn phải backup/thử migration trên bản sao riêng.

Đối chiếu yêu cầu, các thay đổi và kết quả kiểm thử: [báo cáo kỹ thuật](docs/REQUIREMENTS_AUDIT_20261012.md).

Đánh giá chạy thử mới nhất, bộ lọc báo cáo, sửa OTP và checklist dịch vụ còn cần cấu hình: [báo cáo hệ thống](docs/SYSTEM_REVIEW_20261012.md).

Nguyên nhân cổng 1433, backup, migration thật và lệnh chạy SQL Express: [báo cáo sửa SQL Server](docs/SQLSERVER_REPAIR_20261012.md).

OTP gửi đúng người nhận, không điền Gmail App Password vào code: [hướng dẫn cấu hình mail API](docs/OTP_EMAIL_SETUP_20261012.md).
