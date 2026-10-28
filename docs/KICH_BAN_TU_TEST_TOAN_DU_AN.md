# Bộ kịch bản tự kiểm thử toàn dự án

Ngày đối chiếu mã nguồn: **28/10/2026**. Dự án: chuỗi cửa hàng hoa, Java 21 trở lên / Spring Boot / Thymeleaf / PostgreSQL trên Aiven; giữ cấu hình SQL Server cho luồng tương thích cũ.

Tài liệu này là kế hoạch kiểm thử thủ công và đầu vào cho kiểm thử tự động bằng Playwright Python, API và backend, dựa trên cấu trúc, controller, service, template, JavaScript và các nhóm test hiện có. **Chưa thực hiện các ca trên trình duyệt hoặc database thật trong lần cập nhật tài liệu này.** Kết quả mong đợi là tiêu chí để bạn phát hiện lỗi, không phải khẳng định hệ thống đã chạy đạt. Không thể liệt kê hữu hạn mọi tổ hợp đầu vào; bộ này bao phủ các luồng chức năng hiện có, nhánh lỗi, biên dữ liệu, phân quyền và các rủi ro hồi quy chính.

**Quy mô:** **12 kịch bản xuyên suốt + 500 ca chi tiết**, gồm **256 P0, 234 P1 và 10 P2**, cho cả ADMIN, MANAGER, STAFF, USER, khách chưa đăng nhập và cấu hình vận hành. Xem cách phân lớp và thống kê tại mục 9. Các dòng có nhiều biến thể cần chạy từng biến thể riêng. Checklist M1 ở cuối là phần bổ trợ, không cộng lặp vào 500 ca.

Đi theo thứ tự: chuẩn bị dữ liệu (mục 2) → luồng xuyên suốt (mục 3) → ca chức năng (mục 4) → môi trường, Aiven và Cloudflare (mục 5) → nghiệm thu (mục 7) → tổ chức test Playwright Python (mục 9).

## 1. Đọc nhanh cấu trúc và phạm vi

| Thư mục / thành phần | Vai trò | Cần kiểm thử gì |
|---|---|---|
| `pom.xml`, `.mvn`, `mvnw*`, `scripts/maven.ps1` | Build Java 21, Maven Wrapper | Khởi động, build/test và profile |
| `src/main/java/com/finalproject/controller/auth` | Đăng ký, OTP, đăng nhập session/JWT | AUTH, API |
| `controller/customer` | Trang công khai, hồ sơ, giỏ, checkout, thanh toán, đơn, ví khách | PUB, ACC, CART, CHK, PAY, ORD, REV, CW |
| `controller/manager`, `controller/staff` | Quản trị chi nhánh và điểm vào xử lý đơn | STORE, STAFF, PROD, ORD, WAL, REPORT |
| `controller/admin` | Quản trị toàn hệ thống | ADMIN, MASTER, voucher, phí giao, giao dịch, giám sát chat |
| `controller/common`, `InventoryApiController` | Chat, thông báo, kiểm tra kho | CHAT, NOTI, STOCK |
| `service`, `service/impl` | Luật nghiệp vụ, transaction, khóa dữ liệu | Thanh toán, hoàn tiền, checkout đồng thời, tính giá, quyền chi nhánh |
| `entity`, `enums`, `repository` | Mô hình dữ liệu, trạng thái, truy vấn | Tính nhất quán giữa UI, kho, đơn, ví và báo cáo |
| `config`, `security`, `exception`, `utils` | Session/JWT/CSRF, WebSocket, scheduler, xử lý lỗi | SEC, API, OPS |
| `dto/request`, `dto/response` | Validation và dữ liệu gửi/nhận | Biên độ dài, giá, số lượng, trường bắt buộc |
| `src/main/resources/templates` | Thymeleaf: auth/customer/manager/admin/staff, fragments | Form, nút theo quyền/trạng thái, phân trang, modal |
| `src/main/resources/static/js` | Giỏ AJAX, báo giá, kho 5 giây, chat, thông báo, ảnh, kỳ báo cáo | Đồng bộ nhiều tab, retry, trạng thái tải/lỗi |
| `static/images`, `upload` | Ảnh có sẵn và dữ liệu upload cục bộ | Ảnh thiếu/hỏng, quyền upload, fallback |
| `application*.properties` | Profile và cấu hình dịch vụ | Aiven, TLS, thanh toán mô phỏng, mail, Cloudinary, SQL Server |
| `resources/db/migration/postgresql`, `resources/db/migration/sqlserver`, `java/.../db/migration` | PostgreSQL baseline V1 riêng; SQL Server có migration đến V17 | Database mới/nâng cấp, ví khách, STAFF và giữ dữ liệu cũ; không dùng lẫn hai lịch sử Flyway |
| `database.sql`, `sql/migrations`, `resources/db/*.sql` | SQL khởi tạo/sửa/di sản | Không chạy chồng tùy tiện với Flyway |
| `src/test/java`, `src/postgresql-test`, `src/test/js`, `src/test/resources` | Test hồi quy backend/frontend, H2 và test PostgreSQL/SQL Server riêng | H2 hoặc Playwright PASS không thay thế kiểm tra transaction/migration trên đúng hệ quản trị |
| `scripts` | Chạy local, cấu hình mail, tunnel điện thoại, kiểm tra SQL, dữ liệu demo | Chỉ dùng đúng script tương ứng môi trường test |
| `docs`, `README.md`, `HUONG_DAN_KIEM_THU.md` | Tài liệu | Tài liệu cũ có phần đã lỗi thời |
| `target`, `.tools`, `.git`, `.settings`, `.agents` | Đầu ra build, công cụ local, quản lý dự án | Không coi `target` là source; không đưa credential vào bằng chứng test |

### Những quy tắc phải dùng khi đánh giá

1. Role hiện tại có **ADMIN, MANAGER, STAFF, USER**. STAFF là nhân viên riêng, làm việc tại `/staff/orders`; xem quy định và kịch bản mới nhất trong `PHAN_QUYEN_CUA_HANG.md`. Không đăng ký/chuyển sang SHIPPER; `/shipper/**` đã bị chặn.
2. Tài khoản có vai trò cố định. Admin tạo tài khoản riêng theo vai trò, không dùng luồng yêu cầu nâng quyền cũ. ADMIN không mua hàng; trang cart/checkout/customer yêu cầu USER.
3. Giao diện checkout hiện đưa ra **COD, MOMO_DEMO và WALLET_DEMO khi thanh toán mô phỏng được bật**, chỉ COD khi tắt. Ví phải đủ tiền mới được chọn. `BANK_TRANSFER` bị từ chối khi tạo đơn qua checkout. Backend còn luồng BANK_TRANSFER/MOMO để hỗ trợ dữ liệu cũ; xem mục test tùy điều kiện.
4. Đặt thành công trừ kho ngay, kể cả COD. Đặt lỗi rollback. Hủy trước giao hoàn kho đúng một lần về chi nhánh trên đơn.
5. Mẫu riêng chỉ bán ở chi nhánh sở hữu. Mẫu chung có thể bán tại chi nhánh có kho hợp lệ, đang hoạt động, đang mở và cho phép bán mẫu đó. Checkout hiện tìm **một chi nhánh đáp ứng toàn bộ dòng đã chọn**, không mặc định tự tách đơn theo nhiều chi nhánh.
6. Khách hủy tại PENDING/CONFIRMED, không có giới hạn một giờ; từ PREPARING bị chặn. Quản lý/admin có hủy ngoại lệ khi PREPARING, bắt buộc lý do 1–300 ký tự sau bỏ khoảng trắng.
7. Hủy đơn đã trả tiền thường chuyển REFUND_REQUESTED để xác nhận hoàn tiền sau. **Với MOMO_DEMO hoặc WALLET_DEMO, khách tự hủy trước chuẩn bị được hoàn tự động vào ví riêng của chính khách**, thành REFUNDED. Quản lý/admin hủy vẫn đi luồng chờ hoàn; xác nhận hoàn xong mới cộng ví và ghi REFUNDED. Không hoàn vào MoMo/ngân hàng thật cho các phương thức mô phỏng.
8. MoMo demo trả tiền vào **số dư chờ**, chưa vào ví khả dụng. Giao thành công/hoàn tất nhận tại quầy mới kết chuyển thu nhập sau hoa hồng. Không có giao dịch MoMo thật.
9. Xóa sản phẩm tại chi nhánh là xóa mềm, giữ ảnh/kho/lịch sử. Khôi phục về Ngừng bán. Không xóa khi còn đơn đang xử lý.
10. Giá là đồng nguyên, tối thiểu 1đ; khuyến mãi là số tiền, không phải phần trăm. Khuyến mãi không vượt giá bán. Mỗi mẫu có một danh mục chính và tối đa 10 danh mục bổ sung.
11. Báo cáo mặc định 7 ngày, gồm hôm nay, theo UTC+7. KPI tồn kho độc lập kỳ báo cáo; tổng kho quản trị có thể gồm hàng ngừng bán/chi nhánh tạm đóng, khác số lượng công khai được phép bán.
12. MANAGER phụ trách một cửa hàng; STAFF được phân công một cửa hàng và không có quyền quản lý ví, sản phẩm, nhân sự, hoàn tiền hoặc hủy đơn từ cửa hàng. Không dùng mô hình đối tác MANAGER nhiều chi nhánh trong tài liệu cũ làm kỳ vọng mới.
13. Ví khách và ví cửa hàng là hai loại ví khác nhau. Ví khách chỉ nhận hoàn tiền mô phỏng và thanh toán đơn; không có nạp, rút hoặc chuyển tiền. Số dư hiển thị không phải số dư ví MoMo thật.
14. Ngừng bán giữ trạng thái được duyệt nhưng chặn đơn mới; Cấm kinh doanh thu hồi trạng thái được duyệt, chặn cả xem chi tiết công khai. Hai thao tác không tự hủy hoặc hoàn tiền đơn cũ.
15. `mvn spring-boot:run` mặc định chọn Aiven và tự khởi động Cloudflare cho cổng thanh toán khi đủ công cụ/cấu hình. Chỉ các đường dẫn được phép của cổng thanh toán được công khai, không phải toàn bộ website.

## 2. Chuẩn bị trước khi test

### 2.1 Môi trường và tài khoản

- Dùng database test riêng hoặc bản sao database. **Aiven chung của nhóm không mặc nhiên là database test.** Ghi lại tên database, commit, profile, giờ bắt đầu. Các ca khóa tài khoản/xóa/reset/migration, đặt/hủy đơn, hoàn tiền, đổi kho và rút tiền chỉ chạy trên dữ liệu test được phép thay đổi.
- Base URL mặc định: `http://localhost:8080`. Trong bảng, đường dẫn bắt đầu `/` được nối vào base URL này. ID dùng ID thật lấy từ giao diện, không giả định luôn bằng 1.
- Với môi trường Aiven đã thiết lập, chạy `mvn spring-boot:run` sau khi người phụ trách đã kiểm tra cấu hình trỏ tới database test; không chỉ dựa tên profile để kết luận đang dùng DB test. File `.env.aiven` là cấu hình riêng, không đưa vào Git hoặc bằng chứng. Nếu test SQL Server, chọn cấu hình tương ứng riêng; không bật đồng thời `aiven`, `dev`, `test`. Không chạy migration/reset để chỉ lập tài liệu.
- Mở các **profile trình duyệt hoặc BrowserContext độc lập** cho C1, C2, M1, M2, NV1, NV2, A; các tab ẩn danh chung một cửa sổ có thể dùng chung cookie nên không đủ để tách tài khoản.
- Mở DevTools: Network bật Preserve log; Console theo dõi lỗi. Ca gửi request trực tiếp dùng Network → Copy as fetch hoặc Postman. Giữ đúng CSRF cho request session, trừ ca cố ý thiếu/sai token.
- OTP cần hộp thư test bạn kiểm soát và mail được cấu hình, hoặc mock rõ ràng trong môi trường cô lập. Ảnh cần Cloudinary test; không xóa ảnh đang dùng. Test điện thoại cần Node.js và cloudflared; đợi `Demo phone payment ready` và dùng QR mới, không dùng URL tunnel cũ.

| Ký hiệu | Tài khoản / dữ liệu cần có |
|---|---|
| A | ADMIN đang hoạt động |
| M1 | MANAGER, chủ chi nhánh S1 |
| M2 | MANAGER, chủ S2, không được phân quyền S1 |
| NV1, NV2 | Hai STAFF độc lập, lần lượt phụ trách S1 và S2 |
| NV0 | STAFF chưa phân công cửa hàng |
| MN | MANAGER chưa có chi nhánh |
| C1, C2 | Hai USER đang hoạt động, hồ sơ/giỏ/địa chỉ riêng |
| CX | USER dành riêng để khóa/xóa thử |
| S1, S2 | Hai chi nhánh hoạt động và mở cửa; có đơn vị/biểu phí giao hàng phù hợp |

Không mặc định tên tài khoản hoặc mật khẩu seed còn dùng được. Tạo tài khoản test theo từng vai trò bằng chức năng hiện có, ghi lại username nhưng không ghi mật khẩu vào tài liệu/báo cáo. C2/NV1/NV2/NV0/MN/CX phải là tài khoản test độc lập. Không dùng tài khoản SHIPPER cũ làm điều kiện test vai trò đang được hỗ trợ.

### 2.2 Bộ dữ liệu có thể tính tay

Tạo mẫu có tiền tố `TEST_<ma_luot_chay>_` để tìm lại; chạy từng ca với dữ liệu được phục hồi về tiền điều kiện, không nối tùy tiện các ca làm thay đổi kho.

| Ký hiệu | Thiết lập |
|---|---|
| P1 | Mẫu riêng S1, duyệt + mở bán, giá 120.000đ, khuyến mãi 100.000đ, kho S1 = 10 |
| P2 | Mẫu riêng S2, giá 150.000đ, kho S2 = 5 |
| PG | Mẫu chung, giá 200.000đ; kho S1 = 3, S2 = 7; cho phép bán ở cả hai |
| P0 | Mẫu đã duyệt/mở bán nhưng kho = 0 |
| PB | Mẫu cồng kềnh để kiểm phụ phí; xem cách nhận diện thực tế ở `OrderServiceImpl.isBulkyProduct` |
| PH | Mẫu dành riêng để lần lượt ngừng bán, xóa mềm, chờ duyệt, cấm |
| V10 | PERCENT 10%, còn lượt, đúng kỳ, không vướng giá nhập; dùng toàn sàn |
| VF | FIXED_AMOUNT 30.000đ; ngưỡng đơn 200.000đ |
| VS | FREE_SHIP; giới hạn giảm ship 20.000đ |
| V1/V2 | Voucher riêng S1/S2 để thử sai chi nhánh |
| DX | Đơn vị giao hoạt động; biểu phí S1 gần 20.000đ, xa 40.000đ, phụ phí cồng kềnh 10.000đ |
| W | Ghi số dư ví khả dụng và số dư chờ của S1 trước ca; không giả định bằng 0 |
| WC1 / WC2 | Ví khách C1 / C2; dùng hoàn tiền hợp lệ hoặc fixture test được kiểm soát để chuẩn bị 0đ, 99.999đ, 100.000đ và 200.000đ; không tạo chức năng nạp tiền mới để phục vụ test |
| O1 / O2 | Đơn test riêng của C1 / C2; ghi phương thức, trạng thái, chi nhánh, hạn thanh toán và tổng tiền trước thao tác |

Nếu UI không cho đặt đúng một thông số fixture, dùng dữ liệu tương đương đang có và tính lại kỳ vọng. Ca cần dữ liệu di sản/lỗi DB ghi **BLOCKED – thiếu fixture**, không tự sửa database chính để ép chạy.

### 2.3 Cách ghi kết quả

P0 = lỗi làm sai tiền/kho/quyền hoặc chặn luồng mua chính; P1 = chức năng chính/validation; P2 = trình bày và trường hợp ít gặp. Mỗi biến thể trong một ô phải chạy riêng và ghi hậu tố, ví dụ `AUTH-03.a`, `.b`.

Trạng thái: `NOT RUN`, `PASS`, `FAIL`, `BLOCKED`, `N/A`. N/A phải ghi lý do (ví dụ không bật thanh toán cũ). BLOCKED không được tính PASS. Khi lỗi xảy ra, ghi bước tái hiện, dữ liệu, ảnh, request/response, ID đơn, kho và ví trước/sau; che token, cookie, OTP và thông tin riêng tư.

| ID ca / biến thể | Người test | Ngày giờ UTC+7 | Dữ liệu / ID đơn | Thực tế | Trạng thái | Bằng chứng / mã lỗi |
|---|---|---|---|---|---|---|
| … | … | … | … | … | NOT RUN | … |

## 3. Mười hai kịch bản xuyên suốt nên chạy đầu tiên

### E2E-01 — COD giao tận nơi (P0)

Tiền điều kiện: C1; P1 kho 10; S1 mở; DX gần 20.000đ; không voucher. Ghi ví W và điểm S1.

1. C1 tìm P1 → chọn số lượng 2 → thêm giỏ → chọn đúng dòng → checkout.
2. Chọn giao tận nơi, nhập tên/SĐT Việt Nam/địa chỉ, ngày giờ tương lai, DX, vùng gần, COD.
3. Kiểm báo giá: hàng 200.000đ + ship 20.000đ = **220.000đ**. Đặt đúng một lần.
4. Kiểm đơn PENDING/UNPAID, đúng S1, đúng 2 sản phẩm, đúng người nhận/lịch nhận. Dòng đã chọn rời giỏ; dòng chưa chọn còn nguyên. Kho P1 = **8 ngay sau khi đặt**; ví chưa tăng.
5. M1 mở đơn → CONFIRMED → PREPARING (đọc và xác nhận cảnh báo khách mất quyền tự hủy) → DELIVERING → DELIVERED → COMPLETED.
6. Sau DELIVERED: COD được ghi PAID; ghi thu nhập một lần. Nếu hoa hồng 10%, ví tăng **198.000đ**, điểm S1 tăng `floor(220000/10000) = 22`. Từ DELIVERED sang COMPLETED không cộng thêm ví/điểm, không trừ thêm kho.
7. C1 tải lại chi tiết; đối chiếu trạng thái, tổng tiền, thông báo; A đối chiếu đơn/giao dịch/báo cáo trong kỳ chứa ngày tạo đơn.

### E2E-02 — COD nhận tại cửa hàng (P0)

Tiền điều kiện: PG kho S1 = 3, S2 = 7, cả hai mở. C1 chọn một PG.

1. Checkout → PICKUP → chọn S2 → ngày giờ tương lai → COD.
2. Tổng **200.000đ**, ship 0; địa chỉ đơn là địa chỉ nhận ở S2. Kho S2 giảm còn 6, S1 vẫn 3.
3. M2: CONFIRMED → PREPARING → “Khách nhận hoa tại quầy – Hoàn tất”.
4. Đơn COMPLETED/PAID; không có bước đang giao/đã giao. Ví/điểm S2 tăng một lần, S1 không đổi.

### E2E-03 — MoMo demo và kết chuyển tiền chờ (P0)

Tiền điều kiện: môi trường cô lập, thanh toán mô phỏng bật; P1 kho 10; hoa hồng S1 = 10%; ghi W khả dụng và H chờ.

1. C1 mua một P1, PICKUP S1, MOMO_DEMO, tổng **100.000đ**.
2. Đặt xong: kho 9, đơn UNPAID, W/H chưa tăng. Trang QR ghi rõ mô phỏng.
3. Dùng điện thoại hoặc trang xác nhận của QR → xác nhận thanh toán. Máy tính phải nhận kết quả thành công.
4. Đơn PAID; H tăng **100.000đ**, W chưa tăng. Xác nhận QR lần nữa không tăng thêm H, không có thanh toán thứ hai.
5. M1 xác nhận → chuẩn bị → khách nhận tại quầy/hoàn tất. H về mức ban đầu; W tăng **90.000đ**; phí sàn 10.000đ; có dấu vết PAYMENT_HOLD/HOLD_RELEASE/ORDER_PAYMENT/COMMISSION_FEE tương ứng.
6. Tải lại và gửi lại bước hoàn tất: tiền, điểm, giao dịch và kho không thay đổi thêm.

### E2E-04 — Khách hủy COD trước chuẩn bị (P0)

