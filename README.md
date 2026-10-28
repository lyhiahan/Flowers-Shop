# Hoa Tươi Sài Gòn — website chuỗi cửa hàng hoa

Ứng dụng Spring Boot MVC phục vụ khách hàng, quản trị viên, quản lý chi nhánh và nhân viên. Giao diện Thymeleaf và backend chạy trong cùng một ứng dụng; JavaScript xử lý giỏ hàng, bộ lọc, chat và thông báo.

## Chạy nhanh từ bản clone sạch — Windows

**Mặc định dùng dữ liệu Aiven hiện có.** Đổi kết nối không sao chép dữ liệu từ máy local lên cloud. Mỗi thành viên tạo cấu hình riêng; không chia sẻ `.env.aiven`, mật khẩu hoặc JWT key qua Git.

1. Cài **JDK 21** và Git; clone repo, mở PowerShell tại thư mục có `pom.xml`. Maven Wrapper có sẵn, không cần cài Maven riêng. Lần build đầu cần Internet để tải dependency. Node.js 22+ chỉ cần cho JS test và tunnel thanh toán điện thoại.
2. Đặt `JAVA_HOME` đến JDK 21 nếu máy có nhiều Java; script cũng tìm JDK trên PATH và một số thư mục cài đặt thông dụng. Ví dụ bên dưới cần thay bằng đường dẫn thật:

   ```powershell
   $env:JAVA_HOME = 'C:\duong-dan\jdk-21'
   .\start-web.ps1 -Init
   ```

   `-Init` tạo `.env.aiven` từ mẫu và tự sinh JWT key ngẫu nhiên, không ghi đè file đã tồn tại. Nếu PowerShell chặn script, chạy bằng `powershell -NoProfile -ExecutionPolicy Bypass -File .\start-web.ps1 -Init`.
