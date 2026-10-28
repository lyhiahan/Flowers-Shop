# Loại bỏ nghiệp vụ thuê khỏi ứng dụng bán hoa

Ngày 29/10/2026, người dùng chọn xóa hẳn phần thuê trong code và cập nhật báo cáo, thay cho chỉ vô hiệu hóa bằng cấu hình.

## Mã nguồn

- OrderType chỉ còn BUY. Loại bỏ các trạng thái thuê khỏi OrderStatus, bộ lọc đơn, điều kiện đánh giá và kiểm tra đơn đang xử lý.
- Product không còn giá thuê hoặc cọc. CartItem không còn ngày thuê. OrderDetail chỉ tính đơn giá bán nhân số lượng; không có ngày thuê, ngày trả hoặc cọc. Order không còn chi nhánh trả, tổng cọc hoặc tiền phạt.
- Loại bỏ phần cọc/phạt trên lịch sử đơn, đổi nhãn Hoàn cọc thành Hoàn tiền. Bỏ các giá trị giả bằng 0 ở builder/service/seeder và sửa fixture kiểm thử tương ứng.
- Mã kết nối database, định danh ảnh và định dạng snapshot kỹ thuật không phải là chức năng thuê; không đổi các giá trị này gây mất khả năng kết nối/đọc bản sao.

## Nâng cấp cấu trúc dữ liệu

SQL Server thêm V18__sales_only_schema.sql; PostgreSQL thêm V2__sales_only_schema.sql. Không sửa V1–V17 đã triển khai hoặc baseline PostgreSQL V1.

Migration giữ một bản sao các giá trị cột sắp gỡ trong schema_change_archive, rồi bỏ cột và thu hẹp ràng buộc loại/trạng thái. Các đơn, sản phẩm, tồn kho và số tiền hiện có không bị tự chuyển đổi. Nếu phát hiện giao dịch hoặc khuyến mãi chỉ dành cho loại không còn hỗ trợ, migration dừng và yêu cầu đối chiếu trước.

Chưa áp dụng các migration mới trực tiếp lên database đang dùng hoặc Aiven. Khi triển khai, sao lưu database trước rồi chạy Flyway theo cấu hình hiện có. Không chạy lại seed hoặc import database để nâng cấp.

Công cụ DatabaseTransfer cũ vẫn chỉ nhận snapshot SQL Server V17 và đích PostgreSQL V1 đã xác minh. Không dùng công cụ đó để chuyển schema V18/V2; cần một đợt kiểm chứng chuyển dữ liệu riêng nếu nhóm có nhu cầu.

## Báo cáo

Đã tạo bốn bản Word hậu tố _CHI_BAN_HOA, giữ định dạng gốc và bỏ các mô tả còn dùng lớp/trường thuê. Báo cáo gốc đang mở không bị ghi đè. README và báo cáo Markdown đã cập nhật phiên bản và phạm vi hiện tại.

## Kiểm chứng

- SQL Server SQLEXPRESS01: tạo hai database test ngẫu nhiên, chạy schema cũ rồi V18; kiểm tra archive, gỡ cột, giữ giá bán/số bán và ràng buộc BUY. Trường hợp khuyến mãi lịch sử không phù hợp bị chặn và giữ schema/data cũ. Đã xóa đúng hai database test sau kiểm tra.
- PostgreSQL nhúng: kiểm tra V1→V2, rollback khi gặp dữ liệu cần đối chiếu và luồng đơn/ví trên model mới.
- Các bản Word được xuất PDF bằng Microsoft Word ẩn do máy không có LibreOffice; kiểm tra toàn bộ trang qua tổng quan và các trang có nội dung thay đổi, không phát hiện chữ vượt trang hoặc ký tự thay thế.

Kết quả: lần chạy full suite mặc định có 873 ca, không có assertion failure; 5 lỗi môi trường do sandbox chặn ACL/loopback đã được chạy lại ở đúng 3 lớp và cả 10 ca đạt. PostgreSQL: 39 ca ví, các ca chuyển dữ liệu V17/V1 và 3 ca migration V2 đạt sau kiểm tra. SQL Server: cả hai ca nâng cấp/guard đạt, bao gồm bảo toàn constraint thanh toán không liên quan. JavaScript: 186/186 đạt. Không có migration nào được áp dụng vào database nghiệp vụ hoặc Aiven trong tác vụ này.

Có thể chạy lại kiểm chứng SQL Server trong thư mục gốc dự án bằng `./scripts/verify-sales-only-schema-sqlserver.ps1 -ServerInstance '.\SQLEXPRESS01'`. Script dùng Windows Authentication, tạo database test tên GUID và dọn đúng các database đó; không nhận tên database nghiệp vụ để tránh chọn nhầm.
