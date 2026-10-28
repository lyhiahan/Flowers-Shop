# Kiểm tra Aiven trên máy Hân — 26/10/2026

Code: `c61c466`. Database PostgreSQL của nhóm, profile `aiven`.

- TLS verify-full và đăng nhập database: đạt sau khi nhận mật khẩu hiện tại.
- Cấu hình riêng `.env.aiven`, CA và Cloudflare trong `.tools`: đã chuẩn bị; Git bỏ qua. Không chia sẻ `.env.aiven`.
- Schema PostgreSQL V1 có đủ bảng ví khách; JPA khởi tạo/validate thành công.
- Đăng nhập tài khoản khách demo: đạt.
- Đọc `/`, `/products`, `/cart`, `/customer/wallet`, ví có lọc ngày và `/customer/orders`: HTTP 200.
- Phiên thử dùng kết nối database read-only, tắt migration/seed/tác vụ hết hạn. Không tạo đơn, thanh toán, nạp hoặc rút tiền. Số bản ghi và tổng số dư ví không thay đổi trước/sau.
- Đã dừng phiên thử riêng trên 8081; không còn phiên thử chiếm cổng 8080/8081.
- Cloudflare: chưa đạt; kiểm tra tên miền quick tunnel trả `ENOTFOUND` (DNS). Chưa kiểm thử xác nhận thanh toán bằng điện thoại. Không coi kết quả database là kết quả của luồng QR.

## Chạy ứng dụng thông thường

Từ thư mục dự án:

```powershell
.\scripts\maven.ps1 spring-boot:run
```

Sau khi có `Started FinalprojectApplication`, mở http://localhost:8080. Tài khoản database chỉ dùng cho kết nối; đăng nhập website bằng tài khoản website. Phiên chạy thông thường có thể cập nhật dữ liệu dùng chung theo nghiệp vụ, khác phiên smoke chỉ đọc. Chờ `Demo phone payment ready` trước khi thử QR điện thoại.

Không cần nhập lại mật khẩu database sau khi đã cấu hình trên máy này. Không nhập lại hoặc sao chép database khi khởi động.
