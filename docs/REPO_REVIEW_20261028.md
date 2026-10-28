# Đánh giá repo ngày 28/10/2026

## Cập nhật kiểm tra lại tại `d781bcb` — 28/10/2026

Commit mới đã bỏ import cũ trong `JwtTokenService`, khắc phục **F1**. Kiểm tra mới bằng JDK 21: **857 ca Java, 845 đạt, 12 skipped, 0 failure/error; BUILD SUCCESS**. JavaScript **182/182 đạt**. Các kết quả compile thất bại ở phần khảo sát `fedd087` bên dưới là lịch sử.

| Phát hiện | Trạng thái tại `d781bcb` |
|---|---|
| F1 — import JWT | Đã sửa, compile và Java test đạt |
| F2 — chèn HTML thông báo pickup | Còn; chạy lại probe callback vẫn giữ markup không escape |
| F3 — voucher quote/transaction read-only | Còn trong source; chưa chạy lại integration PostgreSQL cho endpoint |
| F4 — stale update số lượt voucher | Còn trong source; chưa tái hiện bằng concurrency test ứng dụng |
| F5 — pickup-check bỏ qua trạng thái inventory | Còn trong source |
| F6 — `/search` không public | Còn; HTTP anonymous thực tế trả 302 tới `/login`, `/products` trả 200 |

Đã cấu hình và chạy web theo yêu cầu:

- Đọc `HUONG_DAN_CAU_HINH_MAY.md`; dùng `ca.pem` tại gốc repo cho TLS xác minh kết nối PostgreSQL Aiven (`verify-full`). Chứng chỉ không được dùng làm chứng chỉ HTTPS cho localhost.
- Thông tin đăng nhập được xác minh bằng `AivenConnectionCheck` trước khi lưu bằng `AivenLocalSetup` vào `.env.aiven` (Git ignore, ACL chỉ tài khoản Windows hiện tại). Không in thông tin xác thực.
- `AivenReadOnlyCheck` đạt: TLS, transaction chỉ đọc, schema V1, các bảng nghiệp vụ chính đọc được, không có số dư ví khách âm. Không thay đổi dữ liệu trong bước kiểm tra này.
- Sửa launcher **local, ignored** `start-web.ps1`: thay profile dev/SQL Server bằng Aiven, không nạp `.env` cũ; kiểm tra JDK 21/cổng/file; dùng trực tiếp `D:/Download/cloudflared.exe`; tắt seed. Bản launcher trước khi sửa lưu tại `target/start-web-before-aiven-20261028.ps1`.
- Đã gọi chính `start-web.ps1`, chạy nền, launcher PID **9308**, web PID **16552**. Có log `Started FinalprojectApplication`; lắng nghe `127.0.0.1:8080`; `/`, `/products`, `/login` trả HTTP 200. Kiểm tra không tạo đơn hoặc thanh toán thử trên DB chung.
- Flyway xác nhận schema public V1 đã cập nhật, **không có migration cần chạy**. Web hoạt động bình thường trên DB chung; scheduler/vòng đời đơn của ứng dụng vẫn chạy theo cấu hình hiện hành.
- Cloudflare báo READY tại `https://climate-col-commonly-planned.trycloudflare.com`. Root gateway trả 200; `/admin/dashboard`, `/customer/wallet`, `/checkout` trả 404 qua gateway. Đây là cổng thanh toán điện thoại, không phải địa chỉ công khai toàn bộ website. URL có thể đổi khi restart.
- Không sửa mã ứng dụng hoặc migration trong lượt kiểm tra lại; không commit/push.

Log mới: `target/repo-recheck-20261028-java.log`, `target/repo-recheck-20261028-js.log`, `target/web-aiven-20261028.out.log`, `target/web-aiven-20261028.err.log`, `target/web-aiven-20261028-tunnel-check.json`. `.env.aiven` chứa cấu hình riêng, không đưa vào báo cáo hoặc Git.

