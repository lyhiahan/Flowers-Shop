# Chat cửa hàng theo ngữ cảnh và phân quyền chi nhánh

## Vấn đề và phạm vi sửa

Widget cũ cho quản lý mở hội thoại như người mua với cửa hàng khác và gộp hội thoại cá nhân/Admin với các chi nhánh được phân công. Dropdown ở trang quản lý Quận 1 vì vậy liệt kê các chi nhánh khác. Mã đã có kiểm tra quyền đối với khách và quản lý không được phân công, nhưng luồng UI và inbox chưa phù hợp nghiệp vụ một chi nhánh đang làm việc.

Ở bước sửa phân quyền ban đầu, chưa mở rộng quyền admin giám sát. Sau khi người dùng xác nhận, đã bổ sung giám sát chỉ đọc có nhật ký và hỗ trợ nội bộ chi nhánh; xem [báo cáo bổ sung](CHAT_SUPPORT_MONITOR_20261013.md). Không xóa/chuyển hội thoại cũ, không sửa đơn #23, không tạo tin nhắn trên RentalShop thật để demo.

## Luồng mới

- Khách bấm Liên hệ cửa hàng từ chi tiết sản phẩm hoặc danh sách/chi tiết đơn. Backend xác minh ngữ cảnh, lấy chi nhánh bán sản phẩm hoặc chi nhánh đã ghi trên đơn, kiểm tra quyền sở hữu đơn. Sản phẩm toàn chuỗi phải xác định một chi nhánh có inventory hợp lệ. Không tự định tuyến tới Admin hoặc một cửa hàng không bán sản phẩm.
- Mở lại hội thoại khách–chi nhánh hiện có. Nội dung gợi ý nêu sản phẩm/đơn trong ô soạn; khách quyết định gửi. Không tạo tin tự động chỉ vì bấm liên hệ.
- Widget chung của khách mở các hội thoại của mình; kênh hỗ trợ riêng đã đổi tên thành **Chăm sóc khách hàng toàn chuỗi**. Bỏ dropdown chọn cửa hàng/Admin chung.
- Quản lý chỉ thấy khách của chi nhánh đang chọn, không chat như người mua và không xem hỗ trợ cá nhân cũ trong inbox công việc. Các API kiểm tra lại chi nhánh, kể cả sửa conversation ID/store ID hoặc gửi ảnh trực tiếp.
- Theo chính sách đã được xác nhận bổ sung, admin có ba hộp thư: CSKH, hỗ trợ nội bộ và giám sát khách–chi nhánh chỉ đọc có nhật ký. Không được trả lời thay cửa hàng.

## Cách ly WebSocket

`chat-store-id` ở CONNECT được kiểm tra với phân công/chủ chi nhánh rồi bind vào session WebSocket. Quản lý chỉ được subscribe queue riêng `/user/queue/chat.store.{id}` đúng scope của connection, không generic `/user/queue/chat` hay queue chi nhánh khác. SEND dùng scope đã bind, không tin store ID trên payload; vẫn kiểm tra quyền hiện hành.

Tin được lưu rồi phát sau commit đến khách qua queue cá nhân và quản lý qua queue chi nhánh. Một quản lý được phân công hai chi nhánh có thể mở hai trang/hai connection với hai scope hợp lệ; connection Quận 1 không nhận tin Quận 7. Việc đổi chi nhánh được thực hiện trong màn hình chọn cửa hàng và tải trang làm việc tương ứng. Browser kiểm tra scope để không trộn sự kiện vào inbox, nhưng không thay thế kiểm tra backend.

Kiểm thử hai kết nối đã phát hiện lỗi runtime `Expected mutable SimpMessageHeaderAccessor` của luồng ordered user-destination khi broadcast cùng account có nhiều session. Giữ cấu hình thứ tự tin; thay publisher chat để tra registry, chỉ gửi đến session đã subscribe đúng destination và dùng session ID/header mới cho mỗi lần gửi. Không gửi tin chat sang connection thông báo hoặc connection đang mở chi nhánh khác. Tin của người offline vẫn được lưu để đọc lịch sử. Sau sửa, kiểm thử WebSocket hai chi nhánh và chặn gửi ngoài scope đã đạt.

## Kiểm thử và demo

`ChatBranchFlowTests`: liên hệ sản phẩm/đơn đúng chi nhánh; giữ ngữ cảnh đơn cũ; sản phẩm riêng/chung/private; giả store ID/đơn người khác bị chặn; scope activeStoreId và nhiều chi nhánh; gửi ảnh trái quyền bị chặn trước upload; không tạo tin khi chỉ mở hội thoại.

`ChatSocketAccessTests`: CONNECT/SUBSCRIBE/SEND đúng scope, chặn generic/queue khác, chi nhánh trái quyền và quyền đã bị thu hồi. `ChatSessionIntegrationTests`: HTTP login + WebSocket thật, chat hai chiều khách–chi nhánh/Admin; hai connection của cùng quản lý nhưng hai chi nhánh nhận tách biệt, gửi vào hội thoại ngoài scope bị từ chối.

JavaScript kiểm tra header/queue manager, lọc sự kiện/inbox trái scope, nút liên hệ sản phẩm/đơn và nội dung gợi ý không tự gửi; giữ các kiểm thử mất kết nối/403/timeout.

Kết quả ở bước sửa phân quyền ban đầu: `maven package` thành công; **499 ca Java trong 57 suite, 496 đạt, 0 failures/errors, 3 bỏ qua** (Cloudinary live và 2 SQL Server opt-in). **41/41 JavaScript đạt**. Nhóm chat có 27 ca: 7 ngữ cảnh/phân quyền API, 14 bảo mật nghiệp vụ, 5 quyền STOMP và 1 HTTP/WebSocket integration thực tế. Hai connection của cùng quản lý được kiểm tra tách chi nhánh; một connection gửi vào hội thoại ngoài scope bị từ chối mà không tạo tin. Kiểm thử dùng H2 và localhost, không thao tác dữ liệu chat/đơn thật, không tự push GitHub. Kết quả bổ sung hỗ trợ/giám sát được ghi riêng trong báo cáo mới liên kết ở trên.

Sau khi khởi động lại bằng `scripts/sqlserver.ps1` và Ctrl+F5: mở sản phẩm bằng khách → Liên hệ cửa hàng → gửi; quản lý đúng chi nhánh trả lời. Mở quản lý chi nhánh khác để xác nhận không thấy tin này và không còn dropdown chọn nơi nhắn. Không dùng đơn #23 để thử thay đổi trạng thái hoặc kho.
