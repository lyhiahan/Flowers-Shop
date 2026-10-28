# Kế hoạch sửa lỗi trên nhánh quocanh-test

Ngày lập: 28/10/2026. Nhánh đã tạo và checkout từ `d781bcb`. Tài liệu này là kế hoạch; chưa sửa mã ứng dụng, thay cấu hình pool, restart app hoặc triển khai bản sửa.

## 1. Mục tiêu và phạm vi

Ưu tiên sự cố HTTP 500 do không lấy được JDBC connection: phân biệt kết nối DB bị gián đoạn, truy vấn chậm/khóa, giữ connection lâu và tải request vượt khả năng pool. Sau đó xử lý năm phát hiện F2–F6 còn lại trong `REPO_REVIEW_20261028.md` bằng các thay đổi riêng.

Giữ đúng xác thực và quyền chi nhánh, tính nguyên tử của đơn/kho/ví, chống thanh toán hoặc hoàn tiền lặp. Không thay DB, không seed/reset, không sửa migration đã áp dụng và không tự push. Không dùng DB Aiven chung để gây lỗi mạng, tạo tải hoặc thử ghi nghiệp vụ.

## 2. Bằng chứng ban đầu

| Quan sát | Bằng chứng | Kết luận được phép rút ra |
|---|---|---|
| Aiven pool có maximum 3; timeout chờ 20 giây | `application-aiven.properties:9–10`, `application.properties:23` | Một request giữ connection lâu có thể ảnh hưởng rõ với pool nhỏ |
| 13:01:07–13:01:17, total=3, active=3, idle=0; waiting=6–8 | `target/web-aiven-20261028.out.log:98` trở đi | Pool bão hòa tại thời điểm lỗi; chưa chứng minh connection leak |
| Có lỗi I/O PostgreSQL, SQLSTATE 08006 | Log cùng sự cố khoảng 13:01:49 | Có gián đoạn giao tiếp DB, không chỉ chờ cấp connection |
| Sau đó total=0, lỗi tạo connection mới, Connection refused | Log khoảng 13:04 | Đầu cuối DB hoặc đường mạng đã từ chối kết nối trong giai đoạn này; tăng pool không xử lý nguyên nhân đó |
| Stack lỗi có `AccountStatusFilter.doFilterInternal:25` | Stack đầu sự cố | Lỗi DB có thể xảy ra ở security filter, trước phạm vi xử lý của MVC advice |
| 13:05:30 TCP đến DB truy cập lại được; probe JDBC chỉ đọc đạt | `AivenReadOnlyCheck` dùng TLS verify-full, schema V1 | Kết nối đã phục hồi tại thời điểm kiểm tra, chưa biết nguyên nhân gián đoạn từ phía dịch vụ |
| Thread dump sau phục hồi: 10 HTTP worker idle, 0 worker trong JDBC wait | `target/jdbc-incident-20261028-threads.txt` | Không quan sát được kẹt JDBC ở snapshot này; snapshot không đại diện thời điểm bão hòa |
| Một GET anonymous trang chủ trả 200 trong 5.358 ms sau khi kết nối phục hồi | Kiểm tra HTTP cục bộ, không restart | App có khả năng phục hồi ở trường hợp này; một lần thành công không chứng minh đã hết lỗi |

Nguồn lực và tải có thể làm sự cố lan rộng:

- `stock-kpi.js` polling mỗi 5 giây, hủy fetch sau 8 giây. `pending` chỉ chống trùng trong cùng một trang khi fetch còn chạy; hủy fetch không bảo đảm truy vấn server đã bị hủy. Timeout pool server hiện 20 giây có thể dài hơn thời gian browser chờ.
- `AccountStatusFilter` đọc user mỗi request đã xác thực, chưa có loại trừ rõ cho static resource công khai. Cần đo tần suất thực tế trước khi tối ưu.
- OSIV đang bật; template có lazy relation. Cần đo thời gian giữ connection và số query; chưa được kết luận OSIV là nguyên nhân gốc.
- Hướng dẫn máy ghi giới hạn 20 connection cho dịch vụ và pool 3/máy; quota/phiên đang dùng cần xác minh lại trước khi thay pool.

