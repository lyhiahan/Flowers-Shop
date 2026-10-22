# Kế hoạch xử lý vấn đề hệ thống — 21/10/2026

Mốc mã nguồn ban đầu: `ca2b9a7`. Phạm vi: hai lỗi migration trong commit mới và các vấn đề còn mở trong báo cáo hệ thống ngày 20/10. Kế hoạch đã được triển khai theo yêu cầu tiếp tục của người dùng; xem [kết quả ngày 22/10](REMEDIATION_RESULTS_20261022.md). Các mục công việc bên dưới lưu lại tiêu chí ban đầu, không phải danh sách lỗi còn mở.

## 1. Lịch sử khởi chạy tạm — đã kết thúc

**Cập nhật 22/10:** Tiến trình preview Java 13620 bên dưới đã dừng. Database gốc `RentalShop` đã migrate chuẩn đến V15, giữ nguyên checksum V11 và đã đối soát dữ liệu demo sau khi sao lưu. Không dùng lại cấu hình tắt Flyway dưới đây. Quy trình chạy hiện hành là `scripts/run-local.ps1 -DatabaseName RentalShop`; trạng thái nghiệm thu nằm trong báo cáo kết quả mới.

- Website xem thử: `http://localhost:8080`, chỉ bind vào `127.0.0.1`.
- Đã xác nhận khởi động lúc **23:19:02 UTC+7 ngày 21/10**, tiến trình Java **13620**. Trang `/`, `/login`, `/products`, `/stores` trả **HTTP 200**; `/cart` và `/admin/dashboard` trả **302 về login** khi chưa đăng nhập. Đây là kiểm tra HTTP, chưa phải nghiệm thu thủ công toàn bộ UI.
- Database chạy thử: `CodexWebPreview_20261021_2210`, được tạo từ backup COPY_ONLY có checksum của `RentalShop`.
- Các script thay đổi schema/giao diện V11–V14 được áp dụng riêng lên bản sao. Trước V14 đã kiểm tra không có cart_id/product_id/store_id NULL. Không ghi các script này vào lịch sử Flyway của bản sao.
- Flyway chỉ tắt bằng biến môi trường của tiến trình preview. Cấu hình Git vẫn bật Flyway; database gốc vẫn giữ lịch sử V11 cũ.
- Seed dữ liệu tự động và tunnel Internet tắt. Thanh toán vẫn là mô phỏng, không phải môi trường bán hàng thật. Tác vụ đơn hàng và thao tác từ giao diện tác động vào bản sao.
- Đây là giải pháp để xem/chạy web trong lúc chờ sửa migration, không phải quy trình deploy hoặc cách khắc phục F01 bên dưới.
- Log: `target/web-preview-20261021.log`; lỗi tiến trình: `target/web-preview-20261021.err.log`; PID/cấu hình: `target/web-preview-20261021.json`. Script tạm nằm tại `target/start-preview-20261021.ps1`; Maven clean sẽ xóa những artifact này. Script từ chối ghi đè database preview đã tồn tại.

## 2. Thứ tự triển khai

| Thứ tự | Vấn đề | Phạm vi xử lý | Điều kiện hoàn thành |
| --- | --- | --- | --- |
| 1 — P1 | F01: Xung đột migration V11 | Đồng bộ migration gốc và phiên bản mới | Flyway validate/migrate đạt trên database mới và bản sao database hiện tại; app chạy với Flyway bật |
| 2 — P2 | F02: V14 cộng sai giỏ có khóa NULL | Sửa SQL và thêm ca dữ liệu cũ | Không tăng tổng quantity, không mất lời chúc/ngày; chạy lại không đổi kết quả |
| 3 — P1 | F03: MoMo mock còn dùng được với prod | Cấu hình, service và backend checkout | Prod từ chối cả mô phỏng và phương thức không nằm trong danh sách cho phép |
| 4 — P1 | F04: XSS tên cửa hàng trong giỏ | JavaScript modal giỏ | Tên có HTML chỉ hiển thị như văn bản, không tạo DOM có thể thực thi |
| 5 — P1 | F05: Phiên web cũ còn hiệu lực sau đổi/reset mật khẩu | Session, filter, các luồng thay mật khẩu | Phiên trước thay đổi bị từ chối ở request tiếp theo |
| 6 — P2 | F06: Payload thông báo có dữ liệu nhạy cảm | DTO WebSocket và serialization | JSON không chứa User entity, password hash hoặc trường OTP |
| 7 — P2 | F07: Đơn PAID thiếu chứng từ | Fixture và đối soát dữ liệu | Từng đơn có kết luận, dữ liệu tài chính đối chiếu được; luồng hoàn tiền hoạt động theo chính sách |
| 8 — P2 | F08: Kho lệch chi nhánh | Rà soát dữ liệu và nguồn seed | Không còn dòng chưa được phân loại; số lượng trước/sau có giải trình |
| 9 — P3 | Vận hành, hiệu năng và tài liệu | Script chạy, truy vấn, Clock, tài liệu, CI | Khởi chạy nhất quán; test tự động và hướng dẫn phù hợp trạng thái mới |

