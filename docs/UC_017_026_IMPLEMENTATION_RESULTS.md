# Báo cáo triển khai UC_017–UC_026

Ngày 04/10/2026. Nhánh: `codex/complete-store-use-cases`, dựa trên `f33474f` đã đồng bộ từ `origin/main`. Code và tài liệu đang ở working tree; chưa commit/push. Các chỉnh sửa local có trước khi đồng bộ được giữ lại; stash dự phòng vẫn còn.

## 1. Từ tổng quan đến chi tiết

FlowerShop là ứng dụng Java Spring Boot MVC, Thymeleaf, Spring Security và JPA; SQL Server là DB vận hành, H2 dùng cho kiểm thử. Chức năng cửa hàng đi qua controller manager/vendor/admin, service phân quyền/nghiệp vụ, repository và các template tương ứng. Phân tích kiến trúc, hiện trạng và sáu lỗi hợp đồng ban đầu nằm trong `UC_017_026_AUDIT_20261004.md`; baseline sau pull nằm trong `UC_017_026_POST_SYNC_TEST_RESULTS_20261004.json`.

Các bất biến đã bổ sung:

- Quyền truy cập shop dựa trên `Store.owner`, membership còn hiệu lực và adapter assignment cũ. Cửa hàng vđang chọn nằm trong session, được xác minh lại mỗi request; chuyển shop không ghi vào `User.store`.
- Owner mới được thêm/xóa nhân sự và rút tiền. Partner quản lý shop được hợp tác và tự rời; membership STAFF không cấp quyền manager dù tài khoản có vai trò quản lý ở shop khác.
- Ban/license thay đổi giấy phép của store, giữ lựa chọn Selling/Stored, cờ duyệt và trạng thái mở cửa. Khả năng hiển thị/mua được kiểm tra ở catalog, public store, chi tiết, cart, quote và checkout.
- Create store và withdrawal có khóa DB cùng request key; gửi lại cùng yêu cầu không tạo thêm shop hoặc trừ ví lần hai. Batch tuyển nhân viên kiểm tra toàn bộ trước khi ghi.

## 2. Ma trận triển khai và bằng chứng

| UC | Triển khai | Kiểm chứng |
|---|---|---|
| UC_017 | New shop, commission còn hiệu lực, validation, chống mass assignment, Confirm/Cancel, redirect đúng shop | MockMvc tạo/invalid/deleted commission/CSRF/retry; browser Submit → Cancel → Submit → Confirm |
| UC_018 | Page danh sách shop sở hữu/hợp tác; context session; Admin Licensed/Unlicensed/search/page; ban/license giữ trạng thái sản phẩm | MockMvc scope/paging/session/revocation/license; ca chặn public/cart/quote sau Ban |
| UC_019 | Avatar/cover/featured CRUD, Save thông tin không tự đóng shop, URL và nội dung file được kiểm tra | MockMvc CRUD featured, file sai MIME/nội dung, partial update; frontend checker |
| UC_020 | Owner/staff tabs và paging; candidate search/page; thêm STAFF/PARTNER, xóa, leave; batch atomic; không lấy nhân viên shop khác | MockMvc owner/partner/foreign staff/batch rollback/multiple memberships |
| UC_021 | Selling/Stored + DB page; create/edit form riêng; Confirm/Cancel; explicit target Selling/Stored; stock đọc batch | MockMvc CRUD/filter; browser New product Confirm/Cancel và chuyển Stored |
| UC_022 | Processing gồm DELIVERED và REFUND_REQUESTED; Processed terminal; DB page/scope; dropdown theo workflow | Suite StoreManagementOrderUseCaseTests và regression workflow/concurrency |
| UC_023 | Page giao dịch; owner-only; BCrypt; amount/bank validation; withdrawal transaction + lock + request key | MockMvc hash rejection/partner rejection/retry; wallet và concurrency regressions |
| UC_024 | Chung AnalyticsService, tiêu chí revenue/orders/products, 4 khoảng thời gian, line/bar/pie, top sản phẩm từ chi tiết đơn | Tests revenue, quantity thực, foreign scope, refund; browser Vendor dashboard; kiểm initializer JS |
| UC_025 | KPI tổng user, chart controls, timestamp/độ chính xác, sample ≤1000 trong khoảng, top điểm + paging | Suite NewUsersStatisticsUseCaseTests; thêm date-only và 1001-user sample test |
| UC_026 | KPI tổng shop, chart controls đủ, sample ≤1000 trong khoảng, top điểm + paging | Suite NewStoresStatisticsUseCaseTests; render Admin và kiểm initializer |

Tên suite thực tế và kết quả từng ca được ghi trong JSON nghiệm thu, tránh coi tên bảng trên là danh sách exhaustive.

Ảnh browser: `UC_017_BROWSER_CONFIRM.png`, `UC_021_BROWSER_CONFIRM.png`. Đây là dữ liệu demo trên H2 riêng ở cổng 8081. Server kiểm thử đã dừng, không giữ một môi trường H2 đang chạy sau nghiệm thu.

