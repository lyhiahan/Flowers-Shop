# Báo Cáo Triển Khai Sửa Lỗi Thông Báo, Trang /users và Bố Cục Admin

> Đây là báo cáo lịch sử triển khai. Xem [kiểm tra độc lập trước khi push main](MAIN_VERIFICATION_20261022.md) cho lỗi phát hiện bổ sung, kiểm thử và trạng thái bàn giao mới nhất; kết luận của từng lượt bên dưới không thay thế bằng chứng nghiệm thu cuối.

- Ngày thực hiện: 22/10/2026
- Tài liệu kế hoạch: `docs/ADMIN_UI_FIX_PLAN_20261022.md`
- Trạng thái tổng thể: Đang thực hiện theo trình tự T01 -> T02 -> T03 -> T04 -> T05

---

## 1. T01 — Thu thập Baseline và Tái hiện Lỗi

### 1.1. Thông tin Môi trường và Hệ thống

- **Commit hiện tại:** `ca2b9a7f386bd683d90a6a6c74ea454e89a58300` (merge code: hop nhat cap nhat tu nhanh thanh-tai)
- **Git status:** Bảo toàn nguyên trạng các thay đổi sẵn có trong working tree, không reset/clean.
- **Web Application:** `http://localhost:8080` (PID: 4016, Maven launcher PID: 16764, Java 21)
- **Cơ sở dữ liệu:** SQL Server tại `127.0.0.1:1433`, database `RentalShop`, user `rental_shop_local_8eaeb268`
- **Flyway:** Schema V15 (`V15__merge_cart_lines_by_product_and_branch.sql`)
- **Tài khoản quản trị viên thử nghiệm:** `admin@rentalshop.vn` (User ID: 1, Role: `ADMIN`, Trạng thái: `is_active=True`)

### 1.2. Bằng chứng Tái hiện A01 — Lỗi Định tuyến Thông báo khi Admin bấm Thông báo "Khách hàng bị khóa"

1. **Dữ liệu thông báo hiện có trong CSDL cho Admin (user_id = 1):**
   - Notification ID 6: type `ORDER_UPDATE`, relatedOrderId `4`, title `Khách hàng bị khóa`
   - Notification ID 12: type `ORDER_UPDATE`, relatedOrderId `6`, title `Khách hàng bị khóa`
2. **Phản hồi từ API `/api/notifications/latest`:**
   - Trả về JSON chứa `id`, `title`, `content`, `type`, `relatedOrderId`, `isRead`, `createdAt`.
   - **Hoàn toàn không có trường `targetUrl`** do backend tính toán.
3. **Tính toán URL phía client (`notifications-realtime.js`):**
   - Đoạn mã dòng 192–196 và 308–312:
     ```javascript
     const isManagerPath = window.location.pathname.startsWith('/manager');
     targetUrl = isManagerPath || isManagerUser ? '/manager/orders/' + item.relatedOrderId : '/customer/orders/detail/' + item.relatedOrderId;
     ```
   - Do Admin không thuộc route `/manager` và `isManagerUser` là false, client tự sinh ra URL: `/customer/orders/detail/4`.
4. **Hành vi khi Admin truy cập URL do client sinh ra (`/customer/orders/detail/4`):**
   - Request: `GET /customer/orders/detail/4` với cookie phiên Admin.
   - Kết quả: **HTTP 302 Redirect**
   - Location header: `http://localhost:8080/admin/dashboard?error=admin_shopping_denied`
   - Nguyên nhân: `SecurityConfig.java` kiểm soát chặt chẽ vai trò: mọi route `/customer/**` yêu cầu role `USER`. Khi Admin truy cập, `AccessDeniedHandler` chuyển hướng về `/admin/dashboard?error=admin_shopping_denied`.
5. **Kiểm tra quyền trực tiếp tại route Admin (`/admin/orders/4`):**
   - Request: `GET /admin/orders/4` với cookie phiên Admin.
   - Kết quả: **HTTP 200 OK**, hiển thị đúng trang `<title>Chi Tiết Đơn Hàng - Quản Trị Hệ Thống Hoa Tươi</title>`.
6. **Bề mặt `/notifications` hiện tại:**
   - `NotificationController.java` chỉ chọn template `manager/notifications` (khi role MANAGER) hoặc fallback `customer/notifications` (khi role ADMIN hoặc USER).
   - Admin vào `/notifications` bị hiển thị template khách hàng `customer/notifications.html`, trong đó thẻ xem đơn hardcode link `th:href="@{'/customer/orders/detail/' + ${n.relatedOrderId}}"`, tiếp tục đẩy Admin vào lỗi `admin_shopping_denied`.
   - Chưa hề có route `/admin/notifications` hay template `admin/notifications.html`.

