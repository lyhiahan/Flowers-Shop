# Prompt suy diễn từ lịch sử commit của quocanhug

**Nhãn nguồn: TÁI DỰNG / SUY DIỄN — KHÔNG PHẢI PROMPT CHAT GỐC.**

Tạo ngày 28/10/2026, từ lịch sử đến `33d489c`. Lọc đúng trường Git author name bằng `quocanhug` theo cấu hình user.name của checkout. Không lấy commit tác giả khác hoặc merge làm đóng góp riêng. Tên tác giả Git chưa đủ chứng minh danh tính người trực tiếp gõ prompt/code; nhóm phải xác nhận nếu dùng làm phân công.

Có 23 commit không phải merge được chọn. Mỗi đoạn dưới đây mô tả một yêu cầu **có thể dẫn tới** thay đổi trong commit, dựa trên tiêu đề và danh sách file thay đổi. Không khẳng định đoạn đó đã được gửi, được gửi lúc nào, cho model nào, AI đã viết bao nhiêu code, hoặc test đã chạy thành công. Các yêu cầu kiểm thử trong đoạn prompt là yêu cầu đề xuất, không phải bằng chứng kết quả.

Một commit có thể gộp nhiều yêu cầu và một yêu cầu có thể đi qua nhiều commit. Các commit hoàn tác được giữ để phản ánh đúng quá trình; nội dung tái dựng mô tả mục tiêu ở thời điểm commit, không tự thay thế quy định vai trò/kiến trúc hiện tại. Tên đường dẫn dưới đây là đường dẫn tại commit; file có thể đã đổi tên hoặc bị dọn về sau.

## Cách đưa vào hồ sơ AI

Dùng tiêu đề phụ lục “Prompt tái dựng từ lịch sử phát triển”. Giữ cột nguồn là commit và mức xác thực là suy diễn. Khi tìm được chat gốc, dẫn sang minh chứng gốc riêng; không đổi nhãn suy diễn thành nguyên văn. Đối chiếu bằng `git show <hash>` hoặc `git show <hash>:<path>`. Không dùng tài liệu này để chứng minh chính xác model hay mức làm chủ code.

## Prompt suy diễn 01 — 15ea348

- Commit: `15ea34867c114436dbc81aebdf99108372f3418c`; ngày author trong Git: `2026-10-03`.
- Tiêu đề gốc: feat: integrate MoMo payment, session-based auth, order management workflows, and notification system
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy tích hợp thanh toán MoMo, xác thực session cho các trang Thymeleaf, quy trình xử lý đơn hàng và thông báo. Nối controller, service và các trang checkout/lịch sử đơn, kiểm tra quyền người thao tác và bổ sung kiểm thử cho các tình huống đặt hàng cạnh tranh.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/MomoConfig.java`
- `src/main/java/com/finalproject/config/SecurityConfig.java`
- `src/main/java/com/finalproject/controller/common/NotificationController.java`
- `src/test/java/com/finalproject/ConcurrencyRegressionTests.java`

## Prompt suy diễn 02 — cce1bf4

- Commit: `cce1bf47384f6b5f1d52aa721e7fd77e402caa6b`; ngày author trong Git: `2026-10-03`.
- Tiêu đề gốc: fix: support unicode category name and set sql port 1433
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy sửa lưu và hiển thị tên danh mục tiếng Việt trong SQL Server, đồng thời cấu hình kết nối cổng 1433. Kiểm tra ánh xạ entity Category và cấu hình datasource; giữ nguyên nội dung Unicode của dữ liệu.

Các file đối chiếu:

- `src/main/java/com/finalproject/entity/Category.java`

## Prompt suy diễn 03 — fe3865a

- Commit: `fe3865a82bad656619bb8bb4cb3e2f0ea5be6826`; ngày author trong Git: `2026-10-04`.
- Tiêu đề gốc: feat: complete flower shop project with UI fixes, MoMo integration, order workflows, and test suite
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy rà soát và hoàn thiện website cửa hàng hoa: sửa giao diện/Unicode, đồng bộ checkout và báo giá, thanh toán MoMo, trạng thái đơn và thông báo. Bổ sung kiểm thử backend/frontend, ghi rõ phần đã chạy và phần chưa xác minh thay vì coi chỉ mở được trang là hoàn tất nghiệp vụ.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/DataInitializer.java`
- `src/main/java/com/finalproject/config/MomoConfig.java`
- `src/main/java/com/finalproject/config/SecurityConfig.java`
- `src/test/java/com/finalproject/BusinessRegressionTests.java`