F01–F02 phải hoàn tất trước khi áp dụng migration mới lên database gốc. F03–F06 phải hoàn tất trước khi mở sử dụng bán hàng thật. Nhóm dữ liệu F07–F08 chỉ thực hiện cập nhật sau khi đã chốt phương án cho từng loại bản ghi và có bản sao lưu.

## 3. Công việc cụ thể và kiểm thử nghiệm thu

### F01 — Đồng bộ migration, bỏ cấu hình preview tạm

Hiện tại SQL Server `RentalShop` có `V11__review_image_public_id.sql`, checksum `306107251`. Commit mới thêm `V11__notification_title_unicode.sql`, checksum `2034378025`. Đã xác minh Flyway báo checksum mismatch bằng metadata sao chép; không repair hoặc thay lịch sử database gốc.

1. Tìm lại **đúng nội dung V11 gốc** từ người tạo migration/bản phát hành/nguồn đã lưu; kiểm tra checksum. Không viết lại nội dung dựa vào tên file rồi coi là bản gốc.
2. Thống nhất chuỗi phiên bản chưa triển khai: cấp số mới cho migration notification, restore title, ảnh và gộp giỏ. Rà soát các database của nhóm trước khi đổi số; nếu một migration mới đã được áp dụng ở máy khác thì cần kế hoạch tương thích, không đổi tùy ý.
3. Cập nhật tên file, test và tài liệu liên quan. Thêm kiểm tra migration bằng lịch sử đã áp dụng, thay vì chỉ thực thi từng file SQL riêng lẻ.
4. Kiểm tra database mới từ V1 và bản sao database hiện tại từ V11: validate → migrate → validate → khởi động ứng dụng. Đối chiếu đơn, payment, giao dịch và tồn kho trước/sau.
5. Khi đạt, bỏ override tắt Flyway của bản preview và diễn tập lại bằng quy trình migration chuẩn. Không dùng `repair`/tắt validate để che checksum mismatch.

### F02 — Gộp giỏ không làm sai dữ liệu cũ

V2 cho phép bổ sung các khóa giỏ cũ dưới dạng NULL. Trong V14, GROUP BY gom NULL nhưng JOIN bằng dấu `=` không khớp NULL; đã tái hiện bằng bảng tạm SQL Server: tổng 5 → 8, chạy lại → 11. Database gốc hiện tại không có store_id NULL.

1. Chọn chính sách rõ ràng: giữ nguyên và báo cáo nhóm thiếu khóa để xử lý riêng, hoặc dùng so sánh NULL nhất quán khi thật sự có thể gộp. Không suy đoán chi nhánh của dòng cũ.
2. Áp dụng cùng quy tắc cho chọn lời chúc/ngày, UPDATE và DELETE.
3. Thêm test SQL Server cho khóa NULL, khác giỏ/sản phẩm/chi nhánh, BUY/RENT, lời chúc rỗng, tổng quantity và chạy hai lần.
4. Kiểm tra runtime `CartServiceImpl` vẫn gộp đúng dưới khóa giỏ; thêm thử hai request đồng thời. Giữ ổn định ID dòng được giữ lại.

