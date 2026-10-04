# Implementation plan — hoàn thiện UC_017–UC_026

Ngày: 04/10/2026. Trạng thái: đã triển khai các luồng chính trên nhánh `codex/complete-store-use-cases`; xem báo cáo nghiệm thu `UC_017_026_IMPLEMENTATION_RESULTS.md`. Checklist chỉ đánh dấu các hạng mục đã hoàn thành đúng nội dung; các mục kết hợp nhiều yêu cầu vẫn để mở nếu còn phần cần kiểm chứng.

## 1. Baseline sau đồng bộ

- Repo: FlowerShop; nhánh `main`; remote `origin/main`.
- Đã fetch và fast-forward từ `12acff6` lên `f33474f`, nhận 6 commit mới; không cần merge commit, không có conflict.
- Đã khôi phục 5 file local modified cùng báo cáo/test audit chưa commit. Stash dự phòng có nhãn `codex-pre-sync-uc017-026-20261004`; không xóa bản dự phòng trong lượt này.
- Báo cáo trước đồng bộ: `docs/UC_017_026_AUDIT_20261004.md`. Các con số test trong báo cáo đó là lịch sử, không dùng làm kết quả của HEAD mới.
- Thay đổi mới cần bảo toàn: upload ảnh sản phẩm bằng Cloudinary, active category, luồng khóa/xóa tài khoản và refund, modal chung, giao diện/payment mới.
- `ManagerDashboardController` mới đã loại việc trừ REFUNDED lần thứ hai và nhận diện REFUND_REQUESTED có ORDER_PAYMENT. Hạng mục này chuyển từ “sửa lỗi đã thấy trong mã cũ” sang “giữ hành vi mới và kiểm tra thống nhất số liệu”.
- Import `global-modal.js` không tự thêm confirmation vào form chưa gọi confirm hoặc API modal. UC_019/UC_021 vẫn phải nối luồng xác nhận cụ thể.
- Clean full test sau sync: **216 ca, 193 đạt, 22 failure, 0 error, 1 skip**. Cả 6 lỗi audit hợp đồng vẫn tái hiện. JSON có HEAD và từng suite: `docs/UC_017_026_POST_SYNC_TEST_RESULTS_20261004.json`; log: `target/uc-post-sync-clean-test.log`.
- Frontend checker: 62 template, 7 vấn đề được báo; checkout quote đạt; initializer Admin vẫn lỗi canvas `adminStoresChart` bị thiếu. Một lần test incremental trước clean không nạp được test class và chạy 0 ca; không dùng lần đó làm baseline.

## 2. Kết quả cần đạt

1. Hoàn thiện main flow, alternative flow, precondition và error handling của cả 10 UC.
2. Owner/partner chỉ xử lý dữ liệu cửa hàng được cấp quyền; chỉ owner rút tiền và thay đổi nhân sự. Admin ban/license và thống kê toàn hệ thống.
3. Submit chỉ chuẩn bị/xác nhận; Confirm mới phát mutation. Cancel không phát request mutation, không làm thay đổi DB, số dư, kho hay ảnh.
4. Danh sách hỗ trợ Next/Previous/số trang, paging tại DB, query có thứ tự ổn định.
5. Backend và UI dùng cùng hợp đồng thống kê; biểu đồ line/bar/pie, hour/day/month/year và top đúng dữ liệu.
6. Không phá luồng checkout, tồn kho, payment/refund, khóa tài khoản và upload ảnh vừa merge.

## 3. Quyết định thiết kế dùng để triển khai

### 3.1 Quan hệ cửa hàng và cửa hàng đang chọn

`User.store` hiện gộp assignment với current shop; việc chuyển dashboard ghi vào field này. Tách hai khái niệm để hoàn thiện yêu cầu nhiều cửa hàng sở hữu/hợp tác:

- Giữ `Store.owner` làm nguồn xác định chủ sở hữu; không tạo bản sao owner trong membership.
- Thêm `StoreMembership(storeId, userId, accessRole, active, joinedAt)` với unique `(storeId,userId)`, accessRole `PARTNER` hoặc `STAFF`.
- Owner có toàn quyền trong shop; PARTNER được quản lý hồ sơ/sản phẩm/đơn/xem ví/thống kê và tự rời shop; STAFF chỉ portal staff/những chức năng được cấp, không tự động có quyền manager.
- Current store dùng session attribute qua `StoreContextService`, không ghi vào User.store khi bấm switch. Mỗi request xác minh quyền lại, không tin storeId gửi từ client hoặc session cũ.
- Giữ User.store cho tương thích assignment staff trong giai đoạn chuyển đổi, nhưng mọi truy cập quản lý phải đi qua service chung. Update toàn bộ manager controllers, InventoryService, WalletService, StaffService và portal staff có liên quan, bao gồm voucher/analytics ngoài 10 UC để tránh mỗi trang chọn một shop khác nhau.
- Remove membership không thu hồi role toàn hệ thống nếu tài khoản còn ownership/membership khác. Không tự chuyển VENDOR/ADMIN thành STAFF vì một request thêm nhân sự.

### 3.2 Trạng thái cửa hàng/sản phẩm

- `Store.status`: được cấp phép; `Store.isOpen`: lựa chọn mở cửa của chủ shop.
- `Product.isActive`: trạng thái duyệt sản phẩm; `isSelling`: lựa chọn Selling/Stored; `isDeleted`: soft-delete.
- Hiển thị/mua sản phẩm yêu cầu store được cấp phép, đang mở, product active, selling và không deleted, cộng các điều kiện stock hiện có.
- Ban/license đổi trạng thái cấp phép store; không ghi đè ý định isOpen/isSelling hay hồi sinh sản phẩm deleted/chưa duyệt. Sản phẩm “bị cấm cùng shop” được thực thi bằng điều kiện eligibility ở catalog/detail/cart/checkout/inventory public.
- Theo thiết kế này, test ban cũ chỉ assert `Product.isActive=false` phải được thay bằng assertion về khả năng hiển thị/mua bị chặn và trạng thái duyệt/selling được bảo toàn. Đây là thay đổi mô hình trạng thái có chủ đích; chỉ đổi test sau khi đã có integration test chứng minh sản phẩm thực sự bị cấm cùng shop.
- Dữ liệu đã bị code cũ đặt tất cả cờ false/true không cho phép khôi phục chính xác lựa chọn lịch sử. Migration không đoán trạng thái cũ; giữ trạng thái hiện hữu, lập danh sách cần owner/Admin rà soát. Cần thêm dữ liệu mẫu chứng minh Ban rồi License giữ nguyên Stored.

### 3.3 Thống kê

