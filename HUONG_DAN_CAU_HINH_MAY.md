# Hướng dẫn cấu hình máy

Hướng dẫn dành cho máy Windows chạy Rental-Retail-Shop với database Aiven
chung của nhóm và Cloudflare cho trang thanh toán trên điện thoại.

Sau khi thiết lập lần đầu, mỗi lần chạy chỉ cần:

```powershell
mvn spring-boot:run
```

## 1. Cài đặt công cụ

Cài các công cụ sau:

- JDK 21 trở lên. Cần bản JDK, không chỉ JRE.
- Maven để chạy lệnh `mvn`.
- Node.js để chạy cổng thanh toán đi kèm Cloudflare.
- Git nếu tải và cập nhật code bằng Git.

Trong phần biến môi trường Windows:

1. Đặt `JAVA_HOME` trỏ tới thư mục JDK, ví dụ `C:\Program Files\Java\jdk-21`.
2. Thêm `%JAVA_HOME%\bin` vào biến `Path`.
3. Thêm thư mục `bin` của Maven vào `Path`.
4. Đóng và mở lại terminal hoặc VS Code để nhận biến môi trường mới.

Mở PowerShell mới và kiểm tra:

```powershell
java -version
mvn -v
node --version
git --version
```

Các lệnh phải hiển thị phiên bản, không báo `not recognized`.
Trong kết quả `mvn -v`, kiểm tra Maven thực sự đang dùng JDK 21 trở lên.

## 2. Nhận code và các file cần thiết

Tải hoặc clone bản code mới nhất có hỗ trợ Aiven, thiết lập cấu hình riêng
và tự khởi động Cloudflare. Nếu đã có code cũ, giữ lại thay đổi riêng trước
khi cập nhật theo nhánh thống nhất của nhóm.

Chuẩn bị thêm:

- `ca.pem`: chứng chỉ CA của đúng dịch vụ Aiven mà nhóm đang sử dụng.
- `cloudflared.exe`: chương trình Cloudflare phù hợp với máy Windows.

Nhận các file này từ người phụ trách nhóm. Có thể tải lại chứng chỉ CA
từ phần thông tin kết nối của dịch vụ Aiven. Với chương trình Cloudflare,
chỉ sử dụng bản từ nguồn chính thức hoặc bản đã được nhóm kiểm tra.

Đặt file theo cấu trúc sau:

```text
CK/
|-- ca.pem
`-- Rental-Retail-Shop/
    |-- pom.xml
    |-- scripts/
    |   `-- run-aiven.ps1
    `-- .tools/
        `-- cloudflared.exe
```

`ca.pem` là chứng chỉ công khai, không chứa mật khẩu database.
Thư mục `.tools` bị Git bỏ qua, nên máy mới không tự có
`cloudflared.exe` chỉ bằng việc tải code từ GitHub.

## 3. Mở đúng thư mục dự án

Mở thư mục `Rental-Retail-Shop` trong VS Code, chọn
**Terminal > New Terminal**.

Hoặc mở PowerShell và chuyển vào thư mục dự án. Ví dụ nếu đặt code tại
`D:\DuAn\CK\Rental-Retail-Shop`:

```powershell
cd D:\DuAn\CK\Rental-Retail-Shop
```

Thay đường dẫn ví dụ bằng vị trí thực tế trên máy mới.
Không chạy các lệnh của dự án khi terminal còn ở `C:\Users\...`.

Kiểm tra:

```powershell
Test-Path .\pom.xml
Test-Path .\scripts\run-aiven.ps1
Test-Path ..\ca.pem
Test-Path .\.tools\cloudflared.exe
```

Cả bốn kết quả phải là `True` nếu dùng cấu trúc thư mục mặc định.

## 4. Thiết lập Aiven một lần trên mỗi máy

Từ thư mục dự án, chạy:

```powershell
.\scripts\run-aiven.ps1 -Mode Check -SaveLocalConfiguration
```

Khi hiện yêu cầu mật khẩu:

1. Nhận mật khẩu database qua kênh riêng từ người phụ trách nhóm.
2. Dán bằng **Shift+Insert** hoặc chuột phải, rồi nhấn **Enter**.
3. Không dùng mật khẩu Google, mật khẩu website, Service URI hoặc các dấu sao che mật khẩu.

Chờ các thông báo thành công:

```text
OK: verified TLS certificate and PostgreSQL login. No data changed.
Local Aiven configuration ready.
```

Script kiểm tra kết nối và chứng chỉ trước khi lưu cấu hình.
Bước này không sao chép dữ liệu và không tạo giao dịch thanh toán.
Lần đầu có thể mất thời gian để Maven tải các thư viện cần thiết.

Sau khi thành công, máy tự tạo file `.env.aiven` ngay cạnh `pom.xml`,
gồm thông tin kết nối và khóa bảo mật riêng. File chứa thông tin nhạy cảm
và được giới hạn quyền truy cập cho tài khoản Windows tạo nó.

- Không đưa `.env.aiven` lên GitHub, không dùng `git add -f` cho file này.
- Không gửi file cấu hình riêng cho người khác hoặc kèm vào bản ZIP dự án.
- Mỗi người tự chạy bước thiết lập trên tài khoản Windows của mình.
- Khi mật khẩu database được thay đổi, chạy lại bước này với mật khẩu mới.
- Nên cấp tài khoản database riêng, với quyền phù hợp, thay vì chia sẻ tài khoản quản trị rộng rãi.

Nếu chứng chỉ đặt ở vị trí khác, chỉ rõ đường dẫn khi thiết lập:

```powershell
.\scripts\run-aiven.ps1 -Mode Check -SaveLocalConfiguration -CaCertificate "D:\ChungChi\ca.pem"
```

Nếu được cấp tài khoản database riêng, thêm tham số người dùng, ví dụ:

```powershell
.\scripts\run-aiven.ps1 -Mode Check -SaveLocalConfiguration -DatabaseUser "tai_khoan_duoc_cap"
```

Tài khoản riêng phải được người quản trị cấp đủ quyền cho ứng dụng;
chỉ tạo tài khoản mà chưa cấp quyền thì chưa thể chạy dự án.

## 5. Chạy website và Cloudflare

Mỗi lần sử dụng, mở terminal trong thư mục dự án rồi chạy:

```powershell
mvn spring-boot:run
```

Chờ dòng `Started FinalprojectApplication`, sau đó mở:

[http://localhost:8080](http://localhost:8080)

Đăng nhập bằng tài khoản website của nhóm, không dùng tài khoản database
hoặc mật khẩu Aiven để đăng nhập website.

Cloudflare tự khởi động cùng ứng dụng. Chờ thông báo
`Demo phone payment ready` trước khi quét QR thanh toán.

- QR sử dụng link HTTPS Cloudflare hiện tại, không dùng IP Tailscale cũ.
- Cloudflare chỉ mở các đường dẫn thanh toán và tài nguyên cần thiết.
- Các trang quản trị, ví khách hàng và checkout không được công khai qua cổng này.
- Trang gốc của link Cloudflare là trang kiểm tra kết nối, không phải trang chủ cửa hàng.
- Link Cloudflare có thể đổi sau mỗi lần khởi động; mở lại trang thanh toán để lấy QR mới.
- Không dùng `localhost` trên điện thoại để truy cập website đang chạy trên máy tính.

Giữ terminal mở trong suốt thời gian sử dụng.
Nhấn **Ctrl+C** trong terminal đang chạy để dừng website;
cổng thanh toán và Cloudflare đi kèm cũng sẽ dừng.
Đóng tab trình duyệt không có nghĩa là đã dừng ứng dụng.

## 6. Lưu ý khi dùng chung trong nhóm

- Không chạy lại bước nhập dữ liệu hoặc sao chép database khi thiết lập máy mới.
- Các máy dùng chung dữ liệu: sửa hoặc xóa sản phẩm, đơn hàng và giao dịch ví ảnh hưởng cả nhóm.
- Mỗi máy đều có thể dùng cổng `8080`, miễn cổng đó còn trống trên chính máy đó.
- Aiven chỉ lưu trữ database; website vẫn cần máy đang chạy ứng dụng và có Internet.
- Gói hiện tại có giới hạn 20 kết nối; mỗi phiên ứng dụng cấu hình tối đa 3 kết nối.
  Không mở nhiều phiên ứng dụng không cần thiết trên cùng máy.
- Email và Cloudinary cần thông tin cấu hình riêng nếu muốn sử dụng đầy đủ các tính năng tương ứng.

## 7. Xử lý lỗi thường gặp

### Không tìm thấy script

Nếu báo `run-aiven.ps1 is not recognized`, kiểm tra terminal đang ở thư mục
`Rental-Retail-Shop`, nơi có `pom.xml` và thư mục `scripts`.

### Không tìm thấy Java, Maven hoặc Node.js

Kiểm tra lại cài đặt, `JAVA_HOME` và `Path`, sau đó mở lại terminal hoặc
VS Code. Không đặt `JAVA_HOME` vào thư mục `bin`.

### Không tìm thấy ca.pem hoặc kiểm tra chứng chỉ thất bại

Kiểm tra đường dẫn và sử dụng chứng chỉ của đúng dịch vụ Aiven.
Nếu đặt chứng chỉ ở vị trí khác, thiết lập lại bằng `-CaCertificate`.
Không tắt xác minh TLS để bỏ qua lỗi chứng chỉ.

### Đăng nhập database bị từ chối: SQLSTATE 28P01

Kiểm tra đúng dịch vụ, đúng người dùng và mật khẩu database hiện tại.
Dán lại bằng **Shift+Insert** hoặc chuột phải. Không dùng Service URI
hoặc mật khẩu đăng nhập website thay cho mật khẩu database.

### Cổng 8080 đang được sử dụng

Nếu đã có một phiên dự án đang chạy, dùng phiên đó hoặc dừng bằng
**Ctrl+C** tại terminal cũ. Không dừng toàn bộ tiến trình Java trên máy.

Để chủ động chạy trên cổng khác:

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Khi đó mở [http://localhost:8081](http://localhost:8081).

### Website chạy nhưng điện thoại không mở được trang thanh toán

Kiểm tra Node.js, `.tools/cloudflared.exe` và kết nối Internet.
Chờ `Demo phone payment ready`, sau đó mở lại trang thanh toán và quét QR mới.
Không mở lại link Tailscale hoặc Cloudflare của lần chạy trước.
Nếu chưa có kết nối Cloudflare, trang thanh toán sẽ chờ hoặc báo lỗi kết nối;
không coi đó là giao dịch đã thanh toán thành công.

### Ứng dụng vẫn kết nối SQL Server hoặc chọn cấu hình cũ

Kiểm tra đã nhận đúng bản code có cấu hình Maven mặc định là Aiven.
Gỡ các biến môi trường `SPRING_*` cũ dùng cho SQL Server hoặc cấu hình chạy
cũ trong terminal/IDE, rồi mở lại terminal. Không bật đồng thời các cấu hình
`aiven`, `dev` hoặc `test` cho phiên ứng dụng Aiven.
