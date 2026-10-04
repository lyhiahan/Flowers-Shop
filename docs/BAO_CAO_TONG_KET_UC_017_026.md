# Báo cáo tổng kết công việc — FlowerShop

**Ngày:** 04/10/2026  
**Phạm vi:** phân tích repo, đồng bộ code, lập kế hoạch, xử lý lỗi và hoàn thiện UC_017–UC_026; khắc phục giao diện Dashboard cửa hàng.

## 1. Kết quả chính

Đã phân tích kiến trúc và đối chiếu 10 use case với controller, service, repository, template và kiểm thử hiện có. Sau khi đồng bộ code từ remote, đã lập implementation plan và triển khai bổ sung các luồng còn thiếu, sửa phân quyền cửa hàng, nghiệp vụ nhân sự/ví, khả năng mua sản phẩm và thống kê.

Lượt kiểm thử toàn bộ sau triển khai có **228 ca: 227 đạt, 0 thất bại, 0 lỗi, 1 bỏ qua** do Cloudinary live chưa bật. Sau đó đã khắc phục lỗi giao diện Dashboard mà người dùng phản ánh; **33 ca kiểm thử liên quan đều đạt**. Lượt toàn bộ 228 ca diễn ra trước lần sửa giao diện cuối, chưa chạy lại toàn bộ sau lần sửa này.

Các thay đổi đang ở working tree của nhánh `codex/complete-store-use-cases`, **chưa commit/push**. Chưa triển khai lên môi trường vận hành và chưa chạy migration trên SQL Server thật. Vì vậy, kết quả này xác nhận phạm vi đã triển khai và kiểm thử, chưa phải nghiệm thu toàn bộ 10 UC trong môi trường production.

## 2. Phân tích và đồng bộ repository

Repo sử dụng Java, Spring Boot MVC, Thymeleaf, Spring Security và JPA. SQL Server là cơ sở dữ liệu vận hành; bộ kiểm thử sử dụng H2 ở chế độ tương thích SQL Server. Đã khảo sát đường đi từ màn hình manager/admin đến tầng nghiệp vụ và truy vấn dữ liệu, đặc biệt các điểm kiểm tra quyền và trạng thái sản phẩm/đơn hàng.

Đã bảo toàn thay đổi local bằng stash, fetch remote và fast-forward `main` từ `12acff6` đến `f33474f`, nhận **6 commit mới**, không có xung đột; sau đó áp dụng lại các thay đổi local. Stash dự phòng `codex-pre-sync-uc017-026-20261004` được giữ lại. Các thay đổi local có trước công việc và thay đổi của người dùng trong `application.properties` không được tính là phần triển khai của báo cáo này.

Baseline sau đồng bộ có **216 ca: 193 đạt, 22 thất bại, 0 lỗi, 1 bỏ qua**. Đã ghi nhận hiện trạng, bằng chứng kiểm thử và lập kế hoạch P0–P6 để xử lý theo nhóm phân quyền, nghiệp vụ, dữ liệu, thống kê và nghiệm thu.

Tài liệu liên quan:

- [Phân tích repo và audit ban đầu](UC_017_026_AUDIT_20261004.md).
- [Baseline sau đồng bộ](UC_017_026_POST_SYNC_TEST_RESULTS_20261004.json).
- [Implementation plan](UC_017_026_IMPLEMENTATION_PLAN.md).

## 3. Tổng kết theo use case

