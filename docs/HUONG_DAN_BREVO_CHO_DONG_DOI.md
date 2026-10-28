# Hướng dẫn dùng Brevo để gửi OTP — gửi cho đồng đội

Tài liệu này có thể chia sẻ qua GitHub/chat nhóm. **Không điền API key, mật khẩu hoặc mã OTP thật vào file này.** Mỗi máy chạy ứng dụng cần được cấu hình riêng; `git pull` không mang theo khóa Brevo từ máy của thành viên khác.

## Thông tin cần nhận từ người quản lý Brevo

- Địa chỉ email **người gửi** đã được xác minh trên Brevo (`MAIL_FROM_EMAIL`). Đây là email của cửa hàng, **không phải** email khách nhận OTP.
- Một Brevo API key còn hiệu lực (`BREVO_API_KEY`). Người quản lý cung cấp qua kênh riêng an toàn; tốt nhất tạo khóa riêng cho việc dùng chung trong nhóm để có thể thu hồi khi cần. Không gửi khóa trong nhóm chat công khai, ảnh chụp, issue, commit hoặc tài liệu này.
- Tên hiển thị người gửi (`MAIL_FROM_NAME`) nếu muốn; mặc định là “Hoa Tươi Sài Gòn”.

Nếu người quản lý không còn thấy giá trị API key cũ, hãy **tạo khóa mới trong Brevo**. Brevo chỉ hiển thị khóa đầy đủ một lần khi tạo; không lấy khóa bằng cách sao chép file `.tools/mail-local.json` của máy khác.

### Nếu người quản lý gửi gói cấu hình đã mã hóa

Người quản lý chạy `./scripts/share-brevo-secret.ps1 -Mode Export`. Script tạo **hai file riêng** trong `.tools`: `brevo-share.enc.json` (cấu hình Brevo đã mã hóa) và `brevo-share-unlock.key` (khóa mở). Gửi hai file qua **hai kênh riêng tư khác nhau**, không đưa cả hai vào cùng một tin nhắn, kho Git, bản nộp hoặc ảnh chụp.

Đồng đội đặt hai file ở `.tools` trên máy Windows của họ, rồi chạy:

```powershell
.\scripts\share-brevo-secret.ps1 -Mode Import
```

Lệnh nhập tạo `.tools/mail-local.json` được mã hóa riêng cho tài khoản Windows của đồng đội. Nếu họ đã có file cấu hình mail, script sẽ từ chối ghi đè; cần thống nhất với chủ file trước khi thay. Sau khi nhập thành công, xóa **bản sao** hai file chuyển giao trên máy nhận khi không còn cần. Sau đó chạy website bằng `./scripts/sqlserver.ps1 -Action run`.

## Cấu hình trên Windows khi chạy bằng script của dự án

Mở PowerShell tại thư mục gốc dự án, sau đó chạy:

```powershell
.\scripts\configure-mail.ps1
```

1. Nhập email người gửi đã xác minh tại lời nhắc `Sender email...`.
2. Dán **toàn bộ** Brevo API key ở lời nhắc ẩn `Brevo API key...` (màn hình không hiện ký tự khi dán).
3. Script tạo `.tools/mail-local.json`, mã hóa khóa bằng Windows DPAPI cho **tài khoản Windows của máy này**. File đó không được Git theo dõi và không dùng được trên máy khác.
4. Khởi động website bằng lệnh đang dùng trong dự án, ví dụ:

```powershell
.\scripts\sqlserver.ps1 -Action run
```

Script khởi động sẽ nạp `MAIL_PROVIDER=brevo`, `MAIL_FROM_EMAIL`, `MAIL_FROM_NAME` và `BREVO_API_KEY` **tạm thời cho tiến trình ứng dụng**. Nó không đặt biến môi trường toàn hệ thống. Lệnh trên vẫn cần database SQL Server đã cấu hình đúng; cấu hình Brevo không thay thế cấu hình database.

Nếu chạy từ IDE, máy Linux/macOS hoặc môi trường cloud, file DPAPI trên không được tự nạp. Hãy đặt bốn biến trên trong **run configuration/secret manager của môi trường đó** rồi khởi động lại ứng dụng. Đừng đưa giá trị thật vào `application.properties`, `.env.example` hoặc GitHub Actions workflow. `MAIL_PROVIDER=brevo` là giá trị mặc định của mã nguồn nhưng vẫn nên đặt rõ trong môi trường triển khai.

## Kiểm tra đúng luồng

1. Dùng **hộp thư thử nghiệm do bạn sở hữu** để đăng ký tài khoản hoặc yêu cầu quên/đổi mật khẩu; không dùng email người khác để thử.
2. Kiểm tra có nhận mã OTP 6 chữ số trong hộp thư hoặc Spam. Email người **nhận** lấy từ biểu mẫu/tài khoản đang thao tác; email người **gửi** là địa chỉ đã xác minh ở Brevo.
3. Trên Brevo, xem mục giao dịch email để kiểm tra trạng thái. API trả `201` và `messageId` chỉ xác nhận Brevo **tiếp nhận** yêu cầu; cần trạng thái `delivered` hoặc xác nhận từ hộp thư để biết thư đã tới.
4. Nếu không nhận được thư, kiểm tra cấu hình đã được nạp trong **đúng tiến trình đang chạy**, email người gửi có hoạt động, quyền IP/API key, thư Spam và log ứng dụng. Không chụp/gửi log có OTP hoặc secret.

Các test Java trong dự án dùng API giả/mô phỏng; test đạt **không** thay thế phép thử gửi thư thật. Có thể chạy test mã nguồn bằng:

```powershell
.\mvnw.cmd -q '-Dtest=BrevoOtpSenderTests,OtpMailProviderTests,OtpRecipientRoutingTests' test
```

## Khi rời nhóm hoặc nghi lộ khóa

Người quản lý Brevo thu hồi/đổi API key, cấp khóa mới theo kênh riêng và yêu cầu từng máy cấu hình lại. Không gửi lại file DPAPI hoặc đưa khóa vào repository để “đồng bộ”.

Tài liệu chính thức: [xác thực API key](https://developers.brevo.com/docs/api-key-authentication), [người gửi và domain](https://developers.brevo.com/docs/getting-started-with-senders-and-domains), [API gửi email giao dịch](https://developers.brevo.com/reference/send-transac-email).