- Dùng cùng `AnalyticsService` cho vendor/admin; truyền scope store có quyền hoặc scope system chỉ cho Admin. Chuẩn hóa enums `criteria=revenue|orders|products`, `chartType=line|bar|pie`, `timeframe=hour|day|month|year`.
- Một `Clock` inject được và zone `Asia/Bangkok`; hour=24 giờ của ngày hiện tại, day=7 ngày gần nhất, month=12 tháng năm hiện tại, year=5 năm gần nhất. Labels phải nêu đúng khoảng, không gọi 12 tháng là “Tháng này”.
- User có timestamp tạo mới `createdAt`; giữ createdDate để tương thích. Bản ghi cũ chỉ có ngày được gắn nguồn độ chính xác DATE_ONLY, không hiển thị như đăng ký thật lúc 00:00 trên hourly chart.
- KPI đếm toàn bộ scope; KPI doanh thu dùng toàn bộ đơn hợp lệ trong khoảng đã chọn. Dataset chart theo UC giới hạn tối đa 1000 mẫu; UI ghi rõ giới hạn, không khiến người xem nhầm tổng chart là doanh thu toàn bộ khi có >1000 đơn.
- Đơn mới nhất lấy `createdAt DESC, id DESC`; user/store mới nhất theo timestamp rồi id. User thiếu giờ vẫn có thể dùng ngày + id để xếp hạng ổn định và bị loại khỏi hourly chart.
- Revenue/order chart: chọn tối đa 1000 đơn mới nhất trong scope/khoảng. Product chart: top tối đa 1000 sản phẩm theo số lượng từ đơn doanh thu hợp lệ trong khoảng, dùng dữ liệu chi tiết để group theo thời gian. Không dùng date tạo product để đại diện thời điểm bán.
- New user/store charts: chọn tối đa 1000 bản ghi mới nhất trong khoảng, sau đó group theo bucket; top điểm được tính độc lập theo toàn scope, sắp `point DESC, id DESC`. Top list giới hạn 1000 nhưng phân trang để không render 1000 card một lúc.
- Giữ định nghĩa recognized revenue mới: PAID và DELIVERED/COMPLETED, hoặc REFUND_REQUESTED đã ghi nhận ORDER_PAYMENT; REFUNDED bị loại một lần. Revenue là tổng tiền đơn theo hợp đồng hiện tại; số thu vào ví sau commission là chỉ tiêu khác, cần nhãn rõ.
- Doanh thu phân bổ theo product dùng unitPrice×quantity và phân bổ phần chênh lệch/discount/phí theo quy tắc thống nhất, rounding BigDecimal và điều chỉnh phần dư để tổng chi tiết khớp order total. Chưa mở rộng partial refund ngoài workflow hiện tại.

### 3.4 HTTP, validation và modal

- Các mutation dùng POST + CSRF; switch context cũng chuyển sang POST, GET legacy không ghi DB và không tự cấp quyền.
- DTO + Bean Validation thay binding entity trực tiếp. Chỉ nhận field được phép; lỗi hiển thị thân thiện, không trả exception/SQL/secret ra UI, giữ dữ liệu form để sửa.
- `page` 0-based, size 1–100; giá trị không hợp lệ trả 400 có thông báo, page vượt cuối đưa về trang cuối; giữ tab/filter/size khi chuyển trang.
- Dùng API `showGlobalConfirmModal` có callback rõ hoặc component Bootstrap hiện có; không dựa vào override `window.confirm()` vì API đó trả false ngay. Ưu tiên `requestSubmit()` với cờ chống lặp để giữ validation và handler, không gửi form hai lần.
- Confirmation hiển thị nội dung escaped, không đưa dữ liệu user qua eval hoặc innerHTML. Cancel giữ form và lựa chọn, Confirm disable nút khi pending rồi bật lại khi lỗi.

## 4. Backlog triển khai theo giai đoạn

### P0 — đồng bộ, kiểm tra và giữ baseline

Phụ thuộc: không có. Hoàn thành trước thay đổi nghiệp vụ.

- [x] Fetch, so sánh nhánh, bảo toàn local, fast-forward, restore local và `git diff --check`.
- [x] Lưu kết quả clean full test sau merge; giữ 22 failure làm backlog và phân biệt failure nghiệp vụ với selector/điều kiện môi trường trong báo cáo trước sync.
- [x] Ghi nhận và xử lý các lỗi frontend checker mới: div chưa đóng ở customer/cart, cú pháp JS ở checkout, duplicate IDs và cấu trúc div/form ở customer/profile. Xem rendered template để phân biệt false positive của checker với lỗi thật.
- [ ] Xác minh modal chung không chặn hoặc làm gửi đôi các luồng thanh toán/Confirm; browser smoke customer checkout sau sửa.
- [ ] Bổ sung provenance HEAD, timestamp, môi trường cho test results; giữ kết quả audit trước merge như lịch sử.

Đầu ra: baseline sau sync và danh sách regression. Không dùng các file Surefire cũ nếu lần Maven chỉ chạy 0 test hoặc bị dừng.

### P1 — sửa các invariant đang vi phạm

Phụ thuộc: P0; có thể triển khai trên mô hình hiện tại trước migration.

**UC_020 — server kiểm tra ứng viên:**