| UC | Nội dung đã xử lý | Bằng chứng và mức kiểm chứng |
|---|---|---|
| **UC_017 — Tạo cửa hàng** | Kiểm tra commission còn hiệu lực, dữ liệu nhập và quyền; giới hạn trường được nhận; Confirm/Cancel; tạo shop trong transaction, chống tạo trùng khi gửi lại; chọn đúng shop sau tạo. | MockMvc kiểm tra validation, CSRF, retry; browser xác minh Submit → Cancel và Submit → Confirm. |
| **UC_018 — Quản lý cửa hàng** | Danh sách shop sở hữu/hợp tác, phân trang; context shop theo session; Admin Licensed/Unlicensed; ban/license giữ nguyên lựa chọn Selling/Stored và trạng thái sản phẩm. | Kiểm thử phạm vi dữ liệu, thu hồi quyền, session, phân trang; sản phẩm shop bị cấm bị chặn ở public/cart/quote. |
| **UC_019 — Profile cửa hàng** | Cập nhật thông tin, avatar/cover; thêm/sửa/xóa featured image; kiểm tra URL và file; partial update không tự đóng shop; cleanup ảnh gắn với kết quả transaction. | Kiểm thử CRUD featured, file sai nội dung/MIME và partial update. Cloudinary live chưa nghiệm thu. |
| **UC_020 — Nhân viên** | Owner/staff tabs, phân trang và tìm ứng viên; owner thêm/xóa STAFF/PARTNER; partner tự rời shop; nhiều membership; kiểm tra cả batch trước khi ghi. | Kiểm thử quyền owner/partner, ngăn lấy staff shop khác, rollback batch lỗi và partner nhiều shop. |
| **UC_021 — Sản phẩm** | Selling/Stored, phân trang DB; form tạo/sửa riêng; Confirm/Cancel; chuyển trạng thái theo đích rõ ràng; đọc tồn kho theo batch. | Kiểm thử CRUD/filter; browser xác minh tạo sản phẩm, Confirm/Cancel và chuyển Stored. |
| **UC_022 — Đơn hàng** | Phân loại Processing/Processed theo workflow; Processing còn gồm DELIVERED và REFUND_REQUESTED; phân trang theo shop; dropdown chỉ có chuyển trạng thái hợp lệ. | Kiểm thử quản lý đơn, workflow và regression đồng thời. |
| **UC_023 — Ví cửa hàng** | Danh sách giao dịch phân trang; rút tiền chỉ dành cho owner; BCrypt, validation tiền/thông tin ngân hàng; transaction, khóa DB và request key chống trừ tiền lặp. | Kiểm thử từ chối partner, không nhận hash làm mật khẩu, retry và concurrency. |
| **UC_024 — Doanh thu** | Thống kê dùng chung, chọn revenue/orders/products, giờ/ngày/tháng/năm và đường/cột/tròn; doanh thu theo trạng thái hợp lệ, lượng bán từ chi tiết đơn; top và phân trang. | Kiểm thử doanh thu, refund, lượng bán thực, phạm vi shop; browser Vendor dashboard và kiểm tra JavaScript khởi tạo biểu đồ. |
| **UC_025 — Người dùng mới** | KPI tổng user, biểu đồ tối đa 1000 user mới trong khoảng; timestamp và dấu xác định độ chính xác; top user theo điểm, phân trang. | Kiểm thử thống kê, dữ liệu lịch sử chỉ có ngày và mẫu 1001 user. |
| **UC_026 — Cửa hàng mới** | KPI tổng shop, bộ lọc thời gian/loại biểu đồ, mẫu tối đa 1000 shop trong khoảng; top shop theo điểm, phân trang. | Kiểm thử thống kê shop, render Admin và khởi tạo biểu đồ. |

Các UC đều có phần triển khai và bằng chứng kiểm thử. Mức kiểm chứng khác nhau giữa các UC; browser smoke chưa bao phủ mọi actor và mọi nhánh của đặc tả.

## 4. Những thay đổi nền tảng

### Phân quyền và quản lý nhiều cửa hàng

Đã bổ sung `StoreMembership` với vai trò PARTNER/STAFF, trạng thái hiệu lực và ràng buộc duy nhất theo user/store. `StoreAccessService` xác minh owner, membership và dữ liệu assignment cũ. Shop đang chọn nằm trong session và được kiểm tra lại mỗi request; chuyển shop không ghi đè `User.store`. Membership STAFF không tự cấp quyền quản lý chỉ vì tài khoản có vai trò manager ở nơi khác.

Thao tác thêm/xóa nhân sự và rút tiền kiểm tra owner ở tầng nghiệp vụ. Tuyển nhân sự theo batch kiểm tra toàn bộ ứng viên trước khi ghi, tránh cập nhật một phần khi có ứng viên không hợp lệ.

### Giao dịch và khả năng mua sản phẩm

Đã bổ sung request key và khóa DB cho tạo cửa hàng/rút tiền để gửi lại yêu cầu không tạo shop hoặc trừ ví lần hai. Điều kiện sản phẩm có thể mua được dùng thống nhất ở catalog, chi tiết cửa hàng/sản phẩm, cart, quote và checkout. Ban shop chỉ đổi trạng thái giấy phép, không ghi đè trạng thái riêng của từng sản phẩm.

### Hợp đồng thống kê

Đã thống nhất múi giờ `Asia/Bangkok`: giờ là 24 giờ hôm nay, ngày là 7 ngày gần nhất, tháng là 12 tháng năm hiện tại, năm là 5 năm gần nhất. KPI doanh thu lấy toàn bộ đơn đủ điều kiện trong khoảng; biểu đồ lấy tối đa 1000 đơn mới nhất và hiển thị phạm vi mẫu. Lượng bán lấy từ chi tiết đơn, không dùng ngày tạo sản phẩm.

Doanh thu ghi nhận từ đơn PAID với DELIVERED/COMPLETED, hoặc REFUND_REQUESTED đã có ORDER_PAYMENT; REFUNDED được loại khỏi tổng. Top sản phẩm tổng hợp quantity trên toàn bộ đơn hợp lệ trong khoảng. Top user/shop theo điểm độc lập với danh sách đăng ký mới. Bảng Top hiển thị 20 dòng/trang.

### Ảnh, migration và lỗi liên quan

Upload giới hạn 10 MB, kiểm MIME/extension/signature; PNG/JPEG/GIF có kiểm tra decode. Ảnh mới được cleanup khi rollback, ảnh cũ cleanup sau commit. Featured image có metadata public ID và giới hạn 20 ảnh.