## Prompt suy diễn 04 — 07e04d6

- Commit: `07e04d602ccfa37132c941ae069e0d4d489b7853`; ngày author trong Git: `2026-10-04`.
- Tiêu đề gốc: fix: harden checkout, inventory and payment workflows
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy củng cố checkout, tồn kho và thanh toán: kiểm tra request phía server, xử lý callback/thử lại, quản lý lần thanh toán và trạng thái đơn nhất quán. Bổ sung hướng dẫn chuyển khoản và xử lý lỗi/redirect phù hợp; viết test cho các tình huống bất lợi và đồng thời.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/BankTransferConfig.java`
- `src/main/java/com/finalproject/config/MomoConfig.java`
- `src/main/java/com/finalproject/config/SecurityConfig.java`
- `src/test/java/com/finalproject/BusinessRegressionTests.java`

## Prompt suy diễn 05 — 7ff352f

- Commit: `7ff352f0b2854bb6c3366afa8fff9cf182189930`; ngày author trong Git: `2026-10-04`.
- Tiêu đề gốc: feat: merge user updates, retire shipper role, streamline roles and orders, and configure sql server connection
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy hợp nhất cập nhật người dùng, tinh gọn vai trò và quy trình đơn hàng theo phạm vi cửa hàng hoa; loại vai trò giao hàng riêng khỏi luồng đang dùng. Đồng bộ cấu hình SQL Server và các kiểm tra quyền liên quan, bảo đảm dữ liệu và luồng đã có tiếp tục hoạt động.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/CustomAuthenticationSuccessHandler.java`
- `src/main/java/com/finalproject/config/DataInitializer.java`
- `src/main/java/com/finalproject/config/RetiredDeliveryRoleCleanup.java`
- `src/test/java/com/finalproject/AdminCategoryManagementUseCaseTests.java`

## Prompt suy diễn 06 — 9db656d

- Commit: `9db656d00d9feffa84b6d36da7d609d94ccb2304`; ngày author trong Git: `2026-10-04`.
- Tiêu đề gốc: feat: add khachhang2 customer test account to DataInitializer
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy bổ sung một tài khoản khách hàng thử nghiệm thứ hai vào DataInitializer để có thể kiểm tra tình huống nhiều người dùng. Tránh tạo trùng khi initializer chạy lại và không coi tài khoản demo là tài khoản thực tế.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/DataInitializer.java`

## Prompt suy diễn 07 — bc82f7c

- Commit: `bc82f7c6a5711e14baaecae3c413776b8159084f`; ngày author trong Git: `2026-10-04`.
- Tiêu đề gốc: fix: harden account refunds and enable Cloudinary uploads
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy tăng độ an toàn khi đóng tài khoản và hoàn tiền, kiểm tra trạng thái người dùng trong phiên đăng nhập. Bổ sung upload Cloudinary cho các thao tác quản lý sản phẩm và lưu thông tin ảnh đúng cách; bổ sung kiểm thử quyền, hoàn tiền và upload thất bại.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/DataInitializer.java`
- `src/main/java/com/finalproject/controller/admin/AdminOrderManagementController.java`
- `src/main/java/com/finalproject/controller/admin/AdminProductController.java`
- `src/test/java/com/finalproject/AdminCategoryManagementUseCaseTests.java`

## Prompt suy diễn 08 — 721ff4f

