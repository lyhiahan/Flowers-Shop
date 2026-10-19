# TÀI LIỆU TỔNG HỢP TOÀN DIỆN HỆ THỐNG WEBSITE BÁN HÀNG THEO MÔ HÌNH CHUỖI CỬA HÀNG
## DỰ ÁN: CHUỖI CỬA HÀNG HOA TƯƠI BLOOM FLOWER CHAIN
### Công nghệ: Spring Boot + Thymeleaf + Bootstrap + JPA + SQL Server + WebSocket + JWT + Cloudinary

---

> **MỤC ĐÍCH TÀI LIỆU**:
> Tài liệu này tổng hợp toàn bộ các báo cáo kỹ thuật, kiểm toán kiến trúc, đặc tả use cases (UC_017 - UC_026), quy trình nghiệp vụ đơn hàng, cơ chế tồn kho tức thời, bảo mật kép, nhật ký tương tác AI và hướng dẫn vận hành từ 24 tài liệu chuyên đề trước đây thành một tài liệu duy nhất phục vụ nghiệm thu, thuyết minh và chấm điểm đồ án.

---

## MỤC LỤC TỔNG QUAN

1. [Chương 1: Tổng quan Đề tài & Báo cáo Đối chiếu Yêu cầu Công nghệ](#chuong-1)
2. [Chương 2: Kiến trúc Hệ thống & Thiết kế Cơ sở Dữ liệu SQL Server](#chuong-2)
3. [Chương 3: Phân hệ Quản lý Chuỗi Cửa hàng (Use Cases UC_017 - UC_026)](#chuong-3)
4. [Chương 4: Quy trình Nghiệp vụ Đơn hàng, Tồn kho Tức thời & Hoàn kho](#chuong-4)
5. [Chương 5: Phân hệ Bảo mật Kép (Dual Security: Session + JWT) & Xác thực OTP](#chuong-5)
6. [Chương 6: Phân hệ Giao tiếp Realtime STOMP WebSocket & Lưu trữ Cloudinary](#chuong-6)
7. [Chương 7: Phân hệ Quản trị viên (Admin), Biểu phí Vận chuyển & Xử lý Khóa](#chuong-7)
8. [Chương 8: Lịch sử Tinh chỉnh Giao diện, Trải nghiệm Người dùng & Gỡ lỗi](#chuong-8)
9. [Chương 9: Nhật ký Tương tác Prompt Chat với AI (Chiếm 2.0 điểm)](#chuong-9)
10. [Chương 10: Hướng dẫn Cài đặt, Khởi chạy & Kết quả Kiểm thử Tự động](#chuong-10)

---

<a name="chuong-1"></a>
## CHƯƠNG 1: TỔNG QUAN ĐỀ TÀI & BÁO CÁO ĐỐI CHIẾU YÊU CẦU CÔNG NGHỆ

### 1.1. Phạm vi đề tài
Website bán hàng theo mô hình chuỗi cửa hàng hoa tươi (100% mô hình Bán hoa - Không cho thuê). Khách hàng xem sản phẩm, chọn chi nhánh còn hàng, đặt hoa giao tận nơi hoặc nhận tại quầy, thanh toán và chat trực tuyến với cửa hàng/admin. Quản lý xử lý tồn kho, đơn hàng, nhân sự, ví chi nhánh và doanh thu theo quyền tại từng chi nhánh. Admin quản trị toàn chuỗi.

### 1.2. Bảng đối chiếu mức đáp ứng công nghệ theo đề cương

#### Bảng kiểm chứng các công nghệ cốt lõi:
| Yêu cầu công nghệ | Mức độ đáp ứng | Bằng chứng mã nguồn & Hiện trạng |
|:---|:---:|:---|
| **Chuỗi cửa hàng** | **Đã triển khai; còn đối soát kho cũ** | `Store`, `StoreInventory`, `StoreMembership`, `StoreAccessService`; chọn chi nhánh, phân quyền theo cửa hàng |
| **Spring Boot** | **Đã triển khai** | `pom.xml`, `FinalprojectApplication`, Spring Boot 4.1.1 trên Java 21 |
| **Thymeleaf** | **Đã triển khai** | Giao diện `customer/`, `manager/`, `admin/`, `fragments/` |
| **Bootstrap** | **Đã triển khai** | Bootstrap 5.3.3 CSS/JS và Bootstrap Icons |
| **JPA (Hibernate)** | **Đã triển khai** | Entity, Spring Data JPA, transaction và khóa bi quan |
| **SQL Server** | **Đã triển khai; RentalShop cục bộ V10** | `mssql-jdbc`; mã nguồn Flyway V1–V10, các môi trường khác cần kiểm tra riêng |
| **WebSocket** | **Đã triển khai** | Endpoint `/ws` và `/ws-chat`, tin nhắn realtime theo quyền chi nhánh |
| **JWT** | **Đã triển khai** | Xác thực stateless cho `/api/**`; web Thymeleaf dùng session riêng |
| **Cloudinary** | **Đã triển khai** | Upload và lưu `secure_url`, `public_id`; test thật cần bật riêng |


### 1.3. Kết luận kiểm toán hệ thống (System Audit)
- 238 file Java và 74 template HTML tại thời điểm hoàn thiện.
- Các luồng nghiệp vụ chính có kiểm thử tự động; xem [báo cáo kiểm thử mới nhất](PROJECT_REVIEW_20261016.md) trước khi kết luận đạt.
- CSDL SQL Server gốc `RentalShop` được bảo toàn toàn vẹn dữ liệu, các bảng được chuẩn hóa schema và kiểm soát qua migration.


---

<a name="chuong-2"></a>
## CHƯƠNG 2: KIẾN TRÚC HỆ THỐNG & THIẾT KẾ CƠ SỞ DỮ LIỆU SQL SERVER

### 2.1. Kiến trúc phân tầng (N-Tier Enterprise Architecture)
Hệ thống được tổ chức thành 5 tầng rõ rệt:
1. **Client Presentation Layer**: Giao diện Thymeleaf, Bootstrap 5.3.3, SockJS client.
2. **Security Gateway**: Cấu hình kép gồm Web MVC Session Chain (CSRF) và REST API JWT Chain (Stateless Bearer).
3. **Controller Layer**: Public/Customer Controllers, Manager Controllers, Admin Controllers, WebSocket Chat Controller.
4. **Business Logic & Service Layer**: OrderService (trừ kho tức thời, hoàn kho), StoreService, ProductAttributeService, CloudinaryService, MailOtpService.
5. **Data Persistence Layer**: Spring Data JPA Repositories kết nối CSDL Microsoft SQL Server `RentalShop` quản lý qua Flyway.

*(Xem sơ đồ kiến trúc chi tiết tại `docs/diagrams/diagram_architecture.png` hoặc tài liệu Word `.docx`).*

### 2.2. Lịch sử Migration CSDL bằng Flyway (V1 - V10)
Hệ thống quản lý schema `dbo` trên SQL Server bằng Flyway với các phiên bản:
- **V1**: Khởi tạo schema gốc từ toàn bộ Entity (bảng `stores`, `products`, `categories`, `orders`, `order_items`, `users`...).
- **V2**: Bổ sung index tối ưu truy vấn và ràng buộc toàn vẹn dữ liệu.
- **V3**: Chuyển đổi và tinh giản hệ thống vai trò người dùng (loại bỏ vai trò Shipper riêng biệt, tích hợp quản lý chi nhánh).
- **V4**: Bổ sung bảng Chat realtime (`chat_sessions`, `chat_messages`), hạn chót thanh toán và ràng buộc đánh giá sản phẩm.
- **V5 - V6**: Bổ sung bảng nhật ký giám sát, thẻ sản phẩm trong khung chat.
- **V7**: Chuyển đổi danh mục theo nhiều dịp tặng (Tình yêu, Sinh nhật, Khai trương, Cưới...) và bảo toàn danh mục cũ vào `product_category_legacy`.
- **V8**: Bổ sung cờ trạng thái kinh doanh/xóa theo chi nhánh trên `store_inventories` phục vụ tính năng xóa mềm.
- **V9**: Mở rộng ràng buộc phương thức thanh toán cho môi trường demo.
- **V10**: Bổ sung cột và đối soát tiền MoMo demo; đã thử trên bản sao và áp dụng cho `RentalShop` cục bộ ngày 19/10/2026 sau bản sao lưu. Bản dữ liệu này không có đơn `MOMO_DEMO` cũ để backfill.

### 2.3. Khắc phục kết nối SQL Server Local
- Database `RentalShop` nằm trên instance `SQLEXPRESS`; cổng TCP hiện tại là `53735`.
- URL mặc định profile `dev` dùng cổng đó. Nếu cổng thay đổi, truyền `SPRING_DATASOURCE_URL` với cổng mới.
- Credential được bảo vệ bằng mã hóa Windows DPAPI trong `.tools/sqlserver-local.json` hoặc nạp qua biến môi trường.


---

<a name="chuong-3"></a>
## CHƯƠNG 3: PHÂN HỆ QUẢN LÝ CHUỖI CỬA HÀNG (USE CASES UC_017 - UC_026)

### 3.1. Danh mục các Use Case Quản lý Cửa hàng
| Mã Use Case | Tên Chức năng | Phân quyền (Actor) | Mô tả chi tiết |
|:---|:---|:---:|:---|
| **UC_017** | **Tạo cửa hàng mới** | Vendor / Admin | Đăng ký chi nhánh mới, nhập tên shop, hotline, địa chỉ, ảnh đại diện, ảnh bìa, ảnh đặc trưng và chọn chính sách hoa hồng |
| **UC_018** | **Quản lý danh sách cửa hàng** | Admin / Vendor | Admin xem và cấp phép/cấm toàn chuỗi; Vendor xem danh sách các cửa hàng sở hữu và hợp tác |
| **UC_019** | **Hồ sơ thông tin cửa hàng** | Vendor / Manager | Cập nhật thông tin chi nhánh, hotline, trạng thái mở/đóng cửa (`isOpen`), ảnh đại diện và ảnh bìa |
| **UC_020** | **Quản lý nhân viên chi nhánh** | Vendor / Manager | Thêm nhân sự mới vào chi nhánh (Quản lý đối tác hoặc Nhân viên giao hàng), phân công và xóa khỏi shop |
| **UC_021** | **Quản lý mẫu hoa chi nhánh** | Vendor / Manager | Xem danh mục hoa, thêm mẫu mới, chỉnh sửa thông tin/giá, chuyển đổi trạng thái kinh doanh hoặc lưu trữ |
| **UC_022** | **Quản lý tồn kho chi nhánh** | Vendor / Manager | Điều chỉnh số lượng tồn kho `store_inventories.quantity` của từng mẫu hoa tại chi nhánh |
| **UC_023** | **Quản lý đơn hàng chi nhánh** | Vendor / Manager | Lọc đơn theo tab: Chờ xác nhận, Cắm hoa, Đang giao, Đã giao, Hoàn tất, Đã hủy; cập nhật tiến độ đơn |
| **UC_024** | **Quản lý ví chi nhánh** | Vendor / Manager | Theo dõi số dư ví doanh thu khả dụng, số dư đóng băng và lịch sử biến động số dư giao dịch |
| **UC_025** | **Báo cáo doanh thu chi nhánh** | Vendor / Manager | Thống kê doanh thu theo đơn hàng/sản phẩm, theo giờ/ngày/tháng/năm và biểu đồ Cột/Đường/Tròn |
| **UC_026** | **Rút tiền từ ví chi nhánh** | Vendor / Manager | Tạo yêu cầu rút tiền về tài khoản ngân hàng từ số dư khả dụng, gửi Admin phê duyệt |

### 3.2. Cơ chế Xóa mềm (Soft Delete) tại chi nhánh
- Xóa sản phẩm tại chi nhánh là **xóa mềm**: Giữ nguyên số lượng kho, ảnh Cloudinary và lịch sử đơn hàng cũ; không gọi lệnh xóa vật lý khỏi CSDL.
- Mẫu chung toàn chuỗi: Chỉ bị ẩn tại chi nhánh thực hiện thao tác, các chi nhánh khác vẫn mở bán bình thường.
- Mẫu riêng của chi nhánh: Bị ẩn khỏi gian hàng công khai và chuyển vào tab "Đã xóa". Có thể khôi phục về trạng thái "Ngừng bán" để kiểm tra trước khi chủ động mở bán lại.


---

<a name="chuong-4"></a>
## CHƯƠNG 4: QUY TRÌNH NGHIỆP VỤ ĐƠN HÀNG, TỒN KHO TỨC THỜI & HOÀN KHO

### 4.1. Quy tắc Trừ tồn kho tức thời (Stock Reservation)
- **Trừ kho ngay lập tức**: Khi đơn hàng được đặt thành công, hệ thống trừ ngay số lượng tại `store_inventories.quantity` của chi nhánh xử lý, áp dụng cho cả hình thức thanh toán khi nhận hàng (COD). Không chờ đến khi duyệt đơn hay bắt đầu giao mới trừ.
- **Chống bán vượt tồn kho (Race Condition)**: Sử dụng Khóa bi quan (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) trên bảng `StoreInventory` trong transaction `@Transactional(isolation = Isolation.REPEATABLE_READ)`.
- **Hoàn kho chính xác 1 lần**: Khi đơn hàng bị hủy trước bước cắm hoa, hệ thống tự động hoàn trả số lượng về đúng chi nhánh trên đơn và ghi log hoàn kho.

### 4.2. Quy trình Vòng đời Đơn hàng hoa tại Chi nhánh
```text
[PENDING] (Chờ xác nhận)
   │
   ▼
[CONFIRMED] (Đã xác nhận)
   │
   ▼
[PREPARING] (Đang cắm hoa/Chuẩn bị)  <--- KHÓA QUYỀN TỰ HỦY CỦA KHÁCH HÀNG
   │
   ▼
[SHIPPING] (Đang giao hàng)
   │
   ▼
[DELIVERED] (Đã giao hàng)
   │
   ▼
[COMPLETED] (Hoàn tất đơn hàng)
```

- **Quy tắc bảo vệ hoa tươi**: Từ thời điểm chi nhánh chuyển đơn sang trạng thái `PREPARING` (Đang cắm hoa), khách hàng **không thể tự hủy đơn**, kể cả đơn giao tận nơi hay nhận tại quầy, nhằm tránh thiệt hại chi phí hoa tươi đã cắt tỉa.
- **Hủy ngoại lệ của Quản lý**: Quản lý/Admin vẫn có quyền hủy ngoại lệ khi đang cắm hoa nhưng **bắt buộc phải nhập lý do** (tối đa 300 ký tự). Lý do hủy và người thực hiện được lưu trên đơn và gửi thông báo minh bạch cho khách hàng.

### 4.3. Chỉ số KPI Tồn kho Realtime
- Dashboard Quản lý và Admin có chỉ số KPI **Tồn kho còn lại** tự động cập nhật mỗi 5 giây qua AJAX/WebSocket, độc lập với bộ lọc ngày.
- Đây là tổng số lượng thực tế của các mẫu hợp lệ tại chi nhánh đang chọn, không tính các bản ghi đã xóa hoặc chi nhánh khác.


---

<a name="chuong-5"></a>
## CHƯƠNG 5: PHÂN HỆ BẢO MẬT KÉP (DUAL SECURITY: SESSION + JWT) & XÁC THỰC OTP

### 5.1. Kiến trúc Bảo mật Kép (Dual Security Configuration)
Hệ thống thiết lập 2 Security Filter Chain song song:
1. **Web MVC Security Chain (`@Order(2)`)**:
   - Phục vụ giao diện người dùng Thymeleaf.
   - Quản lý phiên qua Cookie Session (`JSESSIONID`).
   - Bắt buộc kiểm tra mã CSRF Token trên mọi form submit POST/PUT/DELETE.
   - Phân quyền URL theo Role: `ROLE_USER`, `ROLE_VENDOR`, `ROLE_ADMIN`.
2. **REST API Security Chain (`@Order(1)`)**:
   - Áp dụng cho các request khớp `/api/**`.
   - Cơ chế hoàn toàn Stateless (`SessionCreationPolicy.STATELESS`), tắt CSRF cho API.
   - Xác thực qua Header: `Authorization: Bearer <accessToken>`.
   - Trả lỗi dạng JSON chuẩn REST.

### 5.2. Quản lý Token JWT và Refresh Token Rotation
- **Access Token**: Ký thuật toán HMAC-SHA256, thời hạn sống ngắn (15 phút), chứa thông tin username, vai trò và ID phiên.
- **Refresh Token Rotation**: Token ngẫu nhiên có thời hạn 7 ngày lưu bản băm (hash) trong CSDL (`JwtRefreshToken`). Khi gọi `/api/auth/refresh`, token cũ bị thu hồi và cấp cặp token mới. Nếu phát hiện token cũ được sử dụng lại (replay attack), toàn bộ phiên của gia đình token đó sẽ bị hủy ngay lập tức.
- Các endpoint API chuẩn:
  - `POST /api/auth/login`: Xác thực và cấp token.
  - `POST /api/auth/refresh`: Cấp mới token.
  - `GET /api/auth/me`: Lấy thông tin tài khoản từ token.
  - `POST /api/auth/logout`: Thu hồi token và đăng xuất.

### 5.3. Xác thực mã OTP qua Email khi Quên Mật Khẩu
- Mã OTP 6 chữ số ngẫu nhiên có hiệu lực trong 5 phút.
- Gửi trực tiếp tới hòm thư người dùng qua dịch vụ `JavaMailSender` với định dạng email HTML trang trọng.
- **Nguyên tắc bảo mật**: Tuyệt đối không in mã OTP ra console log hoặc truyền mã qua tham số URL.

### 5.4. Danh sách tài khoản thử nghiệm theo các vai trò
| Vai trò | Tên đăng nhập | Mật khẩu | Trang đích mặc định |
|:---|:---|:---:|:---|
| **Khách vãng lai (Guest)** | *(Không cần đăng nhập)* | - | `http://localhost:8080/` |
| **Khách hàng (Customer)** | `khachhang` | `123456` | `http://localhost:8080/` |
| **Quản lý chi nhánh Q1 (Vendor)** | `manager_q1` | `123456` | `http://localhost:8080/manager/dashboard` |
| **Quản trị viên (Admin)** | `admin` | `123456` | `http://localhost:8080/admin/dashboard` |


---

<a name="chuong-6"></a>
## CHƯƠNG 6: PHÂN HỆ GIAO TIẾP REALTIME STOMP WEBSOCKET & LƯU TRỮ CLOUDINARY

### 6.1. Giao tiếp thời gian thực qua WebSocket & STOMP
- **Endpoint**: Cấu hình tại [WebSocketConfig.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/config/WebSocketConfig.java) với đường dẫn `/ws-chat`, hỗ trợ SockJS fallback.
- **Kênh thông điệp (Channels)**:
  - Gửi tin: `/app/chat.send`
  - Nhận tin nhắn trong phiên: `/topic/messages.{chatSessionId}`
  - Nhận thông báo tin nhắn mới của chi nhánh: `/topic/store.{storeId}`
- **Tính năng mở rộng**: Hỗ trợ gửi ảnh trong tin nhắn chat, hiển thị thẻ sản phẩm hoa đang thảo luận và Admin có màn hình giám sát toàn bộ các cuộc hội thoại trong chuỗi.

### 6.2. Lưu trữ đa phương tiện trên Cloudinary
- Tích hợp qua SDK `cloudinary-http44` trong [CloudinaryService.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/service/CloudinaryService.java).
- Lưu trữ: Ảnh mẫu hoa tươi, ảnh đại diện người dùng, ảnh bìa cửa hàng, ảnh đặc trưng và ảnh tin nhắn chat.
- Quản lý khóa mật: Nạp qua file môi trường `.env` (`CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`), không lưu hardcode trong mã nguồn Java hay cấu hình Git.


---

<a name="chuong-7"></a>
## CHƯƠNG 7: PHÂN HỆ QUẢN TRỊ VIÊN (ADMIN), BIỂU PHÍ VẬN CHUYỂN & XỬ LÝ KHÓA

### 7.1. Quản lý và Cấp phép Chi nhánh / Sản phẩm
- Admin có thẩm quyền: Phê duyệt (`Active`), Cấm hoạt động (`Suspended`), hoặc Khóa tạm thời cửa hàng khi vi phạm chính sách.
- Khi một cửa hàng bị khóa: Toàn bộ sản phẩm riêng của cửa hàng đó sẽ bị ẩn khỏi trang mua sắm, các đơn hàng đang xử lý được bảo lưu và quản lý bị khóa quyền đăng sản phẩm mới.
- **Bảo vệ tài khoản Admin**: Backend chặn tuyệt đối hành vi tự khóa hoặc đổi quyền của tài khoản `ROLE_ADMIN` (kể cả admin đổi tên khác).

### 7.2. Quản lý Đơn vị Vận chuyển & Biểu phí vùng
- Cấu hình các đơn vị vận chuyển: Hỏa tốc 2H, Tiết kiệm, Giao hàng tiêu chuẩn.
- Biểu phí vận chuyển được phân theo khu vực (nội thành, ngoại thành, liên tỉnh) và có chính sách biểu phí chung toàn chuỗi hoặc biểu phí riêng linh hoạt cho từng chi nhánh.


---

<a name="chuong-8"></a>
## CHƯƠNG 8: LỊCH SỬ TINH CHỈNH GIAO DIỆN, TRẢI NGHIỆM NGƯỜI DÙNG & GỠ LỖI

### 8.1. Căn chỉnh 4 thẻ KPI Dashboard cùng một hàng
- Tinh chỉnh CSS Flexbox và CSS Grid trên [manager/dashboard.html](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/templates/manager/dashboard.html) và [admin/dashboard.html](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/templates/admin/dashboard.html).
- Đưa 4 thẻ chỉ số: *Đơn hàng mới*, *Doanh thu trong kỳ*, *Mẫu hoa kinh doanh* và *Tồn kho còn lại* nằm cân đối, thẳng hàng trên 1 dòng màn hình Desktop, tự co giãn trên thiết bị di động.

### 8.2. Chuẩn hóa tên sản phẩm thực tế, dễ hiểu
- Chuyển đổi toàn bộ tên hoa từ dạng mỹ từ trừu tượng (như *"Bó Hoa Hướng Dương Ánh Dương Rạng Rỡ"*) sang tên hoa cụ thể, gần gũi với người tiêu dùng: *"Bó hoa hướng dương tươi"*, *"Bó hoa hồng đỏ Đà Lạt"*, *"Giỏ hoa cẩm tú cầu xanh"*.

### 8.3. Nút bấm sổ xuống (Dropdown) chọn danh mục và thuộc tính hoa
- Xây dựng component [fragments/flower-attributes.html](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/templates/fragments/flower-attributes.html) kết hợp [flower-attributes.js](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/static/js/flower-attributes.js).
- Cho phép người dùng click dropdown để chọn nhanh các đặc tính hoa (Loại hoa, Màu sắc, Phong cách thiết kế, Dịp tặng) thành các thẻ tag trực quan, không bắt buộc phải gõ tay.

### 8.4. Sửa lỗi 413 FileCountLimitExceededException khi thêm sản phẩm
- Tối ưu form multipart, loại bỏ các thẻ input file thừa/trùng lặp và cấu hình đúng Multipart Resolver của Tomcat để upload ảnh sản phẩm ổn định.

### 8.5. Khắc phục triệt để lỗi mất thuộc tính cũ khi sửa sản phẩm
- **Nguyên nhân**: Lệch tên alias giữa `"Phong cách"` trên form và `"Phong cách thiết kế"` trong CSDL; hàm `assignText` trước đây xóa bỏ thuộc tính của các nhóm không có trong request.
- **Giải pháp**:
  - Đồng bộ ánh xạ alias qua [FlowerAttributes.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/utils/FlowerAttributes.java).
  - Cập nhật [ProductAttributeService.java](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/service/ProductAttributeService.java) giữ nguyên thuộc tính cũ đối với các nhóm không bị chỉnh sửa.
  - Bổ sung hàm [Product.getSelectedStyleValueIds()](file:///d:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/entity/Product.java#L236) giúp template Thymeleaf đối chiếu và tick sẵn mọi checkbox/tag cũ khi render.


---

<a name="chuong-9"></a>
## CHƯƠNG 9: NHẬT KÝ TƯƠNG TÁC PROMPT CHAT VỚI AI (CHIẾM 2.0 ĐIỂM RUBRIC)

Bảng tổng hợp chi tiết toàn bộ các câu lệnh (prompts) mà nhóm phát triển đã sử dụng trong suốt quá trình xây dựng hệ thống với sự hỗ trợ của Trợ lý AI (Google DeepMind Antigravity / Gemini):

| Giai đoạn phát triển | Câu lệnh Prompt yêu cầu AI | Kết quả và Đóng góp của AI |
|:---|:---|:---|
| **1. Khởi tạo kiến trúc** | *"Thiết kế kiến trúc Spring Boot 4.x, Java 21, Thymeleaf, Bootstrap 5, JPA, SQL Server, WebSocket, JWT, Cloudinary. Phân chia package chuẩn MVC."* | AI sinh file `pom.xml`, cấu trúc thư mục 3 lớp, tạo entity `Store`, `Product`, `StoreInventory` và cấu hình Flyway migration có phiên bản. |
| **2. Bảo mật phân quyền kép** | *"Tạo Dual Security: Web dùng Session + CSRF, còn API dùng Bearer JWT Stateless với Token Rotation. Gửi mã OTP qua email, không in ra console."* | AI viết `JwtSecurityConfig`, `JwtTokenService`, `JwtAuthController` và `OtpService` gửi mail an toàn. |
| **3. Đặt hàng & Trừ tồn kho** | *"Đặt hàng COD phải trừ tồn kho ngay lập tức tại chi nhánh. Tránh race condition khi 2 người mua đồng thời bông hoa cuối cùng."* | AI cài đặt Pessimistic Write Lock trên `StoreInventory` trong transaction `REPEATABLE_READ` và cơ chế hoàn kho chính xác 1 lần. |
| **4. Vòng đời đơn hàng hoa** | *"Khi cửa hàng chuyển sang Đang cắm hoa (PREPARING), khóa nút hủy của khách. Quản lý hủy bắt buộc nhập lý do <= 300 ký tự."* | AI cập nhật `OrderStatus`, kiểm tra điều kiện tại Backend và tạo modal popup xác nhận kèm ghi log lý do hủy vào CSDL. |
| **5. Realtime Chat & Media** | *"Cấu hình WebSocket STOMP /ws-chat để chat thời gian thực theo chi nhánh. Upload ảnh Cloudinary lấy API key từ file .env, không hardcode."* | AI tạo `WebSocketConfig`, `ChatController` và `CloudinaryService` đọc từ file biến môi trường an toàn. |
| **6. Căn chỉnh giao diện KPI** | *"Cho các KPIs này nằm cùng 1 hàng trên Dashboard quản lý."* | AI viết lại CSS Grid và Flexbox responsive trên `manager/dashboard.html` và `admin/dashboard.html`. |
| **7. Chuẩn hóa tên hoa** | *"Đặt lại tên toàn bộ sản phẩm của cửa hàng cho đúng và dễ hiểu, không đặt tên trừu tượng như 'Bó Hoa Hướng Dương Ánh Dương Rạng Rỡ'."* | AI viết script cập nhật CSDL đổi toàn bộ tên sang tên thực tế: 'Bó hoa hướng dương', 'Bó hoa hồng đỏ tươi'... |
| **8. Dropdown chọn thuộc tính** | *"Lỗi khi thêm sản phẩm, tôi muốn nó có nút bấm sổ xuống để có thể lựa chọn thuộc tính hoa."* | AI viết component `flower-attributes.html` và JS hỗ trợ dropdown sổ xuống chọn nhanh tag Loại hoa, Màu sắc, Phong cách, Dịp tặng. |
| **9. Sửa lỗi upload ảnh 413** | *"Khi thêm sản phẩm bị lỗi 413 FileCountLimitExceededException."* | AI tối ưu hóa multipart resolver, loại bỏ các input file trùng lặp, cho phép upload ảnh ổn định. |
| **10. Bảo toàn thuộc tính cũ** | *"Bạn giúp tôi, sau khi tôi sửa các thuộc tính thì các thuộc tính cũ của tôi bị mất đi."* | AI phân tích lệch alias 'Phong cách' vs 'Phong cách thiết kế', sửa `ProductAttributeService` giữ nguyên giá trị cũ và thêm `getSelectedStyleValueIds()`. |
| **11. Dọn dẹp & Tạo báo cáo** | *"Xóa toàn bộ các file không cần đến trong dự án và tự động tạo trong quá trình biên dịch. Tổng hợp tài liệu hướng dẫn và sinh hình sơ đồ."* | AI chạy `mvn clean` xóa `target/`, xóa `.tmp/`, viết script sinh 5 sơ đồ kiến trúc chất lượng cao, xuất file Word `.docx` và tổng hợp tài liệu master này. |


---

<a name="chuong-10"></a>
## CHƯƠNG 10: HƯỚNG DẪN CÀI ĐẶT, KHỞI CHẠY & KẾT QUẢ KIỂM THỬ TỰ ĐỘNG

### 10.1. Yêu cầu môi trường
- Hệ điều hành: Windows 10 / 11 64-bit.
- Java: JDK 21 LTS (đã tích hợp bản portable trong `.tools/jdk-21.0.12.1+1`).
- CSDL: Microsoft SQL Server 2019+ (Instance `SQLEXPRESS`, database `RentalShop`).

### 10.2. Cấu hình file môi trường `.env`
Tạo file `.env` tại thư mục gốc với nội dung:
```dotenv
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
JWT_SECRET=c2VjdXJlX2Jsb29tX2Zsb3dlcl9jaGFpbl9qd3Rfc2VjcmV0X2tleV8yMDI2
SPRING_PROFILES_ACTIVE=dev
```

### 10.3. Lệnh khởi chạy ứng dụng
Mở terminal PowerShell tại thư mục gốc của dự án và chạy lệnh:
```powershell
.\scripts\sqlserver.ps1
```
Sau khi khởi động thành công, mở trình duyệt truy cập: **`http://localhost:8080`**

### 10.4. Kết quả Kiểm thử Tự động (Automated Test Suite)
- Ngày 19/10/2026: Java **622 ca** (616 đạt, 0 thất bại, 0 lỗi, 6 bỏ qua có chủ đích); JavaScript **102/102 ca đạt** khi cho phép kết nối localhost của test.
- Test SQL Server live và Cloudinary live chỉ chạy khi chủ động bật cờ riêng; test thông thường không chứng minh dịch vụ thật hoạt động.
- Các phát hiện và giới hạn kiểm thử được ghi trong [báo cáo rà soát](PROJECT_REVIEW_20261016.md).

---
*Tài liệu được biên soạn và chuẩn hóa toàn diện phục vụ nghiệm thu đồ án môn học Lập trình Web.*