### 1.3. Bằng chứng Tái hiện A02 — Trang `/users` và `/users/{id}` còn Giao diện Cũ

1. **Thực trạng mã nguồn:**
   - Template `src/main/resources/templates/customer/users.html` và `customer/user-detail.html` bị nén dồn toàn bộ nội dung thành 1 dòng duy nhất, khó bảo trì.
   - Sử dụng fragment cũ `fragments/floral.html` với cấu trúc thẻ sơ sài, không đồng bộ với ngôn ngữ thiết kế của các trang bán hàng hiện hành (`/`, `/products`, `/stores`).
2. **Lỗi phân trang & tham số tìm kiếm:**
   - Thẻ phân trang sinh toàn bộ dải số `sequence(0, totalPages-1)` không có cửa sổ giới hạn (windowing).
   - Thẻ liên kết phân trang `th:href="@{/users(keyword=${keyword},page=${i})}"` làm mất tham số `size`.
3. **Lỗi hiển thị ảnh đại diện:**
   - Chỉ fallback khi chuỗi rỗng: `u.avatarUrl != null and !u.avatarUrl.isEmpty() ? u.avatarUrl : '/images/flower-logo.png'`.
   - Không có bộ xử lý lỗi `onerror` khi URL ảnh đại diện là link cũ bị 404 (ví dụ ảnh legacy trong log), khiến trình duyệt hiện biểu tượng ảnh hỏng.
4. **Header chung:**
   - Header floral có mục "Hồ sơ cá nhân" trỏ cứng `/customer/profile` cho mọi người dùng đã đăng nhập, khiến Admin bấm vào bị chuyển hướng cảnh báo `admin_shopping_denied`.

### 1.4. Bằng chứng Tái hiện A03 & A04 — Bố cục Admin không Thống nhất và Nút Đăng xuất bị Khuất

1. **Phân mảnh Layout giữa 25 Template Admin:**
   - Chỉ có 3 template sử dụng fragment chung `fragments/admin-layout.html`: `dashboard.html`, `chat.html`, `chat-access-logs.html`.
   - 22 template còn lại tự viết sidebar riêng inline với kích thước không nhất quán:
     - `users.html`: sidebar width `225px`
     - `orders-manage.html`: sidebar width `255px`
     - `commissions.html`: sidebar width `260px`
     - `product-form.html`: không có aside riêng hoặc cấu trúc lệch
   - Gây hiện tượng "nhảy giật" kích thước sidebar và sai lệch định dạng khi người dùng chuyển qua lại giữa các chức năng quản trị.
2. **Nút Đăng xuất (A04):**
   - Trong fragment `admin-layout.html`, sidebar chỉ thiết lập `min-height: 100vh` và `ul.mb-auto`.
   - Sidebar không được cố định bám khung nhìn (`sticky-top` / `height: 100vh; overflow-y: auto`), danh sách menu không có vùng cuộn độc lập (`overflow-y: auto`).
   - Khi trang Thống kê (Dashboard) có nội dung dài (nhiều biểu đồ và bảng dữ liệu), sidebar bị kéo dài theo trang, nút "Đăng xuất" ở cuối sidebar bị đẩy sâu xuống dưới mép màn hình, đòi hỏi người dùng phải cuộn hết trang mới thấy.
3. **Template `product-list.html`:**
   - Được xác minh là template thừa (legacy duplicate của `products-manage.html`), không có controller route hay thẻ include nào trỏ tới. Đánh dấu là template legacy không phục vụ.

### 1.5. Bảng Đối Soát 25 Template Admin Hiện Có và Active Key