Đã viết migration additive/repeatable [20261004_store_use_cases.sql](../sql/migrations/20261004_store_use_cases.sql) cho membership, timestamp user, request keys, index/ràng buộc và metadata ảnh. Migration chưa chạy trên DB vận hành.

Trong quá trình kiểm tra frontend, đã sửa lỗi cấu trúc HTML ở cart/profile, JavaScript checkout và phục hồi script quote, cùng lỗi giữ bộ lọc/ID trùng ở catalog.

## 5. Khắc phục giao diện Dashboard cửa hàng

Sau triển khai thống kê, template Dashboard đã bị thay bằng bố cục quá đơn giản, làm mất giao diện quen thuộc trong ảnh người dùng gửi. Đây là lỗi hồi quy phát sinh từ thay đổi của tôi.

Đã khôi phục sidebar, font Nunito, tông xanh và nền trung tính, các thẻ KPI và bố cục responsive trong `manager/dashboard.html`, đồng thời giữ bộ lọc/thống kê mới. Đã bổ sung định dạng VND, trạng thái không có dữ liệu thay cho biểu đồ trống quá lớn, bảng Top/phân trang và thông báo khi thư viện biểu đồ không tải được.

Đã kiểm tra trực quan trên môi trường demo H2 riêng ở cổng **8081**, lưu [ảnh giao diện đã sửa](MANAGER_DASHBOARD_LAYOUT_FIXED.png) và dừng server demo sau kiểm tra. Không coi đây là bằng chứng đã kiểm tra trực tiếp phiên đăng nhập của người dùng trên cổng 8080.

## 6. Kết quả kiểm thử

| Lượt kiểm tra | Tổng | Đạt | Thất bại | Lỗi | Bỏ qua |
|---|---:|---:|---:|---:|---:|
| Baseline sau pull | 216 | 193 | 22 | 0 | 1 |
| Toàn bộ sau triển khai, 18:32 ngày 04/10 | 228 | 227 | 0 | 0 | 1 |
| Hai suite liên quan sau sửa Dashboard, 18:49 | 33 | 33 | 0 | 0 | 0 |

**33 ca là tập kiểm thử liên quan chạy lại, không cộng thành 261 ca độc lập.** Cloudinary live là ca bỏ qua trong lượt toàn bộ. Lượt kiểm thử dùng JDK 25, profile test/H2 MSSQL mode.

Các kiểm tra frontend đã đạt: 64 template/local JavaScript không có lỗi được checker báo; checkout quote checks, Admin chart initializer và `git diff --check` đều đạt. Browser smoke đã kiểm UC_017, UC_021 và Vendor dashboard, gồm lần kiểm tra giao diện phục hồi.

Bằng chứng:

- [Kết quả chi tiết từng ca và provenance](UC_017_026_IMPLEMENTATION_TEST_RESULTS.json) — thuộc lượt trước sửa giao diện cuối.
- Log toàn bộ: `target/uc-implementation-final-test.log`.
- Log sau sửa Dashboard: `target/dashboard-layout-tests.log`.
- [Ảnh xác nhận tạo shop](UC_017_BROWSER_CONFIRM.png), [ảnh xác nhận tạo sản phẩm](UC_021_BROWSER_CONFIRM.png).
- [Báo cáo triển khai kỹ thuật](UC_017_026_IMPLEMENTATION_RESULTS.md).

## 7. Phần chưa hoàn tất và đề xuất tiếp tục

1. **Nghiệm thu SQL Server thật:** kết nối `localhost\SQLEXPRESS` gặp instance lookup/login timeout. Cần chạy migration trên DB kiểm thử và xác minh kiểu tiền, khóa, phân trang, ràng buộc/index. H2 chưa xác nhận hành vi thực tế của SQL Server.
2. **Chạy lại toàn bộ suite trên trạng thái cuối**, sau đó E2E đủ owner/partner/staff/admin cho 10 UC, gồm Confirm/Cancel, file picker, thu hồi quyền và kích thước màn hình nhỏ.
3. **Nghiệm thu upload thật:** bật Cloudinary live khi có cấu hình; kiểm WEBP khi lưu local. Cleanup hiện best-effort, chưa có hàng đợi retry khi dịch vụ xóa ảnh lỗi.
4. **Hoàn thiện hardening trong plan:** DTO/response thống kê có kiểu, tối ưu các đường analytics legacy, execution plan và index tuning trên SQL Server. Chưa đánh dấu hoàn tất toàn bộ checklist P0–P6.
5. **Review diff, commit và push** sau nghiệm thu; đối chiếu riêng các thay đổi của người dùng để không gộp nhầm vào phần triển khai.

Không gọi Mail/MoMo thật trong phạm vi kiểm thử này. Chưa có xác nhận deployment hoặc dữ liệu production đã được cập nhật.
