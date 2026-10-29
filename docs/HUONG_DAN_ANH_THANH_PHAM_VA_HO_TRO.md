# Ảnh thành phẩm, bàn giao và hỗ trợ sau giao

Các thao tác nằm trong trang **Chi tiết đơn hàng** của khách hàng, nhân viên, quản lý và quản trị viên; thông báo dẫn về đơn hàng theo quyền của người nhận.

## Chuẩn bị dữ liệu

Khởi động lại ứng dụng từ mã mới với Flyway được bật như cấu hình hiện tại. Flyway bổ sung bảng `order_aftercare` bằng migration **V19 trên SQL Server**, hoặc **V3 trên PostgreSQL**; không chạy hai bộ migration trên cùng một database, không sửa checksum hay xóa lịch sử Flyway. Bảng mới liên kết với đơn hàng hiện có và không cập nhật tồn kho, số dư hoặc trạng thái thanh toán.

Ảnh dùng dịch vụ tải ảnh hiện có, giới hạn 10 MB mỗi tệp, hỗ trợ JPEG, PNG, WEBP và GIF. Khi Cloudinary chưa được cấu hình, ứng dụng có thể lưu cục bộ theo cơ chế môi trường phát triển; không coi việc dùng chế độ này là đã kiểm chứng kết nối Cloudinary thật.

## Demo duyệt ảnh

1. Khách tạo đơn; nhân viên hoặc quản lý đúng chi nhánh xác nhận rồi chuyển sang **Đang chuẩn bị**.
2. Tại chi tiết đơn, cửa hàng chọn ảnh hoa thực tế, nhập ghi chú và bấm **Gửi ảnh cho khách duyệt**.
3. Khách mở chi tiết đơn qua thông báo, xem ảnh và chọn đồng ý hoặc yêu cầu chỉnh lại; yêu cầu chỉnh phải có ít nhất 5 ký tự giải thích.
4. Nếu khách yêu cầu chỉnh, cửa hàng gửi ảnh mới. Ảnh mới tăng số phiên bản, xóa xác nhận cũ và yêu cầu khách duyệt lại; phản hồi từ trang đang mở ảnh cũ sẽ bị từ chối.
5. Chỉ khi ảnh đã được đồng ý, đơn có luồng duyệt ảnh mới được chuyển sang giao hàng hoặc hoàn tất nhận tại quầy. Các đơn chưa từng gửi ảnh vẫn giữ luồng xử lý cũ.

## Demo bàn giao và xác nhận nhận hoa

Khi đơn đang giao, đã giao hoặc hoàn tất, cửa hàng có thể gửi một ảnh bàn giao kèm ghi chú từ 5 đến 500 ký tự. Minh chứng đã lưu không được ghi đè. Khi đơn đã giao hoặc hoàn tất, chủ đơn có nút **Tôi đã nhận được hoa**; bấm lại không làm thay đổi thời điểm xác nhận ban đầu. Hai thao tác này bổ sung minh chứng, không thay thế thao tác cập nhật trạng thái đơn và thu tiền COD trong quy trình sẵn có.

## Demo hỗ trợ sau giao

Với đơn đã giao hoặc hoàn tất, khách chọn lý do, nhập mô tả từ 10 đến 1.000 ký tự và đính kèm ảnh nếu có. Quản lý đúng chi nhánh hoặc admin có thể chuyển yêu cầu từ **Đã gửi yêu cầu** sang **Đang xử lý**, rồi **Đã giải quyết** hoặc **Không chấp nhận yêu cầu**, đồng thời nhập phản hồi cụ thể. Nhân viên có thể xem nội dung trong đơn thuộc chi nhánh nhưng không được quyết định kết quả xử lý. Khách nhận thông báo và xem phản hồi ngay tại đơn.

Mỗi đơn hiện có một yêu cầu hỗ trợ; yêu cầu đã đóng không được mở lại hoặc ghi đè. Việc đóng yêu cầu không tự hoàn tiền, tạo đơn đổi hoa hay chỉnh sổ ví. Các nghiệp vụ tài chính vẫn đi qua luồng thanh toán và hoàn tiền hiện có. Hệ thống giữ ảnh thành phẩm và phản hồi hiện tại, chưa xây dựng nhật ký đầy đủ mọi lần thay ảnh hoặc mọi lần phản hồi hỗ trợ.

## Phí giao hàng và thông tin dịch vụ

Khách chọn rõ khu vực gần hoặc xa theo biểu phí khi giao tận nơi; giao diện không suy khoảng cách từ từ khóa địa chỉ và không tự chọn vùng gần. Phí hiển thị lấy từ máy chủ theo cấu hình chi nhánh và đơn vị giao; khi chưa đủ dữ liệu hoặc đang tính lại sẽ hiện **Chưa xác định**. Nhận tại cửa hàng có phí giao bằng 0. Trang `/service-policy`, được liên kết ở chân trang, giải thích cơ chế phí, lịch nhận, duyệt ảnh, hỗ trợ và thanh toán demo.

## Kiểm chứng

Các kiểm thử `OrderAftercareTests` xác minh lưu/đọc dữ liệu, duyệt đúng phiên bản ảnh, chặn giao khi chờ duyệt, quyền chủ đơn và chi nhánh, bảo toàn minh chứng, xác nhận nhận hàng lặp lại, điều kiện mở/đóng hỗ trợ, CSRF và render giao diện cả bốn vai trò. Tải ảnh được giả lập trong nhóm kiểm thử này để không gọi Cloudinary thật. `OrderAftercareMigrationTests` kiểm tra DDL và ràng buộc trên hai chế độ tương thích H2; kiểm tra này không thay thế thử nghiệm PostgreSQL thật. Kiểm thử JavaScript xác minh phí phải chờ máy chủ và địa chỉ không tự đổi khu vực đã chọn.