- Commit: `721ff4f1b9b8dc263635e7126bcfda2a8ac62384`; ngày author trong Git: `2026-10-04`.
- Tiêu đề gốc: feat: hoàn thiện use case quản lý cửa hàng UC_017-UC_026, đồng bộ giao diện và form rút tiền
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy đối chiếu và hoàn thiện các use case quản lý cửa hàng UC_017–UC_026: tạo/quản lý cửa hàng, nhân sự, sản phẩm, đơn hàng, doanh thu và ví. Kiểm tra phạm vi chi nhánh bằng service, đồng bộ bố cục/form rút tiền và ghi bằng chứng kiểm thử theo từng use case.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/DataInitializer.java`
- `src/main/java/com/finalproject/config/StoreTimeConfig.java`
- `src/main/java/com/finalproject/controller/admin/AdminDashboardController.java`
- `src/test/java/com/finalproject/BusinessRegressionTests.java`

## Prompt suy diễn 09 — 4bf8560

- Commit: `4bf85603e8cfc86b06ccb58352b5bf5fe00b58d1`; ngày author trong Git: `2026-10-04`.
- Tiêu đề gốc: fix: chuẩn hóa tiêu đề tìm kiếm, nhãn bộ lọc và id lưới sản phẩm catalog
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy chuẩn hóa tiêu đề tìm kiếm, nhãn bộ lọc và ID lưới sản phẩm trong catalog. Giữ nhất quán giữa template và JavaScript để tìm kiếm/lọc không bị lỗi do đổi tên DOM.

Các file đối chiếu:

- `src/main/java/com/finalproject/controller/customer/HomeController.java`
- `src/main/resources/templates/index.html`

## Prompt suy diễn 10 — 8d1ddc8

- Commit: `8d1ddc8504069e0704dc6a0008812ecd8cf26ef5`; ngày author trong Git: `2026-10-04`.
- Tiêu đề gốc: feat: đồng bộ toàn diện bố cục frontend, ui và thanh sidebar dùng chung cho các trang /manager/
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy đồng bộ bố cục frontend và sidebar dùng chung cho các trang quản lý. Giữ trạng thái mục đang chọn, phông chữ và khoảng cách thống nhất khi chuyển giữa các trang con.

Các file đối chiếu:

- `src/main/java/com/finalproject/controller/manager/ManagerLayoutAdvice.java`
- `src/main/resources/templates/fragments/manager-layout.html`
- `src/main/resources/templates/index.html`
- `src/test/java/com/finalproject/StoreUseCaseAuditTests.java`

## Prompt suy diễn 11 — 62a8a48

- Commit: `62a8a48a0c5cd957767f68aaa1889d78cd320527`; ngày author trong Git: `2026-10-04`.
- Tiêu đề gốc: revert: khôi phục lại toàn bộ giao diện, sidebar và nền gốc ban đầu của các trang /manager/
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy hoàn tác thay đổi giao diện quản lý vừa thực hiện và khôi phục giao diện, sidebar, màu nền của bản trước. Kiểm tra các trang quản lý sau hoàn tác để không giữ lại phần bố cục không được chấp nhận.

Các file đối chiếu:

- `src/main/java/com/finalproject/controller/manager/ManagerLayoutAdvice.java`
- `src/main/resources/templates/fragments/manager-layout.html`
- `src/main/resources/templates/manager/analytics.html`
- `src/test/java/com/finalproject/StoreUseCaseAuditTests.java`

Lưu ý: đây là bước điều chỉnh/hoàn tác thiết kế trước đó, không phải một tính năng độc lập mới.

## Prompt suy diễn 12 — 2284d60

- Commit: `2284d604c1b28391a05630cd5ad6503ec7081ee5`; ngày author trong Git: `2026-10-04`.
- Tiêu đề gốc: feat: giữ nguyên thanh sidebar đã đồng bộ và khôi phục màu nền kem hồng ấm #FFF0F3 gốc cho các trang /manager/
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy giữ phần sidebar đã đồng bộ nhưng khôi phục màu nền kem hồng #FFF0F3 cho các trang quản lý. Chỉ điều chỉnh phần bố cục/màu có liên quan, tránh làm mất chức năng của từng trang.

Các file đối chiếu:

- `src/main/java/com/finalproject/controller/manager/ManagerLayoutAdvice.java`
- `src/main/resources/templates/fragments/manager-layout.html`
- `src/main/resources/templates/manager/analytics.html`
- `src/test/java/com/finalproject/StoreUseCaseAuditTests.java`

Lưu ý: đây là bước điều chỉnh/hoàn tác thiết kế trước đó, không phải một tính năng độc lập mới.

## Prompt suy diễn 13 — 9351694

- Commit: `9351694a41cb1db1643de2aadb146b7f2c8f78d1`; ngày author trong Git: `2026-10-04`.
- Tiêu đề gốc: feat: nâng cấp hệ thống thông báo realtime, đồng bộ layout quản lý và chuông header với badge đếm số
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy nâng cấp thông báo realtime và đồng bộ chuông trên header khu vực quản lý, gồm badge đếm thông báo. Kiểm tra nội dung, số chưa đọc và trạng thái giao diện sau khi có sự kiện mới hoặc người dùng đọc thông báo.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/SecurityConfig.java`
- `src/main/java/com/finalproject/config/WebSocketConfig.java`
- `src/main/java/com/finalproject/controller/common/NotificationController.java`
- `src/test/java/com/finalproject/NotificationServiceAndControllerTests.java`

