# Chạy và test bằng bản sao PostgreSQL local

Thiết lập ngày 28/10/2026 trên nhánh `quocanh-test`. Website hiện dùng bản sao dữ liệu Aiven trên máy này; thay đổi khi test không ghi trở lại Aiven.

## Mở website

Địa chỉ: **http://127.0.0.1:8080**.

Lần sau chạy từ gốc repo:

```powershell
.\start-web.ps1
```

Launcher local hiện mặc định chọn `Local`, tự bật PostgreSQL nếu chưa chạy rồi bật Spring Boot. Nếu cổng 8080 đã có phiên đang chạy, launcher sẽ báo thay vì mở thêm phiên trùng.

PostgreSQL lưu bền trong `.tools/local-postgres/data`, nghe ở `127.0.0.1:15432`, database `flower_local_20261028`. Không dùng H2 mem; đã kiểm tra tắt/mở cả DB lẫn web và dữ liệu vẫn còn. Không xóa `.tools/local-postgres/data` nếu muốn giữ kết quả test.

`start-web.ps1 -Database Aiven` là tùy chọn quay về DB chung khi đã dừng phiên local; không dùng tùy chọn này để test ghi dữ liệu trên bản local. Launcher là file local bị Git ignore.

## Tài khoản test local

Bốn tài khoản sau được thêm riêng vào bản local. Mật khẩu chung: **`LocalTest123!`**.

| Vai trò | Tên đăng nhập |
|---|---|
| Khách hàng | `local_customer` |
| Nhân viên | `local_staff` |
| Quản lý | `local_manager` |
| Admin | `local_admin` |

Manager và staff được phân công vào một chi nhánh đang mở đã sao chép. Manager có membership PARTNER nhưng không thay thế chủ chi nhánh gốc; các thao tác chỉ chủ chi nhánh được làm vẫn giữ kiểm tra quyền. Admin có thể dùng các trang quản trị tương ứng.

Quyền cập nhật 28/10: `local_manager` chỉ vận hành chi nhánh, không có giỏ hàng, đặt mua hoặc ví khách hàng; dùng `local_customer` để test mua hoa. Ví cửa hàng của quản lý là chức năng riêng và vẫn hoạt động theo quyền chi nhánh.

Các tài khoản gốc và password hash được giữ nguyên; đăng nhập bằng mật khẩu cũ nếu biết. Bộ tên mẫu `admin`, `manager_q1`, `khachhang` trong README không tồn tại trong snapshot Aiven lần này, vì vậy không dùng chúng để kiểm tra bản sao này.

## Phạm vi dữ liệu

- Nguồn: PostgreSQL Aiven, kết nối TLS verify-full, transaction chỉ đọc REPEATABLE READ để lấy snapshot nhất quán.
- Đích: PostgreSQL 14.22 local dùng bộ binary đã có trong cache; không cần Docker/service PostgreSQL Windows.
- Đã chuyển **45 bảng ứng dụng, 861 dòng** tại thời điểm snapshot; có 17 tài khoản gốc, 4 chi nhánh, 41 sản phẩm, 77 dòng tồn kho, 43 đơn và 27 thanh toán.
- Copy qua PostgreSQL COPY, đối chiếu số dòng và SHA-256 từng bảng, kiểm tra khóa ngoại và đặt lại identity sequence. Flyway V1 ở local được tạo bằng baseline gốc có checksum khớp nguồn; không chỉnh/sửa lịch sử migration để bỏ qua lỗi.
- Sau đó thêm 4 tài khoản test mới, role, cart và membership cần thiết; tổng tài khoản local hiện 21. Snapshot ban đầu vẫn được giữ riêng.
- Không thay đổi dữ liệu nguồn bằng công cụ sao chép. Phiên web nối Aiven trước đó đã được dừng; kiểm tra phiên web local không có connection đến cổng Aiven.

File local riêng (đều bị Git ignore):

