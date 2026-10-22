# Kiểm tra trước khi đưa thay đổi lên main — 22/10/2026

Người dùng yêu cầu kiểm tra, sửa lỗi nếu có, tạo commit tiếng Việt và push nhánh `main`. Phạm vi gồm các thay đổi đang chờ của đợt khắc phục hệ thống và giao diện admin; giữ lại file chạy riêng `start-web.ps1` và artifact `0` ngoài commit. Cookie HTTP cục bộ được thêm vào `.gitignore`.

## Kết quả rà soát mã nguồn

- Hồ sơ công khai đã bỏ địa chỉ cá nhân. Thêm kiểm thử GET anonymous với địa chỉ/email/điện thoại/mật khẩu giả để xác nhận HTML không xuất các giá trị này.
- Menu chat đã có override visibility/transform và breakpoint 992px tương thích sidebar chung.
- Resolver đã dùng quyền hiệu lực của User. Sửa tiếp controller trang thông báo và API badge để dùng cùng resolver, ưu tiên ADMIN kể cả khi vai trò chính là MANAGER.
- Thêm kiểm thử payload WebSocket qua NotificationServiceImpl với tài khoản có ADMIN bổ sung; đích phải là `/admin/orders/{id}`.
- Đổi tiêu đề trong assets admin từ `th:utext` sang `th:text`; tên thuộc tính sản phẩm có HTML phải được escape, không thoát thẻ title.
- URL thông báo phía client khớp toàn bộ route cho phép; từ chối đường dẫn traversal, prefix gần giống và ID không hợp lệ.
- Thêm kiểm thử render sidebar trên 16 trang chính (bao gồm chat và nhật ký chat): mỗi trang chỉ có một sidebar/logout, một mục active và cùng danh mục menu.
- Sửa lỗi thiếu tài nguyên: footer của phần lớn trang admin chưa nạp notifications-realtime.js, trang thông báo thiếu admin-layout.js và chat chưa dùng scripts chung. Tất cả chuyển về fragment scripts duy nhất; test xác nhận mỗi script được tải đúng một lần.
- Kiểm tra tablet phát hiện chat có hai nút đóng menu, một nút không được gắn sự kiện. Chỉ ẩn nút chung khi model `chatWorkbench=true`; trang nhật ký chat vẫn giữ nút chung. Thêm kiểm thử duy nhất một nút đóng menu trên cả 16 route.

## Kiểm thử và khởi chạy

Kết quả cuối được cập nhật sau khi các lượt chạy hoàn thành. Log cục bộ đặt dưới `target/`, không đưa lên Git.

- Java toàn bộ: **686 ca, 674 đạt, 12 bỏ qua, 0 thất bại/lỗi**, BUILD SUCCESS lúc 15:38:31 UTC+7. Log `target/main-review-java-final-20261022.log`.
- Sau khi bổ sung script chung: **32/32 ca mục tiêu đạt**, kiểm tra lại 16 route render và các luồng notification. Log `target/main-review-ui-final-20261022.log`; đây là lượt chạy bổ sung, không cộng lặp vào tổng Java.
- Sau sửa nút đóng menu chat: **5/5 ca AdminUiRegressionTests đạt**, BUILD SUCCESS lúc 17:33:13 UTC+7; log `target/main-review-ui-last-20261022.log`.
- JavaScript: **126/126 đạt**, gồm kiểm thử drawer và từ chối route traversal, `target/main-review-js-20261022.log`.
- Lượt Java đầu phát hiện lỗi fixture mới thiếu fullName và expectation cũ chưa tính HTML escape. Đã sửa fixture/expectation; giữ nguyên điều kiện bắt buộc dữ liệu và cơ chế escape trong code.
- Kiểm tra cả `git diff --check` và `git diff --cached --check`; kiểm tra staged mới phát hiện thêm whitespace trong file admin/notifications trước đó chưa được theo dõi, đã làm sạch.

Báo cáo triển khai trước đó tại `ADMIN_UI_FIX_RESULTS_20261022.md` là lịch sử từng đợt. Các khẳng định nghiệm thu giao diện trong báo cáo đó chỉ được coi là đã kiểm chứng độc lập khi có bằng chứng tương ứng được ghi bổ sung tại đây. Không đồng nhất test JavaScript giả lập DOM với kiểm tra CSS responsive thực tế.

## Kiểm tra trình duyệt và giới hạn nghiệm thu

- `/users` và `/users/1` ở 390×844: không tràn ngang, hồ sơ không còn nhãn/giá trị địa chỉ cá nhân.
- Dashboard mobile 390×844 khi mở menu: sidebar rộng 255px; logout có top 794px, bottom 828px, nằm trong viewport 844px.
- Dashboard desktop 1366×768: trang dài 2577px; logout top 718px, bottom 752px, nằm trong viewport 768px.
- Chuyển qua users admin, orders, commissions, form tạo sản phẩm, notifications và chat ở desktop: cùng 16 mục menu, width 255px, active đúng chức năng cha, logout nhìn thấy và không tràn ngang.
- Chat ở tablet 768×1024: menu mở được, sidebar visible, logout bottom 1008px trong viewport 1024px. Đây cũng là lượt kiểm tra phát hiện nút đóng menu dư để sửa tiếp.
- Kiểm tra lại sau sửa cuối: chat chỉ còn nút `chatNavigationClose` và đóng/mở được; menu tại `/admin/notifications` hoạt động; bấm thông báo mở đúng `/admin/orders/4`, không có `admin_shopping_denied`.
- Dashboard desktop sau khi cuộn xuống khoảng 1809px: logout top khoảng 718px, bottom 752px trong viewport 768px. Screenshot cục bộ `target/admin-desktop-verified-20261022.png`.
- Screenshot mobile cục bộ: `target/admin-mobile-verified-20261022.png`. Screenshot/log dưới target không đưa lên Git và có thể bị clean xóa.

Web được khởi động lại lúc 17:33:42 UTC+7, Java PID 11156, database RentalShop với Flyway bật/schema V15. Log cuối: `target/web-main-verified-final-20261022.log`. DevTools restart bị tắt riêng cho tiến trình nghiệm thu để tránh nạp lại lớp trong lúc kiểm tra.

Chưa kiểm tra zoom 125%/200%, mọi viewport trong kế hoạch và mọi form chỉnh sửa bằng trình duyệt. Không dùng kết quả đại diện trên để tuyên bố nghiệm thu toàn bộ tổ hợp UI. Email/Cloudinary/cổng thanh toán thật không được thực hiện trong lượt này.

## Quy trình Git theo yêu cầu bổ sung

Trước push phải fetch lại `origin`, kiểm tra staged diff/whitespace và so sánh với `origin/main`. Nếu main remote có commit mới, hợp nhất trên nhánh `quocanh`, xử lý xung đột và chạy kiểm thử phù hợp trước khi cập nhật main. Không force-push. Cookie và script chạy cục bộ không được đưa lên remote.
