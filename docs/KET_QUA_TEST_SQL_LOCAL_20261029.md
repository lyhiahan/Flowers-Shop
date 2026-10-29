# Kiểm tra hoạt động với SQL Server local — 29/10/2026

## Website và dữ liệu

Bản mã mới đang chạy tại **http://localhost:8081**, dùng SQL Server `127.0.0.1:53735`, database `RentalShop`. Cổng 8081 được dùng để giữ nguyên phiên web cũ tại 8080; cần mở đúng cổng 8081 để kiểm tra tính năng mới. Database có 43 sản phẩm và 17 tài khoản. Đã sao lưu COPY_ONLY có CHECKSUM, xác minh bản sao lưu rồi áp dụng thành công migration V19 bằng Flyway.

Khởi động lại bằng lệnh:

```powershell
.\scripts\run-local.ps1 -DatabaseName RentalShop -Server '127.0.0.1:53735' -Port 8081
```

Nếu muốn dùng cổng 8080, dừng phiên đang giữ cổng đó rồi đổi tham số `-Port`. Tài khoản SQL lấy từ `.env`; không đưa mật khẩu vào lệnh hoặc báo cáo. Lệnh `start-web.ps1 -Database Local` hiện dành cho PostgreSQL local, không phải SQL Server.

## Kết quả kiểm thử

| Nhóm | Kết quả |
|---|---|
| Nghiệp vụ trên SQL Server thật | **257/257 đạt**, không bỏ qua; BUILD SUCCESS |
| Bộ hồi quy với cấu hình gốc | **893 ca: 881 đạt, 12 bỏ qua, 0 thất bại, 0 lỗi**; BUILD SUCCESS trong một lượt toàn bộ |
| JavaScript | **188/188 đạt** |
| HTTP website đang chạy với dữ liệu local | **56/56 đạt**: trang công khai, đăng nhập, các trang chính của bốn vai trò và kiểm tra sai quyền |
| HTTP bổ sung cho trang chi tiết và tài nguyên | **21/21 đạt**: đăng nhập bằng session, chi tiết đơn cả bốn vai trò, khối ảnh/hỗ trợ mới, sản phẩm/cửa hàng, tài nguyên và chặn đơn khác chủ/chi nhánh |
| Kiểm tra tính nhất quán database chính sau test | Không có tồn kho âm, ví khách âm hoặc voucher có lượt dùng âm/vượt số lượng |

257 ca SQL dùng database thử `CodexFlowTests_20261029_2`, được tạo schema bằng đúng Flyway V1–V19, với Hibernate ở chế độ `validate`. Các ca tạo/hủy đơn, thanh toán, tồn kho và số dư không ghi vào dữ liệu nghiệp vụ của `RentalShop`. 10 nhóm được chạy gồm `BusinessRegressionTests` (153 ca), `AdminCategoryManagementUseCaseTests` (10), `AdminOrdersManagementUseCaseTests` (8), `AdminProductsManagementUseCaseTests` (17), `AdminUsersManagementUseCaseTests` (11), `ChatBranchFlowTests` (10), `CheckoutEdgeSafetyTests` (17), `DemoPaymentTests` (12), `OrderAftercareTests` (9) và `StaffWorkspaceTests` (10).

Phạm vi kiểm tra gồm quản trị danh mục/tài khoản/sản phẩm/đơn, đăng ký–OTP giả lập–đăng nhập, khóa tài khoản và thu hồi quyền, giỏ hàng và đặt hoa, kiểm tra tồn kho, voucher và phí giao hàng, thanh toán demo, hủy/hoàn khoản, dòng tiền ví, xử lý đơn đúng chi nhánh, tư vấn có ngữ cảnh đơn, duyệt ảnh thành phẩm theo phiên bản, minh chứng bàn giao, khách xác nhận nhận hoa, hỗ trợ sau giao, CSRF và chống truy cập đơn khác chủ/khác chi nhánh. Những kiểm tra cạnh tranh và WebSocket khác nằm trong bộ hồi quy gốc; không tính toàn bộ 893 ca là đã chạy trên SQL Server.

## Điều chỉnh hạ tầng kiểm thử

Không thể ép mọi test sang SQL Server chỉ bằng biến môi trường vì một số nhóm cố định URL/driver H2 trong annotation. Đồng thời, bootstrap chuyển đổi vai trò dành cho schema test cũ sẽ làm sai schema đã được Flyway nâng lên V16 trở đi nếu chạy lại trên SQL. Đã thêm điều kiện profile `test & !sql-integration` cho năm bootstrap test cũ, giữ nguyên hành vi test H2 và tắt chúng khi kiểm thử schema SQL đã được Flyway quản lý; cấu hình production không bật các bean test này.

Lượt thử ép toàn bộ ban đầu đã được dừng; kết quả của lượt đó không được tính là đạt. Với database SQL mới, danh mục mẫu do migration thêm vào được dọn trong database thử trống trước khi dựng fixture để tránh trùng tên và làm sai kỳ vọng danh sách trống. Không xóa danh mục thật hoặc nới constraint của database ứng dụng.

Script chạy lại nhóm SQL, sau khi chuẩn bị database thử đúng schema và credential:

```powershell
.\scripts\test-local-sql.ps1 -DatabaseName CodexFlowTests_20261029_2
```

Script chỉ chấp nhận tên database dạng `CodexFlowTests_<số và dấu gạch dưới>`, chỉ kết nối địa chỉ loopback, không tự xóa hoặc tạo lại schema. Không dùng database nghiệp vụ làm nơi chạy fixture test.

## Tốc độ và giới hạn kết luận

Đo một lượt sau khi các bộ test kết thúc: health 335 ms, trang chủ 522 ms, danh sách hoa 638 ms, danh sách cửa hàng 113 ms, chính sách dịch vụ 38 ms. Đây là thời gian nhận HTML qua HTTP local, không bao gồm tải ảnh Cloudinary, thư viện CDN hoặc toàn bộ quá trình trình duyệt dựng trang, và không phải kiểm thử tải nhiều người dùng.

Nhật ký có cảnh báo `Thread starvation or clock leap detected` với khoảng nhảy thời gian dài trong lúc chạy. Vì vậy không dùng tổng thời gian chạy bộ test để suy ra tốc độ của chức năng; các ca kiểm tra ảnh/hỗ trợ riêng lẻ sau khi context sẵn sàng vẫn hoàn tất trong khoảng 0,1–2 giây.

Các luồng được kiểm tra trong phạm vi trên đã đạt; chưa có căn cứ khẳng định mọi thao tác trong mọi điều kiện đều trơn tru. Upload Cloudinary thật, email OTP thật, thanh toán MoMo/ngân hàng thật, quét QR bằng điện thoại qua mạng ngoài và thao tác trực quan trên trình duyệt chưa được xác minh trong phiên này. Kiểm tra HTTP xác nhận phản hồi, trang đích và nội dung HTML, không thay thế thử nghiệm bố cục/click trên thiết bị thật.