1. C1 đặt P1 số lượng 2 từ kho 10 → kho 8.
2. M1 chỉ CONFIRMED. C1 bấm hủy → xác nhận.
3. Đơn CANCELLED, kho về 10, ví không tăng. Tải lại/gửi lại hủy không làm kho thành 12.
4. Lặp với đơn ở PENDING; kết quả tương tự. Với fixture CONFIRMED tạo hơn một giờ trước, vẫn cho hủy nếu chưa PREPARING.

### E2E-05 — Khách hủy MoMo demo đã trả tiền (P0)

1. Đặt một P1 và trả MoMo demo; kho từ 10 thành 9, H tăng 100.000đ, W không đổi.
2. Ghi WC1, WC2; khi PENDING hoặc CONFIRMED, C1 chọn lý do hủy hợp lệ và xác nhận.
3. Đơn **REFUNDED**, payment REFUNDED; kho về 10; H về ban đầu; W không bị trừ vì chưa nhận thu nhập. **WC1 tăng đúng 100.000đ**, có một dòng REFUND liên kết đơn; WC2 không đổi. Khách đọc được lý do và nơi nhận tiền.
4. Lặp hủy/QR cũ không tạo thêm hoàn tiền hoặc phục hồi đơn. Đơn phải xem được trong các bộ lọc hủy/hoàn tương ứng. Không dùng kỳ vọng “chờ hoàn tiền” của thanh toán thường cho ca này.

### E2E-06 — Hủy ngoại lệ bởi quản lý/admin (P0)

1. C1 tạo và trả MoMo demo một P1. M1 chuyển tới PREPARING.
2. C1 thử hủy từ trang cũ và request trực tiếp: bị từ chối; kho/H/trạng thái giữ nguyên.
3. M1 chọn hủy ngoại lệ: thử lý do trống/khoảng trắng/>300 ký tự, đều bị từ chối. Nhập `Khách đề nghị đổi ngày, cửa hàng không thể đáp ứng` → xác nhận.
4. Đơn REFUND_REQUESTED, kho trả một lần, H vẫn giữ đến khi hoàn tiền, W chưa tăng; C1 thấy đúng lý do và người hủy.
5. Ghi WC1; M1 xác nhận hoàn tiền → REFUNDED, H giảm đúng 100.000đ, WC1 tăng đúng 100.000đ. Lý do hủy ban đầu vẫn đọc được. Lặp xác nhận không cộng ví/trừ tiền hai lần.
6. Lặp bằng A, nội dung phải ghi người hủy là admin.

### E2E-07 — Sản phẩm và trạng thái chi nhánh (P0)

1. M1 tạo mẫu riêng S1 với giá/kho/ảnh/danh mục. Nếu mẫu chưa duyệt, A duyệt trước khi thử mua.
2. C1 thêm vào giỏ; M1 ngừng bán → C1 checkout từ trang cũ bị chặn, kho/đơn không thay đổi.
3. Khi không còn đơn xử lý, M1 xóa → mẫu vào Đã xóa, ảnh/lịch sử/kho còn. Khôi phục → vào Ngừng bán; chủ động mở bán → khách mua được.
4. Lặp xóa PG ở S1: PG vẫn bán ở S2. M2 không sửa được mẫu riêng S1 dù cố đổi ID URL.

### E2E-08 — Chat đúng chi nhánh và giám sát (P0)

1. C1 mở P1 → liên hệ S1 → gửi nội dung tiếng Việt và ảnh hợp lệ. M1 nhận và trả lời realtime.
2. M2 ở S2 và C2 không đọc/gửi/nhận được hội thoại này qua sửa ID hoặc subscribe queue.
3. A mở giám sát: xem được lịch sử, xuất hiện nhật ký truy cập, không được trả lời thay S1.
4. C1 mở chat admin; M1 không thấy hội thoại khách–admin. M1 mở hỗ trợ nội bộ S1 tới admin; C1 không thấy kênh nội bộ.

### E2E-09 — Ví và rút tiền (P0)

1. Đảm bảo W khả dụng ≥100.000đ. M1 nhập 50.000đ, ngân hàng, STK 6–30 chữ số, chủ tài khoản và mật khẩu đúng.
2. Gửi: W giảm 50.000đ ngay, tạo WITHDRAWAL/PENDING, H không đổi.
3. A từ chối yêu cầu: W được trả 50.000đ một lần. Gửi lại xử lý không cộng thêm.
4. Tạo yêu cầu mới; A xác nhận đã chuyển: COMPLETED, W không giảm lần hai. NV1/M2 không được tự rút ví S1.

### E2E-10 — Giành đơn vị hàng cuối cùng (P0)

1. Đặt P1 kho = 1, hai tài khoản C1/C2 mỗi người có một sản phẩm trong giỏ. Mở checkout trước ở cả hai.
2. Cùng bấm đặt COD trong khoảng sát nhau; lặp vài lần với fixture mới để tăng khả năng chồng request.
3. Chỉ một đơn đặt thành công, kho = 0; người còn lại nhận lỗi hết/không đủ hàng; không có kho âm hoặc đơn mồ côi.
4. Người thắng hủy trước chuẩn bị: kho về 1 đúng một lần. Người còn lại thử lại và mua được.

### E2E-11 — Dùng tiền hoàn trong ví để mua đơn mới (P0)

Tiền điều kiện: C1 có ví 200.000đ từ một khoản hoàn hợp lệ; C2 có ví riêng; P1 kho 10, nhận tại quầy 100.000đ.

1. C1 chọn P1 và WALLET_DEMO; kiểm UI hiện số dư 200.000đ, đủ để chọn.
2. Đặt một lần: chỉ một đơn PAID, ví C1 còn 100.000đ, có một dòng PAYMENT; kho còn 9; tiền chờ S1 tăng 100.000đ, tiền khả dụng chưa tăng.
3. C1 hủy trước chuẩn bị, nhập lý do hợp lệ: ví về 200.000đ, có một REFUND; kho về 10; tiền chờ về trước đơn; C2 không đổi.
4. Gửi lại yêu cầu từ trang cũ: không cộng tiền/kho lần nữa. Dùng WC1=99.999đ thử đơn tương tự: UI vô hiệu ví và server từ chối request giả, không tạo đơn hoặc trừ kho.

### E2E-12 — Nhân viên hoàn tất đơn đúng chi nhánh (P0)

Tiền điều kiện: NV1 ở S1, NV2 ở S2; C1 có một đơn COD nhận tại quầy và một đơn COD giao tận nơi ở S1.

1. NV1 đăng nhập, tìm đúng đơn, mở chi tiết và đối chiếu người nhận, lịch, lời chúc, các dòng hàng.
2. Đi đúng thứ tự Xác nhận → Chuẩn bị; trước chuẩn bị phải có cảnh báo khách mất quyền tự hủy.
3. Đơn quầy: xác nhận đã giao hoa và thu đủ COD → Hoàn tất. Đơn giao: bàn giao → đang giao → xác nhận đã giao/thu đủ COD → hoàn tất.
4. Kiểm PAID, thu nhập và điểm đúng một lần; gửi lặp không thu thêm. NV2 không xem/sửa được các đơn này.
5. NV1 thử hủy từ cửa hàng, xác nhận chuyển khoản, hoàn tiền và rút ví: đều bị chặn; việc cần quyền cao hơn chuyển M1/A xử lý.

## 4. Danh sách ca chi tiết

Mỗi dòng dưới đây kế thừa tiền điều kiện của nhóm và bộ fixture mục 2. Cột “Thao tác” là các bước theo thứ tự. Mỗi biến thể phân cách bằng dấu `;` hoặc ` / ` được thực hiện độc lập khi ghi kết quả.

### 4.1 Khởi động và trang công khai — PUB

Tiền điều kiện: website chạy trên database test; đăng xuất cho ca guest. Trang chính `/`, `/stores`, `/users`, `/products/{id}`.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| PUB-01 | P0 | Mở `/` sau khởi động, vào sản phẩm/cửa hàng/người dùng | Không 500; tài nguyên chính tải được, tiếng Việt đúng |
| PUB-02 | P1 | Tìm đúng tên P1; một phần tên; từ không có kết quả; từ có dấu | Kết quả phù hợp, rỗng có thông báo, không lỗi trang |
| PUB-03 | P1 | Chọn danh mục chính rồi danh mục bổ sung có PG | Đều tìm thấy PG, không lặp PG trong cùng trang hoặc làm sai tổng mẫu |
| PUB-04 | P1 | Kết hợp loại hoa/màu/kiểu dáng/dịp/phong cách có dữ liệu giao nhau | Chỉ còn mẫu phù hợp các nhóm đang chọn; bỏ lọc khôi phục kết quả |
| PUB-05 | P1 | Lọc khoảng giá chứa/không chứa P1, kiểm sát biên 100.000đ | Giá lọc nhất quán với giá bán hiệu lực đang hiển thị |
| PUB-06 | P1 | Nhập min > max; giá âm; ký tự; giá rất lớn qua query | Không 500/SQL error; từ chối hoặc chuẩn hóa rõ ràng |
| PUB-07 | P1 | Lần lượt chọn các cách sắp xếp và mức sao có trên UI | Thứ tự/điều kiện đúng, không mất bộ lọc đang chọn |
| PUB-08 | P1 | Chọn riêng S1 rồi S2, xem P1/P2/PG | Mẫu riêng đúng shop; mẫu chung theo tồn kho/trạng thái từng shop |
| PUB-09 | P1 | Chuyển trang với nhiều bộ lọc; query page=0/-1/rất lớn | Không lặp/bỏ dữ liệu hợp lệ do join; page sai được xử lý, không 500 |
| PUB-10 | P1 | Mở chi tiết P1, xem ảnh/giá/danh mục/thuộc tính/review/liên hệ | Đúng mẫu, giá 100.000đ với fixture, không hiển thị giá khuyến mãi như % |
| PUB-11 | P0 | Mở trực tiếp ID PH khi bị cấm/xóa; thử thêm giỏ | Không mua được mẫu không hợp lệ dù biết URL cũ |
| PUB-12 | P1 | Mở ID sản phẩm/cửa hàng/người dùng không tồn tại; ID sai kiểu | Có lỗi/chuyển hướng phù hợp, không lộ stack trace/SQL/secret |
| PUB-13 | P1 | Tìm cửa hàng, mở gian hàng S1 và S2 | Tên/ảnh/địa chỉ/rating và danh sách hàng đúng chi nhánh |
| PUB-14 | P1 | Tìm người dùng và mở hồ sơ công khai của C1 | Đúng thông tin công khai; không lộ mật khẩu, OTP, token, giỏ/địa chỉ riêng |
| PUB-15 | P1 | Guest bấm mua, yêu thích, theo dõi hoặc chat | Yêu cầu đăng nhập; không tạo dữ liệu thuộc tài khoản khác |
| PUB-16 | P2 | Tìm bằng chuỗi HTML `<b>TEST</b>` và dấu nháy | Chỉ là văn bản; không thực thi HTML/script, không lỗi truy vấn |

### 4.2 Đăng ký, OTP, đăng nhập — AUTH

Tiền điều kiện: hộp thư test nhận OTP; trang `/register`, `/verify-otp`, `/login`, `/forgot-password`, `/reset-password`. Giới hạn OTP hiện là 5 phút, tối đa 5 lần nhập sai; gửi lại có giới hạn 3 lần trong cửa sổ 10 phút tùy luồng.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| AUTH-01 | P0 | Đăng ký username/email mới, dữ liệu hợp lệ → lấy OTP từ hộp thư → xác thực → đăng nhập | Tạo đúng một USER hoạt động và giỏ của user; không thành MANAGER/ADMIN |
| AUTH-02 | P1 | Đăng ký rồi chưa xác thực; thử đăng nhập | Chưa được dùng như tài khoản đã kích hoạt; không tạo đơn |
| AUTH-03 | P1 | Username 3/4/50/51 ký tự; có khoảng trắng/tiếng Việt/ký tự lạ | Biên 4–50 và tập chữ số/chữ ASCII/`_`/`.` được áp dụng |
| AUTH-04 | P1 | Username trùng; email trùng; gửi hai lần cùng đăng ký | Báo lỗi thích hợp, không có hai account/giỏ cho cùng đăng ký |
| AUTH-05 | P1 | Bỏ từng trường bắt buộc, nhập toàn khoảng trắng | Có thông báo trường thiếu; không gửi đăng ký thành công |
| AUTH-06 | P1 | Mật khẩu 5/6 ký tự; chuỗi vượt 72 byte UTF-8 | Mật khẩu dưới mức tối thiểu bị chặn; dữ liệu vượt giới hạn bcrypt không được chấp nhận âm thầm |
| AUTH-07 | P1 | Họ tên 1/2/100/101 ký tự; email sai định dạng/>100 ký tự | Validation đúng biên, không cắt dữ liệu âm thầm |
| AUTH-08 | P1 | SĐT `0901234567`, `+84901234567`; 9/11 số sai; đầu 01; chữ | Chấp nhận định dạng Việt Nam được hỗ trợ; chặn sai |
| AUTH-09 | P1 | Thiếu tỉnh/quận; đổi tỉnh sau khi chọn quận; địa chỉ có dấu | Không lưu tổ hợp địa chỉ sai; địa chỉ lưu đúng dữ liệu đã chọn |
| AUTH-10 | P0 | Nhập OTP đúng; OTP sai; rỗng; không đủ 6 số | Chỉ mã hợp lệ kích hoạt; thông báo rõ cho mã sai |
| AUTH-11 | P0 | Đợi mã hết 5 phút, gửi mã cũ | Bị từ chối, không kích hoạt/đổi mật khẩu |
| AUTH-12 | P0 | Nhập sai OTP 5 lần rồi nhập đúng mã cũ | Mã bị khóa, yêu cầu mã mới; không vượt khóa bằng tải lại |
| AUTH-13 | P1 | Gửi lại OTP, thử mã trước rồi mã mới | Mã mới dùng được; mã bị thay thế không dùng được |
| AUTH-14 | P1 | Gửi lại quá giới hạn; reload; thử lại trong thời gian chờ | Không vượt giới hạn bằng reload; thông báo giới hạn gửi |
| AUTH-15 | P0 | Dùng OTP C1 cho C2; dùng OTP reset cho kích hoạt; dùng lại mã đã tiêu thụ | Đều bị từ chối, không sửa nhầm account |
| AUTH-16 | P1 | Làm mail gửi thất bại trong môi trường test rồi đăng ký/gửi lại | Không báo gửi thành công giả; không lộ OTP/secret; gửi lại lỗi không làm mất mã cũ còn hiệu lực |
| AUTH-17 | P0 | Đăng nhập lần lượt A/M1/C1 bằng username và email hợp lệ | Đúng tài khoản, đúng vai trò và màn hình sau đăng nhập |
| AUTH-18 | P1 | Đăng nhập sai mật khẩu → tải lại trang lỗi | Giữ đúng username/email vừa nhập; mật khẩu không được website điền lại; URL không chứa mật khẩu |
| AUTH-19 | P1 | Ở trang lỗi sửa username hoặc chọn demo account trong khi script phục hồi chạy | Không bị ghi đè lựa chọn mới của người dùng |
| AUTH-20 | P1 | Sai login → login thành công/logout → mở login mới không có lỗi | Thông tin lần thử cũ được xóa |
| AUTH-21 | P0 | Login bằng account bị khóa/chưa kích hoạt/tài khoản shipper cũ | Không vào được chức năng bảo vệ |
| AUTH-22 | P0 | Đăng xuất bằng nút → dùng Back và gửi lại thao tác cũ | Phiên cũ không thực hiện được thao tác bảo vệ; không chỉ ẩn UI |
| AUTH-23 | P1 | Quên mật khẩu bằng email rồi bằng username có thật | OTP gửi vào email của đúng account, không dùng người nhận cố định |
| AUTH-24 | P1 | Quên mật khẩu với account không tồn tại; dữ liệu rỗng | Có phản hồi phù hợp, không đổi account nào |
| AUTH-25 | P0 | Reset bằng OTP đúng và mật khẩu hợp lệ → thử login mật khẩu cũ/mới | Cũ thất bại, mới thành công; OTP không dùng lại được |
| AUTH-26 | P0 | POST reset thiếu/sai/đã dùng OTP, hoặc vào thẳng URL reset | Không đổi được mật khẩu bằng cách bỏ bước OTP |
| AUTH-27 | P1 | Trang login lỗi với username chứa nháy/HTML | Giá trị được escape, không chạy mã khi phục hồi input |

### 4.3 Hồ sơ, địa chỉ, yêu thích — ACC

Tiền điều kiện: C1/C2 có phiên độc lập. Trang `/customer/profile`, `/customer/profile/wishlist`, `/customer/profile/followed-stores`, `/customer/profile/recently-viewed`, `/following`.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| ACC-01 | P1 | Đổi họ tên/SĐT/địa chỉ hợp lệ → lưu → reload/login lại | Dữ liệu đúng và bền vững; không đổi role/username ngoài quyền |
| ACC-02 | P1 | Gửi rỗng, sai SĐT, giá trị quá dài cho hồ sơ | Báo validation, không phá dữ liệu cũ |
| ACC-03 | P1 | Đổi avatar rồi cover bằng ảnh hợp lệ → reload | Đúng ô ảnh được thay; các ảnh khác không đổi nhầm |
| ACC-04 | P1 | Đổi ảnh với file lỗi/quá lớn/Cloudinary lỗi | Báo lỗi; ảnh cũ còn dùng được, không lưu URL rỗng |
| ACC-05 | P0 | Xin OTP đổi mật khẩu → nhập OTP đúng và mật khẩu hợp lệ | Đổi thành công; mật khẩu cũ không dùng được |
| ACC-06 | P0 | Đổi mật khẩu thiếu/sai/hết hạn OTP; OTP account khác | Không đổi được mật khẩu |
| ACC-07 | P1 | Thêm địa chỉ A1/A2, sửa A2, reload | Đúng tên/SĐT/địa chỉ; không mất địa chỉ còn lại |
| ACC-08 | P1 | Chọn mặc định A1 rồi A2 → mở checkout | Chỉ có một mặc định, checkout lấy đúng địa chỉ mặc định hiện tại |
| ACC-09 | P1 | Xóa địa chỉ thường; xóa địa chỉ mặc định/cuối cùng | Không lỗi trang hoặc tham chiếu hỏng; trạng thái mặc định còn lại nhất quán |
| ACC-10 | P0 | C1 sửa/xóa/set-default bằng ID địa chỉ C2 | Bị chặn; dữ liệu C2 không đổi |
| ACC-11 | P1 | Thêm/sửa địa chỉ thiếu tên/SĐT/địa chỉ; SĐT sai | Không lưu địa chỉ không dùng được |
| ACC-12 | P1 | Yêu thích P1 → mở wishlist → bỏ thích → reload | Toggle đúng, không trùng bản ghi, không ảnh hưởng C2 |
| ACC-13 | P1 | Theo dõi S1 từ gian hàng và từ hồ sơ → bỏ theo dõi | Hai điểm vào nhất quán; danh sách following cập nhật đúng |
| ACC-14 | P1 | Xem P1, PG, rồi P1 lại → mở đã xem gần đây | Đúng lịch sử C1; không lộ lịch sử C2, không nhân trùng vô hạn |
| ACC-15 | P1 | Yêu thích/theo dõi ID không tồn tại hoặc đã ngừng hoạt động | Báo/chặn thích hợp; không tạo dữ liệu hỏng hoặc 500 |

### 4.4 Giỏ hàng — CART

