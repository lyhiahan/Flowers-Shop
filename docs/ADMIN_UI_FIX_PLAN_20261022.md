# Hướng dẫn sửa lỗi thông báo, trang /users và bố cục admin

Ngày lập: 22/10/2026. Đây là kế hoạch triển khai, **chưa phải báo cáo đã sửa**. Người dùng xác nhận “các trang tài khoản từ phiên bản cũ” là **`/users`**, không phải `/admin/users`.

## 1. Phạm vi và kết quả phải đạt

| Mã | Lỗi người dùng báo | Kết quả bắt buộc |
| --- | --- | --- |
| A01 | Bấm thông báo khách hàng bị khóa từ admin thì báo lỗi | Admin mở đúng đơn hàng trong khu vực admin; mọi cách mở thông báo dùng cùng quy tắc. Không làm yếu kiểm tra quyền. |
| A02 | `/users` còn giao diện cũ | Danh sách thành viên và hồ sơ `/users/{id}` theo giao diện bán hàng hiện hành; tìm kiếm/phân trang/đường dẫn vẫn hoạt động. |
| A03 | Sidebar thay đổi khi chuyển chức năng | Tất cả trang admin đang được phục vụ dùng cùng sidebar, cùng thứ tự, chiều rộng, kiểu chữ, nút và quy tắc active. |
| A04 | Nút đăng xuất nằm quá sâu ở thống kê | Sidebar bám khung nhìn; chỉ danh sách menu cuộn; đăng xuất luôn nhìn thấy khi mở menu ở kích thước nghiệm thu. |

Không đổi nghiệp vụ khóa tài khoản, hủy/hoàn đơn, thanh toán, ví hoặc phân quyền. Không chỉnh dữ liệu demo để che lỗi UI. Không mở rộng thành thiết kế lại toàn website.

## 2. Những gì đã xác minh trong mã nguồn

### A01: thiếu đích đến cho admin

1. `src/main/java/com/finalproject/service/CustomerAccountClosureService.java`: khi khóa khách có đơn đang xử lý, gửi admin thông báo tiêu đề “Khách hàng bị khóa”, type `ORDER_UPDATE`, kèm **relatedOrderId**. Đây là thông báo về đơn chịu ảnh hưởng, không phải liên kết tới hồ sơ khách.
2. `src/main/resources/static/js/notifications-realtime.js`: toast, dropdown và chèn thông báo vào danh sách đều tự tính URL. Logic chỉ phân biệt manager/customer, không có nhánh admin.
3. `src/main/java/com/finalproject/controller/common/NotificationController.java`: trang thông báo chỉ chọn template manager/customer; API unread-count chỉ cung cấp `isManager`; latest không có URL đích do server tính.
4. `src/main/java/com/finalproject/config/SecurityConfig.java`: `/customer/**` yêu cầu USER. Admin bị đưa vào đó có thể bị chuyển về dashboard với `error=admin_shopping_denied`. Đây là kiểm soát quyền có chủ đích, cần giữ nguyên.
5. `AdminOrderManagementController` đã có route admin xem đơn: `/admin/orders/{id}` và alias `/admin/orders-manage/{id}`. Không cần tạo route chi tiết đơn thứ ba.

**Kết luận ở mức đọc code:** có lỗi định tuyến thông báo với admin. Chưa tái hiện click của người dùng trong phiên admin nên không khẳng định thông báo lỗi cụ thể là HTTP 403, 500 hay redirect có cảnh báo. Agent triển khai phải ghi lại chuỗi request trước sửa theo bước T01.

### A02: xác định đúng trang

