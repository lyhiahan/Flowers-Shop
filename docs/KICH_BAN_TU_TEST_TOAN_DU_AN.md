# Bộ kịch bản tự kiểm thử toàn dự án

Ngày đối chiếu mã nguồn: **21/10/2026**. Dự án: chuỗi cửa hàng hoa, Java 21 / Spring Boot / Thymeleaf / SQL Server.

Tài liệu này là kế hoạch test thủ công dựa trên cấu trúc, controller, service, template, JavaScript và các nhóm test hiện có. **Chưa thực hiện các ca trên trình duyệt hoặc database thật trong lần lập tài liệu này.** Kết quả mong đợi là tiêu chí để bạn phát hiện lỗi, không phải khẳng định hệ thống đã chạy đạt. Không thể liệt kê hữu hạn mọi tổ hợp đầu vào; bộ này bao phủ các luồng chức năng hiện có, nhánh lỗi, biên dữ liệu, phân quyền và các rủi ro hồi quy chính.

**Quy mô:** 10 kịch bản xuyên suốt + 371 ca chi tiết, trong đó 173 ca P0. Các dòng có nhiều biến thể cần chạy từng biến thể riêng.

Đi theo thứ tự: [chuẩn bị dữ liệu](#2-chuẩn-bị-trước-khi-test) → [10 luồng xuyên suốt](#3-mười-kịch-bản-xuyên-suốt-nên-chạy-đầu-tiên) → [ca chi tiết](#4-danh-sách-ca-chi-tiết) → [ca tùy môi trường](#5-các-ca-tùy-điều-kiện-luồng-cũ-và-vận-hành) → [nghiệm thu](#7-lịch-chạy-đề-xuất-và-điều-kiện-nghiệm-thu).

## 1. Đọc nhanh cấu trúc và phạm vi

| Thư mục / thành phần | Vai trò | Cần kiểm thử gì |
|---|---|---|
| `pom.xml`, `.mvn`, `mvnw*`, `scripts/maven.ps1` | Build Java 21, Maven Wrapper | Khởi động, build/test và profile |
| `src/main/java/com/finalproject/controller/auth` | Đăng ký, OTP, đăng nhập session/JWT | AUTH, API |
| `controller/customer` | Trang công khai, hồ sơ, giỏ, checkout, thanh toán, đơn | PUB, ACC, CART, CHK, PAY, ORD, REV |
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
| `application*.properties` | Profile và cấu hình dịch vụ | Demo bật/tắt, HTTPS, mail, Cloudinary, SQL Server |
| `resources/db/migration/sqlserver`, `java/.../db/migration` | Flyway SQL và Java V1–V10 | Database mới/nâng cấp, giữ dữ liệu cũ |
| `database.sql`, `sql/migrations`, `resources/db/*.sql` | SQL khởi tạo/sửa/di sản | Không chạy chồng tùy tiện với Flyway |
| `src/test/java`, `src/test/js`, `src/test/resources` | Test hồi quy backend/frontend, profile H2 và test SQL Server riêng | Dùng làm căn cứ các tình huống; H2 không thay thế test SQL Server |
| `scripts` | Chạy local, cấu hình mail, tunnel điện thoại, kiểm tra SQL, dữ liệu demo | Chỉ dùng đúng script tương ứng môi trường test |
| `docs`, `README.md`, `HUONG_DAN_KIEM_THU.md` | Tài liệu | Tài liệu cũ có phần đã lỗi thời |
| `target`, `.tools`, `.git`, `.settings`, `.agents` | Đầu ra build, công cụ local, quản lý dự án | Không coi `target` là source; không đưa credential vào bằng chứng test |

### Những quy tắc phải dùng khi đánh giá

1. Role hiện tại có **ADMIN, MANAGER, STAFF, USER**. STAFF là nhân viên riêng, làm việc tại `/staff/orders`; xem quy định và kịch bản mới nhất trong `PHAN_QUYEN_CUA_HANG.md`. Không đăng ký/chuyển sang SHIPPER; `/shipper/**` đã bị chặn.
2. Tài khoản có vai trò cố định. Admin tạo tài khoản riêng theo vai trò, không dùng luồng yêu cầu nâng quyền cũ. ADMIN không mua hàng; trang cart/checkout/customer yêu cầu USER.
3. Giao diện checkout hiện đưa ra **COD và MOMO_DEMO khi demo được bật**, chỉ COD khi tắt. `BANK_TRANSFER` bị từ chối khi tạo đơn qua checkout. Backend còn luồng BANK_TRANSFER/MOMO để hỗ trợ dữ liệu/đường đi cũ; xem mục test tùy điều kiện.
4. Đặt thành công trừ kho ngay, kể cả COD. Đặt lỗi rollback. Hủy trước giao hoàn kho đúng một lần về chi nhánh trên đơn.
5. Mẫu riêng chỉ bán ở chi nhánh sở hữu. Mẫu chung có thể bán tại chi nhánh có kho hợp lệ, đang hoạt động, đang mở và cho phép bán mẫu đó. Checkout hiện tìm **một chi nhánh đáp ứng toàn bộ dòng đã chọn**, không mặc định tự tách đơn theo nhiều chi nhánh.
6. Khách hủy tại PENDING/CONFIRMED, không có giới hạn một giờ; từ PREPARING bị chặn. Quản lý/admin có hủy ngoại lệ khi PREPARING, bắt buộc lý do 1–300 ký tự sau bỏ khoảng trắng.
7. Hủy đơn đã trả tiền thường chuyển REFUND_REQUESTED để xác nhận hoàn tiền sau. **Ngoại lệ MOMO_DEMO: khách tự hủy trước chuẩn bị được hoàn tiền demo tự động**, thành REFUNDED. Quản lý/admin hủy đơn demo vẫn đi luồng chờ hoàn tiền.
8. MoMo demo trả tiền vào **số dư chờ**, chưa vào ví khả dụng. Giao thành công/hoàn tất nhận tại quầy mới kết chuyển thu nhập sau hoa hồng. Không có giao dịch MoMo thật.
9. Xóa sản phẩm tại chi nhánh là xóa mềm, giữ ảnh/kho/lịch sử. Khôi phục về Ngừng bán. Không xóa khi còn đơn đang xử lý.
10. Giá là đồng nguyên, tối thiểu 1đ; khuyến mãi là số tiền, không phải phần trăm. Khuyến mãi không vượt giá bán. Mỗi mẫu có một danh mục chính và tối đa 10 danh mục bổ sung.
11. Báo cáo mặc định 7 ngày, gồm hôm nay, theo UTC+7. KPI tồn kho độc lập kỳ báo cáo; tổng kho quản trị có thể gồm hàng ngừng bán/chi nhánh tạm đóng, khác số lượng công khai được phép bán.

## 2. Chuẩn bị trước khi test

### 2.1 Môi trường và tài khoản

- Dùng database test riêng hoặc bản sao database. Ghi lại tên database, commit, profile, giờ bắt đầu. Các ca khóa tài khoản/xóa/reset/migration chỉ chạy trên dữ liệu test.
- Base URL mặc định: `http://localhost:8080`. Trong bảng, đường dẫn bắt đầu `/` được nối vào base URL này. ID dùng ID thật lấy từ giao diện, không giả định luôn bằng 1.
- Khởi động theo README bằng `./scripts/sqlserver.ps1` nếu máy đã có cấu hình local phù hợp. Spring không tự nạp toàn bộ `.env`. Không cần chạy migration/reset để chỉ lập kịch bản test.
- Mở các **profile trình duyệt độc lập** cho C1, C2, M1, M2, A; các tab ẩn danh chung một cửa sổ có thể dùng chung cookie nên không đủ để tách tài khoản.
- Mở DevTools: Network bật Preserve log; Console theo dõi lỗi. Ca gửi request trực tiếp dùng Network → Copy as fetch hoặc Postman. Giữ đúng CSRF cho request session, trừ ca cố ý thiếu/sai token.
- OTP cần hộp thư thật bạn kiểm soát và mail cấu hình hoạt động. Ảnh chat cần Cloudinary. Test điện thoại cần LAN/tunnel do bạn bật; không dùng URL tunnel cũ làm URL mặc định.

| Ký hiệu | Tài khoản / dữ liệu cần có |
|---|---|
| A | ADMIN đang hoạt động |
| M1 | MANAGER, chủ chi nhánh S1 |
| M2 | MANAGER, chủ S2, không được phân quyền S1 |
| MP | MANAGER, đối tác S1 nhưng không là chủ |
| MN | MANAGER chưa có chi nhánh |
| C1, C2 | Hai USER đang hoạt động, hồ sơ/giỏ/địa chỉ riêng |
| CX | USER dành riêng để khóa/xóa thử |
| S1, S2 | Hai chi nhánh hoạt động và mở cửa; có đơn vị/biểu phí giao hàng phù hợp |

README mô tả tài khoản seed trên database rỗng: `nguyenthanhminh` (admin), `nguyenquanghuy` (quản lý Q1), `tranduclong` (quản lý Q7), `leminhhoa` (khách), mật khẩu demo `123456`. **Chỉ dùng nếu dữ liệu seed thực tế đã có và chưa đổi mật khẩu**; database cũ không chắc được seed bổ sung. Tạo C2/MP/MN/CX bằng chức năng hiện có, ghi lại username thực tế. Không lấy tài khoản `shipper_nhanh` trong hướng dẫn cũ làm điều kiện test.

### 2.2 Bộ dữ liệu có thể tính tay

Tạo mẫu có tiền tố `TEST_20261021_` để tìm lại; chạy từng ca với dữ liệu được phục hồi về tiền điều kiện, không nối tùy tiện các ca làm thay đổi kho.

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

Nếu UI không cho đặt đúng một thông số fixture, dùng dữ liệu tương đương đang có và tính lại kỳ vọng. Ca cần dữ liệu di sản/lỗi DB ghi **BLOCKED – thiếu fixture**, không tự sửa database chính để ép chạy.

### 2.3 Cách ghi kết quả

P0 = lỗi làm sai tiền/kho/quyền hoặc chặn luồng mua chính; P1 = chức năng chính/validation; P2 = trình bày và trường hợp ít gặp. Mỗi biến thể trong một ô phải chạy riêng và ghi hậu tố, ví dụ `AUTH-03.a`, `.b`.

Trạng thái: `NOT RUN`, `PASS`, `FAIL`, `BLOCKED`, `N/A`. N/A phải ghi lý do (ví dụ không bật thanh toán cũ). BLOCKED không được tính PASS. Khi lỗi xảy ra, ghi bước tái hiện, dữ liệu, ảnh, request/response, ID đơn, kho và ví trước/sau; che token, cookie, OTP và thông tin riêng tư.

| ID ca / biến thể | Người test | Ngày giờ UTC+7 | Dữ liệu / ID đơn | Thực tế | Trạng thái | Bằng chứng / mã lỗi |
|---|---|---|---|---|---|---|
| … | … | … | … | … | NOT RUN | … |

## 3. Mười kịch bản xuyên suốt nên chạy đầu tiên

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

Tiền điều kiện: profile dev, demo payment bật; P1 kho 10; hoa hồng S1 = 10%; ghi W khả dụng và H chờ.

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
2. Khi PENDING hoặc CONFIRMED, C1 hủy.
3. Đơn **REFUNDED**, payment REFUNDED, thông báo hủy và hoàn thành công; kho về 10; H về ban đầu; W không bị trừ vì chưa nhận thu nhập.
4. Lặp hủy/QR cũ không tạo thêm hoàn tiền hoặc phục hồi đơn. Không dùng kỳ vọng “chờ hoàn tiền” của thanh toán thường cho ca này.

### E2E-06 — Hủy ngoại lệ bởi quản lý/admin (P0)

1. C1 tạo và trả MoMo demo một P1. M1 chuyển tới PREPARING.
2. C1 thử hủy từ trang cũ và request trực tiếp: bị từ chối; kho/H/trạng thái giữ nguyên.
3. M1 chọn hủy ngoại lệ: thử lý do trống/khoảng trắng/>300 ký tự, đều bị từ chối. Nhập `Khách đề nghị đổi ngày, cửa hàng không thể đáp ứng` → xác nhận.
4. Đơn REFUND_REQUESTED, kho trả một lần, H vẫn giữ đến khi hoàn tiền, W chưa tăng; C1 thấy đúng lý do và người hủy.
5. M1 xác nhận đã hoàn tiền → REFUNDED, H giảm đúng 100.000đ. Lý do hủy ban đầu vẫn đọc được. Lặp xác nhận không hoàn/trừ tiền hai lần.
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
4. Tạo yêu cầu mới; A xác nhận đã chuyển: COMPLETED, W không giảm lần hai. MP không được tự rút ví S1.

### E2E-10 — Giành đơn vị hàng cuối cùng (P0)

1. Đặt P1 kho = 1, hai tài khoản C1/C2 mỗi người có một sản phẩm trong giỏ. Mở checkout trước ở cả hai.
2. Cùng bấm đặt COD trong khoảng sát nhau; lặp vài lần với fixture mới để tăng khả năng chồng request.
3. Chỉ một đơn đặt thành công, kho = 0; người còn lại nhận lỗi hết/không đủ hàng; không có kho âm hoặc đơn mồ côi.
4. Người thắng hủy trước chuẩn bị: kho về 1 đúng một lần. Người còn lại thử lại và mua được.

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
| CHK-23 | P0 | Gửi paymentMethod lạ/BANK_TRANSFER; tắt demo rồi gửi MOMO_DEMO | Phương thức không được hỗ trợ/bị gỡ/tắt bị chặn ở luồng tương ứng |
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

Tiền điều kiện: `/checkout` cho COD/MOMO_DEMO như cấu hình. MoMo demo có hạn 10 phút và token phiên lưu trong bộ nhớ tiến trình. Kiểm ID đơn, bản ghi payment, số dư chờ/khả dụng, không chỉ nhìn màn hình thành công.

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
| ORD-08 | P0 | Khách hủy PENDING; CONFIRMED dưới/trên một giờ | Được hủy trước chuẩn bị, hoàn kho một lần |
| ORD-09 | P0 | Khách hủy PREPARING/DELIVERING/DELIVERED/COMPLETED | Bị chặn cả UI và backend; kho/tiền giữ nguyên |
| ORD-10 | P0 | Khách giữ tab CONFIRMED, M1 chuyển PREPARING, khách hủy từ tab cũ | Backend đọc trạng thái mới và chặn |
| ORD-11 | P0 | M1/A hủy tại PENDING/CONFIRMED/PREPARING, lý do hợp lệ | Đúng vai trò người hủy, đúng lý do, trả kho đúng một lần |
| ORD-12 | P0 | Hủy bằng lý do rỗng/trắng/300/301 ký tự | 300 hợp lệ; rỗng/trắng/301 bị chặn trước thay đổi kho/tiền |
| ORD-13 | P1 | Lý do có HTML/nháy/dấu và xuống dòng | Lưu/hiện như văn bản, không chạy script; khách đọc được lý do |
| ORD-14 | P0 | Hủy COD chưa trả; hủy prepaid thường; khách hủy MoMo demo | COD→CANCELLED; prepaid thường→REFUND_REQUESTED; demo khách hủy→REFUNDED |
| ORD-15 | P0 | M1 hủy MoMo demo đã trả tiền | REFUND_REQUESTED, tiền H chưa tự mất; xác nhận hoàn tiền mới giải phóng H |
| ORD-16 | P0 | Gửi hủy lặp; hai người cùng hủy | Không cộng kho nhiều lần, không gửi nhiều thông báo/hoàn tiền trùng |
| ORD-17 | P0 | C1 yêu cầu hoàn đơn đã giao/hoàn tất và PAID | REFUND_REQUESTED; chưa được coi là tiền đã hoàn, chưa tự trừ ví |
| ORD-18 | P0 | Yêu cầu hoàn khi chưa giao/chưa trả hoặc đơn C2 | Bị chặn; không tạo yêu cầu hoàn tiền sai |
| ORD-19 | P0 | M1/A xác nhận hoàn cho REFUND_REQUESTED | Đơn/payment REFUNDED; hoàn đúng khoản tiền chờ hoặc đảo đúng thu nhập đã ghi |
| ORD-20 | P0 | Lặp xác nhận hoàn; cố hoàn đơn chưa trả tiền | Không trừ tiền lần hai/không hoàn giả |
| ORD-21 | P0 | Hoàn đơn đã kết chuyển thu nhập, còn các đơn khác trong ví | Chỉ đảo thu nhập/điểm/hoa hồng của đơn đó; không đụng đơn khác |
| ORD-22 | P1 | Xem đơn bị hủy trước/sau xác nhận hoàn tiền | Giữ lý do/người hủy ban đầu, bổ sung trạng thái hoàn đúng |
| ORD-23 | P1 | Lọc đơn theo trạng thái/tìm kiếm/chi nhánh và chuyển trang | Danh sách, tổng số và bộ lọc nhất quán; không trộn cửa hàng |
| ORD-24 | P0 | Đổi giá/ảnh/tên mẫu sau khi đơn đã đặt rồi xem lại đơn | Đơn giá/tổng tiền đã chốt trên đơn không bị tính lại theo giá mới |
| ORD-25 | P1 | Vào `/staff/orders` bằng M1/A và xử lý nút có trên UI | Cùng luật chuyển trạng thái, kiểm chi nhánh và thanh toán như manager; không mở đường bỏ qua chính sách |

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
| STOCK-08 | P0 | M1 xem dashboard với active S1 rồi chuyển S2 nếu có quyền | KPI đúng chi nhánh đang chọn, không tổng toàn chuỗi ngoài ý muốn |
| STOCK-09 | P0 | Có dòng kho di sản P1 ở S2 dù P1 riêng S1 | Không cộng kho S2 vào số bán/KPI/danh sách hợp lệ; không dùng làm lý do cho M2 sửa P1 |
| STOCK-10 | P1 | Mở `tab=legacy` ở sản phẩm quản lý | Không xuất hiện mục vận hành kho cũ trái quy tắc mới |
| STOCK-11 | P0 | Gọi `/api/public/catalog-stock` với ID mẫu cấm/xóa/chờ duyệt | Không trả lượng có thể bán của mẫu/chi nhánh bị chặn |
| STOCK-12 | P1 | Ngắt mạng endpoint kho rồi nối lại | Không làm hỏng giỏ/trang, không biến số cũ thành khẳng định còn hàng vô hạn; cập nhật phục hồi |

### 4.10 Cửa hàng và nhân sự — STORE / STAFF

Tiền điều kiện: M1 chủ S1, MP hợp tác S1, M2 ngoài S1, MN chưa có shop. Trang `/manager/stores`, `/manager/profile`, `/manager/staff`.

| ID | Mức | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| STORE-01 | P1 | MN vào cửa hàng/dashboard/đơn/sản phẩm khi chưa phân công | Có trang trống/đường dẫn tạo shop phù hợp, không 500 hoặc thấy shop khác |
| STORE-02 | P0 | Tạo cửa hàng hợp lệ, chọn hoa hồng | Gắn đúng chủ, không tự có doanh thu/kho/đơn không có thật |
| STORE-03 | P1 | Tên 2/3/120/121; địa chỉ 4/5/200/201; bio 500/501; SĐT sai | Đúng biên create, lỗi không lưu shop một phần |
| STORE-04 | P1 | Bỏ chính sách hoa hồng; chọn ID đã xóa/không tồn tại | Bị từ chối |
| STORE-05 | P0 | Gửi lại form tạo với cùng requestId | Không tạo hai cửa hàng từ cùng yêu cầu |
| STORE-06 | P1 | Xem shop sở hữu/hợp tác, chuyển active shop rồi vào các module | Nhãn quan hệ và shop đang chọn đúng; đơn/kho/voucher/ví/chat theo shop đã chọn |
| STORE-07 | P0 | M2 mở sửa/chuyển S1 bằng URL/request trực tiếp | Bị chặn, active shop không bị đổi trái quyền |
| STORE-08 | P1 | Sửa tên/SĐT/địa chỉ/bio của shop có quyền rồi mở gian hàng công khai | Dữ liệu mới đồng bộ, không đổi chủ/hoa hồng ngoài quyền |
| STORE-09 | P1 | Upload avatar/cover, thêm/sửa/xóa ảnh đặc trưng | Đúng loại ảnh; reload còn dữ liệu; xóa một ảnh không xóa nhầm ảnh khác |
| STORE-10 | P0 | Tắt mở cửa khi C1 đang ở checkout; mở lại | Đơn mới bị chặn khi đóng; mở lại chỉ nhận mẫu/kho hợp lệ |
| STORE-11 | P0 | A cấm S1 rồi dùng link cũ/quote/checkout | Không nhận đơn mới cho shop bị cấm; lịch sử cũ được giữ |
| STAFF-01 | P1 | M1 thêm MANAGER hợp lệ chưa gán shop bằng chọn account | Thêm đúng S1, user vẫn role MANAGER; không nâng USER thành manager |
| STAFF-02 | P1 | Thêm đối tác MANAGER có account sẵn; thử đối tác chưa tồn tại | Account hợp lệ được hợp tác; đối tác không có account bị từ chối |
| STAFF-03 | P1 | Tạo nhân sự mới username/email hợp lệ, password 8–72 ký tự | Tạo account MANAGER riêng và membership đúng shop |
| STAFF-04 | P1 | Nhân sự mới thiếu email/pass; pass 7/73; username trùng hoặc email trùng | Không tạo account lỗi/nhân sự trùng; báo rõ trường sai |
| STAFF-05 | P0 | Thêm A, C1 USER, account khóa, bản thân, người đã thuộc shop hoặc vai trò SHIPPER | Bị từ chối theo loại ứng viên, không đổi role cũ |
| STAFF-06 | P0 | MP/M2 cố thêm/xóa nhân sự S1 | Chỉ chủ có quyền; cả request trực tiếp bị chặn |
| STAFF-07 | P0 | M1 xóa nhân sự thường; thử xóa chính chủ hoặc người S2 | Chỉ xóa quan hệ đúng shop, không xóa chủ/nhân sự shop khác |
| STAFF-08 | P0 | Nhân sự bị gỡ vẫn giữ session và socket cũ → vào đơn/chat S1 | Quyền bị thu hồi, không tiếp tục thao tác nhờ phiên cũ |
| STAFF-09 | P1 | MP tự rời S1; M1 chủ thử tự rời | Đối tác rời được, mất quyền/chọn shop cũ; chủ bị từ chối |
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

Tiền điều kiện: M1 chủ S1, MP đối tác, A duyệt giao dịch; `/manager/wallet`, `/admin/transactions`. Ghi riêng W khả dụng và H chờ.

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
| WAL-09 | P0 | MP/M2/A cố POST rút S1 khi không là chủ | Chỉ chủ được rút theo luật hiện tại; không bỏ qua bằng quyền xem |
| WAL-10 | P0 | Gửi cùng requestId hai lần; cùng ID nhưng đổi số tiền/STK | Lặp cùng nội dung không trừ hai lần; đổi nội dung bị chặn |
| WAL-11 | P0 | A từ chối pending; A duyệt pending | Từ chối trả W một lần; duyệt không trừ W lần hai |
| WAL-12 | P0 | A xử lý lại yêu cầu đã COMPLETED/REJECTED; xử lý ID không phải WITHDRAWAL | Bị chặn, không thay tiền |
| WAL-13 | P1 | Chuyển active shop và xem lịch sử/thu nhập/yêu cầu rút | Đúng shop, không lẫn giao dịch của shop khác |
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
| CHAT-19 | P0 | M1 đổi active S1→S2 khi có quyền cả hai, mở chat | Nội dung/queue gửi nhận đúng chi nhánh đang chọn, không rò tin shop cũ |
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
| SEC-04 | P0 | Mọi role mở `/shipper/**`, gửi role SHIPPER/STAFF/VENDOR cũ | Không khôi phục cổng/role đã bỏ qua request |
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
| OPS-03 | P0 | Nâng bản sao DB cũ qua migration tới V10 | Migration thành công, lịch sử/ảnh/giá/kho/đơn giữ theo phạm vi migration; kiểm Flyway history |
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

## 6. Các điểm cần chú ý khi kết luận

- Một số validation ở module danh mục/cấp bậc là **tiêu chí phát hiện thiếu kiểm tra**, không phải cam kết code hiện đã chặn toàn bộ. Nếu lưu được giá trị âm/rỗng/vô nghĩa, ghi FAIL kèm ảnh/request.
- Hạn thanh toán và hủy tự động khác nhau: scheduler hiện chỉ xét UNPAID ở PENDING/CONFIRMED, tối đa 100 đơn mỗi lượt. Không kết luận mọi đơn PREPARING hết hạn cũng tự hủy. Với nhiều đơn quá hạn cần chờ thêm các lượt.
- Nhận tại quầy không đi qua trạng thái vận chuyển. Với DELIVERY, ghi nhận COD/thu nhập tại DELIVERED; COMPLETED sau đó không được cộng thêm lần nữa.
- Tổng doanh thu trên dashboard, thu nhập sau hoa hồng trong ví và tiền chờ là các đại lượng khác nhau. Luôn dùng đúng đại lượng để so sánh.
- Chức năng giá khuyến mãi, form thêm sản phẩm của manager và form ProductForm của admin không hoàn toàn cùng trường; chỉ test trường thực có trên màn hình, dùng request/fixture riêng để kiểm quy tắc backend nếu cần.
- Không lấy việc test tự động có tên tương ứng làm bằng chứng ca thủ công PASS. Không lấy test H2 PASS để kết luận migration/concurrency SQL Server PASS.

## 7. Lịch chạy đề xuất và điều kiện nghiệm thu

1. **Lượt nhanh trước demo:** chạy E2E-01…10, PUB-01, AUTH-01/17/22, các ca quyền SEC-01…06, PAY-10/11, PROD-17…20. Thường cần vài giờ tùy việc chuẩn bị dữ liệu và chờ OTP/hạn thanh toán.
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
| M1-05 | Thêm một manager chưa được phân công vào cửa hàng | Thêm thành công; tài khoản đó được phân công đúng cửa hàng |
| M1-06 | Thêm cùng manager vào cửa hàng thứ hai, thử cả hai lựa chọn vai trò | Bị từ chối; phân công cũ giữ nguyên |
| M1-07 | Gỡ manager không phải chủ khỏi cửa hàng cũ rồi thêm vào cửa hàng mới | Chuyển được sang cửa hàng mới; mất quyền cửa hàng cũ |
| M1-08 | Đăng nhập manager chưa có cửa hàng, tạo cửa hàng đầu tiên | Tạo thành công và tự phân công vào cửa hàng vừa tạo |
| M1-09 | Mở chat/thông báo của cửa hàng khác, kể cả tài khoản còn liên kết cũ | Không được đọc, gửi hoặc nhận tin của cửa hàng khác |
| M1-10 | Đăng nhập admin | Quản lý được toàn bộ cửa hàng, tạo nhiều cửa hàng và chuyển cửa hàng để xem tổng quan |

Dữ liệu cũ: ưu tiên cửa hàng đã phân công trong tài khoản (`users.store_id`). Nếu chưa có phân công, chỉ chấp nhận khi tài khoản có đúng một cửa hàng liên kết. Nếu có nhiều liên kết mà chưa rõ cửa hàng phụ trách, hệ thống chặn truy cập và cần kiểm tra lại phân công; không tự chọn ngẫu nhiên hoặc xóa dữ liệu lịch sử.


## Bổ sung ngày 24/10/2026: tách quyền Staff

Quy định và 10 bước kiểm tra thủ công mới nhất: [Phân quyền cửa hàng](PHAN_QUYEN_CUA_HANG.md). Phần này thay thế mô tả cũ về PARTNER/nhân viên dùng quyền MANAGER ở các kịch bản STAFF và WAL trước đây. Nhân viên dùng STAFF, không truy cập các trang quản lý, không tự rút tiền hay rời chi nhánh qua trang quản lý. Quản lý thực hiện việc thêm/gỡ và chuyển chi nhánh.


## Bổ sung khu vực làm việc Nhân viên

Xem [Kịch bản test Nhân viên](KICH_BAN_TEST_NHAN_VIEN.md) để kiểm tra đầy đủ các trang đơn, chi tiết giao hàng, chuẩn bị hoa, giao/nhận hoa, thu COD và chat theo chi nhánh. Các bước này thay cho luồng Staff chỉ có một bảng đơn trước đây.