**Các mục 1–7 bên dưới giữ kết quả khảo sát ban đầu tại `fedd087`, để đối chiếu và xem chi tiết F2–F6.**

## 1. Kết luận

Khảo sát tại `fedd087` trên nhánh `main`, working tree sạch trước khi kiểm tra. Đây là ứng dụng bán hoa đa chi nhánh, monolith Spring MVC/Thymeleaf. Phạm vi chức năng và bộ kiểm thử khá rộng; cách tách vòng đời đơn, sổ tiền, ví khách và quyền chi nhánh là nền tảng tốt cho đồ án.

**Commit hiện tại chưa build được.** Maven trên Java 21 dừng ở một import trỏ đến package đã bị xóa. Ngoài lỗi build, có đường chèn HTML trong checkout, mâu thuẫn transaction khi báo giá có voucher trên PostgreSQL và các điểm không nhất quán về kho/quyền truy cập công khai. Cần xử lý các lỗi này trước khi nghiệm thu. Chưa đủ bằng chứng để xác nhận sẵn sàng production.

Đây là review dựa trên mã nguồn và các phép kiểm tra được ghi rõ bên dưới, không phải chứng nhận bảo mật hay kiểm thử toàn bộ từng màn hình. Không sửa mã ứng dụng, không đọc `.env`/`.env.aiven`, không chạy app, migration/seed/tunnel của dự án hoặc kết nối DB thật. Chỉ thêm báo cáo và cập nhật ghi chú local; log/probe sinh trong `target/`.

## 2. Tổng quan và kiến trúc

| Thành phần | Quan sát tại commit khảo sát | Đánh giá |
|---|---|---|
| Backend | Java 21, Spring Boot 4.1.1, MVC, JPA, Security, Validation | Phù hợp quy mô đồ án; không cần chia microservice |
| Frontend | Thymeleaf, Bootstrap, JS thuần; trang chủ đã tách fragment/CSS/JS | Dễ triển khai, nhưng nhiều trang khác còn chứa script và style lớn |
| Dữ liệu | SQL Server và PostgreSQL, H2 cho test mặc định | Có chủ đích hỗ trợ hai DB; cần kiểm tra hành vi transaction trên từng engine |
| Auth | Session cho browser; JWT stateless chain cho API dùng Bearer | Phân tách tương đối rõ; kiểm tra lại account/role từ DB |
| Nghiệp vụ | Catalog, giỏ hàng, checkout, đơn, kho, ví, hoàn tiền, chat, quản lý chuỗi | Độ phủ chức năng rộng, cần ưu tiên tính đúng trước khi thêm chức năng |
| Realtime | SockJS/STOMP, broker trong tiến trình | Hợp lý cho một instance; cần thiết kế lại nếu scale nhiều instance |
| Build/CI | Maven Wrapper, CI Java 21 và Node 22, Java/JS tests | Có nền tảng kiểm soát hồi quy; trạng thái checkout hiện tại vẫn lỗi compile |
| Vận hành | Profiles riêng, scripts DB/tunnel, tài liệu setup | Có công cụ hỗ trợ; chưa tìm thấy pipeline deploy/container/quan sát hệ thống trong phạm vi khảo sát |

Kiểm kê file hiện tại: **249** file Java production, **99** file dưới `src/test/java`, **3** file PostgreSQL test, **85** template, **26** file JS production, **11** file CSS. Số file không phải số test case hoặc coverage.

Luồng chính:

```text
Browser → Security chain → Controller → Service → Repository → DB
                            ↓             ↓
                        Thymeleaf     Mail/Cloudinary/MoMo demo/STOMP
```

Controller vẫn truy cập repository trực tiếp ở một số chỗ; sơ đồ trên là luồng chính, không phải hàng rào kiến trúc được compiler áp đặt.