- Controller: `src/main/java/com/finalproject/controller/customer/UserPublicController.java`.
- Template: `src/main/resources/templates/customer/users.html` và `customer/user-detail.html`.
- Cả hai đang dùng `fragments/floral.html`; đây là giao diện thành viên công khai, **không phải bảng quản trị tài khoản**.
- Controller đã phân trang tại database và giới hạn size tối đa 50 trong đợt sửa trước. Phải giữ phần này.
- `/users` đang sinh liên kết cho mọi số trang và link không giữ size; ảnh chỉ fallback khi URL trống, chưa xử lý ảnh URL cũ bị 404.
- Header floral có link “Hồ sơ cá nhân” trỏ `/customer/profile` cho mọi tài khoản đã đăng nhập; cần rà hiển thị theo quyền để admin không gặp lại nhánh mua hàng bị chặn.
- Log web có ảnh cũ `/upload/avatar/img_e69df3eb81aa.jpg` trả 404. Đây là dấu hiệu cần kiểm tra ảnh legacy, không đủ để kết luận nguyên nhân mọi khác biệt giao diện.

### A03–A04: nhiều nguồn layout và CSS xung đột

- Trong **25 template** dưới `templates/admin`, chỉ `dashboard.html`, `chat.html`, `chat-access-logs.html` dùng sidebar chung `fragments/admin-layout.html`.
- 22 template còn lại chưa dùng sidebar chung; `product-form.html` chưa có aside riêng, nhiều trang khác sao chép cả menu/CSS.
- `users.html` tự khai báo sidebar 225px; form có nơi 250px; orders 255px; commissions 260px. Giá trị computed thực tế còn chịu ảnh hưởng CSS global.
- Sidebar chung chỉ có `min-height:100vh` và `ul.mb-auto`, không giới hạn chiều cao theo viewport hoặc vùng cuộn menu độc lập. Nội dung dashboard dài có thể kéo cả sidebar và footer xuống.
- `static/css/floral-theme.css` có nhiều selector `.sidebar` và rule responsive `!important`, trong khi fragment và template lại khai báo thêm rule khác.
- `static/css/chat-widget.css` có layout riêng cho admin chat, drawer mobile và chiều cao 100dvh. Không được sửa sidebar chung rồi bỏ quên ngoại lệ này.

## 3. Nguyên tắc bắt buộc đối với agent thực hiện

1. **Đọc rồi mới sửa:** xem `git status`, các quy tắc trong `.agents`, controller, service, template, CSS và test liên quan. Working tree đã có nhiều thay đổi từ đợt trước; không reset/clean/checkout đè chúng. Giữ nguyên `start-web.ps1` của người dùng.
2. **Một lỗi, một bằng chứng:** mỗi A01–A04 phải có bước tái hiện, kết quả trước sửa và kết quả sau sửa. Không dùng “HTTP 200” thay cho kiểm tra bố cục.
3. **Không chữa bằng mở quyền:** không thêm ADMIN vào `/customer/**`, không bỏ kiểm tra chủ đơn/chi nhánh, không cho đọc thông báo người khác. Đổi URL để tới chức năng có quyền sẵn.
4. **Một nguồn menu:** chỉ fragment admin được định nghĩa danh sách menu. Trang con chỉ truyền active key; không copy sidebar sang các trang.
5. **Một nguồn URL thông báo:** URL do backend tính từ người nhận đã xác thực; JS và template chỉ sử dụng kết quả. Không dùng tiêu đề tiếng Việt, chuỗi content hoặc pathname để đoán quyền.
6. **Không nhận URL tùy ý từ nội dung:** chỉ tạo đường dẫn nội bộ từ route cho phép và ID hợp lệ. Không redirect tới `http:`, `https:`, `javascript:`, `//host` hoặc URL trong query/content do người dùng nhập.
7. **Giữ an toàn đầu ra:** tên, tiêu đề, nội dung, tên shop dùng `th:text`/textContent; không ghép dữ liệu chưa escape vào innerHTML. JSON không chứa password hash, OTP hoặc entity User.
8. **CSS có phạm vi:** layout admin dùng class riêng; CSS trang chỉ chỉnh nội dung trang. Không thêm các lớp `!important` nối tiếp nhau để thắng CSS cũ. Tìm và xử lý rule cạnh tranh trước.
9. **Giữ hợp đồng chức năng:** URL POST, CSRF, input name, model attribute, ID/testid đang được JS/test dùng phải được giữ hoặc cập nhật đầy đủ bên sử dụng. Không xóa một mục menu chỉ vì một trang không có nó.
10. **Không thay dữ liệu cho công việc giao diện:** đợt này mặc định không cần migration SQL. Không xóa tài khoản cũ, không đổi trạng thái, không sửa lịch sử Flyway V11–V15, không seed lại database.
11. **Sửa theo thứ tự ở mục 4:** hoàn thành và kiểm tra từng gói trước khi chuyển tiếp; không thay 25 template bằng regex rồi coi là hoàn tất.
12. **Không báo đạt khi chưa chạy:** ghi rõ tự động/thủ công/chưa kiểm tra. Test static có fragment không chứng minh nút đăng xuất nằm trong viewport.
13. **Không push:** chỉ push khi người dùng yêu cầu rõ ràng. Không tự commit các thay đổi của đợt trước chung vào công việc này.
14. **Không chạy Maven đồng thời:** chạy compile/test/start lần lượt để tránh tranh chấp `target/classes`. Muốn dừng web để kiểm tra thì xác minh đúng PID của dự án; báo trạng thái và khởi động lại sau kiểm thử.

