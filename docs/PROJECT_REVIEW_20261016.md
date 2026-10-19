# Đánh giá dự án ngày 16/10/2026

## Kết luận

Dự án đã triển khai đủ các công nghệ trong yêu cầu: Spring Boot, Thymeleaf, Bootstrap, JPA, SQL Server, WebSocket, JWT và Cloudinary. Cấu trúc phân lớp và các phân hệ khách hàng, quản lý chi nhánh, quản trị viên phù hợp bài toán chuỗi cửa hàng. Phần nghiệp vụ và phân quyền có bộ kiểm thử tương đối rộng.

Tuy nhiên, chưa thể kết luận hoàn thiện 100% hoặc chắc chắn đạt 10 điểm: bộ test hiện còn thất bại, database thật chưa cập nhật theo mã nguồn mới nhất, tồn kho cũ chưa đối soát xong, logic xác định vùng giao demo có lỗi và tài liệu còn thông tin cũ.

Phạm vi đánh giá gốc ngày 16/10: mã nguồn tại commit `0b6bcac` (`merge code`), test tự động, đọc dữ liệu SQL Server và rubric đã cung cấp. Các số liệu test bên dưới là **ảnh chụp tại thời điểm 16/10**, không phải kết quả mới nhất. Đợt sửa tiếp ngày 19/10 đã cập nhật test, vùng giao demo, tài liệu và thử Flyway V9–V10 trên bản sao; `RentalShop` gốc vẫn V8. Xem kết quả mới trong phần cập nhật cuối tài liệu.

## 1. Cấu trúc

```text
src/main/java/com/finalproject/
  config/        Cấu hình ứng dụng, bảo mật, WebSocket
  controller/    Các đầu vào web/API theo vai trò
  service/       Nghiệp vụ, giao dịch, tồn kho, chat, OTP, ảnh
  repository/    Truy vấn JPA
  entity/        Mô hình dữ liệu
  dto/           Dữ liệu đầu vào/đầu ra
  enums/         Vai trò, trạng thái
  db/migration/  Migration Java
  exception/, security/, scheduler/, utils/
src/main/resources/
  templates/     Thymeleaf và fragment dùng chung
  static/        CSS, JavaScript, hình ảnh
  db/migration/sqlserver/
src/test/java/   Kiểm thử Java
src/test/js/     Kiểm thử JavaScript
scripts/        Khởi chạy, môi trường và kiểm thử
docs/           Tài liệu
sql/            Script SQL bổ trợ/cũ
```

Đếm tại thời điểm kiểm tra: 246 file Java chính, 77 template HTML, 73 file Java trong thư mục test và 13 file test JavaScript. Số file không phải thước đo điểm số.

Điểm tốt: Controller–Service–Repository được tách, có DTO, kiểm tra quyền ở backend, transaction cho nghiệp vụ quan trọng, migration có phiên bản, cấu hình test tách database thật.

Nên cải thiện: tách bớt xử lý phí giao và định tuyến vùng khỏi OrderServiceImpl; giảm sự phụ thuộc của template vào entity/JPA và OSIV; làm rõ script SQL nào là nguồn chính. Đây là cải tiến bảo trì, không phải yêu cầu phải viết lại toàn dự án.

## 2. Đối chiếu công nghệ

| Yêu cầu | Bằng chứng trong dự án | Đánh giá |
| --- | --- | --- |
| Spring Boot | pom.xml, Boot 4.1.1, Java 21, controller/service/config | Có triển khai, biên dịch và đóng gói được |
| Thymeleaf | 77 template, phân hệ khách/manager/admin, test render HTTP | Có triển khai và test render đạt |
| Bootstrap | Layout dùng Bootstrap 5.3.3, icon, form/modal/grid | Có sử dụng; chưa kiểm tra trực quan mọi kích thước màn hình |
| JPA | Entity, repository, transaction và khóa tồn kho | Có triển khai; kiểm thử nghiệp vụ trên H2 |
| SQL Server | JDBC SQL Server, Flyway, kết nối đọc RentalShop thành công | Có database thật; còn khoảng cách schema V8 so với code V10 |
| WebSocket | STOMP /ws (SockJS), /ws-chat (native), chat và thông báo, kiểm tra quyền | Có triển khai, các test chat/socket được chạy đạt |
| JWT | Login/refresh/me/logout, stateless Bearer, xoay refresh và thu hồi phiên | 13 test JWT đạt; web Thymeleaf dùng session/CSRF riêng |
| Cloudinary | Upload, lưu thông tin ảnh, kiểm tra định dạng/kích thước, xử lý dọn ảnh | Có tích hợp; test thật lên Cloudinary không được bật trong lần này |

