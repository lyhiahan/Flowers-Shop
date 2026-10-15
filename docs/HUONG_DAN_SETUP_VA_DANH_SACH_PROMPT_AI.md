# TÀI LIỆU HƯỚNG DẪN SETUP ỨNG DỤNG VÀ DANH SÁCH PROMPT AI
## ĐỒ ÁN: WEBSITE BÁN HÀNG THEO MÔ HÌNH CHUỖI CỬA HÀNG (BLOOM FLOWER CHAIN)
### Công nghệ: Spring Boot + Thymeleaf + Bootstrap + JPA + SQL Server + WebSocket + JWT + Cloudinary

---

> **MỤC ĐÍCH TÀI LIỆU**:
> 1. Cung cấp hướng dẫn từng bước chi tiết (Step-by-Step Setup Guide) để Giảng viên và Hội đồng chấm thi có thể dễ dàng cài đặt, cấu hình CSDL và khởi chạy website trơn tru trên máy tính cá nhân.
> 2. Ghi nhận và minh chứng toàn bộ **lịch sử các câu lệnh Prompt** mà nhóm đã sử dụng để AI (Google DeepMind Antigravity / Gemini) hỗ trợ sinh mã nguồn, sinh sơ đồ kiến trúc, tối ưu logic nghiệp vụ và gỡ lỗi (chiếm **2.0 điểm** trong rubric đánh giá).

---

## MỤC LỤC

