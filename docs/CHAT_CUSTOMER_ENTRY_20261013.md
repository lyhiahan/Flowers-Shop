# Điểm vào chat khách và thẻ sản phẩm — 13/10/2026

## Luồng đã chốt với người dùng

1. **Tài khoản → Trò chuyện với hệ thống** mở hoặc dùng lại hội thoại CSKH với admin. Không chọn chi nhánh ở đây, không gửi tự động. Menu có ở trang chủ, hồ sơ, sản phẩm, giỏ hàng, danh sách/chi tiết cửa hàng và header chung của các trang khách. Không hiển thị lối vào này cho quản lý/admin.
2. Nút nổi **Nhắn tin** mở danh sách toàn bộ hội thoại khách sở hữu (cửa hàng và hệ thống), có tải thêm khi trên 30 hội thoại. Không tự tạo hội thoại hoặc chọn admin; chọn hội thoại cũ để tiếp tục. Đã bỏ nút mở CSKH khỏi hộp lịch sử chung.
3. Trong **chi tiết sản phẩm → Nhắn tin với cửa hàng**, backend chọn đúng chi nhánh bán và trả thẻ sản phẩm gồm ảnh, tên, giá bán, liên kết chi tiết. Nút nổi **Nhắn tin** trên chính trang chi tiết sản phẩm cũng đi theo luồng này khi đã xác định được chi nhánh, không mở nhầm CSKH. Thẻ hiện ngay phía trên ô soạn và chỉ tồn tại dạng đính kèm trước khi gửi. Có thể bỏ đính kèm. Mở chat không tạo tin nhắn.

Khi khách bấm Gửi, browser chỉ gửi `productId`, không gửi tên/giá/ảnh tùy ý để server tin theo. Backend kiểm tra người gửi là khách sở hữu hội thoại, hội thoại là `STORE`, sản phẩm đang công khai và thuộc chi nhánh đó hoặc là sản phẩm chung có inventory ở chi nhánh. Sai chi nhánh, sản phẩm riêng tư hoặc quản lý giả sản phẩm khách hỏi đều bị chặn trước upload ảnh.

Thẻ được chụp lại từ dữ liệu sản phẩm tại thời điểm gửi, lưu cùng câu hỏi và trả trong `message.product` cho cả lịch sử HTTP và sự kiện/ack WebSocket. Cửa hàng thấy sản phẩm khách hỏi ngay, không phải đọc tên trong một câu gợi ý. Sửa tên/giá sản phẩm về sau không làm thay đổi thẻ đã gửi. Liên kết vẫn trỏ tới sản phẩm; nếu sản phẩm không còn công khai, màn hình chi tiết có thể không còn truy cập được nhưng thẻ lịch sử vẫn giữ thông tin đã chụp.

Chuyển sang hội thoại khác, hệ thống hoặc trở về danh sách chung xóa đính kèm chưa gửi để tránh gửi nhầm sản phẩm. Gửi lỗi/mất kết nối giữ đính kèm để thử lại. Mã idempotency bao gồm kiểm tra ID sản phẩm: thử lại cùng câu hỏi không tạo trùng; dùng lại mã cho sản phẩm khác bị từ chối. Có thể gửi riêng thẻ hoặc kèm nội dung/ảnh, nhưng vẫn cần bấm Gửi.

## Phân quyền không đổi

Quản lý chỉ nhận chat đúng chi nhánh theo scope API/session WebSocket. CSKH khách và hỗ trợ nội bộ chi nhánh vẫn tách biệt. Admin chỉ đọc chat cửa hàng có nhật ký, không trả lời thay cửa hàng; hai hộp thư hỗ trợ vẫn được trả lời. Tên sản phẩm được render bằng `textContent`, liên kết lấy từ ID hợp lệ, không chèn HTML người dùng; ảnh chỉ nhận HTTPS hoặc đường dẫn ảnh/upload cục bộ.

## Database

