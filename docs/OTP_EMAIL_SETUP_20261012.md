# OTP gửi đúng email, không dùng Gmail App Password

## Hiểu đúng yêu cầu

Người dùng nhập email của họ khi đăng ký. Website tạo OTP, gửi từ một địa chỉ của hệ thống tới đúng email người dùng. Người dùng không cung cấp mật khẩu Gmail/App Password; người vận hành cũng không phải chèn mật khẩu vào code.

Website vẫn cần một phương thức xác thực gửi email để chống giả mạo/spam. Đã chọn **Brevo API** thay cho Gmail SMTP. Cấu hình người gửi chỉ làm một lần; không thiết lập credential cho từng email người nhận. Theo [tài liệu Brevo](https://developers.brevo.com/docs/send-a-transactional-email), cần API key/quyền API và thiết lập sender/domain trước khi gửi.

## Thay đổi mã nguồn

- `EmailServiceImpl`: mặc định chọn API, kiểm tra địa chỉ nhận là một mailbox duy nhất và OTP có 6 chữ số. Nhánh API không cần `spring.mail.password` và không gọi Gmail SMTP. SMTP cũ chỉ chạy nếu chọn rõ `MAIL_PROVIDER=smtp`.
- `BrevoOtpSender`: POST HTTPS `/v3/smtp/email`, API key chỉ nằm trong header phía server, `to` chứa đúng người nhận động, `sender` lấy cấu hình hệ thống. Chỉ gửi một loại body `textContent` theo [API contract](https://developers.brevo.com/reference/send-transac-email).
- Chỉ coi đã tiếp nhận khi API trả 201 và messageId không rỗng. Lỗi key/sender, quota, HTTP 4xx/5xx, timeout, redirect hoặc phản hồi sai không báo gửi thành công; không log OTP/key/nội dung lỗi có thể chứa mã.
- `MailApiConfig`: timeout kết nối 8 giây, đọc 12 giây, không tự đi theo redirect mang API key.
- Giữ cleanup/thử lại khi đăng ký gửi thất bại; gửi lại thất bại không vô hiệu mã đã gửi trước đó; OTP đổi mật khẩu gửi thất bại trả 503 và xóa riêng mã chưa gửi.
- `scripts/configure-mail.ps1`: nhận API key bằng SecureString và lưu mã hóa Windows DPAPI trong `.tools/mail-local.json`, ngoài Git. Cấu hình có sẵn chỉ bị thay nếu người dùng xác nhận REPLACE.
- `scripts/sqlserver.ps1`: tự nạp cấu hình mail mã hóa khi chạy local, ưu tiên biến môi trường đã đặt, khôi phục biến môi trường khi kết thúc. Không yêu cầu người dùng nhập mail sender/key lại cho mỗi OTP.

## Cần làm một lần để gửi mail thật

1. Tạo tài khoản Brevo; thiết lập/xác minh địa chỉ/domain gửi theo điều kiện nhà cung cấp.
2. Trong phần SMTP & API → API keys, tạo API key. Không lấy SMTP key. [Hướng dẫn API key](https://developers.brevo.com/docs/api-key-authentication).
3. Chạy:

   ```powershell
   .\scripts\configure-mail.ps1
   ```

   Nhập địa chỉ **người gửi** đã được xác minh và key tại ô nhập che. Script này chỉ lưu cấu hình, không gửi một thư kiểm thử hoặc tạo tài khoản dịch vụ giúp bạn.

   Phải dán **toàn bộ API key** đã sao chép từ Brevo. Dấu `*` khi nhập chỉ là ký tự che, không phải key cần nhập. Script từ chối key quá ngắn và không ghi đè cấu hình cũ khi nhập chưa đủ; có thể chạy lại và gõ `REPLACE` nếu muốn thay key đã lưu.

4. Chạy website:

   ```powershell
   .\scripts\sqlserver.ps1
   ```

5. Đăng ký bằng một email bạn sở hữu, kiểm tra Inbox/Spam và nhập OTP trong 5 phút. Thử thêm email người nhận khác để xác nhận không cố định địa chỉ.

Không gửi API key trong chat, không commit key, không dùng `app.mail.mock-enabled=true` ở môi trường thật. File mã hóa local chỉ giải mã được bởi tài khoản Windows đã tạo nó; chạy IDE/máy khác phải cấu hình environment/secret manager riêng.

## Nếu dùng IDE hoặc server

Đặt ngoài code:

| Biến môi trường | Ý nghĩa |
|---|---|
| MAIL_PROVIDER | `brevo` (mặc định) |
| BREVO_API_KEY | Key API của tài khoản dịch vụ gửi |
| MAIL_FROM_EMAIL | Địa chỉ người gửi đã được dịch vụ xác minh/chấp nhận |
| MAIL_FROM_NAME | Tên website, tùy chọn |

Không cần `SPRING_MAIL_PASSWORD` khi dùng API. Email **người nhận** không nằm trong bảng cấu hình này: nó đến từ yêu cầu/tài khoản hiện tại.

## Giới hạn và bảo mật

- Đăng ký: gửi tới email mới nhập, sau chuẩn hóa. Gmail/Outlook/domain khác đều được chuyển nguyên địa chỉ tới API nếu hợp lệ; không đảm bảo một mailbox không tồn tại có thể nhận thư.
- Gửi lại OTP đăng ký: gửi tới email của đăng ký đang chờ.
- Quên mật khẩu: phải có tài khoản, gửi tới email lưu trên tài khoản—even khi người dùng nhập tên đăng nhập.
- Đổi mật khẩu: chỉ gửi tới email của tài khoản đang đăng nhập; request thay địa chỉ sang email khác bị từ chối, tránh chuyển OTP cho người không sở hữu tài khoản.
- API tiếp nhận không đồng nghĩa đã vào Inbox. Khi không nhận: kiểm tra email nhập, Spam, trạng thái sender/domain, API key, kích hoạt transactional email, quota và delivery logs của nhà cung cấp.
- Tại máy kiểm tra chưa có API key/người gửi, nên chưa gửi thư thật. Không tạo account, thu phí hoặc gửi thử tới các email demo/tài khoản người khác.

## Kiểm thử

Đã bổ sung kiểm tra API payload, địa chỉ nhận động, tách sender/receiver, không cần Gmail password, phản hồi thành công/lỗi/malformed/redirect, bảo mật log, recipient injection, OTP sai định dạng và routing của đăng ký/gửi lại/quên/đổi mật khẩu. HTTP API được mock trong test, không gửi thư ra ngoài.

Regression/package cuối lượt này: **435 test Java, 0 failures, 0 errors, 3 skipped** (432 pass; 2 SQL Server live opt-in và 1 Cloudinary live không chạy mặc định). Riêng 33 bài mới kiểm tra API/provider/routing đều đạt; JAR đóng gói thành công. Hai script cấu hình/chạy đã kiểm tra cú pháp PowerShell, file mail local được Git ignore. Log: `.tools/email-api-full-verification.log`.

Kiểm thử này chứng minh mã nguồn gửi đúng yêu cầu và xử lý lỗi an toàn; **chưa phải kiểm thử nhận thư thật**, vì chưa có credential dịch vụ tại máy. Cần bước cấu hình và kiểm tra hộp thư ở trên để xác nhận giao thư đầu-cuối.

### Cập nhật khi chạy trên Windows PowerShell 5.1

Đã sửa `scripts/maven.ps1` để đọc stdout/stderr của `java -version` bằng process riêng, không bị `NativeCommandError` khi JAVA_HOME đang là Java 26. Đã xác nhận script chọn JDK 21 portable trên cả PowerShell 5.1 và 7, trả lại JAVA_HOME/PATH sau chạy, và `sqlserver.ps1 -Action info` đọc được Flyway V4 trong PowerShell 5.1.

Sau khi người dùng lưu mail local, kiểm tra chỉ đọc phát hiện key đã nhập quá ngắn; không hiển thị key và chưa gọi API/gửi thư. Script cấu hình và backend đã bổ sung chặn key quá ngắn. Chạy lại `configure-mail.ps1`, xác nhận `REPLACE` rồi dán toàn bộ API key mới; dấu `*` chỉ là ký tự che khi nhập. Nhóm mail chạy bằng Windows PowerShell 5.1 đạt **41/41 test** (thêm kiểm tra key chưa đầy đủ). Log: `.tools/powershell51-mail-tests.log`, `.tools/powershell51-launch-verification.log`.
