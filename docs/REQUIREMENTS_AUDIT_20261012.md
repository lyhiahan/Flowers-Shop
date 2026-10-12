# Đối chiếu yêu cầu và thay đổi ngày 12/10/2026

Yêu cầu kiểm tra: website bán hàng theo mô hình chuỗi cửa hàng, dùng Spring Boot + Thymeleaf + Bootstrap + JPA + SQLServer/MySQL/PostgreSQL + WebSocket + JWT + Cloudinary. Từ “Cloudairy” trong đề được hiểu là Cloudinary.

## Mức đáp ứng công nghệ

| Yêu cầu | Trước sửa | Sau sửa / bằng chứng |
|---|---|---|
| Chuỗi cửa hàng | Đã có | `Store`, `StoreInventory`, `StoreMembership`, `StoreAccessService`; chọn chi nhánh, phân quyền theo cửa hàng, đơn hàng và doanh thu chi nhánh |
| Spring Boot | Đã có | `pom.xml`, `FinalprojectApplication`, Spring Boot 4.1.1 |
| Thymeleaf | Đã có | Starter và 71 template trong `resources/templates` |
| Bootstrap | Đã có | Bootstrap 5.3.3 CSS/JS được dùng tại template và fragment chung |
| JPA | Đã có | Entity, repository Spring Data JPA, transaction và optimistic/pessimistic locking |
| SQL Server/MySQL/PostgreSQL | Chọn SQL Server | Driver Microsoft SQL Server; Flyway quản lý schema SQL Server; H2 chỉ dùng kiểm thử |
| WebSocket | Đã có | `/ws`, `/ws-chat`, STOMP `/app/chat.send`, thông báo và kiểm tra quyền subscribe |
| JWT | Chưa có triển khai | Thêm cấp/kiểm tra JWT, refresh rotation, logout/revocation và filter chain API |
| Cloudinary | Đã tích hợp | Upload ảnh sản phẩm, hồ sơ, cửa hàng, chat; lưu `secure_url` và `public_id`; credential từ biến môi trường |

Ở mức mã nguồn, các công nghệ nêu trong đề đã có. SQL Server đã được xác minh thực tế sau lượt sửa kết nối: migration V4, schema validation và website đọc RentalShop thành công. Cloudinary/mail API/secret production vẫn cần cấu hình/xác minh riêng. Xem [báo cáo sửa SQL Server](SQLSERVER_REPAIR_20261012.md).

## Các thay đổi chính

### JWT có luồng sử dụng thực tế

Thêm `JwtAuthController`, `JwtTokenService`, `JwtConfig`, `JwtProperties`, `JwtSecurityConfig`, bảng/entity `JwtSession`, `JwtRefreshToken` và repository tương ứng.

- `/api/auth/login`: tên đăng nhập/email + mật khẩu; dùng BCrypt và AuthenticationManager hiện có.
- `/api/auth/me`: tên đăng nhập và quyền được nạp từ database.
- `/api/auth/refresh`: token ngẫu nhiên lưu hash; xoay token, giữ thời hạn tuyệt đối 7 ngày.
- `/api/auth/logout`: thu hồi family của phiên JWT hiện tại.
- Access token HS256, TTL 15 phút, kiểm tra issuer/audience/exp/token type và phiên chưa bị thu hồi.
- Token không vượt qua khóa/vô hiệu hóa tài khoản, thay đổi mật khẩu hoặc thay đổi quyền.
- Replay refresh token thu hồi family và commit việc thu hồi ngay cả khi trả lỗi 401.
- API JWT không dùng session cookie và trả JSON cho lỗi xác thực/validation.