Tiền điều kiện: C1 có giỏ riêng, P1/PG bán được. Trang `/cart`. Các thay đổi kho được ghi trước/sau.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| CART-01 | P0 | Thêm P1 từ trang chủ bằng AJAX và từ chi tiết | Đúng sản phẩm/số lượng, badge cập nhật, không cần reload để thấy phản hồi |
| CART-02 | P1 | Thêm lại P1 với cùng lựa chọn | Gộp hoặc hiển thị theo quy tắc giỏ, tổng số lượng đúng, không nhân đôi ngoài ý muốn |
| CART-03 | P1 | Tăng/giảm số lượng, nhập trực tiếp, reload | Tổng dòng/tổng giỏ/badge đồng bộ, dữ liệu được lưu |
| CART-04 | P0 | Số lượng 0/-1/thập phân/chữ/vượt kho; gửi trực tiếp bỏ qua UI | Bị từ chối hoặc chuẩn hóa rõ; không lưu số lượng âm/sai và không vượt kiểm tra checkout |
| CART-05 | P0 | Thêm P0; thêm PH bị cấm/ngừng/xóa; ID không có | Không thêm sản phẩm không bán được |
| CART-06 | P1 | Xóa một dòng; xóa dòng cuối; tải lại | Xóa đúng, tổng/badge cập nhật, giỏ rỗng có hướng dẫn |
| CART-07 | P0 | C1 sửa/xóa dòng giỏ C2 bằng ID | Bị chặn, giỏ C2 nguyên vẹn |
| CART-08 | P1 | Chọn một trong hai dòng → thanh toán | Chỉ dòng chọn đi checkout; dòng không chọn còn trong giỏ sau đặt thành công |
| CART-09 | P1 | Không chọn dòng nào rồi bấm thanh toán ở UI | Có nhắc chọn, không đặt nhầm toàn bộ giỏ |
| CART-10 | P1 | Mở checkout khi giỏ rỗng; selectedItemIds không thuộc giỏ | Quay về giỏ/báo lỗi, không tạo đơn |
| CART-11 | P0 | M1 đổi giá P1 khi C1 đang mở giỏ → báo giá/đặt | Dùng giá hiệu lực phía server, không tin giá cũ do client gửi |
| CART-12 | P1 | Hai tab C1 thay số lượng/xóa dòng, quay lại tab kia | Badge/trạng thái cuối khớp server; không hồi sinh dòng đã xóa |
| CART-13 | P1 | Ngắt mạng khi thêm/cập nhật rồi nối lại và thử lại | Có phản hồi lỗi; không tăng số lượng âm thầm do retry ngoài ý muốn |
| CART-15 | P1 | Giỏ có 1 mẫu đang bán số lượng 2 và 1 mẫu ngừng bán số lượng 5; sau đó mẫu còn lại hết hàng | Dòng Chọn tất cả chỉ hiện 1 mục, tổng số lượng 2; khi hết hàng hiện 0/0 và không đặt được; mẫu ngừng bán vẫn hiện để xóa |
| CART-14 | P0 | Giả `orderType=RENT` hoặc tham số thuê ở request giỏ/checkout | Luồng hiện tại chỉ mua hoa, không tạo đơn thuê mới |

### 4.5 Báo giá, chọn chi nhánh, checkout — CHK

Tiền điều kiện: C1, giỏ có fixture; trang `/checkout`, endpoint báo giá `POST /checkout/quote`. Mọi ca từ chối phải giữ kho, giỏ và số đơn không đổi, trừ khi đơn đã được lưu và chỉ lỗi bước mở thanh toán như CHK-22.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| CHK-01 | P0 | Đặt COD DELIVERY đủ thông tin | Đúng E2E-01; một đơn hợp lệ, trừ kho ngay |
| CHK-02 | P0 | Đặt COD PICKUP S1/S2 có đủ kho | Đúng E2E-02; ship 0, đúng chi nhánh đã chọn |
| CHK-03 | P1 | Đổi giao tận nơi ↔ nhận tại cửa hàng nhiều lần | Trường bắt buộc và phí thay đúng, không lẫn địa chỉ cũ hoặc tính ship cho pickup |
| CHK-04 | P0 | P1 riêng S1, địa chỉ gần S2 hơn → DELIVERY | Vẫn do S1 xử lý nếu hợp lệ; không trừ kho S2 |
| CHK-05 | P0 | P1 riêng S1 → cố chọn PICKUP S2 bằng request | Bị chặn dù S2 có dòng tồn kho cũ của P1 |
| CHK-06 | P0 | PG chung → chọn S2; rồi S2 hết hàng/đóng cửa | Thành công khi hợp lệ, bị chặn khi không đủ hàng/không nhận đơn |
| CHK-07 | P0 | Chọn đồng thời P1 riêng S1 và P2 riêng S2 | Không ép về một shop sai hoặc tự trừ kho chéo; báo không có chi nhánh đáp ứng |
| CHK-08 | P0 | Các dòng PG cộng lại vượt kho của mọi chi nhánh | Không chỉ kiểm từng dòng; từ chối toàn bộ, không tạo đơn một phần |
| CHK-09 | P1 | Thiếu tên/SĐT; tên toàn trắng; DELIVERY thiếu địa chỉ | Báo đúng thiếu trường, không tạo đơn |
| CHK-10 | P1 | Tên 100/101, địa chỉ 255/256, lời chúc 500/501 ký tự | Chấp nhận sát biên hợp lệ; từ chối vượt biên |
| CHK-11 | P1 | SĐT nội địa/+84; SĐT quá ngắn/chữ/chuỗi lẫn hai SĐT | Chuẩn hóa định dạng được hỗ trợ, từ chối giá trị mơ hồ/sai |
| CHK-12 | P1 | Không ngày/giờ; ngày hôm qua; hôm nay nhưng giờ đã qua | Bị chặn; ngày giờ tương lai được chấp nhận |
| CHK-13 | P1 | Chọn từng khung 07–09, 09–11, 11–13, 13–15, 15–17, 17–20; gửi khung giả | Khung hợp lệ được xử lý; khung giả bị chặn; kiểm tra thông tin giờ lưu trên đơn |
| CHK-14 | P1 | DELIVERY thiếu đơn vị; đơn vị bị tắt/xóa/không tồn tại | Bị từ chối; không báo miễn phí sai |
| CHK-15 | P1 | Chọn NEAR rồi FAR với biểu phí DX | Phí 20.000đ rồi 40.000đ với fixture; không tự suy ra cự ly thật từ địa chỉ |
| CHK-16 | P1 | Chọn OUTSIDE; gửi deliveryZone giả | Báo ngoài phạm vi/không hợp lệ, không đặt giao được |
| CHK-17 | P1 | Chọn PB cồng kềnh cùng DX; rồi thay bằng mẫu thường | Phụ phí 10.000đ chỉ cộng khi đúng điều kiện |
| CHK-18 | P1 | Hệ thống có biểu phí nhưng S1 không có biểu phù hợp/biểu bị tắt | Báo không cấu hình/không có biểu phí phù hợp; không fallback phí rẻ âm thầm |
| CHK-19 | P0 | Ghi báo giá rồi đặt ngay cùng dữ liệu không đổi | Tổng hàng + ship − giảm khớp đơn và payment đến từng đơn vị tiền |
| CHK-20 | P0 | Làm một dòng lỗi trong giỏ nhiều dòng rồi đặt | Không có đơn/kho/voucher bị ghi một phần; giỏ được giữ để sửa |
| CHK-21 | P1 | Checkout lỗi validation sau khi chọn dòng/nhập người nhận | Giữ lựa chọn/thông tin cần thiết để sửa, không tự chọn thêm dòng khác |
| CHK-22 | P0 | Mô phỏng lỗi tạo phiên MoMo sau khi đơn đã lưu | Báo đơn đã lưu, cho mở lại từ danh sách; không đặt lại tạo hai đơn |
| CHK-23 | P0 | Gửi paymentMethod lạ/BANK_TRANSFER; tắt demo rồi gửi MOMO_DEMO hoặc WALLET_DEMO | Phương thức không được hỗ trợ/bị gỡ/tắt bị chặn; không tạo đơn, không trừ ví/kho |
| CHK-24 | P0 | Gửi selectedItemIds lẫn ID C2 hoặc trùng một ID nhiều lần | Không mua dòng của C2; ID trùng không nhân số lượng tiền/kho |
| CHK-25 | P0 | Sửa subtotal/total/discount/branch bằng DevTools trước POST | Server tự tính và kiểm chi nhánh; không tin giá do client sửa |
| CHK-26 | P1 | Đổi vùng/ship/voucher liên tục khi mạng chậm | Kết quả báo giá cuối ứng với lựa chọn mới nhất, không bị response cũ ghi đè |
| CHK-27 | P0 | Kho thay đổi hoặc mẫu bị ngừng bán sau quote, trước đặt | Checkout kiểm lại; không bán vượt kho hoặc mẫu bị chặn |

### 4.6 Voucher — VOU

Tiền điều kiện: A quản lý `/admin/vouchers`, M1 quản lý `/manager/vouchers`; dùng checkout/quote để kiểm tác dụng thật. Giá nhập phải biết để kiểm giới hạn không bán dưới giá nhập.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| VOU-01 | P1 | A tạo/sửa voucher toàn sàn; M1 tạo/sửa voucher S1 | Lưu đúng phạm vi, mã/ngày/loại/giá trị/lượt |
| VOU-02 | P1 | Mã trống/trùng; ngày kết thúc trước bắt đầu; quantity=0/âm | Có validation, không tạo voucher hỏng |
| VOU-03 | P1 | PERCENT=0/100/>100/âm; ngưỡng/max âm; loại lạ | Không nhận giá trị ngoài quy tắc; kiểm biên trên form và backend |
| VOU-04 | P0 | Áp V10 cho hàng 200.000đ, giá nhập đủ thấp | Giảm 20.000đ trước giới hạn max; tổng server khớp quote |
| VOU-05 | P1 | Áp VF khi subtotal 199.999/200.000đ | Dưới ngưỡng bị từ chối; đúng ngưỡng áp được |
| VOU-06 | P1 | Voucher % có trần thấp; FIXED_AMOUNT lớn hơn subtotal | Giảm bị giới hạn đúng; tổng không âm |
| VOU-07 | P1 | VS với ship 40.000đ, max 20.000đ; rồi PICKUP | Giảm ship 20.000đ; pickup không tạo khoản giảm giả |
| VOU-08 | P0 | Giảm %/tiền khiến giá hàng xuống dưới tổng giá nhập | Mức giảm bị chặn tại lợi nhuận cho phép; FREE_SHIP vẫn áp theo ship |
| VOU-09 | P1 | Mã chưa tới ngày/hết hạn/tắt/hết lượt/không tồn tại | Bị từ chối, không tạo đơn giảm sai |
| VOU-10 | P0 | Áp V2 riêng S2 cho đơn S1; M2 sửa/xóa voucher S1 qua ID | Không áp sai chi nhánh, không sửa dữ liệu shop khác |
| VOU-11 | P1 | Nhập mã chữ thường và có khoảng trắng đầu/cuối | Chuẩn hóa mã, cùng voucher hợp lệ |
| VOU-12 | P0 | Xem quote nhiều lần rồi đặt thành công một lần; checkout thất bại | Quote không tiêu lượt; đặt thành công tiêu đúng lượt; lỗi rollback không mất lượt |
| VOU-13 | P1 | Tắt/xóa voucher khi C1 đang mở checkout | Báo giá/checkout kiểm lại, không dùng mã cũ bị vô hiệu hóa |

### 4.7 Thanh toán đang dùng — PAY

Tiền điều kiện: `/checkout` cho COD/MOMO_DEMO/WALLET_DEMO như cấu hình; xem thêm nhóm CW cho ví khách. MoMo demo có hạn 10 phút và token phiên lưu trong bộ nhớ tiến trình. Kiểm ID đơn, payment, ví khách, số dư chờ/khả dụng của cửa hàng, không chỉ nhìn màn hình thành công.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| PAY-01 | P0 | COD đặt xong, trước giao; sau giao/hoàn tất pickup | Ban đầu UNPAID, chưa tăng ví; đến mốc thu tiền mới PAID và cộng ví một lần |
| PAY-02 | P0 | MoMo demo đặt xong chưa quét/xác nhận | Kho đã giảm; payment PENDING/đơn UNPAID; chưa cộng H/W |
| PAY-03 | P0 | Xác nhận MoMo demo đúng token còn hạn | PAID/SUCCESS, tiền vào H, chưa vào W, khớp E2E-03 |
| PAY-04 | P0 | Nhấn xác nhận nhanh nhiều lần hoặc gửi lại request | Một payment/hold hợp lệ cho mỗi đơn; không tăng H nhiều lần |
| PAY-05 | P1 | Quét QR bằng điện thoại mạng phù hợp → xác nhận | URL điện thoại truy cập được, số tiền/ID đúng; máy tính cập nhật thành công |
| PAY-06 | P1 | Tunnel/LAN không vào được; mạng mất lúc polling | Có trạng thái lỗi/kết nối và cách thử lại; không tự đánh dấu đã trả tiền |
| PAY-07 | P0 | Token sai cấu trúc/không có; URL success tự gõ trước trả | Không ghi PAID và không báo thành công sai |
| PAY-08 | P0 | C2 mở URL chủ sở hữu `/customer/demo-payment/{token}` của C1 | Không đọc/chạy thao tác thuộc chủ tài khoản C1 |
| PAY-09 | P1 | Mở `/demo-payment/{token}` từ QR không có phiên đăng nhập | Được phép xác nhận đúng phiên demo qua token; token QR là quyền xác nhận, không dùng kỳ vọng phải login ở URL công khai này |
| PAY-10 | P0 | Chờ quá 10 phút rồi xác nhận QR | Không trả tiền cho phiên hết hạn; không hồi sinh đơn hoặc cộng H |
| PAY-11 | P0 | Sau hạn, đợi scheduler ít nhất một chu kỳ (~60 giây khi ít đơn) | Đơn UNPAID PENDING/CONFIRMED bị hủy, hoàn kho một lần; không áp cho COD |
| PAY-12 | P0 | C1 hủy đơn trước khi điện thoại xác nhận QR | Xác nhận bị vô hiệu cho đơn đã hủy; kho không bị trừ lại |
| PAY-13 | P0 | Khóa CX sau tạo QR rồi xác nhận bằng điện thoại | Không trả tiền bằng phiên/QR cũ của account bị khóa |
| PAY-14 | P1 | Restart server khi đang có QR chưa thanh toán | Token bộ nhớ cũ không dùng được; mở lại thanh toán từ danh sách nếu đơn còn hạn, không nhân đôi đơn |
| PAY-15 | P0 | MoMo đã trả → manager cố DELIVERING/COMPLETED nhiều lần | Chỉ kết chuyển tiền một lần; không đổi tổng tiền/snapshot thanh toán |
| PAY-16 | P0 | MoMo chưa trả → cố bắt đầu giao/hoàn tất pickup bằng request | Bị chặn trước giao/hoàn tất; không tạo doanh thu giả |
| PAY-17 | P1 | Tắt `app.demo-payment.enabled`; kiểm ở prod test cô lập | Không hiển thị/không dùng được demo; không bị bật lại bởi request client |

### 4.8 Vòng đời đơn, hủy và hoàn tiền — ORD

Tiền điều kiện: đơn mới riêng cho mỗi ca; `/customer/orders`, `/manager/orders`, `/admin/orders`. PREPARING phải có cảnh báo/xác nhận. Chỉ tạo fixture trạng thái cũ khi có bản sao test.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| ORD-01 | P1 | Xem/lọc/phân trang đơn C1, mở chi tiết | Chỉ đơn C1, đúng tổng/người nhận/dòng hàng/trạng thái/chi nhánh |
| ORD-02 | P0 | C1 mở/hủy/yêu cầu hoàn tiền bằng ID đơn C2 | Không lộ chi tiết riêng, không đổi đơn C2 |
| ORD-03 | P0 | M2 mở/cập nhật/xác nhận thanh toán đơn S1 | Bị chặn theo quyền chi nhánh, kể cả gửi ID trực tiếp |
| ORD-04 | P0 | DELIVERY đi PENDING→CONFIRMED→PREPARING→DELIVERING→DELIVERED→COMPLETED | Chỉ bước hợp lệ được phép; UI khách/quản lý/admin đồng bộ |
| ORD-05 | P0 | PICKUP đi tới PREPARING→COMPLETED; ép qua DELIVERING/DELIVERED | Hoàn tất nhận tại quầy được; bước vận chuyển bị chặn |
| ORD-06 | P0 | Từ PENDING ép COMPLETED; từ DELIVERED về PENDING; trạng thái lạ | Không chuyển sai/lùi hoặc tạo doanh thu trái phép |
| ORD-07 | P1 | Chọn bắt đầu chuẩn bị, đọc modal rồi Hủy; sau đó xác nhận | Hủy modal không cập nhật; xác nhận mới PREPARING và thông báo khách mất quyền tự hủy |
| ORD-08 | P0 | Khách hủy PENDING; CONFIRMED dưới/trên một giờ, chọn lý do hợp lệ | Được hủy trước chuẩn bị, hoàn kho một lần, lưu đúng lý do |
| ORD-09 | P0 | Khách hủy PREPARING/DELIVERING/DELIVERED/COMPLETED | Bị chặn cả UI và backend; kho/tiền giữ nguyên |
| ORD-10 | P0 | Khách giữ tab CONFIRMED, M1 chuyển PREPARING, khách hủy từ tab cũ | Backend đọc trạng thái mới và chặn |
| ORD-11 | P0 | M1/A hủy tại PENDING/CONFIRMED/PREPARING, lý do hợp lệ | Đúng vai trò người hủy, đúng lý do, trả kho đúng một lần |
| ORD-12 | P0 | M1/A hủy bằng lý do rỗng/trắng/300/301 ký tự | 300 hợp lệ; rỗng/trắng/301 bị chặn trước thay đổi kho/tiền; giới hạn lý do khách xem ORD-26…29 |
| ORD-13 | P1 | Lý do có HTML/nháy/dấu và xuống dòng | Lưu/hiện như văn bản, không chạy script; khách đọc được lý do |
| ORD-14 | P0 | Hủy COD chưa trả; hủy prepaid thường; khách hủy MoMo/ví đã trả trước chuẩn bị | COD→CANCELLED; prepaid thường→REFUND_REQUESTED; MoMo/ví mô phỏng khách hủy→REFUNDED và cộng đúng ví khách |
| ORD-15 | P0 | M1 hủy MoMo demo đã trả tiền | REFUND_REQUESTED, tiền H chưa tự mất; xác nhận hoàn tiền mới giải phóng H |
| ORD-16 | P0 | Gửi hủy lặp; hai người cùng hủy | Không cộng kho nhiều lần, không gửi nhiều thông báo/hoàn tiền trùng |
| ORD-17 | P0 | C1 yêu cầu hoàn đơn đã giao/hoàn tất và PAID | REFUND_REQUESTED; chưa được coi là tiền đã hoàn, chưa tự trừ ví |
| ORD-18 | P0 | Yêu cầu hoàn khi chưa giao/chưa trả hoặc đơn C2 | Bị chặn; không tạo yêu cầu hoàn tiền sai |
| ORD-19 | P0 | M1/A xác nhận hoàn cho REFUND_REQUESTED | Đơn/payment REFUNDED; hoàn đúng khoản tiền chờ hoặc đảo đúng thu nhập đã ghi; phương thức mô phỏng cộng đúng ví khách một lần |
| ORD-20 | P0 | Lặp xác nhận hoàn; cố hoàn đơn chưa trả tiền | Không trừ tiền lần hai/không hoàn giả |
| ORD-21 | P0 | Hoàn đơn đã kết chuyển thu nhập, còn các đơn khác trong ví | Chỉ đảo thu nhập/điểm/hoa hồng của đơn đó; không đụng đơn khác |
| ORD-22 | P1 | Xem đơn bị hủy trước/sau xác nhận hoàn tiền | Giữ lý do/người hủy ban đầu, bổ sung trạng thái hoàn đúng |
| ORD-23 | P1 | Lọc đơn theo trạng thái/tìm kiếm/chi nhánh và chuyển trang | Danh sách, tổng số và bộ lọc nhất quán; không trộn cửa hàng |
| ORD-24 | P0 | Đổi giá/ảnh/tên mẫu sau khi đơn đã đặt rồi xem lại đơn | Đơn giá/tổng tiền đã chốt trên đơn không bị tính lại theo giá mới |
| ORD-25 | P1 | Vào `/staff/orders` bằng M1/A và xử lý nút có trên UI | Cùng luật chuyển trạng thái, kiểm chi nhánh và thanh toán như manager; không mở đường bỏ qua chính sách |
| ORD-26 | P0 | C1 chọn hủy nhưng bỏ trống reasonCode hoặc gửi mã giả | Bị từ chối, trạng thái/kho/ví không đổi |
| ORD-27 | P1 | C1 lần lượt chọn từng lý do hủy có sẵn | Đúng nhãn lý do được lưu và hiển thị, không cần nhập lý do khác |
| ORD-28 | P0 | Chọn OTHER, để rỗng/trắng hoặc nhập 200/201 ký tự sau chuẩn hóa | Trống/trắng/201 bị chặn; 200 hợp lệ; không dùng giới hạn 300 của quản lý cho khách |
| ORD-29 | P1 | Nhập OTHER có nhiều khoảng trắng, xuống dòng và ký tự có dấu | Chuẩn hóa khoảng trắng, giữ nghĩa tiếng Việt, hiển thị an toàn; không thay người hủy |
| ORD-30 | P1 | Khách đã hủy và nhận hoàn ví; chọn Đã hủy, Đã hoàn tiền, Chờ hoàn tiền | Bộ lọc phản ánh đúng trạng thái và lịch sử hủy; không làm đơn mất khỏi nhóm hủy chỉ vì đã REFUNDED; không lẫn đơn hoàn sau giao chưa từng hủy |

