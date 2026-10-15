# Rà soát hệ thống — 15/10/2026

## Kết luận và phạm vi

Dự án có đủ thành phần của website bán hàng chuỗi cửa hàng: Spring Boot, Thymeleaf,
Bootstrap, JPA/SQL Server, WebSocket, JWT và Cloudinary. Các luồng nghiệp vụ chính có
kiểm thử tự động, không chỉ có giao diện. Tuy nhiên chưa nên gọi là “hoàn thiện 100%”
hoặc sẵn sàng vận hành thương mại chỉ từ kết quả này.

Rà soát mã nguồn hiện có trong workspace, chạy kiểm thử Java/JavaScript, HTTP thật
trên server kiểm thử, kiểm tra SQL Server riêng và đọc thống kê database gốc.
Không thay đổi dữ liệu RentalShop, không đặt/hủy đơn thật, không gửi OTP/email hay
tải ảnh lên dịch vụ bên ngoài trong đợt này. Không push GitHub trong lượt rà soát.

## Cấu trúc

238 file Java và 74 template HTML tại thời điểm kiểm tra.

```text
src/main/java/com/finalproject/
  config, security       Cấu hình, đăng nhập, JWT, phân quyền, WebSocket
  controller/            Public/customer, manager/vendor, staff, admin, API
  service/               Đặt đơn, tồn kho, thanh toán, chat, OTP, báo cáo
  repository/, entity/   JPA, truy vấn, mô hình dữ liệu
  dto/, enums/, utils/   Dữ liệu đầu vào, trạng thái, quy tắc dùng chung
  db/migration/          Migration Java V3–V4
  scheduler/             Tác vụ định kỳ
src/main/resources/
  templates/             Thymeleaf theo nhóm người dùng
  static/css, static/js  Giao diện và hành vi phía trình duyệt
  db/migration/sqlserver/ Migration SQL V1–V2, V5–V8
src/test/java, src/test/js Kiểm thử nghiệp vụ, HTTP, bảo mật, giao diện JS
scripts/                 Khởi động, cấu hình, kiểm thử
docs/                    Tài liệu vận hành và nghiệm thu
.tools/, target/, .env   Công cụ/build/cấu hình cục bộ; không đưa vào Git
```

Phân lớp khá rõ. Điểm cần cải thiện dần: một số controller còn xử lý nhiều nghiệp vụ,
template chứa nhiều CSS/JavaScript inline, một số query lọc danh sách thực hiện trong
bộ nhớ, và có nhiều tài liệu theo ngày cần một mục lục nghiệm thu thống nhất.

## Các lỗi sửa trong đợt này

| Vấn đề | Cách xử lý | Kiểm chứng |
| --- | --- | --- |
| Tìm người dùng theo từ khóa bỏ qua vai trò đã chọn | Kết hợp điều kiện từ khóa AND vai trò | Từ khóa của khách khi lọc ADMIN phải trả 0 |
| Giao diện quảng bá tìm theo điện thoại nhưng backend chưa hỗ trợ | Thêm điện thoại vào tìm kiếm; escape ký tự LIKE | Tìm số điện thoại trả đúng tài khoản |
| Trang người dùng vượt số trang hiện có hiện danh sách rỗng | Đưa về trang cuối hợp lệ | `page=999&size=2` với 5 tài khoản trả trang cuối có 1 dòng |
| Backend khóa tài khoản chỉ bảo vệ username `admin`, không bảo vệ admin đã đổi tên như giao diện | Chặn vai trò ADMIN ở controller và service trước khi đổi trạng thái | Bổ sung test POST trực tiếp và gọi service cho admin có tên khác |
| Mở trang giao hàng tự sinh biểu phí cho chi nhánh chưa có cấu hình | Bỏ ghi dữ liệu trên GET và hàm tự sinh đi kèm | GET không làm tăng số bản ghi biểu phí |
| Báo phí có thể lấy giá của đơn vị khác hoặc sai vùng | Chỉ nhận biểu phí hoạt động khớp đơn vị và vùng; nếu đã có hệ thống biểu phí nhưng thiếu giá phù hợp thì chặn báo giá | Test khác đơn vị, sai vùng, biểu phí bị tắt đều bị chặn |
| Biểu phí chung toàn chuỗi chưa được tính đúng | Cho phép biểu phí chung; ưu tiên biểu phí riêng của chi nhánh khi cùng đơn vị/vùng | Test giá chung 40.000, riêng 25.000 |
| Test dữ liệu demo còn dùng username `admin` cũ | Kiểm tra username hiện tại và vai trò ADMIN | Kiểm thử khởi tạo và bảo toàn dữ liệu chỉnh tay |
| Các Spring test context dùng chung một tên H2, dễ ảnh hưởng vòng đời schema của nhau | Cấp tên H2 riêng theo context bằng `${random.uuid}` | Chạy lại toàn bộ suite, không sửa cấu hình database production |

