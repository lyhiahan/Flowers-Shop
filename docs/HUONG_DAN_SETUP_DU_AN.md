# 🛠️ TÀI LIỆU HƯỚNG DẪN CÀI ĐẶT & KHỞI CHẠY DỰ ÁN (SETUP GUIDE)
## ĐỒ ÁN: HỆ THỐNG WEBSITE BÁN HÀNG THEO MÔ HÌNH CHUỖI CỬA HÀNG HOA TƯƠI
### DỰ ÁN: HOA TƯƠI SÀI GÒN (BLOOM FLOWER CHAIN)
### Môn học: Lập trình Web — Học kỳ 1 (Năm học 2026 - 2027)

---

> **MỤC ĐÍCH TÀI LIỆU**:
> Tài liệu này cung cấp hướng dẫn từng bước chi tiết (Step-by-Step Setup Guide) để Giảng viên và Hội đồng chấm thi có thể dễ dàng cài đặt môi trường, nạp cơ sở dữ liệu mẫu và khởi chạy website trơn tru trên máy tính cá nhân hoặc môi trường Container.
> **Trạng thái triển khai trực tuyến (Live Demo):** [https://flowers-shop-buzm.onrender.com](https://flowers-shop-buzm.onrender.com)

---

## MỤC LỤC
1. [Yêu cầu Môi trường & Phần mềm cần thiết](#1-yêu-cầu-môi-trường--phần-mềm-cần-thiết)
2. [Danh sách Tài khoản Thử nghiệm (Demo Credentials)](#2-danh-sách-tài-khoản-thử-nghiệm-demo-credentials)
3. [Phương án 1: Khởi chạy bằng Docker Container (Khuyên dùng)](#3-phương-án-1-khởi-chạy-bằng-docker-container-khuyên-dùng)
4. [Phương án 2: Khởi chạy với Database Aiven Cloud (PostgreSQL)](#4-phương-án-2-khởi-chạy-với-database-aiven-cloud-postgresql)
5. [Phương án 3: Khởi chạy với PostgreSQL Local](#5-phương-án-3-khởi-chạy-với-postgresql-local)
6. [Phương án 4: Khởi chạy với Microsoft SQL Server Local](#6-phương-án-4-khởi-chạy-với-microsoft-sql-server-local)
7. [Cấu hình Biến môi trường (`.env`)](#7-cấu-hình-biến-môi-trường-env)
8. [Xử lý Sự cố thường gặp (Troubleshooting)](#8-xử-lý-sự-cố-thường-gặp-troubleshooting)

---

<a name="1-yeu-cau-moi-truong"></a>
## 1. YÊU CẦU MÔI TRƯỜNG & PHẦN MỀM CẦN THIẾT

- **Hệ điều hành**: Windows 10/11 (64-bit), macOS hoặc Linux.
- **Java Development Kit (JDK)**: **JDK 21 LTS** (Khuyên dùng Eclipse Temurin 21 hoặc OpenJDK 21).
  - Dự án tích hợp sẵn bản Temurin JDK 21 portable tại thư mục `.tools/jdk-21.0.12.1+1` trên môi trường Windows.
- **Công cụ build**: Dự án đã tích hợp sẵn **Maven Wrapper** (`mvnw` và `mvnw.cmd`), không cần cài đặt Maven riêng.
- **Cơ sở dữ liệu**: Hỗ trợ 2 hệ quản trị độc lập:
  - **PostgreSQL 16+** (Local hoặc Cloud Aiven/Supabase).
  - **Microsoft SQL Server 2019+** (Khuyên dùng SQL Server Express miễn phí).
- **Trình duyệt**: Google Chrome, Microsoft Edge, Firefox.

---

<a name="2-tai-khoan-thu-nghiem"></a>
## 2. DANH SÁCH TÀI KHOẢN THỬ NGHIỆM (DEMO CREDENTIALS)

Toàn bộ tài khoản đã được nạp sẵn mật khẩu mã hóa an toàn bằng thuật toán **BCrypt**:

| STT | Vai trò (Role) | Tên đăng nhập (Username) | Mật khẩu (Password) | URL đích tự động sau đăng nhập | Quyền hạn và phạm vi chính |
|:---:|:---|:---:|:---:|:---|:---|
| **1** | **Khách vãng lai (Guest)** | *(Không đăng nhập)* | — | [http://localhost:8080/](http://localhost:8080/) | Xem trang chủ, tìm hoa, lọc theo chi nhánh, xem đánh giá |
| **2** | **Khách mua hoa (Customer)** | `local_customer` | `LocalTest123!` | [http://localhost:8080/](http://localhost:8080/) | Đặt hoa, chọn giao tận nơi/nhận tại quầy, ví hoa tươi, chat realtime |
| **3** | **Quản lý Q1 Bến Thành (Manager)** | `local_manager` | `LocalTest123!` | [http://localhost:8080/manager/dashboard](http://localhost:8080/manager/dashboard) | Quản lý kho hoa Q1, tiến độ cắm hoa, xem đơn chi nhánh, ví cửa hàng |
| **4** | **Nhân viên chi nhánh Q1 (Staff)** | `local_staff` | `LocalTest123!` | [http://localhost:8080/staff/orders](http://localhost:8080/staff/orders) | Bàn làm việc nhân viên: Tiếp nhận đơn, cắm hoa, giao hàng COD, chat tại shop |
| **5** | **Quản trị viên (Admin)** | `local_admin` | `LocalTest123!` | [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard) | Quản trị toàn chuỗi, cấp phép cửa hàng, quản lý danh mục, audit chat |

*(Hệ thống cũng hỗ trợ đăng nhập nhanh bằng bộ tài khoản ngắn gọn: `admin`, `manager_q1`, `staff_q1`, `khachhang` với mật khẩu chung `123456`).*

---

<a name="3-phuong-an-1-docker"></a>
## 3. PHƯƠNG ÁN 1: KHỞI CHẠY BẰNG DOCKER CONTAINER (KHUYÊN DÙNG)

Dự án cung cấp `Dockerfile` tối ưu hóa đa tầng (Multi-stage build) để chạy nhanh nhất trên mọi máy tính đã cài Docker:

```bash
# 1. Mở terminal tại thư mục gốc dự án và build image:
docker build -t flowers-shop:latest .

# 2. Khởi chạy container trên cổng 8080:
docker run -d -p 8080:8080 --name flowers-shop flowers-shop:latest

# 3. Mở trình duyệt và truy cập:
# http://localhost:8080
```

---

<a name="4-phuong-an-2-aiven"></a>
## 4. PHƯƠNG ÁN 2: KHỞI CHẠY VỚI DATABASE AIVEN CLOUD (POSTGRESQL)

Đây là phương thức nhóm sử dụng để demo và deploy trên Cloud Render, CSDL đã có sẵn 45 bảng và 939 dòng dữ liệu mẫu:

1. Mở cửa sổ **PowerShell** tại thư mục gốc dự án:
   ```powershell
   .\start-web.ps1 -Init
   ```
2. Mở file `.env.aiven` vừa được tạo, điền thông tin kết nối Aiven:
   ```properties
   AIVEN_HOST=flowershop-db-xxx.aivencloud.com
   AIVEN_PORT=15432
   AIVEN_DATABASE=defaultdb
   AIVEN_USER=avnadmin
   AIVEN_PASSWORD=your_password
   AIVEN_SSLMODE=require
   ```
3. Kiểm tra cấu hình và khởi chạy ứng dụng:
   ```powershell
   .\start-web.ps1 -Check
   .\start-web.ps1
   ```
4. Khi console báo `Started FinalprojectApplication`, mở trình duyệt: **[http://localhost:8080](http://localhost:8080)**.

---

<a name="5-phuong-an-3-postgres-local"></a>
## 5. PHƯƠNG ÁN 3: KHỞI CHẠY VỚI POSTGRESQL LOCAL

Dành cho máy đã cài đặt PostgreSQL:

1. Tạo database và nạp 939 dòng dữ liệu mẫu từ file SQL đính kèm:
   ```bash
   createdb -h 127.0.0.1 -p 5432 -U postgres flower_shop
   psql -h 127.0.0.1 -p 5432 -U postgres -d flower_shop -f database/FlowersShop_PostgreSQL_Full.sql
   ```
2. Khởi tạo cấu hình kết nối local:
   ```powershell
   .\start-web.ps1 -Database Local -Init
   ```
3. Chạy ứng dụng:
   ```powershell
   .\start-web.ps1 -Database Local
   ```

---

<a name="6-phuong-an-4-sqlserver-local"></a>
## 6. PHƯƠNG ÁN 4: KHỞI CHẠY VỚI MICROSOFT SQL SERVER LOCAL

Dành cho máy đã cài đặt Microsoft SQL Server & SSMS:

1. **Tạo Database & Nạp dữ liệu:**
   - Mở SSMS, mở file `database/FlowersShop_SQLServer_Full.sql`.
   - Nhấn **F5** (Execute) để tạo database `FlowersShop` và 939 dòng dữ liệu mẫu.
2. **Cấu hình file `.env`:**
   - Sao chép file `.env.example` thành `.env`, điền thông tin đăng nhập SQL Server (user `sa` hoặc tài khoản Windows).
3. **Khởi chạy ứng dụng bằng script tự động:**
   ```powershell
   .\scripts\run-local.ps1 -DatabaseName FlowersShop -Server '127.0.0.1:1433'
   ```
   *Hoặc chạy qua script tiện ích:*
   ```powershell
   .\scripts\sqlserver.ps1 -Action run
   ```

---

<a name="7-cau-hinh-env"></a>
## 7. CẤU HÌNH BIẾN MÔI TRƯỜNG (`.env`)

Để quản lý an toàn các mã khóa bí mật (Secret Keys), hệ thống sử dụng file `.env` cục bộ (đã được cấu hình trong `.gitignore` để tránh rò rỉ mã khóa):

```dotenv
# Cấu hình lưu trữ ảnh Cloudinary CDN
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret

# Cấu hình gửi OTP Brevo API (nếu dùng xác thực email thật)
MAIL_PROVIDER=brevo
BREVO_API_KEY=your_brevo_api_key
MAIL_FROM_EMAIL=your_verified_email@gmail.com
MAIL_FROM_NAME=Tiệm Hoa Tươi Sài Gòn
```

> [!NOTE]
> Nếu bạn chưa có tài khoản Cloudinary, hệ thống sẽ tự động kích hoạt cơ chế ảnh dự phòng cục bộ (Local Placeholder Fallback) tại `/images/`, không làm gián đoạn trải nghiệm người dùng.

---

<a name="8-troubleshooting"></a>
## 8. XỬ LÝ SỰ CỐ THƯỜNG GẶP (TROUBLESHOOTING)

1. **Lỗi xung đột cổng 8080 (`Port 8080 was already in use`)**:
   - *Khắc phục*: Tắt tiến trình đang chiếm cổng bằng PowerShell:
     ```powershell
     Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess | Stop-Process -Force
     ```
   - Hoặc khởi chạy trên cổng khác:
     ```powershell
     .\start-web.ps1 -Port 8081
     ```
2. **Lỗi kết nối SQL Server `Connection refused: connect`**:
   - *Khắc phục*: Mở `services.msc`, kiểm tra dịch vụ `SQL Server (SQLEXPRESS)` và `SQL Server Browser` đã ở trạng thái **Running**. Đồng thời kiểm tra giao thức TCP/IP đã được **Enabled** trong *SQL Server Configuration Manager*.
3. **Lỗi chứng chỉ TLS Aiven `PKIX path building failed`**:
   - *Khắc phục*: Trong file `.env.aiven`, đảm bảo cấu hình `AIVEN_SSLMODE=require` (hoặc tải chứng chỉ CA `ca.pem` đặt tại thư mục gốc và cấu hình `AIVEN_CA_CERT=ca.pem`).
4. **Lỗi PowerShell không cho chạy file script (`ExecutionPolicy`)**:
   - *Khắc phục*: Mở PowerShell với quyền Administrator và chạy lệnh:
     ```powershell
     Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
     ```
