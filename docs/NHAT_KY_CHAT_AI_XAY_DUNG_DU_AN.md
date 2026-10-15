# NHẬT KÝ TƯƠNG TÁC VÀ PROMPT CHAT VỚI AI XÂY DỰNG DỰ ÁN
## ĐỒ ÁN: WEBSITE BÁN HÀNG THEO MÔ HÌNH CHUỖI CỬA HÀNG (BLOOM FLOWER CHAIN)
### Công nghệ: Spring Boot + Thymeleaf + Bootstrap + JPA + SQL Server + WebSocket + JWT + Cloudinary

---

> **Ghi chú nộp bài**: 
> Tài liệu này tổng hợp toàn bộ các câu lệnh (prompts), ngữ cảnh yêu cầu và tiến trình trao đổi với Trợ lý AI (Google DeepMind Antigravity / Gemini) trong suốt quá trình phân tích, thiết kế, lập trình, kiểm thử và tối ưu hóa hệ thống website bán hoa tươi chuỗi chi nhánh.
> 
> *Các đoạn hội thoại được chọn lọc nghiêm ngặt, chỉ bao gồm các tương tác phục vụ trực tiếp cho việc xây dựng dự án web bán hàng.*

---

## MỤC LỤC TIẾN TRÌNH PHÁT TRIỂN