Các service hiện là class cụ thể trong `com.finalproject.service`; 13 cặp interface/implementation đã được hợp nhất ở đợt refactor 28/10. `service/impl` trong tài liệu 27/10 là thông tin lịch sử. Các trường/enum thuê còn lại nhằm đọc lịch sử dữ liệu, không phải bằng chứng checkout còn hỗ trợ thuê.

## 3. Phát hiện cần sửa

P1: ưu tiên cao, cần sửa trước nghiệm thu. P2: lỗi cần sửa tiếp, có điều kiện kích hoạt hoặc phạm vi hẹp hơn. Mức bằng chứng được ghi riêng để không nhầm suy luận tĩnh với kiểm thử HTTP hoàn chỉnh.

### F1 — P1 — Import cũ làm toàn bộ backend không compile

- Vị trí: `src/main/java/com/finalproject/service/JwtTokenService.java:6`.
- Import còn là `com.finalproject.service.impl.CustomUserDetailsService`, nhưng class hiện ở `com.finalproject.service.CustomUserDetailsService`; tìm kiếm source không có package `service.impl` nào còn tồn tại.
- Thực thi `mvn -B -Pisolated-tests -Duser.timezone=UTC test` với Java 21 báo `package com.finalproject.service.impl does not exist` và `BUILD FAILURE` tại compile.
- Hệ quả: không thể build mới hoặc chạy Java tests cho checkout này. Report xanh trước đó không chứng minh commit hiện tại xanh.
- Hướng sửa: bỏ/cập nhật import, sau đó build trong output sạch để loại class cũ, chạy lại toàn bộ Java và các test PostgreSQL phù hợp.

### F2 — P1 — Tên sản phẩm/cửa hàng đi vào HTML của checkout mà chưa escape

- Nguồn dữ liệu: `controller/customer/PickupCheckController.java:43–45` ghép tên product/store vào `message` khi chọn sai chi nhánh của mẫu riêng.
- Điểm render: `templates/customer/checkout.html:1049–1053` ghép trực tiếp `data.message` vào `stockStatusEl.innerHTML`.
- Điều kiện: manager có thể lưu tên chứa markup; `ManagerProductController.validateName` chỉ kiểm tra rỗng/độ dài, và form sửa tên cửa hàng cũng không loại markup. Khách có mẫu riêng trong giỏ rồi chọn chi nhánh khác sẽ đi qua nhánh lỗi nói trên.
- Hệ quả: HTML do tài khoản quản lý nhập có thể được diễn giải trong phiên checkout của khách; đây là đường stored XSS. Việc Thymeleaf escape tên ở chỗ khác và việc escape `it.productName` trong bảng chi tiết không bảo vệ trường `message` này.
- Bằng chứng thực thi: probe trích đúng callback hiện tại từ template, truyền message chứa thẻ `<b data-audit-marker="true">audit</b>`, xác nhận markup được giữ nguyên trong `innerHTML`. Probe không chạy browser hoặc payload thực thi script.
- Hướng sửa: dựng icon/khung riêng, đặt message bằng `textContent`; nếu dùng helper escape phải áp dụng cho cả nhánh thành công và lỗi. Thêm hồi quy message chứa ký tự HTML/tên sản phẩm và tên chi nhánh.

### F3 — P1 — Báo giá có voucher lấy khóa ghi trong transaction chỉ đọc