3. Lấy thông tin dịch vụ từ người quản lý Aiven, điền `AIVEN_HOST`, `AIVEN_PORT`, `AIVEN_DATABASE`, `AIVEN_USER`, `AIVEN_PASSWORD` trong `.env.aiven`. Tải **CA certificate** của đúng dịch vụ, lưu `ca.pem` ở gốc repo hoặc đặt `AIVEN_CA_CERT` thành đường dẫn thật, ưu tiên dấu `/`. Không dán Service URI vào ô password; không thêm dấu nháy quanh giá trị. Cấu hình dùng cú pháp Java properties, mỗi dòng `KEY=value`.
4. Kiểm tra rồi chạy:

   ```powershell
   .\start-web.ps1 -Check
   .\start-web.ps1
   ```

   `-Check` kiểm tra cấu hình/JDK/CA, **không xác nhận kết nối mạng hoặc schema**. Chỉ coi web sẵn sàng khi log có `Started FinalprojectApplication` và truy cập được [http://127.0.0.1:8080](http://127.0.0.1:8080/). Dừng ứng dụng bằng Ctrl+C. Script không tự chiếm cổng đang có ứng dụng.

### Các tùy chọn khởi động

| Lệnh | Mục đích |
|---|---|
| `.\start-web.ps1` | Aiven, localhost:8080, seed/import/reset/migration/tunnel đều không tự chạy |
| `.\start-web.ps1 -Port 8081` | Chạy ở cổng khác |
| `.\start-web.ps1 -JavaHome 'C:/path/to/jdk-21'` | Chỉ định JDK 21 |
| `.\start-web.ps1 -CaCertificate 'C:/certs/ca.pem'` | Chỉ định CA Aiven |
| `.\start-web.ps1 -Database Local` | PostgreSQL riêng, đọc `.env.local-postgres` |
| `.\start-web.ps1 -Tunnel -Cloudflared 'C:/tools/cloudflared.exe'` | Bật tunnel thanh toán điện thoại; cần Node.js và cloudflared |
| `.\start-web.ps1 -Migrate` | Cho phép Flyway áp dụng migration; chỉ dùng sau khi xác minh DB đích và backup |

Launcher dùng Hibernate `validate`, tắt tự baseline/seed, không import SQL. Bình thường không cần `-Migrate` khi kết nối Aiven đã có schema. Nếu chủ DB chủ động khởi tạo database trống hoặc nâng cấp schema, xác minh đích rồi mới dùng `-Migrate`; không repair checksum/xóa history để vượt lỗi. Chạy Maven trực tiếp có thể dùng cấu hình migration/tunnel khác launcher, xem phần profile bên dưới.

### PostgreSQL riêng cho người chưa có quyền Aiven

Cài và khởi động PostgreSQL. Tạo cấu hình bằng `.\start-web.ps1 -Database Local -Init`, rồi điền `LOCAL_DB_HOST`, `LOCAL_DB_PORT`, `LOCAL_DB_NAME`, `LOCAL_DB_USER`, `LOCAL_DB_PASSWORD`. Mẫu dùng cổng 5432 và database `flower_shop`; không cần thư mục `.tools` hay bản database riêng của người phát triển.

Ví dụ tạo **database thử nghiệm mới** và nạp dữ liệu demo; các lệnh hỏi password của PostgreSQL khi cần:

```powershell
createdb -h 127.0.0.1 -p 5432 -U postgres flower_shop
psql -h 127.0.0.1 -p 5432 -U postgres -d flower_shop -v ON_ERROR_STOP=1 -f database/FlowersShop_PostgreSQL_Full.sql
.\start-web.ps1 -Database Local -Check
.\start-web.ps1 -Database Local
```

SQL demo có lệnh xóa/tạo lại schema; **không chạy vào database đang dùng hoặc Aiven của nhóm**. Script không tự khởi động PostgreSQL, tải dữ liệu cloud hoặc reset khi chạy lại. Máy đã có bản local cổng 15432 vẫn có thể dùng cấu hình cũ; đây không phải cổng bắt buộc cho người mới. Hướng dẫn SQL chi tiết: [database/HUONG_DAN_IMPORT_DATABASE.md](database/HUONG_DAN_IMPORT_DATABASE.md).

## Công nghệ

- Java 21, Spring Boot 4.1.1, Maven Wrapper.
- Spring MVC, Thymeleaf, Bootstrap 5, JavaScript thuần.
- Spring Data JPA/Hibernate, Flyway; SQL Server hoặc PostgreSQL/Aiven.
- Spring Security: session cho trình duyệt, JWT cho API Bearer.
- STOMP/WebSocket cho chat và thông báo; Cloudinary cho ảnh; Brevo hoặc SMTP cho OTP.
- H2 cho Java test; Node.js 22 cho JavaScript test.

## Cấu trúc

```text
src/main/java/com/finalproject/
  config/       Cấu hình và khởi tạo
  controller/   admin, auth, common, customer, manager, staff
  service/      Các service cụ thể; nghiệp vụ và tích hợp
  repository/   Spring Data JPA
  entity/       Ánh xạ dữ liệu
  dto/          Request/response
  enums/        Vai trò, trạng thái
  security/     Kiểm tra tài khoản và thông tin phiên
  db/migration/ Java migration dành cho SQL Server
  tools/        Thiết lập Aiven, chuyển database
  utils/        Tiện ích dùng chung
src/main/resources/
  templates/    Trang Thymeleaf theo vai trò và fragments
  static/       css, js, images, upload
  db/migration/ sqlserver và postgresql
src/test/       Java, JavaScript và cấu hình H2
src/postgresql-test/ Kiểm thử PostgreSQL riêng
scripts/        Chạy ứng dụng, kiểm thử, database và tunnel
reports/        Bằng chứng kiểm tra
```

Quy ước service và cách giữ tương thích dữ liệu cũ: [kiến trúc hiện tại](docs/KIEN_TRUC_HIEN_TAI.md).

## Vai trò và nghiệp vụ

| Vai trò | Quyền hạn và phạm vi hoạt động |
|---|---|
| `USER` | Mua hoa, giỏ hàng, đơn hàng, ví demo, đánh giá, hồ sơ, chat với cửa hàng/admin |
| `ADMIN` | Quản trị toàn hệ thống, danh mục hoa, tài khoản, cấp phép chi nhánh, quản lý đơn hàng toàn chuỗi và hỗ trợ |
| `MANAGER` | Quản lý chi nhánh được phân công: kho hoa, nhân sự, đơn hàng, thống kê doanh thu, ví cửa hàng; không có quyền mua hàng/giỏ hàng/ví khách |
| `STAFF` | Tiếp nhận, cắm hoa/chuẩn bị đơn, giao đơn và chat với khách hàng tại chi nhánh; không quản trị tài chính |

### Danh sách tài khoản thử nghiệm

Tài khoản phụ thuộc database đang kết nối, không có mật khẩu chung được bảo đảm cho mọi môi trường. Với **SQL demo đi kèm repo**, hướng dẫn import liệt kê `local_customer`, `local_staff`, `local_manager`, `local_admin`; không suy ra chúng tồn tại trên Aiven. Khi dùng Aiven hiện có, lấy tài khoản được cấp từ người quản lý; launcher không tự seed tài khoản hay đổi mật khẩu. Khách vãng lai mở trang chủ không cần tài khoản.

Khách chưa đăng nhập là anonymous, không có tài khoản/role GUEST trong database. `StoreAccessService` kiểm tra quyền chi nhánh bên cạnh quyền URL. Admin không dùng chức năng mua hàng. Các đường dẫn `/vendor/**` là alias tương thích của khu vực manager; cổng shipper cũ bị chặn.

### Tính năng lọc theo chi nhánh & bộ lọc đa thuộc tính

- **Lọc theo chi nhánh:** Trang chủ và danh mục hỗ trợ lọc sản phẩm theo từng chi nhánh đang mở cửa (`catalogStores`) hoặc xem toàn chuỗi. Bộ lọc xuất hiện ở thanh điều hướng nhanh và bảng mở rộng *"Tất cả bộ lọc"*.
- **Đồng bộ trạng thái:** Thay đổi chi nhánh, danh mục hoặc sắp xếp giá giữ nguyên từ khóa tìm kiếm và các bộ lọc thuộc tính đã chọn (Kiểu dáng, Loại hoa, Màu sắc, Dịp tặng, Khoảng giá), đồng thời đưa phân trang về trang 1.
- **Tồn kho thực tế:** Hiển thị số lượng còn lại theo từng chi nhánh; khi hết hàng nút mua tự động khóa và cập nhật nhãn trạng thái thời gian thực.

Đơn mua hoa hỗ trợ nhận tại cửa hàng hoặc giao hàng. Giá, phí và hoa hồng được lưu theo đơn. Phí giao hàng dựa trên vùng gần/xa/ngoài phạm vi do người mua chọn và biểu phí chi nhánh, không đo khoảng cách bản đồ. Khách tự hủy khi đơn còn PENDING/CONFIRMED; cửa hàng xử lý theo `OrderWorkflowService`.

Các luồng thanh toán có COD, chuyển khoản, MoMo và mô phỏng; lựa chọn hiển thị phụ thuộc cấu hình/controller. Ví khách và MoMo demo chỉ dành cho mô phỏng, không thu tiền MoMo thật. `OrderFundsService` quản lý tiền chờ và tiền khả dụng của cửa hàng.

## Chọn môi trường trước khi chạy

| Cách chạy / profile | Database và cấu hình |
|---|---|
| `start-web.ps1` | Mặc định Aiven; đọc `.env.aiven`; migration/tunnel tắt nếu không opt-in |
| `start-web.ps1 -Database Local` | PostgreSQL riêng; đọc `.env.local-postgres`; không tự khởi động/reset DB |
| Maven `spring-boot:run` | Mặc định `aiven` theo `pom.xml` |
| Spring chạy trực tiếp, không chỉ định profile | Mặc định `dev` theo `application.properties` |
| `dev` | SQL Server; đọc `.env` |
| `aiven` | PostgreSQL; chỉ đọc `.env.aiven`; không ghép với `dev`/`test` |
| `prod` | Cache template, cookie Secure, yêu cầu Cloudinary, tắt seed demo; database phải được cấu hình rõ |
| `test` | H2 riêng, create-drop, Flyway tắt, email mock |

### PostgreSQL/Aiven qua Maven trực tiếp

```powershell
.\scripts\maven.ps1 spring-boot:run
```

Đường chạy trực tiếp này dùng mặc định profile Aiven trong POM, **Flyway và auto-tunnel có thể bật theo profile**, khác launcher ở trên. Đợi `Started FinalprojectApplication`, sau đó mở `http://localhost:8080`. TLS bắt buộc `verify-full` và CA hợp lệ. Khởi động không sao chép dữ liệu local; ứng dụng vẫn có thể cập nhật dữ liệu dùng chung qua nghiệp vụ/tác vụ định kỳ.

Thiết lập lần đầu có kiểm tra kết nối rồi lưu cấu hình riêng:

```powershell
.\scripts\run-aiven.ps1 -Mode Check -SaveLocalConfiguration -JavaHome $env:JAVA_HOME -CaCertificate .\ca.pem
```

Kiểm tra database chỉ đọc, không khởi động ứng dụng:

```powershell
.\scripts\verify-aiven.ps1 -JavaHome $env:JAVA_HOME
# Validate schema and render public pages with writes disabled:
.\scripts\verify-aiven.ps1 -JavaHome $env:JAVA_HOME -Application
```

### SQL Server local

Sao chép `.env.example` thành `.env`, điền thông tin riêng, rồi chỉ định database và cổng SQL thực tế:

```powershell
.\scripts\run-local.ps1 -DatabaseName FlowersShop -Server '127.0.0.1:1433'
```

Script chọn profile `dev` rõ ràng. Cổng 1433 là giá trị mặc định, không phải xác nhận cổng đang dùng trên máy; SQL Express có thể dùng cổng động. Script `sqlserver.ps1` vẫn hỗ trợ cấu hình DPAPI cũ cho instance/database đã xác minh trên máy gốc.

`FlowersShop` là tên database SQL Server đã tồn tại, không phải tính năng thuê. Không đổi tên database, email demo hoặc public_id Cloudinary chỉ để đổi cách đặt tên trong source.

## Schema và migration

- SQL Server: `db/migration/sqlserver` cùng `com/finalproject/db/migration`, từ V1 đến **V18**; V3/V4/V10/V16 viết bằng Java.
- PostgreSQL: `db/migration/postgresql/V1__initial_schema.sql` là baseline độc lập tương ứng mô hình ứng dụng SQL Server V17. PostgreSQL V2 gỡ các cột không còn dùng và thu hẹp model về bán hoa. Hai engine có chuỗi phiên bản riêng.
- Phiên bản trong source không chứng minh database đang dùng đã migrate; đọc `flyway_schema_history` để xác nhận.
- Profile Aiven dùng Hibernate `validate`; SQL Server mặc định `ddl-auto=none`. Flyway quản lý nâng cấp schema.
- Khi chạy thông thường Flyway có thể áp dụng migration đang chờ. Database đã có dữ liệu cần sao lưu và thử trên bản sao trước khi nâng cấp/baseline.
- `run-local.ps1` tắt tự baseline. `dev` chạy trực tiếp có cấu hình `baseline-on-migrate=true`; cần lưu ý khác biệt này khi dùng IDE.
- Plugin Maven `flyway:*` hiện cấu hình riêng cho SQL Server/dbo; không dùng các goal đó cho Aiven.
- `database.sql` là script reset cũ có `DROP TABLE`; `sql/migrations` và các SQL cũ ngoài thư mục Flyway là tài liệu lịch sử, không chạy lại lên dữ liệu hiện có.

Ứng dụng chỉ tạo đơn mua hoa (`BUY`). Form sản phẩm/giỏ hàng và thống kê doanh thu chỉ phục vụ bán hàng. Các cột, enum và phép tính dữ liệu cũ được giữ để đọc lịch sử; không mở lại nghiệp vụ cũ. Xem [ranh giới tương thích](docs/KIEN_TRUC_HIEN_TAI.md#du-lieu-lich-su).

## Xác thực, OTP và ảnh

Trình duyệt dùng HttpSession và CSRF (một số endpoint đăng ký/OTP/callback có ngoại lệ trong cấu hình). API `/api/**` có Bearer dùng security chain JWT stateless; `/api/auth/**` thuộc chain JWT. WebSocket tiếp tục dùng phiên đăng nhập, kiểm tra quyền gửi/subscribe và chi nhánh.

| API | Nội dung |
|---|---|
| `POST /api/auth/login` | JSON `username`, `password` |
| `POST /api/auth/refresh` | JSON `refreshToken`; xoay token |
| `GET /api/auth/me` | Thông tin/quyền hiện hành |
| `POST /api/auth/logout` | Thu hồi token family hiện tại |

JWT HS256 mặc định sống 15 phút, refresh tối đa 7 ngày; database lưu hash refresh token. Khóa `JWT_SECRET` là Base64 của ít nhất 32 byte ngẫu nhiên. Aiven cần cấu hình khóa; dev có thể sinh khóa tạm. `spring-session-jdbc` trong dependency không đồng nghĩa phiên trình duyệt đã được lưu JDBC.

OTP mặc định dùng `MAIL_PROVIDER=brevo`, `BREVO_API_KEY`, `MAIL_FROM_EMAIL`, tùy chọn `MAIL_FROM_NAME`. Có thể chạy `scripts/configure-mail.ps1` cho cấu hình DPAPI mà `sqlserver.ps1` hỗ trợ. Chạy Aiven/IDE cần cung cấp các biến mail qua môi trường hoặc cấu hình profile tương ứng. SMTP chỉ dùng khi chọn `MAIL_PROVIDER=smtp`. Kiểm thử mock không chứng minh email đã đến hộp thư thật.

Ảnh dùng `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`. Dev có fallback cục bộ khi chưa cấu hình; ảnh chat cần Cloudinary. `.env`, `.env.aiven`, `.tools` và `upload` bị Git ignore; không đưa credential vào source, báo cáo hay bản nộp công khai.

## Chat và thanh toán trên điện thoại

Chat hỗ trợ khách–cửa hàng, khách–admin và manager–admin. Admin giám sát hội thoại khách–cửa hàng ở chế độ chỉ đọc, có nhật ký truy cập. Thẻ sản phẩm trong chat chụp tên/ảnh/giá tại lúc gửi.

Dev/Aiven có thể tự bật Cloudflare tunnel bằng `.tools/cloudflared.exe` và Node.js khi cho phép `DEMO_PAYMENT_AUTO_TUNNEL_ENABLED=true`. Chỉ công bố QR sau khi gateway nhận diện đúng kết nối và báo READY; URL tunnel thay đổi sẽ cập nhật QR. Không dùng lại quick-tunnel URL của phiên cũ. Supervisor chờ DNS trong hạn khởi động, tăng thời gian giữa các lần thử lại và chờ ít nhất 60 giây khi nhà cung cấp trả HTTP 429. Có thể tắt bằng `DEMO_PAYMENT_AUTO_TUNNEL_ENABLED=false`.

Gateway chỉ chuyển tiếp các route thanh toán điện thoại và tài nguyên cho phép; không công khai admin, cart hay API đăng nhập. Kiểm tra kết nối thật với fixture tạm, không đụng database:

```powershell
node scripts/verify-demo-phone.cjs
```

Kết quả live và giới hạn kiểm chứng: [báo cáo cập nhật 28/10/2026](docs/HOAN_THIEN_CAU_TRUC_20261028.md). Kết quả cũ không xác nhận trạng thái tunnel hiện tại.

## Kiểm thử

```powershell
.\scripts\maven.ps1 clean test
.\scripts\test-js.ps1
# Kiểm thử launcher trong thư mục tạm, mock Maven, không kết nối database:
.\scripts\test-start-web.ps1 -JavaHome $env:JAVA_HOME
.\scripts\maven.ps1 -DskipTests package
# Chạy bộ PostgreSQL riêng với database embedded:
.\scripts\maven.ps1 -Ppostgresql-tests '-Dtest=PostgreSql*Tests' test
```

Java test mặc định dùng H2, không ghi SQL Server/Aiven. Một số test live yêu cầu opt-in; WebSocket/gateway cần quyền kết nối localhost. Profile Maven `isolated-tests` dùng thư mục build riêng nếu cần. Sau khi đổi/xóa package Java, chạy `clean` để loại class cũ. GitHub Actions chạy Java test và JavaScript test; bộ PostgreSQL riêng chưa nằm trong job mặc định.

`open-in-view=true` được giữ vì các template còn đọc quan hệ JPA lazy. Muốn tắt cần chuyển màn hình sang DTO/projection và kiểm tra render trước.

## Tài liệu

- [Kiến trúc và quy ước hiện tại](docs/KIEN_TRUC_HIEN_TAI.md)
- [Báo cáo hoàn thiện cấu trúc](docs/HOAN_THIEN_CAU_TRUC_20261028.md)
- [Tài liệu tổng hợp nghiệp vụ](docs/TAI_LIEU_TONG_HOP_DU_AN.md)
- [Hướng dẫn kiểm thử](HUONG_DAN_KIEM_THU.md)
- [Thiết lập và hướng dẫn của nhóm](docs/HUONG_DAN_SETUP_VA_DANH_SACH_PROMPT_AI.md)

Các báo cáo có ngày cũ mô tả thời điểm kiểm tra tương ứng. Khi khác biệt, đối chiếu source, profile được chọn và kết quả mới nhất.