Chưa xác định được: vì sao ba connection ban đầu đều bận, truy vấn nào giữ chúng, DB có restart/bảo trì hay không, có lock từ máy khác hay không. Không gán sự cố cho rò rỉ connection hoặc password/CA khi chưa có bằng chứng; TLS/login hiện đã kiểm tra thành công.

## 3. Giai đoạn A — Thu thập đủ nguyên nhân

1. Bổ sung chẩn đoán bật/tắt bằng cấu hình local: Hikari active/idle/pending/timeout, thời gian chờ connection, thời gian xử lý endpoint và số query. Không log password, token, email, nội dung SQL parameter hoặc request body.
2. Cân nhắc bật Hikari MBeans hoặc metric nội bộ cho môi trường chẩn đoán; không công khai endpoint quản trị qua web/Cloudflare. Leak detection chỉ bật tạm để tìm connection mượn lâu, không coi cảnh báo là bằng chứng leak.
3. Nếu lỗi tái diễn, chụp thread dump ngay khi pool bão hòa, đối chiếu timestamp với metric và log SQLSTATE. Probe riêng không lấy connection từ pool đang cạn.
4. Kiểm tra PostgreSQL bằng truy vấn chỉ đọc tổng hợp: quota, số phiên theo ứng dụng, wait event, tuổi transaction, blocking PID; không xuất SQL text/PII. Đối chiếu thời điểm restart/maintenance/network với người quản lý Aiven nếu dữ liệu trong repo không giải thích được.
5. Đo riêng anonymous/authenticated: trang chủ, catalog stock, notifications, chat và scheduler; đếm bao nhiêu DB round-trip cho một lần tải trang. Chỉ đo tải đồng thời ở PostgreSQL tạm.

Đầu ra: timeline, phân bố thời gian mượn connection và một reproducer có kiểm soát. Quyết định pool/timeout/query dựa trên số liệu đó.

File dự kiến: `application-aiven.properties`, cấu hình chẩn đoán JDBC mới nếu cần, script probe chỉ đọc hoặc test trong `src/postgresql-test`. Không thêm dependency chỉ để đọc một vài số liệu nếu API hiện có đáp ứng.

## 4. Giai đoạn B — Phản hồi an toàn khi DB mất kết nối

1. Phân loại lỗi thiếu kết nối từ cause chain: Hikari timeout, lỗi JDBC kết nối SQLSTATE nhóm 08 và transaction không mở được do DB unavailable. Không gom mọi lỗi SQL/transaction thành lỗi tạm thời.
2. Trả HTTP **503 Service Unavailable** kèm thông báo ngắn và `Retry-After` cho lỗi tạm thời. Lỗi lập trình hoặc SQL sai vẫn cần log/phân loại riêng.
3. Xử lý cả lỗi trong `AccountStatusFilter` và MVC. Với request cần quyền: từ chối xử lý trong lúc không xác minh được DB, giữ kiểm tra role/store và không biến người dùng thành hợp lệ bằng dữ liệu cache cũ. Không xóa session chỉ vì lỗi mạng tạm thời.
4. Loại DB lookup khỏi các đường static resource công khai đã xác định bằng prefix chính xác. Không dùng quy tắc theo đuôi `.css`/`.js` để vô tình bỏ qua kiểm tra cho endpoint nghiệp vụ.
5. HTML error dùng thông báo do ứng dụng kiểm soát, không render exception thô từ `${message}`. API trả JSON với mã lỗi ổn định; giữ tương thích `message` mà client hiện đọc.
6. Trang lỗi phải render được khi datasource hoàn toàn không dùng được, không kích hoạt global model query hoặc widget chat/reconnect gây thêm request. Rà soát `GlobalCartAdvice`, layout advice và cả hai error template.
7. Rà soát catch ở HomeController để DB outage không trở thành trang catalog trống HTTP 200 hoặc lỗi template tiếp theo. Không biến lỗi tải dữ liệu thành bằng chứng tồn kho bằng 0.

File dự kiến: `security/AccountStatusFilter.java`, `exception/GlobalExceptionHandler.java`, helper phản hồi lỗi nếu cần, `templates/error.html`, `templates/common/error.html`, `GlobalCartAdvice.java`, `HomeController.java` và test liên quan.