## 4. Trình tự công việc có đầu vào và đầu ra rõ ràng

### T01 — Thu thập baseline và tái hiện

**Chỉ đọc trước.** Lưu kết quả trong `docs/ADMIN_UI_FIX_RESULTS_20261022.md` khi bắt đầu thực hiện; không ghi mật khẩu, cookie hoặc CSRF token vào tài liệu.

1. Ghi commit hiện tại, `git status --short`, URL web, database và PID. Dùng `scripts/run-local.ps1`, bật Flyway, tắt seed/tunnel.
2. Với phiên ADMIN hợp lệ, mở thông báo “Khách hàng bị khóa”. Ghi notification ID, relatedOrderId, vị trí bấm (toast/dropdown/trang danh sách), URL đích, status và chuỗi redirect. Kiểm tra đơn đó tồn tại và admin có quyền mở `/admin/orders/{id}` trực tiếp.
3. Nếu phải tạo thông báo để tái hiện, dùng fixture test hoặc database bản sao; không khóa thêm người dùng trong database đang phục vụ chỉ để lấy ảnh.
4. Mở `/users` rồi một `/users/{id}`: chụp bố cục, header, ảnh đại diện và trạng thái tìm kiếm. Đối chiếu với trang bán hàng hiện hành trên cùng viewport.
5. Mở dashboard, users admin, orders, commissions và chat: ghi các mục menu, chiều rộng computed, phần tử active, vị trí logout trước/sau cuộn.
6. Lập bảng controller route → template → active key cho 25 file ở mục 5. File nào không còn route phục vụ thì đánh dấu “legacy không được route dùng” sau khi tìm cả controller và include; không tự xóa ngay.

**Hoàn thành T01 khi:** có bằng chứng tái hiện hoặc test fixture tái hiện được từng lỗi; nguyên nhân còn nghi vấn phải được ghi rõ, không tự đoán.

### T02 — Sửa định tuyến thông báo theo quyền, xử lý A01 trước

**Thiết kế chốt:** thêm helper backend `NotificationNavigationResolver` trong package service. Helper trả URL nội bộ, không truy cập HTTP hoặc nhận role do client truyền. Dùng `hasRole`/quyền hiệu lực, ưu tiên ADMIN trước MANAGER; manager vẫn phải qua kiểm tra chi nhánh tại endpoint đích.

| Ngữ cảnh người nhận | Có relatedOrderId hợp lệ | Không có đơn liên quan |
| --- | --- | --- |
| ADMIN | `/admin/orders/{id}` | `/admin/notifications#notif_{notificationId}` |
| MANAGER | `/manager/orders/{id}` | `/manager/notifications#notif_{notificationId}` |
| USER | `/customer/orders/detail/{id}` | `/notifications#notif_{notificationId}` |