- [x] Tạo `StoreStaffManagementService`; owner-check ở service, validate cả selectedUserIds và username bằng cùng logic.
- [x] Chỉ thêm tài khoản active, không retired/disabled, đúng vai trò hợp lệ và chưa thuộc shop khác; chặn owner/self/Admin và candidate ID không tồn tại. Deduplicate selected IDs.
- [x] Batch add trong transaction, kiểm hết trước khi lưu; một candidate sai không để lại nửa danh sách đã thêm. Khóa user candidate theo thứ tự ID để ngăn hai shop cùng tuyển.
- [x] Delete chỉ đối tượng thực thuộc shop, không xóa owner; partner leave chỉ membership của chính mình.

**UC_023 — mật khẩu rút tiền:**

- [x] Bỏ `storedPassword.equals(input)`; chỉ BCrypt matches, thiếu/sai/hash input đều bị từ chối.
- [x] Đưa requireOwner vào WalletService trước kiểm số dư/ghi transaction; không chỉ dựa vào controller.
- [x] Loại bank/account default giả; validate thông tin ngân hàng, amount >=50.000, precision/scale theo cột tiền, số dư đủ. Không log password hoặc account đầy đủ.

**UC_019 — partial update:**

- [x] Tách DTO/endpoint cập nhật BasicInfo với Address/OpenStatus hoặc dùng field presence rõ ràng; Save name/bio/phone không đổi isOpen/address.
- [ ] Validate bio và các field giới hạn đúng entity, báo lỗi trước DB constraint; không ghi null do field không có trong form.

**UC_017/018 — commission và license:**

- [ ] Commission bắt buộc active; ID sai/deleted báo lỗi, không fallback âm thầm. Nếu không có commission active, khóa Submit và hướng dẫn Admin cấu hình.
- [x] Thực hiện chính sách Ban/License giữ nguyên selling/open/deleted theo mục 3.2; transaction + lock store, bỏ mass setSelling(true).

Đầu ra: 6 test hợp đồng trong StoreUseCaseAuditTests đều đạt; thêm ca username ngoài danh sách, inactive, self/owner, batch rollback, concurrent assignment và service withdrawal non-owner.

### P2 — nền tảng store context và migration

Phụ thuộc: P1.

- [ ] Thêm StoreMembership, repository, context/access services và DTO response cho relationship owned/partner.
- [ ] Migration SQL Server idempotent: table/index/FK/check constraints, unique membership; backfill non-owner User.store thành PARTNER cho VENDOR/MANAGER và STAFF cho STAFF, bỏ owner khỏi membership. Không lấy quyền Admin từ membership.
- [ ] Bản ghi role/assignment bất thường đưa vào báo cáo rà soát; không tự mở quyền để backfill cho hết lỗi.
- [x] CurrentStore ở session; resolve kiểm owner/membership active và tài khoản active ở mỗi request; fallback ổn định shop có quyền đầu tiên hoặc redirect stores.
- [x] Refactor toàn bộ nơi đọc/ghi User.store cho portal manager, stock, wallet, analytics/voucher; adapter cho staff assignment cũ trong giai đoạn chuyển đổi.
- [ ] Migration User.createdAt + độ chính xác timestamp; đăng ký mới dùng Clock, entity seeding/fixtures cập nhật; không giả định giờ cho dữ liệu legacy.
- [ ] Thêm index theo membership user/store, owner, store/status/date của order/product/transaction và user/store createdAt. Kiểm execution plan/query trên SQL Server trước quyết định index cuối.

Đầu ra: owner hai shop switch không đổi assignment; vendor nhiều membership nhìn thấy đầy đủ; hai session cùng user chọn shop khác nhau hoạt động độc lập; member bị remove mất quyền ngay cả session cũ.

### P3 — hoàn thiện UC_017–UC_020 và ảnh

Phụ thuộc: P2.

**UC_017:**

