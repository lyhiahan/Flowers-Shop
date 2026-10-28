# Đánh giá mẫu có ảnh

Dữ liệu phục vụ trình diễn, có thông báo chung về nội dung mô phỏng ở đầu khu vực đánh giá và chú thích ảnh minh họa. Không dùng danh tính khách thật hoặc tạo đơn hàng giả để hợp thức hóa đánh giá.

## Số lượng theo số đã bán

| Số đã bán | Tổng đánh giá mong muốn |
|---|---:|
| 0 | Không thêm |
| 1 | 1 |
| 2–19 | 2 |
| 20–49 | 3 |
| 50–79 | 4 |
| 80–119 | 5 |
| Từ 120 | 6 |

Chỉ bổ sung phần thiếu sau khi tính cả đánh giá đang có, không xóa/sửa đánh giá của khách. Chọn hoa chưa xóa, còn hiển thị và cửa hàng hoạt động. Mỗi ảnh được lấy từ ảnh sản phẩm hiện có hoặc ảnh hoa dự phòng trong dự án, không giả là ảnh khách chụp. Nội dung 50–1.000 ký tự; sao từ 3–5 để minh họa nhiều mức đánh giá.

Dùng tối đa 6 tài khoản `demo_review_01` đến `demo_review_06`, tên “Khách mẫu … (Demo)”. Tài khoản bị vô hiệu hóa, không đăng nhập hoặc xác thực email. Bản ghi mẫu không gắn đơn hàng thật. Điểm trung bình sản phẩm/cửa hàng được cập nhật theo các đánh giá hiển thị; số bán, kho, đơn và tiền không bị thay đổi.

## Công cụ

Cần cấu hình `.env.aiven` riêng và JDK 21. Không ghi mật khẩu vào mã hoặc Git.

```powershell
.\scripts\seed-demo-reviews.ps1 -Mode SelfTest
.\scripts\seed-demo-reviews.ps1 -Mode Plan
.\scripts\seed-demo-reviews.ps1 -Mode Rehearse
.\scripts\seed-demo-reviews.ps1 -Mode Apply
```

`Plan` chỉ đọc; `Rehearse` tạo dữ liệu và kiểm tra trong transaction rồi rollback. `Apply` chạy cùng kiểm tra, lưu snapshot ID và điểm cũ dưới `.tools/demo-review-backups/`, rồi commit. Chạy lại khi đã đủ không thêm trùng. Công cụ xác minh đơn, tổng số bán, tồn kho và số dư không thay đổi trong transaction.

Gỡ đúng batch bằng đường dẫn snapshot được in khi Apply:

```powershell
.\scripts\seed-demo-reviews.ps1 -Mode Rollback -Snapshot '.tools/demo-review-backups/reviews-<batch>.properties'
```

Giữ snapshot để gỡ dữ liệu. Khi gỡ, chỉ xóa đánh giá được đánh dấu Demo và ID trong batch; cập nhật lại điểm trung bình. Không chạy rollback với snapshot không rõ nguồn gốc.

## Kiểm tra ngày 28/10/2026

Kế hoạch: bổ sung 137 đánh giá cho 33 mẫu hoa, giữ đánh giá hiện có. Đã chạy rehearsal rollback thành công; cả 33 nguồn ảnh truy cập được; kiểm thử giao diện nhãn Demo/chú thích ảnh đạt.


Đã áp dụng thành công **137 đánh giá mẫu cho 33 mẫu hoa trên Aiven**, theo lựa chọn của người dùng. Kiểm tra lại cho kết quả cần thêm 0 đánh giá (không thêm trùng). Snapshot batch: `.tools/demo-review-backups/reviews-1793173975555.properties`.

Đã kiểm tra HTTP chi tiết các sản phẩm #1, #4, #21, #31: hiển thị đúng số nhãn Demo và ảnh minh họa; đánh giá có sẵn ở #21 vẫn còn. Avatar mẫu tải được. Bản xem Aiven hiện mở tại http://localhost:8081/product/1#review-section; website SQL Server cũ ở 8080 không được chuyển database trong tác vụ này. Bản xem tắt migration, seed và tác vụ hết hạn; trang chi tiết vẫn tăng lượt xem theo luồng thông thường.