### 4.9 Tồn kho và cập nhật 5 giây — STOCK

Tiền điều kiện: mở trang chủ và dashboard M1/A song song. Trang khách hiển thị kho được bán; dashboard hiển thị KPI tồn kho hợp lệ, độc lập lọc ngày.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| STOCK-01 | P0 | Xem P1 và PG trên trang chủ/hoa bán chạy/chi tiết/lọc S1 | Mẫu riêng chỉ kho chủ sở hữu; PG cộng đúng kho có thể bán hoặc riêng shop đang lọc |
| STOCK-02 | P0 | C1 đặt COD 2 P1, chờ ≥5 giây ở các trang còn mở | Số lượng P1 giảm 2 trên thẻ/KPI đúng phạm vi; số mẫu sản phẩm không giảm 2 |
| STOCK-03 | P0 | Hủy trước giao, chờ lần cập nhật kế tiếp | Tăng lại đúng 2; không cần tải lại thủ công để thấy kho mới |
| STOCK-04 | P1 | Kho từ 1→0; từ 0→1 do nhập/hoàn kho | Nhãn hết hàng và nút mua khóa/mở theo kho mới |
| STOCK-05 | P1 | Đổi tab lâu rồi quay lại; Back về trang từ cache | Refresh kho, không giữ số lượng đã cũ mãi |
| STOCK-06 | P0 | S1 ngừng bán/xóa PG tại S1, S2 vẫn mở bán | Kho công khai không cộng S1; S2 vẫn mua được |
| STOCK-07 | P1 | Đóng cửa S1; đổi khoảng ngày dashboard | Kho bán công khai loại S1; KPI tồn hợp lệ vẫn có hàng tạm đóng/ngừng bán, không đổi theo ngày |
| STOCK-08 | P0 | M1 xem KPI S1 rồi sửa storeId sang S2; A xem từng chi nhánh/toàn chuỗi | M1 chỉ thấy S1 và bị chặn S2; A có phạm vi quản trị đúng, không lẫn tổng và từng chi nhánh |
| STOCK-09 | P0 | Có dòng kho di sản P1 ở S2 dù P1 riêng S1 | Không cộng kho S2 vào số bán/KPI/danh sách hợp lệ; không dùng làm lý do cho M2 sửa P1 |
| STOCK-10 | P1 | Mở `tab=legacy` ở sản phẩm quản lý | Không xuất hiện mục vận hành kho cũ trái quy tắc mới |
| STOCK-11 | P0 | Gọi `/api/public/catalog-stock` với ID mẫu cấm/xóa/chờ duyệt | Không trả lượng có thể bán của mẫu/chi nhánh bị chặn |
| STOCK-12 | P1 | Ngắt mạng endpoint kho rồi nối lại | Không làm hỏng giỏ/trang, không biến số cũ thành khẳng định còn hàng vô hạn; cập nhật phục hồi |

### 4.10 Cửa hàng và nhân sự — STORE / STAFF

Tiền điều kiện: M1 quản lý phụ trách S1, NV1 là STAFF S1, M2 quản lý S2, MN chưa có shop. Trang `/manager/stores`, `/manager/profile`, `/manager/staff`. Mỗi manager chỉ một cửa hàng; không dùng MANAGER đối tác nhiều cửa hàng để thử luồng mới.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| STORE-01 | P1 | MN vào cửa hàng/dashboard/đơn/sản phẩm khi chưa phân công | Có trang trống/đường dẫn tạo shop phù hợp, không 500 hoặc thấy shop khác |
| STORE-02 | P0 | Tạo cửa hàng hợp lệ, chọn hoa hồng | Gắn đúng chủ, không tự có doanh thu/kho/đơn không có thật |
| STORE-03 | P1 | Tên 2/3/120/121; địa chỉ 4/5/200/201; bio 500/501; SĐT sai | Đúng biên create, lỗi không lưu shop một phần |
| STORE-04 | P1 | Bỏ chính sách hoa hồng; chọn ID đã xóa/không tồn tại | Bị từ chối |
| STORE-05 | P0 | Gửi lại form tạo với cùng requestId | Không tạo hai cửa hàng từ cùng yêu cầu |
| STORE-06 | P0 | M1 xem Cửa hàng của tôi và các module; cố thêm/chuyển sang cửa hàng thứ hai | Chỉ một cửa hàng phụ trách; không có đường chọn cửa hàng khác trái quyền; A vẫn xem được toàn chuỗi |
| STORE-07 | P0 | M2 mở sửa/chuyển S1 bằng URL/request trực tiếp | Bị chặn, active shop không bị đổi trái quyền |
| STORE-08 | P1 | Sửa tên/SĐT/địa chỉ/bio của shop có quyền rồi mở gian hàng công khai | Dữ liệu mới đồng bộ, không đổi chủ/hoa hồng ngoài quyền |
| STORE-09 | P1 | Upload avatar/cover, thêm/sửa/xóa ảnh đặc trưng | Đúng loại ảnh; reload còn dữ liệu; xóa một ảnh không xóa nhầm ảnh khác |
| STORE-10 | P0 | Tắt mở cửa khi C1 đang ở checkout; mở lại | Đơn mới bị chặn khi đóng; mở lại chỉ nhận mẫu/kho hợp lệ |
| STORE-11 | P0 | A cấm S1 rồi dùng link cũ/quote/checkout | Không nhận đơn mới cho shop bị cấm; lịch sử cũ được giữ |
| STAFF-01 | P1 | M1 thêm STAFF có sẵn, đang hoạt động và chưa gán shop | Thêm đúng S1, giữ role STAFF; không nâng USER thành nhân viên/quản lý |
| STAFF-02 | P0 | Chọn MANAGER làm ứng viên hoặc giả requestedRole=ADMIN/MANAGER/PARTNER | Bị từ chối; biểu mẫu nhân viên chỉ chấp nhận STAFF |
| STAFF-03 | P1 | Tạo nhân sự mới username/email/SĐT hợp lệ, password 8–72 ký tự | Tạo account STAFF riêng và membership đúng S1; đăng nhập vào bàn làm việc nhân viên |
| STAFF-04 | P1 | Nhân sự mới thiếu email/pass; pass 7/73; username trùng hoặc email trùng | Không tạo account lỗi/nhân sự trùng; báo rõ trường sai |
| STAFF-05 | P0 | Thêm A, C1 USER, account khóa, bản thân, người đã thuộc shop hoặc vai trò SHIPPER | Bị từ chối theo loại ứng viên, không đổi role cũ |
| STAFF-06 | P0 | NV1/M2 cố thêm/xóa nhân sự S1 | Chỉ quản lý phụ trách hoặc admin có quyền theo endpoint; request trực tiếp trái quyền bị chặn |
| STAFF-07 | P0 | M1 xóa nhân sự thường; thử xóa chính chủ hoặc người S2 | Chỉ xóa quan hệ đúng shop, không xóa chủ/nhân sự shop khác |
| STAFF-08 | P0 | Nhân sự bị gỡ vẫn giữ session và socket cũ → vào đơn/chat S1 | Quyền bị thu hồi, không tiếp tục thao tác nhờ phiên cũ |
| STAFF-09 | P0 | NV1 gửi yêu cầu rời qua URL quản lý; M1 phụ trách thử tự rời; M1 gỡ NV1 đúng chức năng | STAFF không truy cập luồng quản lý; người phụ trách không tự bỏ cửa hàng; quản lý gỡ STAFF được và thu hồi quyền đúng |
| STAFF-10 | P1 | Chọn nhiều ứng viên, một người không hợp lệ rồi gửi | Không thêm nửa danh sách ngoài transaction mong muốn; có thông báo lỗi |

### 4.11 Sản phẩm, danh mục và xóa mềm — PROD

Tiền điều kiện: M1 tại S1, A quản trị catalog; `/manager/products`, `/admin/products`. Có mẫu riêng P1, mẫu chung PG, mẫu lịch sử `is_deleted=NULL` nếu cần.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| PROD-01 | P1 | Tạo mẫu riêng S1 đầy đủ giá/kho/ảnh/danh mục | Lưu đúng chi nhánh; trạng thái hiển thị theo duyệt/mở bán thực tế |
| PROD-02 | P1 | A tạo mẫu chung rồi thêm kho chi nhánh theo chức năng có sẵn | Mẫu chung xuất hiện đúng nơi đã có kho, không gán ID shop cứng |
| PROD-03 | P1 | Bỏ tên/danh mục/giá; chọn danh mục đã xóa | Chặn lưu thiếu dữ liệu hoặc dùng danh mục không còn hợp lệ |
| PROD-04 | P0 | Giá bán/nhập 0/-1/0.01/1; vượt 16 chữ số khi form có giới hạn | Đồng nguyên từ 1đ; không làm tròn giá sai thành 0đ |
| PROD-05 | P0 | Giá niêm yết 120.000, khuyến mãi 100.000/trống/120.000/120.001 | Chấp nhận không vượt niêm yết; giá hiệu lực đúng; không hiểu 100.000 là phần trăm |
| PROD-06 | P0 | Khuyến mãi âm/0/0.01; fixture cũ khuyến mãi không hợp lệ | Dữ liệu mới bị chặn; dữ liệu cũ dùng giá niêm yết khi hiển thị/tính tiền |
| PROD-07 | P1 | A đặt giá bán dưới giá nhập, chọn Hủy rồi xác nhận cảnh báo bán lỗ | Không lưu trước xác nhận; xác nhận có chủ đích mới lưu |
| PROD-08 | P1 | Kho 0/1/1.000.000/-1/1.000.001 | Chấp nhận biên hợp lệ, chặn ngoài biên ở server |
| PROD-09 | P1 | Chọn 0/1/10/11 danh mục bổ sung; lặp ID/danh mục chính | Tối đa 10 bổ sung, không nhân liên kết; đúng danh mục chính |
| PROD-10 | P1 | Chọn thuộc tính ở vài nhóm rồi bỏ toàn bộ một nhóm, lưu lại | Không bắt mọi nhóm; xóa lựa chọn thực sự được lưu, không giữ giá trị cũ do binding |
| PROD-11 | P1 | Upload nhiều ảnh hợp lệ, sửa mẫu không thay ảnh | Ảnh đúng thứ tự/chính theo UI; sửa không làm mất ảnh cũ |
| PROD-12 | P0 | M1 sửa PG bằng form chung hoặc đổi request tên/giá/owner | Chỉ sửa kho/trạng thái chi nhánh của PG; thông tin chung do A quản lý |
| PROD-13 | P0 | M2 sửa/xóa/restore/toggle P1 bằng ID, kể cả có kho di sản ở S2 | Bị chặn theo sở hữu mẫu, không dựa riêng dòng inventory |
| PROD-14 | P1 | Lần lượt mục Đang bán/Ngừng bán/Tất cả/Đã xóa, tìm và phân trang | Đủ mẫu riêng + chung trong kho; tổng số mẫu là tổng bộ lọc, không chỉ số dòng trang |
| PROD-15 | P1 | Mẫu lịch sử `is_deleted=NULL` trong kho hợp lệ | Không biến mất khỏi danh sách vì NULL |
| PROD-16 | P0 | Ngừng bán P1 khi có đơn đang xử lý | Ngăn đơn mới; đơn cũ vẫn có dữ liệu để xử lý |
| PROD-17 | P0 | Xóa P1 khi còn đơn đang xử lý | Bị chặn và gợi ý ngừng bán; không xóa ảnh/kho/đơn |
| PROD-18 | P0 | Xóa mẫu khi không còn đơn xử lý → kiểm kho/ảnh/lịch sử | Chỉ xóa mềm, dữ liệu lịch sử và lượng kho giữ nguyên |
| PROD-19 | P0 | Xóa PG tại S1 → C1 xem S1/S2 | Chỉ S1 ẩn PG; S2 vẫn kinh doanh nếu hợp lệ |
| PROD-20 | P1 | Khôi phục mẫu đã xóa rồi mở bán | Khôi phục về Ngừng bán, chưa tự nhận đơn; mở bán riêng mới bán được |
| PROD-21 | P0 | A cấm/chưa duyệt mẫu chung, M1 cố mở bán tại chi nhánh | Không vượt được trạng thái toàn hệ thống |
| PROD-22 | P1 | Sửa mẫu từ trang 2 đang lọc → lưu/hủy | Quay về đúng bộ lọc/trang nếu hợp lệ; không chuyển tới URL ngoài do returnUrl |
| PROD-23 | P1 | Nhập mô tả/tên có dấu, emoji, HTML, ký tự đặc biệt | Văn bản hiển thị an toàn, không hỏng Unicode/không chạy script |
| PROD-24 | P0 | A ngừng bán sản phẩm đang duyệt, C1 xem chi tiết và đặt từ giỏ cũ | Vẫn giữ được duyệt, xem chi tiết được nếu cửa hàng hợp lệ, không đặt đơn mới; đơn cũ không tự hủy |
| PROD-25 | P0 | A cấm kinh doanh sản phẩm đang bán, C1 dùng URL chi tiết/giỏ/checkout cũ | Không xem chi tiết công khai hoặc mua mới được; giữ lịch sử và không tự hoàn tiền đơn cũ |
| PROD-26 | P0 | Mẫu đang bị cấm, chỉ bật isSelling; sau đó A cấp phép lại | Chỉ bật mở bán không vượt lệnh cấm; sau cấp phép còn phải thỏa cờ mở bán, kho và trạng thái chi nhánh |
| PROD-27 | P1 | A cấp phép lại mẫu đã ngừng bán trước lúc bị cấm | Không tự bật isSelling ngoài thao tác được chọn; mẫu vẫn ngừng bán đến khi mở lại |
| PROD-28 | P1 | Ngừng bán/cấm/mở lại cùng mẫu khi đang lọc trang danh sách | Nhãn, menu và bộ lọc đồng bộ; không nhầm hai trạng thái hoặc đổi sang mẫu khác |

### 4.12 Admin và dữ liệu dùng chung — ADMIN / MASTER

Tiền điều kiện: A; luôn dùng record test khi xóa/reset. Với “CRUD” thực hiện: tạo → tìm → sửa → tải lại → xóa → xem mục đã xóa/khôi phục nếu module có nút. Không áp thao tác khôi phục cho module không có endpoint đó.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| ADMIN-01 | P0 | A tạo USER và MANAGER riêng bằng `/admin/users` | Mỗi account một vai trò đúng, không sửa role account hiện có |
| ADMIN-02 | P1 | Tạo account trùng username/email, sai role, thiếu dữ liệu | Bị từ chối, không lưu một phần |
| ADMIN-03 | P1 | Lọc người dùng theo vai trò + từ khóa/SĐT; đổi kích thước trang | Kết hợp đúng các điều kiện, không mất lọc khi chuyển trang |
| ADMIN-04 | P0 | Khóa CX đang có đơn UNPAID PENDING/CONFIRMED/PREPARING | Account bị chặn; đơn trước giao bị hủy và hoàn kho một lần |
| ADMIN-05 | P0 | Khóa CX có đơn PAID trước giao; có đơn DELIVERING/DELIVERED | Đơn paid trước giao vào chờ hoàn; không tự hủy đơn đã bàn giao/giao thành công |
| ADMIN-06 | P0 | CX dùng session/JWT/chat/QR cũ sau bị khóa | Bị chặn chức năng bảo vệ, không mua/trả/gửi tin tiếp |
| ADMIN-07 | P0 | Xóa khách có đơn đang xử lý/chờ hoàn tiền; thử lại khi mọi đơn đã kết thúc | Khi còn đơn: chặn xóa, không đổi tài khoản/đơn/kho. Khi đủ điều kiện: thông báo ngắn, giữ lịch sử hóa đơn |
| ADMIN-07A | P0 | Xóa Staff đang đăng nhập | Thu hồi phân công/quyền; yêu cầu tiếp theo bị chặn; đơn khách của chi nhánh và tin nhắn lịch sử vẫn còn |
| ADMIN-07B | P0 | Xóa Manager phụ trách rồi phân công người thay và thử lại | Chặn trước khi thay; sau thay có thể xóa, người mới tiếp tục quản lý và đơn không bị hủy |
| ADMIN-08 | P0 | Khóa/xóa A khác hoặc chính admin, kể cả admin đổi username | Bảo vệ theo role, không theo tên đăng nhập cố định |
| ADMIN-09 | P1 | Mở khóa CX rồi đăng nhập lại | Account hoạt động theo trạng thái mới; đơn hủy không tự phục hồi |
| ADMIN-10 | P0 | Dùng endpoint nâng/đổi vai trò cũ hoặc gửi role trên form hồ sơ | Không chuyển role account có sẵn hoặc tự cấp quyền |
| ADMIN-11 | P1 | Duyệt/cấm S1; đổi cấp cửa hàng bằng ID hợp lệ/sai | Trạng thái/cấp đúng, ID sai không làm hỏng bản ghi |
| ADMIN-12 | P1 | Duyệt/cấm/toggle-selling P1 và tìm/lọc sản phẩm toàn sàn | Quyết định áp đúng mẫu, hiển thị công khai đồng bộ |
| ADMIN-13 | P1 | Tìm/lọc đơn toàn sàn, mở từng đơn S1/S2 | A thấy đúng phạm vi toàn hệ thống, không sai thông tin chi nhánh |
| ADMIN-14 | P1 | Tìm giao dịch theo mô tả và phân trang `/admin/transactions` | Đúng số tiền/chiều tăng giảm/trạng thái/shop/đơn, không lặp dữ liệu giữa trang |
| MASTER-01 | P1 | CRUD danh mục `/admin/categories`, thử khôi phục | Dữ liệu mới hiện ở form chọn; xóa không làm mất đơn/sản phẩm lịch sử |
| MASTER-02 | P1 | CRUD nhóm thuộc tính `/admin/styles` và giá trị trong nhóm | Giá trị thuộc đúng nhóm; dùng trong form sản phẩm và lọc đúng |
| MASTER-03 | P1 | Xóa/khôi phục giá trị; thử xóa vĩnh viễn giá trị đang dùng | Không làm hỏng liên kết mẫu; thao tác không cho phép phải bị chặn có thông báo |
| MASTER-04 | P1 | CRUD cấp khách `/admin/user-levels`, tên/ngưỡng/giảm giá | Lưu và hiển thị đúng; thử rỗng, âm, >100%, trùng ngưỡng để phát hiện thiếu validation |
| MASTER-05 | P1 | CRUD cấp shop `/admin/store-levels`, giá trị biên | Danh sách/xếp hạng đúng; không lưu ngưỡng/giá trị vô nghĩa âm |
| MASTER-06 | P0 | CRUD hoa hồng `/admin/commissions`, thử 0/100/âm/>100 | Mức hợp lệ 0–100%; không gây số tiền kết chuyển âm hoặc vượt tổng |
| MASTER-07 | P1 | CRUD đơn vị giao `/admin/deliveries`, toggle active | Chỉ đơn vị hoạt động/chưa xóa xuất hiện khi checkout |
| MASTER-08 | P1 | Tạo/sửa/tắt/xóa biểu phí theo shop/đơn vị/vùng | Quote đổi đúng phí, không ảnh hưởng biểu phí shop khác |
| MASTER-09 | P1 | Nhập phí cơ bản/phụ phí âm, ID shop/đơn vị không có | Báo validation, không tạo giá ship âm/tham chiếu hỏng |
| MASTER-10 | P1 | Xóa dữ liệu dùng chung đang được tham chiếu; khôi phục | Lịch sử không mất; form mới không dùng mục đã xóa; restore trở lại đúng trạng thái |
| MASTER-11 | P2 | Tìm kiếm không kết quả, page âm/quá lớn, size=0/rất lớn trên từng module | Trang rỗng/phân trang ổn định, không 500; giới hạn kích thước phù hợp |
| MASTER-12 | P1 | Trên DB bản sao, chạy từng nút khôi phục mặc định có trên UI hai lần | Không nhân đôi record, không xóa dữ liệu tùy chỉnh/lịch sử ngoài phạm vi thông báo; ghi rõ tác động thực tế |