Không cần triển khai đồng thời SQL Server, MySQL và PostgreSQL. SQL Server đáp ứng lựa chọn database trong đề bài.

Các file chính: [pom.xml](D:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/pom.xml), [JWT](D:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/config/JwtSecurityConfig.java), [WebSocket](D:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/config/WebSocketConfig.java).

## 3. Kết quả đã chạy

| Kiểm tra | Kết quả |
| --- | --- |
| Maven test toàn bộ | 620 test: 613 đạt, 1 thất bại, 0 lỗi chạy, 6 bỏ qua. BUILD FAILURE |
| JavaScript qua test-js.ps1, ngoài sandbox | 95 test: 86 đạt, 9 thất bại |
| JavaScript chạy lại tuần tự | 95 test: 87 đạt, 8 thất bại |
| Riêng test tunnel chạy lại | 5/5 đạt |
| Bộ kiểm tra nạp cấu hình Cloudinary | 10/10 đạt, chỉ dùng credential giả và Maven giả |
| Đóng gói Maven với -DskipTests | BUILD SUCCESS, tạo JAR. Không đồng nghĩa bộ test đã đạt |

Lệnh chính:

```powershell
.\scripts\maven.ps1 -MavenArguments @('test','-B')
.\scripts\test-js.ps1
$auditJsFiles = Get-ChildItem -LiteralPath src/test/js -File -Filter '*.test.cjs' | Select-Object -ExpandProperty FullName
node --test --test-concurrency=1 --test-reporter=spec @auditJsFiles
.\scripts\test-cloudinary-env.ps1
.\scripts\maven.ps1 -MavenArguments @('package','-DskipTests','-B')
```

Môi trường Java dùng JDK 21 qua script, JavaScript dùng Node 24.19.0. Báo cáo Java chi tiết nằm ở [surefire-reports](D:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/target/surefire-reports).

### Test Java thất bại

`StoreProductCatalogTests.includesOwnAndSharedProductsWithoutExposingForeignStock` đòi HTML chứa chính xác `>Sửa</a>`. Template hiện có icon và xuống dòng bên trong thẻ liên kết. HTML trả về vẫn có nút Sửa và sản phẩm của fixture.

Đây là assertion giao diện quá phụ thuộc định dạng HTML, chưa phải bằng chứng trang sản phẩm không hoạt động hoặc lộ kho chi nhánh khác. Nên kiểm tra liên kết/nhãn theo cấu trúc DOM thay vì chuỗi HTML cứng, sau đó chạy lại toàn suite.

Nguồn: [test dòng 75](D:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/test/java/com/finalproject/StoreProductCatalogTests.java:75), [template dòng 251](D:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/templates/manager/products.html:251).

### Test JavaScript thất bại

8 test trong flower-attributes.test.cjs đọc DOM theo bố cục cũ. Setup lấy trực tiếp `[tags, controls, error]`, trong khi editor mới chứa `[row, choices, error]`; row mới chứa surface và controls. Vì vậy nhiều test truy cập input không tồn tại hoặc sai phần tử. Test gợi ý cũng còn kỳ vọng thuộc tính list của datalist trong khi giao diện đã có nút chọn gợi ý riêng.

Cần cập nhật fixture và bổ sung test cho chọn có sẵn, lọc gợi ý, nhập giá trị mới, bỏ tag, giữ thuộc tính cũ và lưu form. Hiện chưa thể dùng các test thất bại này để kết luận 8 chức năng thực tế đều lỗi hoặc giao diện đã hoàn toàn đúng.