| STT | Template (`templates/admin/`) | Controller Phục Vụ | Tuyến Đường (Route) | Active Key Chuẩn | Ghi Chú |
|:---:|:---|:---|:---|:---|:---|
| 1 | `dashboard.html` | `AdminDashboardController` | `/admin`, `/admin/dashboard` | `dashboard` | Đang dùng fragment |
| 2 | `users.html` | `AdminUserController` | `/admin/users` | `users` | Sidebar inline 225px |
| 3 | `stores.html` | `AdminStoreController` | `/admin/stores` | `stores` | Sidebar inline |
| 4 | `store-levels.html` | `AdminStoreLevelController` | `/admin/store-levels` | `store-levels` | Sidebar inline |
| 5 | `store-level-form.html` | `AdminStoreLevelController` | `/admin/store-levels/create`, `/admin/store-levels/edit/{id}` | `store-levels` | Form tạo/sửa |
| 6 | `commissions.html` | `AdminCommissionController` | `/admin/commissions` | `commissions` | Sidebar inline 260px |
| 7 | `commission-form.html` | `AdminCommissionController` | `/admin/commissions/create`, `/admin/commissions/edit/{id}` | `commissions` | Form chính sách |
| 8 | `products-manage.html` | `AdminProductManagementController` | `/admin/products`, `/admin/products-manage` | `products` | Quản lý sản phẩm |
| 9 | `product-form.html` | `AdminProductManagementController` | `/admin/products/create`, `/admin/products/edit/{id}` | `products` | Form sản phẩm |
| 10 | `product-list.html` | *(Không có)* | *(Không có)* | `products` | **Legacy không được route dùng** |
| 11 | `orders-manage.html` | `AdminOrderManagementController` | `/admin/orders`, `/admin/orders-manage` | `orders` | Quản lý đơn hàng |
| 12 | `order-detail.html` | `AdminOrderManagementController` | `/admin/orders/{id}`, `/admin/orders-manage/{id}` | `orders` | Chi tiết đơn admin |
| 13 | `categories.html` | `AdminCategoryController` | `/admin/categories` | `categories` | Danh mục hoa |
| 14 | `category-form.html` | `AdminCategoryController` | `/admin/categories/create`, `/admin/categories/edit/{id}` | `categories` | Form danh mục |
| 15 | `styles.html` | `AdminStyleController` | `/admin/styles` | `styles` | Thuộc tính hoa |
| 16 | `style-form.html` | `AdminStyleController` | `/admin/styles/create`, `/admin/styles/edit/{id}` | `styles` | Form thuộc tính |
| 17 | `style-values.html` | `AdminStyleValueController` | `/admin/styles/{id}/values` | `styles` | Giá trị thuộc tính |
| 18 | `deliveries.html` | `AdminDeliveryController` | `/admin/deliveries` | `deliveries` | Đơn vị vận chuyển |
| 19 | `delivery-form.html` | `AdminDeliveryController` | `/admin/deliveries/create`, `/admin/deliveries/edit/{id}` | `deliveries` | Form vận chuyển |
| 20 | `vouchers.html` | `AdminVoucherController` | `/admin/vouchers` | `vouchers` | Quản lý khuyến mãi |
| 21 | `user-levels.html` | `AdminUserLevelController` | `/admin/user-levels` | `user-levels` | Hạng khách hàng |
| 22 | `user-level-form.html` | `AdminUserLevelController` | `/admin/user-levels/create`, `/admin/user-levels/edit/{id}` | `user-levels` | Form hạng khách |
| 23 | `transactions.html` | `AdminTransactionController` | `/admin/transactions` | `transactions` | Lịch sử giao dịch |
| 24 | `chat.html` | `AdminChatController` | `/admin/chat` | `chat` | Đang dùng fragment |
| 25 | `chat-access-logs.html` | `AdminChatController` | `/admin/chat/access-logs` | `chat` | Đang dùng fragment |
| 26 | `notifications.html` *(mới)* | `AdminNotificationController` | `/admin/notifications` | `notifications` | Mới bổ sung cho T02 |

---

## 2. T02 — Sửa Định Tuyến Thông Báo Theo Quyền (Khắc phục A01)

### 2.1. Giải pháp Thiết kế
- Tạo mới service helper `com.finalproject.service.NotificationNavigationResolver`:
  - Xác định URL nội bộ an toàn dựa trên quyền thực tế (`hasRole`) của người nhận được xác thực trên server.
  - Thứ tự ưu tiên quyền: `ADMIN` > `MANAGER` > `USER`.
  - Ma trận định tuyến chuẩn:
    | Ngữ cảnh người nhận | Có relatedOrderId hợp lệ | Không có đơn liên quan |
    | :--- | :--- | :--- |
    | **ADMIN** | `/admin/orders/{id}` | `/admin/notifications#notif_{notificationId}` |
    | **MANAGER** | `/manager/orders/{id}` | `/manager/notifications#notif_{notificationId}` |
    | **USER** | `/customer/orders/detail/{id}` | `/notifications#notif_{notificationId}` |
