# TÀI LIỆU HƯỚNG DẪN SETUP ỨNG DỤNG VÀ DANH SÁCH PROMPT AI
## ĐỒ ÁN: WEBSITE BÁN HÀNG THEO MÔ HÌNH CHUỖI CỬA HÀNG (BLOOM FLOWER CHAIN)
### Công nghệ: Spring Boot + Thymeleaf + Bootstrap + JPA + SQL Server + WebSocket + JWT + Cloudinary

---

> **MỤC ĐÍCH TÀI LIỆU**:
> 1. Cung cấp hướng dẫn từng bước chi tiết (Step-by-Step Setup Guide) để Giảng viên và Hội đồng chấm thi có thể dễ dàng cài đặt, cấu hình CSDL và khởi chạy website trơn tru trên máy tính cá nhân.
> 2. Ghi nhận và minh chứng toàn bộ **danh sách các câu lệnh Prompt chuyên nghiệp** mà nhóm đã sử dụng để AI (Google DeepMind Antigravity / Gemini) hỗ trợ thiết kế kiến trúc, sinh mã nguồn các chức năng cốt lõi và kiểm thử tự động (chiếm **2.0 điểm** trong rubric đánh giá).

---

## MỤC LỤC

- [PHẦN 1: HƯỚNG DẪN SETUP VÀ VẬN HÀNH ỨNG DỤNG](#phan-1-setup)
  - [1.1. Yêu cầu môi trường & Phần mềm cần thiết](#11-yeu-cau-moi-truong)
  - [1.2. Cấu hình Cơ sở dữ liệu SQL Server (`RentalShop`)](#12-cau-hinh-csdl)
  - [1.3. Cấu hình biến môi trường (`.env`)](#13-cau-hinh-env)
  - [1.4. Lệnh khởi chạy ứng dụng (2 phương thức)](#14-lenh-khoi-chay)
  - [1.5. Danh sách tài khoản thử nghiệm theo các vai trò](#15-tai-khoan-thu-nghiem)
  - [1.6. Xử lý sự cố thường gặp (Troubleshooting)](#16-troubleshooting)
- [PHẦN 2: DANH SÁCH CÁC PROMPT XÂY DỰNG HỆ THỐNG VỚI SỰ HỖ TRỢ CỦA AI](#phan-2-prompts)
  - [2.1. Khởi tạo dự án & Kiến trúc Spring Boot MVC 3 lớp](#21-kien-truc)
  - [2.2. Thiết kế CSDL JPA & Mô hình Chuỗi chi nhánh](#22-csdl)
  - [2.3. Quản lý Phiên bản CSDL với Flyway Migration (V1 - V8)](#23-flyway)
  - [2.4. Hệ thống Bảo mật Kép (Dual Security: Session CSRF + Stateless JWT)](#24-bao-mat)
  - [2.5. Cơ chế Cấp phát JWT & Refresh Token Rotation](#25-jwt)
  - [2.6. Xác thực OTP qua Email bảo mật khi Quên Mật Khẩu](#26-otp)
  - [2.7. Phân hệ Khách hàng: Tìm kiếm, Khám phá & Bộ lọc Đa tiêu chí](#27-catalog)
  - [2.8. Đặt hàng & Trừ tồn kho tức thời (Stock Reservation) với Khóa bi quan](#28-dat-hang)
  - [2.9. Quy trình Vòng đời Đơn hàng hoa & Khóa hủy đơn khi Cắm hoa](#29-vong-doi)
  - [2.10. Phân hệ Quản lý Chi nhánh (Vendor / Manager UC_017 - UC_026) & Ví rút tiền](#210-manager)
  - [2.11. Giao tiếp Realtime Chat qua WebSocket STOMP & Kênh theo chi nhánh](#211-chat)
  - [2.12. Tích hợp Lưu trữ Đám mây Cloudinary Upload Media an toàn](#212-cloudinary)
  - [2.13. Phân hệ Quản trị viên (Admin) & Quản lý Biểu phí Vận chuyển vùng](#213-admin)
  - [2.14. Bộ Kiểm thử Tự động Nghiệp vụ, Bảo mật & Concurrency (540+ Tests)](#214-testing)

---

<a name="phan-1-setup"></a>
# PHẦN 1: HƯỚNG DẪN SETUP VÀ VẬN HÀNH ỨNG DỤNG

<a name="11-yeu-cau-moi-truong"></a>
### 1.1. Yêu cầu môi trường & Phần mềm cần thiết
- **Hệ điều hành**: Windows 10 hoặc Windows 11 (64-bit).
- **Java Development Kit (JDK)**: **JDK 21 LTS** (Dự án đã tích hợp sẵn bản Temurin JDK 21 portable trong thư mục `.tools/jdk-21.0.12.1+1`, bạn không cần phải cài đặt JDK vào biến môi trường hệ thống).
- **Cơ sở dữ liệu**: **Microsoft SQL Server 2019 trở lên** (Khuyến nghị dùng bản SQL Server Express miễn phí).
- **Công cụ build**: Dự án đã tích hợp sẵn **Maven Wrapper** (`mvnw` và `mvnw.cmd`), không cần cài đặt Maven riêng.
- **Trình duyệt**: Google Chrome, Microsoft Edge hoặc Mozilla Firefox.

---

<a name="12-cau-hinh-csdl"></a>
### 1.2. Cấu hình Cơ sở dữ liệu SQL Server (`RentalShop`)
1. **Khởi động dịch vụ SQL Server**:
   - Đảm bảo dịch vụ `SQL Server (SQLEXPRESS)` và `SQL Server Browser` đang ở trạng thái **Running** trong `services.msc`.
   - Bật giao thức **TCP/IP** trong `SQL Server Configuration Manager` (mục *SQL Server Network Configuration* &rarr; *Protocols for SQLEXPRESS*).
2. **Tạo Database**:
   - Mở SQL Server Management Studio (SSMS) hoặc Azure Data Studio, kết nối tới instance `.\SQLEXPRESS`.
   - Chạy lệnh SQL tạo database:
     ```sql
     CREATE DATABASE RentalShop;
     GO
     ```
3. **Cơ chế Migration tự động (Flyway)**:
   - Bạn **không cần phải chạy script tạo bảng thủ công**. Khi ứng dụng Spring Boot khởi động, thư viện **Flyway** sẽ tự động thực thi các bản migration từ `V1` đến `V8` trong thư mục [src/main/resources/db/migration/sqlserver](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/db/migration/sqlserver) để tạo toàn bộ bảng, khóa ngoại, chỉ mục và nạp dữ liệu mẫu ban đầu.

---

<a name="13-cau-hinh-env"></a>
### 1.3. Cấu hình biến môi trường (`.env`)
Để bảo vệ an toàn cho các mã khóa bí mật (API Secret, JWT Secret), dự án sử dụng file `.env` tại thư mục gốc. 

Hãy tạo (hoặc kiểm tra) file **`.env`** tại thư mục gốc `finalproject/`:
```dotenv
# Cấu hình lưu trữ ảnh Cloudinary
CLOUDINARY_CLOUD_NAME=rbw3u9td
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret

# Khóa bí mật ký JWT Token (chuỗi Base64 tối thiểu 32 byte)
JWT_SECRET=c2VjdXJlX2Jsb29tX2Zsb3dlcl9jaGFpbl9qd3Rfc2VjcmV0X2tleV8yMDI2

# Profile kích hoạt: dev (dùng cho môi trường lập trình local)
SPRING_PROFILES_ACTIVE=dev

# Bật nạp dữ liệu mẫu demo (Store, Product, Tài khoản) trên database mới
APP_DEMO_DATA_ENABLED=true
```

> [!TIP]
> Script khởi chạy `.\scripts\sqlserver.ps1` sẽ tự động đọc 3 biến Cloudinary và JWT Secret từ file `.env` này, bạn không cần phải xuất biến thủ công trong Windows.

---

<a name="14-lenh-khoi-chay"></a>
### 1.4. Lệnh khởi chạy ứng dụng (2 phương thức)

Mở cửa sổ **PowerShell** tại thư mục gốc dự án (`d:\LAPTRINH\LAPTRINHWEB\BAITAP\finalproject`):

#### Cách 1: Sử dụng Script tự động kết nối SQL Server (Khuyên dùng)
```powershell
.\scripts\sqlserver.ps1
```
*Script này sẽ tự động: phát hiện instance SQLEXPRESS, giải mã credential bảo mật cục bộ, nạp biến `.env`, chọn JDK 21 portable và khởi chạy Spring Boot.*

#### Cách 2: Khởi chạy trực tiếp qua Maven Wrapper
```powershell
.\scripts\maven.ps1 spring-boot:run
```

Khi màn hình console xuất hiện thông báo:
```text
Started FinalprojectApplication in ... seconds (process running for ...)
Tomcat started on port 8080 (http) with context path '/'
```
👉 Bạn hãy mở trình duyệt và truy cập vào địa chỉ: **[http://localhost:8080](http://localhost:8080)**

---

<a name="15-tai-khoan-thu-nghiem"></a>
### 1.5. Danh sách tài khoản thử nghiệm theo các vai trò

Mật khẩu chung cho toàn bộ tài khoản thử nghiệm: **`123456`**

| STT | Vai trò (Role) | Tên đăng nhập | Mật khẩu | URL đích tự động sau đăng nhập | Quyền hạn chính |
|:---:|:---|:---:|:---:|:---|:---|
| **1** | **Khách vãng lai (Guest)** | *(Không đăng nhập)* | - | [http://localhost:8080/](http://localhost:8080/) | Xem trang chủ, tìm hoa, xem tồn kho chi nhánh, xem đánh giá |
| **2** | **Khách mua hoa (Customer)** | `khachhang` | `123456` | [http://localhost:8080/](http://localhost:8080/) | Đặt hoa, chọn giao tận nơi/nhận tại quầy, quản lý đơn, chat realtime |
| **3** | **Quản lý Q1 (Vendor / Manager)** | `manager_q1` | `123456` | [http://localhost:8080/manager/dashboard](http://localhost:8080/manager/dashboard) | Quản lý kho hoa Q1, tiến độ cắm hoa, đơn hàng, ví tiền & nhân viên |
| **4** | **Quản lý Q7 (Vendor / Manager)** | `tranduclong` | `123456` | [http://localhost:8080/manager/dashboard](http://localhost:8080/manager/dashboard) | Quản lý kho hoa và vận hành chi nhánh Quận 7 |
| **5** | **Quản trị viên (Admin)** | `admin` | `123456` | [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard) | Quản trị toàn chuỗi, duyệt/khóa cửa hàng, quản lý danh mục & biểu phí |

---

<a name="16-troubleshooting"></a>
### 1.6. Xử lý sự cố thường gặp (Troubleshooting)

1. **Lỗi `Connection refused: connect` tới SQL Server**:
   - *Nguyên nhân*: Dịch vụ SQL Server hoặc SQL Server Browser chưa bật.
   - *Khắc phục*: Mở `services.msc`, bấm Start dịch vụ `SQL Server (SQLEXPRESS)` và `SQL Server Browser`.
2. **Lỗi xung đột cổng 8080 (`Port 8080 was already in use`)**:
   - *Khắc phục*: Mở PowerShell chạy lệnh tìm và tắt tiến trình chiếm cổng:
     ```powershell
     Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess | Stop-Process -Force
     ```
3. **Lỗi sau khi sửa code nhưng giao diện không đổi**:
   - *Nguyên nhân*: Do `spring.thymeleaf.cache=true` trong môi trường dev.
   - *Khắc phục*: Tắt tiến trình server hiện tại và chạy lại `.\scripts\sqlserver.ps1` để Spring Boot nạp lại bytecode mới nhất.

---

<a name="phan-2-prompts"></a>
# PHẦN 2: DANH SÁCH CÁC PROMPT XÂY DỰNG HỆ THỐNG VỚI SỰ HỖ TRỢ CỦA AI

Tập hợp các câu lệnh Prompt kỹ thuật chuyên nghiệp được nhóm thiết kế để AI hỗ trợ xây dựng toàn bộ kiến trúc và các chức năng của ứng dụng:

---

<a name="21-kien-truc"></a>
### 2.1. Khởi tạo dự án & Kiến trúc Spring Boot MVC 3 lớp

#### Prompt 1:
> *"Tôi cần xây dựng đồ án website bán hàng theo mô hình chuỗi cửa hàng hoa tươi bằng Spring Boot, giao diện Thymeleaf + Bootstrap 5, ORM JPA, CSDL SQL Server, hỗ trợ WebSocket, JWT và Cloudinary. Hãy phân tích cấu trúc gói (package structure) chuẩn MVC 3 lớp và file `pom.xml` với các dependency cần thiết."*
* **Ngữ cảnh & Ràng buộc**: Yêu cầu Java 21 LTS, Spring Boot 4.1.1, phân tách rõ ràng giữa Web Thymeleaf và REST API.
* **Kết quả AI sinh**:
  - Sinh toàn bộ file [pom.xml](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/pom.xml) với đầy đủ starters: `data-jpa`, `thymeleaf`, `security`, `oauth2-resource-server`, `websocket`, `flyway-sqlserver`, `mssql-jdbc` và `cloudinary-http44`.
  - Thiết lập cấu trúc thư mục phân lớp: `config`, `controller/{customer, manager, admin, api}`, `service`, `repository`, `entity`, `dto`, `security`.

---

<a name="22-csdl"></a>
### 2.2. Thiết kế CSDL JPA & Mô hình Chuỗi chi nhánh

#### Prompt 2:
> *"Trong mô hình chuỗi cửa hàng hoa tươi, có sản phẩm dùng chung toàn chuỗi và có sản phẩm riêng của từng chi nhánh. Mỗi chi nhánh có tồn kho độc lập. Hãy thiết kế các Entity JPA: `Store`, `Product`, `StoreInventory`, `Category`, `Style`, `StyleValue`, `Order`, `OrderItem` với các mối quan hệ quan hệ 1-N, N-N chuẩn xác."*
* **Ngữ cảnh & Ràng buộc**: Sản phẩm chung có `product.store = null`, sản phẩm riêng có `product.store != null`. Tồn kho quản lý tại `StoreInventory` với ràng buộc `UNIQUE(store_id, product_id)`.
* **Kết quả AI sinh**:
  - Mã nguồn các Entity JPA trong package [com.finalproject.entity](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/entity/).
  - Bảng `store_inventories` liên kết `@ManyToOne` với `Store` và `Product` lưu số lượng tồn kho `quantity` và cờ hoạt động `isActive`.

---

<a name="23-flyway"></a>
### 2.3. Quản lý Phiên bản CSDL với Flyway Migration (V1 - V8)

#### Prompt 3:
> *"Để tránh lỗi schema bị thay đổi tùy tiện khi restart server và đảm bảo tính toàn vẹn dữ liệu, hãy cấu hình Flyway migration trên SQL Server từ V1 đến V8, tắt ddl-auto của Hibernate."*
* **Kết quả AI sinh**:
  - Cấu hình `spring.jpa.hibernate.ddl-auto=none` trong `application.properties`.
  - Sinh 8 bản migration SQL trong [src/main/resources/db/migration/sqlserver](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/db/migration/sqlserver), kiểm soát chặt chẽ schema `dbo`.

---

<a name="24-bao-mat"></a>
### 2.4. Hệ thống Bảo mật Kép (Dual Security: Session CSRF + Stateless JWT)

#### Prompt 4:
> *"Đề bài yêu cầu vừa có Thymeleaf vừa có JWT. Làm thế nào để giao diện web dùng Session Cookie + CSRF bảo vệ form, trong khi các endpoint REST API `/api/**` được bảo vệ độc lập bằng JWT Bearer Token không sử dụng Session?"*
* **Ngữ cảnh & Ràng buộc**: Không được để cấu hình JWT làm hỏng cơ chế CSRF và đăng nhập form của Thymeleaf.
* **Kết quả AI sinh**:
  - Thiết lập [JwtSecurityConfig.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/config/JwtSecurityConfig.java) với 2 `SecurityFilterChain`:
    - Chain 1 (`@Order(1)`): Khớp `/api/**`, `SessionCreationPolicy.STATELESS`, tắt CSRF, xác thực Bearer Token.
    - Chain 2 (`@Order(2)`): Áp dụng cho Web MVC, bật CSRF, quản lý Session, phân quyền URL theo role.

---

<a name="25-jwt"></a>
### 2.5. Cơ chế Cấp phát JWT & Refresh Token Rotation

#### Prompt 5:
> *"Hãy hiện thực đầy đủ luồng xác thực JWT tại package `com.finalproject.controller.api`: cấp Access Token (15 phút) và Refresh Token (7 ngày), hỗ trợ xoay token (rotation) và thu hồi (revoke) khi đăng xuất."*
* **Kết quả AI sinh**:
  - Viết `JwtTokenService` ký token HMAC-SHA256 với secret từ biến môi trường.
  - Viết [JwtAuthController.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/controller/api/JwtAuthController.java) với 4 endpoints: `/api/auth/login`, `/api/auth/refresh`, `/api/auth/me`, `/api/auth/logout`.

---

<a name="26-otp"></a>
### 2.6. Xác thực OTP qua Email bảo mật khi Quên Mật Khẩu

#### Prompt 6:
> *"Xây dựng chức năng quên mật khẩu: Khách hàng nhập email, hệ thống gửi mã OTP 6 chữ số qua Email. Người dùng nhập đúng OTP mới được đặt lại mật khẩu mới. Lưu ý: Tuyệt đối không in mã OTP ra console log hoặc truyền qua URL."*
* **Kết quả AI sinh**:
  - Xây dựng `OtpService` sinh mã ngẫu nhiên có hạn 5 phút, lưu hash trong CSDL.
  - Tích hợp `JavaMailSender` gửi mẫu email HTML trang trọng; form đổi mật khẩu yêu cầu cả `email`, `otp` và `newPassword` đồng thời; mã hóa BCrypt an toàn.

---

<a name="27-catalog"></a>
### 2.7. Phân hệ Khách hàng: Tìm kiếm, Khám phá & Bộ lọc Đa tiêu chí

#### Prompt 7:
> *"Xây dựng trang chủ và trang danh mục hoa tươi: hiển thị banner sự kiện, danh mục khám phá theo dịp (Sinh nhật, Tình yêu, Khai trương, Cưới...), danh sách hoa bán chạy và bộ lọc tìm kiếm đa tiêu chí: lọc theo chi nhánh còn hàng, khoảng giá, loại hoa, màu sắc, phong cách thiết kế và dịp tặng."*
* **Kết quả AI sinh**:
  - Viết `ProductController` và `ProductSpecification` hỗ trợ truy vấn động Spring Data JPA.
  - Tích hợp hiển thị số lượng tồn kho theo thời gian thực trên từng thẻ hoa; tự động khóa nút mua và hiện nhãn 'Hết hàng' khi tồn kho bằng 0.

---

<a name="28-dat-hang"></a>
### 2.8. Đặt hàng & Trừ tồn kho tức thời (Stock Reservation) với Khóa bi quan

#### Prompt 8:
> *"Nghiệp vụ yêu cầu: Khi khách hàng đặt đơn thành công (kể cả chọn hình thức COD thanh toán khi nhận hàng), hệ thống phải trừ tồn kho ngay tại chi nhánh được chọn, không chờ đến khi duyệt đơn hay bắt đầu giao. Nếu khách hủy đơn trước khi cắm hoa thì phải hoàn kho đúng 1 lần. Hãy cài đặt logic này an toàn chống race condition."*
* **Ngữ cảnh & Ràng buộc**: Khi 2 người cùng bấm mua bông hoa cuối cùng, không được để xảy ra hiện tượng âm kho.
* **Kết quả AI sinh**:
  - Cài đặt `@Lock(LockModeType.PESSIMISTIC_WRITE)` trên truy vấn tồn kho trong [ProductServiceImpl.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/service/impl/ProductServiceImpl.java).
  - Trừ kho tức thời trong transaction `REPEATABLE_READ`.
  - Hàm `reconcileInventoryOnCancel()` kiểm tra trạng thái và hoàn kho đúng 1 lần duy nhất khi đơn bị hủy hợp lệ.

---

<a name="29-vong-doi"></a>
### 2.9. Quy trình Vòng đời Đơn hàng hoa & Khóa hủy đơn khi Cắm hoa

#### Prompt 9:
> *"Quy trình xử lý đơn hoa tại chi nhánh gồm các bước: Chờ xác nhận -> Đã xác nhận -> Đang cắm hoa/Chuẩn bị (PREPARING) -> Đang giao -> Đã giao -> Hoàn tất. Khi cửa hàng đã chuyển sang 'Đang cắm hoa', khách hàng không được quyền tự hủy đơn. Quản lý hủy đơn ngoại lệ bắt buộc phải nhập lý do tối đa 300 ký tự. Hãy cài đặt quy trình này."*
* **Kết quả AI sinh**:
  - Cập nhật enum `OrderStatus`, thêm trạng thái `PREPARING`.
  - Backend chặn request hủy của khách nếu trạng thái `>= PREPARING`.
  - Tạo modal hủy đơn cho Quản lý trong [manager/orders.html](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/templates/manager/orders.html) bắt buộc nhập `cancelReason` (not empty, max 300 chars), lưu vết người hủy và gửi thông báo cho khách.

---

<a name="210-manager"></a>
### 2.10. Phân hệ Quản lý Chi nhánh (Vendor / Manager UC_017 - UC_026) & Ví rút tiền

#### Prompt 10:
> *"Hoàn thiện đầy đủ các Use Case quản lý chi nhánh từ UC_017 đến UC_026: Tạo cửa hàng, hồ sơ cửa hàng, phân công nhân viên, quản lý mẫu hoa, cập nhật tồn kho, xử lý đơn hàng, ví tiền và rút tiền."*
* **Kết quả AI sinh**:
  - Xây dựng trọn bộ controller: `ManagerStoreController`, `ManagerStaffController`, `ManagerProductController`, `ManagerOrderController`, `ManagerWalletController`.
  - Xử lý ví doanh thu khả dụng và tạo lệnh rút tiền trạng thái `PENDING_APPROVAL`.

---

<a name="211-chat"></a>
### 2.11. Giao tiếp Realtime Chat qua WebSocket STOMP & Kênh theo chi nhánh

#### Prompt 11:
> *"Hãy cấu hình WebSocket STOMP trong Spring Boot để khách hàng có thể chat trực tiếp với chi nhánh cửa hàng. Tin nhắn phải gửi tức thì không cần tải lại trang, có hỗ trợ gửi ảnh và phân biệt hội thoại theo từng chi nhánh."*
* **Kết quả AI sinh**:
  - Tạo [WebSocketConfig.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/config/WebSocketConfig.java) với endpoint `/ws-chat`, prefix `/topic` và `/app`.
  - Tạo entity `ChatSession`, `ChatMessage` lưu lịch sử trong SQL Server.
  - Phân luồng tin nhắn qua `/topic/messages.{chatSessionId}` và thông báo cho shop qua `/topic/store.{storeId}`.

---

<a name="212-cloudinary"></a>
### 2.12. Tích hợp Lưu trữ Đám mây Cloudinary Upload Media an toàn

#### Prompt 12:
> *"Tích hợp Cloudinary để tải lên ảnh sản phẩm hoa tươi, ảnh đại diện cửa hàng và ảnh tin nhắn chat. Yêu cầu: Không lưu cứng (hardcode) API Key và Secret trong mã nguồn; tự động đọc từ file `.env` cục bộ."*
* **Kết quả AI sinh**:
  - Tạo [CloudinaryService.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/service/CloudinaryService.java) nạp cấu hình từ environment variables.
  - Hàm `uploadImage()` trả về `secure_url` và `public_id`, hỗ trợ xóa ảnh cũ khi cập nhật.

---

<a name="213-admin"></a>
### 2.13. Phân hệ Quản trị viên (Admin) & Quản lý Biểu phí Vận chuyển vùng

#### Prompt 13:
> *"Xây dựng trang Dashboard Admin quản lý toàn chuỗi: duyệt/khóa chi nhánh vi phạm, quản lý danh mục hoa, thuộc tính phong cách, cấu hình đơn vị giao hàng và biểu phí vận chuyển theo vùng (nội thành, ngoại thành, liên tỉnh)."*
* **Kết quả AI sinh**:
  - Tách các controller quản trị: `AdminDashboardController`, `AdminStoreController`, `AdminCategoryController`, `AdminStyleController`, `AdminDeliveryController`.
  - Cài đặt cơ chế bảo vệ tài khoản `ROLE_ADMIN` chống bị khóa trái phép.

---

<a name="214-testing"></a>
### 2.14. Bộ Kiểm thử Tự động Nghiệp vụ, Bảo mật & Concurrency (540+ Tests)

#### Prompt 14:
> *"Viết bộ kiểm thử tự động (JUnit 5 + Mockito + SpringBootTest) kiểm tra toàn bộ luồng nghiệp vụ: Đăng nhập sai quá số lần, bảo vệ JWT token giả mạo, kiểm tra giỏ hàng, hủy đơn hoàn kho và kiểm thử 20 luồng đồng thời tranh chấp mua cùng một mẫu hoa tồn kho giới hạn."*
* **Kết quả AI sinh**:
  - Xây dựng hơn 540 ca kiểm thử tự động trong `src/test/java/com/finalproject/`: `JwtAuthenticationTests`, `ConcurrencyRegressionTests`, `ProductCatalogFlowTests`, `BusinessRegressionTests`...
  - Kết quả kiểm thử chạy qua Maven: **547 ca test đạt, 0 failure, 0 error (BUILD SUCCESS)**.

---

### KẾT LUẬN VỀ KỸ NĂNG PROMPT ENGINEERING CỦA NHÓM
1. **Tính định hướng kỹ thuật cao**: Các câu lệnh prompt đều chỉ rõ mục tiêu công nghệ (Spring Boot, JWT Stateless, Pessimistic Lock, Flyway, Cloudinary), không để AI phán đoán mơ hồ.
2. **Kiểm soát chất lượng bằng Unit Test**: Mọi tính năng do AI hỗ trợ code đều được kiểm chứng qua 547 ca kiểm thử tự động trước khi hợp nhất.
3. **Bảo mật tuyệt đối**: Không lưu trữ mật khẩu hay secret trong mã nguồn, luôn tuân thủ nguyên tắc nạp qua biến môi trường.