## 3. Hợp đồng thống kê

Zone `Asia/Bangkok`; hour = 24 giờ hôm nay, day = 7 ngày gần nhất, month = 12 tháng năm hiện tại, year = 5 năm gần nhất. Admin dùng chung khoảng thời gian cho các chart và giữ lựa chọn loại biểu đồ khi đổi filter.

Doanh thu ghi nhận với PAID + DELIVERED/COMPLETED, hoặc REFUND_REQUESTED đã có ORDER_PAYMENT. REFUNDED được loại, không trừ hai lần. KPI doanh thu lấy tổng DB trong khoảng; chart thể hiện tối đa 1000 đơn mới nhất trong khoảng và ghi rõ số mẫu/tổng số đơn. Product series là số lượng bán từ chi tiết các đơn trong mẫu, không dùng ngày tạo product. Top sản phẩm lấy tổng quantity của toàn bộ đơn đủ điều kiện trong khoảng, giới hạn 1000 sản phẩm; các bảng Top render 20 dòng/trang. Top điểm user/store độc lập với dataset đăng ký mới.

User lịch sử chỉ có `createdDate` được backfill ngày, đánh dấu không biết giờ; không được coi là đăng ký lúc 00:00 trong hourly chart.

## 4. Ảnh và dữ liệu

Upload kiểm tra size ≤10 MB, MIME, extension, signature; PNG/JPEG/GIF còn kiểm tra khả năng decode. WEBP kiểm tra signature; khả năng decode thực tế phụ thuộc Cloudinary, cần kiểm thử riêng nếu dùng local WEBP. URL chỉ nhận http(s) có host hoặc path local cho phép; server không tự fetch URL người dùng nhập.

Ảnh mới được theo dõi để cleanup khi transaction rollback; ảnh cũ chỉ cleanup sau commit. Featured public IDs được lưu song song theo thứ tự ảnh, tương thích chuỗi URL cũ không có public ID; tối đa 20 ảnh. Cleanup Cloudinary là best-effort, chưa có hàng đợi retry khi dịch vụ xóa ảnh mất kết nối. ProductImage giữ các ảnh trước đó như gallery, không xóa ảnh vẫn được tham chiếu trong thư viện.

Migration additive, repeatable: `sql/migrations/20261004_store_use_cases.sql`, gồm membership, FK/check/index, timestamp user, request keys với filtered unique indexes và metadata featured image. Không chạy migration trên DB vận hành.

## 5. Kiểm thử và giới hạn nghiệm thu

**Maven: 228 ca, 227 đạt, 0 failure, 0 error, 1 skip (Cloudinary live).** So với baseline sau pull: 216 ca, 22 failure; hiện không còn failure trong toàn suite.

Kết quả cuối cùng xem `UC_017_026_IMPLEMENTATION_TEST_RESULTS.json`; log Maven ở `target/uc-implementation-final-test.log`. Các lệnh frontend:

```text
node scripts/check-frontend.cjs
node scripts/test-checkout-quote.cjs
node scripts/test-store-dashboard-audit.cjs
git diff --check
```

Đã kiểm tra 64 template và local JavaScript, không còn lỗi được checker báo; checkout quote checks đạt; Admin chart initializer đạt; diff không có lỗi whitespace.

**Giới hạn phải giữ rõ:**

- SQL Server `localhost\SQLEXPRESS` không truy cập được (instance lookup/login timeout). Chưa nghiệm thu migration, money/locking/paging trên SQL Server thật; các DB tests hiện chạy H2 MSSQL mode.
- Test Cloudinary thật opt-in đang skip; mock upload và kiểm tra file có chạy. Không gọi Mail/MoMo thật cho các UC này.
- Browser smoke xác minh UC_017, UC_021 và Vendor dashboard. Chưa chạy đầy đủ browser cho mọi actor, file picker, tất cả Confirm/Cancel của 10 UC; MockMvc/render/service tests có phạm vi rộng hơn browser smoke.
- Một số checklist kiến trúc của plan vẫn mở: DTO/typed statistics response thay entity allowlist/Map, index tuning/execution plan SQL Server và tối ưu các đường analytics legacy còn đọc nhiều dữ liệu. Đây là phần cần tiếp tục hardening; không ghi nhận đã hoàn tất toàn bộ P0–P6.

## 6. Cách chạy lại

Dùng JDK ≥21; lượt này dùng Eclipse Adoptium 25.0.3. Profile `test` dùng H2, tắt demo seed/Mail/Cloudinary thật. Chạy `mvnw.cmd test` sau khi đặt JAVA_HOME phù hợp. Để chạy browser demo riêng, dùng spring-boot:run với test classpath, `--spring.config.additional-location=file:src/test/resources/application-test.properties --server.port=8081 --app.demo-data.enabled=true`, và tắt devtools restart qua JVM property nếu vừa chạy browser vừa compile. Không bật demo seed trên DB vận hành chỉ để nghiệm thu.