### Lựa chọn chạy cuối cùng: SQL Server local

Người dùng chuyển về local để tải nhanh. Website hiện chạy SQL Server `RentalShop`, profile `dev`, tại http://localhost:8080. Local đã có 107 đánh giá, trong đó sản phẩm #22 có 2 đánh giá có ảnh; không thêm trùng. Đã nạp code mới, xác nhận trang #22 trả HTTP 200, HTML đầy đủ và hiển thị 2 ảnh đánh giá sau đăng nhập khách. Hai phiên Aiven tạm đã dừng; bản ghi đánh giá trên Aiven vẫn tồn tại trong database cloud riêng.

Tên mẫu trên thẻ đánh giá hiển thị bằng tên nhân vật hư cấu tự nhiên (Mai Anh, Hoàng Nam, Thu Hà, Khánh Linh, Quỳnh Trang, Bảo Ngọc). Tài khoản gốc và nội dung đánh dấu trong database vẫn giữ để nhận diện/gỡ dữ liệu. Phần hiển thị bỏ tiền tố và câu giải thích lặp; thông báo chung về nội dung mô phỏng và chú thích ảnh minh họa vẫn hiện rõ.

Cập nhật giao diện theo yêu cầu: bỏ nhãn cạnh tên và chữ DEMO trên avatar, dùng tên hiển thị tự nhiên; thẻ đánh giá ngăn bằng đường mảnh, căn nội dung và ảnh cùng cột. Thông báo minh họa chỉ xuất hiện một lần ở đầu khu vực có đánh giá mô phỏng.


## Sửa Unicode và đa dạng đánh giá local — 28/10/2026

Một script legacy từng được đọc sai UTF-8 thành Windows-1252, làm dấu tiếng Việt biến thành `Ã`, `Ä`, `áº...`. Công cụ `repair-local-review-samples.ps1` chỉ chọn nhận xét không gắn đơn và khớp chính xác nội dung fixture cũ (hoặc marker mẫu đã sửa), truyền tham số NVARCHAR và đọc nguồn bằng UTF-8. File PowerShell có BOM để tương thích Windows PowerShell 5.1. Script SQL legacy đã được chặn chạy trực tiếp để tránh tái tạo lỗi và gán đánh giá mẫu cho tài khoản khách.

```powershell
.\scripts\repair-local-review-samples.ps1 -Mode Plan
.\scripts\repair-local-review-samples.ps1 -Mode Rehearse
.\scripts\repair-local-review-samples.ps1 -Mode Apply
```

Đã rehearsal rollback rồi sửa 106 đánh giá mẫu, giữ nguyên 1 đánh giá còn lại. Các mức sao của nhóm mẫu: 1 sao: 6; 2 sao: 17; 3 sao: 30; 4 sao: 35; 5 sao: 18. Nội dung gồm hạn chế về bố cục, mức giá, thông tin kích thước, ưu điểm và đề xuất cải thiện, tương ứng số sao. Các đánh giá cùng sản phẩm luân phiên mức sao để tránh một loạt nhận xét giống nhau. Giữ ảnh, ngày tạo, định danh và số bản ghi; cập nhật điểm trung bình sản phẩm/cửa hàng. Không đổi đơn, tồn kho, số bán hoặc ví.

Bản sao trước sửa: `.tools/demo-review-backups/local-review-repair-20261028-153008.json`. Chỉ sửa local, không ghi vào Aiven. Mẫu legacy không có đơn được nhận diện bằng marker để áp dụng thông báo minh họa chung; đánh giá thật gắn đơn không bị đổi tên/nội dung/số sao.

Xác minh sau sửa: 5 ca Java đạt; đọc trang #22, #1, #32 đều HTTP 200 và tiếng Việt đúng. Sao hiển thị lần lượt [2,4], [2,5,4,3], [1,3,2,4]. Kiểm tra SQL bằng collation nhị phân không còn chuỗi mojibake ở 106 mẫu; điểm trung bình khớp. Tổng 107 đánh giá local sau sửa: 1 sao 6, 2 sao 17, 3 sao 30, 4 sao 35, 5 sao 19 (bao gồm 1 đánh giá không sửa).