- `service/OrderService.java:321,329`: các entry point `quote` dùng `@Transactional(readOnly = true)`.
- `OrderService.java:498`: tính tiền gọi `voucherService.findByCode` khi có mã.
- `service/VoucherService.java:22–26`: `findByCode` dùng transaction mặc định REQUIRED và `entityManager.refresh(..., PESSIMISTIC_WRITE)`.
- Transaction của service con tham gia transaction chỉ đọc bên ngoài; annotation mặc định ở service con không biến nó thành transaction ghi riêng. Khi JDBC áp dụng read-only, PostgreSQL không cho phép `SELECT ... FOR UPDATE`.
- Hệ quả dự kiến theo đường gọi: áp voucher tại `/checkout/quote` trên PostgreSQL bị lỗi thay vì trả tổng tiền; frontend cần quote hợp lệ trước khi đặt đơn.
- Bằng chứng bổ sung: PostgreSQL embedded tạm với JDBC read-only trả đúng SQLSTATE `25006`, `cannot execute SELECT FOR UPDATE in a read-only transaction`. DB tạm đã shutdown. Đây là probe hành vi engine, **chưa phải** HTTP integration test của ứng dụng vì F1 chặn compile.
- Hướng sửa: tách lookup chỉ đọc dùng cho quote khỏi lookup có khóa khi checkout tiêu thụ voucher. Giữ khóa/tái xác minh số lượt ở checkout. Bổ sung test PostgreSQL gọi quote qua Spring proxy, không bọc test trong transaction ghi khiến read-only của service bị che khuất.

### F4 — P2 — Sửa/bật tắt voucher có thể ghi đè số lượt đã dùng

- `controller/manager/ManagerVoucherController.java:130–181,191–195` đọc entity và save trong các repository transaction riêng; admin có đường tương tự tại `AdminVoucherController.java:144–213`.
- Checkout khóa/refresh voucher rồi tăng `usedCount` (`OrderService.java:251–259`), nhưng đường quản trị không dùng cùng khóa. `entity/Voucher.java` không có `@Version` hoặc `@DynamicUpdate`.
- Lịch xen kẽ có lỗi: request quản trị đọc `usedCount = 4`; checkout khác tăng thành 5 và commit; request quản trị save entity cũ. Hibernate cập nhật entity có thể mang lại giá trị 4, dù quản trị chỉ muốn đổi tên/trạng thái. Khóa riêng của checkout không ngăn stale update đến sau.
- Hệ quả: sai hạn mức voucher, có thể phát hành nhiều lượt hơn cấu hình; kiểm tra quantity dựa trên bản cũ cũng có thể sai.
- Mức bằng chứng: phân tích source và lịch giao dịch; chưa chạy reproducer đa luồng trên ứng dụng.
- Hướng sửa: đưa mutation quản trị vào service transaction, khóa/refresh voucher trước khi kiểm tra và cập nhật; cân nhắc optimistic version qua migration mới. Thêm test checkout đồng thời với sửa/toggle voucher, kiểm tra `usedCount` không giảm.

### F5 — P2 — API kiểm tra nhận tại quầy bỏ qua trạng thái bán/xóa của kho chi nhánh

- `controller/customer/PickupCheckController.java:50–53` đọc mỗi `quantity`, sau đó xác định đủ hàng từ `available >= needed`.
- Không kiểm tra `StoreInventory.isAvailableForSale()`, dù trạng thái `isSelling=false` hoặc `isDeleted=true` có thể tồn tại khi quantity còn dương.
- `entity/StoreInventory.java:35–38` đã có predicate phù hợp; checkout thật tại `OrderService.java:216–217` dùng predicate này.
- Trường hợp: mẫu chung còn hàng ở chi nhánh A nhưng đã ngừng bán/lưu trữ riêng tại A; product tổng vẫn public. API nhận tại quầy báo đủ hàng, trong khi quote/checkout từ chối.
- Hệ quả: người mua nhận thông báo không nhất quán. Chưa thấy đường vượt kiểm tra checkout để mua hàng đã ngừng bán.
- Mức bằng chứng: đối chiếu tĩnh hai luồng; chưa kiểm thử HTTP tại commit này.
- Hướng sửa: dùng cùng quy tắc khả dụng cho snapshot, pickup-check, quote và checkout; test trạng thái paused/deleted với tồn kho dương.

### F6 — P2 — Alias tìm kiếm `/search` yêu cầu đăng nhập ngoài dự kiến catalog