1. Thêm kiểm thử resolver theo ma trận trên, bao gồm nhiều quyền (ADMIN ưu tiên), ID null/không hợp lệ. Không dựa vào chuỗi title “bị khóa”. Notification ID null thì bỏ anchor, vẫn trả trang danh sách an toàn.
2. Thêm `targetUrl` vào dữ liệu latest và DTO WebSocket `NotificationEvent`; cùng resolver phải tạo ra chúng. Giữ toàn bộ field hiện có để tương thích JS. DTO vẫn không chứa quan hệ User.
3. Với WebSocket, xác định URL từ người nhận và chụp DTO khi còn trong transaction; vẫn chỉ publish **sau commit** như cơ chế hiện tại. Không truy cập lazy entity sau transaction để tính URL.
4. Với thông báo cũ, tính targetUrl khi đọc. Không cần cập nhật từng hàng notification hoặc thêm cột database chỉ để sửa navigation.
5. Thêm `/admin/notifications` và template `admin/notifications.html`, chỉ admin truy cập. Hiển thị danh sách, badge chưa đọc, “đã đọc”, “đã đọc tất cả” bằng service hiện có, trong shell admin chung.
6. `/notifications` khi người dùng là admin chuyển đến `/admin/notifications`; manager dùng trang manager; customer dùng trang customer. Tránh vòng lặp redirect. Giữ đường dẫn đã phát hành như alias có chủ đích.
7. Cập nhật **cả bốn chỗ**: toast, dropdown chuông, danh sách realtime, danh sách render từ Thymeleaf. Chỉ đọc targetUrl đã tính; khi dữ liệu cũ chưa có targetUrl thì fallback tới trang thông báo theo ngữ cảnh đã xác thực, không đoán customer order.
8. Trước khi dùng targetUrl trong JS, chỉ chấp nhận đường dẫn bắt đầu một dấu `/` cùng origin và thuộc route thông báo/đơn đã cho phép; URL sai dùng fallback. Escape href bằng API DOM/th:href phù hợp.
9. Đánh dấu đọc vẫn phải kiểm tra quyền sở hữu và CSRF. API đọc lỗi không được giả lập badge giảm thành công. Điều hướng xem chi tiết không được hủy đơn/khóa khách/thực hiện thao tác nghiệp vụ.
10. Đơn không còn tồn tại: hiển thị thông báo 404 có nút về danh sách đơn **admin**, hoặc redirect danh sách admin kèm flash giải thích theo quy ước endpoint. Không để NPE/500 hoặc redirect sang cart. Truy cập không có quyền vẫn là 403.

**Không sửa:** `CustomerAccountClosureService` để đổi relatedOrderId thành userId; chính sách bảo mật `SecurityConfig`; các quy tắc hủy/hoàn tiền khi khóa khách.

**Hoàn thành T02 khi:** cùng notification từ 4 bề mặt dẫn đúng URL admin; khách/quản lý không xem được đơn trái quyền; thông báo cũ vẫn mở được; danh sách admin không dùng template customer.

### T03 — Tạo shell admin chuẩn, xử lý gốc A03–A04

File chính: `fragments/admin-layout.html`; tạo `static/css/admin-layout.css` và `static/js/admin-layout.js` nếu cần cho menu mobile. Tách CSS layout khỏi `<style>` fragment để dễ tìm nguồn; tải sau CSS nền, tải đúng một lần.

**Cấu trúc cố định:**

```text
admin-layout-wrapper
├── sidebar-admin
│   ├── brand                     (không co lại)
│   ├── admin-navigation          (flex:1; min-height:0; overflow-y:auto)
│   └── admin-session-actions     (đăng xuất; không co lại)
└── main-wrapper-admin            (min-width:0; nội dung trang)
```