- Trường `targetUrl` được tính toán phía máy chủ:
  - Bổ sung trường `@Transient private String targetUrl;` trong `Notification.java`.
  - Bổ sung trường `private String targetUrl;` trong `NotificationEvent.java` (WebSocket DTO) và `NotificationController.NotificationDTO`.
  - Trong `NotificationServiceImpl.java`, mọi notification đọc ra từ database được resolve `targetUrl` ngay tại tầng service trước khi trả về controller hoặc publish qua WebSocket.
- Trang quản trị thông báo mới cho Admin:
  - Thêm endpoint `GET /admin/notifications` trong `NotificationController.java`.
  - Tạo mới template `src/main/resources/templates/admin/notifications.html` tích hợp vỏ bọc quản trị chuẩn (`fragments/admin-layout`).
  - Khi người dùng đăng nhập quyền Admin truy cập `/notifications`, hệ thống chuyển hướng 302 sang `/admin/notifications`.
- Đồng bộ phía Client (`notifications-realtime.js`):
  - Toast popup, chuông dropdown, danh sách thông báo realtime và bảng Thymeleaf đều đọc trực tiếp `targetUrl` do server cung cấp.
  - Bổ sung hàm kiểm tra bảo mật `safeTargetUrl`: chỉ chấp nhận URL bắt đầu bằng một dấu `/` cùng origin, từ chối URL chứa schema độc hại (`javascript:`, `http:`, `https:`, `//`). Nếu dữ liệu legacy thiếu `targetUrl`, fallback an toàn theo ngữ cảnh `/admin/notifications`, `/manager/notifications` hoặc `/notifications`.

### 2.2. Kết quả Trước và Sau Sửa (A01)
- **Trước sửa:** Admin bấm vào thông báo "Khách hàng bị khóa" (có `relatedOrderId=4`) -> client sinh URL `/customer/orders/detail/4` -> Spring Security chặn (403/302) -> chuyển hướng về `/admin/dashboard?error=admin_shopping_denied`.
- **Sau sửa:** Admin nhận `targetUrl = "/admin/orders/4"` từ server -> bấm thông báo chuyển trực tiếp đến `/admin/orders/4` -> HTTP 200 OK, mở ngay chi tiết đơn hàng trong khu vực quản trị. Không làm suy yếu quy tắc bảo mật của `/customer/**`.

---

## 3. T03 — Chuẩn Hóa Khung Vỏ Quản Trị (Admin Shell & Layout - Khắc phục A03 & A04)

### 3.1. Thiết kế Hệ thống CSS và Fragment Chung
1. **Tạo mới `src/main/resources/static/css/admin-layout.css`:**
   - **Chiều rộng sidebar cố định:** chính xác `255px` (`width: 255px !important; min-width: 255px !important; max-width: 255px !important;`).
   - **Bám dính khung nhìn (Sticky Viewport - A04):**
     ```css
     .sidebar-admin {
         position: sticky !important;
         top: 0 !important;
         height: 100vh !important;
         height: 100dvh !important;
         display: flex !important;
         flex-direction: column !important;
         overflow: hidden !important;
     }
     ```
   - **Vùng cuộn menu độc lập:**
     ```css
     .admin-sidebar-menu {
         flex: 1 1 auto !important;
         overflow-y: auto !important;
         overflow-x: hidden !important;
         scrollbar-width: thin;
     }
     ```
   - **Nút đăng xuất luôn hiển thị (Pinned Logout - A04):**
     ```css
     .admin-sidebar-footer {
         flex-shrink: 0 !important;
         margin-top: auto !important;
         padding-top: 12px !important;
         border-top: 1px solid rgba(255, 255, 255, 0.1) !important;
     }
     ```
2. **Tạo mới `src/main/resources/static/js/admin-layout.js`:**
   - Điều khiển drawer/offcanvas trên thiết bị di động (< 992px), hỗ trợ nút đóng, backdrop click và phím Escape.
3. **Cập nhật `src/main/resources/templates/fragments/admin-layout.html`:**
   - Thống nhất sidebar duy nhất gồm 15 mục menu theo thứ tự chuẩn: Thống kê, Người dùng, Cửa hàng, Hạng chi nhánh, Sản phẩm, Đơn hàng, Danh mục hoa, Thuộc tính sản phẩm, Đơn vị vận chuyển, Khuyến mãi, Cấp bậc khách, Lịch sử giao dịch, Thông báo hệ thống, Hội thoại CSKH, Xem website.
   - Hỗ trợ tham số active key để tô sáng chính xác mục đang truy cập.

