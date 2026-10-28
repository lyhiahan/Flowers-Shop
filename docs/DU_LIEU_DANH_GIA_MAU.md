# Đánh giá mẫu có ảnh

Dữ liệu phục vụ trình diễn, có nhãn **Demo · Đánh giá mẫu** và chú thích ảnh minh họa. Không dùng danh tính khách thật hoặc tạo đơn hàng giả để hợp thức hóa đánh giá.

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
