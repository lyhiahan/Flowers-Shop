# Ba kênh chat và giám sát chỉ đọc — 13/10/2026

## Chính sách đã triển khai

| Kênh | Khách | Quản lý đúng chi nhánh | Admin |
| --- | --- | --- | --- |
| Khách–cửa hàng (`STORE`) | Đọc/gửi hội thoại của mình | Đọc/gửi trong chi nhánh đang làm việc | Chỉ đọc, bắt buộc nhật ký |
| CSKH toàn chuỗi (`ADMIN`) | Đọc/gửi hội thoại của mình | Không vào CSKH của khách | Đọc/trả lời với tài khoản admin |
| Hỗ trợ vận hành (`BRANCH_SUPPORT`) | Không được truy cập | Đọc/gửi, dùng chung một hội thoại cho đội quản lý chi nhánh | Đọc/trả lời với tài khoản admin |

Không còn dropdown cho quản lý chọn cửa hàng khác như người mua. Khách liên hệ từ sản phẩm/đơn để backend chọn đúng chi nhánh; CSKH là nút riêng. Widget khách thông báo rằng admin có thể giám sát chỉ đọc chat cửa hàng để xử lý hỗ trợ. Hội thoại cũ không bị xóa, đổi người nhận hay chuyển chi nhánh; đơn #23 không được sửa.

Quản lý mở tab **Hỗ trợ từ admin**, bấm **Liên hệ admin về vận hành** để mở hội thoại nội bộ. Hội thoại gắn với chi nhánh, không riêng một quản lý; quản lý hiện hành khác trong cùng chi nhánh cũng được tiếp nhận. Người mất phân công, bị hạ vai trò/khóa tài khoản hoặc khách đều bị chặn. Backend khóa chi nhánh khi tạo để hai quản lý không tạo trùng hội thoại.

## Admin và nhật ký

Menu **Trung tâm hội thoại**, đường dẫn `/admin/chat`, có ba tab riêng:

- CSKH khách hàng: trả lời các yêu cầu hỗ trợ toàn chuỗi.
- Hỗ trợ chi nhánh: trả lời vận hành nội bộ, không trộn với tin khách hàng.
- Giám sát chỉ đọc: xem chat khách–chi nhánh, không được gửi văn bản/ảnh hay giả danh cửa hàng; không có API sửa/xóa tin.

Bộ lọc: ID chi nhánh, tên/tên đăng nhập người liên hệ, từ ngày/đến ngày theo **ngày hoạt động gần nhất của hội thoại**, gồm trọn hai ngày biên. Bộ lọc không phải tìm kiếm nội dung từng tin. Kênh CSKH không có chi nhánh nên để trống ô chi nhánh khi xem kênh này.

Ở chế độ giám sát, tin của **khách hàng bên phải, nền xanh nhạt**; tin của **chi nhánh bên trái, nền hồng nhạt**. Phân biệt dựa trên khách sở hữu hội thoại, không dựa vào tài khoản admin đang xem; quản lý khác cùng chi nhánh vẫn ở bên trái. Giữ tên người gửi/thời gian. Các kênh chat thông thường vẫn căn tin của người đang đăng nhập bên phải; thay đổi hiển thị không mở quyền gửi cho admin trong giám sát.

## Bố cục chat gọn và đủ chiều cao

Trang `/admin/chat` dùng chiều cao viewport và flex để khung chat chiếm phần còn lại dưới tiêu đề. Không còn giới hạn `100dvh - 190px`/820px của widget popup; chỉ danh sách hội thoại, lịch sử tin và menu quản trị cuộn riêng. CSS toàn màn hình được giới hạn bởi `admin-chat-page`, không đổi bố cục các trang quản trị khác.

Trạng thái kết nối cùng hàng tiêu đề. Đã bỏ phần giải thích dài bên trái ở chế độ admin; giữ cảnh báo giám sát chỉ đọc ngắn và tooltip đầy đủ. Bộ lọc mặc định đóng, có nút mở/đóng, dấu báo Đang lọc và Xóa lọc. CSKH không hiện/áp dụng trường chi nhánh; hai hộp thư có chi nhánh vẫn giữ trường này. Đóng bộ lọc không xóa điều kiện đang dùng.