### F03 — Cô lập thanh toán giả lập

Các file trọng tâm: `MomoConfig`, `MomoServiceImpl.simulatePayment`, `BankSandboxService`, `CheckoutController`, `OrderServiceImpl`, cấu hình prod.

- Chặn mô phỏng trong prod ở service, kể cả request trực tiếp; rà soát đồng thời ngân hàng mock và MOMO_DEMO.
- Backend checkout kiểm tra đúng danh sách phương thức được phép trong cấu hình hiện hành, thống nhất với UI.
- Prod phải vô hiệu hóa phương thức chưa hỗ trợ hoặc từ chối khởi động với cấu hình thanh toán không hợp lệ. Hiện MoMo chỉ có mock/sandbox, chưa có luồng live được nghiệm thu.
- Test ma trận dev/test/prod × mock/sandbox/demo × request trực tiếp; bảo đảm không tạo PAID/payment SUCCESS/giao dịch ví khi yêu cầu bị từ chối. Giữ đúng giới hạn và idempotency của demo.

### F04 — Loại bỏ XSS trong giỏ

Trọng tâm: `customer/cart.html`, hàm `confirmDeleteCartItem`.

- Tạo icon riêng, đưa tên chi nhánh vào text node/textContent; không nối dữ liệu tên cửa hàng vào innerHTML.
- Rà các sink tương tự nhận tên sản phẩm/chi nhánh/nội dung người dùng. Escape tại thời điểm xuất đúng ngữ cảnh.
- Test tên chứa dấu nháy, `<img ... onerror=...>`, dấu `&` và tiếng Việt; xác nhận modal vẫn xóa đúng cartItem, hủy modal không gửi request.

### F05 — Thu hồi phiên sau thay đổi mật khẩu

Trọng tâm: `AccountStatusFilter`, `AuthServiceImpl`, `UserServiceImpl`, các controller đổi/reset mật khẩu, `JwtTokenService`.

- Chọn cơ chế credential version/security stamp hoặc thu hồi phiên tập trung. Mỗi session phải được đối chiếu với thông tin hiện tại; bao phủ mọi đường thay mật khẩu.
- Chốt hành vi với phiên đang đổi mật khẩu: đăng xuất hoặc cấp lại phiên hợp lệ; các phiên cũ ở thiết bị khác phải mất hiệu lực.
- Test hai phiên trình duyệt, reset qua OTP, request sau thay đổi, tài khoản khóa và đổi quyền. Xác nhận JWT access/refresh vẫn thu hồi theo hash thông tin xác thực như hiện tại.

### F06 — DTO thông báo an toàn

Trọng tâm: `NotificationServiceImpl`, entity `Notification`/`User`, JavaScript realtime và test serialization.

- Publish DTO chỉ gồm id, title, content, type, relatedOrderId, isRead, createdAt; không gửi quan hệ User/Store.
- Bổ sung bảo vệ serialization cho trường nhạy cảm như lớp phòng vệ thứ hai; không dựa riêng vào quyền subscribe để bảo vệ hash mật khẩu.
- Test JSON thực tế không có password/otpCode/email/địa chỉ không cần thiết; quyền subscribe vẫn đúng; notification vẫn publish sau commit và không phát khi rollback.

### F07 — Đối soát đơn đã thanh toán

Truy vấn chỉ đọc lại ngày 21/10 vẫn có **15 đơn PAID thiếu payment SUCCESS khớp số tiền**. Đây là vấn đề dữ liệu/fixture; không thể tự coi cờ PAID là chứng từ nhận tiền.

- Xuất danh sách đối soát nội bộ: order ID, phương thức, tổng tiền, trạng thái, receipt, giao dịch ví và hoàn tiền. Tách đơn mẫu/legacy khỏi đơn vận hành.
- Xác định nguồn dữ liệu; sửa `DemoOrderSeeder` và các fixture để tạo đủ receipt/giao dịch nhất quán.
- Với dữ liệu thật, chỉ bổ sung/sửa theo chứng từ và quyết định đã xác nhận; không tự chuyển tiền hoặc tự đánh dấu đã hoàn.
- Thử các đơn chờ hoàn thiếu receipt trên bản sao; giao dịch lỗi phải rollback toàn bộ ví/điểm/payment/order.