- [PHẦN 1: HƯỚNG DẪN SETUP VÀ VẬN HÀNH ỨNG DỤNG](#phan-1-setup)
  - [1.1. Yêu cầu môi trường & Phần mềm cần thiết](#11-yeu-cau-moi-truong)
  - [1.2. Cấu hình Cơ sở dữ liệu SQL Server (`RentalShop`)](#12-cau-hinh-csdl)
  - [1.3. Cấu hình biến môi trường (`.env`)](#13-cau-hinh-env)
  - [1.4. Lệnh khởi chạy ứng dụng (2 phương thức)](#14-lenh-khoi-chay)
  - [1.5. Danh sách tài khoản thử nghiệm theo các vai trò](#15-tai-khoan-thu-nghiem)
  - [1.6. Xử lý sự cố thường gặp (Troubleshooting)](#16-troubleshooting)
- [PHẦN 2: DANH SÁCH TOÀN BỘ PROMPT NHÓM ĐÃ SỬ DỤNG VỚI AI](#phan-2-prompts)
  - [Danh sách chi tiết từ Prompt 1 đến Prompt 20](#phan-2-prompts)

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
# PHẦN 2: DANH SÁCH TOÀN BỘ PROMPT NHÓM ĐÃ SỬ DỤNG VỚI AI

Dưới đây là tập hợp đầy đủ các câu lệnh Prompt (kèm ngữ cảnh và kết quả hỗ trợ của AI) trong suốt quá trình phát triển dự án:

---

### Prompt 1:
> *"Tôi cần xây dựng đồ án website bán hàng theo mô hình chuỗi cửa hàng hoa tươi bằng Spring Boot, giao diện Thymeleaf + Bootstrap 5, ORM JPA, CSDL SQL Server, hỗ trợ WebSocket, JWT và Cloudinary. Hãy phân tích cấu trúc gói (package structure) chuẩn MVC 3 lớp và file `pom.xml` với các dependency cần thiết."*
* **Ngữ cảnh & Ràng buộc**: Yêu cầu Java 21 LTS, Spring Boot 4.1.1, phân tách rõ ràng giữa Web Thymeleaf và REST API.
* **Kết quả AI sinh**:
  - Sinh toàn bộ file [pom.xml](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/pom.xml) với đầy đủ starters: `data-jpa`, `thymeleaf`, `security`, `oauth2-resource-server`, `websocket`, `flyway-sqlserver`, `mssql-jdbc` và `cloudinary-http44`.
  - Thiết lập cấu trúc thư mục phân lớp: `config`, `controller/{customer, manager, admin, api}`, `service`, `repository`, `entity`, `dto`, `security`.

---

### Prompt 2:
> *"Trong mô hình chuỗi cửa hàng hoa tươi, có sản phẩm dùng chung toàn chuỗi và có sản phẩm riêng của từng chi nhánh. Mỗi chi nhánh có tồn kho độc lập. Hãy thiết kế các Entity JPA: `Store`, `Product`, `StoreInventory`, `Category`, `Style`, `StyleValue`, `Order`, `OrderItem` với các mối quan hệ quan hệ 1-N, N-N chuẩn xác."*
* **Ngữ cảnh & Ràng buộc**: Sản phẩm chung có `product.store = null`, sản phẩm riêng có `product.store != null`. Tồn kho quản lý tại `StoreInventory` với ràng buộc `UNIQUE(store_id, product_id)`.
* **Kết quả AI sinh**:
  - Mã nguồn các Entity JPA trong package [com.finalproject.entity](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/entity/).
  - Bảng `store_inventories` liên kết `@ManyToOne` với `Store` và `Product` lưu số lượng tồn kho `quantity` và cờ hoạt động `isActive`.

### Prompt 3:
> *"Để tránh lỗi schema bị thay đổi tùy tiện khi restart server và đảm bảo tính toàn vẹn dữ liệu, hãy cấu hình Flyway migration trên SQL Server từ V1 đến V8, tắt ddl-auto của Hibernate."*
* **Kết quả AI sinh**:
  - Cấu hình `spring.jpa.hibernate.ddl-auto=none` trong `application.properties`.
  - Sinh 8 bản migration SQL trong [src/main/resources/db/migration/sqlserver](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/db/migration/sqlserver), kiểm soát chặt chẽ schema `dbo`.

---

### Prompt 4:
> *"Đề bài yêu cầu vừa có Thymeleaf vừa có JWT. Làm thế nào để giao diện web dùng Session Cookie + CSRF bảo vệ form, trong khi các endpoint REST API `/api/**` được bảo vệ độc lập bằng JWT Bearer Token không sử dụng Session?"*
* **Ngữ cảnh & Ràng buộc**: Không được để cấu hình JWT làm hỏng cơ chế CSRF và đăng nhập form của Thymeleaf.
* **Kết quả AI sinh**:
  - Thiết lập [JwtSecurityConfig.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/config/JwtSecurityConfig.java) với 2 `SecurityFilterChain`:
    - Chain 1 (`@Order(1)`): Khớp `/api/**`, `SessionCreationPolicy.STATELESS`, tắt CSRF, xác thực Bearer Token.
    - Chain 2 (`@Order(2)`): Áp dụng cho Web MVC, bật CSRF, quản lý Session, phân quyền URL theo role.

### Prompt 5:
> *"Hãy hiện thực đầy đủ luồng xác thực JWT tại package `com.finalproject.controller.api`: cấp Access Token (15 phút) và Refresh Token (7 ngày), hỗ trợ xoay token (rotation) và thu hồi (revoke) khi đăng xuất."*
* **Kết quả AI sinh**:
  - Viết `JwtTokenService` ký token HMAC-SHA256 với secret từ biến môi trường.
  - Viết [JwtAuthController.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/controller/api/JwtAuthController.java) với 4 endpoints: `/api/auth/login`, `/api/auth/refresh`, `/api/auth/me`, `/api/auth/logout`.

---

### Prompt 6:
> *"Xây dựng chức năng quên mật khẩu: Khách hàng nhập email, hệ thống gửi mã OTP 6 chữ số qua Email. Người dùng nhập đúng OTP mới được đặt lại mật khẩu mới. Lưu ý: Tuyệt đối không in mã OTP ra console log hoặc truyền qua URL."*
* **Kết quả AI sinh**:
  - Xây dựng `OtpService` sinh mã ngẫu nhiên có hạn 5 phút, lưu hash trong CSDL.
  - Tích hợp `JavaMailSender` gửi mẫu email HTML trang trọng; form đổi mật khẩu yêu cầu cả `email`, `otp` và `newPassword` đồng thời; mã hóa BCrypt an toàn.

---

### Prompt 7:
> *"Nghiệp vụ yêu cầu: Khi khách hàng đặt đơn thành công (kể cả chọn hình thức COD thanh toán khi nhận hàng), hệ thống phải trừ tồn kho ngay tại chi nhánh được chọn, không chờ đến khi duyệt đơn hay bắt đầu giao. Nếu khách hủy đơn trước khi cắm hoa thì phải hoàn kho đúng 1 lần. Hãy cài đặt logic này an toàn chống race condition."*
* **Ngữ cảnh & Ràng buộc**: Khi 2 người cùng bấm mua bông hoa cuối cùng, không được để xảy ra hiện tượng âm kho.
* **Kết quả AI sinh**:
  - Cài đặt `@Lock(LockModeType.PESSIMISTIC_WRITE)` trên truy vấn tồn kho trong [ProductServiceImpl.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/service/impl/ProductServiceImpl.java).
  - Trừ kho tức thời trong transaction `REPEATABLE_READ`.
  - Hàm `reconcileInventoryOnCancel()` kiểm tra trạng thái và hoàn kho đúng 1 lần duy nhất khi đơn bị hủy hợp lệ.

---

### Prompt 8:
> *"Quy trình xử lý đơn hoa tại chi nhánh gồm các bước: Chờ xác nhận -> Đã xác nhận -> Đang cắm hoa/Chuẩn bị (PREPARING) -> Đang giao -> Đã giao -> Hoàn tất. Khi cửa hàng đã chuyển sang 'Đang cắm hoa', khách hàng không được quyền tự hủy đơn. Quản lý hủy đơn ngoại lệ bắt buộc phải nhập lý do tối đa 300 ký tự. Hãy cài đặt quy trình này."*
* **Kết quả AI sinh**:
  - Cập nhật enum `OrderStatus`, thêm trạng thái `PREPARING`.
  - Backend chặn request hủy của khách nếu trạng thái `>= PREPARING`.
  - Tạo modal hủy đơn cho Quản lý trong [manager/orders.html](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/templates/manager/orders.html) bắt buộc nhập `cancelReason` (not empty, max 300 chars), lưu vết người hủy và gửi thông báo cho khách.

---

### Prompt 9:
> *"Hoàn thiện đầy đủ các Use Case quản lý chi nhánh từ UC_017 đến UC_026: Tạo cửa hàng, hồ sơ cửa hàng, phân công nhân viên, quản lý mẫu hoa, cập nhật tồn kho, xử lý đơn hàng, ví tiền và rút tiền."*
* **Kết quả AI sinh**:
  - Xây dựng trọn bộ controller: `ManagerStoreController`, `ManagerStaffController`, `ManagerProductController`, `ManagerOrderController`, `ManagerWalletController`.
  - Xử lý ví doanh thu khả dụng và tạo lệnh rút tiền trạng thái `PENDING_APPROVAL`.

---

### Prompt 10:
> *"Hãy cấu hình WebSocket STOMP trong Spring Boot để khách hàng có thể chat trực tiếp với chi nhánh cửa hàng. Tin nhắn phải gửi tức thì không cần tải lại trang, có hỗ trợ gửi ảnh và phân biệt hội thoại theo từng chi nhánh."*
* **Kết quả AI sinh**:
  - Tạo [WebSocketConfig.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/config/WebSocketConfig.java) với endpoint `/ws-chat`, prefix `/topic` và `/app`.
  - Tạo entity `ChatSession`, `ChatMessage` lưu lịch sử trong SQL Server.
  - Phân luồng tin nhắn qua `/topic/messages.{chatSessionId}` và thông báo cho shop qua `/topic/store.{storeId}`.

---

### Prompt 11:
> *"Tích hợp Cloudinary để tải lên ảnh sản phẩm hoa tươi, ảnh đại diện cửa hàng và ảnh tin nhắn chat. Yêu cầu: Không lưu cứng (hardcode) API Key và Secret trong mã nguồn; tự động đọc từ file `.env` cục bộ."*
* **Kết quả AI sinh**:
  - Tạo [CloudinaryService.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/service/CloudinaryService.java) nạp cấu hình từ environment variables.
  - Hàm `uploadImage()` trả về `secure_url` và `public_id`, hỗ trợ xóa ảnh cũ khi cập nhật.

---

### Prompt 12:
> *"Cho các KPIs này nằm cùng 1 hàng."*
* **Kết quả AI sinh**:
  - Chỉnh sửa CSS Flexbox và Grid trên [manager/dashboard.html](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/templates/manager/dashboard.html) và [admin/dashboard.html](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/templates/admin/dashboard.html).
  - Đưa 4 thẻ chỉ số: *Đơn hàng mới*, *Doanh thu trong kỳ*, *Mẫu hoa kinh doanh* và *Tồn kho còn lại* nằm cân đối trên 1 hàng ngang màn hình Desktop, tự co giãn trên mobile.

### Prompt 13:
> *"Giữ nguyên thanh sidebar đã đồng bộ và khôi phục màu nền kem hồng ấm #FFF0F3 gốc cho các trang /manager/."*
* **Kết quả AI sinh**:
  - Tinh chỉnh CSS hệ thống, giữ layout sidebar xanh rừng tự nhiên và nền nội dung màu kem hồng ấm `#FFF0F3` đồng nhất thương hiệu hoa tươi.

---

### Prompt 14:
> *"Đặt lại tên toàn bộ sản phẩm của cửa hàng cho đúng và dễ hiểu không đặt tên trừ tượng như 'Bó Hoa Hướng Dương Ánh Dương Rạng Rỡ'."*
* **Kết quả AI sinh**:
  - Viết script cập nhật toàn bộ tên hoa trong CSDL `RentalShop` sang các tên cụ thể, thực tế: *"Bó hoa hướng dương tươi"*, *"Bó hoa hồng đỏ Đà Lạt"*, *"Giỏ hoa cẩm tú cầu xanh"*.

### Prompt 15:
> *"Lỗi khi thêm sản phẩm, tôi muốn nó có nút bấm sổ xuống để có thể lựa chọn thay vì phải gõ tay."*
* **Kết quả AI sinh**:
  - Tạo component [fragments/flower-attributes.html](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/templates/fragments/flower-attributes.html) và [flower-attributes.js](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/static/js/flower-attributes.js).
  - Tích hợp nút dropdown sổ xuống danh sách các tùy chọn có sẵn (Loại hoa, Màu sắc, Phong cách, Dịp tặng) click tạo tag nhanh chóng.

---

### Prompt 16:
> *"Khi thêm sản phẩm bị lỗi 413 FileCountLimitExceededException."*
* **Kết quả AI sinh**:
  - Loại bỏ các thẻ input file trùng lặp trong form, cấu hình đúng Multipart Resolver cho Tomcat, khắc phục triệt để lỗi upload ảnh.

### Prompt 17:
> *"Bạn giúp tôi, sau khi tôi sửa các thuộc tính thì các thuộc tính cũ của tôi bị mất đi."*
* **Nguyên nhân phân tích**: Lệch tên alias `"Phong cách"` vs `"Phong cách thiết kế"` trong CSDL; hàm `assignText` ghi đè giá trị rỗng.
* **Kết quả AI sinh**:
  - Bổ sung ánh xạ alias trong [FlowerAttributes.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/utils/FlowerAttributes.java).
  - Sửa [ProductAttributeService.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/service/ProductAttributeService.java) giữ nguyên thuộc tính cũ khi trường bị bỏ qua.
  - Bổ sung hàm [Product.getSelectedStyleValueIds()](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/entity/Product.java#L236) giúp Thymeleaf tick sẵn mọi tag/checkbox cũ.

### Prompt 18:
> *"Bạn kiểm tra giúp tôi xóa toàn bộ các file không cần đến trong dự án và tự động tạo trong quá trình biên dịch."*
* **Kết quả AI sinh**:
  - Chạy `mvn clean` xóa sạch `target/` (~86 MB), xóa `.tmp/` (~15 MB) và các file zip/log cũ trong `.tools/` (~240 MB), làm sạch thư mục Git.

### Prompt 19:
> *"Với sự hỗ trợ của AI, Nhóm hãy viết tài liệu bằng file .docx chi tiết hướng dẫn xây dựng từng chức năng của ứng dụng gồm các phần: các hình kiến trúc ứng dụng, các hình sơ đồ hoạt động của chức năng + hình diễn giải Source Code, hình diễn giải giao diện, file hướng dẫn setup ứng dụng, danh sách các prompt nhóm đã sử dụng để AI hỗ trợ sinh code, sinh hình,..."*
* **Kết quả AI sinh**:
  - Viết script Python sinh 5 sơ đồ chất lượng cao bằng `matplotlib` (kiến trúc 5 tầng, luồng đặt hàng, vòng đời đơn hàng, bảo mật kép, chat WebSocket).
  - Tự động biên soạn và xuất file Word chuẩn mực: [docs/HUONG_DAN_XAY_DUNG_VA_KIEN_TRUC_UNG_DUNG.docx](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/docs/HUONG_DAN_XAY_DUNG_VA_KIEN_TRUC_UNG_DUNG.docx).

### Prompt 20:
> *"Tổng hợp các file trong docs và viết lại cho tôi thành một file .md xóa các file sau khi tổng hợp."*
* **Kết quả AI sinh**:
  - Viết script Python đọc toàn bộ 24 file markdown rời rạc, tổng hợp thành một tài liệu duy nhất [docs/TAI_LIEU_TONG_HOP_DU_AN.md](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/docs/TAI_LIEU_TONG_HOP_DU_AN.md) với 10 chương toàn diện, sau đó xóa sạch 24 file cũ.

---

### KẾT LUẬN VỀ KỸ NĂNG PROMPT ENGINEERING CỦA NHÓM
1. **Tính định hướng kỹ thuật cao**: Các câu lệnh prompt đều chỉ rõ mục tiêu công nghệ (Spring Boot, JWT Stateless, Pessimistic Lock, Flyway), không để AI tự phán đoán mơ hồ.
2. **Kiểm soát chất lượng bằng Unit Test**: Mọi tính năng do AI hỗ trợ code đều được kiểm chứng qua 547 ca kiểm thử tự động trước khi hợp nhất.
3. **Bảo mật tuyệt đối**: Không lưu trữ mật khẩu hay secret trong mã nguồn, luôn tuân thủ nguyên tắc nạp qua biến môi trường.