- `controller/customer/HomeController.java:45` ánh xạ cùng một handler cho `/`, `/products`, `/search`.
- `config/SecurityConfig.java:87–93` cho phép public `/` và `/products/**`, không có `/search`; route này rơi vào `.anyRequest().authenticated()`.
- Hệ quả: khách truy cập alias `/search?keyword=...` bị đưa đến đăng nhập, trong khi cùng truy vấn tại `/products` là công khai. Các form hiện dùng `/products` vẫn hoạt động; phạm vi lỗi là alias được controller công bố.
- Mức bằng chứng: mapping và thứ tự matcher trong source; chưa chạy MockMvc vì F1.
- Hướng sửa: thêm `/search` vào public allowlist nếu giữ alias, hoặc loại alias và redirect thống nhất. Thêm anonymous regression test cho từng đường catalog.

## 4. Rủi ro cần xác minh tiếp

Các mục dưới đây chưa được coi là sự cố đã tái hiện trên ứng dụng.

1. **Thứ tự lock ở thao tác mẫu riêng:** checkout khóa product trước inventory (`OrderService.java:193,202`); `StoreProductService.setSelling/delete/restore` lấy inventory lock rồi sửa product (`:72–105`). Flush product có thể chờ khóa product trong khi checkout giữ product và chờ inventory. Cần test hai transaction độc lập cho mẫu có `product.store != null`, thống nhất product → inventory trước khi sửa. `@Version` trên inventory không tự giải quyết vòng chờ khóa.
2. **Thu hồi WebSocket đang mở:** inbound interceptor kiểm tra CONNECT/SEND/SUBSCRIBE, nhưng chưa tìm thấy outbound authorization hoặc cơ chế đóng kết nối khi account/quyền chi nhánh đổi. Cần kiểm tra người đã subscribe còn nhận topic cũ sau khi bị khóa/chuyển chi nhánh hay không; việc kiểm tra request HTTP tiếp theo không đủ chứng minh socket được thu hồi.
3. **Quy mô và hiệu năng:** `open-in-view=true`, template truy cập lazy relation; vẫn có các màn hình tải list và lọc trong JVM. Chưa đo query count/N+1, p95, index plan hoặc tải đồng thời; không khẳng định đã có sự cố hiệu năng chỉ từ cấu trúc này.
4. **State trong JVM:** pending registration, session demo payment và simple broker phụ thuộc instance. Restart/multiple instances cần có chính sách rõ cho dữ liệu chờ, session và realtime.
5. **Ranh giới môi trường:** dev auto-baseline và dev/Aiven auto-tunnel mặc định bật; startup có order-expiry runner nếu không tắt cờ. Các lệnh run/smoke không mặc nhiên là chỉ đọc. Production cần lựa chọn profile, migration/backup/rehearsal và callback URL có kiểm soát.
6. **Quan sát và vận hành:** chưa tìm thấy cấu hình đo coverage, quét dependency, metrics/alert hay quy trình deploy tự động trong phạm vi khảo sát. Không kiểm tra hạ tầng bên ngoài repo, CI remote, CVE hoặc rotation credential. Không kết luận dịch vụ bên ngoài an toàn hay không an toàn.
7. **Giao diện:** checkout/product-detail/profile vẫn là template lớn có logic JS bên trong; kiểm thử JS hiện chủ yếu dùng mock DOM/VM. Chưa kiểm tra layout responsive, accessibility, browser autofill/back-forward và toàn bộ tương tác Bootstrap trong lần này.

## 5. Điểm làm tốt nên giữ