Nghiệm thu: DB down → static asset vẫn tải; HTML/API có 503 phù hợp, không lộ JDBC/SQL/host; request mua hàng hoặc quản trị không vượt quyền; trang lỗi không truy vấn DB.

## 5. Giai đoạn C — Giới hạn thời gian và hỗ trợ phục hồi

1. Rà soát timeout theo thứ tự: chờ Hikari, kết nối TCP/TLS, đọc socket PostgreSQL, truy vấn và toàn request. Browser abort không phải timeout truy vấn server.
2. Chọn ngân sách thử nghiệm cho endpoint polling ngắn hơn timeout 8 giây phía client; ví dụ Hikari wait 3–5 giây. Giá trị cuối phải được kiểm tra với độ trễ Aiven và số query. Endpoint checkout cần ngân sách riêng phù hợp transaction nghiệp vụ.
3. Thử `connectTimeout`, `socketTimeout`, TCP keepalive, validation/keepalive/maxLifetime phù hợp. Xác minh connection lỗi được loại và pool tạo lại được sau mất mạng. Không dùng `connectionTestQuery` nếu JDBC validation hiện có đã đủ.
4. Nếu đặt statement/transaction timeout, xác minh cancellation và rollback giải phóng connection; kiểm tra tác động lên locks, checkout, hủy/hoàn tiền. Không coi timeout socket là chứng minh giao dịch chưa commit.
5. Giữ pool 3 làm baseline, đo quota tổng của tất cả máy trước khi quyết định tăng. Không đặt pool của từng máy bằng toàn bộ quota DB; chừa dung lượng cho migration/vận hành và các instance khác.
6. Không tự retry POST tạo đơn, xác nhận thanh toán, hoàn/rút tiền khi kết quả commit không rõ. Chỉ retry thao tác đọc có giới hạn; các thao tác ghi cần khóa/idempotency và kiểm tra trạng thái hiện có.

File dự kiến: `application-aiven.properties`, cấu hình timeout ở service/repository đã đo, tests PostgreSQL. Không sửa lịch sử migration cho cấu hình runtime.

Nghiệm thu: trong môi trường tạm, ngắt/khôi phục kết nối không làm request chờ vô hạn hoặc pool kẹt vĩnh viễn; app hoạt động lại không cần restart; không nhân đôi trừ kho/ghi ví khi client gửi lại sau timeout.

## 6. Giai đoạn D — Giảm request và thời gian giữ connection

1. Đổi polling tồn kho sang lịch hẹn sau khi request trước kết thúc; xử lý 503/Retry-After, backoff có jitter và giới hạn tối đa. Dừng khi tab ẩn/offline; quay lại tab chỉ tạo một request.
2. Giữ số liệu xác nhận gần nhất kèm cảnh báo cũ; không bật nút mua bằng dữ liệu lỗi. Sau hồi phục, cập nhật lại trạng thái bán, chi nhánh và giỏ hàng như các test hiện tại.
3. Kiểm tra các fetch notifications/chat và reconnect để tránh nhiều vòng retry đồng thời; chỉ áp dụng điều chỉnh nếu đo được góp phần tăng tải. Không gửi tin nhắn tự động để thử trên dữ liệu thật.
4. Gom snapshot đọc bằng projection/DTO, giảm N+1 và global cart query lặp khi có số liệu xác nhận. Ưu tiên `CatalogStockService`, truy vấn store/category và dữ liệu card trang chủ.
5. Xác định transaction đọc ngắn, trả dữ liệu đủ để render ngoài transaction. Không bọc toàn bộ controller và render template trong transaction để chữa cạn pool.
6. Giữ OSIV hiện hành trong bản sửa đầu; chỉ chuyển sang tắt sau khi các màn hình dùng DTO/projection có render test. Không cache quyền/account hoặc tồn kho để bỏ qua xác minh checkout.

File dự kiến: `static/js/stock-kpi.js`, `src/test/js/stock-kpi.test.cjs`, `service/CatalogStockService.java`, `HomeController.java`, các projection/repository và render test thực sự cần đổi.

## 7. Ma trận kiểm thử và điều kiện hoàn tất