- `.env.local-postgres`: mật khẩu DB local ngẫu nhiên và JWT key riêng, ACL chỉ tài khoản Windows hiện tại.
- `.tools/local-postgres/source-snapshot.zip`: snapshot trước khi test, gồm schema/các COPY stream và checksum; có dữ liệu của bản sao, chỉ lưu ở máy này trong thư mục giới hạn ACL.
- `.tools/local-postgres/clone-verified.txt`: kết quả đối chiếu từng bảng.
- `.tools/local-postgres/clone.log`: log sao chép, chỉ in tên bảng/số dòng, không in dữ liệu hàng.
- `.tools/local-postgres/LocalPostgres.java`, `run-db.ps1`, `classpath.txt`: runtime bật DB local; cần giữ cùng thư mục dữ liệu để launcher hoạt động.

## Cấu hình và giới hạn

Profile mới `local-postgres` chỉ import `.env.local-postgres`, không nạp `.env` SQL Server hoặc `.env.aiven`. Datasource cố định loopback, JPA validate, Flyway PostgreSQL, seed tắt. DB dùng password SCRAM và chỉ nghe loopback. Các scheduler đơn hàng hoạt động trên bản local, nên trạng thái đơn test có thể thay đổi theo thời gian như ứng dụng bình thường.

MoMo và chuyển khoản chạy chế độ mock; demo payment/ví demo bật. Hỗ trợ profile local trong auto tunnel để có thể dùng Cloudflare cho thanh toán điện thoại. Truy vấn DB không đi qua Cloudflare.

Local PostgreSQL 14.22 khác phiên bản Aiven 18.6. Baseline và Hibernate schema đã validate thành công; test local không thay thế nghiệm thu mọi hành vi riêng của PostgreSQL 18/Aiven.

Ảnh có URL Cloudinary vẫn tải qua Internet; email/OTP và upload ảnh chat cần cấu hình dịch vụ tương ứng. Không sao chép secret mail/Cloudinary từ file SQL Server cũ hoặc vô hiệu hóa xác thực OTP. Cloudflare phụ thuộc mạng/provider; log có thể báo NETWORK_UNAVAILABLE và tự thử lại, không ngăn website/DB local chạy. Chưa nghiệm thu email gửi thật, ảnh chat, QR trên điện thoại hoặc toàn bộ thao tác ghi nghiệp vụ trong lần thiết lập này.

Các lỗi nghiệp vụ F2–F6 trong báo cáo review chưa được sửa bằng việc chuyển DB; đặc biệt lỗi quote voucher read-only có thể vẫn tái hiện trên PostgreSQL local.

## Kiểm tra đã chạy

- Clone 45 bảng: count/checksum khớp, các FK không có dòng mồ côi, identity đã đặt lại.
- Java compile và `DemoPaymentTunnelTests`: **13/13 đạt**; kiểm tra profile local hỗ trợ tunnel, vẫn cấm khi kèm test/prod hoặc tắt cờ.
- JavaScript: **182/182 đạt**; whitespace đạt. Không chạy lại toàn bộ Java suite vì thay đổi Java chỉ mở rộng điều kiện profile của tunnel.
- Đăng nhập 4 vai trò local và GET 15 trang guest/customer/staff/manager/admin: **15/15 HTTP 200**, 7–148 ms trong lượt đo sau khi web đã khởi động. Đây là smoke check, không phải benchmark hoặc kiểm thử toàn bộ thao tác nghiệp vụ.
- Dừng web, shutdown DB sạch bằng pg_ctl, mở lại bằng chính `start-web.ps1`: dữ liệu và cả 4 tài khoản test còn nguyên, schema validate, homepage HTTP 200 (609 ms lần đầu sau restart).
- Phiên sau kiểm tra: web PID 2548 tại cổng 8080, PostgreSQL PID 1720 tại cổng 15432; PID có thể đổi sau lần chạy tiếp theo.

Log: `target/local-postgres-java-tests.log`, `target/local-postgres-js-tests.log`, `target/web-local-postgres-restart.out.log`, `target/web-local-postgres-restart.err.log`, `.tools/local-postgres/http-checks.json`. Không commit/push trong lượt thiết lập này.