Nguồn: [fixture](D:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/test/js/flower-attributes.test.cjs:25), [DOM mới](D:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/resources/static/js/flower-attributes.js:48).

Một test tunnel timeout khi chạy cùng các test khác nhưng đạt khi chạy riêng và khi toàn suite chạy tuần tự. Cần ổn định đồng bộ/deadline của test; chưa xác nhận đây là lỗi tunnel của ứng dụng. Lần chạy sandbox ban đầu còn có lỗi fetch cục bộ, không lặp lại sau khi cho phép máy chủ test cục bộ chạy.

### Các nhóm đáng chú ý đã đạt

- BusinessRegressionTests: 122/122.
- JwtAuthenticationTests: 13/13.
- ChatBranchFlowTests: 10/10; ChatSecurityTests: 14/14; ChatSupportMonitorTests: 11/11.
- ChatSessionIntegrationTests: 1/1; ChatSocketAccessTests: 5/5.
- CatalogStockDisplayTests: 10/10; CatalogFilterTests: 9/9; BranchStockConsistencyTests: 6/6.
- FlowerAttributeViewTests: 6/6, gồm render nhóm thuộc tính khác và lựa chọn hiện có.
- AdminUsersManagementUseCaseTests: 11/11.
- SystemHttpSmokeTests: 4/4. Mỗi test có thể kiểm tra nhiều request/trang.

HTTP smoke đã kiểm tra trang chủ, danh sách/chi tiết hoa, cửa hàng, đăng nhập/đăng ký/quên mật khẩu, giỏ hàng, đơn hàng, hồ sơ, yêu thích; các màn manager về sản phẩm, thêm/sửa và các tab, đơn, nhân viên, ví, báo cáo; các màn admin về người dùng, cửa hàng, sản phẩm, đơn, danh mục, thuộc tính, giao hàng/biểu phí, khuyến mãi và chat. Có kiểm tra phân quyền 401/403 và giữ email đã nhập khi đăng nhập sai.

Đây là HTTP/render và kiểm thử nghiệp vụ, không phải kiểm tra đầy đủ mọi thao tác bằng trình duyệt hay chứng minh bố cục đẹp trên mobile.

### Sáu test bị bỏ qua

Các test live cần chủ động bật system property: một upload Cloudinary thật, một đối soát demo funds SQL Server, một catalog SQL Server, một migration catalog và hai migration SQL Server. Không bật tự động để tránh upload hoặc ghi vào database ngoài phạm vi.

## 4. Database thật và rủi ro triển khai

Đã dùng truy vấn chỉ đọc trên RentalShop: 4 cửa hàng, 15 người dùng, 40 sản phẩm, 26 đơn hàng. Không sửa/xóa/di chuyển dữ liệu, không đổi đơn #23.

### A. Migration chưa theo kịp mã nguồn

Flyway history ghi nhận thành công đến V8; code hiện có V9 và V10. Các cột mới chưa có trong database:

- stores.pending_balance
- orders.funds_status
- orders.funds_amount
- orders.commission_rate_snapshot
- transactions.wallet_bucket

V9 bổ sung phương thức MOMO_DEMO; V10 bổ sung trạng thái tiền chờ và có xử lý dữ liệu cũ. Đây không chỉ là thay đổi giao diện. Ứng dụng thường chạy Flyway khi khởi động, nhưng lần đánh giá này chưa kiểm chứng V9–V10 trên SQL Server hiện có.

Trước khi demo bản code mới: phối hợp thành viên phụ trách thanh toán, sao lưu RentalShop, chạy thử migration và kiểm tra nghiệp vụ trên bản sao. Sau khi xác nhận mới áp dụng database chính. Không tự xóa database để ứng dụng khởi động được.

Nguồn: [V10](D:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/db/migration/V10__demo_pending_funds.java:23).

### B. Tồn kho lệch chi nhánh còn tồn tại