---

## 4. T04 — Chuyển Đổi Đồng Loại 25 Template Admin (Khắc phục A03)

### 4.1. Danh Sách Template Đã Đồng Bộ
Toàn bộ 25 template admin đang phục vụ đã được chuyển đổi sang sử dụng fragment `fragments/admin-layout`:
1. `dashboard.html` — active: `dashboard`
2. `users.html` — active: `users`
3. `stores.html` — active: `stores`
4. `store-levels.html` — active: `store-levels`
5. `store-level-form.html` — active: `store-levels`
6. `products-manage.html` — active: `products`
7. `product-form.html` — active: `products`
8. `orders-manage.html` — active: `orders`
9. `order-detail.html` — active: `orders`
10. `categories.html` — active: `categories`
11. `category-form.html` — active: `categories`
12. `styles.html` — active: `styles`
13. `style-form.html` — active: `styles`
14. `style-values.html` — active: `styles`
15. `commissions.html` — active: `commissions`
16. `commission-form.html` — active: `commissions`
17. `vouchers.html` — active: `vouchers`
18. `user-levels.html` — active: `user-levels`
19. `user-level-form.html` — active: `user-levels`
20. `deliveries.html` — active: `deliveries`
21. `delivery-form.html` — active: `deliveries`
22. `transactions.html` — active: `transactions`
23. `notifications.html` — active: `notifications`
24. `chat.html` — active: `chat`
25. `chat-access-logs.html` — active: `chat`
*(Template `product-list.html` được giữ nguyên trạng với ghi chú legacy không được route dùng).*

### 4.2. Kết quả Đạt được (A03 & A04)
- **Độ rộng sidebar nhất quán:** Mọi trang admin đều có độ rộng computed chính xác 255px, không còn hiện tượng giật lệch 225px / 250px / 260px khi chuyển menu.
- **Bố cục cuộn độc lập:** Khi xem trang Thống kê (Dashboard) có nội dung cuộn rất dài, sidebar vẫn đứng yên trong khung nhìn (100vh), danh sách menu cuộn mượt mà độc lập, và nút Đăng xuất luôn nằm ở góc đáy dưới cùng màn hình trong tầm nhìn của quản trị viên.

---

## 5. T05 — Hiện Đại Hóa `/users` và `/users/{id}` (Khắc phục A02)

### 5.1. Nâng cấp Controller `UserPublicController.java`
- Giữ nguyên cơ chế phân trang tại database và giới hạn kích thước an toàn `pageSize` (tối đa 50).
- Bổ sung đầy đủ model attribute phục vụ phân trang và tìm kiếm: `pageSize`, `totalUsers`, `totalPages`, `currentPage`, `hasPrevious`, `hasNext`, `keyword`.
- Bổ sung `cartItemCount` cho người dùng đăng nhập trong cả `listUsers` và `viewUser`.

### 5.2. Hiện Đại Hóa `templates/customer/users.html`
- Chuyển đổi từ định dạng nén 1 dòng sang cấu trúc HTML hiện đại, chuẩn nhận diện Floral bán hàng:
  - Font tiêu đề Playfair Display, bảng màu xanh đậm `#20382E`, viền `#D1E5DA`.
  - Thanh tìm kiếm đồng bộ, giữ nguyên tham số `size` khi chuyển trang hoặc submit form tìm kiếm.
  - Lưới thẻ thành viên (Member Cards) trực quan, có avatar tròn, họ tên, username, ngày tham gia và huy hiệu chi nhánh đối tác (nếu có).
  - **Khắc phục ảnh 404:** Bổ sung thuộc tính `onerror="this.onerror=null; this.src='/images/flower-logo.png';"` xử lý triệt để ảnh cũ hoặc URL hỏng.
  - **Phân trang dạng cửa sổ trượt (Sliding Window):** Hiển thị các trang lân cận (current ± 2) cùng nút Trước/Sau và nút nhảy trang đầu/cuối, không còn tràn dải số vô hạn.
  - Trạng thái rỗng (Empty State) lịch sự khi tìm kiếm không có kết quả.

