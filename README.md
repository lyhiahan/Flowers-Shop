# 🌸 TIỆM HOA TƯƠI SÀI GÒN — BLOOM FLOWER CHAIN
> **Đồ án môn học:** Lập trình Web — Học kỳ 1 (Năm học 2026 - 2027)  
> **Mô hình kiến trúc:** Website bán hàng theo mô hình chuỗi cửa hàng hoa tươi đa chi nhánh (Multi-Store Flower Retail Chain)  
> **Ngăn xếp công nghệ:** Java 21 LTS + Spring Boot 4.1.1 + Spring Security + JPA Hibernate + Thymeleaf + Bootstrap 5 + PostgreSQL / SQL Server + WebSocket STOMP + Cloudinary  
> **Trạng thái triển khai Cloud (Live Demo):** [https://flowers-shop-buzm.onrender.com](https://flowers-shop-buzm.onrender.com)

---

## 👥 THÀNH VIÊN NHÓM THỰC HIỆN
- **Lý Gia Hân** (Trưởng nhóm / Full-stack Developer): Kiến trúc hệ thống, Quản trị chi nhánh, Kiểm soát tồn kho đa chi nhánh, Security kép, Báo cáo & Prompt Engineering.
- **Nguyễn Quốc Anh** (Backend & Data Engineer): Tích hợp cổng thanh toán MoMo Sandbox, Concurrency Locking chống bán âm kho, Live Chat WebSocket, Đóng gói CSDL & Test.
- **Đỗ Thành Thanh Tài** (Frontend & UX Engineer): Giao diện Floral Theme responsive, Trải nghiệm mua hàng & Giỏ hàng, Bộ lọc đa thuộc tính, Đánh giá sản phẩm Cloudinary.

---

## 📋 MỤC LỤC
1. [Giới thiệu Đề tài & Điểm nhấn Kỹ thuật](#1-giới-thiệu-đề-tài--điểm-nhấn-kỹ-thuật)
2. [Danh sách Tài khoản Thử nghiệm (Demo Credentials)](#2-danh-sách-tài-khoản-thử-nghiệm-demo-credentials)
3. [Hướng dẫn Cài đặt & Khởi chạy Ứng dụng (Setup & Run)](#3-hướng-dẫn-cài-đặt--khởi-chạy-ứng-dụng-setup--run)
   - [Phương án 1: Khởi chạy bằng Docker Container](#phương-án-1-khởi-chạy-bằng-docker-container-khuyên-dùng)
   - [Phương án 2: Khởi chạy với Database Aiven Cloud (PostgreSQL)](#phương-án-2-khởi-chạy-với-database-aiven-cloud-postgresql)
   - [Phương án 3: Khởi chạy với PostgreSQL Local](#phương-án-3-khởi-chạy-với-postgresql-local)
   - [Phương án 4: Khởi chạy với Microsoft SQL Server Local](#phương-án-4-khởi-chạy-với-microsoft-sql-server-local)
4. [Phân quyền Người dùng & Ma trận Nghiệp vụ](#4-phân-quyền-người-dùng--ma-trận-nghiệp-vụ)
5. [Cấu trúc Thư mục Mã nguồn](#5-cấu-trúc-thư-mục-mã-nguồn)
6. [Kết quả Kiểm thử Tự động (Testing Summary)](#6-kết-quả-kiểm-thử-tự-động-testing-summary)
7. [Danh mục Hồ sơ & Tài liệu Nộp kèm](#7-danh-mục-hồ-sơ--tài-liệu-nộp-kèm)

---

## 1. GIỚI THIỆU ĐỀ TÀI & ĐIỂM NHẤN KỸ THUẬT

Hệ thống **Tiệm Hoa Tươi Sài Gòn (Bloom Flower Chain)** giải quyết bài toán vận hành chuỗi bán lẻ hoa tươi thực tế với 4 chi nhánh tại TP.HCM và Đồng Tháp:
- **Tồn kho độc lập theo chi nhánh (`StoreInventory`):** Khách hàng có thể chọn xem tồn kho và đặt hoa tại chi nhánh gần nhất (Quận 1, Quận 7, TP. Thủ Đức, Cao Lãnh). Khi đổi chi nhánh trên thanh bộ lọc, hệ thống tự động kiểm tra số lượng hoa sẵn có của từng cửa hàng.
- **Xử lý giao dịch đồng thời & Chống bán âm kho (Concurrency Control):** Áp dụng khóa bi quan `@Lock(LockModeType.PESSIMISTIC_WRITE)` trên kho hàng chi nhánh và kho mã giảm giá (Voucher), bảo vệ an toàn giao dịch khi nhiều khách cùng đặt hoa tại giờ cao điểm.
- **Bảo mật kép kết hợp (Hybrid Security Architecture):**
  - Trình duyệt Web Thymeleaf: Quản lý phiên bằng `HttpSession`, Cookie an toàn và bảo vệ chống tấn công `CSRF`.
  - Cổng REST API: Xác thực phi trạng thái bằng `JWT Bearer Token` (Access Token + Refresh Token).
  - Phân quyền theo chi nhánh: `StoreAccessService` ngăn chặn lỗ hổng IDOR (Quản lý chi nhánh A không thể xem hoặc sửa dữ liệu chi nhánh B).
- **Thời gian thực (Realtime STOMP WebSocket):**
  - Live Chat tư vấn hoa tươi trực tiếp giữa Khách hàng và Nhân viên cửa hàng có đính kèm thẻ sản phẩm.
  - Chuông thông báo đơn hàng mới và biến động ví thời gian thực qua kênh WebSocket Topic.
- **Thanh toán đa phương thức & Ví hoa tươi nội bộ:**
  - Cổng MoMo Sandbox QR Code.
  - Ví hoa tươi nội bộ (`customer_wallets`) nạp tiền và thanh toán 1 click.
  - Thanh toán khi nhận hoa (COD).
- **Đánh giá sản phẩm kèm ảnh thực tế qua Cloudinary:** Khách mua hàng có thể gửi review sao và tải ảnh hoa thực nhận lên Cloudinary CDN.

---

## 2. DANH SÁCH TÀI KHOẢN THỬ NGHIỆM (DEMO CREDENTIALS)

Toàn bộ tài khoản đã được nạp sẵn mật khẩu mã hóa an toàn bằng thuật toán **BCrypt**:

| Vai trò (Role) | Tên đăng nhập (Username) | Mật khẩu (Password) | Phạm vi & Chức năng thử nghiệm |
|---|---|---|---|
| **Quản trị viên (ADMIN)** | `local_admin` | `LocalTest123!` | Quản trị toàn chuỗi, cấp phép chi nhánh, giám sát audit chat, thống kê toàn hệ thống |
| **Quản lý chi nhánh (MANAGER)** | `local_manager` | `LocalTest123!` | Quản lý kho hoa Q1 Bến Thành, phân công nhân sự, quản lý ví chi nhánh (không có quyền mua hoa) |
| **Nhân viên chi nhánh (STAFF)** | `local_staff` | `LocalTest123!` | Bàn làm việc nhân viên: Tiếp nhận đơn, cắm hoa, giao hàng COD, chat tư vấn khách tại cửa hàng |
| **Khách hàng (CUSTOMER)** | `local_customer` | `LocalTest123!` | Đặt hoa, chọn chi nhánh giao, dùng ví hoa tươi, đổi mật khẩu OTP, đánh giá sản phẩm |

*(Hệ thống cũng hỗ trợ đăng nhập bằng bộ tài khoản ngắn gọn: `admin`, `manager_q1`, `staff_q1`, `khachhang` với mật khẩu chung `123456`).*

---

## 3. HƯỚNG DẪN CÀI ĐẶT & KHỞI CHẠY ỨNG DỤNG (SETUP & RUN)

### Yêu cầu môi trường tối thiểu:
- **Java:** JDK 21 trở lên (LTS).
- **Maven:** Đã tích hợp sẵn Maven Wrapper (`mvnw` / `mvnw.cmd`), không bắt buộc cài Maven riêng.
- **Git:** Đã cài đặt trên máy.

---

### Phương án 1: Khởi chạy bằng Docker Container (Khuyên dùng)
Dự án đã được container hóa hoàn chỉnh với `Dockerfile` tối ưu hóa đa tầng (Multi-stage build):

```bash
# 1. Build image Docker
docker build -t flowers-shop:latest .

# 2. Khởi chạy container trên cổng 8080
docker run -d -p 8080:8080 --name flowers-shop flowers-shop:latest

# 3. Truy cập ứng dụng:
# Mở trình duyệt tại: http://localhost:8080
```

---

### Phương án 2: Khởi chạy với Database Aiven Cloud (PostgreSQL)
Đây là phương án nhóm sử dụng để demo và deploy trực tuyến, CSDL đã có sẵn 45 bảng và 939 dòng dữ liệu mẫu:

1. Mở PowerShell tại thư mục dự án.
2. Khởi tạo file cấu hình môi trường:
   ```powershell
   .\start-web.ps1 -Init
   ```
3. Điền thông tin kết nối Aiven vào file `.env.aiven` (được cung cấp riêng hoặc dùng CSDL nhóm):
   ```properties
   AIVEN_HOST=flowershop-db-xxx.aivencloud.com
   AIVEN_PORT=15432
   AIVEN_DATABASE=defaultdb
   AIVEN_USER=avnadmin
   AIVEN_PASSWORD=your_password
   AIVEN_SSLMODE=require
   ```
4. Kiểm tra và khởi chạy:
   ```powershell
   .\start-web.ps1 -Check
   .\start-web.ps1
   ```
5. Khi log hiển thị `Started FinalprojectApplication`, mở trình duyệt: **[http://localhost:8080](http://localhost:8080)**.

---

### Phương án 3: Khởi chạy với PostgreSQL Local
Dành cho máy đã cài đặt PostgreSQL:

1. Tạo database và nạp 939 dòng dữ liệu mẫu:
   ```bash
   createdb -h 127.0.0.1 -p 5432 -U postgres flower_shop
   psql -h 127.0.0.1 -p 5432 -U postgres -d flower_shop -f database/FlowersShop_PostgreSQL_Full.sql
   ```
2. Khởi tạo cấu hình local:
   ```powershell
   .\start-web.ps1 -Database Local -Init
   ```
3. Khởi chạy ứng dụng:
   ```powershell
   .\start-web.ps1 -Database Local
   ```

---

### Phương án 4: Khởi chạy với Microsoft SQL Server Local
Dành cho máy sử dụng Microsoft SQL Server & SSMS:

1. Mở SSMS, mở file `database/FlowersShop_SQLServer_Full.sql` và nhấn **F5** (Execute) để tạo database `FlowersShop` và 939 dòng dữ liệu.
2. Tạo file `.env` từ `.env.example`, điền user/password sa của SQL Server.
3. Chạy lệnh khởi chạy tự động:
   ```powershell
   .\scripts\run-local.ps1 -DatabaseName FlowersShop -Server '127.0.0.1:1433'
   ```

---

## 4. PHÂN QUYỀN NGƯỜI DÙNG & MA TRẬN NGHIỆP VỤ

```
                            ┌────────────────────────────────────────┐
                            │    TIỆM HOA TƯƠI SÀI GÒN (SECURITY)    │
                            └───────────────────┬────────────────────┘
                                                │
         ┌──────────────────┬───────────────────┼──────────────────┬──────────────────┐
         │                  │                   │                  │                  │
         ▼                  ▼                   ▼                  ▼                  ▼
    [ANONYMOUS]        [CUSTOMER]            [STAFF]           [MANAGER]           [ADMIN]
  - Xem danh mục     - Giỏ hàng & Mua    - Bàn làm việc     - Quản lý kho Q1   - Quản trị chuỗi
  - Lọc chi nhánh    - Ví hoa tươi       - Nhận/cắm hoa     - Doanh thu Q1     - Quản lý shop
  - Tìm kiếm hoa     - Đánh giá sao      - Giao đơn COD     - Nhân sự Q1       - Quản lý user
  - Xem tin tức      - Chat tư vấn       - Chat cửa hàng    - Ví cửa hàng      - Audit chat
```

- **Tách biệt quyền nghiêm ngặt:** Tài khoản `MANAGER` và `ADMIN` chỉ tập trung quản trị, bị chặn hoàn toàn quyền mua hàng/giỏ hàng server-side để tránh xung đột lợi ích nghiệp vụ.
- **Staff Workspace:** Nhân viên chi nhánh có sidebar làm việc riêng, xử lý tuần tự vòng đời đơn hàng: `PENDING` $\rightarrow$ `CONFIRMED` $\rightarrow$ `PREPARING` $\rightarrow$ `DELIVERING` $\rightarrow$ `COMPLETED`.

---

## 5. CẤU TRÚC THƯ MỤC MÃ NGUỒN

```text
flowers-shop/
├── src/main/java/com/finalproject/
│   ├── config/              # Cấu hình Spring Security, WebSocket, Cloudinary, MoMo, Async
│   ├── controller/          # Tầng điều khiển phân lớp: admin, manager, staff, customer, auth
│   ├── service/             # Nghiệp vụ: Đơn hàng, Kho chi nhánh, Ví, OTP Brevo, Voucher
│   ├── repository/          # Tầng truy xuất Spring Data JPA với Pessimistic Locks
│   ├── entity/              # Mô hình thực thể JPA chuẩn hóa 45 bảng
│   ├── dto/                 # Đối tượng truyền dữ liệu (Request/Response)
│   ├── security/            # Bộ lọc JWT Bearer, Session Security, Phân quyền chi nhánh
│   └── tools/               # Tiện ích nạp đánh giá mẫu chân thực, đối soát CSDL
├── src/main/resources/
│   ├── templates/           # Giao diện Thymeleaf: admin, manager, staff, customer, fragments
│   ├── static/              # Tài nguyên CSS Floral Theme, JS giỏ hàng/bộ lọc/chat, Icons
│   ├── db/migration/        # File Flyway Migration cho SQL Server và PostgreSQL
│   └── application.properties # Cấu hình môi trường mặc định
├── database/                # Toàn bộ script CSDL nộp kèm đồ án
│   ├── FlowersShop_PostgreSQL_Full.sql  # CSDL PostgreSQL 45 bảng & 939 dòng
│   ├── FlowersShop_SQLServer_Full.sql   # CSDL SQL Server 45 bảng & 939 dòng
│   └── HUONG_DAN_IMPORT_DATABASE.md     # Hướng dẫn chi tiết import CSDL 1-click
├── docs/                    # Tài liệu báo cáo hoàn chỉnh đồ án
│   ├── HUONG_DAN_XAY_DUNG_VA_KIEN_TRUC_UNG_DUNG.docx          # Tài liệu kiến trúc & hướng dẫn xây dựng
│   ├── DANH_SACH_PROMPT_AI_DO_AN.md                            # Hồ sơ 1.049 dòng Prompt AI đầy đủ
│   ├── HUONG_DAN_SETUP_DU_AN.md                                # Hướng dẫn cài đặt & khởi chạy dự án
│   ├── KICH_BAN_VIDEO_DEMO_DO_AN.md                            # Kịch bản trình diễn video demo
│   └── PHAN_QUYEN_CUA_HANG.md                                  # Đặc tả phân quyền chi nhánh
├── scripts/                 # Bộ script PowerShell hỗ trợ chạy nhanh và test
├── Dockerfile               # File đóng gói container Docker
├── render.yaml              # Cấu hình Blueprint tự động deploy lên Render
└── pom.xml                  # Cấu hình Maven dependencies Spring Boot 4.1.1
```

---

## 6. KẾT QUẢ KIỂM THỬ TỰ ĐỘNG (TESTING SUMMARY)

Dự án áp dụng quy trình kiểm thử nghiêm ngặt trên cả hai tầng Backend và Frontend:

- **Java Backend Test:** **880 ca kiểm thử tự động** (868 passed, 12 skipped, 0 failure).
  - Bao gồm kiểm thử luồng đồng thời `ConcurrencyRegressionTests`, kiểm thử phân quyền quản lý `ManagerShoppingSeparationTests`, kiểm thử vai trò nhân sự `StoreUseCaseAuditTests`, kiểm thử hiển thị review `ReviewDisplayTests`.
  - Lệnh chạy: `.\mvnw.cmd test`
- **JavaScript Frontend Test:** **185 / 185 ca kiểm thử Node.js native** đạt **100% pass rate** cho logic giỏ hàng, tính toán báo giá giao hàng và bộ lọc chi nhánh.
  - Lệnh chạy: `.\scripts\test-js.ps1`
- **Kiểm thử đa hệ quản trị (Multi-DB Compatibility):** Kiểm thử thực nghiệm thành công cả trên **PostgreSQL 16** (Aiven Cloud / Local) và **Microsoft SQL Server 2022**.

---

## 7. DANH MỤC HỒ SƠ & TÀI LIỆU NỘP KÈM

Để Hội đồng chấm thi và Giảng viên tiện tra cứu, toàn bộ tài liệu đã được tổ chức khoa học:

1. **Tài liệu Kiến trúc & Hướng dẫn Kỹ thuật:**
   - [docs/HUONG_DAN_XAY_DUNG_VA_KIEN_TRUC_UNG_DUNG.docx](docs/HUONG_DAN_XAY_DUNG_VA_KIEN_TRUC_UNG_DUNG.docx) — Tài liệu đặc tả kiến trúc 5 tầng, luồng đặt hàng, vòng đời đơn hàng, bảo mật kép và chat WebSocket.
   - [docs/HUONG_DAN_SETUP_DU_AN.md](docs/HUONG_DAN_SETUP_DU_AN.md) — Tài liệu hướng dẫn thiết lập môi trường, tài khoản và quy chuẩn vận hành.
2. **Hồ sơ minh chứng ứng dụng AI (Tiêu chí 2):**
   - [docs/DANH_SACH_PROMPT_AI_DO_AN.md](docs/DANH_SACH_PROMPT_AI_DO_AN.md) — Tài liệu 1.049 dòng tích hợp đầy đủ:
     - 6 Prompt sinh hình ảnh & sơ đồ kỹ thuật (`IMG-01` đến `IMG-06`).
     - 15 Prompt nguyên văn khôi phục từ nhật ký (`WEB-009` đến `WEB-085`).
     - 23 Prompt kỹ thuật tái dựng từ lịch sử commit (`P-01` đến `P-23`).
     - 49 Prompt chức năng khách hàng và kiểm thử (`BS-01` đến `BS-49`).
     - **Bộ 20 Prompt nền tảng thiết kế hệ thống và 96 yêu cầu phát triển thực nghiệm** (Lý Gia Hân).
     - Bảng đối chiếu sai sót của AI và ma trận Rubric chấm điểm.
3. **Cơ sở dữ liệu mẫu (SQL Scripts):**
   - [database/FlowersShop_PostgreSQL_Full.sql](database/FlowersShop_PostgreSQL_Full.sql) & [database/FlowersShop_SQLServer_Full.sql](database/FlowersShop_SQLServer_Full.sql) kèm [Hướng dẫn Import CSDL](database/HUONG_DAN_IMPORT_DATABASE.md).
4. **Kịch bản Thuyết trình & Trình diễn:**
   - [docs/KICH_BAN_VIDEO_DEMO_DO_AN.md](docs/KICH_BAN_VIDEO_DEMO_DO_AN.md) — Kịch bản từng bước quay video trình diễn chức năng và trả lời phản biện.

