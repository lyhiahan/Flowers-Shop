# Hoàn thiện cấu trúc dự án — 28/10/2026

## Phạm vi

Hoàn thiện 5 nội dung trong lượt rà soát cấu trúc: tài liệu, dấu vết thuê, trang chủ lớn, tổ chức service và kiểm chứng Aiven/QR. Giữ nguyên các chỉnh sửa người dùng đang có trong `docs/HUONG_DAN_SETUP_VA_DANH_SACH_PROMPT_AI.md` và báo cáo Aiven ngày 26/10.

## 1. Tài liệu và profile

README đã được viết lại dựa trên source hiện tại:

- 4 role ADMIN/MANAGER/STAFF/USER và phạm vi chi nhánh.
- SQL Server V1–V17, PostgreSQL baseline V1 riêng; không so sánh version giữa hai engine như cùng một chuỗi.
- Maven spring-boot:run mặc định Aiven; chạy Spring trực tiếp mặc định dev.
- Hai script SQL Server (`run-local.ps1`, `sqlserver.ps1`) chọn `-Dspring-boot.run.profiles=dev` rõ ràng, tránh Maven mặc định chọn Aiven.
- Bỏ URL trycloudflare cố định cũ khỏi application-dev.properties; URL thủ công lấy từ DEMO_PAYMENT_BASE_URL.
- Giải thích Flyway, cấu hình mail/ảnh, test, database lịch sử và giới hạn kiểm chứng.
- Thêm `KIEN_TRUC_HIEN_TAI.md` làm quy ước cho source sau refactor. Các báo cáo cũ tiếp tục là lịch sử của ngày tương ứng.

## 2. Nghiệp vụ thuê

Đã bỏ scheduler thuê quá hạn, truy vấn quét quá hạn và các phương thức service trả/chuyển đơn thuê không còn dùng. Không còn cờ app.rental.enabled.

Giỏ/checkout chỉ tính tiền mua hoa, không có dòng tiền cọc hoặc JavaScript cộng tiền cọc. Dòng giỏ loại RENT cũ không thể thanh toán; service đặt đơn cũng kiểm tra BUY trước khi tạo đơn. Sản phẩm mới do manager tạo không còn được gán giá thuê/cọc theo tỷ lệ giá bán. Bỏ thống kê thuê không dùng ở dashboard admin và hàm toggleRentDates không còn dùng trên trang chủ.

Không DROP cột, đổi tên database hoặc viết lại migration cũ. `RentalShop`, email demo, public_id Cloudinary, các cột/enum thuê và phép tính chi tiết đơn lịch sử được giữ để đọc dữ liệu đã tồn tại. Thông tin cọc/phạt của đơn cũ vẫn có thể hiển thị trong lịch sử. Xem ranh giới tương thích trong tài liệu kiến trúc.

## 3. Trang chủ

`index.html`: **2989 → 408 dòng**. Đã tách nguyên CSS và JavaScript khỏi index, giữ thứ tự tải và cấu trúc DOM:

- `static/css/home.css`.
- `static/js/home-navigation.js`, `home-follow.js`, `home-filters.js`.
- `templates/fragments/home-header.html`, `home-catalog.html`, `home-filters.html`.

Bỏ hàm điều khiển ngày thuê không còn được gọi. Bổ sung kiểm thử hành vi bộ lọc: sort giữ từ khóa/chi nhánh, xóa rating giữ bộ lọc khác, reset thuộc tính giữ danh mục/chi nhánh. Test render tiếp tục kiểm tra layout, giỏ hàng và danh mục; test cũ phụ thuộc JavaScript inline được cập nhật để kiểm tra tệp script thực sự được tải.

## 4. Service

Hợp nhất 13 cặp interface + implementation thành service cụ thể trong `com.finalproject.service`; bỏ package impl. Chuyển CustomUserDetailsService sang cùng package, vẫn triển khai UserDetailsService của Spring. Cập nhật import, test, reflection và record lồng nhau.

