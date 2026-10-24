# TÀI LIỆU HƯỚNG DẪN SETUP ỨNG DỤNG VÀ DANH SÁCH PROMPT AI
## ĐỒ ÁN: WEBSITE BÁN HÀNG THEO MÔ HÌNH CHUỖI CỬA HÀNG (BLOOM FLOWER CHAIN)
### Công nghệ: Spring Boot + Thymeleaf + Bootstrap + JPA + SQL Server + WebSocket + JWT + Cloudinary

---

> **MỤC ĐÍCH TÀI LIỆU**:
> 1. Cung cấp hướng dẫn từng bước chi tiết (Step-by-Step Setup Guide) để Giảng viên và Hội đồng chấm thi có thể dễ dàng cài đặt, cấu hình CSDL và khởi chạy website trơn tru trên máy tính cá nhân.
> 2. Tổng hợp các prompt/yêu cầu AI liên quan đến dự án từ tài liệu cũ và cuộc trò chuyện hiện có. Đây là **bản tóm lược để đối chiếu**, không thay thế bản xuất lịch sử chat, commit hoặc minh chứng demo khi nộp bài.

**Công cụ AI đã dùng/được nhắc đến:** Google Antigravity (Gemini) và OpenAI Codex. Hai công cụ có thể hỗ trợ ở các giai đoạn khác nhau; báo cáo không tự gán mọi thay đổi mã nguồn cho một công cụ khi chưa đối chiếu lịch sử làm việc. Ảnh chụp do người dùng gửi là **minh chứng lỗi/yêu cầu**, không phải prompt do AI tự tạo.

---

## MỤC LỤC