Migration **V6 — chat product context** chỉ thêm bảng `chat_product_contexts`, khóa duy nhất theo tin nhắn và khóa ngoại đến `chat_messages`. ID sản phẩm được giữ như giá trị snapshot, không bắt buộc sản phẩm phải tồn tại mãi trong database. Không sửa migration V1–V5, tin cũ, tồn kho hoặc đơn #23.

Đã chạy SQL Server live riêng với **2/2 ca đạt**, không có lỗi/bỏ qua:

- `CodexRentalShopFresh_20261013_1730`: V1–V6 trên database trống, Hibernate validate mọi entity, migrate lại không thực thi thêm.
- `CodexRentalShopCopy_20261013_1730`: restore backup trước Flyway, baseline V1 tường minh, V2–V4, nâng V5 rồi V6 riêng; bảo toàn số dòng nghiệp vụ/chat; Hibernate validate thành công.

Không chạy thử gửi chat hoặc thay đổi đơn trên RentalShop thật. Database thử nghiệm được giữ riêng, không dùng để chạy website. Khi khởi động website, Flyway kiểm tra/chạy migration còn thiếu trước Hibernate; không cần chạy SQL thủ công hoặc baseline lại database đã có lịch sử Flyway. Nên giữ backup trước khi nâng cấp môi trường thật.

## Cách thử

1. Khởi động lại bằng `scripts/sqlserver.ps1` nếu website còn dùng bản cũ, Ctrl+F5.
2. Khách → Tài khoản → Trò chuyện với hệ thống. Kiểm tra mở đúng CSKH; admin trả lời từ tab CSKH.
3. Khách → Nhắn tin nổi. Kiểm tra thấy cả hội thoại chi nhánh và hệ thống; không tự tạo/gửi tin.
4. Khách → chi tiết sản phẩm → Nhắn tin với cửa hàng. Kiểm tra đúng tên chi nhánh và thẻ sản phẩm hiện ngay.
5. Gửi câu hỏi. Quản lý đúng chi nhánh nhận cả câu hỏi và thẻ không tải lại trang; đóng/mở chat vẫn thấy thẻ. Quản lý chi nhánh khác không nhận.
6. Mở hỏi sản phẩm khác cùng cửa hàng để kiểm tra thẻ mới; chuyển sang hệ thống để kiểm tra không mang theo đính kèm cũ.

## Kiểm thử cuối

- `maven package` thành công: **513 ca Java trong 58 suite**, 510 đạt, 0 failures/errors, 3 bỏ qua (Cloudinary live và 2 SQL Server opt-in). Chỉ tính báo cáo của lượt chạy mới, không cộng hai XML stale của test cũ đã bỏ khỏi source.
- **50/50 JavaScript đạt**: menu hệ thống mở trực tiếp không tự gửi; trang chủ hiện lịch sử chung; chi tiết sản phẩm/thao tác nút nổi hiện thẻ đúng chi nhánh; gửi kèm ID sản phẩm, ack hiển thị thẻ; đổi hội thoại xóa đính kèm; giữ các ca phân quyền/kết nối/bộ lọc trước đó.
- Nhóm chat Java **41 ca**: 10 ngữ cảnh/menu/thẻ sản phẩm, 14 bảo mật, 5 STOMP, 11 hỗ trợ/giám sát và 1 HTTP/WebSocket thực tế. Kiểm tra snapshot lịch sử sau khi xóa persistence context và sửa sản phẩm; replay không trùng; giả chi nhánh/ảnh bị chặn trước upload; cửa hàng nhận thẻ sản phẩm thực tế qua WebSocket.
- SQL Server live **2/2 đạt** riêng như trên; không dùng kết quả H2 để thay thế chứng cứ migration SQL Server.
- Đóng gói lại artifact sau chỉnh sửa JavaScript/HTML cuối. Không tự khởi động website trên dữ liệu thật, không tạo chat thử ở RentalShop hoặc sửa đơn #23, không push GitHub.