Truy vấn nối store_inventories với products, chọn sản phẩm có store_id khác store_id của dòng kho, cho kết quả **81 dòng, tổng 2.003 đơn vị**. Không có dòng quantity âm trong snapshot đã kiểm tra.

Luồng catalog mới đã có lọc kho hợp lệ, nhưng điều đó không đồng nghĩa dữ liệu lịch sử đã được xử lý. Không nên cộng các dòng này vào tồn có thể bán, cũng không được tự chuyển/xóa khi chưa đối chiếu đơn cũ, sở hữu sản phẩm và số lượng thực tế. Giữ nguyên đơn #23 theo lựa chọn trước đó.

## 5. Lỗi logic vùng giao demo

Trong estimateDistanceKm, nhánh trung tâm kiểm tra `contains("quận 1")` trước `quận 10` và `quận 12`. Chuỗi Quận 10/Quận 12 chứa Quận 1 nên đi vào nhánh 2,5 km thay vì nhánh dự kiến tương ứng. Dạng q10 cũng khớp q1. Nhánh Quận 7 có rủi ro tương tự khi kiểm tra quận 1 trước quận 12.

Hệ quả: xác định sai vùng, chọn sai phí hoặc không từ chối đúng vùng ngoài phạm vi. Đây là lỗi được xác định từ mã nguồn, không phải từ thao tác tạo đơn thật trong đợt kiểm tra này.

Nguồn: [estimateDistanceKm](D:/LAPTRINH/LAPTRINHWEB/BAITAP/finalproject/src/main/java/com/finalproject/service/impl/OrderServiceImpl.java:387).

Vì giao hàng chỉ demo, không cần thêm bản đồ thật. Có thể chọn rõ vùng demo Gần/Xa/Ngoài phạm vi, dùng mã vùng và dịch vụ thay vì dò chữ trong tên vùng/địa chỉ. Dùng trường riêng xác định hàng cồng kềnh thay vì đoán qua tên sản phẩm. Báo phí vẫn phải nhất quán giữa checkout và đơn hàng.

## 6. Quy tắc nghiệp vụ cần trình bày đúng

- Khách được tự hủy ở PENDING hoặc CONFIRMED. Đến PREPARING đã không được tự hủy, không phải đợi đến DELIVERING mới khóa hủy.
- Quản lý/admin có đường hủy ngoại lệ khi đang chuẩn bị; phải nhập lý do và thông báo khách. Cần phân biệt với quyền tự hủy của khách.
- Chat chi nhánh bị giới hạn theo quyền; admin giám sát có nhật ký và chỉ đọc. Kênh hỗ trợ hệ thống là hội thoại riêng.
- Trang Thymeleaf dùng session/CSRF, API Bearer có JWT. Không nên trình bày rằng mọi lần đăng nhập web đều dùng JWT.
- Thanh toán demo không chứng minh đã tích hợp thanh toán thật. Phần do thành viên khác phụ trách cần có checklist bàn giao riêng, không bắt buộc người làm catalog/chat tự viết lại.
- KPI tồn kho cập nhật qua polling không phải mặc nhiên là WebSocket. Bằng chứng WebSocket nên dùng chat/thông báo không tải lại trang.

## 7. Đối chiếu rubric

Nguồn: [rubric được cung cấp](D:/Downloads/LTWEB_CuoiKy(Rubric)_5tieuchi_ST2_ST4_HK120262027.xlsx), sheet `Nhóm...(mẫu)`, vùng A11:E25.