- [ ] `CreateStoreRequest` + `StoreManagementService.createStore` atomic owner/store/default level/commission; giữ giới hạn tên/phone/address/bio.
- [x] Vendor đăng nhập truy cập New shop, chọn commission, Submit review, Confirm tạo và chọn context rồi redirect dashboard; Cancel không tạo row và không upload ảnh trước Confirm.
- [x] Không mass-bind id/status/wallet/point/owner; double-click Confirm/idempotency token không tạo hai shop cho một lần submit. Không áp unique name toàn hệ thống nếu đặc tả không yêu cầu.

**UC_018:**

- [ ] StoreRepository query Page owned OR active membership, DISTINCT để tránh trùng, lọc/sort ở DB; bảng owned/partner hiển thị relationship rõ.
- [x] Admin Page query Licensed/Unlicensed + keyword, giữ filter khi paging; nút Ban/License review đúng shop, Confirm mutate, Cancel không đổi.
- [ ] Test public store/product, cart và checkout sau Ban; License không hồi sinh deleted và không bật Stored; thao tác concurrent không bỏ lọt điều kiện license.

**UC_019:**

- [x] Avatar/cover chọn file và lưu đúng; featured New→Submit→Confirm, Edit, Delete→Confirm; basic info pencil→Save→Confirm và thông báo.
- [ ] Củng cố image MIME/extension/magic bytes/size, filename/folder an toàn; giới hạn số ảnh/thư viện và độ dài URL theo schema.
- [ ] Giữ Cloudinary hành vi mới: cấu hình thật lỗi phải báo lỗi, không tự fallback local. Dùng mock cho test bình thường, local storage chỉ chế độ dev explicit.
- [ ] Replace ảnh: upload mới, commit DB rồi cleanup ảnh cũ best-effort; DB lỗi cleanup ảnh mới, không xóa ảnh cũ trước khi lưu thành công. Lưu publicId featured theo entity riêng hoặc bảng ảnh để delete chính xác, migrate chuỗi URL cũ có publicId null.
- [x] URL nhập tay chỉ http(s)/path local cho phép, parse URI; không claim đã xác minh ảnh nếu chỉ kiểm prefix. Không fetch URL arbitrary trên server để tránh mở thêm bề mặt truy cập mạng.

**UC_020:**

- [x] Shop owner/Shop staffs tabs + Page, tìm candidate không giới hạn 20 cố định; eligibility query và server POST dùng cùng chính sách.
- [x] Form chọn staff/partner rõ accessRole, Submit→Confirm, Cancel; Owner có Add/Delete, PARTNER có Out this shop, STAFF chỉ quyền staff.
- [ ] Khi cần đổi authority của CUSTOMER mới tuyển, không bỏ các vai trò còn có; refresh/invalidate session phù hợp. Không mặc định password yếu cho account mới; việc mời/tạo staff phải có validation và cơ chế đặt mật khẩu.

### P4 — hoàn thiện UC_021–UC_023

Phụ thuộc: P2; có thể làm sau P3 để dùng chung confirmation/validation.

**UC_021:**

- [x] GET `/manager/products?tab=selling|stored&page&size`; query scope/soft-delete/isSelling tại DB; stock query batch cho page để tránh N+1.
- [ ] GET trang create/edit riêng theo flow UC, giữ modal legacy bằng redirect/adapter nếu cần. DTO dùng chung cho create/update, giữ imageFile/Cloudinary/ProductImage mới merge.
- [x] Submit form→Confirm→POST; Store/Sell chuyển rõ trạng thái bằng giá trị đích thay toggle dễ đảo hai lần. Cancel không đổi product/stock/upload.
- [ ] Chặn foreign-store/deleted product ở tất cả mutation, kể cả stock; validation category active, tiền, initialStock; ảnh mới upload lỗi không tạo product/stock một phần.

**UC_022:**