## Prompt suy diễn 14 — 0322611

- Commit: `0322611b423f2336d10b8bea98d65fc22ec5a349`; ngày author trong Git: `2026-10-04`.
- Tiêu đề gốc: fix: kết nối trực tiếp cổng 1433 của SQL Server tránh lỗi timeout SQLEXPRESS
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy sửa kết nối SQL Server bằng địa chỉ/cổng trực tiếp 1433 để tránh cấu hình instance gây timeout. Không ghi mật khẩu vào mã nguồn hoặc log; xác minh kết nối và nêu rõ cấu hình máy phải cung cấp.

Các file đối chiếu:

- `src/main/resources/application.properties`

## Prompt suy diễn 15 — 0857e57

- Commit: `0857e5736394f8ccbfa404810b11dcb419b49338`; ngày author trong Git: `2026-10-05`.
- Tiêu đề gốc: feat: khoi tao bo du lieu mau toan dien cho vai tro VENDOR (nhan vien, don hang, khuyen mai, vi cua hang va doanh thu)
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy bổ sung dữ liệu demo cho không gian vận hành cửa hàng ở phiên bản hiện tại, gồm nhân sự, đơn, khuyến mãi, ví và doanh thu. Giữ quan hệ và trạng thái dữ liệu nhất quán, tránh nhân bản khi chạy lại và ghi rõ đây là dữ liệu mẫu.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/DataInitializer.java`
- `src/main/java/com/finalproject/service/VendorDataSeederService.java`

## Prompt suy diễn 16 — 1acc601

- Commit: `1acc601b5d79d4c2f134b32dc9021d356f6d7f55`; ngày author trong Git: `2026-10-05`.
- Tiêu đề gốc: feat: consolidate roles and add secure branch chat with restored checkout safeguards
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy hợp nhất quy tắc vai trò, triển khai chat theo chi nhánh có kiểm soát truy cập và khôi phục các kiểm tra an toàn tại checkout. Kiểm tra người được tham gia hội thoại và phạm vi dữ liệu, đồng thời giữ các ràng buộc giỏ/kho/thanh toán.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/AdminRoleSanitizer.java`
- `src/main/java/com/finalproject/config/BankTransferConfig.java`
- `src/main/java/com/finalproject/config/CustomAuthenticationSuccessHandler.java`
- `src/test/java/com/finalproject/AdminCategoryManagementUseCaseTests.java`

## Prompt suy diễn 17 — 450ce39

- Commit: `450ce397679ab8986d9be6c33791e1b7f4cc9cf9`; ngày author trong Git: `2026-10-07`.
- Tiêu đề gốc: fix: su ly role migration, payment expiry, danh muc, quyen phien dang nhap va cac loi lien quan
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy sửa các lỗi chuyển đổi vai trò, quyền của phiên đăng nhập, thời hạn thanh toán và danh mục. Đồng bộ kiểm tra giữa cấu hình, controller/service và dữ liệu lịch sử; bổ sung hồi quy cho các lỗi vừa phát hiện.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/DataInitializer.java`
- `src/main/java/com/finalproject/config/OrderExpirySchedule.java`
- `src/main/java/com/finalproject/config/PaymentSchemaMigration.java`
- `src/test/java/com/finalproject/BusinessRegressionTests.java`

## Prompt suy diễn 18 — e8d0966

- Commit: `e8d0966fb89b533f5fca756280790725e4705fef`; ngày author trong Git: `2026-10-08`.
- Tiêu đề gốc: fix: stabilize chat and provision accounts with fixed roles
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy ổn định chat và việc cấp tài khoản theo vai trò cố định. Kiểm tra các thao tác tạo tài khoản, quyền hiện hành và truy cập hội thoại để không cấp nhầm vai trò hoặc cho đọc hội thoại trái phép.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/ChatSchemaMigration.java`
- `src/main/java/com/finalproject/config/WebSocketConfig.java`
- `src/main/java/com/finalproject/controller/admin/AdminDashboardController.java`
- `src/test/java/com/finalproject/BusinessRegressionTests.java`

## Prompt suy diễn 19 — b177ed1