Mở rộng HTTP smoke test sang form thêm/sửa và 4 tab sản phẩm quản lý, hồ sơ/ví chi
nhánh, danh mục/thuộc tính, cấp bậc, giao hàng/biểu phí, chat admin. Test kiểm tra
HTTP 200 và HTML đóng đầy đủ, nhằm phát hiện trang bị dựng dang dở như lỗi người dùng.

## Kết quả kiểm thử

- Lượt Java đầu tiên: 546 ca, 540 đạt, 1 thất bại do tên tài khoản demo cũ, 5 bỏ qua.
- Lượt toàn bộ sau sửa, kết thúc 13:52 ngày 15/10: **552 ca, 547 đạt, 0 thất bại, 0 lỗi, 5 bỏ qua; BUILD SUCCESS**.
- 5 ca bỏ qua có điều kiện: upload Cloudinary thật (1), truy vấn catalog SQL Server (1),
  kiểm tra bản sao catalog đã migration (1), chạy migration SQL Server từ đầu/nâng cấp (2).
  Kiểm chứng catalog/schema SQL Server được chạy riêng trên bản sao, không dùng H2 thay thế.
- SQL Server thật trên bản sao `CodexCatalogCopy_20261014_2130`: **2/2 đạt**, kết thúc
  13:59, `BUILD SUCCESS`. Kiểm tra truy vấn/phân trang catalog 4 tab của từng chi nhánh,
  validate toàn bộ entity với schema, không có sản phẩm gắn danh mục đã ẩn, migration V8.
  Không chạy lại migration từ đầu và không sửa bản sao trong lượt kiểm chứng này.
- Sau lượt toàn bộ, bổ sung chặn khóa ADMIN đã đổi tên và chạy lại các nhóm người dùng,
  nghiệp vụ và xử lý đồng thời: **135/135 đạt**, bao gồm 2 test mới; kết thúc 13:56,
  `BUILD SUCCESS`. Đây là lượt hồi quy riêng, không phải chạy lại toàn bộ suite lần nữa.
- Một lượt chạy lại bị gián đoạn với cảnh báo clock leap khoảng 86 phút và lỗi H2 đã đóng;
  lượt đó không được coi là đạt. Lưu log riêng để không che mất kết quả thất bại.
- JavaScript: 65/65 đạt.
- Bộ nạp môi trường Cloudinary: 10/10 kiểm tra đạt, chỉ sử dụng khóa giả.
- Log cục bộ: `.tools/full-audit-java.log`, `.tools/full-audit-fixes.log`,
  `.tools/full-audit-final-java.log`, `.tools/full-audit-isolated-java.log`, `.tools/full-audit-js.log`,
  `.tools/full-audit-cloudinary-env.log`.
- Lượt cuối lấy tổng kết trực tiếp từ Maven; báo cáo XML/TXT theo lớp nằm trong
  `target/surefire-reports`. Những lượt chạy riêng ghi đè báo cáo của lớp tương ứng,
  vì vậy không cộng tổng file hiện tại rồi gọi đó là kết quả của một lượt toàn bộ.

Không đồng nhất test H2 với việc đã kiểm chứng toàn bộ migration/query trên SQL Server.
Không đồng nhất test HTTP với việc đã xem xét mọi màn hình ở mọi kích thước trình duyệt.

### Phạm vi kiểm thử tự động

| Nhóm luồng | Nhóm test tiêu biểu | Giới hạn |
| --- | --- | --- |
| Đăng nhập, phân quyền, khóa tài khoản, JWT | `LoginRetryTests`, `FixedAccountRoleTests`, `JwtAuthenticationTests`, `AdminUsersManagementUseCaseTests` | Tài khoản kiểm thử, không đổi tài khoản thật |
| Danh mục, sản phẩm, tồn kho từng chi nhánh | `ProductCatalogFlowTests`, `StoreProductCatalogTests`, `BranchStockConsistencyTests`, `CodStockKpiTests` | Dữ liệu kiểm thử; dữ liệu cũ được kiểm tra chỉ đọc |
| Giỏ hàng, đặt COD, hủy, hoàn kho, đặt đồng thời | `CheckoutEdgeSafetyTests`, `CodCheckoutCommitTests`, `ConcurrencyRegressionTests`, `BusinessRegressionTests` | Không tạo đơn trong RentalShop |
| Chat, thông báo, giám sát chỉ đọc | `ChatBranchFlowTests`, `ChatSessionIntegrationTests`, `ChatSocketAccessTests`, `ChatSupportMonitorTests` | Không thay thế việc xem trực quan hai trình duyệt/mobile |
| Bộ lọc ngày, doanh thu, quản trị cửa hàng | `ReportDateFilteringTests`, `ReportRangeTests`, các lớp `Admin*UseCaseTests` và `StoreManagement*Tests` | Không xác nhận độ chính xác dữ liệu kế toán thực tế |
| OTP, Cloudinary, thanh toán | `OtpRecipientRoutingTests`, `BrevoOtpSenderTests`, `CloudinaryServiceTests`, `MomoSandboxSecurityTests`, `BankSandboxTests` | Mock/sandbox; không gửi thư, upload hoặc thanh toán thật trong đợt này |
| Render trang qua HTTP | `SystemHttpSmokeTests` | Server test cổng ngẫu nhiên; kiểm tra trạng thái/HTML, không kiểm tra toàn bộ CSS bằng ảnh chụp |