1. Desktop ≥992px: chiều rộng sidebar 255px, không co; `position:sticky; top:0; align-self:flex-start; height:100vh; height:100dvh; min-height:0`. Nội dung dài cuộn trang, sidebar không dài theo nội dung. Menu có scrollbar riêng; brand/logout không nằm trong vùng menu cuộn.
2. Footer có background, padding/border thống nhất, logout `POST /logout` giữ CSRF. Không dùng `position:fixed` cho riêng nút để phủ menu/nội dung. Không dùng spacer/`margin-top` theo chiều cao dashboard.
3. Loại class `.sidebar` generic khỏi shell mới nếu nó làm dính rule legacy; giữ class riêng `.sidebar-admin` và cập nhật các selector liên quan. Rà cả `floral-theme.css`, inline style và CSS chat; ghi rule đã thay và trang chịu ảnh hưởng. Không thay CSS global theo cách làm hỏng manager/customer.
4. <992px: sidebar thành drawer/offcanvas dùng **một cơ chế chung**; nút “Mở menu quản trị” luôn có ở các trang, có aria-controls/expanded, Escape đóng, focus trở lại nút mở, backdrop không chặn trang sau khi đóng. Menu bên trong vẫn cuộn riêng và footer nhìn thấy khi drawer mở.
5. Dùng Bootstrap offcanvas hiện có nếu phù hợp; không cài framework UI mới. Tránh hai nút/hai handler offcanvas cùng tồn tại ở trang chat. Khi đổi kích thước qua breakpoint phải đóng/xóa trạng thái mobile đúng cách.
6. Giữ chat ở chế độ nội dung vừa viewport; thống nhất chiều cao sidebar và footer với shell. Kiểm tra `chatNavigationToggle`, `chatAdminNavigation`, `chatNavigationClose` và handlers trong `chat-widget.js` trước khi chuyển sang handler chung. Xóa handler cũ chỉ sau khi chuyển đủ nơi sử dụng.
7. Header/main/sidebar có cùng khoảng cách, màu nền, font giữa các trang. CSS bảng, biểu đồ, form được phép khác theo chức năng; không được đổi kích thước sidebar.
8. `active` truyền bằng key ở mục 5. Chỉ đúng một link chức năng active, có `aria-current="page"`; form tạo/sửa và chi tiết dùng key chức năng cha. “Xem website” là link phụ, không active.
9. Bổ sung CSRF meta trong assets admin nếu dùng fetch cho notification; giữ form token. Bootstrap, notification JS, SockJS/STOMP mỗi loại chỉ tải một lần ở trang cần dùng.

**Hoàn thành T03 khi:** dashboard dài vẫn thấy logout, menu cuối truy cập được, chat không tràn viewport, drawer mobile đóng/mở được bằng chuột và bàn phím.

### T04 — Chuyển từng nhóm template sang shell chung

Thứ tự: dashboard làm chuẩn → users/stores/orders → sản phẩm → danh mục/thuộc tính → cấp bậc/hoa hồng/voucher/vận chuyển/giao dịch → chat và trang thông báo mới. Với **mỗi template**, lặp đúng quy trình sau:

1. Xác định GET route và dữ liệu cần có; tạo fixture nếu trang yêu cầu ID.
2. Ghi các CSS/JS, model attributes, form actions, ID/testid đang được dùng.
3. Đổi head sang assets chung; nếu có tài nguyên trang riêng, dùng block bổ sung hợp lệ để không bị `th:replace` xóa mất. Không chèn nhiều `<head>` hoặc mất chart/upload script.
4. Gắn `body.admin-body`, wrapper, fragment sidebar(activeKey), main và scripts chung.
5. Xóa **chỉ** menu/CSS khung cũ đã được thay; giữ form/bảng/modal và script nghiệp vụ. Không để hai `<aside>` hoặc hai logout trong DOM render.
6. Mở GET bằng admin, kiểm tra render và chức năng quan trọng của trang. Kiểm tra CSS computed và active trước khi sang template tiếp theo.
7. Sau mỗi nhóm, chạy test render/route liên quan. Không để lỗi template dồn tới cuối.

### T05 — Làm mới /users và /users/{id}, xử lý A02