1. [Giai đoạn 1: Khởi tạo kiến trúc, thiết lập cơ sở dữ liệu & Mô hình chuỗi](#giai-doan-1)
2. [Giai đoạn 2: Bảo mật phân quyền kép (Session CSRF + JWT) & Xác thực OTP](#giai-doan-2)
3. [Giai đoạn 3: Phân hệ Khách hàng, Danh mục hoa & Đặt hàng đa chi nhánh](#giai-doan-3)
4. [Giai đoạn 4: Phân hệ Quản lý chi nhánh (Manager / Vendor UC_017 - UC_026)](#giai-doan-4)
5. [Giai đoạn 5: Tích hợp công nghệ nâng cao: WebSocket Realtime Chat & Cloudinary](#giai-doan-5)
6. [Giai đoạn 6: Phân hệ Quản trị viên (Admin) & Thống kê báo cáo chuỗi](#giai-doan-6)
7. [Giai đoạn 7: Kiểm thử tự động (540+ Unit & Integration Tests)](#giai-doan-7)
8. [Giai đoạn 8: Tinh chỉnh giao diện, trải nghiệm người dùng & Gỡ lỗi thực tế](#giai-doan-8)

---

<a name="giai-doan-1"></a>
## GIAI ĐOẠN 1: KHỞI TẠO KIẾN TRÚC, THIẾT LẬP CƠ SỞ DỮ LIỆU & MÔ HÌNH CHUỖI

### Prompt 1.1: Thiết kế cấu trúc dự án Spring Boot và chọn công nghệ
* **Người dùng (Prompt)**:
  > "Tôi cần xây dựng đồ án website bán hàng theo mô hình chuỗi cửa hàng hoa tươi bằng Spring Boot, giao diện Thymeleaf + Bootstrap 5, ORM JPA, CSDL SQL Server, hỗ trợ WebSocket, JWT và Cloudinary. Hãy phân tích cấu trúc gói (package structure) chuẩn MVC 3 lớp và file `pom.xml` với các dependency cần thiết."
* **AI phản hồi & thực thi**:
  - Khởi tạo `pom.xml` với Spring Boot starter parent, Java 21 LTS, các starters: `web`, `thymeleaf`, `data-jpa`, `security`, `oauth2-resource-server`, `websocket`, `flyway-sqlserver`, `mssql-jdbc` và `cloudinary-http44`.
  - Phân chia kiến trúc thư mục chuẩn:
    - `config/`, `security/`: Cấu hình hệ thống, bảo mật kép, WebSocket.
    - `controller/`: Chia package theo actor: `customer/`, `manager/`, `admin/`, `api/`.
    - `service/`, `service/impl/`: Tách interface và implementation xử lý nghiệp vụ.
    - `repository/`: Spring Data JPA interfaces.
    - `entity/`: Các thực thể ánh xạ CSDL quan hệ.
    - `dto/`: Request/Response data transfer objects.

### Prompt 1.2: Thiết kế Entity CSDL cho mô hình chuỗi cửa hàng
* **Người dùng (Prompt)**:
  > "Trong mô hình chuỗi cửa hàng hoa tươi, có sản phẩm dùng chung toàn chuỗi và có sản phẩm riêng của từng chi nhánh. Mỗi chi nhánh có tồn kho độc lập. Hãy thiết kế các Entity JPA: `Store`, `Product`, `StoreInventory`, `Category`, `Style`, `StyleValue`, `Order`, `OrderItem` với các mối quan hệ quan hệ 1-N, N-N chuẩn xác."
* **AI phản hồi & thực thi**:
  - Thiết kế entity `Store` (quản lý chi nhánh, hotline, địa chỉ, trạng thái hoạt động).
  - Thiết kế `StoreInventory` với khóa phức hợp hoặc liên kết `@ManyToOne` tới `Store` và `Product` để lưu trữ số lượng tồn kho theo từng chi nhánh cụ thể.
  - Thiết kế `Product` có trường nullable `store` (nếu null là mẫu chung toàn chuỗi, nếu có giá trị là mẫu riêng của chi nhánh).
  - Tách bạch phân loại hoa: `Category` (Dịp tặng chính/bổ sung) và `StyleValue` (thuộc tính hoa: Loại hoa, Màu sắc, Phong cách, Kiểu dáng, Kích cỡ).

### Prompt 1.3: Quản lý phiên bản CSDL bằng Flyway Migration
* **Người dùng (Prompt)**:
  > "Để tránh lỗi schema bị thay đổi tùy tiện khi restart server và đảm bảo tính toàn vẹn dữ liệu, hãy cấu hình Flyway migration trên SQL Server từ V1 đến V8, tắt ddl-auto của Hibernate."
* **AI phản hồi & thực thi**:
  - Thiết lập `spring.jpa.hibernate.ddl-auto=none` trong `application.properties`.
  - Tạo các file migration SQL trong `src/main/resources/db/migration/sqlserver/`:
    - `V1__init_schema.sql`: Khởi tạo bảng cốt lõi.
    - `V2` đến `V6`: Bổ sung bảng chat, review, ràng buộc khóa ngoại và cột chi nhánh.
    - `V7`: Chuyển đổi danh mục theo dịp tặng và bảo toàn danh mục cũ.
    - `V8`: Bổ sung cờ trạng thái kinh doanh/xóa theo chi nhánh trên `store_inventories`.

---

<a name="giai-doan-2"></a>
## GIAI ĐOẠN 2: BẢO MẬT PHÂN QUYỀN KÉP (SESSION CSRF + JWT) & XÁC THỰC OTP

### Prompt 2.1: Triển khai kiến trúc bảo mật kép (Dual Security Configuration)
* **Người dùng (Prompt)**:
  > "Đề bài yêu cầu vừa có Thymeleaf vừa có JWT. Làm thế nào để giao diện web dùng Session Cookie + CSRF bảo vệ form, trong khi các endpoint REST API `/api/**` được bảo vệ độc lập bằng JWT Bearer Token không sử dụng Session?"
* **AI phản hồi & thực thi**:
  - Thiết lập 2 `SecurityFilterChain` riêng biệt trong Spring Security:
    1. **JWT Filter Chain** (`@Order(1)`): Bắt các request khớp pattern `/api/**`, cấu hình `SessionCreationPolicy.STATELESS`, vô hiệu hóa CSRF cho API, tích hợp bộ giải mã Bearer Token.
    2. **Web MVC Filter Chain** (`@Order(2)`): Bắt các request web thông thường, bật CSRF token cho form Thymeleaf, cấu hình form login tùy chỉnh (`/auth/login`), remember-me và xử lý phân quyền URL theo role: `ADMIN`, `VENDOR`, `STAFF`, `USER`.

### Prompt 2.2: Xây dựng cơ chế xác thực JWT với Refresh Token Rotation
* **Người dùng (Prompt)**:
  > "Hãy hiện thực đầy đủ luồng xác thực JWT tại package `com.finalproject.controller.api`: cấp Access Token (15 phút) và Refresh Token (7 ngày), hỗ trợ xoay token (rotation) và thu hồi (revoke) khi đăng xuất."
* **AI phản hồi & thực thi**:
  - Viết `JwtTokenService` sử dụng thuật toán HMAC-SHA256 ký token với secret từ biến môi trường.
  - Tạo entity `JwtSession` và `JwtRefreshToken` lưu hash trong CSDL để phát hiện token bị đánh cắp hoặc tái sử dụng trái phép (replay attack).
  - Xây dựng [JwtAuthController.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/controller/api/JwtAuthController.java) với 4 endpoints:
    - `POST /api/auth/login`
    - `POST /api/auth/refresh`
    - `GET /api/auth/me`
    - `POST /api/auth/logout`

### Prompt 2.3: Gửi mã xác thực OTP qua Email khi quên mật khẩu
* **Người dùng (Prompt)**:
  > "Xây dựng chức năng quên mật khẩu: Khách hàng nhập email, hệ thống gửi mã OTP 6 chữ số qua Email. Người dùng nhập đúng OTP mới được đặt lại mật khẩu mới. Lưu ý: Tuyệt đối không in mã OTP ra console log hoặc truyền qua URL."
* **AI phản hồi & thực thi**:
  - Tạo `OtpService` sinh mã 6 số ngẫu nhiên an toàn, đặt thời hạn hết hạn 5 phút và lưu mã đã băm.
  - Tích hợp `JavaMailSender` định dạng HTML email chuyên nghiệp gửi tới hòm thư người dùng.
  - Tạo form đổi mật khẩu yêu cầu nhập `email`, `otp`, `newPassword` đồng thời và mã hóa mật khẩu mới bằng `BCryptPasswordEncoder`.

---

<a name="giai-doan-3"></a>
## GIAI ĐOẠN 3: PHÂN HỆ KHÁCH HÀNG, DANH MỤC HOA & ĐẶT HÀNG ĐA CHI NHÁNH

### Prompt 3.1: Trang chủ và bộ lọc hoa thông minh đa tiêu chí
* **Người dùng (Prompt)**:
  > "Xây dựng trang chủ hiển thị: Banner sự kiện, danh mục khám phá theo dịp (Sinh nhật, Tình yêu, Khai trương...), danh sách hoa bán chạy và bộ lọc đa tiêu chí: lọc theo chi nhánh còn hàng, theo khoảng giá, loại hoa, màu sắc, phong cách thiết kế."
* **AI phản hồi & thực thi**:
  - Viết `ProductController` và `ProductSpecification` hỗ trợ truy vấn động Spring Data JPA.
  - Tích hợp nhãn số lượng tồn kho theo thời gian thực trên từng thẻ hoa; nếu hết hàng thì tự động đổi nhãn sang "Hết hàng" và khóa nút "Thêm vào giỏ".

### Prompt 3.2: Giỏ hàng và logic trừ tồn kho tức thời (Stock Reservation)
* **Người dùng (Prompt)**:
  > "Nghiệp vụ yêu cầu: Khi khách hàng đặt đơn thành công (kể cả chọn hình thức COD thanh toán khi nhận hàng), hệ thống phải trừ tồn kho ngay tại chi nhánh được chọn, không chờ đến khi duyệt đơn hay bắt đầu giao. Nếu khách hủy đơn trước khi cắm hoa thì phải hoàn kho đúng 1 lần. Hãy cài đặt logic này an toàn chống race condition."
* **AI phản hồi & thực thi**:
  - Viết service đặt hàng trong transaction `@Transactional(isolation = Isolation.REPEATABLE_READ)`.
  - Sử dụng Pessimistic Write Lock (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) trên bảng `StoreInventory` khi truy vấn kiểm tra số lượng, đảm bảo khi 2 khách hàng cùng đặt bông hoa cuối cùng thì không bị âm kho.
  - Viết hàm `reconcileInventoryOnCancel()` kiểm tra trạng thái đơn trước khi hoàn kho, ghi log chi nhánh và cập nhật số lượng chính xác 1 lần duy nhất.

### Prompt 3.3: Phương thức nhận hàng: Giao tận nơi hoặc Nhận tại quầy
* **Người dùng (Prompt)**:
  > "Tại trang Checkout, khách có thể chọn 1 trong 2 hình thức:
  > 1. Giao tận nơi: Tự động điều phối chi nhánh gần nhất có đủ hàng và tính phí vận chuyển theo bảng biểu phí.
  > 2. Nhận tại quầy: Khách tự chọn chi nhánh mong muốn đến nhận hoa.
  > Hãy hoàn thiện luồng checkout này."
* **AI phản hồi & thực thi**:
  - Hoàn thiện giao diện [checkout.html](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/templates/customer/checkout.html) với radio chuyển đổi hình thức nhận hàng linh hoạt.
  - Tích hợp `DeliveryFeeService` tính phí giao hàng dựa trên đơn vị vận chuyển và khoảng cách chi nhánh.

---

<a name="giai-doan-4"></a>
## GIAI ĐOẠN 4: PHÂN HỆ QUẢN LÝ CHI NHÁNH (MANAGER / VENDOR UC_017 - UC_026)

### Prompt 4.1: Dashboard thống kê KPI và doanh thu chi nhánh
* **Người dùng (Prompt)**:
  > "Xây dựng màn hình Dashboard cho Quản lý cửa hàng (/manager/dashboard) hiển thị 4 chỉ số KPI quan trọng: Đơn hàng mới, Doanh thu trong kỳ, Số mẫu hoa kinh doanh, và Tồn kho còn lại của chi nhánh. Kèm theo biểu đồ doanh thu Chart.js có thể lọc theo: Hôm nay, 7 ngày, 30 ngày, hoặc khoảng ngày tùy chọn."
* **AI phản hồi & thực thi**:
  - Viết `ManagerDashboardController` lấy dữ liệu được scope chặt chẽ theo `store_id` của tài khoản quản lý đăng nhập.
  - Thiết kế 4 thẻ KPI đồng bộ trên 1 hàng với màu sắc thương hiệu hoa tươi, hiển thị tăng trưởng và số liệu tức thời.
  - Tích hợp Chart.js hỗ trợ chuyển đổi giữa biểu đồ Cột (Bar), Đường (Line), Tròn (Doughnut).

### Prompt 4.2: Quy trình quản lý tiến độ đơn hàng hoa (Order Processing Flow)
* **Người dùng (Prompt)**:
  > "Quy trình xử lý đơn hoa tại chi nhánh gồm các bước:
  > Chờ xác nhận -> Đã xác nhận -> Đang cắm hoa/Chuẩn bị (PREPARING) -> Đang giao -> Đã giao -> Hoàn tất.
  > Khi cửa hàng đã chuyển sang 'Đang cắm hoa', khách hàng không được quyền tự hủy đơn. Quản lý hủy đơn ngoại lệ bắt buộc phải nhập lý do tối đa 300 ký tự. Hãy cài đặt quy trình này."
* **AI phản hồi & thực thi**:
  - Thiết lập enum `OrderStatus`: `PENDING`, `CONFIRMED`, `PREPARING`, `SHIPPING`, `DELIVERED`, `COMPLETED`, `CANCELLED`.
  - Trong [ManagerOrderController.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/controller/manager/ManagerOrderController.java):
    - Kiểm tra điều kiện hủy phía backend: Chặn request hủy của khách nếu trạng thái `>= PREPARING`.
    - Modal popup hủy đơn của quản lý bắt buộc trường `cancelReason` (not empty, max 300 chars).
    - Lưu vết người hủy (`cancelled_by`) và lý do hủy để hiển thị minh bạch cho khách hàng.

### Prompt 4.3: Quản lý ví chi nhánh và yêu cầu rút tiền doanh thu
* **Người dùng (Prompt)**:
  > "Tạo chức năng Quản lý ví cửa hàng (/manager/wallet): Quản lý xem được số dư khả dụng từ các đơn hàng đã hoàn tất, lịch sử biến động số dư và gửi yêu cầu rút tiền về tài khoản ngân hàng."
* **AI phản hồi & thực thi**:
  - Tạo entity `Wallet` và `Transaction` liên kết với `Store`.
  - Xây dựng form yêu cầu rút tiền với kiểm tra số dư khả dụng (`availableBalance >= withdrawAmount`), ghi nhận giao dịch ở trạng thái `PENDING_APPROVAL` để Admin phê duyệt.

---

<a name="giai-doan-5"></a>
## GIAI ĐOẠN 5: TÍCH HỢP CÔNG NGHỆ NÂNG CAO: WEBSOCKET REALTIME CHAT & CLOUDINARY

### Prompt 5.1: Cấu hình WebSocket và STOMP cho Chat thời gian thực
* **Người dùng (Prompt)**:
  > "Hãy cấu hình WebSocket STOMP trong Spring Boot để khách hàng có thể chat trực tiếp với chi nhánh cửa hàng. Tin nhắn phải gửi tức thì không cần tải lại trang, có hỗ trợ gửi ảnh và phân biệt hội thoại theo từng chi nhánh."
* **AI phản hồi & thực thi**:
  - Tạo [WebSocketConfig.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/config/WebSocketConfig.java):
    - Đăng ký endpoint `/ws-chat` có SockJS fallback.
    - Cấu hình prefix message broker: `/topic` (pub-sub) và application prefix: `/app`.
  - Tạo entity `ChatSession`, `ChatMessage` lưu lịch sử tin nhắn trong SQL Server.
  - Xây dựng kênh tin nhắn: `/topic/messages.{chatSessionId}` và thông báo cho người bán qua `/topic/store.{storeId}`.

### Prompt 5.2: Tích hợp dịch vụ lưu trữ đám mây Cloudinary an toàn
* **Người dùng (Prompt)**:
  > "Tích hợp Cloudinary để tải lên ảnh sản phẩm hoa tươi, ảnh đại diện cửa hàng và ảnh tin nhắn chat. Yêu cầu: Không lưu cứng (hardcode) API Key và Secret trong mã nguồn; tự động đọc từ file `.env` cục bộ."
* **AI phản hồi & thực thi**:
  - Tạo [CloudinaryService.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/service/CloudinaryService.java) nạp cấu hình `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` từ environment variables.
  - Tạo hàm `uploadImage(MultipartFile, folder)` trả về `secure_url` và `public_id` để tiện quản lý và xóa ảnh khi cần.
  - Bổ sung file mẫu `.env.example` và đưa `.env` vào `.gitignore` để bảo vệ thông tin mật.

---

<a name="giai-doan-6"></a>
## GIAI ĐOẠN 6: PHÂN HỆ QUẢN TRỊ VIÊN (ADMIN) & THỐNG KÊ BÁO CÁO CHUỖI

### Prompt 6.1: Thống kê tổng hợp toàn chuỗi cho Quản trị viên
* **Người dùng (Prompt)**:
  > "Xây dựng trang Dashboard Admin (/admin/dashboard) tổng hợp số liệu toàn hệ thống: Tổng doanh thu chuỗi, số lượng đơn hàng, người dùng mới và chi nhánh mới tham gia. Hỗ trợ biểu đồ so sánh giữa các chi nhánh."
* **AI phản hồi & thực thi**:
  - Viết repository truy vấn gom nhóm dữ liệu theo ngày/tháng/năm và tính toán tổng số liệu chuỗi.
  - Phân quyền bảo vệ: Chỉ tài khoản có vai trò `ROLE_ADMIN` mới truy cập được các URL bắt đầu bằng `/admin/**`.

### Prompt 6.2: Quản lý danh mục, thuộc tính và biểu phí vận chuyển
* **Người dùng (Prompt)**:
  > "Tách các chức năng cấu hình của Admin thành các màn hình độc lập: Quản lý danh mục hoa (Category), Quản lý thuộc tính/phong cách hoa (Style/StyleValue), Quản lý đơn vị giao hàng và biểu phí vùng (Delivery), Quản lý cấp bậc thành viên (UserLevel, StoreLevel)."
* **AI phản hồi & thực thi**:
  - Tách `AdminConfigController` lớn thành các controller chuyên biệt:
    - `AdminCategoryController`: Thêm/sửa/ẩn danh mục.
    - `AdminStyleController`: Quản lý các nhóm đặc tính hoa.
    - `AdminDeliveryController`: Cấu hình đơn vị giao hàng và cước phí theo khu vực.
    - `AdminUserLevelController`, `AdminStoreLevelController`: Quản lý cấp bậc và chính sách ưu đãi.

---

<a name="giai-doan-7"></a>
## GIAI ĐOẠN 7: KIỂM THỬ TỰ ĐỘNG (540+ UNIT & INTEGRATION TESTS)

### Prompt 7.1: Xây dựng bộ kiểm thử nghiệp vụ và bảo mật
* **Người dùng (Prompt)**:
  > "Để chuẩn bị nghiệm thu đồ án, hãy viết các bộ kiểm thử tự động (JUnit 5 + Mockito + SpringBootTest) kiểm tra toàn bộ: Đăng nhập sai quá số lần, bảo vệ JWT token giả mạo, kiểm tra giỏ hàng, hủy đơn hoàn kho và kiểm thử luồng đặt hàng đồng thời."
* **AI phản hồi & thực thi**:
  - Xây dựng hơn 540 ca kiểm thử tự động trong `src/test/java/com/finalproject/`:
    - `JwtAuthenticationTests`: Kiểm tra token hết hạn, sai chữ ký, vô hiệu hóa tài khoản.
    - `ConcurrencyRegressionTests`: Mô phỏng 20 luồng đồng thời tranh chấp mua cùng một mẫu hoa tồn kho giới hạn.
    - `ProductCatalogFlowTests`: Kiểm tra phân trang, bộ lọc danh mục và thuộc tính.
    - `BusinessRegressionTests`: Kiểm tra chính sách hủy đơn, hoàn tiền và tính cước vận chuyển.
  - Kết quả kiểm thử chạy qua Maven Wrapper: **547 ca test đạt, 0 failure, 0 error (BUILD SUCCESS)**.

---

<a name="giai-doan-8"></a>
## GIAI ĐOẠN 8: TINH CHỈNH GIAO DIỆN, TRẢI NGHIỆM NGƯỜI DÙNG & GỠ LỖI THỰC TẾ

### Prompt 8.1: Canh chỉnh hàng hiển thị các thẻ KPI Dashboard
* **Người dùng (Prompt)**:
  > "Cho các KPIs này nằm cùng 1 hàng."
* **AI phản hồi & thực thi**:
  - Chỉnh sửa CSS Flexbox / CSS Grid trong [manager/dashboard.html](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/templates/manager/dashboard.html) và [admin/dashboard.html](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/templates/admin/dashboard.html).
  - Đưa 4 thẻ chỉ số KPI dàn đều thành một hàng ngang trên màn hình Desktop, tự động xuống dòng mượt mà trên thiết bị di động.

### Prompt 8.2: Đặt lại tên hoa thực tế, tránh tên trừu tượng
* **Người dùng (Prompt)**:
  > "Đặt lại tên toàn bộ sản phẩm của cửa hàng cho đúng và dễ hiểu, không đặt tên trừu tượng như 'Bó Hoa Hướng Dương Ánh Dương Rạng Rỡ'."
* **AI phản hồi & thực thi**:
  - Viết script cập nhật toàn bộ tên sản phẩm trong CSDL SQL Server `RentalShop`.
  - Chuyển đổi các tên mỹ từ trừu tượng thành tên hoa thực tế, rõ ràng như: *"Bó hoa hướng dương tươi"*, *"Bó hoa hồng đỏ Đà Lạt"*, *"Giỏ hoa cẩm tú cầu xanh"*, *"Lẵng hoa khai trương phát tài"*.

### Prompt 8.3: Nút bấm sổ xuống (Dropdown) chọn danh mục & thuộc tính hoa
* **Người dùng (Prompt)**:
  > "Lỗi khi thêm sản phẩm, tôi muốn nó có nút bấm sổ xuống để có thể lựa chọn thay vì phải gõ tay."
* **AI phản hồi & thực thi**:
  - Xây dựng component [fragments/flower-attributes.html](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/templates/fragments/flower-attributes.html) kèm [flower-attributes.js](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/static/js/flower-attributes.js).
  - Tích hợp nút dropdown sổ xuống danh sách các tùy chọn có sẵn (Loại hoa, Màu sắc, Phong cách, Dịp tặng), cho phép người dùng click chọn nhanh để tạo tag trực quan.

### Prompt 8.4: Sửa lỗi 413 FileCountLimitExceededException khi upload ảnh
* **Người dùng (Prompt)**:
  > "Khi thêm sản phẩm bị lỗi upload file."
* **AI phản hồi & thực thi**:
  - Phân tích nguyên nhân: Form gửi nhiều input file rỗng vượt quá giới hạn cấu hình của Tomcat.
  - Sửa `ProductForm.java` và cấu hình `spring.servlet.multipart` trong `application.properties`, loại bỏ các input file trùng lặp và xử lý upload đa file ổn định.

### Prompt 8.5: Bảo toàn thuộc tính cũ khi chỉnh sửa sản phẩm
* **Người dùng (Prompt)**:
  > "Bạn giúp tôi, sau khi tôi sửa các thuộc tính thì các thuộc tính cũ của tôi bị mất đi."
* **AI phản hồi & thực thi**:
  - Tìm ra nguyên nhân gốc rễ:
    1. Lệch tên alias: Trong CSDL lưu `"Phong cách thiết kế"` nhưng form tìm chuỗi `"Phong cách"`, làm ô nhập bị rỗng và khi submit ghi đè giá trị rỗng vào CSDL.
    2. Hàm `assignText` trong `ProductAttributeService` trước đây bỏ qua các nhóm không có trong request, dẫn đến xóa nhầm thuộc tính cũ.
  - Khắc phục:
    - Bổ sung chuẩn hóa tên nhóm qua [FlowerAttributes.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/utils/FlowerAttributes.java).
    - Cập nhật [ProductAttributeService.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/service/ProductAttributeService.java) giữ nguyên giá trị cũ nếu trường bị bỏ qua.
    - Thêm hàm [Product.getSelectedStyleValueIds()](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/entity/Product.java#L236) để pre-check chính xác mọi checkbox và tag cũ lên giao diện.

### Prompt 8.6: Dọn dẹp dự án chuẩn bị nộp bài
* **Người dùng (Prompt)**:
  > "Bạn kiểm tra giúp tôi xóa toàn bộ các file không cần đến trong dự án và tự động tạo trong quá trình biên dịch."
* **AI phản hồi & thực thi**:
  - Chạy `mvn clean` xóa sạch toàn bộ thư mục `target/` (~86 MB).
  - Xóa thư mục tạm `.tmp/` và các file zip cài đặt dư thừa trong `.tools/` (~240 MB).
  - Xóa công cụ phụ trợ `rubric-audit` sau khi hoàn tất kiểm tra.
  - Đảm bảo cây thư mục Git sạch sẽ (`working tree clean`), sẵn sàng đóng gói nộp bài.

---

## KẾT LUẬN & ĐÁNH GIÁ KẾT QUẢ ỨNG DỤNG AI

Quá trình phối hợp làm việc với AI trong đồ án này tuân thủ các nguyên tắc kỹ thuật công nghệ thông tin:
1. **Chủ động định hướng kiến trúc**: Người phát triển đưa ra yêu cầu cụ thể về công nghệ (Spring Boot, JWT, WebSocket, Flyway, Cloudinary) và quy tắc nghiệp vụ chuỗi hoa tươi (trừ kho ngay, phân quyền chi nhánh, quy trình cắm hoa).
2. **Kiểm soát chất lượng bằng Unit Test**: Mọi đoạn mã AI hỗ trợ viết đều được kiểm chứng thông qua hơn 540 ca kiểm thử tự động trước khi hợp nhất vào nhánh `main`.
3. **Bảo mật mã nguồn**: Tuân thủ nguyên tắc không hardcode mật khẩu, token hay API secret vào mã nguồn mà nạp qua biến môi trường.
4. **Giải quyết vấn đề triệt để**: Khi phát sinh lỗi giao diện hoặc logic CSDL, tương tác với AI tập trung vào việc phân tích nguyên nhân gốc rễ (Root Cause Analysis) thay vì chắp vá tạm thời.