- [x] Processing dựa vào workflow còn bước xử lý; gồm DELIVERED→COMPLETED và REFUND_REQUESTED→REFUNDED. Processed là terminal; không chỉ dùng set tĩnh khiến hành động hợp lệ bị ẩn.
- [x] Giữ allowedTransitions ở service làm nguồn duy nhất cho dropdown; Page theo scope + group/status, detail permission.
- [ ] Confirm mutation, Cancel giữ status; chặn stale status/concurrent transition, kết quả rõ ràng không double credit/refund/restock.
- [x] Giữ luồng cancel/refund vì khóa/xóa customer mới merge và cancellationReason; chưa loại enum rental lịch sử chỉ để cho test đạt.

**UC_023:**

- [ ] TransactionRepository Page theo store, sort createdAt/id; owner Withdraw button/endpoint/service đều đồng nhất.
- [x] Form amount/password/bank; Submit review không hiển thị password; Confirm tạo PENDING và trừ ví atomic; Cancel không tạo Transaction.
- [x] Idempotency key được ràng buộc user/store/request; double submit không rút hai lần. Admin resolve/reject idempotent đúng semantics, reject hoàn ví một lần.
- [ ] Test khóa DB hai withdrawal song song, insufficient funds, sai/missing/hash password, non-owner, negative/precision, stale session, bank invalid và retry.

### P5 — hoàn thiện UC_024–UC_026

Phụ thuộc: P2 timestamp và các invariants store/order ở P1/P4.

- [ ] Tạo `StatisticsFilter` và response thống nhất: scope, interval, criterion, chart type, buckets, values, units, total/full-vs-sample, sampleCount, sampleLimit, top lists/page.
- [ ] Chuyển tính recognized revenue/quantity/top vào AnalyticsService để dashboard và analytics không lệch; query aggregate hoặc fetch giới hạn <=1000 với detail batch/entity graph, không findAll cả DB cho mỗi chart.
- [x] Repository latest/top có createdAt/id và point/id sort; thời gian tính bằng Clock; sản phẩm bán chạy không fallback danh sách chưa sort sold như cũ.
- [x] Vendor/Admin UC_024 đều có tổng product/order, criteria orders/products/revenue, hour/day/month/year và line/bar/pie; chọn criterion thay đổi dataset, labels và units thật.
- [x] Admin UC_025 có KPI tổng user, nút xem chart, controls đầy đủ, topPointUsers cạnh chart. UC_026 tương tự với store chart/topPointStores, không lấy stores findAll để giả top điểm.
- [x] Khôi phục canvas adminStoresChart và bảo vệ DOM null ở initializer; render dữ liệu thật/empty/error rõ, bỏ sample fallback tài chính có thể khiến trang lỗi hiển thị doanh thu giả.
- [x] Chart pie dùng Chart.js type `pie` đúng yêu cầu; giữ selection khi reload/chuyển filter. Control thời gian riêng user/store hoặc ghi rõ dùng filter chung; không reset chart type do link timeframe bỏ tham số.
- [ ] Empty dataset vẽ trạng thái trống, không JS exception; lỗi server hiện thông báo, không trả HTTP 200 với số 0 giả như số liệu thật.
- [ ] Test số liệu với Clock cố định, boundaries ngày/tháng/năm, không dữ liệu, 1000/1001 mẫu, ties, foreign store, refund pending/refunded, commission và rounding.

### P6 — nghiệm thu và tài liệu

Phụ thuộc: P0–P5.

- [x] Chạy toàn bộ Maven suite, frontend checker, checkout quote và dashboard initializer; cập nhật test selector khi thay UI có chủ đích nhưng giữ kiểm hành vi tương đương.
- [ ] E2E browser trên server dev/test có fixture riêng: guest, owner, vendor partner, staff, admin, outsider; mọi Confirm/Cancel, paging, file picker và chart controls.
- [ ] Integration SQL Server trong database test riêng: migration, NVARCHAR(MAX), pagination, money precision, locking, rollback; không dùng DB vận hành để chạy test destructive.
- [x] Cloudinary test mock chạy mặc định; test cloud thật chỉ opt-in bằng env cấu hình, không log secret và cleanup asset test. Mail/MoMo không gọi thật chỉ để test các UC quản lý.
- [x] Cập nhật hướng dẫn kiểm thử, dữ liệu demo, ma trận UC→route→service→test→evidence; báo cáo final có HEAD/test counts và giới hạn còn lại.