1. Giữ `/users` là trang public và `/admin/users` là trang quản trị. Không gắn sidebar admin vào `/users`, không redirect mọi admin truy cập public sang trang quản trị.
2. Lấy header/footer, font, màu, spacing và card từ giao diện bán hàng hiện hành (`/`, `/products`, `/stores`), thống nhất một bộ token. Không copy cả trang home vào users. Nếu sửa `fragments/floral.html`, liệt kê tất cả trang dùng fragment và smoke test chúng.
3. Định dạng lại hai template đang viết dồn một dòng để có thể review. Danh sách cần tiêu đề, mô tả ngắn, ô tìm kiếm có label, tổng kết quả, card thành viên, empty state và pagination rõ ràng.
4. Desktop: lưới 3 cột; tablet: 2; mobile: 1. Avatar cùng kích thước, object-fit cover; tên dài xuống dòng hợp lý; không để hàng card có nút nhảy vị trí. Profile có nút về danh sách, ảnh bìa/ảnh đại diện và thông tin công khai hiện có.
5. URL avatar/cover trống hoặc 404: dùng ảnh cục bộ mặc định; fallback chỉ chạy một lần để tránh vòng lặp onerror. Không tự thay URL trong database và không tải lại avatar thất lạc từ nguồn không xác định.
6. Giữ filter/page/size khi phân trang, clamp page và size như controller hiện tại; kết quả tìm kiếm mới trở về page 0. Dùng cửa sổ pagination hữu hạn (đầu/trước, tối đa 5 số quanh trang hiện tại, sau/cuối), không render hàng nghìn số trang.
7. Không hiện email, số điện thoại, địa chỉ, password, OTP hoặc thông tin tài chính lên trang public. Không mở rộng việc tìm kiếm/hiển thị thông tin cá nhân ngoài hợp đồng hiện có trong đợt UI này.
8. Header theo quyền: USER mới có link `/customer/profile`; ADMIN có link quản trị; MANAGER có kênh quản lý. Không tạo link hồ sơ admin tới customer route bị chặn. Nếu không có trang hồ sơ admin, không tự xây thêm chức năng chỉnh tài khoản trong đợt này.
9. Thành viên ID không tồn tại phải có hành vi có chủ đích (giữ redirect `/users` hiện có hoặc trang 404 thân thiện), không 500. Các tài khoản cũ thiếu ảnh/tên/ngày tham gia không được làm template lỗi.
10. Giữ chat widget, chuông thông báo và login/logout đúng quyền, không khởi tạo script hai lần. Không biến mất trạng thái tìm kiếm sau Back/Forward.

**Hoàn thành T05 khi:** `/users` và `/users/{id}` đồng bộ với trang bán hàng hiện hành, responsive, đọc được dữ liệu legacy có trường thiếu và không lộ dữ liệu riêng tư.

## 5. Danh mục template và active key bắt buộc

Menu chuẩn giữ thứ tự của fragment hiện hành: Thống kê → Người dùng → Cửa hàng → Hạng chi nhánh → Chính sách hoa hồng → Sản phẩm → Đơn hàng → Danh mục hoa → Thuộc tính sản phẩm → Đơn vị vận chuyển → Khuyến mãi → Cấp bậc khách → Lịch sử giao dịch → Trung tâm hội thoại → **Thông báo** (mới) → Xem website. Đăng xuất ở footer riêng. Tất cả trang admin có cùng danh mục này; chức năng theo trang chỉ thay active.

| Template trong templates/admin | Active key |
| --- | --- |
| dashboard.html | dashboard |
| users.html | users |
| stores.html | stores |
| store-levels.html, store-level-form.html | store-levels |
| commissions.html, commission-form.html | commissions |
| products-manage.html, product-form.html, product-list.html | products |
| orders-manage.html, order-detail.html | orders |
| categories.html, category-form.html | categories |
| styles.html, style-form.html, style-values.html | styles |
| deliveries.html, delivery-form.html | deliveries |
| vouchers.html | vouchers |
| user-levels.html, user-level-form.html | user-levels |
| transactions.html | transactions |
| chat.html, chat-access-logs.html | chat |
| notifications.html (mới) | notifications |