Session của giao diện Thymeleaf được giữ để tương thích form, CSRF và kết nối WebSocket hiện có. JWT hoạt động trên HTTP API có Bearer header; không đưa token vào URL. Cách xử lý Bearer dựa trên [Spring Security Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

### Cấu hình và credential

Tách `application-dev.properties` và `application-prod.properties`; cấu hình chung tắt cache-debug/DevTools ở mặc định, không trả stack trace, không seed demo mặc định. Dev chỉ seed khi bật rõ ràng; `DataInitializer` chỉ được nạp trong profile dev. Production yêu cầu Cloudinary, khóa JWT đủ dài, cookie Secure và kết nối SQL Server có chứng chỉ tin cậy.

Gỡ password database và credential Cloudinary khỏi giá trị mặc định; cung cấp `.env.example` không chứa secret thật. `.env`, `.tools`, `application-local.properties` được ignore. Người quản trị cần thay khóa Cloudinary/database cũ ở dịch vụ thật vì lịch sử Git vẫn chứa giá trị đã commit; thay cấu hình hiện tại không thể thu hồi khóa ở nhà cung cấp.

### Toolchain và kiểm thử

Đặt Temurin JDK 21 portable và Node.js 22 portable trong `.tools` tại máy hiện tại; cả hai được tải từ nguồn chính thức và kiểm tra SHA-256. Không thay Java mặc định của Windows. Script `scripts/maven.ps1` chọn JDK 21, thực thi Maven và khôi phục biến môi trường; `scripts/test-js.ps1` chạy các test frontend.

Thêm Mockito Java agent vào Surefire, tránh cơ chế tự attach bị treo. Cập nhật regression test cũ đang yêu cầu JWT bị ngừng sử dụng để kiểm tra luồng JWT đã được khôi phục theo yêu cầu đề bài.

### Tổ chức mã nguồn

Tách `AdminConfigController` thành 7 controller: category, commission, delivery, voucher, user level, store level và style. Giữ nguyên URL/template. Phần danh sách/lọc sản phẩm được chuyển sang `AdminProductCatalogController`; CRUD giữ ở `AdminProductController`.

Tách seeder thành các service cho branch/account, product fixture, style, staff, voucher, order và wallet. Bỏ bước tự sửa ảnh/tên sản phẩm và tự xóa/hợp nhất category trong bộ tạo dữ liệu demo. Test khởi tạo kiểm tra chuỗi có ít nhất ba cửa hàng và chạy lại không ghi đè tên/trạng thái sản phẩm người dùng đã sửa.

### Database có lịch sử phiên bản

Thêm Flyway và SQL Server module; schema `dbo`, `clean-disabled=true`, `baseline-on-migrate=false`.

- `V1`: schema SQL Server được sinh từ toàn bộ entity hiện hành, không chứa reset hoặc dữ liệu demo.
- `V2`: nâng cấp bổ sung cho database đã được baseline rõ ràng; không xóa lịch sử để vượt lỗi.
- `V3`: chuyển role cũ, ngừng role giao hàng và tách quyền admin.
- `V4`: schema chat, deadline thanh toán và review integrity.

Production áp dụng migration trước Hibernate; các runner cũ được giới hạn ở profile test. Các SQL lịch sử giữ để tra cứu. Script reset `database.sql` được đánh dấu rõ; không chạy nó trong quá trình nâng cấp.

Database đang có dữ liệu cần backup, thử migration trên bản sao và gọi `flyway:baseline` chủ động ở version 1. Xem [hướng dẫn baseline của Flyway](https://documentation.red-gate.com/fd/flyway-baseline-on-migrate-setting-277578974.html) và README. Không tự baseline database hiện có trong lượt sửa này.

### Frontend

Thêm test checkout quote cho chặn submit trước khi có báo giá, lỗi/hết phiên và phản hồi cũ đến muộn. Thêm test badge/CSRF/thất bại của thông báo. Sửa việc UI chuyển “đã đọc” dù server trả HTTP lỗi; bảo toàn trạng thái chưa đọc và giải phóng nút thao tác.

CSRF được yêu cầu trở lại cho các thao tác thông báo dùng session; widget đã gửi CSRF header. Bearer API dùng chain stateless riêng.

## Phạm vi đã kiểm chứng và giới hạn

- Full regression + package sau đổi OTP sang mail API đạt **435 test, 0 failures, 0 errors, 3 skipped** trên JDK 21 (432 pass; Cloudinary live và 2 SQL Server opt-in bỏ qua mặc định). Riêng SQL Server live đã chạy **2/2 pass** ở lượt sửa SQL; build/repackage JAR thành công. Mail API dùng API key ngoài code, không cần Gmail App Password; chưa gửi live khi chưa có key/sender. Xem [hướng dẫn OTP](OTP_EMAIL_SETUP_20261012.md).
- 13/13 test JavaScript đã pass qua script chạy Node.
- Bổ sung bộ lọc ngày cho mọi trang thống kê, kiểm tra quyền analytics và xử lý lỗi/không log OTP. Chạy HTTP thật kiểm tra 25 lượt mở trang chính. Chi tiết trong [báo cáo hệ thống](SYSTEM_REVIEW_20261012.md).
- Các test JWT kiểm tra token không session, sai chữ ký, hết hạn, sai issuer/audience, refresh rotation/replay, logout, khóa tài khoản, đổi mật khẩu, thay đổi quyền và truy cập chat API.
- Kiểm tra CSRF xác nhận POST thông báo dùng session cần CSRF token; Bearer API thực hiện cùng thao tác qua chain JWT không dùng cookie.
- Test WebSocket dùng HTTP server thật và trao đổi tin nhắn qua kết nối localhost đã pass khi chạy ngoài sandbox.
- Ngoài kiểm tra schema offline, migration đã chạy trên SQL Server thật với database rỗng/bản sao, validate toàn bộ entity, migrate lại không chạy thêm và giữ nguyên số dòng chính.
- Đã sửa kết nối local sang instance SQLEXPRESS (cổng động 53735). Sau backup và xác nhận của người dùng, RentalShop gốc đã baseline V1/migrate V2–V4/validate thành công. Website khởi động và các trang public trả HTTP 200 bằng dữ liệu SQL Server thật.
- Một test Cloudinary live được bỏ qua mặc định vì cần credential thật; test upload/delete với SDK mock và kiểm tra file vẫn chạy.

`open-in-view=true` vẫn được giữ vì template hiện truy cập quan hệ lazy khi render. Đề bài không yêu cầu tắt OSIV; chuyển sang DTO/projection cần một đợt sửa riêng để không gây lỗi màn hình. Cấu hình này đã được ghi rõ thay vì tắt mà chưa kiểm chứng.

Chưa triển khai website lên server và chưa commit các thay đổi. Hướng dẫn cấu hình, baseline database, chạy ứng dụng/test nằm trong README ở thư mục gốc.