| Tiêu chí | Trọng số | Đánh giá theo bằng chứng hiện có |
| --- | --- | --- |
| Độ khó, tính thực tiễn, phân công | 10% | Mô hình nhiều chi nhánh, tồn kho/đơn/chat phù hợp. Cần bảng phân công theo thành viên và module để chứng minh phần phân công |
| Sử dụng AI và cộng tác GitHub | 20% | Có tài liệu prompt và lịch sử local Git từ 3 tác giả. Số commit không chứng minh mức đóng góp hoặc làm chủ code; từng người cần giải thích/sửa được phần mình phụ trách |
| Cấu trúc dữ liệu, vai trò, giao diện | 20% | Phân lớp và vai trò có cơ sở; test truy cập/phân quyền đạt. Còn dữ liệu kho lệch và cần kiểm tra giao diện thực tế sau các thay đổi thuộc tính |
| Chất lượng code, cài đặt, chức năng | 35% | Bộ test rộng và đóng gói được. Chưa nên tự chấm tối đa khi còn test thất bại và chưa kiểm chứng migration mới trên SQL Server |
| Sáng tạo ngoài chương trình | 15% | Có các phần có thể trình bày như nhật ký giám sát chat, xử lý cạnh tranh tồn kho, bảo toàn ảnh khi rollback. Cần đối chiếu với nội dung đã học; không tự tính công nghệ bắt buộc là sáng tạo ngoài chương trình |

Không chốt điểm tổng từ mã nguồn vì còn tiêu chí thuyết trình, làm chủ code, phân công và phạm vi kiến thức đã học. Tài liệu prompt tổng hợp không tự chứng minh đó là toàn bộ lịch sử tương tác thực tế.

## 8. Tài liệu và kiểm chứng còn thiếu

- README đang trỏ đến 6 file không còn tồn tại: CHAT_CUSTOMER_ENTRY_20261013.md, CHAT_SUPPORT_MONITOR_20261013.md, OTP_EMAIL_SETUP_20261012.md, REQUIREMENTS_AUDIT_20261012.md, SQLSERVER_REPAIR_20261012.md, SYSTEM_REVIEW_20261012.md trong docs.
- Tài liệu tổng hợp còn ghi 552 Java test và 65/65 JavaScript test đạt. Không còn đúng với kết quả hiện tại. Một số dòng tự nhận “ĐẠT 100%” cũng cần thay bằng trạng thái và bằng chứng cụ thể.
- Chưa có GitHub Actions trong cây file được theo dõi. Nên tự động chạy Java/JS test mỗi lần push/PR sau khi ổn định suite.
- CSS/JS/font dùng CDN: chuẩn bị mạng khi demo hoặc bản dự phòng cục bộ, tránh nhầm lỗi mất mạng với lỗi ứng dụng.
- .env không được Git theo dõi tại thời điểm kiểm tra. Chưa kiểm toán toàn bộ lịch sử Git hoặc xác nhận các khóa từng lộ đã được thu hồi.

Tại lúc kiểm tra không có ứng dụng lắng nghe cổng 8080. Không khởi động profile thật vì có migration đang chờ. Chưa thử lại OTP vào hộp thư, upload Cloudinary thật, thanh toán thật, tải cao, nhiều trình duyệt và mọi breakpoint màn hình. Các lần người dùng đã xác nhận demo OTP/Cloudinary thành công là bằng chứng trước đây, không được tính là test live mới trong báo cáo này.

## 9. Thứ tự hoàn thiện đề xuất

1. Phối hợp người phụ trách thanh toán kiểm tra V9–V10 trên bản sao SQL Server, rồi quyết định áp dụng sau sao lưu.
2. Cập nhật các test bị lệch cấu trúc giao diện, ổn định test tunnel và chạy lại cả Java lẫn JavaScript. Không bỏ assertion chỉ để làm xanh test.
3. Kiểm tra trực tiếp form thuộc tính: dữ liệu cũ, chọn gợi ý, nhập nhiều giá trị mới, xóa tag, lưu và mở lại, cả admin/manager.
4. Sửa vùng giao demo để hết nhận nhầm Quận 1/10/12 và kiểm tra phí theo chi nhánh/dịch vụ.
5. Đối soát 81 dòng kho bằng báo cáo riêng. Chỉ chuyển/xóa sau khi xác nhận phương án, không làm ảnh hưởng đơn cũ.
6. Cập nhật README, số liệu test, bảng phân công và bằng chứng demo theo rubric. Chạy thử bản cuối trên SQL Server với ít nhất hai phiên người dùng.

Sau các bước này mới nên chốt bộ source dùng để nộp và quay video demo.

## Cập nhật ngày 19/10/2026

