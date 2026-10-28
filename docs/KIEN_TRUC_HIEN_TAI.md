# Kiến trúc và quy ước hiện tại

Cập nhật ngày 28/10/2026. Đây là ứng dụng bán hoa theo chuỗi cửa hàng, monolith Spring MVC + Thymeleaf.

## Các lớp xử lý

1. Security chain xác thực và kiểm tra quyền URL.
2. Controller nhận/kiểm tra input, chuẩn bị model hoặc JSON, chuyển nghiệp vụ cho service.
3. Service kiểm tra quyền sở hữu/chi nhánh và thực hiện nghiệp vụ trong transaction phù hợp.
4. Repository truy vấn dữ liệu; entity ánh xạ schema do Flyway quản lý.
5. Template/fragment render giao diện; JavaScript bổ sung tương tác và WebSocket.

## Quy ước service

Các service nội bộ nằm trực tiếp trong `com.finalproject.service`, là lớp cụ thể có tên `*Service`. Không tạo cặp interface/implementation chỉ có một implementation. Đợt refactor này đã hợp nhất 13 cặp; constructor injection và các annotation transaction được giữ. Adapter `CustomUserDetailsService` ở cùng package và vẫn triển khai interface `UserDetailsService` của Spring.

- Dùng constructor injection, ưu tiên trường `final` và `@RequiredArgsConstructor`.
- Đặt transaction ở phương thức public được gọi qua Spring proxy. Khi wrapper tự gọi phương thức trong cùng lớp, wrapper cũng phải có transaction nếu cần.
- Chỉ tạo interface khi có nhiều implementation thực sự hoặc cần hợp đồng tích hợp độc lập.
- Không dùng tên bean phụ thuộc hậu tố Impl. Controller và test tham chiếu service theo kiểu lớp.
- Các record trả về như `CloudinaryService.UploadResult`, `StaffService.OrderAction` giữ nguyên vị trí API.
- `OrderService` xử lý đặt hàng/báo giá; `OrderWorkflowService` xử lý vòng đời; `OrderFundsService` xử lý tiền cửa hàng; `CustomerWalletService` xử lý ví khách.
- `StoreAccessService` kiểm tra chi nhánh tại service; việc ẩn nút giao diện không thay kiểm tra quyền.

## Trang chủ

`templates/index.html` giữ khung trang và thứ tự tải script. Các phần lớn được tách:

| Tệp | Trách nhiệm |
|---|---|
| `fragments/home-header.html` | Header, điều hướng, mega menu và tài khoản |
| `fragments/home-catalog.html` | Danh mục, bộ lọc đang áp dụng, danh sách và phân trang |
| `fragments/home-filters.html` | Drawer chọn bộ lọc |
| `static/css/home.css` | CSS trước đây nằm trong style của index |
| `static/js/home-navigation.js` | Điều hướng và tương tác mega menu |
| `static/js/home-follow.js` | Yêu thích nhanh và toast |
| `static/js/home-filters.js` | Trạng thái bộ lọc, sort và URL |

Giữ nguyên thứ tự CSS/script và ID DOM để giỏ hàng, chat, thông báo và làm mới tồn kho tiếp tục hoạt động. Các hàm được gọi bởi HTML vẫn có tên toàn cục tương thích. CSS/JS đã tách không chứa biểu thức Thymeleaf; dữ liệu động nằm trong template/thuộc tính DOM.

## Dữ liệu lịch sử

<a id="du-lieu-lich-su"></a>

Nghiệp vụ đang hỗ trợ là mua hoa (`BUY`). Không còn scheduler quét thuê quá hạn hoặc phương thức service trả đồ/chuyển đơn thuê. Cart/checkout không tính tiền cọc; request tạo giỏ, báo giá và đặt hàng từ chối loại đơn khác BUY. Các dòng giỏ thuê cũ bị khóa mua và có thể xóa.

Các thành phần dưới đây được giữ để không làm hỏng database và lịch sử:

- Tên database SQL Server `RentalShop`; email demo và folder/public_id Cloudinary cũ.
- Enum `RENT`, `RENTING`, `RETURNED`, `OVERDUE` để Hibernate đọc bản ghi cũ.
- Ánh xạ các cột ngày thuê, giá thuê/cọc, `returnStore`, `totalDeposit`, `fineAmount`.
- Cách tính subtotal chi tiết đơn thuê và thông tin tiền cọc/phạt trên lịch sử đơn đã tồn tại.
- Ràng buộc repository nhận diện đơn cũ còn mở, tránh xóa sản phẩm mất liên kết lịch sử.
- Script/migration cũ là lịch sử bất biến; không sửa checksum migration đã triển khai.

Không có migration DROP cột hoặc đổi database trong đợt này. Nếu muốn xóa hoàn toàn các cột/enum cũ cần kiểm kê dữ liệu, chuyển/đóng đơn lịch sử, sao lưu và triển khai migration riêng cho từng engine.

## Database

SQL Server dùng chuỗi migration V1–V17 (kết hợp SQL/Java). PostgreSQL dùng baseline V1 riêng đã chứa mô hình hiện tại. Không chạy migration SQL Server lên PostgreSQL. Xác nhận database thật bằng profile và `flyway_schema_history`; không suy từ tên version giữa hai engine.

Aiven đọc `.env.aiven` riêng và chặn ghép với dev/test. Maven spring-boot:run mặc định Aiven; Spring chạy trực tiếp mặc định dev. Script run-local chọn dev rõ ràng.

## Kiểm chứng

Sau thay đổi package chạy clean để xóa class cũ. Kiểm tra Java/Spring bằng H2; kiểm tra JavaScript bằng node:test; kiểm tra PostgreSQL riêng bằng profile postgresql-tests. Kiểm tra Aiven live chỉ đọc bằng verify-aiven.ps1. Kiểm tra tunnel live dùng fixture cục bộ, không tạo đơn trên database dùng chung.

Chứng cứ và phần chưa thể xác nhận được ghi ở `docs/HOAN_THIEN_CAU_TRUC_20261028.md`.