`product-list.html` cần kiểm tra còn được controller/include dùng hay không; không dựng route giả chỉ để làm đủ bảng. Template legacy không phục vụ phải được ghi riêng và có quyết định rõ ràng trước khi bỏ.

## 6. Bộ kiểm thử và bằng chứng bàn giao

### Tự động: các ca tối thiểu cần thêm

- **Notification navigation:** ADMIN/MANAGER/USER × có/không có order; admin nhận thông báo khóa khách; thông báo cũ; dữ liệu URL không hợp lệ; role ưu tiên; order mất/ID sai; API latest và WebSocket cùng targetUrl; vẫn chỉ publish sau commit.
- **Quyền:** admin GET đúng chi tiết đơn; customer không vào `/admin/**`; manager sai chi nhánh bị chặn; user không mở đơn người khác; user A không đánh dấu notification của B là đã đọc; POST thiếu CSRF bị từ chối. Không thay expectation thành 200 để che lỗ hổng.
- **Render admin:** mỗi GET route đang dùng ở mục 5 được render với fixture đúng, duy nhất một sidebar/logout, cùng thứ tự href/label/id, đúng một active key. So sánh DOM render sau Thymeleaf, không chỉ regex file nguồn.
- **/users:** anonymous và các role mở được; tìm kiếm có/không kết quả; page âm/quá lớn/size quá lớn; giữ keyword/size khi phân trang; profile thiếu ảnh/tên/ngày; ID không tồn tại; không có trường nhạy cảm trong HTML.
- **JS:** mở notification từ toast/dropdown/realtime list dùng targetUrl giống nhau; badge chỉ đổi khi server thành công; drawer hoạt động; fallback avatar không lặp. Mở rộng `notifications-realtime.test.cjs` hiện có, không viết test chỉ kiểm tra chuỗi implementation tồn tại.

Tham chiếu test hiện hành: `NotificationServiceAndControllerTests`, `PublicUserPagingTests`, `FixedAccountRoleTests`, các test chat và `src/test/js/notifications-realtime.test.cjs`. Đọc fixture hiện có trước khi tạo test mới để tránh vô tình vô hiệu hóa AccountStatusFilter bằng tài khoản giả không tồn tại.

Chạy từng lệnh **lần lượt**, điều chỉnh danh sách test mục tiêu theo class thực tế vừa thêm:

```powershell
# Chạy nhóm test liên quan trước, sau đó toàn bộ Java.
.\scripts\maven.ps1 -MavenArguments @('test','-B')
node --test src/test/js/*.test.cjs
git diff --check
# Sau khi test đã kết thúc, khởi động lại web.
.\scripts\run-local.ps1 -DatabaseName RentalShop -Mode Run
```

Không cần chạy lại migration live chỉ vì sửa CSS/URL; nếu thật sự thay schema/service dữ liệu thì bổ sung kiểm tra tương ứng và giải thích vì sao scope tăng.

### Thủ công hoặc automation trình duyệt có kiểm tra hình học

| Nhóm | Các trường hợp | Kết quả phải quan sát |
| --- | --- | --- |
| Chuyển menu | Dashboard → users admin → orders → commissions → form sản phẩm → chat → notifications → dashboard | Menu giữ nguyên thứ tự/nội dung; width desktop 255px, x/sidebar và mép nội dung không nhảy; active đúng cha. |
| Dashboard dài | Đầu trang, giữa/cuối biểu đồ, đổi khoảng ngày, cuộn menu | Logout luôn trong viewport; truy cập được mục menu cuối; không đè content. |
| Kích thước | 1440×900, 1366×768, 1024×640, 768×1024, 390×844, 360×640 | Không tràn ngang toàn trang; mobile có nút mở/đóng menu; logout thấy được khi menu mở; chart/bảng cuộn trong vùng riêng nếu cần. |
| Zoom/bàn phím | Desktop zoom 125%/200%, Tab, Enter, Escape | Menu cuộn được, focus thấy rõ; logout không mất; drawer đóng và trả focus. |
| Thông báo | Cũ/mới, toast/dropdown/list, admin không có quyền USER | Đích admin đúng; không cảnh báo mua hàng bị chặn, không vòng lặp/500; unread đúng. |
| Thành viên | /users, tìm tên tiếng Việt, không có kết quả, trang cuối, profile legacy | Cùng phong cách bán hàng; fallback ảnh đúng; trạng thái tìm kiếm/phân trang được giữ. |
| Hồi quy | Chat desktop/mobile, manager dashboard, trang sản phẩm/cửa hàng | Không vỡ CSS hoặc drawer do thay stylesheet chung. |