### 4.13 Ví, giao dịch, hoa hồng — WAL

Tiền điều kiện: M1 quản lý phụ trách S1, NV1 là STAFF không có quyền ví, A duyệt giao dịch; `/manager/wallet`, `/admin/transactions`. Ghi riêng W khả dụng và H chờ; không nhầm với WC1 là ví khách.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| WAL-01 | P0 | Mở ví trước/sau MoMo trả tiền, sau giao thành công | H và W tách đúng; tiền chờ không trở thành tiền rút được |
| WAL-02 | P0 | Hoa hồng 10%, đơn tổng 100.000đ kết chuyển | Thu nhập 90.000đ, phí 10.000đ; một lần duy nhất |
| WAL-03 | P0 | MoMo đã trả khi rate=10%, đổi rate shop=20% trước giao | Dùng snapshot 10% của đơn demo, không trừ lại theo cấu hình mới |
| WAL-04 | P0 | Tạo đơn demo mới sau đổi rate=20% rồi giao | Đơn mới dùng 20%; đơn cũ không đổi |
| WAL-05 | P0 | Rút 49.999/50.000đ; vượt W; W=0 dù H>0 | Tối thiểu 50.000đ và chỉ trong W; không lấy từ H |
| WAL-06 | P0 | Rút thiếu/sai mật khẩu | Không tạo yêu cầu hoặc trừ W |
| WAL-07 | P1 | Ngân hàng/chủ trống hoặc >100 ký tự; STK 5/6/30/31 số/chữ | STK 6–30 số, các trường đúng biên; sai bị chặn |
| WAL-08 | P1 | Amount âm, >2 chữ số lẻ, quá lớn; requestId sai UUID | Từ chối dữ liệu sai, không tạo giao dịch lỗi |
| WAL-09 | P0 | NV1/M2/A cố POST rút S1 khi không là người phụ trách được quyền rút | Bị chặn theo luật chủ ví hiện tại; quyền xem/duyệt không tự cấp quyền rút thay |
| WAL-10 | P0 | Gửi cùng requestId hai lần; cùng ID nhưng đổi số tiền/STK | Lặp cùng nội dung không trừ hai lần; đổi nội dung bị chặn |
| WAL-11 | P0 | A từ chối pending; A duyệt pending | Từ chối trả W một lần; duyệt không trừ W lần hai |
| WAL-12 | P0 | A xử lý lại yêu cầu đã COMPLETED/REJECTED; xử lý ID không phải WITHDRAWAL | Bị chặn, không thay tiền |
| WAL-13 | P0 | M1 xem lịch sử/thu nhập/yêu cầu rút rồi sửa storeId sang S2 | Chỉ dữ liệu S1, không lẫn hoặc sửa được giao dịch S2 |
| WAL-14 | P0 | Cộng/trừ các giao dịch fixture và so W/H trước/sau | Khớp số dư; không có PAYMENT_HOLD bị tính thành thu nhập khả dụng lần nữa |

### 4.14 Dashboard và thống kê — REPORT

Tiền điều kiện: `/admin/dashboard`, `/manager/dashboard`, `/manager/analytics`; đơn có thời gian tạo biết trước, PAID hợp lệ và đơn UNPAID/REFUNDED để so sánh. Ca >1.000 dòng/biên ngày cần dataset riêng, không coi dữ liệu ít là đã kiểm chứng.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| REPORT-01 | P1 | Mở lần đầu ba màn hình báo cáo | Mặc định 7 ngày bao gồm hôm nay, đúng UTC+7 |
| REPORT-02 | P1 | Chọn Hôm nay/7 ngày/30 ngày, đối chiếu đơn mẫu | Tổng và biểu đồ chỉ lấy kỳ đã chọn |
| REPORT-03 | P1 | Chọn tùy chỉnh một ngày; khoảng nhiều ngày; bấm Áp dụng | Bao gồm trọn ngày đầu/cuối; thay đổi chỉ áp theo thao tác được thiết kế |
| REPORT-04 | P1 | Từ > đến; thiếu một ngày; định dạng sai | Bị từ chối rõ ràng, không 500 hoặc âm thầm báo cáo sai kỳ |
| REPORT-05 | P1 | Chuyển tiêu chí đơn/sản phẩm, loại biểu đồ, trang bảng | Giữ khoảng ngày đang chọn |
| REPORT-06 | P1 | Chuyển bar/line/pie theo UI | Cùng dữ liệu nguồn/tổng, không nhân số liệu hoặc mất nhãn |
| REPORT-07 | P0 | Cùng kỳ có đơn UNPAID, PAID đủ điều kiện, REFUNDED | Doanh thu chỉ tính dữ liệu đủ điều kiện, không cộng chưa trả/đã hoàn |
| REPORT-08 | P1 | Đơn tạo ngoài kỳ nhưng giao trong kỳ; đơn tạo trong kỳ giao sau | Bộ lọc dùng ngày tạo đơn theo thiết kế; không nhầm sang ngày hoàn tất |
| REPORT-09 | P1 | User/store/product tạo đúng biên 00:00 và cuối ngày UTC+7 | Đếm đúng trong kỳ, không lệch ngày do timezone |
| REPORT-10 | P1 | Kỳ dài 90/91 và 730/731 ngày | Trên 90 gom tháng, trên 730 gom năm; vẫn lọc chính xác hai biên kỳ |
| REPORT-11 | P1 | Xem thống kê user mới/store mới và xếp hạng điểm | Lọc theo ngày tạo; cột điểm là điểm tích lũy hiện tại, không giả làm lịch sử điểm theo ngày |
| REPORT-12 | P0 | M1/M2 so báo cáo chi nhánh và A toàn sàn | Mỗi manager chỉ shop có quyền đang chọn; A tổng phù hợp toàn hệ thống |
| REPORT-13 | P1 | Dataset >1.000 đơn hợp lệ trong kỳ, tính tổng độc lập | Tổng/biểu đồ không bị cắt ở 1.000; bảng/xếp hạng tối đa 1.000 và phân trang |
| REPORT-14 | P1 | Kho thay đổi trong khi báo cáo đang lọc kỳ cũ | KPI kho cập nhật hiện tại, không bị lọc theo kỳ doanh thu |
| REPORT-15 | P2 | Kỳ không dữ liệu; chỉ một điểm; số tiền rất lớn | Hiện 0/rỗng hợp lý, không NaN/Infinity, tràn nhãn hoặc lỗi chart |
| REPORT-16 | P1 | Mở URL lọc giờ/tháng/năm cũ với dữ liệu phù hợp | Tiếp tục tương thích hoặc có phản hồi rõ, không mất bộ lọc âm thầm |

### 4.15 Chat realtime — CHAT

Tiền điều kiện: C1/C2/M1/M2/A ở trình duyệt độc lập; WebSocket/STOMP hoạt động. Dùng widget, `/admin/chat`, `/admin/chat/access-logs`, `/api/chat/**`. Mỗi ca sai quyền phải thử cả REST lẫn socket khi có điểm tương ứng.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| CHAT-01 | P0 | C1 mở chat S1 từ shop → gửi/nhận với M1 | Đúng hội thoại/chi nhánh, xuất hiện realtime và giữ sau reload |
| CHAT-02 | P1 | Đóng/mở lại cùng kênh nhiều lần | Dùng lại hội thoại phù hợp, không sinh nhiều thread trùng |
| CHAT-03 | P1 | Liên hệ từ P1, từ PG chọn S2, từ đơn S1 | Gắn đúng ngữ cảnh sản phẩm/đơn và nơi nhận, không shop ID cố định |
| CHAT-04 | P0 | C1 liên hệ bằng ID đơn C2; P1 riêng S1 nhưng storeId=S2 | Bị từ chối trước tạo/gửi dữ liệu sai |
| CHAT-05 | P0 | C1 chat admin, M1 xem hộp thư; M1 hỗ trợ nội bộ, C1 thử đọc | Hai kênh riêng tư, đúng người tham gia |
| CHAT-06 | P0 | M1 thử khởi tạo chat như khách hoặc tự nhắn shop mình | Bị chặn; manager chỉ dùng hộp thư chi nhánh/kênh hỗ trợ được phép |
| CHAT-07 | P1 | Mở chat shop chưa có manager hoạt động hoặc không có admin hoạt động ở kênh admin | Báo không có người tiếp nhận, không giả gửi thành công |
| CHAT-08 | P0 | C2/M2 sửa conversationId, lấy history, gửi text/ảnh | Không đọc/ghi hội thoại S1 của C1 |
| CHAT-09 | P0 | A giám sát chat khách–shop rồi thử gửi/subscribe queue riêng | A xem lịch sử qua luồng giám sát có audit; không gửi thay shop hoặc nghe queue riêng trái phép |
| CHAT-10 | P1 | A xem lịch sử và vào access logs | Có người xem/hội thoại/thời gian; tải history có no-store, không lộ cho role khác |
| CHAT-11 | P1 | Tin nhắn tiếng Việt/emoji/xuống dòng/URL/HTML | Hiển thị an toàn và đúng Unicode, không chạy script |
| CHAT-12 | P1 | Text rỗng/trắng; 2.000/2.001 ký tự; text rỗng có ảnh hoặc product context | Chặn rỗng không nội dung; cho đúng biên/nội dung hợp lệ; chặn vượt giới hạn |
| CHAT-13 | P0 | Retry cùng clientMessageId/content; cùng ID đổi content | Không tạo tin trùng; đổi nội dung với ID đã dùng bị từ chối |
| CHAT-14 | P1 | Gửi ảnh JPEG/PNG/WEBP/GIF hợp lệ | URL Cloudinary HTTPS, ảnh xem được và tồn tại sau reload |
| CHAT-15 | P1 | Ảnh giả MIME, ảnh hỏng, quá 10MB, Cloudinary chưa cấu hình/lỗi | Báo lỗi, không tạo tin ảnh rỗng hoặc ghi message thành công giả |
| CHAT-16 | P1 | Nạp lịch sử >50 tin, cuộn lên nhiều đợt | Thứ tự đúng, không thiếu/trùng tin tại mốc before |
| CHAT-17 | P1 | Tìm hội thoại theo từ khóa/ngày/kênh; chuyển trang | Đúng phạm vi/điều kiện; ngày đảo hoặc từ khóa quá dài có validation |
| CHAT-18 | P1 | Ngắt mạng rồi nối lại, gửi thử lại | Kết nối phục hồi; không mất/trùng tin, không chỉ hiển thị đã gửi ở client |
| CHAT-19 | P0 | M1 sửa storeId S1→S2; NV1 bị chuyển hợp lệ từ S1 sang S2 trong fixture | M1 không có quyền S2; STAFF sau chuyển chỉ dùng S2, không đọc/gửi/subscribe dữ liệu S1 từ phiên cũ |
| CHAT-20 | P0 | Gỡ membership hoặc khóa account khi socket vẫn mở | REST và socket không tiếp tục cho phép gửi/đọc trái quyền |
| CHAT-21 | P0 | Cố subscribe queue người khác/topic chi nhánh không có quyền; SEND không có principal | Bị chặn; không nhận payload riêng tư |

### 4.16 Thông báo — NOTI

Tiền điều kiện: C1 và M1 có thông báo chưa đọc. Trang `/notifications`, `/manager/notifications`, API `/api/notifications/*`; khách có thể đi từ chuông trên UI.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| NOTI-01 | P1 | Tạo đơn, đổi trạng thái, bắt đầu chuẩn bị, trả/hoàn tiền | Đúng người nhận/tiêu đề/nội dung/ID đơn; không lẫn S1/S2 |
| NOTI-02 | P1 | Nhận thông báo khi trang đang mở | Chuông/danh sách/count cập nhật không cần reload, không nhân đôi do nhiều listener |
| NOTI-03 | P1 | Đọc một thông báo rồi đọc lại; đọc tất cả | Count giảm đúng về 0, không âm; trạng thái đọc được lưu |
| NOTI-04 | P0 | C1 đánh dấu đã đọc notification ID của C2 | Bị chặn, dữ liệu C2 không đổi |
| NOTI-05 | P1 | Bấm thông báo đơn hủy có lý do hoặc bắt đầu chuẩn bị | Dẫn đúng đơn, đọc được lý do/chính sách hủy |
| NOTI-06 | P1 | Hai tab cùng account; ngắt/reconnect WebSocket/polling | Count cuối khớp server, không hồi lại thông báo đã đọc |
| NOTI-07 | P0 | Khóa account rồi dùng request đọc/list/latest cũ | Không truy cập tiếp dữ liệu bảo vệ |

### 4.17 Review — REV

Tiền điều kiện: C1 đã nhận P1 trong đơn O1 (DELIVERED/COMPLETED), C2 chưa mua. Nội dung tính sau trim, giới hạn 50–1.000 ký tự.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| REV-01 | P1 | C1 đánh giá O1/P1 với 5 sao, nội dung ≥50 ký tự | Tạo review đúng user/product/order/store, thấy trên trang mẫu |
| REV-02 | P0 | C2 chưa mua; C1 mới PENDING/PREPARING thử review | Bị chặn cả request trực tiếp |
| REV-03 | P1 | Nội dung 49/50/1.000/1.001 ký tự; chỉ khoảng trắng | Đúng biên, trim trước kiểm tra; nội dung trắng bị chặn |
| REV-04 | P1 | Rating 0/1/5/6 | Chỉ nhận 1–5 |
| REV-05 | P0 | Gửi lại review cùng user/product/order | Không tạo review trùng; đơn mua hợp lệ khác có thể đánh giá theo luật từng đơn |
| REV-06 | P0 | Đổi orderId sang đơn C2 hoặc đơn C1 không chứa P1 | Bị chặn dù C1 có từng mua P1 ở đơn khác |
| REV-07 | P1 | Hai review 3 và 5 sao cho cùng mẫu/shop, xem rating | Trung bình cập nhật và làm tròn một chữ số; shop dùng đúng review của shop |
| REV-08 | P1 | Review có ảnh/HTML/Unicode, reload chi tiết | Ảnh hợp lệ nếu hỗ trợ; nội dung escape; không làm thay đổi giá/kho/ví |

### 4.18 Upload và giao diện — UI

Tiền điều kiện: file ảnh nhỏ hợp lệ, ảnh gần 10MB, >10MB, file text đổi đuôi JPG, ảnh hỏng; kiểm cả customer/manager/admin/chat theo tính năng đang có. Giới hạn multipart chung: 10MB/file, 25MB/request; nhóm ảnh có thể có giới hạn riêng trên UI.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| UI-01 | P1 | Upload từng JPEG/PNG/WEBP/GIF thực, tên file tiếng Việt/dấu cách | Ảnh được xử lý đúng, URL xem được, không hỏng tên/encoding |
| UI-02 | P1 | File rỗng, >10MB, sai MIME/chữ ký, ảnh hỏng, SVG không hỗ trợ | Từ chối rõ; không lưu URL/bản ghi ảnh lỗi |
| UI-03 | P1 | Nhiều file tổng >25MB trong cùng request | Lỗi dung lượng được xử lý có thông báo, không treo/ghi một phần sai |
| UI-04 | P1 | Upload avatar/cover/sản phẩm khi Cloudinary chưa cấu hình ở dev | Theo fallback local được hỗ trợ; ảnh chat vẫn yêu cầu Cloudinary |
| UI-05 | P1 | Đường dẫn ảnh trống/404, ảnh lớn, thay ảnh rồi Back/reload | Có hiển thị thay thế hợp lý, không vỡ bố cục hoặc ghi đè ảnh nhầm |
| UI-06 | P2 | Viewport 360/390/768/1366px ở home/cart/checkout/order/chat/dashboard | Không mất nút chính/tràn ngang gây không thao tác được; bảng có cuộn phù hợp |
| UI-07 | P2 | Zoom 200%, bàn phím Tab/Enter/Escape ở form và modal | Đọc/nhập được; focus hợp lý, không bị kẹt modal |
| UI-08 | P1 | Bấm Hủy/đóng modal xóa/hủy đơn/chuẩn bị/rút tiền | Không gửi mutation; nội dung xác nhận đúng record |
| UI-09 | P2 | Reload/Back giữa trang form và danh sách có lọc | Không mất trạng thái cần thiết, không tự gửi lặp thao tác tiền/kho |
| UI-10 | P1 | Chrome và Edge (thêm trình duyệt khác nếu có), cache thường và tab mới | Luồng mua chính/chat/upload hoạt động; không lỗi JS làm chặn nút |
| UI-11 | P1 | Chạy thao tác với mạng Slow 3G/offline/reconnect | Có loading/error; nút không tạo mutation trùng do bấm nhiều lần |
| UI-12 | P2 | Soát toàn bộ menu/link ảnh/nhãn trạng thái và Console/Network | Không liên kết sai, lỗi Unicode hoặc lỗi JS/404 ảnh chính ảnh hưởng sử dụng |

### 4.19 Phân quyền, CSRF, API JWT — SEC / API

Tiền điều kiện: account từng role riêng. Ca session dùng cookie + CSRF; ca JWT dùng Bearer hợp lệ, không dựa vào cookie. API auth gồm POST `/api/auth/login`, POST `/api/auth/refresh`, GET `/api/auth/me`, POST `/api/auth/logout`.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| SEC-01 | P0 | Guest vào cart/checkout/customer/manager/admin/chat | Không đọc/ghi dữ liệu cần đăng nhập; điều hướng login hoặc lỗi phù hợp |
| SEC-02 | P0 | C1 vào `/admin/**`, `/manager/**`, `/staff/**`, `/api/admin/**` | Bị chặn, không chỉ ẩn menu |
| SEC-03 | P0 | M1 vào `/admin/**`; A/M1 thử checkout/cart bằng account không có USER | Không vượt quyền role; admin bị từ chối mua hàng |
| SEC-04 | P0 | Mở `/shipper/**`, gửi role SHIPPER/VENDOR/PARTNER cũ; USER giả role STAFF | Không khôi phục role/cổng đã bỏ; STAFF là role hợp lệ nhưng USER không được tự cấp hoặc tự đổi sang STAFF |
| SEC-05 | P0 | Mutation session thiếu/sai CSRF: cart, quote, status, read notification, chat REST | Bị chặn ở endpoint có CSRF; không dùng kết quả này để yêu cầu CSRF ở JWT stateless |
| SEC-06 | P0 | POST có CSRF đúng nhưng ID tài nguyên thuộc người/shop khác | Vẫn bị chặn ownership; CSRF đúng không có nghĩa có quyền |
| SEC-07 | P1 | GET thay cho POST mutation và URL sai | Không gây mutation qua GET ngoài điểm vào được thiết kế; lỗi được xử lý |
| SEC-08 | P0 | Sửa hidden storeId/userId/orderId, role, amount, returnUrl ngoài site | Không cấp quyền/đổi chủ/tin tiền client; không open redirect |
| SEC-09 | P1 | Nhập HTML/script không phá hoại vào trường text rồi xem các màn hình render | Chỉ hiển thị text, không thực thi tại customer/manager/admin |
| SEC-10 | P0 | Account bị khóa/vừa gỡ membership tiếp tục dùng session/request/socket cũ | Kiểm trạng thái/quyền hiện tại, không duy trì đặc quyền cũ |
| API-01 | P0 | POST login JSON username/password hợp lệ, không cookie | Nhận access/refresh, no-store; không trả password/OTP; GET me bằng Bearer đúng account |
| API-02 | P1 | Login sai password/account khóa; JSON hỏng/thiếu trường | Sai xác thực 401; dữ liệu lỗi 400 theo endpoint; không lộ stack trace |
| API-03 | P0 | Gọi me chỉ bằng session cookie, không Bearer | Session không thay được JWT trong `/api/auth/**` |
| API-04 | P0 | Bearer sai định dạng/sửa chữ ký/hết hạn/sai issuer/audience | Bị từ chối 401, không fallback sang cookie của người đang login |
| API-05 | P0 | Refresh hợp lệ → dùng token mới → thử refresh cũ lần nữa | Refresh xoay vòng; replay thu hồi family theo thiết kế, không cho tái dùng vô hạn |
| API-06 | P0 | JWT logout rồi dùng lại access và refresh cũ | Đều không dùng được; logout hợp lệ trả 204 |
| API-07 | P0 | Đổi password/khóa user sau cấp token → gọi API bằng token cũ | Token cũ không tiếp tục hoạt động |
| API-08 | P0 | Role hiện tại thay đổi trong fixture test, dùng token đã cấp | Quyền hiệu lực theo account hiện tại; không giữ đặc quyền cũ |
| API-09 | P0 | USER Bearer gọi `/api/admin/**`; hợp lệ gọi API chat của mình | Sai role 403; đúng quyền dùng được; scope dữ liệu vẫn được kiểm |
| API-10 | P1 | API session POST notification thiếu CSRF; Bearer hợp lệ gửi cùng API | Session cần CSRF; Bearer stateless hoạt động theo chain JWT, không dựa cookie |