Danh sách CSKH hiển thị tên khách trước, không lặp nhãn Trò chuyện với hệ thống. Giám sát hiển thị tên khách và chi nhánh ở hai dòng riêng. Ô soạn ban đầu một dòng, tự tăng từ 42px tới tối đa 112px; nút ảnh/Gửi cùng hàng, tên file chỉ xuất hiện khi chọn ảnh. Giữ Enter để gửi, Shift+Enter để xuống dòng, thẻ sản phẩm và kiểm tra file.

Màn hình nhỏ có nút mở/đóng menu quản trị. Escape đóng menu, không làm ẩn toàn bộ workbench; popup chat thông thường vẫn đóng như cũ. Giữ phân quyền, định tuyến chi nhánh và ghi nhật ký; không thêm migration hoặc sửa dữ liệu cho thay đổi giao diện này.

Kiểm chứng lượt sửa bố cục: **59/59 JavaScript đạt**, gồm 7 ca mới cho bộ lọc, tiêu đề, ô soạn, menu và các ràng buộc CSS. **36/36 ca Java của 4 suite chat đạt**, gồm render Thymeleaf, phân quyền và HTTP/WebSocket thật. Đây là kiểm thử tự động, không phải chứng nhận đã kiểm tra hình ảnh trên mọi kích thước trình duyệt. Không tự mở trình duyệt hoặc khởi động website dữ liệu thật, không push GitHub.

Danh sách giám sát chỉ hiện metadata, không lộ nội dung tin cuối. Mỗi lần mở, làm mới hoặc tải lịch sử cũ phải lưu admin thực hiện, hội thoại, thời điểm và mốc tin cũ trong `chat_access_audits` trước khi trả nội dung. Không lưu được nhật ký thì trả lỗi, không trả tin. HTTP lịch sử có `Cache-Control: no-store`.

Nhật ký ở `/admin/chat/access-logs`, chỉ admin đọc được, có phân trang; không có nút/endpoint sửa/xóa. Đây là nhật ký ứng dụng, không phải cơ chế chống sửa dữ liệu bằng quyền quản trị SQL Server.

Giám sát không nhận broadcast tin cửa hàng và không được subscribe queue quản lý. Admin dùng **Làm mới** để xem lịch sử mới, đồng thời ghi nhận lượt đọc. Hai kênh hỗ trợ và chat khách–cửa hàng vẫn gửi/nhận trực tiếp qua WebSocket không cần tải lại trang.

## Cách ly WebSocket

CONNECT của quản lý bind `chat-store-id` với quyền hiện hành. Chỉ được subscribe `/user/queue/chat.store.{id}` và `/user/queue/chat.support.{id}` đúng scope; không được queue chi nhánh khác hay queue generic chat. SEND lấy scope từ session WebSocket, không tin store ID do client gửi. Một tài khoản phụ trách nhiều chi nhánh vẫn có inbox riêng theo từng connection.

Publisher chỉ phát sau commit đến session đã subscribe đúng queue. Tin nội bộ chỉ đến quản lý hiện hành của chi nhánh và admin, không đến khách hoặc quản lý chi nhánh khác. Admin gửi vào `STORE` bị từ chối ở backend, kể cả tự gọi STOMP hoặc upload ảnh.

## Migration và kiểm chứng SQL Server

`V5__chat_monitor_access_audit.sql` bổ sung bảng nhật ký, hai khóa ngoại và hai index; không thay migration V1–V4, không sửa/xóa lịch sử chat hoặc đơn. Khởi động ứng dụng bằng luồng SQL Server hiện có để Flyway kiểm tra/chạy phiên bản còn thiếu; không chạy lại SQL lịch sử thủ công. Tại lần kiểm tra chỉ đọc 13/10/2026, RentalShop/SQLEXPRESS đã ở Flyway V5, bảng nhật ký có 0 dòng.

Đã chạy hai ca `SqlServerMigrationLiveTests` thực tế trên SQL Server 16.0/SQLEXPRESS, cổng 53735:

- `CodexRentalShopFresh_20261013_1700`: database trống, V1–V5 thành công, Hibernate validate toàn bộ entity, migrate lại không thực thi thêm.
- `CodexRentalShopCopy_20261013_1700`: restore backup trước Flyway, chứng minh cần baseline tường minh; baseline V1 rồi V2–V4; nâng riêng V4→V5; số dòng users/stores/products/orders/order_details/store_inventories/transactions và chat không đổi; Hibernate validate thành công, migrate lại không thực thi thêm.

Kết quả live: **2 đạt, 0 lỗi, không bỏ qua**. Không chạy kiểm thử gửi tin, tạo đơn hay thay đổi trạng thái trên RentalShop gốc. Hai database thử nghiệm được giữ riêng để kiểm tra tiếp, không phải database chạy website.

## Kiểm thử ứng dụng ở bước bổ sung hỗ trợ/giám sát

Sau bước này, điểm vào chat khách đã được điều chỉnh theo yêu cầu mới: CSKH chuyển vào **Tài khoản → Trò chuyện với hệ thống**, nút nổi chỉ mở lịch sử chung và hỏi sản phẩm có thẻ sản phẩm lưu kèm. Xem [luồng khách hàng và kiểm thử mới](CHAT_CUSTOMER_ENTRY_20261013.md). Số liệu dưới đây là kết quả của bước bổ sung hỗ trợ/giám sát trước đó.

- `maven package`: **510 ca Java trong 58 suite**, 507 đạt, 0 failures/errors, 3 bỏ qua (Cloudinary live và 2 SQL Server opt-in). Không cộng hai báo cáo stale từ các kiểm thử cũ không còn trong source.
- SQL Server opt-in được chạy riêng như trên: **2/2 đạt thực tế**, không dùng H2 để thay thế chứng cứ migration SQL Server.
- JavaScript: **45/45 đạt**, gồm các kênh mới, chế độ chỉ đọc, không trộn kết quả trả chậm khi chuyển hộp thư và tin đến trực tiếp không vượt bộ lọc chi nhánh của admin.
- Nhóm chat Java: **38 ca**, gồm 7 ngữ cảnh chi nhánh, 14 bảo mật, 5 quyền STOMP, 11 hỗ trợ/giám sát và 1 HTTP/WebSocket thực tế. Có kiểm tra fail-closed khi repository nhật ký lỗi: trả 503, không trả nội dung chat.
- HTTP/WebSocket thực tế dùng các phiên đăng nhập riêng trên localhost: khách–chi nhánh/CSKH và chi nhánh–admin hai chiều; hai connection cùng quản lý ở hai chi nhánh nhận tách biệt; khách không nhận nội bộ; admin gửi vào `STORE` bị từ chối mà không tạo tin.
- Đã đóng gói lại artifact sau chỉnh sửa JavaScript cuối. Không khởi động lại website dữ liệu thật thay người dùng, không gửi tin giả vào RentalShop, không push GitHub.

## Demo bằng các phiên riêng

1. Khởi động lại website nếu còn chạy bản cũ; Ctrl+F5 để nhận JavaScript mới. Không bật seed demo trên database thật.
2. Khách mở sản phẩm → **Liên hệ cửa hàng** → gửi. Quản lý đúng chi nhánh nhận và trả lời ngay, không tải lại trang. Quản lý chi nhánh khác không thấy hội thoại.
3. Quản lý → **Hỗ trợ từ admin** → **Liên hệ admin về vận hành** → gửi. Admin vào **Hỗ trợ chi nhánh**, trả lời; quản lý nhận ngay. Khách không thấy tin nội bộ.
4. Khách → **Tài khoản → Trò chuyện với hệ thống** → gửi. Admin vào **CSKH khách hàng** và trả lời, không nhầm với kênh nội bộ.
5. Admin → **Giám sát chỉ đọc** → lọc/open hội thoại khách–chi nhánh. Không có ô gửi; vào **Nhật ký truy cập** thấy lượt đọc. Bấm Làm mới hoặc tải tin cũ tạo thêm lượt đọc.
6. Không dùng đơn #23 để demo thay đổi kho/trạng thái. Không cần push GitHub để sử dụng bản sửa cục bộ.
