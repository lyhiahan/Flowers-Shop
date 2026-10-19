# BÁO CÁO TOÀN DIỆN ĐỒ ÁN MÔN LẬP TRÌNH WEB
## HỆ THỐNG WEBSITE THƯƠNG MẠI ĐIỆN TỬ BÁN HOA THEO MÔ HÌNH CHUỖI CỬA HÀNG
### DỰ ÁN: BLOOM FLOWER CHAIN (TIỆM HOA SÀI GÒN)

---

> **Dành cho sinh viên bảo vệ đồ án:**
> Tài liệu này được thiết kế để bạn sử dụng làm **sườn thuyết minh đồ án**, **tài liệu nộp bài chấm** và **cẩm nang đối đáp phản biện** với Giảng viên bộ môn Lập trình Web. Tài liệu bám sát 100% mã nguồn thực tế, kiến trúc Spring Boot - Thymeleaf - SQL Server, các kịch bản demo trực quan và 10 câu hỏi phản biện chuyên sâu.

---

## MỤC LỤC
1. [Phần 1: Giới thiệu Đề tài & Tính cấp thiết](#phan-1)
2. [Phần 2: Kiến trúc Hệ thống & Ngăn xếp Công nghệ (Tech Stack)](#phan-2)
3. [Phần 3: Phân tích Thiết kế & Đặc tả Use Cases theo Vai trò](#phan-3)
4. [Phần 4: Các Điểm nhấn Kỹ thuật Nổi bật (Key Technical Highlights)](#phan-4)
5. [Phần 5: Kịch bản Thuyết trình & Demo Thực tế (10 - 15 Phút)](#phan-5)
6. [Phần 6: Danh sách Tài khoản & Dữ liệu Mẫu Phục vụ Demo](#phan-6)
7. [Phần 7: Bộ Câu hỏi Phản biện Giảng viên thường gặp & Hướng dẫn Trả lời](#phan-7)
8. [Phần 8: Kết luận & Hướng phát triển Đề tài](#phan-8)

---

<a name="phan-1"></a>
## PHẦN 1: GIỚI THIỆU ĐỀ TÀI & TÍNH CẤP THIẾT

### 1.1. Đặt vấn đề thực tế
- Kinh doanh hoa tươi là ngành bán lẻ đặc thù: sản phẩm có vòng đời rất ngắn (perishable goods), dễ hư hỏng trong quá trình vận chuyển đường dài và yêu cầu kỹ thuật cắm hoa thủ công theo từng đơn.
- Các sàn thương mại điện tử truyền thống (Shopee, Lazada, Tiki) xử lý theo mô hình kho tập trung hoặc gian hàng độc lập, không đáp ứng được bài toán:
  - Khách hàng cần nhận hoa trong 1 - 2 giờ từ **chi nhánh gần nhất**.
  - Tồn kho hoa tươi phải được **quản lý độc lập theo từng chi nhánh vật lý**.
  - Sau khi cửa hàng bắt đầu cắt cành cắm hoa, khách **không được tự ý hủy đơn** để tránh lãng phí nguyên liệu.
  - Khách cần chat trực tiếp với nghệ nhân cắm hoa tại chi nhánh để trao đổi về mẫu hoa thực tế.

### 1.2. Mục tiêu của Đồ án
Xây dựng một hệ thống website thương mại điện tử chuyên biệt cho chuỗi cửa hàng hoa tươi giải quyết trọn vẹn:
1. **Mô hình chuỗi đa chi nhánh (Multi-branch Chain)**: Quản lý độc lập kho hàng, đơn hàng, doanh thu và nhân sự của từng chi nhánh.
2. **Trừ tồn kho tức thời (Realtime Stock Reservation)**: Khóa bi quan chống bán vượt (Overselling) khi nhiều khách cùng mua 1 mẫu hoa.
3. **Bảo mật kép (Dual Security)**: Kết hợp Session-based CSRF cho giao diện Web và Stateless JWT Token cho REST API.
4. **Giao tiếp thời gian thực (Realtime Chat STOMP WebSocket)**: Khách hàng chat trực tiếp với chi nhánh kèm thẻ ngữ cảnh sản phẩm tự động.
5. **Cơ chế kiểm toán & đối soát minh bạch**: Admin giám sát toàn chuỗi chỉ đọc kèm nhật ký truy cập (Access Log) không thể can thiệp.

---

<a name="phan-2"></a>
## PHẦN 2: KIẾN TRÚC HỆ THỐNG & NGĂN XẾP CÔNG NGHỆ (TECH STACK)

### 2.1. Kiến trúc phân tầng 5 lớp (5-Tier Enterprise Architecture)
Hệ thống tuân thủ chặt chẽ nguyên lý phân tách trách nhiệm (Separation of Concerns):

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. CLIENT PRESENTATION LAYER                                           │
│    Thymeleaf Template Engine, Bootstrap 5.3.3, Vanilla JS, SockJS      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 2. DUAL SECURITY GATEWAY                                               │
│    - Web MVC Security: Session Cookie (JSESSIONID) + CSRF Protection    │
│    - REST API Security: Stateless Bearer JWT + Refresh Token Rotation │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 3. CONTROLLER LAYER                                                    │
│    CustomerController, ManagerController, AdminController,             │
│    ChatWebSocketController, RestApiController                          │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 4. SERVICE & BUSINESS LOGIC LAYER                                      │
│    OrderService (Pessimistic Lock), StoreService, InventoryService,    │
│    CloudinaryService, MailOtpService (Brevo API)                       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 5. DATA PERSISTENCE & DATABASE LAYER                                   │
│    Spring Data JPA (Hibernate ORM), Microsoft SQL Server (RentalShop), │
│    Flyway Database Migration (V1 - V10)                                │
└────────────────────────────────────────────────────────────────────────┘
```

### 2.2. Bảng đối chiếu Công nghệ và Vai trò trong Dự án

| Thành phần | Công nghệ sử dụng | Phiên bản / Đặc tính kỹ thuật | Vai trò trong hệ thống |
|:---|:---|:---|:---|
| **Backend Core** | Java SDK | JDK 21 LTS | Nền tảng thực thi hiệu năng cao, tận dụng Virtual Threads & Pattern Matching. |
| **Framework** | Spring Boot | 3.4.1 / 4.1.1 | Khởi tạo ứng dụng, quản lý Dependency Injection và cấu hình tự động. |
| **Giao diện (View)** | Thymeleaf | Thymeleaf Spring 6 | Render HTML phía server an toàn, tích hợp trực tiếp Spring Security Dialect. |
| **CSS / UI** | Bootstrap & Icons | Bootstrap 5.3.3, BS Icons | Thiết kế Responsive, Grid layout chuẩn cho Desktop, Tablet và Mobile. |
| **Cơ sở dữ liệu** | Microsoft SQL Server | SQL Server 2022 / SQLEXPRESS | Lưu trữ dữ liệu quan hệ, hỗ trợ Transaction ACID và Index tối ưu. |
| **ORM / Data** | Spring Data JPA | Hibernate 6.x | Ánh xạ Object-Relational, thực thi Khóa bi quan (`LockModeType.PESSIMISTIC_WRITE`). |
| **Database Migration** | Flyway | Flyway Core 12.x | Quản lý lịch sử nâng cấp schema từ `V1` đến `V10`, không dùng `ddl-auto=update`. |
| **Bảo mật** | Spring Security | 6.x | Phân quyền 2 tầng: Cookie Session + CSRF cho Web, JWT Stateless cho API. |
| **Giao tiếp Realtime** | Spring WebSocket | STOMP over SockJS | Đẩy tin nhắn chat và thông báo tức thời giữa khách hàng, chi nhánh và admin. |
| **Lưu trữ Đám mây** | Cloudinary CDN | Cloudinary Java SDK | Tải lên và quản lý ảnh sản phẩm, ảnh đại diện, ảnh chat qua HTTPS CDN. |
| **Dịch vụ Email** | Brevo API | REST API / JavaMail | Gửi mã OTP xác thực 6 chữ số khi quên mật khẩu qua HTML email. |

---

<a name="phan-3"></a>
## PHẦN 3: PHÂN TÍCH THIẾT KẾ & ĐẶC TẢ USE CASES THEO VAI TRÒ

Hệ thống phân định ranh giới chức năng rõ ràng cho 4 nhóm tác nhân (Actors):

```
                       ┌─────────────────────────┐
                       │   BLOOM FLOWER CHAIN    │
                       └────────────┬────────────┘
         ┌──────────────────┬───────┴────────┬──────────────────┐
         ▼                  ▼                ▼                  ▼
┌─────────────────┐ ┌───────────────┐ ┌───────────────┐ ┌────────────────┐
│ Khách vãng lai  │ │  Khách hàng   │ │ Quản lý Shop  │ │ Quản trị viên  │
│    (Guest)      │ │    (User)     │ │   (Manager)   │ │    (Admin)     │
└─────────────────┘ └───────────────┘ └───────────────┘ └────────────────┘
```

### 3.1. Phân hệ Khách vãng lai (Guest)
- **Xem danh mục hoa đa dịp**: Lọc hoa theo dịp chính (Sinh nhật, Khai trương, Tình yêu, Cưới, Chia buồn, Sự kiện).
- **Tra cứu tồn kho theo chi nhánh**: Xem chi nhánh nào còn hàng trước khi quyết định mua.
- **Tìm kiếm & Xem chi tiết hoa**: Xem hình ảnh chất lượng cao trên Cloudinary, mô tả, giá bán và đánh giá của khách đã mua.
- **Không yêu cầu tài khoản**: Không ép người dùng tạo tài khoản nếu chỉ duyệt xem sản phẩm (tránh lưu rác dữ liệu).

### 3.2. Phân hệ Khách hàng (Registered Customer - `ROLE_USER`)
- **Xác thực & Bảo mật tài khoản**: Đăng ký, đăng nhập, đổi mật khẩu và lấy lại mật khẩu qua OTP email (Brevo).
- **Giỏ hàng & Báo giá tức thời**: Thêm hoa vào giỏ, chọn số lượng, chọn chi nhánh phục vụ.
- **Đặt hàng theo vùng giao Demo (Zone-based Delivery)**:
  - Vùng gần (`NEAR` - Dưới 5km): Phí giao tiêu chuẩn nội quận.
  - Vùng xa (`FAR` - 5km đến 15km): Phí giao ngoại quận bằng xe máy chuyên dụng.
  - Ngoài vùng (`OUTSIDE` - Trên 15km): Cảnh báo từ chối giao hoa tươi trực tiếp, khuyến nghị nhận tại cửa hàng để đảm bảo độ tươi.
- **Áp dụng Mã giảm giá (Voucher)**: Tính toán giảm giá tự động theo điều kiện đơn tối thiểu.
- **Theo dõi vòng đời đơn hàng**: Xem tiến độ đơn từ lúc tiếp nhận đến khi giao thành công.
- **Quyền tự hủy đơn**: Tự hủy đơn khi ở trạng thái `PENDING` hoặc `CONFIRMED`. Khi đơn chuyển sang `PREPARING` (Đang cắm hoa), nút hủy tự động khóa.
- **Chat tư vấn thông minh với cửa hàng**: Bấm nút "Nhắn tin với Cửa hàng" tại chi tiết sản phẩm, hệ thống tự động ghim thẻ hoa đang hỏi vào khung chat mà không cần chọn người nhận thủ công.

### 3.3. Phân hệ Quản lý Chi nhánh (Branch Manager - `ROLE_MANAGER`)
- **Bảng điều khiển chi nhánh (Dashboard)**: Theo dõi KPI doanh thu hôm nay, tổng số đơn và **tồn kho tức thời** cập nhật realtime mỗi 5 giây.
- **Quản lý danh mục hoa chi nhánh**:
  - Thêm mẫu hoa mới, gán danh mục chính và danh mục phụ.
  - Điều chỉnh giá bán và giá nhập.
  - **Xóa mềm (Soft Delete)**: Ẩn mẫu hoa khỏi gian hàng mà không xóa vật lý trong CSDL, bảo toàn lịch sử đơn hàng cũ.
- **Quản lý kho hàng (`StoreInventory`)**: Nhập hàng, điều chỉnh số lượng tồn kho của từng mẫu hoa tại chi nhánh mình quản lý.
- **Xử lý đơn hàng đa trạng thái**:
  - `PENDING` (Chờ xác nhận) ➔ `CONFIRMED` (Đã duyệt) ➔ `PREPARING` (Đang cắm hoa) ➔ `SHIPPING` (Đang giao) ➔ `DELIVERED` (Đã giao) ➔ `COMPLETED` (Hoàn tất).
  - Quyền hủy đơn ngoại lệ kèm **bắt buộc nhập lý do hủy** để thông báo minh bạch cho khách hàng.
- **Ví doanh thu chi nhánh**: Theo dõi số dư khả dụng, số dư đóng băng đang xử lý và lịch sử biến động số dư.
- **Hộp thư khách hàng chi nhánh**: Nhận và trả lời tin nhắn của khách hàng gửi riêng cho chi nhánh qua WebSocket.
- **Kênh hỗ trợ vận hành**: Tab gửi yêu cầu hỗ trợ trực tiếp lên Admin toàn hệ thống (`BRANCH_SUPPORT`).

### 3.4. Phân hệ Quản trị viên Toàn hệ thống (Admin - `ROLE_ADMIN`)
- **Quản lý mạng lưới chi nhánh (`Store`)**: Mở chi nhánh mới, cập nhật thông tin, kích hoạt hoặc tạm dừng hoạt động chi nhánh.
- **Phân công nhân sự (`StoreMembership`)**: Bổ nhiệm hoặc chuyển đổi quản lý cho các chi nhánh.
- **Cấu hình biểu phí vận chuyển (`ShippingTariff`)**: Thiết lập bảng cước phí giao hoa theo từng chi nhánh và vùng cự ly.
- **Trung tâm hội thoại đa luồng (`/admin/chat`)**:
  - Kênh 1: Chăm sóc khách hàng toàn hệ thống (CSKH).
  - Kênh 2: Hỗ trợ vận hành các chi nhánh.
  - Kênh 3: **Giám sát chỉ đọc (Read-only Monitoring)**: Xem toàn bộ hội thoại giữa khách hàng và các chi nhánh.
- **Nhật ký giám sát bất biến (`chat_access_logs`)**: Mỗi lần Admin mở xem hội thoại của chi nhánh, hệ thống bắt buộc ghi log (Admin nào xem, hội thoại nào, vào lúc nào). Tuyệt đối không có API sửa/xóa log.
- **Quản lý mã giảm giá & tài khoản**: Cấp phát voucher khuyến mãi toàn sàn, khóa tài khoản vi phạm.

---

<a name="phan-4"></a>
## PHẦN 4: CÁC ĐIỂM NHẤN KỸ THUẬT NỔI BẬT (KEY TECHNICAL HIGHLIGHTS)

Khi báo cáo với giảng viên, đây là 6 luận điểm kỹ thuật thể hiện chiều sâu chuyên môn cao của dự án:

### 4.1. Cơ chế Trừ tồn kho tức thời (Stock Reservation) chống Race Condition
- **Vấn đề**: Khi một mẫu hoa độc bản chỉ còn 1 bó cuối cùng trong kho, nếu 2 khách hàng bấm "Đặt hàng" cùng một mili-giây, nếu không có cơ chế khóa, cả 2 đơn đều thành công dẫn đến tồn kho bị âm (-1) hoặc cửa hàng không có hoa giao (Overselling).
- **Giải pháp**: Áp dụng **Khóa bi quan (Pessimistic Write Lock)** trong Spring Data JPA:
  ```java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT si FROM StoreInventory si WHERE si.store.id = :storeId AND si.product.id = :productId")
  Optional<StoreInventory> findByStoreIdAndProductIdForUpdate(Long storeId, Long productId);
  ```
- **Quy trình thực thi**:
  1. Transaction mở với mức cô lập `REPEATABLE_READ`.
  2. Câu lệnh SQL sinh ra có mệnh đề `WITH (UPDLOCK, ROWLOCK)` trên SQL Server, khóa dòng dữ liệu đó lại.
  3. Request thứ 2 phải chờ cho đến khi Request thứ 1 hoàn tất kiểm tra số lượng và trừ kho.
  4. Trừ kho ngay khi đặt hàng thành công (kể cả đơn COD), hoàn kho tự động 1 lần nếu đơn bị hủy trước bước cắm hoa.

### 4.2. Bảo vệ Chi phí Hoa tươi trong Vòng đời Đơn hàng
- Hoa tươi đã cắt cành, lên khung cắm hoa không thể tháo ra bán lại như hàng tiêu dùng đóng gói.
- Nghiệp vụ hệ thống quy định:
  - Khi đơn hàng ở trạng thái `PENDING` hoặc `CONFIRMED`: Khách có quyền tự hủy đơn trên giao diện, hệ thống tự động hoàn lại số lượng tồn kho.
  - Khi quản lý chi nhánh bấm chuyển sang `PREPARING` (Đang cắm hoa): Nút hủy của khách lập tức bị khóa. Khách không thể gửi request hủy đơn (backend kiểm tra và chặn `IllegalStateException`).
  - Chỉ Quản lý/Admin mới có quyền hủy đơn trong tình huống bất khả kháng (hết hoa tươi cùng màu) và bắt buộc phải nhập lý do giải trình.

### 4.3. Kiến trúc Bảo mật Kép (Dual Security Filter Chain)
- Thông thường các bài tập web chỉ dùng một trong hai: hoặc Session Cookie, hoặc JWT. Dự án này triển khai song song cả hai chuỗi bảo mật độc lập:
  - **Chuỗi 1 - Web Thymeleaf (`@Order(2)`)**: Quản lý trạng thái đăng nhập qua `JSESSIONID`, bật cờ `HttpOnly`, tích hợp Spring Security CSRF bảo vệ chống giả mạo request từ trang web độc hại.
  - **Chuỗi 2 - REST API (`@Order(1)`)**: Áp dụng cho các route `/api/**`, cơ chế hoàn toàn Stateless (`SessionCreationPolicy.STATELESS`), tắt CSRF, xác thực qua `Authorization: Bearer <JWT>`.
  - **Refresh Token Rotation**: Xoay vòng Refresh Token mỗi lần gọi cấp mới Access Token, phát hiện và thu hồi toàn bộ token family nếu phát hiện token cũ bị đánh cắp dùng lại (Replay Attack).

### 4.4. Realtime WebSocket STOMP phân quyền theo Chi nhánh
- Khác với chat nhóm công cộng, hệ thống chat hỗ trợ đa chi nhánh:
  - Endpoint `/ws-chat` bắt tay qua giao thức STOMP over SockJS.
  - Quản lý chi nhánh chỉ được phép subscribe vào hàng đợi của chính chi nhánh mình: `/user/queue/chat.store.{storeId}`. Backend kiểm tra quyền `StoreMembership`, ngăn chặn việc nhân viên chi nhánh này nghe lén tin nhắn của chi nhánh khác.
  - Tích hợp **Thẻ ngữ cảnh sản phẩm (Product Card Context)**: Khách hàng đang xem bó hoa hồng đỏ và bấm "Nhắn tin", khung chat tự động mang theo ảnh thu nhỏ, tên hoa và giá bán để người cắm hoa nắm được nhu cầu ngay lập tức mà không cần hỏi lại mã sản phẩm.

### 4.5. Phân loại Danh mục Kép (Primary & Secondary Categories)
- Cơ chế linh hoạt: Mỗi sản phẩm có **1 danh mục chính** (Primary Category - ví dụ: Hoa Tươi) để định danh kế toán/kho bãi, và **nhiều danh mục phụ theo dịp tặng** (Secondary Categories - ví dụ: Sinh nhật, Tình yêu, Khai trương).
- Khách hàng lọc theo "Hoa Sinh Nhật" hay "Hoa Tình Yêu" đều tìm thấy sản phẩm một cách trực quan, tối ưu trải nghiệm mua sắm thương mại điện tử hiện đại.

### 4.6. Kiểm soát Schema CSDL bằng Flyway Migration & Dữ liệu Chuẩn hóa
- Tuyệt đối không dùng `spring.jpa.hibernate.ddl-auto=update` trên môi trường chạy thực tế để tránh làm sai lệch cấu trúc dữ liệu.
- Quản lý toàn bộ vòng đời schema qua 10 phiên bản Flyway (`V1` đến `V10`), kiểm tra toàn vẹn dữ liệu tự động khi khởi động.
- Dữ liệu kho hàng đã được kiểm toán và đối soát: xử lý triệt để 81 dòng dữ liệu nhân bản cũ, bảo đảm 100% kho hàng không có tồn kho ảo, không lệch chi nhánh sở hữu.

---

<a name="phan-5"></a>
## PHẦN 5: KỊCH BẢN THUYẾT TRÌNH & DEMO THỰC TẾ (10 - 15 PHÚT)

Đây là kịch bản chuẩn từng phút giúp nhóm bạn làm chủ thời gian và gây ấn tượng mạnh với hội đồng chấm:

```
┌────────────────────────────────────────────────────────────────────────┐
│ PHÂN PHỐI THỜI GIAN THUYẾT TRÌNH (12 PHÚT)                             │
├──────────────┬─────────────────────────────────────────────────────────┤
│ Phút 00 - 02 │ Giới thiệu thành viên, mục tiêu đề tài & kiến trúc tổng thể │
│ Phút 02 - 05 │ Demo Khách hàng: Lọc hoa theo dịp, chọn chi nhánh & đặt đơn│
│ Phút 05 - 08 │ Demo Quản lý: Trừ kho tức thời, cắm hoa & khóa quyền hủy │
│ Phút 08 - 11 │ Demo Realtime: Chat 2 màn hình kèm thẻ hoa & Admin giám sát │
│ Phút 11 - 12 │ Báo cáo kiểm thử tự động, kết luận & mở phần hỏi đáp   │
└──────────────┴─────────────────────────────────────────────────────────┘
```

### Kịch bản chi tiết từng bước:

#### Bước 1: Giới thiệu Kiến trúc & Bài toán kinh doanh (2 phút)
- Mở slide hoặc tài liệu tổng quan.
- Tuyên bố rõ: Dự án giải quyết bài toán kinh doanh hoa tươi theo mô hình chuỗi đa chi nhánh, sử dụng Spring Boot 3 + SQL Server + WebSocket + Dual Security.

#### Bước 2: Demo Khách hàng đặt hoa & Tính phí giao hàng (3 phút)
- Trình duyệt 1 (Google Chrome bình thường): Mở trang chủ `http://localhost:8080/`.
- Thao tác:
  1. Cho thầy cô xem danh mục hoa theo dịp (Sinh nhật, Khai trương, Cưới...).
  2. Bấm vào chi tiết một mẫu hoa (ví dụ: Bó Hoa Hồng Đỏ). Chỉ rõ trên màn hình: Mẫu hoa này thuộc sở hữu của Chi nhánh Quận 1, hiển thị số lượng tồn kho của chi nhánh.
  3. Đăng nhập tài khoản khách hàng `leminhhoa` (mật khẩu `123456`).
  4. Bấm "Thêm vào giỏ hàng" và tiến hành vào trang Thanh toán (`/checkout`).
  5. **Điểm nhấn**: Tại trang thanh toán, chọn hình thức "Giao hoa tận nơi", chọn đơn vị giao hàng và chọn **Vùng giao demo**:
     - Chọn "Vùng gần (Dưới 5km)": Phí giao hiển thị 25.000đ.
     - Đổi sang "Vùng ngoài phạm vi (>15km)": Hệ thống hiển thị cảnh báo từ chối giao để bảo vệ hoa tươi.
     - Chọn lại "Vùng gần", áp dụng mã giảm giá (nếu có), bấm "Đặt hàng".
  6. Đơn hàng được tạo thành công với mã đơn mới (trạng thái `PENDING`).

#### Bước 3: Demo Quản lý Chi nhánh & Chứng minh Trừ kho tức thời (3 phút)
- Trình duyệt 2 (Cửa sổ Ẩn danh hoặc Firefox): Mở `http://localhost:8080/login`.
- Đăng nhập tài khoản Quản lý Quận 1: `nguyenquanghuy` (mật khẩu `123456`).
- Thao tác:
  1. Mở trang **Quản lý kho** (`/manager/inventory`): Chỉ cho thầy cô thấy số lượng tồn kho của mẫu hoa vừa đặt **đã bị trừ ngay 1 đơn vị** (chứng minh cơ chế trừ kho tức thời, không chờ giao mới trừ).
  2. Vào trang **Quản lý đơn hàng** (`/manager/orders`): Thấy đơn hàng vừa tạo ở tab "Chờ xác nhận".
  3. Bấm "Xác nhận đơn" ➔ Trạng thái chuyển sang `CONFIRMED`.
  4. Bấm tiếp "Bắt đầu cắm hoa" ➔ Trạng thái chuyển sang `PREPARING`.
  5. **Điểm nhấn chứng minh nghiệp vụ**:
     - Quay lại Trình duyệt 1 của Khách hàng (`leminhhoa`), bấm F5 trang chi tiết đơn hàng:
     - Nút "Hủy đơn hàng" đã biến mất hoặc bị vô hiệu hóa kèm thông báo: *"Đơn hàng đang được cắm hoa, không thể tự hủy"*.
     - Giảng viên sẽ đánh giá rất cao điểm nhấn nghiệp vụ này!

#### Bước 4: Demo Chat Realtime WebSocket kèm Thẻ sản phẩm & Giám sát Admin (3 phút)
- Đặt 2 cửa sổ trình duyệt song song (Nửa màn hình trái là Khách `leminhhoa`, nửa màn hình phải là Quản lý Q1 `nguyenquanghuy`).
- Thao tác:
  1. Ở màn hình Khách hàng, vào trang chi tiết hoa, bấm nút "Nhắn tin với Cửa hàng".
  2. Khung chat mở ra, hiển thị ngay **Thẻ sản phẩm thu nhỏ** (ảnh, tên, giá).
  3. Khách gõ tin nhắn: *"Shop ơi mẫu này có thể đổi sang giấy gói màu đen được không?"* và bấm Gửi.
  4. Màn hình Quản lý Q1 **nhận ngay tin nhắn và thẻ sản phẩm trong tích tắc** mà không cần tải lại trang (chứng minh STOMP WebSocket hoạt động hoàn hảo).
  5. Quản lý trả lời: *"Dạ được bạn nhé, shop hỗ trợ đổi màu giấy gói miễn phí ạ"*. Khách nhận ngay phản hồi.
- Mở thêm 1 tab đăng nhập Admin: `nguyenthanhminh` (mật khẩu `123456`).
  1. Vào `/admin/chat`: Cho thầy cô xem mục **Giám sát hội thoại chỉ đọc**.
  2. Admin thấy được nội dung chat giữa Khách và Quản lý Q1 để kiểm soát chất lượng phục vụ nhưng không thể can thiệp giả mạo.
  3. Vào `/admin/chat/access-logs`: Cho xem dòng nhật ký ghi lại việc Admin vừa xem hội thoại nào, lúc mấy giờ.

#### Bước 5: Báo cáo Kiểm thử Tự động & Kết thúc (1 phút)
- Trình bày kết quả kiểm thử: Toàn bộ hệ thống có **622 ca kiểm thử tự động Java** và **102 ca kiểm thử JavaScript** đảm bảo không có lỗi hồi quy (regression).
- Cảm ơn hội đồng và chuyển sang phần trả lời câu hỏi.

---

<a name="phan-6"></a>
## PHẦN 6: DANH SÁCH TÀI KHOẢN & DỮ LIỆU MẪU PHỤC VỤ DEMO

Để buổi thuyết minh diễn ra suôn sẻ, chuẩn bị sẵn bảng tài khoản sau (tất cả tài khoản đều dùng mật khẩu chung `123456`):

| Vai trò | Tên đăng nhập | Mật khẩu | Chi nhánh phụ trách | Đường dẫn kiểm tra |
|:---|:---|:---:|:---|:---|
| **Khách vãng lai** | *(Không cần login)* | - | Toàn hệ thống | `http://localhost:8080/` |
| **Khách hàng** | `leminhhoa` | `123456` | - | `http://localhost:8080/profile` |
| **Khách hàng 2** | `lygiahan` | `123456` | - | `http://localhost:8080/cart` |
| **Quản lý Q1** | `nguyenquanghuy` | `123456` | Chi Nhánh Bến Thành (Quận 1) | `http://localhost:8080/manager/dashboard` |
| **Quản lý Q7** | `tranduclong` | `123456` | Chi Nhánh Phú Mỹ Hưng (Quận 7) | `http://localhost:8080/manager/dashboard` |
| **Quản trị viên** | `nguyenthanhminh` | `123456` | Toàn quyền hệ thống | `http://localhost:8080/admin/dashboard` |

### Danh sách các Chi nhánh mẫu trên hệ thống:
1. **Chi Nhánh Bến Thành (Quận 1)**: `123 Lê Lợi, P. Bến Thành, Quận 1, TP.HCM` - Hotline: `0281111222`
2. **Chi Nhánh Phú Mỹ Hưng (Quận 7)**: `456 Nguyễn Thị Thập, Tân Hưng, Quận 7, TP.HCM` - Hotline: `0283333444`
3. **Chi Nhánh Làng Đại Học (TP. Thủ Đức)**: `789 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức` - Hotline: `0285555666`
4. **Tiệm Hoa Sài Gòn - Chi Nhánh Cao Lãnh**: `112 An Bình, TP. Cao Lãnh, Đồng Tháp` - Hotline: `08730123123`

---

<a name="phan-7"></a>
## PHẦN 7: BỘ CÂU HỎI PHẢN BIỆN THƯỜNG GẶP CỦA GIẢNG VIÊN & HƯỚNG DẪN TRẢ LỜI

Dưới đây là 10 câu hỏi "kinh điển" giảng viên chuyên ngành Web thường hỏi và gợi ý trả lời chuẩn xác:

#### Câu 1: Tại sao nhóm lại xây dựng cả Session và JWT trong cùng một project?
> **Gợi ý trả lời:**
> *"Dạ thưa Thầy/Cô, hệ thống áp dụng kiến trúc Dual Security để tối ưu cho từng loại client:
> - Với người dùng duyệt web qua trình duyệt (Thymeleaf), nhóm sử dụng **Session Cookie kết hợp CSRF Token** vì đây là chuẩn mực an toàn cao nhất chống lại tấn công XSS và Cross-Site Request Forgery cho giao diện Server-side rendering.
> - Với các endpoint `/api/**`, nhóm sử dụng **Stateless JWT** để phục vụ việc tích hợp ứng dụng di động (Mobile App) hoặc các dịch vụ bên thứ ba trong tương lai. API không lưu session, giúp máy chủ dễ dàng mở rộng theo chiều ngang (scale-out)."*

#### Câu 2: Làm thế nào nhóm giải quyết bài toán Concurrency khi 2 khách hàng cùng bấm mua 1 sản phẩm cuối cùng?
> **Gợi ý trả lời:**
> *"Dạ thưa Thầy/Cô, nhóm xử lý bằng cơ chế **Khóa bi quan (Pessimistic Locking)** với cờ `@Lock(LockModeType.PESSIMISTIC_WRITE)` của Spring Data JPA. Khi khách hàng bắt đầu bước trừ kho trong Transaction, Hibernate phát lệnh khóa dòng `UPDLOCK, ROWLOCK` trên bảng `store_inventories` của SQL Server. Người thứ hai gửi request sẽ phải chờ cho đến khi Transaction của người thứ nhất hoàn tất. Nếu số lượng sau khi người thứ nhất mua bằng 0, hệ thống báo lỗi hết hàng cho người thứ hai ngay lập tức, ngăn chặn 100% tình trạng Overselling."*

#### Câu 3: Nhóm chống tấn công SQL Injection và CSRF như thế nào?
> **Gợi ý trả lời:**
> *"Dạ thưa Thầy/Cô:
> - Về **SQL Injection**: Nhóm sử dụng 100% Spring Data JPA và Hibernate với cơ chế Parameterized Queries (truy vấn tham số hóa). Toàn bộ tham số truyền vào `@Query` đều được bind qua biến `:param`, tuyệt đối không cộng chuỗi SQL thô.
> - Về **CSRF**: Với giao diện Web Thymeleaf, Spring Security tự động chèn mã `_csrf` token ẩn vào mọi form POST/PUT/DELETE. Máy chủ kiểm tra token này trước khi xử lý, nếu request không có token hợp lệ từ chính phiên đó thì sẽ bị chặn với mã lỗi 403 Forbidden."*

#### Câu 4: WebSocket khác gì so với HTTP Polling thông thường và phân quyền tin nhắn ra sao?
> **Gợi ý trả lời:**
> *"Dạ thưa Thầy/Cô:
> - HTTP Polling bắt trình duyệt phải liên tục gửi request sau mỗi vài giây, gây tốn băng thông và tải nặng cho máy chủ. Ngược lại, WebSocket tạo một kết nối hai chiều song công (Full-duplex) duy nhất qua giao thức TCP.
> - Nhóm sử dụng sub-protocol **STOMP**. Về phân quyền: Mỗi nhân viên chi nhánh chỉ được subscribe vào kênh `/user/queue/chat.store.{storeId}` của chi nhánh mình. Ở phương thức xử lý message, backend kiểm tra `Principal` và quyền `StoreMembership`, nếu không thuộc chi nhánh đó thì sẽ từ chối gửi tin, đảm bảo tính riêng tư giữa các chi nhánh."*

#### Câu 5: Tại sao hệ thống lại thiết kế 1 danh mục chính và nhiều danh mục phụ?
> **Gợi ý trả lời:**
> *"Dạ thưa Thầy/Cô, đây là thiết kế chuẩn trong các hệ thống E-commerce hiện đại:
> - **Danh mục chính (Primary Category)**: Ví dụ Bó hoa, Giỏ hoa, Kệ hoa - dùng để định danh phân loại hàng hóa trong quản lý tồn kho và hạch toán kế toán.
> - **Danh mục phụ (Secondary / Occasion Categories)**: Ví dụ Hoa Sinh Nhật, Hoa Tình Yêu, Hoa Khai Trương - phục vụ hành vi tìm kiếm của người mua theo dịp tặng. Một bó hoa hồng đỏ vừa có thể tặng Sinh nhật, vừa có thể tặng Tình yêu. Thiết kế này giúp người dùng dễ dàng tìm thấy hoa mà không cần nhân bản dữ liệu sản phẩm trong CSDL."*

#### Câu 6: Làm thế nào để upload ảnh lên Cloudinary mà không làm lộ API Secret?
> **Gợi ý trả lời:**
> *"Dạ thưa Thầy/Cô, nhóm cấu hình upload ảnh hoàn toàn qua Backend. Trình duyệt gửi file ảnh dạng `MultipartFile` lên Spring Controller qua kết nối HTTPS bảo mật. Backend Spring Boot đọc `CLOUDINARY_API_SECRET` từ biến môi trường (Environment Variable), ký xác thực và truyền file lên Cloudinary SDK. Frontend hoàn toàn không biết Secret Key, chỉ nhận lại đường link ảnh an toàn (`secure_url`) từ backend trả về."*

#### Câu 7: Cơ chế Xóa mềm (Soft Delete) tại chi nhánh hoạt động như thế nào?
> **Gợi ý trả lời:**
> *"Dạ thưa Thầy/Cô, khi chi nhánh ngừng kinh doanh một mẫu hoa, hệ thống không dùng lệnh `DELETE FROM store_inventories` vì sẽ làm hỏng khóa ngoại tới các đơn hàng lịch sử. Thay vào đó, hệ thống sử dụng cờ trạng thái `is_active = false` hoặc `deleted_at`. Mẫu hoa bị ẩn khỏi giao diện bán lẻ của chi nhánh đó, nhưng dữ liệu lịch sử đơn hàng, doanh thu và đối soát kế toán vẫn được bảo toàn nguyên vẹn."*

#### Câu 8: Tại sao lại khóa tính năng tự hủy đơn của khách khi đơn ở trạng thái PREPARING?
> **Gợi ý trả lời:**
> *"Dạ thưa Thầy/Cô, đây là quy tắc nghiệp vụ đặc thù của ngành hoa tươi. Khi đơn chuyển sang `PREPARING` (Đang cắm hoa), hoa tươi đã bị cắt gốc, tỉa lá và cắm vào mút xốp định hình. Nếu khách hủy lúc này, chi nhánh sẽ chịu tổn thất 100% nguyên liệu vì hoa cắm dở không thể hoàn lại kho bán cho người khác. Vì vậy, hệ thống tự động khóa nút hủy của khách hàng. Trong trường hợp hãn hữu, chỉ có Quản lý mới được hủy và bắt buộc phải ghi rõ lý do để lưu vết kiểm toán."*

#### Câu 9: Tại sao nhóm dùng Flyway Migration thay vì để Hibernate tự tạo bảng (`ddl-auto=update`)?
> **Gợi ý trả lời:**
> *"Dạ thưa Thầy/Cô:
> - `hibernate.ddl-auto=update` chỉ phù hợp khi làm bài tập nhỏ cá nhân vì nó không thể quản lý các thay đổi phức tạp như đổi tên cột, di chuyển dữ liệu, tạo chỉ mục Index hay kiểm soát ràng buộc toàn vẹn dữ liệu. Thậm chí nó có nguy cơ làm mất dữ liệu khi deploy lên môi trường thật.
> - **Flyway Migration** giúp toàn bộ nhóm làm việc trên cùng một phiên bản CSDL chính xác qua các file script từ `V1` đến `V10`. Mọi thay đổi schema đều có lịch sử rõ ràng trong bảng `flyway_schema_history`, đảm bảo tính toàn vẹn và có thể triển khai tự động (CI/CD) một cách an toàn."*

#### Câu 10: Tồn kho trên Dashboard Quản lý cập nhật realtime như thế nào?
> **Gợi ý trả lời:**
> *"Dạ thưa Thầy/Cô, trên Dashboard Quản lý và Admin, chỉ số KPI tồn kho được trang bị cơ chế thăm dò định kỳ tự động (Polling mỗi 5 giây qua AJAX) kết hợp lắng nghe thông báo đơn hàng mới qua WebSocket. Nhờ đó, ngay khi có đơn hàng phát sinh hoặc kho được nhập mới, chỉ số tồn kho trên màn hình của Quản lý sẽ tự động nhảy số mà không cần người dùng phải bấm F5 tải lại toàn bộ trang."*

---

<a name="phan-8"></a>
## PHẦN 8: KẾT LUẬN & HƯỚNG PHÁT TRIỂN ĐỀ TÀI

### 8.1. Kết quả đạt được
1. **Đáp ứng toàn diện chuẩn môn học Lập trình Web**: Hoàn thành xuất sắc từ kiến trúc phân tầng MVC, xử lý cơ sở dữ liệu quan hệ, cơ chế bảo mật xác thực phân quyền đến giao tiếp thời gian thực hiện đại.
2. **Nghiệp vụ thực tế sâu sắc**: Giải quyết trọn vẹn bài toán vận hành chuỗi hoa tươi với trừ kho tức thời, quản lý theo chi nhánh, bảo vệ hoa tươi trong vòng đời đơn và giám sát chat minh bạch.
3. **Chất lượng mã nguồn cao**: 238 lớp Java, 74 trang giao diện Thymeleaf sạch sẽ, kiểm thử tự động toàn diện (622 test cases Java và 102 test cases JS) đảm bảo hệ thống vận hành ổn định và tin cậy.

### 8.2. Hướng mở rộng trong tương lai
- **Tích hợp Cổng thanh toán quốc gia**: Tích hợp VNPAY / PayOS với Webhook xác thực chữ ký số thời gian thực.
- **Tích hợp AI Florist Assistant**: Ứng dụng mô hình AI nhận diện ảnh hoa khách tải lên để gợi ý mẫu hoa tương đồng có sẵn trong kho của chi nhánh gần nhất.
- **Ứng dụng di động (Mobile App Flutter / React Native)**: Kết nối trực tiếp vào hệ thống REST API JWT đã sẵn sàng.

---
*Tài liệu được biên soạn phục vụ đồ án môn Lập trình Web - Bloom Flower Chain.*