### 4.20 Đồng thời và gửi lặp — CON

Tiền điều kiện: hai profile trình duyệt hoặc Postman chạy sát nhau; lặp với fixture mới. Sau mỗi ca kiểm server/database hoặc giao diện quản trị để đếm đơn/payment/giao dịch, không chỉ dựa toast. Bấm tay không chứng minh đã có race thực sự; ghi “chưa tái hiện đồng thời” nếu request không chồng nhau.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| CON-01 | P0 | Hai khách tranh kho=1 | Chỉ một thành công, kho không âm (E2E-10) |
| CON-02 | P0 | Double click đặt và gửi lại POST từ cùng giỏ | Không tạo hai đơn cho cùng dòng đã checkout; không trừ kho hai lần |
| CON-03 | P0 | C1 hủy cùng lúc M1 bắt đầu PREPARING | Một kết quả hợp lệ theo thứ tự khóa: hủy thắng hoặc chuẩn bị thắng; không vừa hủy vừa chuẩn bị/trả kho sai |
| CON-04 | P0 | MoMo confirm cùng lúc khách hủy | Hoặc hủy chưa trả, hoặc trả rồi hoàn theo luật; không PAID trên đơn CANCELLED chưa xử lý tiền |
| CON-05 | P0 | Scheduler hết hạn cùng lúc MoMo confirm | Không ghi hai kết quả trái nhau; tiền/kho nhất quán với kết quả cuối |
| CON-06 | P0 | M1/A cùng hủy hoặc cùng hoàn tiền | Một lần hoàn kho/hoàn tiền, không giao dịch trùng |
| CON-07 | P0 | Hai lần hoàn tất/giao thành công đồng thời | Ví/điểm/thu nhập chỉ tăng một lần |
| CON-08 | P0 | Hai yêu cầu rút khác ID cùng muốn dùng hết W | Không duyệt giữ tiền vượt W, số dư không âm do race |
| CON-09 | P0 | Hai khách dùng lượt voucher cuối cùng | Không tiêu quá lượt; bên thất bại rollback kho/đơn |
| CON-10 | P0 | Hai review hợp lệ đồng thời, có sửa giá/cập nhật ví cùng lúc | Review/rating đúng; không ghi đè mất giá/số dư cập nhật khác |
| CON-11 | P0 | Admin khóa account cùng lúc checkout/xác nhận QR | Không cho account đã bị khóa tạo mutation trái quy tắc; không sót đơn/tiền cần xử lý |
| CON-12 | P1 | Tin chat gửi lại sau reconnect cùng ID và cùng ảnh | Chỉ một message; không upload/thuộc tính ngữ cảnh trùng |
| CON-13 | P0 | C1 mở hai checkout ví, hai đơn có tổng tiền lớn hơn WC1, gửi đồng thời | Không trừ vượt ví; đơn bị từ chối không để lại kho/payment/giỏ cập nhật một phần |
| CON-14 | P0 | M1 và A cùng xác nhận hoàn một đơn vào ví khách | Chỉ một REFUND, một khoản cộng WC1 và một lần đảo tiền cửa hàng |
| CON-15 | P0 | Hai đơn cùng hoàn lần đầu vào ví khách chưa có bản ghi | Tạo một ví và cộng đủ hai khoản hợp lệ; không mất cập nhật hoặc ghi trùng ví |
| CON-16 | P0 | Hai manager/admin cùng từ chối một yêu cầu rút ví cửa hàng | Hoàn tiền giữ lại đúng một lần; trạng thái cuối nhất quán |
| CON-17 | P0 | Khách dùng ví mua đơn mới trong lúc nhận hoàn một đơn khác | Khóa số dư đúng; cuối cùng khớp số đầu kỳ + hoàn − thanh toán, không ghi đè một phía |
| CON-18 | P0 | NV1 hoàn tất COD cùng lúc M1 cập nhật cùng đơn | Thu COD, thu nhập, điểm và trạng thái chỉ được ghi một lần |
| CON-19 | P0 | Một request hủy thất bại giữa bước hoàn kho và cộng ví trong fixture lỗi | Toàn bộ transaction rollback; không tồn tại hoàn kho nhưng chưa xử lý tiền hoặc ngược lại |
| CON-20 | P0 | Hai máy ứng dụng cùng kết nối DB test, tranh hàng cuối hoặc cùng xác nhận hoàn | Quy tắc đúng giữa hai tiến trình, không chỉ dựa khóa trong bộ nhớ của một máy |

### 4.21 Ví khách hàng và thanh toán từ ví — CW

Tiền điều kiện: `/customer/wallet`; C1/C2 là hai USER độc lập; WC1 là số dư
khách, W/H là ví khả dụng/tiền chờ cửa hàng. Đơn test đủ 100.000đ gồm cả
phí và giảm giá đã chốt; chuẩn bị WC1 lần lượt 0/99.999/100.000/200.000đ.
Việc tạo dữ liệu ví trực tiếp chỉ được dùng trong fixture DB test cô lập.

Các ca tiền phải đối chiếu `customer_wallets`, `customer_wallet_entries`,
order, payment và sổ giao dịch cửa hàng bằng truy vấn chỉ đọc trên DB test
hoặc công cụ kiểm thử backend. Đếm theo ID đơn, không chỉ đọc thông báo UI.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| CW-01 | P1 | C1 chưa từng có ví mở trang Ví của tôi, tải lại nhiều lần | Hiện 0đ/lịch sử rỗng hợp lý, không lỗi thiếu bản ghi và không tự sinh tiền |
| CW-02 | P0 | Guest/A/M1/NV1 mở trực tiếp `/customer/wallet` hoặc sửa username/userId sang C2 | Chỉ USER đủ quyền xem ví chính mình; không lộ số dư/lịch sử C2 |
| CW-03 | P0 | C1 trả MoMo mô phỏng 100.000đ rồi tự hủy trước chuẩn bị với lý do hợp lệ | WC1 tăng 100.000đ; một REFUND; order/payment REFUNDED; tiền chờ/kho xử lý một lần; WC2 không đổi |
| CW-04 | P0 | Hủy COD chưa trả, MoMo chưa xác nhận, hoặc đơn hết hạn chưa thanh toán | Không cộng tiền vào ví khách; kho chỉ hoàn đúng một lần |
| CW-05 | P0 | Gửi lại hủy đã hoàn hoặc mở lại QR cũ | Không có REFUND/tiền/kho tăng lần hai; không hồi sinh đơn đã kết thúc |
| CW-06 | P1 | WC1 đủ tổng thanh toán, mở checkout rồi chọn Ví của tôi | Hiện đúng số dư, lựa chọn ví bật và chọn được; không tính tiền chờ cửa hàng vào WC1 |
| CW-07 | P0 | WC1 đúng bằng tổng đã tính ship/voucher; đặt WALLET_DEMO | Thành công, số dư còn 0đ; dùng tổng cuối cùng, không chỉ tiền hàng trước phí/giảm |
| CW-08 | P1 | WC1 thiếu 1đ so với tổng; quan sát lựa chọn ví và thử bấm | Hiện Số dư ví không đủ, giao diện mờ và không chọn được; COD/MoMo vẫn dùng theo điều kiện riêng |
| CW-09 | P0 | WC1 không đủ nhưng sửa radio/hidden field/request thành WALLET_DEMO | Server từ chối; ví, kho, voucher, giỏ và số đơn không đổi do yêu cầu thất bại |
| CW-10 | P0 | WC1=200.000đ, đặt một đơn ví 100.000đ rồi reload | WC1=100.000đ; một PAYMENT đúng đơn và balanceAfter; order PAID, tiền vào H, chưa tăng W |
| CW-11 | P0 | Hủy đơn WALLET_DEMO đã trả khi còn PENDING/CONFIRMED | Hoàn về chính ví đã trừ; một PAYMENT và một REFUND; WC1 về số trước đơn; không trả MoMo/ngân hàng thật |
| CW-12 | P0 | M1/A hủy ngoại lệ đơn ví/MoMo đã trả tại PREPARING | REFUND_REQUESTED; chưa cộng WC1; khách thấy đúng lý do/người hủy và trạng thái chờ hoàn |
| CW-13 | P0 | Sau CW-12, M1/A xác nhận hoàn rồi gửi lại yêu cầu | Chỉ lần hợp lệ đầu tiên cộng đúng WC1, giảm H/đảo thu nhập tương ứng; không có hoàn tiền hai lần |
| CW-14 | P0 | C1 yêu cầu hoàn đơn mô phỏng đã giao/hoàn tất rồi được xác nhận | Tiền về WC1 một lần; đảo đúng tiền thu nhập, hoa hồng và điểm của đơn; không tự hoàn kho đơn đã giao khi không có nghiệp vụ nhận trả |
| CW-15 | P1 | Refresh ví, Back/Forward, đăng xuất/đăng nhập và mở nhiều tab | Số dư khớp server, không sinh giao dịch chỉ vì xem trang; không lưu nhầm số dư C1 cho C2 |
| CW-16 | P1 | Có 16 và 31 giao dịch, xem từng trang lịch sử | Mỗi trang tối đa 15 dòng; thứ tự nhất quán, không mất/trùng dòng giữa trang trên bộ dữ liệu đứng yên |
| CW-17 | P1 | Lọc chỉ Từ ngày, chỉ Đến ngày, cả hai trống | Hỗ trợ từng tổ hợp; kết quả đúng phạm vi, không lỗi tham số PostgreSQL NULL |
| CW-18 | P1 | Giao dịch tại 00:00, 23:59:59 của hai biên ngày và 00:00 hôm sau | Bao gồm trọn ngày đầu/cuối theo múi giờ ứng dụng, loại giao dịch sang ngày kế tiếp |
| CW-19 | P1 | Ngày sai định dạng, ngày không tồn tại, năm 0000/10000, biên 9999-12-31 | Dữ liệu sai báo lỗi, không 500; biên hợp lệ không tràn ngày; số dư không thay đổi |
| CW-20 | P1 | Từ ngày sau Đến ngày rồi sửa lại hợp lệ | Có thông báo và không trả lịch sử sai; sửa xong lọc hoạt động bình thường |
| CW-21 | P1 | Lọc có nhiều trang → chuyển trang → xóa bộ lọc | Giữ ngày khi phân trang; xóa lọc về danh sách phù hợp; bộ lọc không làm thay đổi số dư tổng ví |
| CW-22 | P0 | Mở hai tab checkout khi WC1 đủ một đơn, tab thứ nhất trả hết ví, tab kia tiếp tục | Tab cũ không vượt kiểm tra server; số dư không âm, không bán miễn phí |
| CW-23 | P0 | Gửi đồng thời hai thanh toán hoặc hai hoàn của cùng đơn | Khóa và ràng buộc duy nhất ngăn PAYMENT/REFUND trùng; kết quả tài chính đúng một lần |
| CW-24 | P0 | Hai khoản hoàn khác đơn cùng tạo ví lần đầu cho C1 | Chỉ một ví của C1, nhận đủ hai khoản; không mất cập nhật và không tạo ví cho C2 |
| CW-25 | P0 | Tiêm lỗi DB test sau khi trừ ví nhưng trước khi checkout commit | Rollback toàn bộ ví/sổ/đơn/payment/kho/voucher; không hiện thành công giả |
| CW-26 | P0 | Fixture payment sai số tiền, sai method/environment/mã giao dịch hoặc thiếu debit của đơn ví | Từ chối hoàn không khớp; số dư các bên không đổi; không tạo khoản hoàn để che dữ liệu sai |
| CW-27 | P0 | C1 mở link đơn của một dòng ví thuộc C2 hoặc thay ID trong request | Không đọc/chi tiền/hoàn thay người khác; principal và chủ tài nguyên đều được kiểm |
| CW-28 | P0 | Tắt thanh toán mô phỏng rồi gửi yêu cầu WALLET_DEMO trực tiếp | Không thanh toán từ ví bằng cách vượt UI; không mất số dư đã có |
| CW-29 | P0 | Tìm nút nạp/rút/chuyển ví khách hoặc gửi yêu cầu giả tới đường dẫn tự đặt | Không có chức năng tiền thật/nạp/rút/chuyển trái phạm vi; không thay đổi tiền bằng request không hỗ trợ |
| CW-30 | P2 | Soát giao diện ví trên desktop/mobile và thông báo hoàn | Không còn số thẻ giả hoặc khối Thanh toán 1 chạm đã bỏ; số tiền dễ đọc, có lọc ngày; không tuyên bố đã hoàn vào MoMo/ngân hàng thật |

### 4.22 Bàn làm việc và phạm vi STAFF — NV

Tiền điều kiện: NV1 thuộc S1, NV2 thuộc S2, NV0 chưa phân công; các tài khoản
độc lập. Dùng đơn test ở đúng/sai cửa hàng, có cả COD, MoMo/ví đã thanh toán
và đơn chuyển khoản di sản chỉ khi chuẩn bị được fixture hợp lệ.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| NV-01 | P1 | NV1 đăng nhập, mở `/staff/orders`, preparation, delivery, chat | Đúng bàn làm việc, danh tính và S1; sidebar/đường dẫn không chuyển sang chức năng quản lý |
| NV-02 | P0 | NV0 đăng nhập và thử gõ ID đơn/cửa hàng bất kỳ | Hướng dẫn liên hệ quản lý; không tự lấy cửa hàng mặc định hoặc xem dữ liệu không được phân công |
| NV-03 | P1 | Tìm mã đơn có/không dấu #, tên/SĐT; lọc trạng thái và hình thức nhận | Chỉ đơn khớp thuộc S1; xóa lọc/phân trang giữ đúng điều kiện và tổng số |
| NV-04 | P1 | Mở chi tiết có nhiều hoa, lịch hẹn/lời chúc và phí/giảm | Đúng thông tin chốt lúc đặt; không tính lại giá từ sản phẩm hiện tại |
| NV-05 | P0 | Xác nhận đơn rồi bắt đầu chuẩn bị, thử hủy modal và xác nhận | Chỉ request xác nhận hợp lệ thay đổi; có cảnh báo khách mất quyền tự hủy từ PREPARING |
| NV-06 | P0 | Đơn quầy COD chưa trả: bỏ xác nhận đã giao hoặc chưa thu đủ tiền rồi gửi | Chặn cả UI/backend; không COMPLETED/PAID, không ghi tiền vào ví cửa hàng |
| NV-07 | P0 | Đơn quầy COD: xác nhận đã giao và đã thu đủ rồi hoàn tất | Đi thẳng từ chuẩn bị tới hoàn tất theo luật pickup; thu đúng một lần, tiền còn thu 0, không qua trạng thái giao tận nơi |
| NV-08 | P0 | Đơn giao COD đi chuẩn bị → đang giao → đã giao/thu đủ → hoàn tất | Đúng thứ tự, ghi nhận COD/thu nhập một lần tại mốc hợp lệ; hoàn tất không thu lại |
| NV-09 | P0 | Đơn MoMo/ví đã PAID được giao/nhận tại quầy | Không yêu cầu thu COD thêm; chỉ kết chuyển tiền chờ đúng một lần |
| NV-10 | P0 | Đơn trả trước chưa PAID, cố gửi thao tác giao/hoàn tất | Bị chặn; nếu chuyển khoản di sản thì chuyển quản lý xác nhận tiền, STAFF không tự xác nhận |
| NV-11 | P0 | Bỏ bước, gửi lại trạng thái cũ hoặc gửi trạng thái không tồn tại | Không chuyển sai/lùi hoặc tạo doanh thu/trừ kho lần nữa |
| NV-12 | P0 | NV2 dùng URL chi tiết/cập nhật đơn S1; NV1 sửa ID sang S2 | Bị chặn, không lộ người nhận/lời chúc hoặc đổi dữ liệu chi nhánh khác |
| NV-13 | P0 | NV1 gửi hủy từ cửa hàng, hoàn tiền, duyệt bank và truy cập ví/sản phẩm/báo cáo/nhân sự | STAFF không được làm các thao tác này qua UI hoặc request trực tiếp |
| NV-14 | P1 | NV1 trả lời chat khách S1, xem lịch sử và gửi ảnh hợp lệ | Đúng hội thoại/ảnh; không thấy kênh khách–admin hoặc hỗ trợ nội bộ Manager–Admin |
| NV-15 | P0 | M1 gỡ NV1 trong lúc NV1 mở đơn/chat/socket rồi thử thao tác tiếp | Quyền S1 mất hiệu lực ở request/subscribe/send sau; lịch sử vẫn giữ |
| NV-16 | P0 | Gỡ NV1 ở S1 rồi thêm vào S2, giữ tab và socket S1 cũ | Chỉ dữ liệu S2 được truy cập; không tiếp tục nhận/gửi tin S1 |
| NV-17 | P1 | Manager đổi trạng thái khi NV1 đang xem trang và nhập thông tin | Có nhắc cập nhật phù hợp, không tự reload làm mất phần đang nhập; gửi từ trạng thái cũ vẫn kiểm lại server |
| NV-18 | P2 | Bàn làm việc trên điện thoại; mở/đóng sidebar bằng nút, nền tối và Escape | Điều hướng dùng được, giữ mục hiện tại, không che nút xử lý đơn hoặc cuộn ngang khó dùng |

### 4.23 Phân trang lớn và đồng bộ bộ lọc — PAG

Tiền điều kiện: tạo dữ liệu test để có 0/1/2/6/7/8/100 trang trên các màn
hình sản phẩm, cửa hàng, người dùng, đơn khách, admin, manager, STAFF và
lịch sử ví. Không tạo hàng nghìn record trên DB chung để thử phân trang.

Đây là tiêu chí nghiệm thu trải nghiệm trên dữ liệu lớn, không khẳng định
mọi màn hình hiện đã có dấu ba chấm. Khi đối chiếu mã nguồn ngày 28/10,
phần đơn khách còn vòng lặp toàn bộ số trang; phải chạy PAG-04/05 để đánh
giá, không ghi PASS chỉ vì dữ liệu hiện mới có vài trang.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| PAG-01 | P1 | Danh sách không dữ liệu hoặc chỉ một trang | Trạng thái rỗng/đủ dữ liệu đúng; không có nút trỏ tới trang âm hoặc trang không tồn tại |
| PAG-02 | P1 | Đang trang đầu bấm Trước; đang cuối bấm Sau, cả bàn phím và chuột | Điều khiển tương ứng vô hiệu, không tạo request sai trang |
| PAG-03 | P1 | Chọn số trang rồi đối chiếu số trang UI với tham số backend | Ánh xạ 0-based/1-based đúng từng module, không bỏ trang đầu hoặc lặp trang |
| PAG-04 | P1 | Có 100 trang, lần lượt mở trang 1, 50 và 100 | Thanh số trang gọn, có trang đầu/cuối và vùng lân cận hoặc cơ chế tương đương; không dàn 100 nút gây tràn ngang |
| PAG-05 | P1 | Lặp PAG-04 ở rộng 360px/390px và desktop 1366px | Nút rõ, không chồng chữ, không bị cắt; trang có thể điều hướng thực sự trên mobile |
| PAG-06 | P1 | Lọc/tìm/sắp xếp ở trang 2 rồi chuyển trang kế/trước | Từ khóa, trạng thái, chi nhánh, ngày và kích thước trang vẫn được giữ đúng phạm vi |
| PAG-07 | P1 | Từ trang cao đổi lọc khiến chỉ còn một trang hoặc xóa record cuối trang | Quay về trang hợp lệ hoặc có phản hồi nhất quán, không màn hình trống giả/500 |
| PAG-08 | P1 | Nhập page âm, chữ, rất lớn; size 0/âm/quá lớn theo từng endpoint | Validation/giới hạn hoặc phản hồi 4xx phù hợp; không lỗi server, truy vấn vô hạn hoặc lộ dữ liệu |
| PAG-09 | P1 | Đối chiếu tổng số record, số trang và số dòng thực tế sau lọc | Tổng theo toàn bộ kết quả lọc, không lấy số dòng của trang hiện tại làm tổng |
| PAG-10 | P1 | Dữ liệu đứng yên, xem mọi trang rồi Back/Forward | Không mất/trùng ID; URL và trang đang chọn nhất quán |
| PAG-11 | P1 | Lọc ngày lịch sử ví rồi phân trang | Đủ giao dịch đúng khoảng, số dư tổng không bị biến thành tổng riêng trang |
| PAG-12 | P2 | Tab bằng bàn phím qua số trang, mục hiện tại và dấu ba chấm | Có focus rõ; dấu ba chấm không thành link lỗi; trang hiện tại được phân biệt không chỉ bằng vị trí |