- [PHẦN 1: HƯỚNG DẪN SETUP VÀ VẬN HÀNH ỨNG DỤNG](#phan-1-setup)
  - [1.1. Yêu cầu môi trường & Phần mềm cần thiết](#11-yeu-cau-moi-truong)
  - [1.2. Cấu hình Cơ sở dữ liệu SQL Server (`RentalShop`)](#12-cau-hinh-csdl)
  - [1.3. Cấu hình biến môi trường (`.env`)](#13-cau-hinh-env)
  - [1.4. Lệnh khởi chạy ứng dụng (2 phương thức)](#14-lenh-khoi-chay)
  - [1.5. Danh sách tài khoản thử nghiệm theo các vai trò](#15-tai-khoan-thu-nghiem)
  - [1.6. Xử lý sự cố thường gặp (Troubleshooting)](#16-troubleshooting)
- [PHẦN 2: DANH SÁCH PROMPT VÀ YÊU CẦU AI ĐÃ GHI NHẬN](#phan-2-prompts)
  - [Prompt 1–20 đang có trong bản báo cáo](#phan-2-prompts)
  - [Nhật ký yêu cầu bổ sung từ cuộc trò chuyện](#nhat-ky-bo-sung)
  - [Prompt nguyên văn bổ sung ngày 23/10/2026](#prompts-bo-sung-20261023)
  - [Prompt tóm lược từ các ảnh chụp màn hình](#prompts-tu-anh)

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
   - Bạn **không cần chạy script tạo bảng thủ công**. Với database rỗng, Flyway áp dụng các migration `V1`–`V10` khi ứng dụng khởi động. Với database đã có dữ liệu, phải sao lưu, thử trên bản sao và kiểm tra `flyway_schema_history` trước. Database `RentalShop` cục bộ đã được nâng lên V10 ngày 19/10/2026; điều này **không** có nghĩa database trên máy khác cũng đã lên V10. Migration nằm trong `src/main/resources/db/migration/sqlserver` và `src/main/java/com/finalproject/db/migration`.

---

<a name="13-cau-hinh-env"></a>
### 1.3. Cấu hình biến môi trường (`.env`)
Để tránh đưa mã khóa bí mật vào Git, có thể dùng biến môi trường hoặc file `.env` cục bộ đã bị Git ignore. `.env` **không được mã hóa**; Spring Boot không tự đọc file này. Script `scripts/sqlserver.ps1` nạp các biến Cloudinary từ `.env`, còn các biến khác cần cấu hình theo hướng dẫn của từng dịch vụ hoặc trong môi trường chạy.

Hãy tạo (hoặc kiểm tra) file **`.env`** tại thư mục gốc `finalproject/`:
```dotenv
# Cấu hình lưu trữ ảnh Cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret

```

> [!TIP]
> Script hiện chỉ nạp ba biến Cloudinary từ `.env`. `JWT_SECRET`, `SPRING_PROFILES_ACTIVE` và `APP_DEMO_DATA_ENABLED` cần đặt trong môi trường PowerShell/IDE nếu dùng; riêng script chạy SQL Server đặt profile `dev`. Chỉ bật `APP_DEMO_DATA_ENABLED=true` khi chủ động tạo dữ liệu mẫu trên **database demo**. Thay/thu hồi mọi API key từng xuất hiện trong chat, ảnh chụp hoặc Git; tệp `.env` bị Git ignore không bảo vệ những khóa đã lộ trước đó.

---

<a name="14-lenh-khoi-chay"></a>
### 1.4. Lệnh khởi chạy ứng dụng (2 phương thức)

Mở cửa sổ **PowerShell** tại thư mục gốc dự án (`d:\LAPTRINH\LAPTRINHWEB\BAITAP\finalproject`):

#### Cách 1: Sử dụng Script tự động kết nối SQL Server (Khuyên dùng)
```powershell
.\scripts\sqlserver.ps1 -Action run
```
*Script dùng instance `SQLEXPRESS` mặc định, nạp credential SQL Server cục bộ đã bảo vệ và cấu hình Cloudinary từ `.env`, rồi chọn JDK 21 để khởi chạy. Máy khác cần tự thiết lập credential/URL phù hợp; không sao chép tệp credential cục bộ.*

#### Cách 2: Khởi chạy trực tiếp qua Maven Wrapper
Trước khi chạy, đặt `SPRING_PROFILES_ACTIVE`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` và các biến Cloudinary trong môi trường của terminal/IDE. Cách này **không tự nạp `.env`**.
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

Các tài khoản bên dưới chỉ là **ví dụ minh họa**; tên đăng nhập/mật khẩu thực tế phụ thuộc dữ liệu mẫu trên database của người chạy. Không công bố hoặc sử dụng mật khẩu demo trên hệ thống thật.

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
# PHẦN 2: DANH SÁCH PROMPT VÀ YÊU CẦU AI ĐÃ GHI NHẬN

Dưới đây là 20 mục có sẵn trong bản báo cáo trước và phần bổ sung từ cuộc trò chuyện. Những câu đặt trong dấu ngoặc kép ở phần cũ cần được đối chiếu với log nếu muốn trình bày như trích dẫn nguyên văn; mô tả “Kết quả AI sinh” cũng cần được kiểm chứng bằng mã nguồn/commit tương ứng.

---

### Prompt 1:
> *"Tôi cần xây dựng đồ án website bán hàng theo mô hình chuỗi cửa hàng hoa tươi bằng Spring Boot, giao diện Thymeleaf + Bootstrap 5, ORM JPA, CSDL SQL Server, hỗ trợ WebSocket, JWT và Cloudinary. Hãy phân tích cấu trúc gói (package structure) chuẩn MVC 3 lớp và file `pom.xml` với các dependency cần thiết."*
* **Ngữ cảnh & Ràng buộc**: Yêu cầu Java 21 LTS, Spring Boot 4.1.1, phân tách rõ ràng giữa Web Thymeleaf và REST API.
* **Kết quả AI sinh**:
  - Sinh toàn bộ file [pom.xml](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/pom.xml) với đầy đủ starters: `data-jpa`, `thymeleaf`, `security`, `oauth2-resource-server`, `websocket`, `flyway-sqlserver`, `mssql-jdbc` và `cloudinary-http44`.
  - Thiết lập cấu trúc thư mục phân lớp: `config`, `controller/{customer, manager, admin, auth}`, `service`, `repository`, `entity`, `dto`, `security`.

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
> *"Hãy hiện thực luồng xác thực JWT: cấp Access Token và Refresh Token, hỗ trợ xoay token (rotation) và thu hồi (revoke) khi đăng xuất."*
* **Kết quả AI sinh**:
  - Viết `JwtTokenService` ký token HMAC-SHA256 với secret từ biến môi trường.
  - Xem [JwtAuthController.java](../src/main/java/com/finalproject/controller/auth/JwtAuthController.java) với các endpoint xác thực JWT trong `/api/auth/**`.

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

<a name="nhat-ky-bo-sung"></a>
## NHẬT KÝ BỔ SUNG: CÁC YÊU CẦU TRONG CUỘC TRÒ CHUYỆN

Các mục sau **tóm lược theo thứ tự chủ đề xuất hiện trong cuộc trò chuyện được cung cấp**, không phải bản chép nguyên văn từng tin nhắn và không tự chứng minh chức năng đã hoàn thành. Một số yêu cầu được hỏi lại hoặc bổ sung bằng ảnh; chúng được gộp khi cùng một vấn đề. Không ghi API key, mật khẩu, OTP hay dữ liệu riêng tư vào báo cáo. Các Prompt 1–20 ở phần trên là nội dung đã có sẵn trong tài liệu trước lần cập nhật này; cần đối chiếu lịch sử/commit nếu dùng chúng như bằng chứng nguyên văn.

### A. Tài khoản, đơn hàng và tồn kho

| Số | Yêu cầu/prompt người dùng đã nêu (tóm lược) |
| ---: | --- |
| 21 | OTP của khách B khi đổi/quên mật khẩu phải gửi đến đúng email đã đăng ký của B, không gửi về email cấu hình của cửa hàng. Hướng dẫn cấu hình Brevo và kiểm tra thư thật bằng hai email sở hữu. |
| 22 | Kiểm tra cấu hình gửi mail đã lưu, lỗi chạy `scripts/sqlserver.ps1` liên quan PowerShell/Java, và hướng dẫn các bước để hoàn thành demo. |
| 23 | Đăng nhập sai email, tên đăng nhập hoặc mật khẩu phải giữ lại đúng email/tên đăng nhập người dùng vừa nhập; không tự điền tên demo mặc định. |
| 24 | Khi đặt hàng thành công, kể cả COD, phải trừ tồn kho ngay tại **đúng chi nhánh** và cập nhật số lượng/nhãn “Hết hàng” trên trang khách; không chờ giao hàng. |
| 25 | Người dùng được hủy trước khi bắt đầu chuẩn bị/giao, hoàn kho đúng một lần; khi đang giao thì không được hủy. Giữ nguyên đơn lịch sử `#23`, chỉ sửa luồng đặt hàng mới. |
| 26 | Thảo luận các trạng thái quản lý đơn: xác nhận, bắt đầu cắm hoa, giao hàng, hoàn tất, hủy và quyền hủy theo từng giai đoạn. |
| 27 | Khi admin/quản lý hủy đơn phải có ô nhập lý do; gửi thông báo cho khách theo dạng “Đơn hàng của bạn đã bị hủy bởi admin với lý do…”. |
| 28 | Giải thích vì sao tổng tồn kho ở thống kê khác số mẫu hoa quản lý Q1 đang thấy; trang sản phẩm phải hiện đầy đủ mẫu thuộc quyền quản lý, có thao tác Thêm/Sửa/Xóa bằng tiếng Việt và xóa mềm an toàn. |
| 29 | Không hiển thị thẻ tổng tồn kho toàn chuỗi trên giao diện khách; vẫn phải cập nhật số lượng/“Hết hàng” của từng sản phẩm. |
| 30 | Sửa tên hiển thị và tên đăng nhập demo của khách/admin cho tự nhiên, phân biệt tài khoản; giải thích khách vãng lai là phiên chưa đăng nhập, không cần bản ghi role `GUEST` trong database. |
| 31 | Sửa lỗi trang sản phẩm và form thêm sản phẩm của quản lý Q1; không hiển thị tồn kho chi nhánh khác như hàng bán được tại Q1. |

### B. Chat, WebSocket và phân quyền chi nhánh

| Số | Yêu cầu/prompt người dùng đã nêu (tóm lược) |
| ---: | --- |
| 32 | Hướng dẫn test WebSocket: khách và quản lý chat/nhận thông báo giữa hai phiên mà không tải lại trang. |
| 33 | Kiểm tra quyền chat: chi nhánh chỉ nhận và đọc hội thoại gửi đến mình; quản lý Q1 không được xem tin của Q7. Admin có thể giám sát toàn chuỗi ở màn hình riêng, **chỉ đọc** và có nhật ký truy cập. |
| 34 | Khách nhắn với cửa hàng bằng nút “Nhắn tin với Cửa hàng” trên chi tiết sản phẩm; hệ thống tự chọn đúng chi nhánh bán và đính kèm sản phẩm đang hỏi, không bắt khách chọn người nhận thủ công. |
| 35 | Thêm “Tài khoản → Trò chuyện với hệ thống” để khách nhắn admin. Nút “Nhắn tin” ở trang chủ mở toàn bộ lịch sử hội thoại; hỗ trợ toàn chuỗi và hỏi sản phẩm là hai luồng khác nhau. |
| 36 | Giao diện chat không nên giải thích dài như AI; dùng nhãn ngắn, tự nhiên. Trong giám sát admin, tin khách ở bên phải, tin chi nhánh ở bên trái và khác màu. |
| 37 | Nới chiều cao vùng hội thoại và chỉnh bố cục màn hình chat/admin cho dễ đọc; xác nhận sau đó đẩy code lên GitHub với commit tiếng Việt ngắn gọn (chỉ áp dụng cho lần được yêu cầu). |

### C. Cloudinary, sản phẩm, danh mục và thuộc tính

| Số | Yêu cầu/prompt người dùng đã nêu (tóm lược) |
| ---: | --- |
| 38 | Đánh giá mức hoàn thành stack Spring Boot, Thymeleaf, Bootstrap, JPA, SQL Server, WebSocket, JWT và Cloudinary theo rubric môn học; hướng dẫn demo chức năng thật. |
| 39 | Hướng dẫn và kiểm tra tải ảnh sản phẩm, gửi ảnh chat, xác nhận ảnh thực sự có trên Cloudinary; cấu hình qua biến môi trường, không công khai API secret. |
| 40 | Đổi danh mục từ tên loại hoa/kiểu bó sang dịp hoặc mục đích: Hoa Tình Yêu, Sinh Nhật, Cưới, Khai Trương… Mỗi sản phẩm có **một danh mục chính và nhiều danh mục bổ sung**. |
| 41 | Chuyển sản phẩm cũ về đúng danh mục dịp; nhóm chưa rõ dịp vào Hoa Quà Tặng/Hoa Trang Trí. Giữ lịch sử và không làm mất ảnh, tồn kho, đơn hàng. |
| 42 | Phân biệt giá bán với giá khuyến mãi là **số tiền khách trả**, không phải phần trăm; không nhận giá lẻ như `0.01` đồng rồi hiển thị thành `0đ`. |
| 43 | Form thêm/sửa sản phẩm quá nhiều thuộc tính: loại hoa, màu sắc, phong cách phải cho **tự nhập hoặc chọn gợi ý**; một bó có thể gồm nhiều loại/màu/phong cách. |
| 44 | Hiển thị lại thuộc tính cũ khi sửa sản phẩm; nhấn Enter hoặc “Thêm” để tạo từng giá trị; trình bày tag, gợi ý và các trường cho gọn, dễ nhìn, không để khu vực thuộc tính trống. |

### D. Quản trị, giao hàng demo và chất lượng dự án

| Số | Yêu cầu/prompt người dùng đã nêu (tóm lược) |
| ---: | --- |
| 45 | Tab “Đơn vị vận chuyển” chỉ quản lý thông tin đơn vị; giá cước đặt ở tab “Khu vực & Biểu phí”. Nhãn thao tác dùng tiếng Việt. |
| 46 | Trang admin “Người dùng” phải hiển thị danh sách người dùng hiện có; điều tra trường hợp tiêu đề báo 15 người nhưng bảng trống. Sửa xong mới push GitHub với commit tiếng Việt ngắn gọn (yêu cầu của lần đó). |
| 47 | Điều tra trang sản phẩm của role quản lý chuyển sang trang lỗi hệ thống; cần tìm nguyên nhân từ code/log, không chỉ dựa vào ảnh lỗi chung. |
| 48 | Kiểm tra toàn bộ cấu trúc dự án, chạy test Java/JavaScript và rà soát các luồng web; nêu rõ phần đã chứng minh và phần chưa thể kiểm thử trực tiếp. |
| 49 | Vùng giao hàng chỉ là **demo**, không cần bản đồ thật; bỏ cách đoán khoảng cách từ chuỗi địa chỉ vì “Quận 1” có thể khớp nhầm “Quận 10/12”. Chọn vùng giao rõ ràng và dùng cùng quy tắc ở báo giá/checkout. |
| 50 | Phần thanh toán do thành viên khác phụ trách; giải thích ranh giới công việc, cấu hình mock/sandbox và các điểm cần phối hợp, không tự nhận hệ thống thanh toán thật đã hoàn tất. |
| 51 | Đối soát 81 dòng kho lệch chi nhánh (2.003 đơn vị) trước khi sửa/xóa; không đổi đơn `#23`. Sau đó người dùng xác nhận đây là **dữ liệu kho cũ bị nhân bản** và yêu cầu xóa các dòng đó. |
| 52 | Hoàn thiện các điểm audit: schema SQL Server V8→V10, test cũ lệch HTML, vùng giao demo, tài liệu/liên kết, rồi “sửa tất cả, tự sửa”. |
| 53 | Hỏi vì sao tin nhắn trong hàng chờ của Antigravity không tự gửi sau khi agent hoàn thành và muốn hướng dẫn cấu hình; đây là yêu cầu về công cụ AI, **không phải** chức năng website. |
| 54 | Yêu cầu cập nhật chính báo cáo setup/danh sách prompt này để phản ánh cuộc trò chuyện, thay vì chỉ giữ 20 prompt mẫu ban đầu. |

### E. Các yêu cầu bổ sung về trải nghiệm khách hàng và thanh toán

| Số | Yêu cầu/prompt người dùng đã nêu (tóm lược) |
| ---: | --- |
| 55 | Trang đăng ký phải để trống các trường cho khách mới tự nhập; không tự điền tên đăng nhập/mật khẩu từ tài khoản đã lưu hoặc dữ liệu demo. Khi biểu mẫu có lỗi, chỉ giữ dữ liệu phù hợp do chính khách vừa nhập, không giữ mật khẩu. |
| 56 | Tìm kiếm sản phẩm phải trả kết quả khớp từ khóa, đặc biệt khi nhập “hân” hoặc “hoa hướng dương”; kiểm tra toàn bộ luồng từ ô tìm kiếm, tham số URL đến truy vấn và cách sắp xếp. Không để sản phẩm chỉ khớp lỏng lẻo ở mô tả/thuộc tính lấn át sản phẩm khớp tên. |
| 57 | Khi thêm hoặc sửa địa chỉ nhận hàng bị lỗi kiểm tra dữ liệu, phải báo rõ trường sai và giữ lại những thông tin khách đã nhập để họ sửa, không xóa trắng biểu mẫu. |
| 58 | Trên chi tiết sản phẩm dùng tên thao tác “Yêu thích/Bỏ yêu thích” thay cho “Theo dõi”; thông báo sau thao tác phải đúng ngữ cảnh sản phẩm và không báo nhầm “không có quyền truy cập” khi khách bỏ yêu thích. |
| 59 | Kiểm thử nhập sai thông tin đặt hàng: thiếu tên, số điện thoại, địa chỉ giao; chọn ngày/giờ đã qua; chưa chọn chi nhánh nhận tại cửa hàng. Mỗi lỗi phải được giải thích dễ hiểu, không tạo đơn và không giảm tồn kho. |
| 60 | Khi thiếu tên, số điện thoại hoặc địa chỉ giao, nhấn Đặt hàng phải **hiện thông báo ngay**, không để nút im lặng do báo giá chưa xong. Người mua không tự chọn đơn vị vận chuyển; hệ thống tự chọn đơn vị phù hợp theo chi nhánh, vùng giao demo và biểu phí, hiển thị kết quả trên trang thanh toán. |

### F. Các yêu cầu tiếp theo về quản lý sản phẩm, mua hàng và đánh giá

Các mục dưới đây diễn đạt lại yêu cầu người dùng trong các lượt sau; **không phải trích dẫn nguyên văn** và không mặc nhiên chứng minh chức năng đã hoàn thành.

| Số | Yêu cầu/prompt người dùng đã nêu (tóm lược) |
| ---: | --- |
| 61 | Admin/người bán phải có cách **nhập thêm hàng để bán** cho sản phẩm đã hết (ví dụ chậu lan hồ điệp). Tách số lượng đã bán — thống kê không sửa trực tiếp — khỏi số lượng tồn có thể bổ sung theo đúng chi nhánh, có kiểm tra hợp lệ và ghi nhận biến động. |
| 62 | Nút “Đến phần nhập thêm hàng” trên form sửa sản phẩm phải thao tác được và thực sự đưa người dùng đến khu vực nhập hàng; không chỉ thêm liên kết `#restockTitle` mà không mở/hiển thị form tương ứng. |
| 63 | Rà soát danh mục không còn đơn hàng hoặc liên kết liên quan trước khi đề xuất xóa khỏi database; không xóa chỉ vì nhãn đang “Đã xóa”. Đề xuất bộ danh mục bao quát, dễ gắn cho sản phẩm, giữ lịch sử đơn và quan hệ còn dùng. |
| 64 | Trang đăng nhập phải để trống tên đăng nhập và mật khẩu khi mở để người dùng đăng nhập tài khoản khác; phân biệt dữ liệu điền từ ứng dụng với tính năng tự điền của trình duyệt. |
| 65 | Đồng bộ trang chi tiết đơn hàng với giao diện trang chủ: font/chữ tiếng Việt, hệ icon, màu trạng thái “Đã tiếp nhận”, viền và khoảng cách; không trộn kiểu chữ hoặc nút tiếng Anh. |
| 66 | Ở trang “Đơn hoa của tôi”, thẻ đơn phải ưu tiên **trạng thái và mã đơn → ảnh/tên sản phẩm, số lượng → tổng thanh toán → nút xem chi tiết**. Địa chỉ/chi nhánh chỉ là dòng tóm tắt, chi tiết đầy đủ nằm ở trang chi tiết đơn. |
| 67 | Thêm nút “Mua ngay” trên trang chi tiết sản phẩm; thiết kế luồng chỉ thanh toán sản phẩm và số lượng đang chọn, không vô ý mua cả giỏ hoặc làm mất các mặt hàng khác. Kiểm tra đăng nhập, chi nhánh, tồn kho, trạng thái bán trước khi sang checkout. |
| 68 | Sau khi nhận hàng thành công, nút “Đánh giá” trên đơn phải mở thẳng `#review-form` của đúng sản phẩm. Lỗi thiếu sao/nội dung chưa đủ 50 ký tự hoặc dữ liệu ảnh sai chỉ hiện thông báo nhỏ cạnh biểu mẫu, không mở hộp thoại lớn. |
| 69 | Chỉnh ô chấm sao không đè lên chữ; ảnh đánh giá được **tải từ máy** thay vì bắt khách nhập URL. Sau khi gửi, đánh giá hợp lệ phải lưu và xuất hiện ở phần đánh giá cuối trang chi tiết sản phẩm. |
| 70 | Điều tra lỗi SQL khi gửi đánh giá: bảng `reviews` yêu cầu cột `stars` không null nhưng câu lệnh chỉ ghi `rating`. Đồng bộ entity, migration và schema thật; không lộ lỗi SQL thô cho khách. |
| 71 | Kiểm tra đường dẫn “Mua ngay” `/cart/buy-now` không chuyển đến trang “Trang hoặc chức năng này không tồn tại”; form, phương thức HTTP, phân quyền và redirect phải khớp với controller. |
| 72 | Khi khách yêu cầu hủy đơn, cho chọn một lý do trong danh sách: đổi địa chỉ nhận hàng, đổi mã giảm giá, đặt nhầm sản phẩm, không còn nhu cầu, chưa sẵn sàng thanh toán hoặc lý do khác; lưu lý do cùng yêu cầu hủy và giữ ràng buộc trạng thái đơn. |
| 73 | Giải thích vì sao ảnh/sản phẩm và thay đổi người dùng trên máy người phát triển không xuất hiện khi đồng đội `git pull`: Git chia sẻ mã/migration, không tự đồng bộ bản ghi SQL Server hay ảnh upload cục bộ. Hướng dẫn cách chia sẻ database/seed/backup an toàn; không đẩy khóa Cloudinary lên GitHub. |
| 74 | Hướng dẫn kiểm thử thông tin cửa hàng và nhân sự: sửa tên/điện thoại/địa chỉ/ảnh, thêm rồi gỡ nhân sự, xác minh người bị gỡ mất quyền; dùng tài khoản đối tác thử thao tác chỉ dành cho chủ cửa hàng như quản lý nhân sự hoặc rút tiền. |
| 75 | Sản phẩm đã có trong giỏ nhưng sau đó bị quản lý **ngừng bán** phải được chặn ở cả giỏ và checkout, không tạo đơn hay trừ kho. Giỏ và trang chi tiết sản phẩm phải báo trạng thái ngừng bán ngay, trước khi khách điền đủ thông tin đặt hàng. |
| 76 | Không cho admin xóa sản phẩm vẫn có đơn đang xử lý (ví dụ đơn chờ xác nhận). Với sản phẩm không còn đơn đang xử lý, xóa mềm rồi khôi phục phải trở về trạng thái **ngừng bán**, chỉ mua được khi người có quyền mở bán lại. |
| 77 | Trước khi admin xác nhận xóa sản phẩm, phải thấy rõ tên sản phẩm và cửa hàng/chi nhánh sở hữu để tránh xóa nhầm sản phẩm của cửa hàng khác. |
| 78 | Sau khi sửa trạng thái sản phẩm, phải giữ nguyên các phần chức năng trên trang chi tiết: số lượng, “Thêm vào giỏ”, “Mua ngay”, liên hệ cửa hàng, chăm sóc hoa và đánh giá. Nếu ngừng bán/hết hàng thì hiển thị lý do và vô hiệu hóa thao tác mua, **không làm trang bị cắt giữa chừng**. |
| 79 | Khi một biến mới của controller chưa có trong tiến trình Java đang chạy, template Thymeleaf phải xử lý an toàn; bổ sung test kiểm tra HTML trả về có đủ phần mua, chăm sóc và đánh giá cho sản phẩm đang bán lẫn ngừng bán. |

<a name="prompts-bo-sung-20261023"></a>
### G. Prompt nguyên văn bổ sung từ cuộc trò chuyện ngày 21–23/10/2026

Cập nhật ngày **23/10/2026**. Các câu trong cột “Prompt của người dùng” được giữ nguyên cách viết trong cuộc trò chuyện, gồm cả lỗi gõ. Cột “Nội dung yêu cầu” chỉ diễn giải để dễ tra cứu; đây là nhật ký yêu cầu, không phải xác nhận mọi hạng mục đã được kiểm thử hoàn tất.

| Số | Prompt của người dùng (nguyên văn) | Nội dung yêu cầu |
| ---: | --- | --- |
| 80 | “đọc lại cấu trúc toàn dự án và lên kịch bản cho tôi tất cả trường hợp tôi tự test” | Đọc cấu trúc toàn dự án, soạn kịch bản tự kiểm thử theo chức năng, có bước thực hiện và kết quả mong đợi. |
| 81 | “lập kịch bản để toi test thủ công, nói dễ hiểu” | Viết lại hướng dẫn test bằng ngôn ngữ dễ hiểu, lần lượt theo vai khách hàng, quản lý và admin. |
| 82 | “tại sao khi tôi đăng kí tài khoản không gửi được email” | Điều tra việc đăng ký báo đã gửi OTP nhưng khách chưa nhận email; đối chiếu cấu hình đang chạy và nhật ký gửi thư. |
| 83 | “tại sao lại gửi lâu như vậy” | Giải thích độ trễ gửi OTP dựa trên thời điểm dịch vụ nhận yêu cầu và giao thư; phân biệt điều đã xác minh với nguyên nhân chưa đủ bằng chứng. |
| 84 | “tại sao lịa bị ẩn hết các nút” | Kiểm tra trang admin quản lý sản phẩm bị mất các nút cùng giá, tồn kho và trạng thái; xử lý lỗi hiển thị giữa dòng. |
| 85 | “không hiển thị full hết  tất cả sản phẩm” | Kiểm tra danh sách sản phẩm có bị cắt, thiếu dòng hoặc mất phân trang; đối chiếu đủ sản phẩm qua các trang và chế độ tăng số dòng. |
| 86 | “khi sản phẩm ngưng bán thì không phát ra thông báo lỗi như này bà ghi một tag nhỏ thay thế Còn x sản phẩm thành sản phẩm tạm ngưng bán, nếu khách đã thêm vào giỏ hàng thì ghi ở góc trên bên phải tại thẻ này và khóa không cho đặt sản phẩm mặc dù đã thêm vào giỏ hàng còn nếu đang ở trạng thái chờ xác nhận thì vẫn được giao vì khách đặt hàng trowcs khi ngưng bán” | Sản phẩm tạm ngưng bán vẫn xem được; thay số lượng bằng nhãn nhỏ “Sản phẩm tạm ngưng bán”. Trong giỏ, đặt nhãn ở góc trên bên phải và khóa chọn/đặt hàng. Đơn đã đặt trước khi ngưng bán, kể cả chờ xác nhận, vẫn được tiếp tục xử lý và giao. |
| 87 | “không hiện thông báo "1 đơn đang xử lý, chưa thể xóa' nhỏ như này cần hiện hộp thoại để thong báo” | Bỏ cảnh báo nhỏ dưới tên sản phẩm. Khi bấm Xóa mà còn đơn đang xử lý, mở hộp thoại nêu tên sản phẩm, số đơn và lý do chưa thể xóa. |
| 88 | “hình ảnh và chữ quá gần nhau hãy giản cách khoảng nhỏ để dễ nhìn hơn” | Tăng nhẹ khoảng cách giữa ảnh và phần chữ trong danh sách quản lý sản phẩm; giãn phần danh mục cho dễ đọc. |
| 89 | “thêm những promt của tôi vào file .md promt AI” | Bổ sung các prompt của người dùng vào tài liệu Markdown tổng hợp prompt AI này. |

Tài liệu liên quan: [Kịch bản tự test toàn dự án](KICH_BAN_TU_TEST_TOAN_DU_AN.md) và [Bản đồ cấu trúc và route](BAN_DO_CAU_TRUC_VA_ROUTE.md).

<a name="prompts-tu-anh"></a>
### Prompt tóm lược từ các ảnh chụp màn hình người dùng đã gửi

Đây là **cách diễn đạt lại yêu cầu bằng lời** dựa trên ảnh và tin nhắn kèm ảnh, giúp tái hiện cách nhóm giao việc cho Antigravity/Gemini hoặc Codex. Không khẳng định từng câu dưới đây đã được gửi nguyên văn. Ảnh có khóa API/thông tin cá nhân phải được che trước khi đưa vào bản nộp; các đường dẫn ảnh tạm trong chat không phải liên kết minh chứng lâu dài.

| Mã | Ảnh/hiện tượng được phản ánh | Prompt có thể dùng để tái hiện yêu cầu |
| --- | --- | --- |
| A01 | Màn hình tạo API key Brevo. | “Hãy hướng dẫn cấu hình gửi OTP bằng Brevo cho Spring Boot. Phân biệt email **người gửi** của cửa hàng và email **người nhận** của từng khách; không in hoặc lưu API key trong mã nguồn, log hay báo cáo. Kiểm tra OTP đến đúng hai tài khoản thử nghiệm.” |
| A02 | PowerShell lưu cấu hình mail thành công nhưng chạy `sqlserver.ps1` báo lỗi tại bước đọc `java -version`. | “Hãy đọc script khởi động và sửa cách PowerShell xử lý đầu ra stderr của Java. Xác nhận ứng dụng chạy được bằng JDK 21, không sửa nhầm cấu hình Brevo đã lưu.” |
| A03 | Trang `/login?error=true` báo đăng nhập sai nhưng ô tên/email lại hiện tên demo mặc định. | “Khi xác thực thất bại, hãy giữ lại đúng tên đăng nhập hoặc email người dùng vừa nhập, không tự thay bằng tài khoản demo; không giữ lại mật khẩu đã nhập và không làm lộ thông tin tài khoản tồn tại.” |
| A04 | Đơn COD `#23` mua 15 sản phẩm, thẻ sản phẩm vẫn ghi còn 15. | “Hãy truy vết từ checkout tới `StoreInventory` và trang khách. Đặt hết hàng tại đúng chi nhánh phải trừ kho và hiển thị ‘Hết hàng’; hủy hợp lệ phải hoàn đúng một lần. **Giữ nguyên đơn #23**, chỉ sửa luồng đặt hàng mới.” |
| A05 | Menu trạng thái admin chỉ có ‘Bắt đầu cắm hoa & Chuẩn bị’/‘Hủy đơn hàng’; modal hủy chưa có lý do. | “Hãy thiết kế vòng đời đơn rõ các bước xác nhận, chuẩn bị, đang giao, hoàn tất/hủy. Khi admin hủy, bắt buộc nhập lý do và gửi thông báo cho khách; khách không được tự hủy khi đã bắt đầu chuẩn bị hoặc đang giao.” |
| A06 | Tài khoản quản lý Q1 thấy danh sách nơi nhận tin gồm các chi nhánh khác. | “Hãy kiểm tra phân quyền chat ở cả API, truy vấn lịch sử và WebSocket: quản lý chỉ đọc/trả lời hội thoại thuộc chi nhánh mình. Admin giám sát ở màn hình chỉ đọc có nhật ký; không dùng dropdown ở giao diện để thay thế kiểm tra quyền backend.” |
| A07 | Menu ‘Tài khoản’ và nút nhắn tin trên trang chủ. | “Thêm ‘Tài khoản → Trò chuyện với hệ thống’ để khách liên hệ admin; nút nhắn tin nổi ở trang chủ mở danh sách lịch sử. Nút liên hệ trên chi tiết sản phẩm tự mở hội thoại với chi nhánh bán và kèm sản phẩm cần hỏi.” |
| A08 | Ô chat hiển thị đoạn hướng dẫn dài, trông như câu trả lời của AI. | “Hãy rút gọn lời hướng dẫn trong giao diện chat thành nhãn và trạng thái ngắn, tự nhiên; không đưa quy trình sử dụng dài vào khung hội thoại như một tin nhắn.” |
| A09 | Trang giám sát chat admin có tin khách và chi nhánh cùng lệch về một phía. | “Trong chế độ giám sát chỉ đọc, căn tin khách sang phải, tin chi nhánh sang trái và dùng nền khác nhau; giữ tên người gửi/thời gian rõ ràng, không cho admin gửi thay chi nhánh.” |
| A10 | Vùng đọc tin nhắn admin thấp, còn nhiều khoảng trống dưới trang. | “Hãy tăng chiều cao vùng hội thoại theo viewport, để danh sách và nội dung cuộn độc lập; giữ bộ lọc, ô nhập và nút gửi dễ thao tác trên màn hình desktop lẫn nhỏ hơn.” |
| A11 | Form sản phẩm có URL Cloudinary, ảnh sản phẩm/chat hiển thị và Media Library có ảnh tương ứng. | “Hãy hướng dẫn kiểm tra ảnh sản phẩm và ảnh chat thật sự được upload lên đúng Cloudinary account: xem URL `res.cloudinary.com`, đối chiếu asset trong Media Library, thử tải lại trang; không kết luận chỉ từ việc ảnh hiển thị.” |
| A12 | Form thêm sản phẩm có danh mục, thuộc tính hoa và giá khuyến mãi `0.01`. | “Đổi danh mục thành dịp/mục đích, cho một danh mục chính và nhiều danh mục bổ sung. Loại hoa, màu, phong cách cho chọn gợi ý **hoặc tự nhập nhiều giá trị**. Giá khuyến mãi phải là số đồng hợp lệ; chặn `0.01` để không hiện `0đ`.” |
| A13 | Quản lý Q1 thấy 1 mẫu hoa ở tab Sản phẩm nhưng thống kê báo 749 đơn vị tồn. | “Hãy phân biệt số **mẫu sản phẩm** và tổng **đơn vị tồn**; hiển thị đầy đủ sản phẩm quản lý được phép bán ở Q1, không lấy hàng riêng của chi nhánh khác. Bổ sung Thêm/Sửa/Xóa mềm với nhãn tiếng Việt và giải thích cách tính KPI.” |
| A14 | Thẻ tổng tồn kho toàn chuỗi trên trang khách hiển thị 1.296 đơn vị. | “Ẩn thẻ tổng tồn kho nội bộ khỏi giao diện khách; vẫn cập nhật tồn và nhãn Hết hàng của từng sản phẩm theo đúng chi nhánh, kể cả sau khi đặt hoặc hủy đơn.” |
| A15 | Form quản lý Q1 bị vỡ bố cục checkbox danh mục; cảnh báo 428 đơn vị kho cũ lệch chi nhánh. | “Sửa grid/checkbox danh mục bổ sung để không tràn ra ngoài form. Điều tra dòng kho có `store_id` khác chi nhánh sở hữu sản phẩm, tách khỏi kho bán được và lập báo cáo đối soát; không chuyển/xóa số lượng hoặc đơn cũ khi chưa có căn cứ.” |
| A16 | Hai tab quản lý giao hàng trộn cột đơn vị, cước phí, khu vực và biểu phí. | “Tab ‘Đơn vị vận chuyển’ chỉ quản lý thông tin đơn vị; chuyển giá, khu vực, phụ phí và thời gian sang ‘Khu vực & Biểu phí’. Dùng nhãn tiếng Việt nhất quán và không giả định có đo khoảng cách bản đồ.” |
| A17 | `/manager/products` chuyển sang trang lỗi hệ thống chung. | “Hãy tìm exception thực tế từ log, controller, service, query và template của trang sản phẩm quản lý; viết test tái hiện và sửa nguyên nhân gốc, không che lỗi bằng thông báo chung.” |
| A18 | Trang admin Người dùng báo 15 người nhưng bảng danh sách trống. | “Hãy kiểm tra điều kiện lọc `role=ALL`, phân trang và truy vấn backend. Khi chọn ‘Tất cả’, phải liệt kê đúng tài khoản hiện có; test cả bộ lọc vai trò và trang rỗng hợp lệ.” |
| A19 | Antigravity hiển thị ‘Queued Messages’ nhưng tin chỉ gửi khi bấm mũi tên. | “Hãy giải thích cơ chế hàng chờ trong Antigravity và chỉ cách kiểm tra/cấu hình tự gửi sau khi agent kết thúc, nếu phiên bản công cụ hỗ trợ; phân biệt lỗi công cụ AI với lỗi chat của website.” |
| A20 | Form sửa sản phẩm ban đầu trống vùng thuộc tính; bản sau hiện tag cũ nhưng bố cục xấu. | “Hãy giữ nguyên các thuộc tính cũ khi mở form sửa, cho thêm/xóa từng tag bằng Enter hoặc nút Thêm, hỗ trợ gợi ý lẫn giá trị mới, và bố trí ba nhóm Loại hoa/Màu sắc/Phong cách gọn, cân đối, dễ đọc.” |
| A21 | Form đăng ký tự hiện tên đăng nhập và mật khẩu có sẵn. | “Hãy kiểm tra nguồn dữ liệu tự điền trên form đăng ký, bao gồm template, script và autocomplete của trình duyệt. Khách mới phải thấy biểu mẫu trống; không hiển thị tài khoản demo hay mật khẩu đã lưu. Viết test cho trạng thái ban đầu.” |
| A22 | Tìm “hân” nhưng danh sách hiện nhiều mẫu không liên quan trực tiếp. | “Hãy truy vết tham số tìm kiếm và điều kiện truy vấn. Ưu tiên kết quả khớp tên sản phẩm; nêu rõ khi nào mô tả/thuộc tính được tính là khớp và không hiển thị kết quả sai từ khóa.” |
| A23 | Tìm “hoa hướng dương” vẫn thấy kệ hoa, giỏ hoa không có tên này. | “Hãy kiểm tra cách tách từ, so khớp cụm từ và xếp hạng kết quả. Mẫu có tên chứa ‘hoa hướng dương’ phải đứng trước; loại bỏ mẫu không khớp tiêu chí tìm kiếm đã công bố.” |
| A24 | Biểu mẫu địa chỉ báo số điện thoại sai rồi xóa hết thông tin vừa nhập. | “Khi thêm/sửa địa chỉ không hợp lệ, hiển thị lỗi cụ thể cạnh trường hoặc ở đầu biểu mẫu, giữ lại tên người nhận, số điện thoại và địa chỉ vừa nhập; không ghi dữ liệu sai vào database.” |
| A25 | Chi tiết sản phẩm ghi “Theo dõi”; khi bỏ yêu thích trang danh sách trống và hiện thông báo không có quyền. | “Đổi thao tác sản phẩm thành ‘Yêu thích/Bỏ yêu thích’, phân biệt với theo dõi cửa hàng. Sau khi bỏ yêu thích, hiển thị thông báo thành công đúng nghĩa và cập nhật danh sách, không báo lỗi phân quyền sai.” |
| A26 | Form admin sửa sản phẩm chỉ hiện “Số lượng đã bán” bị khóa, không có chỗ bổ sung kho. | “Thêm luồng nhập hàng theo chi nhánh cho sản phẩm hết hàng; giữ số lượng đã bán là thống kê tính từ đơn thật, không dùng nó làm ô sửa tồn kho.” |
| A27 | Nút “Đến phần nhập thêm hàng” hiện trên form nhưng bấm không có tác dụng. | “Truy vết liên kết `#restockTitle`, vị trí đích và trạng thái phần nhập hàng; sửa để nút cuộn/mở đúng form nhập hàng, có phản hồi khi không đủ quyền.” |
| A28 | Trang admin Danh mục hiển thị 10 loại, nhiều mục có nhãn “Đã xóa”. | “Đối soát từng danh mục với sản phẩm, đơn và quan hệ khác trước khi xóa dữ liệu dư. Đề xuất hệ danh mục bao quát theo dịp/mục đích; không làm mất lịch sử.” |
| A29 | Trang đăng nhập tự có sẵn tên tài khoản và mật khẩu. | “Khi mở `/login`, để trống trường đăng nhập và mật khẩu để có thể dùng tài khoản khác; kiểm tra cả dữ liệu demo từ script lẫn browser autofill.” |
| A30 | Chi tiết đơn hàng dùng font, khung và nhãn trạng thái khác trang chủ. | “Lấy trang chủ làm chuẩn thiết kế, đồng bộ font, icon, màu và viền của chi tiết đơn; thay nhãn tiếng Anh, giữ rõ trạng thái ‘Đã tiếp nhận’.” |
| A31 | Trang chi tiết sản phẩm có “Thêm vào giỏ” nhưng thiếu “Mua ngay”. | “Bổ sung Mua ngay dùng đúng sản phẩm/số lượng đang xem, đi thẳng vào checkout riêng mà không xóa các món khác trong giỏ.” |
| A32 | Biểu mẫu đánh giá: sao đè chữ, nhập URL ảnh, lỗi 50 ký tự hiện modal; sau khi gửi phát sinh lỗi `reviews.stars`. | “Đưa nút Đánh giá đến đúng form, dùng thông báo nhỏ cạnh trường, chỉnh bố cục sao, cho upload ảnh từ máy và sửa lệch schema `rating`/`stars`; đánh giá thành công phải hiện trên chi tiết sản phẩm.” |
| A33 | Truy cập `/cart/buy-now` gặp trang báo không tồn tại. | “Kiểm tra route GET/POST của Mua ngay, form action, quyền truy cập và đường chuyển trang; người mua không được rơi vào lỗi 404.” |
| A34 | Ảnh/sản phẩm trên máy chủ của một thành viên không xuất hiện sau khi đồng đội kéo mã từ GitHub. | “Phân biệt dữ liệu SQL Server, ảnh Cloudinary và mã Git. Hướng dẫn chia sẻ bản sao dữ liệu/seed và cấu hình Cloudinary mà không commit khóa bí mật hoặc giả định `git pull` đồng bộ database.” |
| A35 | Thẻ đơn hàng để khối địa chỉ quá lớn trong khi sản phẩm/số lượng/tổng tiền khó nhìn. | “Sắp lại thẻ đơn theo ưu tiên trạng thái, sản phẩm có ảnh và số lượng, tổng thanh toán, thao tác; thu gọn địa chỉ/chi nhánh thành một dòng.” |
| A36 | Sản phẩm ngừng bán vẫn có thể đi đến bước đặt hàng; admin xóa sản phẩm có đơn chờ xác nhận và thiếu tên cửa hàng trong xác nhận. | “Báo ngừng bán ngay ở giỏ/chi tiết, chặn lại ở backend checkout, khóa xóa khi còn đơn đang xử lý và nêu tên chi nhánh sở hữu trước khi admin xác nhận xóa.” |
| A37 | Trang `/product/40` chỉ còn ảnh và thuộc tính; mất nút mua, liên hệ, chăm sóc hoa và đánh giá. | “Tìm điểm làm HTML bị cắt sau dòng Kích thước; đảm bảo template an toàn khi biến controller chưa được nạp, giữ các khu vực chức năng cho sản phẩm đang bán và thông báo rõ khi ngừng bán.” |

### Kết quả kiểm chứng và phần chưa hoàn tất tại thời điểm cập nhật

- Trên máy cục bộ, `RentalShop` đã được sao lưu và nâng Flyway V9–V10; kiểm tra HTTP cơ bản và đối soát số dư demo chỉ đọc đạt. Java **622 ca: 616 đạt, 6 bỏ qua**, JavaScript **102/102 đạt**. Đây là kết quả test tự động, không thay thế việc demo mọi thao tác bằng trình duyệt trên máy chấm.
- Người dùng đã xác nhận 81 dòng kho lệch là dữ liệu nhân bản và yêu cầu xóa. Sau khi sao lưu và diễn tập trên **bản sao**, đã xóa 81 dòng/2.003 đơn vị khỏi `RentalShop` thật; bảng audit và backup giữ bản gốc. Còn 73 dòng kho/1.336 đơn vị, không còn kho lệch; các đơn, gồm `#23`, không bị sửa.
- Một số yêu cầu trong lịch sử là **hướng dẫn, đánh giá hoặc câu hỏi**; không nên ghi tất cả thành “AI đã sinh code”. Với mỗi tính năng đem trình bày, hãy gắn commit, test hoặc ảnh demo tương ứng. Không công bố khóa Brevo/Cloudinary/JWT từng dùng trong quá trình thử nghiệm.

---

### KẾT LUẬN VỀ KỸ NĂNG PROMPT ENGINEERING CỦA NHÓM
1. **Tính định hướng kỹ thuật cao**: Các câu lệnh prompt đều chỉ rõ mục tiêu công nghệ (Spring Boot, JWT Stateless, Pessimistic Lock, Flyway), không để AI tự phán đoán mơ hồ.
2. **Kiểm soát chất lượng bằng kiểm thử**: Lượt kiểm tra gần nhất có 622 ca Java (616 đạt, 6 bỏ qua) và 102/102 ca JavaScript đạt; một số tích hợp bên ngoài vẫn cần demo trực tiếp.
3. **Bảo mật cần được duy trì**: Đưa secret ra khỏi tài liệu/mã nguồn, thu hồi khóa đã lộ và kiểm tra cấu hình môi trường trước khi nộp hoặc triển khai.


### Bổ sung prompt ngày 24/10/2026 — phạm vi quản lý cửa hàng

- Vấn đề người dùng phản ánh: “Nghiệp vụ của manager chưa rõ, một người có thể quản lý nhiều cửa hàng”.
- Quy định người dùng xác nhận: “Mỗi manager chỉ được quản lý một cửa hàng; chỉ chủ chuỗi/admin quản lý nhiều cửa hàng.”
- Phạm vi triển khai: giới hạn cửa hàng trong backend, danh sách/tổng quan, tạo cửa hàng, phân công nhân sự, chat và thông báo. Giữ dữ liệu lịch sử; tài khoản cũ có phân công rõ thì chỉ truy cập cửa hàng đó. Các bước tự kiểm tra nằm ở phần bổ sung ngày 24/10/2026 trong `KICH_BAN_TU_TEST_TOAN_DU_AN.md`.


### Bổ sung prompt ngày 24/10/2026 — tách Staff

- Phản ánh: “Vai trò staff chưa rõ, vai trò cửa hàng chưa rõ”.
- Người dùng xác nhận: “Tách riêng quyền Staff như trên (đề xuất)”: Admin quản lý toàn chuỗi; Manager một chi nhánh; Staff xử lý đơn và hỗ trợ khách ở một chi nhánh, không quản lý nhân sự/tài chính/cấu hình; Khách hàng mua hàng.
- Thực hiện: thêm STAFF riêng, đổi nhãn Chủ sở hữu thành Quản lý chi nhánh, giới hạn chức năng và chat theo chi nhánh, bổ sung migration V16 cho nhân viên cũ. Quy định và kịch bản test nằm trong `PHAN_QUYEN_CUA_HANG.md`.

- Prompt bổ sung: “tạo role đăng nhập nhanh staff cho tôi”. Thêm nút chọn tài khoản Staff `nguyenmailan` trên trang đăng nhập, dùng tài khoản demo đã được phân công chi nhánh.

- Prompt bổ sung: “xóa phần này và đặt nút thông báo về vị trí phù hợp, menu và thống kê không cần vì nó đã có trang riêng”. Bỏ nút Menu và nhãn trang lặp trong đầu trang admin; đưa chuông thông báo lên góc phải, giữ huy hiệu chưa đọc.


### Bổ sung prompt — hoàn thiện khu vực Nhân viên

Yêu cầu: “Bổ sung đầy đủ chức năng của staff, có thể trang thành các trang trong role nhân viên”: xem đơn và thông tin giao hàng; xác nhận/chuẩn bị hoa; cập nhật giao hàng/hoàn tất đúng thứ tự; giao hoa và thu COD tại quầy; đọc/trả lời tin nhắn khách của cửa hàng.

Triển khai thành Đơn hàng, Chi tiết đơn, Chuẩn bị hoa, Giao & nhận hoa và Tin nhắn khách hàng. Giới hạn theo chi nhánh trong backend, xác nhận thu đủ COD trước khi ghi nhận đã thanh toán, kiểm thử chống nhảy bước và ghi nhận tiền trùng. Kịch bản tự test trong `KICH_BAN_TEST_NHAN_VIEN.md`.

- Prompt bổ sung: “tôi muốn các khu vực là thanh dọc như trong role admin”. Chuyển điều hướng Staff sang sidebar xanh đậm bên trái, mục đang chọn nổi bật; ghim tài khoản/đăng xuất ở dưới và hỗ trợ mở/đóng trên màn hình nhỏ.


### Bổ sung prompt — xóa tài khoản

- Yêu cầu: “hiển thị đã xóa tài khoản.... thành công, không hiển thị dài dòng” và làm rõ khi Admin xóa khách hàng có đơn đang đặt, nhân viên, quản lý.
- Người dùng chọn: **Chặn xóa đến khi đơn xử lý xong (đề xuất)**.
- Thực hiện: thông báo xóa ngắn; chặn đơn chưa kết thúc/chờ hoàn tiền; thu hồi phân công và quyền của Staff; Manager phụ trách phải được thay thế trước; bảo toàn lịch sử. Không tự hủy đơn khi xóa.

- Prompt bổ sung: “Ngừng bán ko được chọn nhưng vẫn tính vào tổng mục sản phẩm”. Sửa dòng Chọn tất cả trong giỏ chỉ đếm mục và số lượng còn có thể đặt; loại sản phẩm ngừng bán/hết hàng, cập nhật lại sau đồng bộ giỏ.


### Bổ sung prompt — quyền quản trị database dùng chung (24/10/2026)

- Yêu cầu: “nâng cấp database lên tài khoản quản trị”. Xác nhận phạm vi: “Cả tv_thanhtai và tv_quocanh”.
- Đã cấp db_owner cho hai tài khoản trong RentalShop và kiểm tra tư cách thành viên thành công. Không cấp sysadmin trên SQL Server; giữ nguyên mật khẩu. Hai tài khoản có quyền tạo/sửa/xóa bảng và dữ liệu, chạy migration trong RentalShop.