- Commit: `b177ed105f2582308c013f297e19b5029b3bae79`; ngày author trong Git: `2026-10-22`.
- Tiêu đề gốc: Khắc phục thông báo, đồng bộ giao diện quản trị và bảo vệ tài khoản
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy sửa thông báo và đồng bộ giao diện quản trị, đồng thời bảo vệ thông tin tài khoản. Rà trang người dùng, dữ liệu công khai và kiểm tra quyền; bổ sung test cho lỗi thông báo, bố cục và dữ liệu không được phép lộ.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/BankTransferConfig.java`
- `src/main/java/com/finalproject/config/MomoConfig.java`
- `src/main/java/com/finalproject/controller/common/NotificationController.java`
- `src/test/java/com/finalproject/AdminUiRegressionTests.java`

## Prompt suy diễn 20 — b9a315a

- Commit: `b9a315a05889541cf8dc1b531d5157ba10e9c884`; ngày author trong Git: `2026-10-27`.
- Tiêu đề gốc: feat: thêm lọc sản phẩm theo chi nhánh và đồng bộ thanh bộ lọc
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy bổ sung lọc sản phẩm theo chi nhánh và đồng bộ với các bộ lọc hiện có. Giữ lựa chọn qua tìm kiếm, sắp xếp, phân trang và thao tác xóa bộ lọc; kiểm tra hiển thị theo tồn/trạng thái chi nhánh và thêm hồi quy.

Các file đối chiếu:

- `src/main/java/com/finalproject/controller/customer/HomeController.java`
- `src/main/java/com/finalproject/repository/ProductSpecification.java`
- `src/main/resources/templates/fragments/floral.html`
- `src/test/java/com/finalproject/CatalogFilterTests.java`

## Prompt suy diễn 21 — 0d8fa2e

- Commit: `0d8fa2ef3eb1816f686db210cd5a46028ce4f227`; ngày author trong Git: `2026-10-27`.
- Tiêu đề gốc: fix: đưa lọc chi nhánh vào bảng tất cả bộ lọc
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy chuyển bộ lọc chi nhánh vào bảng tất cả bộ lọc. Quản lý lựa chọn nháp: đóng không áp dụng thì bỏ nháp, chỉ ghi URL khi bấm Áp dụng; đồng bộ reset/xóa bộ lọc và kiểm tra cả frontend lẫn backend.

Các file đối chiếu:

- `src/main/java/com/finalproject/controller/customer/HomeController.java`
- `src/main/resources/templates/index.html`
- `src/test/js/catalog-store-filter.test.cjs`

## Prompt suy diễn 22 — 0cd6f52

- Commit: `0cd6f52d6ac3cf72a576163335462f1b0b2f7519`; ngày author trong Git: `2026-10-28`.
- Tiêu đề gốc: feat: tach biet quyen mua hang cua quan ly, sua loi voucher quote va toi uu thanh toan local
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy bỏ quyền mua của MANAGER trên server, session cũ, JWT và UI nhưng giữ nghiệp vụ cửa hàng. Đồng thời sửa báo giá voucher và cải thiện luồng thanh toán local theo thay đổi trong commit. Không xóa dữ liệu hoặc sửa migration đã áp dụng; kiểm tra quyền các vai trò và hồi quy đặt hàng/thanh toán.

Các file đối chiếu:

- `src/main/java/com/finalproject/config/SecurityConfig.java`
- `src/main/java/com/finalproject/controller/admin/AdminVoucherController.java`
- `src/main/java/com/finalproject/controller/customer/CheckoutController.java`
- `src/test/java/com/finalproject/AdminCustomerRoleSeparationTests.java`

## Prompt suy diễn 23 — fe67eec

- Commit: `fe67eec33e4e9d8d9587dcdaf2c08db06b592983`; ngày author trong Git: `2026-10-28`.
- Tiêu đề gốc: feat: bo sung database dump full postgresql va sqlserver kem huong dan import va datasheet ai
- Căn cứ: tiêu đề và các file thay đổi; suy diễn mục tiêu, không phải trích lịch sử chat.

> Hãy chuẩn bị SQL schema và dữ liệu demo cho PostgreSQL/SQL Server, kèm hướng dẫn import và datasheet AI để nộp đồ án. Ghi rõ cấu hình đích, yêu cầu chạy thử trên database riêng và phần nào đã xác minh; không đưa secret hoặc nhận dữ liệu mẫu là giao dịch thật.

Các file đối chiếu:

- `database/HUONG_DAN_IMPORT_DATABASE.md`
- `database/RentalShop_PostgreSQL_Full.sql`
- `database/RentalShop_SQLServer_Full.sql`
- `database/datasheet_ai_prompts.csv`
