# Kết quả hoàn thiện luồng website ngày 29/10/2026

## Phạm vi đã thực hiện

Theo lựa chọn hoàn thiện các luồng hiện có, phiên này bổ sung ảnh thành phẩm gắn với đơn hàng và duyệt theo phiên bản, minh chứng bàn giao, xác nhận nhận hoa của chủ đơn, cùng yêu cầu hỗ trợ sau giao được quản lý chi nhánh hoặc admin phản hồi. Chưa mở rộng sang đặt hàng không cần tài khoản, quản lý nguyên liệu cắm hoa hoặc thanh toán thật.

Các thao tác ghi dữ liệu dùng khóa đơn hàng theo cơ chế hiện có, kiểm tra chủ đơn hoặc quyền chi nhánh, bảo vệ CSRF và giới hạn độ dài dữ liệu. Ảnh thay mới phải được duyệt lại; đơn đang chờ duyệt hoặc yêu cầu chỉnh ảnh chưa được bàn giao. Minh chứng bàn giao đã lưu và yêu cầu hỗ trợ đã đóng không được ghi đè. Xử lý hỗ trợ không tự sửa trạng thái thanh toán hoặc số dư ví.

Giao diện bỏ cam kết giao 60–90 phút và hoàn tiền 100% chưa được hệ thống bảo đảm, bổ sung trang giao nhận và hỗ trợ, đồng thời cho khách chọn khu vực biểu phí rõ ràng. Phí giao hàng lấy từ máy chủ; bỏ giá hiển thị cố định và đoạn tính phí inline trùng với JavaScript dùng chung. Địa chỉ không còn tự quyết định vùng gần/xa.

## Kiểm thử

| Hạng mục | Kết quả |
|---|---|
| Tổng hợp báo cáo Java sau lượt chạy toàn bộ và chạy lại nhóm bị ảnh hưởng | **893 ca: 881 đạt, 12 bỏ qua, 0 thất bại, 0 lỗi; 104 nhóm** |
| Nhóm chạy lại cuối cùng | **35/35 đạt, BUILD SUCCESS**: StorefrontUiTests, OrderAftercareTests, OrderAftercareMigrationTests, SqlServerSchemaTests và MigrationHistoryTests |
| JavaScript | **188/188 đạt** |
| Migration SQL Server V19 | Chạy thử trên SQL Server local trong transaction; tạo đủ **26 cột**, sau đó rollback thành công, không để lại bảng mới |
| Ràng buộc migration | Đã kiểm tra hai chế độ tương thích H2: khóa ngoại, trạng thái hợp lệ, số phiên bản ảnh không âm và giữ nguyên số tiền đơn |
| Giao diện và phân quyền mới | Render trang chi tiết của USER/STAFF/MANAGER/ADMIN; kiểm tra CSRF, chặn người khác và khác chi nhánh, thoát HTML trong nội dung hỗ trợ |

Lượt toàn bộ ban đầu phát hiện kiểm tra giao diện vẫn đòi các cam kết dịch vụ cũ, cùng lỗi lớp Java và template không đồng bộ trong lúc cập nhật nội dung. Sau khi cập nhật kỳ vọng theo dịch vụ thực tế và đồng bộ bản biên dịch, nhóm 35 ca liên quan được chạy lại thành công; tổng hợp trên là kết quả mới nhất của từng nhóm, không phải tuyên bố một lượt chạy toàn bộ cuối cùng duy nhất đã có BUILD SUCCESS. Các kiểm tra nghiệp vụ đặt hàng, thanh toán, ví, tồn kho và phân quyền khác đã đạt trong lượt toàn bộ. Các ca tích hợp bị bỏ qua không được tính là đã đạt.

## Áp dụng và giới hạn

Database SQL Server local vẫn ở **V18** sau kiểm tra; V19 mới chỉ được thử rồi rollback. Cần khởi động lại ứng dụng từ mã mới để Flyway áp dụng V19; PostgreSQL tương ứng dùng V3. Không sửa migration cũ hoặc xóa lịch sử Flyway. Xem [hướng dẫn thao tác](HUONG_DAN_ANH_THANH_PHAM_VA_HO_TRO.md).

Kiểm thử tải ảnh mới dùng CloudinaryService giả lập; chưa xác nhận upload Cloudinary thật trong phiên này. PostgreSQL thật, quét QR điện thoại và giao dịch thanh toán thật chưa được kiểm tra. Mỗi đơn hiện có một yêu cầu hỗ trợ và chỉ giữ ảnh thành phẩm/phản hồi hiện tại, chưa có nhật ký đầy đủ các phiên bản ảnh và mọi lần phản hồi.

Các thay đổi nằm trong working tree; không commit hoặc push GitHub.
