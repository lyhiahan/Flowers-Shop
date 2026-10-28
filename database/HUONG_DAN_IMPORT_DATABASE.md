# HƯỚNG DẪN IMPORT CƠ SỞ DỮ LIỆU ĐỒ ÁN (DATABASE SETUP GUIDE)
## ĐỒ ÁN: WEBSITE BÁN HÀNG THEO MÔ HÌNH CHUỖI CỬA HÀNG (BLOOM FLOWER CHAIN / HOA TƯƠI SÀI GÒN)
### Môn học: Lập trình Web - Học kỳ 1 (2026 - 2027)

---

Tài liệu này hướng dẫn Giảng viên và Hội đồng chấm thi cách import cơ sở dữ liệu hoàn chỉnh của đồ án với **1 cú click**. Nhóm đã chuẩn bị sẵn **2 tệp SQL riêng biệt** tương thích 100% với 2 hệ quản trị cơ sở dữ liệu phổ biến nhất:

1. **`RentalShop_PostgreSQL_Full.sql`**: Dành cho **PostgreSQL** (pgAdmin / DBeaver / psql / Cloud Aiven / Supabase).
2. **`RentalShop_SQLServer_Full.sql`**: Dành cho **Microsoft SQL Server** (SQL Server Management Studio - SSMS / Azure Data Studio).

---

## 1. THÔNG TIN TỔNG QUAN DỮ LIỆU NẠP SẴN
- **Số lượng bảng:** 45 bảng chuẩn hóa (3NF).
- **Tổng số dòng dữ liệu (Rows):** 939 dòng dữ liệu nghiệp vụ thực tế.
- **Dữ liệu thực nghiệm bao gồm:**
  - **4 chi nhánh cửa hàng hoa tươi:** Quận 1 (Bến Thành), Quận 7 (Phú Mỹ Hưng), TP. Thủ Đức (Làng Đại Học), Cao Lãnh (Đồng Tháp).
  - **41 mẫu hoa tươi:** Đầy đủ hình ảnh Unsplash sắc nét, giá bán lẻ, mô tả chi tiết, phân loại dịp tặng & kiểu dáng.
  - **77 bản ghi tồn kho chi nhánh (`store_inventories`):** Tồn kho thực tế riêng cho từng cửa hàng.
  - **23 danh mục hoa (`categories`):** Phân cấp danh mục cha/con (Hoa sinh nhật, khai trương, tốt nghiệp, hoa hồng, lan hồ điệp...).
  - **Mã giảm giá (`vouchers`):** Giảm theo số tiền, giảm theo %, miễn phí vận chuyển.
  - **Đơn hàng mẫu & Đánh giá (`orders`, `reviews`):** Đầy đủ các đơn hàng theo luồng vòng đời và đánh giá chân thực.
  - **Ví khách hàng & Ví cửa hàng (`customer_wallets`, `stores.e_wallet`):** Đầy đủ số dư thử nghiệm.

---

## 2. DANH SÁCH TÀI KHOẢN ĐĂNG NHẬP THỬ NGHIỆM

Toàn bộ tài khoản đã được nạp sẵn mật khẩu mã hóa BCrypt an toàn.

### Bộ tài khoản Test Chuẩn (Khuyến nghị sử dụng):
| Vai trò | Tên đăng nhập (Username) | Mật khẩu (Password) | Chức năng chính |
|---|---|---|---|
| **Quản trị viên (Admin)** | `local_admin` | `LocalTest123!` | Quản trị toàn chuỗi, duyệt cửa hàng, xem audit chat |
| **Quản lý chi nhánh (Manager)** | `local_manager` | `LocalTest123!` | Quản lý kho hoa Q1, xem đơn chi nhánh, ví cửa hàng |
| **Nhân viên chi nhánh (Staff)** | `local_staff` | `LocalTest123!` | Bàn làm việc nhân viên, chuẩn bị hoa, tiếp nhận đơn |
| **Khách hàng (Customer)** | `local_customer` | `LocalTest123!` | Xem mẫu hoa, lọc theo shop, đặt hoa, dùng ví khách |

*(Ngoài ra hệ thống còn hỗ trợ các tài khoản legacy: `admin`, `manager_q1`, `khachhang` với mật khẩu chung `123456`).*

---

## 3. HƯỚNG DẪN IMPORT VÀO POSTGRESQL

### Cách 1: Sử dụng giao diện pgAdmin
1. Mở **pgAdmin 4**, kết nối vào Server PostgreSQL của bạn.
2. Tạo mới một database tên: `flower_local_20261028` (hoặc `RentalShop`).
3. Chuột phải vào database vừa tạo $\rightarrow$ Chọn **Query Tool**.
4. Mở file **`RentalShop_PostgreSQL_Full.sql`** (bằng nút Open Folder hoặc kéo thả toàn bộ nội dung file vào Query Tool).
5. Nhấn phím **F5** (hoặc bấm nút **Execute / Run** hình tam giác).
6. Thông báo thành công: `Query returned successfully in ... secs`. Toàn bộ 45 bảng và 939 dòng dữ liệu đã sẵn sàng!

### Cách 2: Sử dụng dòng lệnh `psql`
```bash
psql -U postgres -d postgres -c "CREATE DATABASE flower_local_20261028;"
psql -U postgres -d flower_local_20261028 -f RentalShop_PostgreSQL_Full.sql
```

---

## 4. HƯỚNG DẪN IMPORT VÀO MICROSOFT SQL SERVER

### Cách 1: Sử dụng SQL Server Management Studio (SSMS)
1. Mở **SQL Server Management Studio (SSMS)** và kết nối vào instance SQL Server (ví dụ: `.\SQLEXPRESS` hoặc `localhost`).
2. Vào menu **File > Open > File...** (phím tắt `Ctrl + O`), duyệt và chọn file **`RentalShop_SQLServer_Full.sql`**.
3. File SQL đã có sẵn lệnh `CREATE DATABASE RentalShop;` và `USE RentalShop;`.
4. Nhấn phím **F5** (hoặc bấm nút **! Execute** trên thanh công cụ).
5. Chờ vài giây khi xuất hiện thông báo:
   ```text
   Commands completed successfully.
   (45 tables created, 939 rows inserted)
   ```
6. Toàn bộ CSDL `RentalShop` đã được khởi tạo hoàn chỉnh!

---

## 5. CẤU HÌNH KẾT NỐI VỚI ỨNG DỤNG SPRING BOOT

Sau khi import cơ sở dữ liệu, ứng dụng Spring Boot có thể kết nối ngay:

* **Nếu dùng PostgreSQL:**
  Chỉ cần chạy lệnh:
  ```powershell
  .\start-web.ps1
  ```
  Ứng dụng tự động kết nối cổng `15432` hoặc `5432` và mở tại [http://localhost:8080](http://localhost:8080).

* **Nếu dùng SQL Server:**
  Chỉ định biến môi trường khi chạy:
  ```powershell
  mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
  ```