### F08 — Đối soát kho

Truy vấn chỉ đọc lại ngày 21/10 vẫn có **22 dòng kho lệch chi nhánh, 212 đơn vị**. Code catalog/KPI hiện loại những dòng này, nhưng dữ liệu thô cần được giải trình.

- Liệt kê từng dòng, chi nhánh sở hữu, hàng thực tế, đơn tham chiếu và nguồn seed/import.
- Chọn phương án giữ, cách ly, điều chỉnh hoặc xóa cho từng nhóm; không coi mọi dòng là dữ liệu trùng chỉ vì lệch chi nhánh.
- Chuẩn bị script có precondition, transaction, thống kê trước/sau và audit; diễn tập trên bản sao. Kiểm tra tổng kho hợp lệ, hoàn kho khi hủy và báo cáo không thay đổi ngoài dự kiến.

### Cải tiến vận hành và bảo trì

- Chuẩn hóa script chạy theo host/cổng/database qua cấu hình cục bộ; không ghi cổng động của máy cá nhân làm cấu hình chung. Preflight phát hiện sai schema/checksum trước khi chạy web.
- Giảm việc tải toàn bộ entity rồi phân trang trong Java; dùng Pageable/projection/aggregate và giới hạn page size. Đo query trước khi điều chỉnh OSIV.
- Thống nhất Clock UTC+7 cho voucher/OTP; hash sớm mật khẩu trong pending registration và thu hẹp vùng synchronized quanh việc gửi mail.
- Cập nhật tài liệu Shipper/VNPay cũ, tài khoản demo và migration hiện hành; thêm CI chạy Java/JavaScript và các kiểm tra migration phù hợp.

## 4. Mốc kiểm chứng và điều kiện bàn giao

1. **Mốc A — khởi chạy chuẩn:** database mới và bản sao database cũ đều migrate thành công, app khởi động với Flyway bật.
2. **Mốc B — bảo mật:** F03–F06 có test hồi quy đạt; kiểm tra request trực tiếp ngoài UI.
3. **Mốc C — dữ liệu:** có kết luận và audit cho 15 đơn/22 dòng kho; không còn sai khác chưa giải thích trước khi dùng dữ liệu thật.
4. **Mốc D — nghiệm thu:** COD/MoMo demo, hủy trước PREPARING, hoàn tiền, trừ/hoàn kho, rút tiền lặp, chat sai/đúng chi nhánh, ảnh, lịch sử xem, voucher và thống kê khoảng ngày. Kiểm tra desktop/mobile và luồng thất bại ngoài dịch vụ.

Mốc tham chiếu trước sửa: Java **653 ca: 643 đạt, 10 bỏ qua**, JavaScript **120/120 đạt** ở commit `ca2b9a7`. Đây không phải chứng nhận các lỗi nêu trên đã được sửa. Sau từng nhóm thay đổi chạy test liên quan; trước bàn giao chạy lại toàn bộ và ghi rõ test live nào chưa thực hiện.

## 5. Trạng thái sau triển khai

F01–F08 đã có thay đổi mã nguồn hoặc xử lý dữ liệu tương ứng và kiểm thử hồi quy. Người dùng xác nhận toàn bộ dữ liệu là demo; 15 chứng từ mô phỏng được gắn nhãn, 22 dòng kho được cách ly có audit, giữ nguyên 212 đơn vị. Lịch sử V11 gốc không bị repair. Nhóm P3 đã có script chạy, phân trang người dùng ở database, Clock cho AuthService/voucher, hash mật khẩu đăng ký chờ xác thực và CI.

Các cải tiến còn lại: đo và tối ưu truy vấn thống kê/OSIV; thống nhất Clock ở những controller còn dùng giờ hệ thống; thu hẹp khóa đồng bộ ở resend/verify OTP; nghiệm thu giao diện desktop/mobile và tích hợp dịch vụ thật. Chưa commit/push hoặc công bố website ra Internet. Số liệu kiểm thử và địa chỉ web hiện hành được ghi trong báo cáo kết quả ngày 22/10.