Giữ constructor injection, transaction và logic nghiệp vụ. Wrapper checkout(username, paymentMethod) được đánh dấu transactional để việc tự gọi overload không bỏ qua transaction trên Spring proxy. Các record UploadResult và OrderAction giữ tên API cũ. Bỏ helper uploadImage đã deprecated và không có caller.

## 5. Aiven và kết nối điện thoại

### Aiven trực tiếp

- TLS verify-full và PostgreSQL login: đạt.
- Transaction database xác nhận transaction_read_only=on: đạt.
- Flyway hiện tại trên Aiven: V1, phù hợp baseline PostgreSQL.
- Các bảng tài khoản, cửa hàng, sản phẩm, kho, đơn, thanh toán và ví đọc được.
- Không phát hiện ví khách có số dư âm trong truy vấn kiểm tra.
- Mã hiện tại khởi động và Hibernate validate schema thành công.
- GET `/`, `/products`, `/stores`, CSS home và 3 script mới: HTTP 200.

Phiên ứng dụng dùng kết nối chỉ đọc, tắt migration/seed/tác vụ hết hạn/tunnel và cổng tạm; tự đóng sau kiểm tra. Không tạo đơn, thanh toán, nạp/rút ví hoặc chuyển dữ liệu. Không thử đăng nhập/tạo JWT trên Aiven trong lượt kiểm tra mới này.

Công cụ tái chạy: `scripts/verify-aiven.ps1` và `-Application`. Thêm cờ app.order-expiry.enabled cho phiên smoke; mặc định ứng dụng thường vẫn chạy tác vụ hết hạn như trước.

### Cloudflare/QR

Kiểm tra live ngày 28/10: nhà cung cấp trả **HTTP 429** khi cấp quick tunnel. Supervisor đã nhận diện RATE_LIMITED; lượt kiểm tra fixture cuối không đạt READY trong 90 giây. Không coi lỗi này là kiểm chứng QR điện thoại đã thành công.

Đã khắc phục các điểm trong mã có thể làm tình trạng này nặng hơn:

- Không thay hostname mới chỉ vì 3 probe đầu chưa resolve; chờ đến hết hạn khởi động cho DNS cập nhật.
- Exponential backoff cho các lần khởi tạo thất bại, tối đa 60 giây; 429 chờ ít nhất 60 giây.
- Thu hồi URL/QR khi mất kết nối; chỉ đưa URL trở lại khi gateway được nhận diện và kiểm tra đúng.
- Hiển thị trạng thái bị giới hạn và tự thử lại cho người dùng.
- Thêm script fixture `verify-demo-phone.cjs`: kiểm tra HTTPS, chặn route admin/API, chuyển cookie và body xác nhận; không dùng database và tự đóng tunnel.

Bộ test cục bộ kiểm tra gateway, QR và trạng thái tunnel; kết quả live vẫn cần thử lại khi nhà cung cấp cấp tunnel. Chưa thao tác điện thoại vật lý và chưa xác nhận thanh toán QR trên Aiven dùng chung. Không dùng URL tunnel cũ để báo thành công.

## Kết quả kiểm thử

- JavaScript: **176/176 đạt**, gồm gateway localhost, chờ DNS và rate limit.
- Java toàn bộ: 854 ca: 842 đạt, 12 bỏ qua theo điều kiện, 0 thất bại/lỗi; BUILD SUCCESS.
- PostgreSQL embedded: 48 ca: 47 đạt, 1 bỏ qua theo điều kiện, 0 thất bại/lỗi; BUILD SUCCESS.
- Aiven database và application smoke chỉ đọc: đạt như mô tả trên.
- Cloudflare live: chưa đạt, bị nhà cung cấp giới hạn HTTP 429.

Bằng chứng cục bộ trong `reports/structure-review-20261028/`: `java-tests-final.log`, `js-tests.log`, `postgresql-tests.log`, `aiven-readonly.log`, `aiven-application.log`, `phone-live-final.log`.

Không sửa database dùng chung, không commit hoặc push trong đợt này.