## Database gốc: kiểm tra chỉ đọc

SQLEXPRESS cổng 53735, database RentalShop:

- Migration mới nhất V8, thành công.
- 4 cửa hàng, 15 tài khoản, 39 sản phẩm, 26 đơn hàng tại thời điểm đọc.
- Không có bản ghi tồn kho âm.
- Có 81 dòng tồn kho mà chi nhánh của sản phẩm khác chi nhánh của dòng kho.
  Đây là dữ liệu cũ cần đối soát; không tự chuyển/xóa, không sửa đơn #23.
  Luồng danh mục/đặt đơn phải tiếp tục loại kho lệch chi nhánh khỏi lượng bán hợp lệ.

## Các điểm còn cần hoàn thiện trước vận hành thật

1. **Khoảng cách giao hàng chưa phải khoảng cách bản đồ.** `estimateDistanceKm`
   hiện suy đoán từ chuỗi địa chỉ và tên chi nhánh; địa chỉ lạ vẫn có giá trị mặc định.
   Không dùng nó làm cam kết chính xác 5/15 km. Cần chốt vùng giao bằng mã địa bàn
   hoặc tích hợp tọa độ/bản đồ; việc thêm nhà cung cấp cần tài khoản và lựa chọn của chủ dự án.
2. **Biểu phí vẫn phụ thuộc cách viết tên vùng** (`< 5km`, `5 - 15km`). Cần chuẩn hóa
   mã vùng, dịch vụ và thuộc tính hàng cồng kềnh thay vì suy đoán từ tên sản phẩm.
   Checkout hiện chọn đơn vị, chưa chọn riêng dịch vụ; nếu cấu hình nhiều dịch vụ
   cùng đơn vị/vùng thì phải bổ sung lựa chọn dịch vụ hoặc quy tắc ưu tiên rõ ràng.
   Khi hoàn toàn chưa có biểu phí, hệ thống còn đường tương thích lấy `delivery.price`;
   phải cấu hình đầy đủ biểu phí trước demo luồng giao hàng và trước vận hành thật.
3. **Dữ liệu kho lệch chi nhánh:** đối soát 81 dòng, có backup và phương án chuyển đổi
   được duyệt; không sửa hàng loạt chỉ để số liệu trông khớp.
4. **Thanh toán:** cấu hình mặc định MoMo là `mock`, endpoint là sandbox. Không trình
   bày mô phỏng như giao dịch ngân hàng thật; cần tích hợp/đối soát production nếu triển khai.
5. **Bí mật đã từng chia sẻ:** nên thu hồi/đổi khóa Brevo và Cloudinary từng gửi trong
   chat/ảnh; chỉ đặt khóa mới ngoài source control. Chưa kiểm chứng việc chủ tài khoản đã đổi khóa.
6. **Nghiệm thu thủ công sau restart:** hai phiên khách/quản lý nhận chat/thông báo không
   reload; upload ảnh và nhận OTP thật; giao diện mobile; dữ liệu/ảnh lưu lại sau restart.
   Người dùng đã xác nhận OTP/Cloudinary thật ở các lượt trước, không được coi là đã
   chạy lại hai dịch vụ đó trong đợt này.
7. **Phát hành:** còn nhiều thay đổi chưa commit trong workspace từ các đợt trước.
   Cần rà soát phạm vi, test và commit theo nhóm; không `git add .` rồi push toàn bộ thiếu kiểm tra.

## Luồng nghiệp vụ cần dùng khi nghiệm thu

- Mua đúng chi nhánh → trừ kho khi đặt thành công, kể cả COD → hết hàng được hiển thị.
- Khách được hủy khi PENDING/CONFIRMED; đã PREPARING (bắt đầu cắm hoa) hoặc DELIVERING
  thì không được tự hủy. Hủy hợp lệ hoàn kho; admin hủy phải có lý do.
- Quản lý chỉ thao tác chi nhánh được phân quyền; không truy cập chat/kho chi nhánh khác.
- Admin giám sát chat khách–chi nhánh ở chế độ chỉ đọc và có nhật ký truy cập.
- Bộ lọc ngày, quyền JWT, khóa tài khoản, CSRF, đồng thời đặt hàng và thanh toán lặp
  được kiểm tra bằng các test chuyên biệt trong suite, không chỉ nhìn giao diện.

Để nhận bản sửa Java, dừng tiến trình website cũ rồi chạy `./scripts/sqlserver.ps1`.
Chỉ tải lại trình duyệt không làm tiến trình Java đang chạy nạp bản sửa.