## 5. Các ca tùy điều kiện: luồng cũ và vận hành

### 5.1 Ngân hàng và MoMo backend cũ — LEG

Chỉ chạy khi có **đơn/fixture cũ phù hợp trên DB test**, cấu hình cần thiết và biết rõ cổng đang ở mock/sandbox. Không chọn BANK_TRANSFER ở checkout mới; không dùng tiền thật. Thiếu fixture ghi BLOCKED hoặc N/A theo phạm vi nghiệm thu.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| LEG-01 | P1 | Mở `/checkout/bank/{orderId}` cho đơn cũ thuộc C1 | Đúng thông tin/số tiền/nội dung chuyển; C2 không xem/xử lý được |
| LEG-02 | P0 | M1 xác nhận tiền bank đúng; thử xác nhận lần hai | PAID một lần; chưa giao thì chưa ghi thu nhập tùy mốc nghiệp vụ |
| LEG-03 | P0 | Bank chưa PAID cố giao/hoàn tất pickup | Bị chặn, không cộng thu nhập |
| LEG-04 | P0 | Bank quá hạn UNPAID trước chuẩn bị; sau đó tiền đến muộn | Hủy/trả kho; xác nhận tiền muộn thành REFUND_REQUESTED, không hồi sinh giao hàng |
| LEG-05 | P0 | Hủy bank đã PAID trước chuẩn bị | Chờ hoàn tiền; chỉ REFUNDED khi quản lý xác nhận hoàn thực tế |
| LEG-06 | P1 | Mở `/checkout/momo/{orderId}` và retry đơn MOMO cũ trong mock | Đúng owner, tiền và hạn; lỗi cổng giữ đơn để thử lại, không tạo đơn trùng |
| LEG-07 | P0 | Callback/IPN sai chữ ký/số tiền/orderId/kết quả; lặp callback hợp lệ | Không giả PAID, không thanh toán/thu nhập hai lần |
| LEG-08 | P0 | Trong sandbox thật, chỉ quay về callback URL mà IPN chưa xác minh | Không tin tham số return client để tự thanh toán |
| LEG-09 | P0 | Mock/simulate khi config tắt hoặc prod | Không mở đường thanh toán giả ngoài môi trường cho phép |
| LEG-10 | P1 | Đơn cũ có trạng thái PICKED_UP/RETURNED/RENTING/OVERDUE | Hiển thị lịch sử không 500; không biến thành đường tạo đơn thuê/shipper mới |
| LEG-11 | P0 | Hủy đơn di sản P1 ghi S2 nhưng P1 riêng S1 và kho S2 bị cách ly | Không tái tạo kho bán giả S2; ghi nhận ngoại lệ dữ liệu để đối soát |

### 5.2 Cấu hình, migration và phục hồi — OPS

Các ca migration dùng database trắng/bản sao riêng. Không chạy SQL sửa dữ liệu hoặc migration thủ công chỉ để “thử xem” trên database đang dùng.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| OPS-01 | P0 | Khởi động với database trắng, đúng cấu hình và seed demo tắt | Flyway dựng schema; app chạy, màn hình rỗng dùng được, không phụ thuộc account demo |
| OPS-02 | P1 | Bật seed rõ ràng trên DB demo trắng rồi khởi động lại | Đúng dữ liệu mẫu, không nhân đôi; không tự ghi đè dữ liệu đã sửa khi DB có sẵn |
| OPS-03 | P0 | Nâng bản sao SQL Server cũ qua lịch sử migration hiện có đến V17 | Migration thành công, lịch sử/ảnh/giá/kho/đơn giữ theo phạm vi; V16 tách STAFF, V17 thêm ví khách; không áp lịch sử SQL Server này lên PostgreSQL |
| OPS-04 | P0 | Sau V7 kiểm một mẫu nhiều danh mục và bảng lưu phân loại cũ | Phân loại chuyển đúng, không nhân mẫu; danh mục tự tạo không bị đổi ngoài thiết kế |
| OPS-05 | P0 | Sau V8 kiểm trạng thái branch catalog, kho/đơn trước sau | Thêm cờ đúng, không đổi lượng kho/đơn cũ |
| OPS-06 | P0 | Sau V10 kiểm đơn demo cũ cần đối soát và số dư chờ | Không tự tạo tiền khả dụng giả; dữ liệu không khớp được nhận diện để đối soát |
| OPS-07 | P1 | Chạy lại app/migration trên DB đã nâng cấp | Không chạy lặp làm hỏng dữ liệu, không duplicate schema/seed |
| OPS-08 | P1 | Sai kết nối DB/DB offline lúc khởi động; lỗi DB trong trang danh sách | Lỗi được nhận diện; không báo thao tác tiền/kho thành công giả, không lộ secret trong UI |
| OPS-09 | P1 | Cloudinary cấu hình sai, mail API lỗi, tunnel lỗi từng dịch vụ | Chức năng phụ thuộc báo lỗi rõ; phần không phụ thuộc còn sử dụng được |
| OPS-10 | P0 | Khởi động prod test với thiếu JWT secret/cấu hình bắt buộc; sau đó cấu hình hợp lệ | Không âm thầm dùng secret demo; prod hợp lệ dùng cookie Secure/HTTPS và demo tắt |
| OPS-11 | P1 | Restart app sau dữ liệu đơn/kho/ví đã commit | Dữ liệu bền vững; không đòi session còn tồn tại qua restart vì chỉ có dependency JDBC không chứng minh đã bật session persistence |
| OPS-12 | P1 | Đối chiếu log Network/Console/server của ca lỗi | Không OTP, mật khẩu, API secret, raw token trong log/response công khai |
| OPS-13 | P1 | Sao lưu DB test → phục hồi sang DB riêng → chạy smoke | Phục hồi được đơn/kho/ví/lịch sử nhất quán, không phụ thuộc dữ liệu bộ nhớ |
| OPS-14 | P1 | Chạy script test JS/Cloudinary env theo README | Script dùng dữ liệu giả khi thích hợp, không gửi ảnh/thay config thật ngoài phạm vi; ghi kết quả thực tế riêng |

### 5.3 Cấu hình Aiven và máy mới — AIV

Tiền điều kiện: database PostgreSQL test tách biệt, chứng chỉ đúng dịch vụ,
JDK 21 trở lên, Maven, cấu hình `.env.aiven` riêng từng máy. Các ca sai
mật khẩu/chứng chỉ dùng cấu hình test hoặc tiến trình riêng; không reset
mật khẩu chung của nhóm hoặc sửa cấu hình phiên đang dùng để thử lỗi.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| AIV-01 | P0 | Trên máy đã thiết lập, chạy `mvn spring-boot:run` từ đúng thư mục | Chọn aiven, TLS hợp lệ, kết nối đúng PostgreSQL test, website hoạt động và Cloudflare chạy theo cờ cấu hình |
| AIV-02 | P1 | Dùng mật khẩu test sai, tài khoản không tồn tại hoặc mật khẩu dán thiếu | Kiểm tra đăng nhập báo thất bại phù hợp; không lưu cấu hình thành công giả hoặc thay đổi dữ liệu |
| AIV-03 | P0 | Thiếu CA, CA khác dịch vụ, host sai hoặc chứng chỉ không xác minh được | Không bỏ qua TLS hoặc tự chuyển trust-all; hướng dẫn sửa đúng chứng chỉ/host |
| AIV-04 | P0 | Máy mới chạy `run-aiven.ps1 -Mode Check -SaveLocalConfiguration`, nhập bí mật riêng trên máy | Chỉ ghi `.env.aiven` sau kiểm tra TLS/login thành công; tạo khóa JWT ngẫu nhiên, không in mật khẩu/khóa ra màn hình |
| AIV-05 | P1 | Cài ở đường dẫn có khoảng trắng; CA đặt khác thư mục mặc định và truyền `-CaCertificate` | Xử lý đường dẫn đúng; cấu hình được lưu tới đúng project, không đọc nhầm chứng chỉ khác |
| AIV-06 | P0 | Kiểm Git, log, bản ZIP bàn giao và quyền truy cập file cấu hình sau thiết lập | Không có mật khẩu/khóa trong Git/log/bản bàn giao; file riêng không công khai và chỉ chủ tài khoản được quyền đọc theo thiết lập |
| AIV-07 | P0 | Cố kết hợp aiven với dev hoặc test khi khởi động | Bị chặn trước kết nối/ghi DB; không trộn migration hoặc lấy credential SQL Server cũ |
| AIV-08 | P0 | Kiểm `.env` cũ và các biến `SPRING_*` kế thừa trước smoke test | Xác nhận URL/driver/schema thực sự là PostgreSQL test; nếu override sai thì BLOCKED và sửa cấu hình trước mọi mutation, không tiếp tục test nhầm DB |
| AIV-09 | P1 | Thiếu/sai email API hoặc Cloudinary trên môi trường test | Chức năng phụ thuộc báo lỗi đúng, không tạo kết quả gửi mail/upload giả; phần không phụ thuộc không bị lỗi dây chuyền |
| AIV-10 | P0 | Chạy truy vấn báo cáo và lịch sử ví với ngày NULL, một biên, hai biên trên PostgreSQL | Không lỗi kiểu timestamp/tham số không xác định; đúng dữ liệu và phạm vi quyền |
| AIV-11 | P1 | Tạo dữ liệu test tiếng Việt, emoji, mô tả dài và STAFF rồi đọc lại | Chuỗi, vai trò và trường tiền giữ chính xác; không lỗi NVARCHAR/T-SQL trên PostgreSQL |
| AIV-12 | P1 | Mở một vài phiên ứng dụng test và quan sát số kết nối | Mỗi pool không vượt mức cấu hình 3; không tăng vô hạn; phân biệt số kết nối với số người và chừa kết nối cho quản trị |
| AIV-13 | P0 | Làm DB test mất kết nối lúc khởi động hoặc giữa transaction kiểm thử | Không báo hoàn tất giả; mutation chưa commit rollback; không lộ credential/SQL nhạy cảm qua UI |
| AIV-14 | P0 | Khởi động hai máy/tiến trình nối cùng DB test đã có dữ liệu | Không seed/reset/ghi đè tài khoản, đơn, kho, ví; dữ liệu commit của một máy được máy kia đọc đúng |
| AIV-15 | P0 | Dựng PostgreSQL test rỗng bằng baseline Flyway riêng, rồi chạy lại | Schema public đúng entity, lịch sử PostgreSQL V1 hợp lệ; lần sau không chạy lặp tạo bảng/trùng dữ liệu |
| AIV-16 | P0 | Fixture DB đã có lịch sử/schema SQL Server hoặc lịch sử migration sai | Không âm thầm baseline/clean/đè schema; báo lỗi và giữ DB để đối soát |
| AIV-17 | P1 | Tài khoản DB test thiếu quyền cần thiết; thử dùng tài khoản đã được cấp đúng quyền | Thiếu quyền báo lỗi rõ, không tạo schema một phần; không yêu cầu chia sẻ tài khoản quản trị vào code |
| AIV-18 | P0 | Sau giao dịch test đã commit, restart app rồi xem lại đơn, kho, ví và khóa JWT cục bộ | Dữ liệu bền vững, khóa lưu hợp lệ không bị sinh lại vô cớ; QR/session bộ nhớ cũ được xử lý theo thiết kế, không nhân đôi đơn |
| AIV-19 | P0 | Tắt mô phỏng qua cấu hình, restart và thử gửi method demo từ request cũ | Không mở lại MoMo/ví giả trái cờ; cờ tắt Cloudflare riêng cũng có hiệu lực |
| AIV-20 | P1 | Cấu hình hai tài khoản DB test khác nhau trên hai máy bằng DatabaseUser | Mỗi máy dùng đúng tài khoản được cấp, quyền DB đủ/hạn chế theo chính sách; không phải gửi `.env.aiven` hoặc khóa JWT giữa các máy |

### 5.4 Cloudflare, QR và phạm vi công khai — CF

Tiền điều kiện: gateway và supervisor hiện có, Node.js, cloudflared phù hợp
Windows; mạng thử nghiệm được phép tạo tunnel. Không thử vượt quyền trên
tunnel/dịch vụ của người khác. Không ghi token thanh toán đầy đủ vào báo
cáo công khai; đường dẫn QR là một khả năng xác nhận thanh toán mô phỏng.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| CF-01 | P1 | Chạy ứng dụng aiven bằng lệnh Maven mặc định, đợi kết nối | Website tự gọi gateway và Cloudflare; chỉ báo READY sau kiểm tra đúng gateway, không phải ngay khi thấy URL trong log |
| CF-02 | P0 | Thử dev/aiven với các cờ bật/tắt; thử prod/test có cờ bật | Chỉ dev/aiven và đủ cờ mới tự mở tunnel; prod/test không tự công khai cổng thanh toán |
| CF-03 | P0 | Mở thanh toán lúc tunnel STARTING/ERROR và có URL cũ trong cấu hình | Hiện trạng thái chờ/lỗi phù hợp, chưa phát QR cũ, IP Tailscale hoặc link chưa kiểm tra được |
| CF-04 | P0 | Trong test supervisor, đưa URL sai scheme/host, có userinfo/port/path/query/fragment | Không quảng bá URL không đúng origin trycloudflare HTTPS đã kiểm tra; không dùng dữ liệu log tùy ý để tạo QR |
| CF-05 | P0 | Qua tunnel mở asset thanh toán rồi admin/customer/wallet/checkout/chat/API ngoài danh sách, kể cả path mã hóa | Asset hợp lệ tải được; đường dẫn ngoài phạm vi bị chặn, không lộ website quản trị hoặc tài khoản |
| CF-06 | P0 | Guest dùng QR public hợp lệ; C2 mở endpoint desktop/QR/status của C1 | Guest được dùng đúng token public mô phỏng; endpoint của chủ đơn vẫn kiểm quyền, C2 không xem được dữ liệu riêng |
| CF-07 | P1 | Quét QR mới trên điện thoại cùng Wi-Fi và mạng di động | Mở HTTPS hiện tại, đúng đơn/số tiền; không cần IP Tailscale hoặc localhost trên điện thoại |
| CF-08 | P1 | Hai máy chạy cùng DB, tạo phiên QR riêng rồi mở đúng/sai tunnel của phiên | Chỉ token tồn tại ở tiến trình sở hữu mới dùng được; token sai báo lỗi, không gán nhầm đơn hoặc thanh toán chéo |
| CF-09 | P0 | Ngắt mạng/cho child tunnel kết thúc trong lúc còn QR | Thu hồi READY/link cũ; không báo thanh toán thành công; website còn chạy và có phản hồi kết nối |
| CF-10 | P1 | Phục hồi mạng để supervisor tạo origin mới | Trang thanh toán cập nhật link/ảnh QR mới mà không cần hardcode hoặc restart website; QR cũ không được dùng như link hiện tại |
| CF-11 | P1 | Thiếu node, thiếu cloudflared hoặc đường dẫn công cụ không đúng | Có lỗi hướng dẫn xử lý; không treo vô hạn toàn bộ website và không phát link giả |
| CF-12 | P1 | Ctrl+C ở phiên ứng dụng do test tạo, kiểm gateway và child process | Gateway/Cloudflare của phiên đó được thu dọn; không dừng Java/tunnel của ứng dụng khác; có thể chạy lại cổng cũ |
| CF-13 | P1 | Phát nhiều ApplicationReadyEvent hoặc khởi động lại kết nối | Một supervisor cho mỗi phiên ứng dụng; reconnect không để lại nhiều worker/tunnel mồ côi |
| CF-14 | P0 | Kiểm listener website, listener gateway và root tunnel | Website/gateway vẫn theo địa chỉ local đã cấu hình; root tunnel chỉ kiểm tra kết nối, không trở thành trang chủ hoặc mở cả port quản trị |
| CF-15 | P0 | Kiểm môi trường child process bằng test tự động, không in giá trị bí mật | Không truyền AIVEN_PASSWORD/JWT_SECRET/mail/API key hoặc toàn bộ biến SPRING vào Node/cloudflared |
| CF-16 | P0 | Gửi lại xác nhận public QR khi đã thành công, đã hủy, hết hạn hoặc chủ bị khóa | Tuân thủ luật thanh toán/idempotency của backend; tunnel không trở thành đường bỏ qua trạng thái hoặc tạo tiền thật |

### 5.5 Công cụ sao chép SQL Server sang Aiven — MIG

Chỉ áp dụng khi nghiệm thu công cụ chuyển dữ liệu. Dùng hai database thử
nghiệm được phép, target chưa có dữ liệu nghiệp vụ. **Không chạy import,
export nhạy cảm hoặc phục hồi trên database chung chỉ để thực hiện tài liệu.**
Đặt BLOCKED nếu thiếu quyền, bản sao, cửa sổ ngừng ghi hoặc người phụ trách.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| MIG-01 | P0 | Export nguồn test đúng schema sau xác nhận tạm ngừng ghi | Chỉ đọc nguồn, tạo snapshot nhất quán; không khóa/xóa/sửa dữ liệu nguồn ngoài giao dịch đọc được thiết kế |
| MIG-02 | P0 | Kiểm file backup, file khóa, Git và log sau export | Payload mã hóa; khóa được bảo vệ cục bộ; không có dump mật khẩu/PII/khóa dạng rõ trong Git hoặc log |
| MIG-03 | P0 | Dùng backup bị sửa, sai khóa, sai phiên bản/DB nguồn | Từ chối trước import; không ghi dữ liệu một phần vào target |
| MIG-04 | P1 | Chạy Rehearse với snapshot test trên PostgreSQL cục bộ | Không kết nối/ghi Aiven thật; đối chiếu dữ liệu sau chuyển và báo kết quả thực tế |
| MIG-05 | P0 | Chạy Plan với đúng target test | Chỉ đọc và báo kế hoạch, không tạo bảng archive/đổi số dư hoặc lịch sử |
| MIG-06 | P0 | Target đã có một bản ghi nghiệp vụ, archive hoặc lịch sử schema không đúng | Từ chối, không truncate/xóa/ghi đè để ép import thành công |
| MIG-07 | P0 | Import thiếu xác nhận ngừng ghi hoặc không nhập đúng câu xác nhận | Không bắt đầu chuyển; không tự đoán sự đồng ý của nhóm |
| MIG-08 | P0 | Import hợp lệ rồi đối soát số dòng, ID, Unicode, password hash, đơn, kho, ví, payment | Dữ liệu khớp bằng count/hash và kiểm tra nghiệp vụ; giữ ID/quan hệ/tiền, không chạy lại dịch vụ để sinh thu nhập/hoàn tiền |
| MIG-09 | P0 | Kiểm dữ liệu nguồn phụ/di sản và phiên xác thực sau import | Phần được lưu trữ giữ trong archive hạn chế quyền; không kích hoạt lại OTP/JWT/session cũ; PostgreSQL giữ Flyway riêng |
| MIG-10 | P0 | Sau import trên DB test, tạo bản ghi mới bằng luồng hợp lệ | Identity/sequence tiếp tục sau ID lớn nhất; không trùng PK hoặc sai quan hệ vòng users/stores/categories |
| MIG-11 | P0 | Tiêm lỗi FK, làm tròn tiền không khớp hoặc lỗi giữa import trên DB test | Rollback dữ liệu/DDL/sequence theo thiết kế; không báo committed nếu chưa hoàn tất đối soát |
| MIG-12 | P0 | Import thành công rồi chạy lại; đọc báo cáo đối soát và thử quyền đọc archive | Lần hai bị từ chối, không nhân dữ liệu; chỉ báo committed sau thành công; không công khai archive hoặc tạo backup thay thế nguồn không được kiểm chứng |