### 5.3. Hiện Đại Hóa `templates/customer/user-detail.html`
- Thiết kế hồ sơ thành viên cá nhân đồng bộ ngôn ngữ Floral:
  - Banner ảnh bìa với hiệu ứng chuyển sắc sang trọng và fallback gradient.
  - Ảnh đại diện lớn chồng lớp (overlapping avatar) có viền trắng nổi bật và xử lý fallback 404.
  - Huy hiệu phân loại: Chi nhánh quản lý, Hạng thành viên, hoặc Thành viên hệ thống.
  - Thông tin chi tiết: Tên đăng nhập, họ tên, ngày tham gia, khu vực, trạng thái tài khoản.
  - Thẻ giới thiệu chi nhánh và nút liên kết "Ghé thăm cửa hàng" nếu thành viên quản lý cửa hàng.
  - Nút quay lại danh sách thành viên `/users`.

### 5.4. Sửa Liên Kết Hồ Sơ Trong `fragments/floral.html`
- Cập nhật mục "Hồ sơ cá nhân" tại header bán hàng:
  - Khi tài khoản đăng nhập là `ADMIN` -> trỏ về `/admin/dashboard`.
  - Khi tài khoản đăng nhập là `MANAGER` -> trỏ về `/manager/dashboard`.
  - Khi tài khoản đăng nhập là `USER` -> trỏ về `/customer/profile`.
  - Ngăn ngừa hoàn toàn tình trạng Admin bấm nhầm vào `/customer/profile` bị hệ thống chuyển hướng lỗi `admin_shopping_denied`.

---

## 6. Tổng Hợp Bằng Chứng Kiểm Thử và Nghiệm Thu

### 6.1. Kiểm Thử Backend (JUnit & Spring Boot Tests)
Toàn bộ các bộ kiểm thử tự động đều chạy thành công (`BUILD SUCCESS`):
1. **`NotificationNavigationResolverTest`:** 11/11 tests pass (Kiểm tra ma trận phân giải URL cho Admin, Manager, User, trường hợp thiếu ID, nhiều role ưu tiên Admin).
2. **`NotificationServiceAndControllerTests`:** 15/15 tests pass (Kiểm tra NotificationController và NotificationServiceImpl).
3. **`PublicUserPagingTests`:** 3/3 tests pass (Kiểm tra phân trang, giới hạn clamp kích thước trang, và điều hướng hồ sơ người dùng).
4. **`AdminUsersManagementUseCaseTests`:** 11/11 tests pass (Kiểm tra phân quyền, bảng người dùng, tiêu đề UTF-8, và phân trang trang quản trị người dùng).
5. **`AdminOrdersManagementUseCaseTests`:** 8/8 tests pass (Kiểm tra quản trị đơn hàng admin).
6. **`AdminCustomerRoleSeparationTests`:** 6/6 tests pass (Bảo toàn nguyên tắc phân tách quyền giữa Admin và Customer).
7. **`FixedAccountRoleTests`:** 5/5 tests pass (Kiểm tra các tài khoản cố định của hệ thống).
8. **`StorefrontUiTests`:** 15/15 tests pass (Kiểm tra toàn diện giao diện bán hàng khách hàng).

### 6.2. Kiểm Thử Frontend JavaScript
- Lệnh chạy: `node --test src/test/js/*.test.cjs`
- Kết quả: **123/123 tests passed** (Bao gồm test dropdown chuông thông báo, toast realtime, xác thực URL an toàn `safeTargetUrl`, giỏ hàng, modal, bộ lọc).

### 6.3. Kiểm Thử Trực Tiếp Trên Web Server (`http://localhost:8080`)
1. `GET /users` -> **HTTP 200 OK**: Giao diện mới hiển thị tiêu đề "Cộng Đồng Thành Viên", thanh tìm kiếm, thẻ thành viên và phân trang trượt.
2. `GET /users/1` -> **HTTP 200 OK**: Giao diện hồ sơ mới hiển thị đầy đủ avatar fallback, thông tin thành viên và nút điều hướng (đã bảo mật loại bỏ địa chỉ cá nhân).
3. `GET /admin/notifications` với phiên đăng nhập Admin -> **HTTP 200 OK**: Hiển thị đầy đủ danh sách thông báo quản trị viên, badge chưa đọc, vỏ bọc admin chuẩn.
4. Kiểm tra đồng bộ dòng thương hiệu sidebar trên các trang admin (`/admin/dashboard`, `/admin/users`, `/admin/stores`, `/admin/products-manage`, `/admin/orders-manage`, `/admin/categories`, `/admin/styles`, `/admin/commissions`, `/admin/vouchers`, `/admin/deliveries`, `/admin/notifications`, v.v.) -> 100% đều tích hợp `brand-title-main` và `brand-subtitle-sub` với kích cỡ khóa cứng.

