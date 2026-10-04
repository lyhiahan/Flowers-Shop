# Kết quả sửa theo báo cáo kiểm thử ngày 03/10/2026

Đã xử lý bảy nhóm lỗi trong báo cáo từ `test_frontend/`, giữ Bootstrap và các thư viện hiện có.

| Lỗi | Thay đổi |
| --- | --- |
| Nội dung Manager rớt dưới sidebar | CSS cho cột nội dung lấy phần chiều rộng còn lại sau sidebar 250px; màn hình nhỏ xếp thành một cột. |
| Giao dịch Admin lỗi ORDER BY SQL Server | Pageable không chứa Sort; repository sắp xếp một lần theo createdAt DESC, id DESC. Giới hạn size từ 1 đến 100. |
| Dữ liệu tiếng Việt thành `?` | Mapping Hibernate Nationalized cho các trường văn bản; migration chuyển kiểu cột sang NVARCHAR, giữ độ dài, nullable và unique constraint. |
| Menu vận chuyển trùng | Xóa khối li bị lặp và thiếu thẻ đóng ở commissions.html. |
| Ảnh sản phẩm bị vỡ | Ảnh lớn và thumbnail dùng ảnh `/images/flower-logo.png` khi tải lỗi. |
| Nút Tìm hoa xuống dòng | Thêm text-nowrap và flex-shrink-0. |
| Bảng Danh mục/Voucher cắt cột phải | Giữ lớp table-responsive sẵn có, sửa min-width của khung cha và cho bảng cuộn ngang bằng chuột/bàn phím. |

Khi khởi động lại còn phát hiện bộ tạo dữ liệu mẫu không xử lý được chi nhánh trùng tên. DataInitializer dùng `findFirstByNameOrderByIdAsc` để chọn bản ghi cũ nhất, không xóa hay gộp chi nhánh.

## Dữ liệu Unicode

`sql/migrations/20261003_ui_unicode.sql` được tạo bằng `node scripts/build-ui-unicode-repair.cjs`. Chỉ phục hồi chuỗi chứa `?` khi khớp duy nhất bản gốc trong seed Java/SQL, so sánh cả độ dài và các ký tự còn giữ được. Không đoán dấu hoặc thay `?` hàng loạt.

Đã áp dụng trên SQL Server local, database RentalShop, cổng 1433: phục hồi **60 giá trị**, lưu bản trước/sau trong `dbo.unicode_text_repair_backup`. Kiểm tra lại **33 cột: không còn giá trị chứa `?`**. Migration chạy lại không ghi đè bản sao. Bản sao sửa 33 tên sản phẩm ở migration trước vẫn nằm trong `dbo.product_name_unicode_repair_backup`.

## Kiểm chứng

- `mvn -B -ntp test` với JDK 21: **136 test, 0 lỗi, 0 bỏ qua**. Có test thứ tự giao dịch khi trùng thời gian, tìm kiếm giao dịch, size không hợp lệ, chi nhánh trùng tên và lưu/reload/render tiếng Việt.
- `node scripts/check-frontend.cjs`: **61 template và JavaScript nội bộ, 0 lỗi**.
- `node scripts/test-checkout-quote.cjs`: qua kiểm tra giá server, phản hồi đến muộn, trạng thái chờ và lỗi báo giá.
- HTTP sau khởi động: **16 lượt trả 200**: stores, products/1; bảy trang Manager trong báo cáo; transactions (danh sách và tìm kiếm), users, categories, deliveries, commissions, vouchers của Admin. Trang giao dịch không còn thông báo lỗi JDBC/ORDER BY.
- HTML HTTP lưu trong `target/report-live/`; log Maven trong `target/report-fixes-tests.log`. Đây là kiểm tra HTTP, không phải ảnh chụp sau sửa.

Server hiện chạy tại `http://localhost:8080/` với URL database override sang cổng 1433 và `app.demo-data.enabled=false`, để không chạy lại seed trong lượt kiểm tra. File application.properties của nhóm vẫn giữ cấu hình cổng 64078; lần chạy sau cần dùng đúng cổng SQL Server của môi trường đó. Script khởi động lượt này ở `target/start-review-server.ps1`.

## Lượt chụp lại giao diện cho agent kiểm thử

Chụp sau khi mở từng trang Manager trong báo cáo; kiểm tra nội dung xuất hiện ngang sidebar trên desktop. Chụp lại product-detail khi ảnh online lỗi, commissions chỉ có một mục vận chuyển, categories/vouchers có thể cuộn đến trọn cột thao tác. Chụp stores/users/deliveries/vouchers để đối chiếu chữ có dấu. Kiểm tra ở 375, 768 và 1440px; HTTP 200 không chứng minh bố cục đã đúng bằng mắt.

Không thêm thư viện/framework, không commit hoặc push.

## Bổ sung lỗi nút đặt hàng và hiển thị tồn kho

Đã tái hiện API báo giá trả 400 với thông báo “Chi nhánh không có hàng”: luồng giao tận nơi chọn chi nhánh theo địa chỉ trước khi kiểm tra mẫu hoa. Báo giá và đặt hàng hiện dùng chung cách chọn chi nhánh đang mở, có đủ toàn bộ sản phẩm đã chọn. Ưu tiên chi nhánh theo địa chỉ, rồi chi nhánh trong giỏ và các chi nhánh khác còn hàng. Nếu cùng sản phẩm nằm ở nhiều dòng giỏ, số lượng được cộng lại trước khi kiểm tra. Với đến lấy, vẫn giữ đúng chi nhánh khách chọn và báo thiếu hàng khi cần.

Tồn kho đã có trong DB, không tạo bảng mới: `store_inventories(store_id, product_id, quantity)`. Lượt kiểm tra DB có 34 dòng thuộc 3 chi nhánh; ví dụ store_id=1/product_id=1 còn 20. Giỏ hàng và chi tiết sản phẩm nay hiển thị số còn tại chi nhánh; số lượng mua có giới hạn từ kho. Backend kiểm tra cả thêm mới, cộng vào giỏ và đổi số lượng, không chỉ dựa vào max của input. Giỏ hàng không giữ chỗ; tồn kho vẫn được kiểm tra lại khi đặt đơn.

Các test bổ sung kiểm tra giao từ chi nhánh khác khi chi nhánh theo địa chỉ không có hàng, giữ nguyên lựa chọn đến lấy, cộng tồn kho cho các dòng trùng sản phẩm, render số tồn và từ chối số lượng vượt kho.

Kết quả sau bổ sung: 139 test Maven qua, 0 lỗi và 0 bỏ qua; kiểm tra 61 template/JavaScript và test bất đồng bộ báo giá đều qua. Log ở `target/cart-stock-tests.log`.