- `StoreAccessService` tập trung kiểm tra tài khoản và phạm vi chi nhánh; session/browser và JWT đều có cơ chế đọc lại quyền/tình trạng account.
- `OrderService` khóa user/cart/product/stock, sort ID product trước khi lock, refresh dữ liệu trước kiểm tra và trừ kho; giá/tổng tiền được tính lại ở server.
- `OrderWorkflowService`, `OrderFundsService`, `CustomerWalletService` tách trách nhiệm vòng đời, tiền giữ và ví khách; số tiền dùng `BigDecimal`, có kiểm tra chống xử lý lặp.
- Order/store/stock/customer-wallet có optimistic version, các vùng quan trọng còn dùng pessimistic lock. Đây là lớp bảo vệ có ích; vẫn cần rà soát mọi đường ghi để tránh lock order hoặc stale entity.
- Notification và thông báo đơn qua STOMP được đăng ký sau commit, giảm nguy cơ công bố đơn chưa lưu thành công.
- Flyway phân biệt chuỗi SQL Server và baseline PostgreSQL; test PostgreSQL dùng database embedded có lifecycle riêng.
- Trang chủ tách fragment/CSS/JS là cải thiện thực tế; không nên refactor hàng loạt các trang khác trước khi sửa lỗi đúng/sai hiện tại.
- CI có Java/JS; test đã bao phủ nhiều use case admin/manager/staff, quyền, session, kho, hoàn tiền và UI regression. Sự tồn tại của test không thay cho số liệu coverage hoặc kết quả chạy tại HEAD.

## 6. Kiểm tra đã thực hiện

| Kiểm tra | Kết quả hiện tại |
|---|---|
| Git status ban đầu | Sạch; HEAD `fedd087` |
| Maven Java 21, `-Pisolated-tests -Duser.timezone=UTC test` | **FAIL tại compile**, 1 lỗi import; Java tests chưa được chạy |
| Node v24.14.1, `node --test src/test/js/*.test.cjs` | **182 passed, 0 failed, 0 skipped** |
| `node --check` cho toàn bộ 26 JS production | 0 lỗi cú pháp |
| Probe callback pickup trong checkout | Xác nhận message được đưa vào HTML không escape |
| Probe PostgreSQL embedded, transaction read-only + FOR UPDATE | SQLSTATE 25006 đúng như dự kiến; DB tạm đã đóng |
| `git diff --check` | Đạt trước khi thêm báo cáo; kiểm tra lại sau khi thêm tài liệu |
| Java integration/full PostgreSQL suite | Chưa chạy được vì compile thất bại; probe engine không thay thế suite này |
| Browser, app startup, DB thật, mail/Cloudinary/payment/tunnel live | Không thực hiện |

JDK dùng: `C:/Users/LOQ/.antigravity/extensions/redhat.java-1.54.0-win32-x64/jre/21.0.10-win32-x86_64`. Node local khác Node 22 CI. Không fetch/push và không lấy báo cáo test cũ làm kết quả hiện tại.

Log và probe local:

- `target/repo-audit-20261028-java.log`
- `target/repo-audit-20261028-js.log`
- `target/repo-audit-20261028-probe.cjs`
- `target/RepoAuditPostgres.java`
- `target/repo-audit-20261028-postgres-probe.log`
- `target/repo-audit-20261028-dependencies.log`

## 7. Thứ tự xử lý đề xuất

1. Sửa F1, build bằng output sạch và chạy lại suite Java. Đây là điều kiện để mọi kết quả integration sau đó có ý nghĩa.
2. Sửa F2 và F3; thêm regression cho message HTML và quote có voucher trên PostgreSQL ngoài transaction ghi của test.
3. Sửa F4/F5/F6; kiểm thử mutation voucher đồng thời, stock paused/deleted và anonymous routes.
4. Xác minh lock order và WebSocket revocation bằng transaction/kết nối thật trong môi trường tạm.
5. Nghiệm thu browser các luồng mua → thanh toán demo → xử lý đơn → hủy/hoàn ví, theo từng role và chi nhánh.
6. Sau khi tính đúng được bảo đảm, đo truy vấn/tải, bổ sung quan sát vận hành, rồi tiếp tục giảm logic inline ở từng trang có test bảo vệ.

Không cần viết lại toàn bộ repo hoặc chuyển framework để giải quyết các vấn đề đã phát hiện.