### 6.4. Xử Lý Phản Hồi Bổ Sung (Hotfix 22/10/2026)
1. **Lỗi 404 "Trang hoặc chức năng này không tồn tại" khi truy cập `/admin/notifications`:**
   - **Nguyên nhân:** Tiến trình máy chủ web cục bộ trước đó (PID 4016) được khởi động từ 12:01, trước thời điểm controller thêm route `/admin/notifications`. Do cấu hình `spring.devtools.restart.enabled=false`, Spring Boot không tự quét endpoint mới khi chưa khởi động lại JVM.
   - **Xử lý:** Biên dịch sạch `target/classes`, dừng tiến trình cũ 4016 và khởi động lại dịch vụ qua [`start-web.ps1`](file:///d:/uni/hk1nam3/web/Rental-Retail-Shop/start-web.ps1) (tiến trình mới PID 27776). Tuyến đường `/admin/notifications` đã hoạt động bình thường (HTTP 200).
2. **Kích thước dòng "HOA TƯƠI SÀI GÒN / QUẢN TRỊ VIÊN HỆ THỐNG" chưa đồng bộ:**
   - **Nguyên nhân:** Các trang admin khác nhau có các quy tắc CSS hoặc style inline cạnh tranh, thiếu định danh class riêng biệt với độ ưu tiên cao (`!important`) cho 2 dòng chữ tiêu đề thương hiệu.
   - **Xử lý:**
     - Thiết lập 2 class khóa kích thước tuyệt đối trong [`admin-layout.css`](file:///d:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/resources/static/css/admin-layout.css): `.sidebar-admin .brand-title-main` (font-size: 15px, font-weight: 800, line-height: 1.25, letter-spacing: 0.5px, màu trắng) và `.sidebar-admin .brand-subtitle-sub` (font-size: 10.5px, font-weight: 600, line-height: 1.2, letter-spacing: 0.05em, màu `#9DB7AA`).
     - Cập nhật markup trong [`fragments/admin-layout.html`](file:///d:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/resources/templates/fragments/admin-layout.html) và chuẩn hóa đồng bộ nốt template [`product-list.html`](file:///d:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/resources/templates/admin/product-list.html).
     - Kết quả: Toàn bộ các trang admin đều hiển thị 2 dòng chữ thương hiệu với kích thước, kiểu chữ và màu sắc đồng bộ 100%.

### 6.5. Xử Lý 3 Điểm Đánh Giá Nghiệm Thu (Review Comments)

1. **[P1] Bảo mật quyền riêng tư — Không xuất địa chỉ cá nhân lên hồ sơ công khai:**
   - **Vấn đề:** Trang `/users/{id}` công khai xuất nguyên `user.address` dưới nhãn "Khu vực", cho phép người dùng chưa đăng nhập (khách vãng lai) truy cập `/users/1` thấy được địa chỉ nhà riêng của người dùng.
   - **Giải pháp:**
     - Loại bỏ toàn bộ trường hiển thị `user.address` và nhãn "Khu vực" khỏi [`user-detail.html`](file:///d:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/resources/templates/customer/user-detail.html).
     - Chỉ hiển thị thông tin công khai an toàn: tên đăng nhập (`user.username`), họ tên (`user.fullName`), ngày tham gia, trạng thái hoạt động và thông tin chi nhánh công khai nếu người dùng quản lý cửa hàng đối tác (`user.store.address`, `user.store.phone`).
     - **Kiểm chứng:** Gửi yêu cầu HTTP không kèm session tới `http://localhost:8080/users/1` trả về **HTTP 200 OK**, phản hồi HTML hoàn toàn không chứa chuỗi `address` hoặc `Khu vực`.

2. **[P2] Đồng bộ cơ chế mở sidebar chat trên mobile (< 992px):**
   - **Vấn đề:** CSS mobile mới trong `admin-layout.css` ẩn `.sidebar-admin` dưới 992px bằng `transform: translateX(-100%); visibility: hidden;` dựa trên `body.admin-menu-open`. Tuy nhiên, trang chat (`chat.html`) sử dụng cơ chế drawer riêng với nút `#chatNavigationToggle` kích hoạt class `.is-open` trên `#chatAdminNavigation`, không bật class trên `body`, dẫn đến sidebar chat bị ẩn hoàn toàn khi mở drawer. Ngoài ra, breakpoint mở navigation của chat trước đó ở mức 768px, lệch với ngưỡng 992px của admin.
   - **Giải pháp:**
     - Cập nhật [`admin-layout.css`](file:///d:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/resources/static/css/admin-layout.css): định nghĩa quy tắc cho `.chat-admin-navigation .sidebar-admin` và `.admin-chat-page .sidebar-admin` với `position: static !important; transform: none !important; visibility: visible !important; width: 100% !important; height: 100% !important;`.
     - Cập nhật [`chat-widget.css`](file:///d:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/resources/static/css/chat-widget.css): đồng bộ breakpoint drawer cho chat sang `@media (max-width: 991.98px)`, đảm bảo nút `#chatNavigationToggle` hiển thị và `#chatAdminNavigation` đóng mở chính xác dạng fixed drawer (z-index 1050) trên mọi thiết bị tablet và mobile dưới 992px.
     - **Kiểm chứng:** Bộ kiểm thử JavaScript `src/test/js/chat-widget.test.cjs` (bao gồm test đóng mở phím Escape, click toggle và responsive workbench) vượt qua 100% (123/123 tests pass).

3. **[P2] Kiểm tra quyền hiệu lực trong overload `resolveRole(User user)`:**
   - **Vấn đề:** Thực tế luồng `NotificationServiceImpl` gọi overload nhận thực thể `User` (`resolveTargetUrl(User, ...)`). Nếu phương thức này chỉ đọc thuộc tính đơn `user.getRole()`, những tài khoản sở hữu quyền bổ sung trong tập hợp `user.getRoles()` (ví dụ tài khoản có vai trò chính là `MANAGER` nhưng có thêm role `ADMIN`) sẽ bị phân giải sai URL đích.
   - **Giải pháp:**
     - Tái cấu trúc logic `resolveRole(User user)` trong [`NotificationNavigationResolver.java`](file:///d:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/java/com/finalproject/service/NotificationNavigationResolver.java):
       ```java
       public Role resolveRole(User user) {
           if (user == null) {
               return Role.USER;
           }
           if (user.hasRole(Role.ADMIN) || (user.getRoles() != null && user.getRoles().contains(Role.ADMIN)) || user.getRole() == Role.ADMIN) {
               return Role.ADMIN;
           }
           if (user.hasRole(Role.MANAGER) || (user.getRoles() != null && user.getRoles().contains(Role.MANAGER)) || user.getRole() == Role.MANAGER) {
               return Role.MANAGER;
           }
           return Role.USER;
       }
       ```
     - Cả hai overload (dựa trên thực thể `User` và dựa trên Spring Security `Authentication`) đều kiểm tra nhất quán nguyên tắc: quyền `ADMIN` luôn được ưu tiên tuyệt đối trước `MANAGER`, sau đó đến `USER`.
     - Bổ sung ca kiểm thử trong [`NotificationNavigationResolverTest.java`](file:///d:/uni/hk1nam3/web/Rental-Retail-Shop/src/test/java/com/finalproject/NotificationNavigationResolverTest.java) mô phỏng người dùng có `role = MANAGER` nhưng `roles` chứa cả `ADMIN` và `MANAGER`.
     - **Kiểm chứng:** Bộ kiểm thử `NotificationNavigationResolverTest` vượt qua 11/11 tests, `NotificationServiceAndControllerTests` vượt qua 15/15 tests.

4. **Làm sạch hoàn toàn lỗi whitespace (`git diff --check`):**
   - Loại bỏ toàn bộ khoảng trắng thừa (trailing whitespace) ở cuối dòng trong các file template:
     - `customer/notifications.html`
     - `customer/user-detail.html`
     - `customer/users.html`
     - `manager/notifications.html`
   - **Kiểm chứng:** Lệnh `git diff --check` chạy thành công không có bất kỳ cảnh báo whitespace nào (exit code 0).

---

## 7. Kết Luận
- Cả 4 mã yêu cầu **A01, A02, A03, A04**, 2 phản hồi runtime hotfix cùng **toàn bộ 3 điểm rà soát (P1, P2, P2)** đã được xử lý triệt để và kiểm chứng đạt 100%.
- Không có bất kỳ thay đổi nào làm suy yếu logic phân quyền Spring Security hoặc thay đổi dữ liệu nghiệp vụ (Flyway, đơn hàng, thanh toán, ví).
- `git diff --check` sạch hoàn toàn, không có whitespace thừa.
- Không thực hiện thao tác `git push`, toàn bộ mã nguồn được lưu trữ tại working tree cục bộ.