## 5. Ma trận nghiệm thu tối thiểu

| UC | Main flow phải đạt | Alternative/exception và quyền |
|---|---|---|
| 017 | navigation→New→valid form/commission→Submit→Confirm→1 store→dashboard | Cancel 0 mutation; invalid/deleted commission/name/phone/address; guest/CSRF; retry |
| 018 | owned + nhiều partner stores, page boundaries, dashboard đúng shop; Admin Licensed/Unlicensed | foreign scope 403; Ban/License Confirm/Cancel; Stored/deleted giữ nguyên; public checkout bị chặn khi ban |
| 019 | avatar/cover, featured add/edit/delete, name/bio update | invalid file/URL/size; Confirm/Cancel; giữ isOpen; upload/DB error không mất ảnh cũ; foreign shop |
| 020 | owner/staff tabs, paging/candidate search, add multi atomic | owner Delete; partner Leave; Cancel; owner/self/admin/foreign/inactive candidate từ chối; concurrent add |
| 021 | Selling/Stored Page; new/edit form; validated product+stock+image | Store/Sell Confirm/Cancel; foreign/deleted/category inactive; upload fail; retry target status |
| 022 | processing/processed Page; detail; allowed dropdown; Confirm update | Cancel; foreign detail/mutation; terminal no dropdown; refund/delivered hành động hợp lệ; concurrent no double money |
| 023 | balance/transaction Page; owner form/password→Confirm→pending | Cancel; wrong/hash/missing password; partner/staff denied; insufficient funds/bank; concurrent/idempotency; reject refund once |
| 024 | vendor/admin KPI, criteria×timeframe×chart, top products/orders | empty/error; >1000 sampling; scope; recognized revenue/refund/rounding; controls đổi dataset thật |
| 025 | Admin count/view chart/line-bar-pie/hour-day-month-year/top user point | vendor/customer 403; timestamp legacy; 1000/1001; time boundaries; no dummy counts |
| 026 | Admin count/view store chart/full controls/top store point | role 403; no missing canvas/null JS; new timestamp order; 1000/1001; top point sort ties |

Cross-cutting: mọi list giữ filter/paging; mọi write có CSRF/authorization ở service; Cancel không POST; Flash/error escaped; context không cấp quyền; không làm mất bảo vệ payment/stock/refund vừa merge.

## 6. Thứ tự commit và bàn giao

Đề xuất các commit nhỏ theo thứ tự phụ thuộc:

1. `fix: resolve post-sync frontend regressions and capture baseline`.
2. `fix: enforce staff candidates, owner withdrawals and profile partial updates`.
3. `fix: validate active commission and preserve selling intent on store licensing`.
4. `feat: add store memberships, session context and timestamp migrations`.
5. `feat: complete store creation, listing, profile and staff use cases`.
6. `feat: complete product, order and wallet management flows`.
7. `feat: unify revenue and new-user/store statistics dashboards`.
8. `test: verify UC017-026 in browser and SQL Server and update acceptance report`.

Khi bắt đầu coding, dùng nhánh `codex/complete-store-use-cases` hoặc nhánh người dùng chỉ định. Tách migration schema với rollout code, backup database và dry-run migration trước deploy. Không chạy down migration xóa membership/timestamp/transaction đã phát sinh; rollback ưu tiên code tương thích schema và giữ dữ liệu mới.

Definition of done: 10 UC đạt ma trận trên; tất cả failure thực được xử lý, test suite xanh hoặc chỉ skip test external có điều kiện được mô tả; script static/runtime xanh; E2E Confirm/Cancel có evidence; SQL Server được xác minh; không còn TODO làm chặn flow; cập nhật tài liệu và migration chạy lại an toàn. Chưa tự commit/push/deploy trong bước tạo plan.
