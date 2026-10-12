# Sửa kết nối và xác minh migration SQL Server — 12/10/2026

## Kết quả

Đã kết nối và khởi động website bằng SQL Server thật, không phải H2. `RentalShop` đã ở Flyway version **4**; validate thành công. Hibernate `ddl-auto=validate` kiểm tra schema thực và ứng dụng khởi động thành công.

Nguyên nhân lỗi trước đây: máy có hai instance SQL Express dùng cổng động, trong khi cấu hình local trỏ cứng `127.0.0.1:1433`.

| Instance | Cổng khi kiểm tra | Database RentalShop | Quyết định |
|---|---|---|---|
| SQLEXPRESS | 53735 | 4 cửa hàng, 14 người dùng, 21 đơn | Người dùng xác nhận sử dụng bản này |
| SQLEXPRESS01 | 56003 | 3 cửa hàng, 7 người dùng, 5 đơn | Không thay đổi |

## Đã thực hiện

1. Đọc service/registry/cổng và metadata database bằng Windows Authentication; không thay đổi TCP, firewall hoặc restart SQL Server.
2. Xác minh tài khoản `rental_user` kết nối đúng database, không hiển thị mật khẩu và không đưa credential trở lại Git.
3. Tạo backup `COPY_ONLY` có checksum và chạy `RESTORE VERIFYONLY`. Backup giữ ở:

   `C:\Program Files\Microsoft SQL Server\MSSQL16.SQLEXPRESS\MSSQL\Backup\RentalShop_before_flyway_20261012_01.bak`

4. Chạy **2/2 kiểm thử live đạt** trên SQL Server 2022 / Microsoft JDBC 13.4:
   - Database rỗng: áp dụng V1–V4, validate toàn bộ entity, migrate lại không chạy thêm.
   - Bản sao RentalShop: xác nhận không tự baseline schema có dữ liệu; baseline V1 rõ ràng rồi áp dụng V2–V4; validate entity và giữ nguyên số dòng chính.
5. Sau khi người dùng đồng ý, baseline V1 và migrate V2–V4 trên RentalShop gốc; Flyway validate thành công. Không reset/drop bảng hay database gốc. Migration chuẩn hóa quyền và vô hiệu hóa SHIPPER cũ đúng phạm vi đã được người dùng chấp thuận.
6. Chạy website tạm ở cổng 18080 bằng RentalShop thật, tắt seed demo, bật Hibernate schema validation. HTTP `/`, `/login`, `/stores`, `/products` đều trả 200; trang chủ render tồn kho; `/api/auth/me` chưa đăng nhập JWT trả 401 đúng thiết kế. Đã dừng riêng tiến trình chẩn đoán sau thử nghiệm.
7. Đóng gói lại JAR: **BUILD SUCCESS**. Regression mặc định: **402 test, 0 failures, 0 errors, 3 skipped** (399 pass; 2 SQL Server live opt-in và 1 Cloudinary live bỏ qua mặc định). SQL Server live đã chạy riêng đạt 2/2, không chỉ dựa vào H2.
8. Dọn đúng hai database thử nghiệm `CodexRentalShopFresh_20261012_01` và `CodexRentalShopCopy_20261012_01` sau khi xác nhận không còn kết nối. Backup được giữ nguyên để khôi phục; RentalShop và instance SQLEXPRESS01 không bị xóa.

## Số dòng trước/sau migration database gốc

| Bảng | Trước | Sau |
|---|---:|---:|
| users | 14 | 14 |
| stores | 4 | 4 |
| products | 38 | 38 |
| orders | 21 | 21 |
| order_details | 23 | 23 |
| store_inventories | 152 | 152 |
| transactions | 20 | 20 |

Đây là xác nhận bảo toàn số dòng chính, không phải tuyên bố mọi cột không thay đổi: migration có bổ sung/backfill cột và chuẩn hóa quyền như đã thông báo.

## Chạy website từ nay

```powershell
.\scripts\sqlserver.ps1
```

Mở `http://localhost:8080`. Script chọn profile dev, tài khoản `rental_user` và URL:

```text
jdbc:sqlserver://127.0.0.1;instanceName=SQLEXPRESS;databaseName=RentalShop;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true;loginTimeout=15;
```

SQL Server Browser đang chạy và đã được JDBC kiểm chứng tìm đúng cổng. Không dùng cổng động 53735 làm giá trị cố định lâu dài. Nếu tắt Browser, cần cấu hình cổng TCP cố định và ghi đè URL tương ứng; không tự đổi cấu hình toàn máy trong lượt sửa này.

Credential local được mã hóa Windows DPAPI trong `.tools/sqlserver-local.json`, bị Git ignore và chỉ user Windows đã mã hóa giải mã được. Script ưu tiên `SPRING_DATASOURCE_PASSWORD`; nếu không có credential sẽ hỏi mật khẩu bằng SecureString, không ghi mật khẩu vào console. Không lấy credential từ Git trong script chạy thường ngày. Nên thay mật khẩu cũ từng tồn tại trong lịch sử Git và cung cấp mật khẩu mới qua biến môi trường.

Chạy từ IDE: chọn profile dev và đặt username/password trong cấu hình chạy; IDE không tự giải mã file của script. Production phải đặt URL/chứng chỉ/secret riêng, không sao chép tùy chọn tin cậy chứng chỉ local.

Đọc trạng thái không sửa database:

```powershell
.\scripts\sqlserver.ps1 -Action info
.\scripts\sqlserver.ps1 -Action validate
```

**Không baseline lại RentalShop hiện tại**: nó đã có lịch sử version 4. Các action baseline/migrate dành cho database cần nâng cấp, phải backup và xác nhận tên đích trước.

## Log và kiểm thử bổ sung

- `.tools/sqlserver-live-validation.log`: migration thật trên database rỗng/bản sao.
- `.tools/sqlserver-rentalshop-migration.log`: baseline/migrate/validate RentalShop gốc.
- `.tools/sqlserver-current-info.log`: script local đọc lịch sử V4.
- `.tools/sqlserver-http.out.log`, `.tools/sqlserver-http.err.log`: khởi động với SQL Server và validate schema.
- `.tools/sqlserver-change-regression.log`: regression/package sau sửa cấu hình.

`SqlServerMigrationLiveTests` chỉ chạy khi bật `-Dsqlserver.live=true`; cần provision database rỗng và bản sao riêng rồi đặt `SQLSERVER_TEST_FRESH_DATABASE` / `SQLSERVER_TEST_COPY_DATABASE`. Tên đích phải có tiền tố `CodexRentalShopFresh_` hoặc `CodexRentalShopCopy_` và hậu tố số; test từ chối tên RentalShop gốc. Không dùng các database đã migrate trong lần trước để thay thế fixture rỗng/chưa baseline.

Cloudinary/mail API/khóa JWT production vẫn cần cấu hình riêng; sửa SQL Server không xác nhận các dịch vụ ngoài này. OTP hiện mặc định dùng API thay Gmail SMTP; xem [hướng dẫn cấu hình OTP](OTP_EMAIL_SETUP_20261012.md).
