# Tên tài khoản mẫu

Đã đổi 11 tài khoản mẫu trên `RentalShop` ở SQLEXPRESS. Bản sao được thử trước; database gốc được sao lưu và kiểm tra backup trước khi cập nhật. Chỉ đổi `users.username` và `users.full_name`, không tạo tài khoản mới.

| Tên đăng nhập cũ | Tên đăng nhập mới | Họ tên hiển thị |
| --- | --- | --- |
| admin | nguyenthanhminh | Nguyễn Thanh Minh |
| manager_q1 | nguyenquanghuy | Nguyễn Quang Huy |
| khachhang | leminhhoa | Lê Minh Hoa |
| khachhang2 | tranthihoa | Trần Thị Hoa |
| vendor_flower | tranduclong | Trần Đức Long |
| vendor | tranminhtuan | Trần Minh Tuấn |
| staff_q7_flt | nguyenmailan | Nguyễn Thị Mai Lan |
| staff_q7_order | tranbaophuc | Trần Bảo Phúc |
| staff_q7_inv | lehoangkhoi | Lê Hoàng Khôi |
| khachhang_q7 | nguyenbichtram | Nguyễn Ngọc Bích Trâm |
| khachhang_dn | namsaigon | Công Ty Cổ Phần Công Nghệ Nam Sài Gòn |

Họ tên có dấu dành cho hiển thị; tên đăng nhập không dấu và duy nhất dành cho đăng nhập. Chức vụ/vai trò tách khỏi họ tên. Tài khoản khách doanh nghiệp vẫn giữ tên tổ chức, không đổi thành tên cá nhân giả.

Email, mật khẩu hiện có, ID, quyền, ví, đơn hàng (kể cả #23), người nhận trên đơn và nội dung tin nhắn không đổi. Những tài khoản tự đăng ký như `lygiahan`, `lygiahan03`, `dinhquocanh` không bị sửa. Tài khoản giao hàng cũ đã vô hiệu hóa không được kích hoạt lại.

Sau khi đổi tên, đăng xuất và đăng nhập lại bằng tên mới với mật khẩu hiện có. Bộ nút đăng nhập nhanh dùng bốn tài khoản do bộ khởi tạo database rỗng thực sự tạo ra; không còn nút `khachhang2`/`khachhang3` giả định luôn tồn tại. Tài khoản mẫu trên môi trường mới dùng mật khẩu demo `123456`; việc đổi tên không đặt lại mật khẩu của database đang sử dụng.

## Cách kiểm tra

1. Dừng website và chạy lại `scripts/sqlserver.ps1`; tải lại trang bằng Ctrl+F5.
2. Trang chủ khách không còn khối tổng tồn kho, nhưng từng sản phẩm vẫn hiện số lượng/“Hết hàng” và tự cập nhật.
3. Đăng nhập admin bằng `nguyenthanhminh`, quản lý Q1 bằng `nguyenquanghuy`, khách mẫu bằng `leminhhoa` hoặc `tranthihoa`. Email cũ vẫn dùng để đăng nhập được; mật khẩu không đổi.
4. Mở cửa sổ ẩn danh: được xem/tìm sản phẩm; vào giỏ hàng/đặt hàng hoặc chat phải đăng nhập.

Khách vãng lai là **anonymous**, không phải bản ghi trong `users`, không cần thêm role `GUEST`. `ADMIN`, `MANAGER`, `USER` là quyền của tài khoản đã xác thực, không phải của người truy cập chưa đăng nhập.

Script `scripts/rename-demo-accounts.ps1` mặc định chỉ xem trước. `-Action Test` thử rồi rollback; `-Action Apply` cập nhật có backup và kiểm tra toàn bộ bảng nghiệp vụ bằng checksum. Chỉ đổi tài khoản khớp tên đăng nhập cũ + họ tên mẫu + role; nếu tên mới đã thuộc người khác thì rollback, không ghi đè người đó.