Để xác nhận logout, đo `getBoundingClientRect()` sau render: top ≥0, bottom ≤ chiều cao viewport (sai số khoảng 2px), button có kích thước >0 và không bị overlay che. Kiểm tra lúc menu ở đầu/cuối và body đã cuộn sâu. Ảnh chụp cần có kích thước viewport, route và thời điểm; không chụp mật khẩu/token.

## 7. Điều kiện dừng, hoàn thành và báo cáo

**Dừng thay đổi phụ thuộc và báo rõ khi:** phát hiện thông báo đang trỏ loại tài nguyên khác với relatedOrderId; không có fixture/phiên admin hợp lệ để tái hiện; route được giả định không tồn tại; hoặc buộc phải thay chính sách quyền/nghiệp vụ. Vẫn có thể tiếp tục phần CSS/template độc lập. Không hỏi lại những điều đã được người dùng chốt: trang cũ là `/users`, dữ liệu liên quan là demo.

**Chỉ đánh dấu hoàn thành toàn bộ khi:**

- [ ] A01 có bằng chứng trước/sau; cùng notification mở đúng từ cả bốn bề mặt.
- [ ] A02 hoàn tất cho `/users` và trang hồ sơ đi kèm; không nhầm thành sửa riêng `/admin/users`.
- [ ] A03 phủ mọi template admin đang được route dùng, kể cả form/chi tiết/chat; legacy không phục vụ có ghi chú.
- [ ] A04 logout luôn hiện trong các viewport đã nêu và khi dashboard dài.
- [ ] Test mới và test hồi quy đạt; bỏ qua/chưa chạy được giải thích cụ thể.
- [ ] Không thay quyền để né lỗi; không sửa dữ liệu; không mất script, CSRF hoặc mục menu.
- [ ] Web được khởi động lại, có URL/PID/schema/Flyway và smoke test sau thay đổi.
- [ ] Báo cáo `ADMIN_UI_FIX_RESULTS_20261022.md` có file đã sửa, nguyên nhân, test, screenshot, hạn chế và trạng thái git; không tuyên bố deploy/commit/push khi chưa thực hiện.

Agent phải báo từng A01–A04 là **đạt / chưa đạt / chưa kiểm chứng**, kèm bằng chứng tương ứng. Nếu thiếu kiểm tra UI thực tế, chỉ báo “đã sửa code và test tự động đạt, còn nghiệm thu giao diện”, không báo hoàn tất giao diện.

## 8. Prompt giao việc có thể dùng nguyên văn

> Đọc `docs/ADMIN_UI_FIX_PLAN_20261022.md` và các quy tắc repository. Thực hiện T01→T05 lần lượt cho A01–A04. Trang tài khoản cũ người dùng xác nhận là `/users`; đừng nhầm với `/admin/users`. Bảo toàn mọi thay đổi đang có trong working tree. URL thông báo phải do backend tính theo quyền người nhận; mọi trang admin dùng một sidebar và vùng menu cuộn riêng, logout luôn hiện. Không mở quyền customer cho admin, không đổi nghiệp vụ/dữ liệu, không sửa lịch sử migration, không push. Thêm kiểm thử hành vi và nghiệm thu UI theo mục 6; ghi kết quả trước/sau vào `docs/ADMIN_UI_FIX_RESULTS_20261022.md`. Khởi động lại web sau kiểm thử và báo rõ trạng thái từng lỗi cùng hạng mục chưa kiểm chứng. Chưa có bằng chứng thì không đánh dấu hoàn thành.