- Đã sửa test kiểm tra nút quản lý sản phẩm theo liên kết thực tế, cập nhật fixture thuộc tính hoa và ổn định chờ gateway; thêm test vùng giao demo và trường hợp hủy đơn cũ không hoàn kho nhân bản. Lượt chạy cuối: Java 622 ca (616 đạt, 0 thất bại, 0 lỗi, 6 bỏ qua có chủ đích); JavaScript 102/102 đạt. Các test cần socket localhost được cấp quyền cục bộ để chạy.
- Đã bỏ suy đoán vùng giao từ địa chỉ tự do. Người mua chọn vùng gần/xa/ngoài phạm vi; API quote và checkout dùng cùng vùng để chọn biểu phí, từ chối vùng ngoài phạm vi hoặc không có biểu phí. Đây là mô phỏng, **không phải khoảng cách bản đồ**.
- Đã sao lưu `RentalShop` bằng COPY_ONLY/CHECKSUM, RESTORE VERIFYONLY và tạo bản sao `CodexCatalogCopy_20261019_0740`. V9–V10 cùng hai test xác thực schema/catalog đạt trên bản sao. Kiểm tra lại: database gốc V8, bản sao V10; cả hai đều có 26 đơn, 154 dòng kho, 3.339 đơn vị, 21 giao dịch. Bản sao hiện có 0 đơn `MOMO_DEMO` nên chưa kiểm chứng backfill với dữ liệu thanh toán cũ. Chưa chạy V9–V10 trên database gốc do cần xác nhận với thành viên phụ trách thanh toán.
- Đã đối soát chỉ đọc 81 dòng kho lệch / 2.003 đơn vị; 4 dòng liên quan đơn, 3 dòng gắn đơn chưa kết thúc. Chi tiết ở [báo cáo đối soát](DOI_SOAT_KHO_LECH_CHI_NHANH_20261019.md). Không chuyển/xóa kho, không sửa đơn #23.
- README đã thay sáu liên kết gãy bằng tài liệu hiện có; tài liệu tổng hợp bỏ số test cũ và nhận định “đạt 100%”.

### Sau khi người dùng yêu cầu tự hoàn tất

- Đã tạo và kiểm tra bản sao lưu mới `RentalShop_before_v10_20261019_0800.bak`, sau đó áp dụng V9–V10 lên `RentalShop`. Flyway báo thành công; số đơn, dòng kho, tổng lượng tồn và giao dịch không thay đổi. Không có đơn `MOMO_DEMO` cũ để backfill.
- Website đã khởi động với profile `dev` trên SQL Server V10. Kiểm tra GET cục bộ: trang chủ, trang sản phẩm và đăng nhập trả 200; trang manager/admin chuyển hướng đăng nhập 302 khi chưa xác thực. Đã dừng máy chủ thử nghiệm sau khi kiểm tra.
- 81 dòng kho lệch vẫn được giữ nguyên, không tự gán là hàng có thể bán hoặc xóa; cần kiểm kê vật lý/chứng từ để quyết định xử lý số lượng. Mã nguồn hiện loại các dòng lệch khỏi tồn kho bán được.
- Đã sửa kết nối mặc định `dev` khỏi cổng SQL Server cũ và bổ sung bài kiểm tra sổ số dư chỉ đọc với kết nối `RentalShop` được chỉ định rõ. Kiểm tra này đạt trên V10; chạy lại bằng `scripts/audit-demo-funds-sqlserver.ps1`. Truy vấn lại sau migration xác nhận 81 dòng / 2.003 đơn vị lệch, tổng 154 dòng / 3.339 đơn vị và không có tồn kho âm.
- Người dùng xác nhận 81 dòng là dữ liệu nhân bản. Diễn tập trên bản sao V10 đã lưu giá trị gốc vào bảng đối soát, đưa 81 dòng về 0/ngừng bán/xóa mềm; bản sao còn 1.336 đơn vị, đơn hàng không đổi. Việc áp dụng tương tự lên `RentalShop` thật bị chặn chờ phê duyệt riêng vì thay đổi 2.003 đơn vị; database thật chưa bị sửa ở bước này.