## 6. Các điểm cần chú ý khi kết luận

- Một số validation ở module danh mục/cấp bậc là **tiêu chí phát hiện thiếu kiểm tra**, không phải cam kết code hiện đã chặn toàn bộ. Nếu lưu được giá trị âm/rỗng/vô nghĩa, ghi FAIL kèm ảnh/request.
- Hạn thanh toán và hủy tự động khác nhau: scheduler hiện chỉ xét UNPAID ở PENDING/CONFIRMED, tối đa 100 đơn mỗi lượt. Không kết luận mọi đơn PREPARING hết hạn cũng tự hủy. Với nhiều đơn quá hạn cần chờ thêm các lượt.
- Nhận tại quầy không đi qua trạng thái vận chuyển. Với DELIVERY, ghi nhận COD/thu nhập tại DELIVERED; COMPLETED sau đó không được cộng thêm lần nữa.
- Tổng doanh thu trên dashboard, thu nhập sau hoa hồng trong ví và tiền chờ là các đại lượng khác nhau. Luôn dùng đúng đại lượng để so sánh.
- Chức năng giá khuyến mãi, form thêm sản phẩm của manager và form ProductForm của admin không hoàn toàn cùng trường; chỉ test trường thực có trên màn hình, dùng request/fixture riêng để kiểm quy tắc backend nếu cần.
- Không lấy việc test tự động có tên tương ứng làm bằng chứng ca thủ công PASS. Không lấy test H2/SQL Server PASS để kết luận migration/concurrency PostgreSQL PASS hoặc ngược lại.
- Ca PAG-04/05 là tiêu chí khả dụng cho nhiều trang; code phân trang đơn khách được đối chiếu còn hiển thị toàn bộ số trang. Ghi lỗi nếu chạy với 100 trang bị tràn, không sửa kỳ vọng thành “100 nút là đúng” để coi là đạt.
- Trang ví bỏ nhãn demo theo yêu cầu giao diện không làm thay đổi bản chất mô phỏng của WALLET_DEMO/MOMO_DEMO. Chỉ kiểm tiền thật khi có một phạm vi tích hợp thật riêng, được cho phép và có bằng chứng từ cổng thanh toán; tài liệu này không yêu cầu dùng tiền thật.
- MANAGER/STAFF chỉ một cửa hàng là luật hiện hành. Các phần ghi ngày cũ ở cuối chỉ bổ sung lịch sử; nếu có khác biệt thì dùng bảng quyền hiện tại và nhóm STAFF/NV trong tài liệu này.

## 7. Lịch chạy đề xuất và điều kiện nghiệm thu

1. **Lượt nhanh trước demo:** chạy E2E-01…12, PUB-01, AUTH-01/17/22, SEC-01…06, PAY-10/11, PROD-17…20, CW-01/03/06/09/12/13, NV-01/05/07/12, AIV-01/04 và CF-01/03/05. Thời gian tùy dữ liệu, OTP, hạn thanh toán và kết nối dịch vụ; không coi đây là toàn bộ hồi quy.
2. **Lượt chức năng:** chạy toàn bộ mục 4 theo nhóm; đánh dấu từng biến thể và chụp bằng chứng cho ca P0.
3. **Lượt hồi quy:** sau sửa lỗi, chạy lại ca lỗi và luồng chịu ảnh hưởng, ưu tiên tiền/kho/quyền.
4. **Lượt môi trường:** chạy mục 5 đúng điều kiện; ghi N/A/BLOCKED rõ ràng thay vì bỏ qua không ghi.

Điều kiện đề xuất: toàn bộ ca P0 trong phạm vi phải PASS; không có sai tiền, kho âm, rò dữ liệu hoặc quyền vượt chi nhánh. Các lỗi P1/P2 còn lại phải được ghi rõ cùng ảnh hưởng; thống kê NOT RUN/BLOCKED/N/A riêng. Không ghi “hoàn thiện 100%” chỉ vì đã đi được luồng chính.

## 8. Nguồn đối chiếu trong dự án

- Quyền: `config/SecurityConfig.java`, `config/JwtSecurityConfig.java`, `security/AccountStatusFilter.java`, `service/StoreAccessService.java`.
- Mua hàng: `controller/customer/CheckoutController.java`, `service/impl/CartServiceImpl.java`, `service/impl/OrderServiceImpl.java`, `service/ProductAvailability.java`, `utils/ProductPrices.java`.
- Đơn/tiền: `service/OrderWorkflowService.java`, `service/OrderFundsService.java`, `service/DemoPaymentService.java`, `service/WalletService.java`, `config/OrderExpirySchedule.java`.
- Quản lý: `service/StoreCreationService.java`, `service/StoreStaffManagementService.java`, `service/StoreProductService.java`, `service/CustomerAccountClosureService.java` và controller manager/admin tương ứng.
- Danh mục/giảm giá/review: `service/ProductAttributeService.java`, `service/impl/VoucherServiceImpl.java`, `service/impl/ReviewServiceImpl.java`, controller và DTO tương ứng.
- Thời gian: `service/ReportRange.java`, `service/impl/AnalyticsServiceImpl.java`, controller dashboard/analytics và `static/js/report-period.js`.
- Realtime: `service/ChatService.java`, controller chat/notification, cấu hình WebSocket và `static/js/chat-widget.js`, `notifications-realtime.js`, `stock-kpi.js`, `home-cart.js`, `checkout-quote.js`.
- Ví khách: `service/CustomerWalletService.java`, `controller/customer/CustomerWalletController.java`, `enums/CustomerCancellationReason.java`, repository ví và `static/js/customer-wallet.js`.
- Aiven/Cloudflare: `application-aiven.properties`, `config/AivenProfileConfiguration.java`, `service/DemoPaymentTunnel.java`, `scripts/run-aiven.ps1`, `scripts/demo-phone-tunnel.cjs`, `scripts/demo-phone-gateway.cjs`.
- Chuyển dữ liệu: `tools/DatabaseTransfer.java`, `scripts/transfer-aiven.ps1` và nhóm `PostgreSqlTransferTests`; các đường dẫn scripts tính từ thư mục gốc dự án.
- Test tham chiếu: `BusinessRegressionTests`, `ConcurrencyRegressionTests`, `BranchStockConsistencyTests`, `StoreProductCatalogTests`, `CheckoutEdgeSafetyTests`, `DemoPendingFundsTests`, `DemoPaymentTests`, `ReportRangeTests`, `ReportDateFilteringTests`, `ChatSecurityTests`, `ChatSocketAccessTests`, `JwtAuthenticationTests`, các `*UseCaseTests`, test JS và test SQL Server riêng.

Đường dẫn Java ở mục này tương đối từ `src/main/java/com/finalproject/`; tên test từ `src/test/java/com/finalproject/`. Xem thêm bản kê cấu trúc và route: [BAN_DO_CAU_TRUC_VA_ROUTE.md](BAN_DO_CAU_TRUC_VA_ROUTE.md).


## Bổ sung ngày 24/10/2026: mỗi manager phụ trách một cửa hàng

Quy định: một tài khoản MANAGER chỉ được quản lý một cửa hàng. Chủ chuỗi dùng tài khoản ADMIN để quản lý nhiều chi nhánh. Manager vẫn có thể tạo cửa hàng đầu tiên nếu chưa được phân công.

| Mã | Thao tác tự kiểm tra | Kết quả cần thấy |
|---|---|---|
| M1-01 | Đăng nhập manager đã có cửa hàng, mở Tổng quan | Chỉ thấy dữ liệu cửa hàng phụ trách, không có ô chọn cửa hàng khác |
| M1-02 | Mở Cửa hàng của tôi | Chỉ thấy một cửa hàng; không có nút thêm cửa hàng thứ hai |
| M1-03 | Gõ trực tiếp `/manager/stores/create` khi đã có cửa hàng | Quay về danh sách và thông báo mỗi manager chỉ quản lý một cửa hàng |
| M1-04 | Đổi `storeId` trên URL tổng quan, tồn kho, sửa cửa hàng sang chi nhánh khác | Bị từ chối quyền truy cập; không thay đổi dữ liệu |
| M1-05 | Thêm STAFF chưa được phân công vào cửa hàng bằng biểu mẫu nhân viên | Thêm thành công; giữ role STAFF và phân công đúng cửa hàng |
| M1-06 | Thêm STAFF đã thuộc cửa hàng khác hoặc giả requestedRole=MANAGER/ADMIN | Bị từ chối; role và phân công cũ giữ nguyên |
| M1-07 | Gỡ STAFF khỏi cửa hàng cũ rồi thêm vào cửa hàng mới | Chuyển được sang cửa hàng mới; mất quyền cửa hàng cũ |
| M1-08 | Đăng nhập manager chưa có cửa hàng, tạo cửa hàng đầu tiên | Tạo thành công và tự phân công vào cửa hàng vừa tạo |
| M1-09 | Mở chat/thông báo của cửa hàng khác, kể cả tài khoản còn liên kết cũ | Không được đọc, gửi hoặc nhận tin của cửa hàng khác |
| M1-10 | Đăng nhập admin, xem danh sách và thống kê nhiều chi nhánh | Có phạm vi toàn chuỗi; không áp giới hạn một chi nhánh của MANAGER cho ADMIN; chỉ thử thao tác thực có trên UI/endpoint |

Dữ liệu cũ: ưu tiên cửa hàng đã phân công trong tài khoản (`users.store_id`). Nếu chưa có phân công, chỉ chấp nhận khi tài khoản có đúng một cửa hàng liên kết. Nếu có nhiều liên kết mà chưa rõ cửa hàng phụ trách, hệ thống chặn truy cập và cần kiểm tra lại phân công; không tự chọn ngẫu nhiên hoặc xóa dữ liệu lịch sử.


## Bổ sung ngày 24/10/2026: tách quyền Staff

Quy định và 10 bước kiểm tra thủ công mới nhất: [Phân quyền cửa hàng](PHAN_QUYEN_CUA_HANG.md). Phần này thay thế mô tả cũ về PARTNER/nhân viên dùng quyền MANAGER ở các kịch bản STAFF và WAL trước đây. Nhân viên dùng STAFF, không truy cập các trang quản lý, không tự rút tiền hay rời chi nhánh qua trang quản lý. Quản lý thực hiện việc thêm/gỡ và chuyển chi nhánh.


## Bổ sung khu vực làm việc Nhân viên

Xem [Kịch bản test Nhân viên](KICH_BAN_TEST_NHAN_VIEN.md) để kiểm tra đầy đủ các trang đơn, chi tiết giao hàng, chuẩn bị hoa, giao/nhận hoa, thu COD và chat theo chi nhánh. Các bước này thay cho luồng Staff chỉ có một bảng đơn trước đây.

## 9. Tổ chức kiểm thử và chuyển sang Playwright Python

### 9.1 Thống kê phạm vi

Các con số dưới đây đếm dòng ca có mã riêng và mức P0/P1/P2, không đếm mỗi
biến thể đầu vào thành một ca mới. Không phải kết quả thực thi.

| Nhóm | Nội dung | Số ca |
|---|---|---|
| PUB | Trang công khai, tìm kiếm, lọc, cửa hàng, người dùng | 16 |
| AUTH | Đăng ký, OTP, đăng nhập, quên/reset mật khẩu | 27 |
| ACC | Hồ sơ, ảnh, địa chỉ, yêu thích, theo dõi, đã xem | 15 |
| CART | Giỏ hàng, số lượng, chọn dòng, giá và quyền sở hữu | 15 |
| CHK | Báo giá, nhận tại quầy/giao tận nơi, chi nhánh và checkout | 27 |
| VOU | Voucher toàn sàn/chi nhánh và tính giảm | 13 |
| PAY | COD và MoMo mô phỏng, QR, hạn thanh toán | 17 |
| ORD | Vòng đời đơn, lý do hủy, hoàn tiền và bộ lọc | 30 |
| STOCK | Kho theo chi nhánh và cập nhật hiện tại | 12 |
| STORE | Cửa hàng, cấp phép, thông tin và giới hạn một manager/một shop | 11 |
| STAFF | Thêm/gỡ/phân công nhân viên và thu hồi quyền | 10 |
| PROD | Sản phẩm, giá, ảnh, danh mục, ngừng bán/cấm/xóa mềm | 28 |
| ADMIN | Tài khoản, quyền, khóa/xóa, quản trị toàn chuỗi | 16 |
| MASTER | Danh mục, thuộc tính, cấp bậc, hoa hồng, đơn vị và phí giao | 12 |
| WAL | Ví cửa hàng, tiền chờ, thu nhập và rút tiền | 14 |
| REPORT | Dashboard, biểu đồ, khoảng ngày và đối soát số liệu | 16 |
| CHAT | Hội thoại, ảnh, realtime, phạm vi và giám sát | 21 |
| NOTI | Thông báo, số chưa đọc, realtime và quyền truy cập | 7 |
| REV | Đánh giá sản phẩm và tính toàn vẹn nhận xét | 8 |
| UI | Giao diện, bàn phím, đa thiết bị/trình duyệt và mạng | 12 |
| SEC | Phân quyền session, CSRF, IDOR và an toàn dữ liệu | 10 |
| API | Đăng nhập JWT, refresh, logout và thu hồi token | 10 |
| CON | Request đồng thời, gửi lặp và transaction tiền/kho | 20 |
| CW | Ví khách, hoàn vào ví, trả bằng ví và lịch sử ngày | 30 |
| NV | Bàn làm việc STAFF, giao/nhận, COD và giới hạn chi nhánh | 18 |
| PAG | Phân trang lớn, bộ lọc và khả dụng trên mobile | 12 |
| LEG | Thanh toán/trạng thái di sản, chỉ chạy khi có fixture | 11 |
| OPS | Khởi động, nâng cấp, cấu hình, sao lưu và phục hồi | 14 |
| AIV | Aiven, PostgreSQL, TLS, máy mới và bí mật cục bộ | 20 |
| CF | Tự chạy Cloudflare, QR, lỗi mạng và cổng công khai | 16 |
| MIG | Công cụ chuyển dữ liệu và đối soát | 12 |
| **Tổng** | **Không bao gồm 12 luồng E2E và checklist M1 bổ trợ** | **500** |

### 9.2 Không dùng Playwright thay cho mọi loại kiểm thử

| Hình thức | Phù hợp với ca nào | Cần kiểm chứng |
|---|---|---|
| Playwright Python qua trình duyệt | PUB/AUTH/ACC/CART/CHK/ORD/UI/PAG/NV và phần giao diện các module khác | Trang, trường, thông báo, điều hướng, nút theo quyền, kết quả người dùng nhìn thấy |
| Playwright kết hợp API được phép | Ownership, role, CSRF, gửi lại request, trạng thái cũ | Backend chặn dù bỏ qua nút UI; dùng cookie/CSRF/Bearer đúng loại từng endpoint |
| Test backend/transaction trên PostgreSQL test | Tiền/kho/hoa hồng, unique ledger, CON, CW, WAL, MIG | Số dư/count/dòng ledger trước sau, rollback và request thực sự chồng nhau |
| Kiểm thử dịch vụ tích hợp có kiểm soát | Mail/OTP, Cloudinary, Cloudflare, thiết bị điện thoại | Lỗi dịch vụ, kết nối thật và bằng chứng; mock PASS không chứng minh nhà cung cấp thật hoạt động |
| Thủ công hỗ trợ | Khả dụng, ảnh, nội dung dài, người dùng thật và màn hình nhỏ | Tính rõ ràng/thao tác được, không chỉ ảnh chụp có tồn tại |

Không gắn nhãn toàn bộ 500 ca là “đã tự động bằng Python”. Khi mới viết
Playwright, ưu tiên ca chỉ đọc trước, sau đó mutation trên DB test riêng.
Một bài kiểm thử có thể dùng nhiều lớp bằng chứng; chỉ PASS khi đủ các
điều kiện mong đợi của ca, không chỉ khi đoạn Python không ném lỗi.

### 9.3 Cách chuyển một dòng thành bài test

1. Chọn ID ca và một biến thể cụ thể, ví dụ `CW-09.a` cho thiếu đúng 1đ.
2. Chuẩn bị đúng fixture, chụp số dư/kho/trạng thái ban đầu; không dựa kết quả ca trước.
3. Mỗi vai trò dùng BrowserContext riêng; không dùng chung cookie giữa C1/M1/NV1/A.
4. Thực hiện từng thao tác của dòng. Dùng locator theo vai trò/nhãn hoặc ID ổn định, tránh tọa độ và chờ cứng.
5. Kiểm kết quả UI bằng assertion; ca bị từ chối phải kiểm cả trạng thái/số dư/kho không đổi.
6. Ca tiền/kho dùng thêm kết quả API/truy vấn chỉ đọc được phép trên DB test để đếm và đối soát.
7. Ca đồng thời có đồng bộ/barrier hoặc test backend kiểm soát thời điểm; không coi hai cú click cách nhau là đã chứng minh race.
8. Ghi PASS/FAIL/BLOCKED cùng bằng chứng; dọn dữ liệu theo kế hoạch của DB test, không xóa dữ liệu nhóm bằng thao tác tự động không được duyệt.

Ví dụ ánh xạ tên để dễ truy vết: `PUB-01` → `test_pub_01_homepage`,
`CW-09` → `test_cw_09_insufficient_balance_rejected`,
`NV-12` → `test_nv_12_other_branch_order_denied`.
Các tên này chỉ minh họa quy ước, không phải file test đã được tạo hoặc đã chạy.

### 9.4 Thứ tự chạy đề xuất

1. **Kiểm tra môi trường:** AIV-01/03/08, PUB-01 và đăng nhập theo vai trò. Sai DB hoặc không có fixture thì dừng trước mutation.
2. **Khói chỉ đọc:** trang chủ, tìm kiếm, chi tiết, trang login, trang rỗng, menu từng role, sidebar/mobile; không cần tạo đơn.
3. **Luồng mua và quyền:** E2E-01…12 trên DB test; đối soát sau từng luồng.
4. **Toàn bộ ca P0 trong phạm vi:** đặc biệt SEC/API, CON, CW, WAL, ORD và checkout; không bỏ qua vì thao tác UI trông bình thường.
5. **Chức năng và giao diện:** ca P1/P2, dữ liệu biên, upload, 100 trang, mạng chậm và Unicode.
6. **Vận hành/di sản:** LEG/MIG/OPS chỉ khi có điều kiện; ghi N/A hoặc BLOCKED có lý do, không tính vào PASS.
7. **Hồi quy sau sửa lỗi:** chạy lại ca lỗi, ca có cùng service/dữ liệu và các luồng E2E bị ảnh hưởng.

### 9.5 Hồ sơ kết quả cần bàn giao

| Trường | Nội dung cần ghi |
|---|---|
| Mã ca / biến thể | Ví dụ CW-09.a; không đổi ID chỉ để che một lần chạy thất bại |
| Phiên bản / môi trường | Commit, profile, base URL, DB test, browser, màn hình và múi giờ |
| Tiền điều kiện | Tài khoản theo ký hiệu, ID đơn/sản phẩm/cửa hàng, số dư/kho trước ca |
| Các bước thực tế | Thứ tự đã làm và request quan trọng khi cần, không chỉ sao chép tên ca |
| Mong đợi / thực tế | Giá trị cụ thể về trạng thái, tiền, kho, lỗi hoặc quyền |
| Kết luận | NOT RUN / PASS / FAIL / BLOCKED / N/A; kèm lý do nếu chưa chạy hoặc không áp dụng |
| Bằng chứng | Ảnh, video/trace hoặc log đã che dữ liệu; ID giao dịch và truy vấn đối soát nếu cần |
| Mã lỗi / kiểm lại | Issue liên quan, người xử lý, commit sửa và kết quả retest |

Không đưa mật khẩu, OTP, secret, cookie, Authorization header, refresh token,
token QR đầy đủ, file `.env.aiven` hoặc dump dữ liệu thật vào báo cáo công khai.
Ảnh/trace sau đăng nhập có thể chứa thông tin riêng; lưu ở nơi hạn chế quyền,
không tự đính kèm GitHub Actions/artifact công khai.

**Trạng thái của lần cập nhật tài liệu này:** mới lập và đối chiếu kế hoạch;
không chạy các ca trên database hoặc trình duyệt, không xác nhận 500 ca đã PASS,
không tạo tài khoản/đơn/giao dịch test và không thay đổi mã ứng dụng.