| Tình huống | Môi trường | Kết quả cần có |
|---|---|---|
| Pool hết connection do bị giữ có chủ đích | Datasource tạm, pool 3 | Request kết thúc theo budget; HTML/API 503; không lộ exception |
| DB trả connection refused / gián đoạn socket | PostgreSQL embedded qua proxy hoặc datasource fault injection | Lỗi được phân loại, connection hỏng bị loại; không retry ghi mù |
| DB khôi phục sau outage | Môi trường tạm | Tự trả 200 trở lại; pending trở về 0 sau tải dừng, không cần restart |
| 1/3/5 tab có polling, thêm authenticated user | Môi trường tạm với dữ liệu đại diện | Không bão request; báo cáo p50/p95, số query và pool wait so với baseline |
| Account bị khóa/đổi role/chuyển branch | Java security tests | Quyền thay đổi vẫn có hiệu lực; DB outage không cấp quyền |
| Filter lỗi trước controller; error dispatch; JSON/HTML | MockMvc + filter tests | 503 đúng loại nội dung; không phụ thuộc DB để render lỗi |
| Stock fetch lỗi, abort, retry, hidden/focus/pageshow | Node tests | Không request trùng, giữ stock cũ, backoff đúng và hồi phục đúng |
| Timeout giữa checkout/refund và client retry | Transaction tests PostgreSQL | Không bán âm kho, không trừ/hoàn tiền hai lần, kết quả đối soát được |
| Full Java/JS, whitespace và render trang chính | H2 + Node + PostgreSQL tạm | Không regression; ghi kết quả chạy mới, không dùng kết quả cũ thay thế |

Nghiệm thu live sau bản sửa: kiểm tra nhẹ TLS/login, một vòng HTTP anonymous/authenticated bằng tài khoản được phép, và xác minh Cloudflare vẫn chỉ mở cổng thanh toán. Không thử outage hoặc load test trên Aiven chung. Mốc p95 cụ thể sẽ chốt sau đo baseline thay vì cam kết trước khi biết độ trễ/khối lượng dữ liệu.

Baseline trước sự cố ở `d781bcb`: Java 857 ca (845 đạt/12 skipped), JS 182 đạt. **Không chạy lại suite trong lượt lập kế hoạch**, vì chưa đổi source; các phép kiểm tra hiện tại là log/thread dump, TCP, JDBC read-only và một GET homepage.

## 8. Chia thay đổi để review và rollback

1. `diagnostics`: ghi nhận timeline/reproducer và số liệu cần thiết.
2. `db-unavailable-response`: lỗi 503 an toàn từ filter đến template/API, kèm test.
3. `jdbc-timeouts-recovery`: timeout/khôi phục pool đã được thử ở môi trường tạm.
4. `polling-backoff`: điều tiết polling và bảo toàn UI stock.
5. `catalog-query-duration`: chỉ tối ưu truy vấn/transaction đã xác minh chậm.

Mỗi phần độc lập có kiểm thử trước khi ghép. Runtime configuration giữ khả năng trở về giá trị trước; diagnostic có cờ tắt. Các thay đổi không cần migration DB được ưu tiên để rollback bằng mã/cấu hình. Không commit/push tự động trong bước lập kế hoạch này.

## 9. Backlog năm lỗi review trước

Thực hiện sau khi ổn định sự cố DB, hoặc bằng thay đổi độc lập nếu cần ưu tiên bảo mật:

| Mục | Việc sửa | Kiểm thử chính |
|---|---|---|
| F2 — XSS pickup | Dùng textContent cho message chứa tên product/store | Message chứa HTML chỉ hiển thị như text |
| F3 — voucher quote | Tách lookup đọc khỏi lookup khóa khi checkout | PostgreSQL quote chạy qua proxy read-only, không transaction ghi bên ngoài |
| F4 — stale usedCount | Mutation voucher trong service transaction có lock/refresh hoặc version phù hợp | Checkout đồng thời update/toggle không làm giảm usedCount |
| F5 — pickup stock | Dùng cùng predicate khả dụng với checkout | Inventory paused/deleted có số lượng dương vẫn bị từ chối |
| F6 — public search | Thống nhất security matcher và alias `/search` | Anonymous `/`, `/products`, `/search` hoạt động theo cùng chính sách |

Không gộp các lỗi này thành nguyên nhân của JDBC outage khi chưa có bằng chứng. Chi tiết vị trí source và giới hạn xác minh xem `REPO_REVIEW_20261028.md`.
