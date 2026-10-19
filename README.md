# Chuỗi cửa hàng hoa — Spring Boot

Website bán hoa theo mô hình nhiều chi nhánh. Khách hàng xem sản phẩm, chọn chi nhánh còn hàng, đặt hàng, thanh toán và chat với cửa hàng/admin. Quản lý xử lý tồn kho, đơn hàng, nhân sự, voucher, doanh thu và ví theo quyền tại từng chi nhánh. Admin quản trị toàn chuỗi.

Khách chỉ được hủy đơn ở trạng thái chờ xác nhận hoặc đã xác nhận, không giới hạn một giờ kể từ lúc đặt. Từ lúc cửa hàng bắt đầu cắm hoa/chuẩn bị (`PREPARING`), khách không thể tự hủy, kể cả đơn chưa giao hoặc nhận tại quầy. Chính sách được hiển thị trước khi đặt hàng, trên đơn và trong thông báo bắt đầu chuẩn bị; quản lý/admin phải xác nhận cảnh báo trước khi bắt đầu cắm hoa. Hủy đơn chưa thanh toán trả tồn kho và chuyển sang đã hủy; đơn đã thanh toán chuyển sang chờ cửa hàng hoàn tiền, không tự ghi nhận tiền đã được hoàn.

Luồng giao tận nơi: Chờ xác nhận → Đã xác nhận → Đang cắm hoa/Chuẩn bị → Đang giao → Đã giao → Hoàn tất. Đơn nhận tại cửa hàng đi từ Đang chuẩn bị tới Khách đã nhận tại quầy/Hoàn tất, không có bước vận chuyển. Quản lý/admin vẫn có quyền hủy ngoại lệ trong lúc chuẩn bị theo luồng hiện có; đây không phải quyền tự hủy của khách. Việc hủy ngoại lệ ghi lý do, trả tồn kho về chi nhánh trên đơn và chuyển đơn đã thanh toán sang chờ xác nhận hoàn tiền; cần kiểm tra hàng thực tế khi xử lý ngoại lệ.

Admin/quản lý chọn **Hủy đơn hàng** trong hộp xác nhận phải nhập lý do (không được trống, tối đa 300 ký tự). Backend cũng kiểm tra, kể cả gửi request trực tiếp. Lý do và người hủy được lưu trên đơn; khách nhận thông báo “Đơn hàng của bạn đã bị hủy bởi admin (quản trị viên)/quản lý cửa hàng với lý do: …” và xem lại trên danh sách/chi tiết đơn. Đơn đã thanh toán vẫn chờ hoàn tiền; xác nhận hoàn tiền không làm mất lý do hủy ban đầu. Thao tác cập nhật trạng thái khác không yêu cầu lý do.

Đơn đặt thành công trừ tồn kho ngay tại chi nhánh xử lý, kể cả thanh toán khi nhận hàng (COD); không chờ xác nhận, thu tiền hoặc bắt đầu giao mới trừ. Đặt thất bại rollback tồn kho; hủy trước khi giao hoàn kho đúng một lần. Dashboard quản lý/admin có KPI **Tồn kho còn lại** tự cập nhật mỗi 5 giây, độc lập bộ lọc ngày. Đây là tổng số lượng `store_inventories.quantity` hợp lệ của mẫu riêng đúng chi nhánh hoặc mẫu chung, chưa xóa tại hệ thống/chi nhánh (bao gồm sản phẩm tạm ngừng bán/cửa hàng tạm đóng), không phải số mẫu sản phẩm mới hay số đơn đã thu tiền. Trang quản lý chỉ hiển thị tồn kho chi nhánh đang chọn. Kho cũ của mẫu riêng thuộc chi nhánh khác không cộng vào KPI, không xuất hiện trong danh sách/cảnh báo; URL `tab=legacy` không còn được hỗ trợ. Dữ liệu kho và đơn cũ giữ nguyên. Doanh thu COD chỉ được ghi nhận khi đủ điều kiện thanh toán/hoàn tất.

Trang **Sản phẩm** của quản lý hiển thị cả mẫu riêng và mẫu chung đã có trong kho chi nhánh, kể cả bản ghi cũ có `products.is_deleted=NULL`. Số mẫu là tổng của mục đang chọn, không phải số dòng trên một trang; danh sách có phân trang. Các mục: Đang bán, Ngừng bán, Tất cả và Đã xóa. Các thao tác dùng tiếng Việt: Thêm sản phẩm, Sửa, Ngừng bán/Mở bán, Xóa và Khôi phục. Mẫu chung chỉ cho quản lý sửa tồn kho/trạng thái tại chi nhánh đang chọn; tên, giá và thông tin chung do admin quản lý. Không được sửa/xóa mẫu riêng của cửa hàng khác chỉ vì có một dòng kho cũ tại cửa hàng mình.

Xóa tại chi nhánh là **xóa mềm**: giữ số lượng kho, ảnh Cloudinary và lịch sử đơn; không gọi xóa ảnh hoặc xóa đơn. Mẫu chung chỉ bị ẩn ở chi nhánh thực hiện, chi nhánh khác vẫn bán được. Mẫu riêng bị ẩn công khai và được giữ lại trong mục Đã xóa. Không cho xóa khi còn đơn đang xử lý; có thể ngừng bán để chặn đơn mới. Khôi phục về mục Ngừng bán, kiểm tra thông tin/tồn kho rồi chủ động mở bán. Giỏ hàng cũ, báo giá và checkout cũng kiểm tra trạng thái chi nhánh, không cho đặt hàng đã ngừng bán/xóa. V8 chỉ thêm hai cột trạng thái mặc định trên `store_inventories`, không đổi kho, sản phẩm hoặc đơn cũ.

Trang chủ hiển thị số lượng còn lại ở cả hoa bán chạy và danh sách sản phẩm. Số lượng chỉ tính tại chi nhánh hoạt động và mở cửa, hoặc riêng chi nhánh đang lọc. Hết hàng có nhãn rõ ràng và khóa nút thêm vào giỏ; sản phẩm chung toàn chuỗi được chọn chi nhánh có tồn kho thay vì dùng ID cố định.

Sản phẩm riêng của chi nhánh (`product.store` có giá trị) chỉ được đặt và trừ kho ở chi nhánh đó, thống nhất với số lượng trên trang chủ. Giỏ hàng, kiểm tra nhận tại cửa hàng, báo giá và checkout đều kiểm tra quy tắc này; giao tận nơi không chọn kho chi nhánh khác chỉ vì gần địa chỉ hơn. Sản phẩm chung toàn chuỗi (`product.store` rỗng) vẫn được chọn chi nhánh đang mở có đủ hàng. Đơn cũ giữ nguyên chi nhánh đã ghi nhận; khi hủy trước giao, hoàn kho về chính chi nhánh trên đơn.

Nhãn tồn kho trên từng thẻ sản phẩm tự cập nhật mỗi 5 giây, khi quay lại tab hoặc trang được khôi phục bằng nút Back; không hiển thị khối KPI tổng tồn kho trên trang khách hàng. Đơn thành công (kể cả COD) làm giảm số lượng; hủy trước giao hoàn kho và số hiển thị tăng lại. Khi hết hàng nút mua bị khóa; nếu nhập/hoàn kho, nút và chi nhánh còn hàng được cập nhật lại. API công khai chỉ trả số lượng hàng được phép bán, không lấy sản phẩm chưa duyệt/đã xóa/ngừng bán hay kho chi nhánh đóng cửa. Số **mẫu hoa** trong kết quả tìm kiếm/phân trang là chỉ số khác, không giảm khi đặt một vài đơn vị của cùng mẫu.

Stack: Java 21, Spring Boot 4.1.1, Thymeleaf, Bootstrap 5.3.3, JPA/Hibernate, SQL Server, WebSocket/STOMP, JWT và Cloudinary. SQL Server là lựa chọn trong yêu cầu SQLServer/MySQL/PostgreSQL; không cần dùng đồng thời cả ba.

## Phân loại hoa và giá sản phẩm

Mỗi sản phẩm có **một danh mục chính** và tối đa **10 danh mục bổ sung**, chọn bằng checkbox. Danh mục mặc định phân theo dịp/mục đích: Tình yêu, Sinh nhật, Cưới, Khai trương, Tốt nghiệp, Chúc mừng, Chia buồn, Tân gia, Trang trí và Quà tặng. Một mẫu có thể xuất hiện ở nhiều danh mục; bộ lọc danh mục và dịp tặng trên trang khách hàng tìm cả danh mục chính/bổ sung, không lặp sản phẩm hoặc làm sai phân trang. Loại hoa, màu sắc, kiểu dáng, kích cỡ và phong cách chọn riêng trong các nhóm thuộc tính; không cần giữ Ctrl. Thuộc tính không bắt buộc chọn ở mọi nhóm.

Giá bán/giá khuyến mãi là **số tiền bằng đồng nguyên, từ 1đ**, không phải phần trăm giảm; giá khuyến mãi không lớn hơn giá bán. Ví dụ: giá niêm yết 120000, giá khuyến mãi 100000 thì khách trả 100000đ. Để trống giá khuyến mãi nếu không áp dụng. Cả trình duyệt và backend từ chối `0.01`, số âm hoặc giá khuyến mãi quá cao. Giá khuyến mãi không hợp lệ đã lưu trước đó bị bỏ qua khi hiển thị/tính tiền: dùng giá niêm yết, không làm tròn thành 0đ. Đơn cũ vẫn giữ nguyên đơn giá đã ghi nhận.

Flyway V7 thêm bảng danh mục bổ sung và chuyển những danh mục mặc định cũ đang trộn kiểu dáng/loại hoa sang nhóm dịp phù hợp. Nhóm quà tặng/trang trí có dịp rõ trong tên sản phẩm được chuyển theo dịp đó; nếu chưa rõ thì giữ ở Quà tặng/Trang trí để quản lý chọn thêm sau. Danh mục cũ chỉ bị ẩn, không xóa; bảng `product_category_legacy` lưu phân loại trước khi chuyển. Danh mục tự tạo ngoài danh sách mặc định không bị đổi. Không thay sản phẩm, ảnh, giá, tồn kho, đơn hàng hoặc chat ngoài trường phân loại. Sao lưu và thử trên bản sao SQL Server trước khi áp dụng lên database có dữ liệu.

Đăng nhập thất bại giữ đúng email/tên đăng nhập vừa nhập trong phiên trình duyệt, kể cả khi tải lại trang lỗi; không đổi thành tài khoản demo hoặc tài khoản trình duyệt đã lưu. Mật khẩu không được website lưu/điền lại. Khi đăng nhập thành công, đăng xuất hoặc mở trang đăng nhập mới không có lỗi, thông tin lần thử trước được xóa. Trang lỗi phục hồi giá trị đã gửi trước khi người dùng tương tác, không ghi đè khi họ đang sửa hoặc chọn tài khoản demo; giá trị hiển thị được escape để tránh XSS và không đưa vào URL chuyển hướng.

## Báo cáo theo khoảng ngày

Dashboard admin, dashboard cửa hàng và báo cáo doanh thu theo đơn/sản phẩm đều có **Hôm nay**, **7 ngày**, **30 ngày** và **Từ ngày… đến ngày…**. Mặc định là 7 ngày, gồm hôm nay; chọn khoảng tùy chỉnh rồi bấm **Áp dụng**. Đổi tiêu chí, kiểu biểu đồ hoặc chuyển trang vẫn giữ khoảng ngày.

Khoảng ngày bao gồm toàn bộ ngày bắt đầu và ngày kết thúc theo múi giờ ứng dụng UTC+7. Doanh thu/số đơn/sản phẩm bán dùng ngày tạo đơn; người dùng mới dùng ngày đăng ký, cửa hàng/sản phẩm mới dùng ngày tạo. Doanh thu chỉ tính đơn đủ điều kiện ghi nhận, không tính đơn chưa thanh toán hoặc đã hoàn tiền. Số liệu tổng và biểu đồ dùng toàn bộ dữ liệu trong kỳ, không giới hạn 1.000 bản ghi; bảng danh sách/xếp hạng hiển thị tối đa 1.000 bản ghi và có phân trang. Xếp hạng điểm lọc người dùng/cửa hàng tạo trong kỳ nhưng cột điểm là **điểm tích lũy hiện tại**, không phải lịch sử điểm từng ngày.

Khoảng tùy chỉnh trên 90 ngày gom biểu đồ theo tháng, trên 730 ngày gom theo năm; vẫn chỉ tính dữ liệu trong chính xác khoảng đã chọn. Ngày đảo ngược, thiếu ngày hoặc sai định dạng bị từ chối. Các URL lọc giờ/tháng/năm cũ tiếp tục hoạt động.

## Chạy trên Windows

1. Cài JDK 21, SQL Server 2019+ và bật TCP/IP. Dự án có Maven Wrapper nên không cần cài Maven riêng. Bản JDK 21 portable đã được đặt trong `.tools` trên máy hiện tại; thư mục này không được đưa vào Git.
2. Tạo một database rỗng, ví dụ `RentalShop`, và tài khoản database có quyền tạo bảng/index trong schema `dbo` để chạy migration. Dùng tài khoản riêng cho môi trường production.
3. Xem `.env.example`, đặt biến môi trường trong PowerShell hoặc IDE. Spring Boot **không tự đọc** `.env`; riêng `scripts/sqlserver.ps1` tự nạp ba biến Cloudinary như hướng dẫn bên dưới.

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlserver://127.0.0.1;instanceName=SQLEXPRESS;databaseName=RentalShop;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true;'
$env:SPRING_DATASOURCE_USERNAME = 'your_database_user'
$env:SPRING_DATASOURCE_PASSWORD = 'your_local_password'
$jwtBytes = [byte[]]::new(32)
[Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$env:CLOUDINARY_CLOUD_NAME = 'your_cloud_name'
$env:CLOUDINARY_API_KEY = 'your_api_key'
$env:CLOUDINARY_API_SECRET = 'your_api_secret'
# Chỉ bật trên database demo: tạo tài khoản/mật khẩu demo và dữ liệu mẫu.
$env:APP_DEMO_DATA_ENABLED = 'true'
.\scripts\maven.ps1 spring-boot:run
```

Mở `http://localhost:8080`. Các biến môi trường trên chỉ tồn tại trong terminal hiện tại. Lưu khóa JWT một lần ở cấu hình riêng nếu cần giữ token hợp lệ qua lần khởi động tiếp theo. Nếu thiếu khóa trong profile `dev`, hệ thống tạo khóa tạm; production yêu cầu khóa Base64 có ít nhất 32 byte.

### SQL Express trên máy hiện tại

Database đã chọn là `RentalShop` trong `SQLEXPRESS`. Cổng TCP hiện tại là `53735`, không phải `1433`; URL mặc định của profile `dev` dùng cổng này. Nếu SQL Server đổi cổng sau khi khởi động lại, đặt `SPRING_DATASOURCE_URL` theo cổng mới trước khi chạy ứng dụng. Instance `SQLEXPRESS01` chứa database khác, không được sửa.

```powershell
# Đọc trạng thái Flyway, không sửa database:
.\scripts\sqlserver.ps1 -Action info
# Chạy website đúng instance/database/profile:
.\scripts\sqlserver.ps1
```

Script dùng mật khẩu từ biến môi trường, hoặc credential mã hóa Windows DPAPI trong `.tools/sqlserver-local.json` trên máy hiện tại; nếu không có/không giải mã được sẽ hỏi mật khẩu bằng SecureString. File này bị Git ignore và chỉ tài khoản Windows đã mã hóa mới giải mã được. Không sao chép credential vào source. Sau khi script kết thúc, các biến môi trường ban đầu được khôi phục. Các action `baseline`/`migrate` luôn yêu cầu gõ lại chính xác tên database; phải có backup trước khi dùng trên database khác.

Nếu chạy từ IDE thay vì script, chọn profile `dev` và cung cấp `SPRING_DATASOURCE_USERNAME`/`SPRING_DATASOURCE_PASSWORD` trong cấu hình chạy; Spring không đọc được file credential DPAPI của script. `SPRING_DATASOURCE_URL` vẫn có thể ghi đè URL mặc định khi dùng máy/instance khác. Production dùng URL và chứng chỉ của môi trường triển khai, không dùng `trustServerCertificate=true` của local.

### Cloudinary local, không lưu secret trong code

Trong `.env` tại thư mục gốc, điền ba biến dưới đây bằng giá trị thật của tài khoản Cloudinary (không điền vào các file `application*.properties`):

```dotenv
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_new_api_secret
```

Chạy `./scripts/sqlserver.ps1`: script tự nạp đúng ba biến này, không in giá trị ra màn hình, không thực thi nội dung `.env` và không thay cấu hình database/mail từ file này. Biến đã đặt trong terminal được ưu tiên; kết thúc script sẽ khôi phục môi trường ban đầu. Hỗ trợ giá trị trần hoặc bọc trong dấu nháy; đặt chú thích trên dòng riêng, không thêm chú thích sau secret. `CLOUDINARY_URL` không được dùng bởi cấu hình hiện tại; chỉ cần ba biến trên.

`.env` bị Git ignore nhưng **không được mã hóa**; không chia sẻ, chụp ảnh hoặc đưa vào bản nộp công khai. Khi chạy từ IDE hoặc `scripts/maven.ps1` trực tiếp, vẫn phải đặt ba biến trong môi trường của cấu hình chạy. Nếu key/secret đã xuất hiện trong chat, ảnh chụp hoặc Git, hãy thu hồi/thay key trên Cloudinary rồi cập nhật `.env` (và các bản sao như `CLOUDINARY_URL` nếu có). Git ignore không bảo vệ giá trị từng được lưu trong file đã theo dõi. Kiểm tra script nạp `.env` bằng `./scripts/test-cloudinary-env.ps1`; kiểm thử chỉ dùng giá trị giả, không gửi ảnh lên Cloudinary.

Tài khoản demo trên **database rỗng**: `nguyenthanhminh` (Nguyễn Thanh Minh, admin), `nguyenquanghuy` (Nguyễn Quang Huy, quản lý Q1), `tranduclong` (Trần Đức Long, quản lý Q7), `leminhhoa` (Lê Minh Hoa, khách hàng); mật khẩu mẫu `123456`. Họ tên dùng để hiển thị, tên đăng nhập duy nhất dùng để xác thực, chức vụ/vai trò hiển thị riêng. Không bật dữ liệu demo trên production. Nếu database đã có user/store/product, bộ khởi tạo bỏ qua; không tự bổ sung lại sản phẩm đã được người vận hành đổi tên.

Với database demo cũ trên SQLEXPRESS, `scripts/rename-demo-accounts.ps1` mặc định chỉ **Preview** danh sách đổi tên. `-Action Test` thử trong transaction rồi rollback; `-Action Apply` sao lưu/kiểm tra backup trước khi cập nhật RentalShop. Chỉ tài khoản khớp chính xác tên đăng nhập cũ, họ tên mẫu và role mới được đổi; trùng tên đăng nhập mới thì dừng toàn bộ. Không thay email, mật khẩu, quyền, ID, tồn kho, đơn hoặc chat. Sau đổi tên cần đăng xuất/đăng nhập lại bằng tên mới; thông tin người nhận trên đơn/tin nhắn đã gửi không bị viết lại.

Khách vãng lai là phiên **chưa đăng nhập** (anonymous), không cần lưu user hoặc role `GUEST` trong database. Được xem trang chủ, tìm sản phẩm, chi tiết và cửa hàng; giỏ hàng/đặt hàng cần quyền `USER`, chat cần đăng nhập. Quyền `USER` cấp cho tài khoản đăng ký, không cấp cho phiên anonymous. Trang khách hàng không hiển thị tổng tồn kho toàn chuỗi; số lượng từng sản phẩm và nhãn **Hết hàng** vẫn cập nhật mỗi 5 giây.

## Database hiện có và Flyway

Flyway là nơi quản lý schema ở môi trường chạy. Các bản `V1`–`V8` tạo schema và phân loại sản phẩm; `V9` mở rộng phương thức thanh toán demo, `V10` thêm các cột đối soát tiền demo. Database `RentalShop` cục bộ đã lên **V10 ngày 19/10/2026** sau khi sao lưu và thử trên bản sao; môi trường khác vẫn phải kiểm tra `flyway_schema_history` riêng. **Spring Boot tự chạy migration đang chờ khi khởi động**, vì vậy `scripts/sqlserver.ps1 -Action run` chặn riêng `RentalShop` chưa lên V10. File SQL trong `sql/migrations` và `resources/db` là tài liệu lịch sử; không cần chạy chúng lại. Các runner migration cũ chỉ được bật ở profile `test` để giữ kiểm thử H2.

81 dòng kho lệch đã được người dùng xác nhận là dữ liệu nhân bản. Phương án cô lập và lưu giá trị gốc đã đạt trên bản sao SQL Server; `RentalShop` thật **chưa** đưa 2.003 đơn vị nhân bản về 0 vì thao tác này cần phê duyệt riêng. Chi tiết tại [báo cáo đối soát kho](docs/DOI_SOAT_KHO_LECH_CHI_NHANH_20261019.md).

Với **database rỗng**, chạy ứng dụng để Flyway áp dụng từ `V1`. Với **database đã có dữ liệu nhưng chưa có `flyway_schema_history`**, sao lưu và thử trên bản sao trước. Xác nhận URL database và schema `dbo`, sau đó chủ động ghi nhận baseline:

```powershell
.\scripts\maven.ps1 flyway:info
.\scripts\maven.ps1 flyway:baseline
.\scripts\maven.ps1 spring-boot:run
```

Plugin Flyway đọc ba biến `SPRING_DATASOURCE_*` đã đặt ở trên. Baseline ở version 1 không chạy lại schema tạo mới; lần khởi động sau chạy phần nâng cấp `V2`–`V6` trước khi Hibernate đọc entity. Không bật tự baseline để tránh chấp nhận nhầm database. Nếu migration báo dữ liệu trùng/ràng buộc không hợp lệ, cần rà soát dữ liệu được báo; migration không tự xóa lịch sử để vượt lỗi.

`database.sql` là script reset cũ có `DROP TABLE`; không dùng script này để nâng cấp database đang có dữ liệu. `spring.jpa.hibernate.ddl-auto=none`; Hibernate không thay đổi schema khi ứng dụng khởi động.

## Xác thực và phân quyền

Giao diện Thymeleaf và AJAX trong trình duyệt dùng session cùng CSRF. API JWT dùng `Authorization: Bearer <accessToken>` và không lấy quyền từ cookie session. Các request `/api/**` có Bearer header được xử lý qua JWT; request trình duyệt không có Bearer vẫn giữ luồng session hiện có. `/api/auth/me` và `/api/auth/logout` luôn yêu cầu JWT.

| Endpoint | Dữ liệu / hành vi |
|---|---|
| `POST /api/auth/login` | JSON `username`, `password`; username chấp nhận tên đăng nhập hoặc email |
| `POST /api/auth/refresh` | JSON `refreshToken`; xoay refresh token sau mỗi lần dùng |
| `GET /api/auth/me` | Tên đăng nhập và các quyền hiện hành |
| `POST /api/auth/logout` | Thu hồi token family của lần đăng nhập JWT hiện tại |

Access token là JWT HS256, thời hạn 15 phút; refresh token ngẫu nhiên có thời hạn tối đa 7 ngày tính từ lúc đăng nhập. Database chỉ lưu SHA-256 của refresh token. Dùng lại refresh token đã xoay sẽ thu hồi toàn bộ token family. Đăng xuất, khóa/vô hiệu hóa tài khoản và đổi mật khẩu ngăn sử dụng token đã phát hành. JWT được kiểm tra chữ ký, issuer, audience, thời hạn và token type. Quyền được nạp lại từ database mỗi request.

API Bearer hiện dùng được với chat HTTP, thông báo và API tồn kho. Kết nối WebSocket `/ws`, `/ws-chat` của giao diện tiếp tục xác thực bằng session; không truyền JWT trong URL. STOMP kiểm tra quyền gửi/subscribe theo người dùng và chi nhánh. Quyền tổng quát là `ADMIN`, `MANAGER`, `USER`; nhân sự từng cửa hàng được giới hạn thêm bởi ownership/membership.

Chat khách hàng có ba điểm vào: **Tài khoản → Trò chuyện với hệ thống** mở trực tiếp CSKH/admin; nút nổi **Nhắn tin** mở tất cả lịch sử cửa hàng và hệ thống, không tạo/chọn người nhận tự động; **Nhắn tin với cửa hàng** ở chi tiết sản phẩm mở đúng chi nhánh và hiện thẻ sản phẩm (ảnh, tên, giá, liên kết) sẵn để hỏi. Không còn nút mở CSKH trong hộp lịch sử chung. Chỉ khi khách bấm Gửi, backend xác minh lại sản phẩm/chi nhánh và lưu thẻ sản phẩm cùng tin nhắn; cửa hàng nhận thẻ qua WebSocket và xem lại trong lịch sử. Tên/ảnh/giá được chụp tại lúc gửi, không đổi theo lần sửa sản phẩm về sau. Sản phẩm dùng chung phải có chi nhánh bán hợp lệ; liên hệ từ đơn vẫn dùng chi nhánh đã ghi trên đơn và kiểm tra sở hữu. Không tự gửi tin chỉ vì mở chat. Xem [tài liệu tổng hợp](docs/TAI_LIEU_TONG_HOP_DU_AN.md).

Quản lý có **Hộp thư khách hàng** của chi nhánh đang chọn và tab **Hỗ trợ từ admin** riêng; nút **Liên hệ admin về vận hành** mở một hội thoại nội bộ chung cho đội quản lý chi nhánh (`BRANCH_SUPPORT`). Không chat như người mua với chi nhánh khác hoặc trộn hỗ trợ cá nhân. API inbox/history/images được giới hạn theo chi nhánh có quyền. WebSocket bind chi nhánh tại CONNECT, chỉ cho subscribe `/user/queue/chat.store.{storeId}` và `/user/queue/chat.support.{storeId}` tương ứng, kiểm tra lại quyền khi gửi. Tài khoản phụ trách nhiều chi nhánh phải chuyển chi nhánh/mở lại trang để dùng hộp thư tương ứng. Hội thoại cũ được giữ trong database, không xóa lịch sử.

Admin mở **Trung tâm hội thoại** (`/admin/chat`) với ba hộp thư: CSKH khách hàng, Hỗ trợ chi nhánh và Giám sát chỉ đọc. Hai kênh hỗ trợ cho phép trả lời dưới tài khoản admin; chat khách–chi nhánh chỉ được đọc, không gửi văn bản/ảnh, sửa/xóa tin hay giả danh cửa hàng. Bộ lọc dùng ID chi nhánh, tên/tên đăng nhập người liên hệ và ngày hoạt động gần nhất của hội thoại. Danh sách giám sát không hiện nội dung tin cuối; mở/tải lịch sử phải ghi nhật ký admin–hội thoại–thời điểm–mốc tin cũ trước khi trả nội dung và không cache. Không ghi được nhật ký thì từ chối trả lịch sử. Nhật ký có màn hình riêng `/admin/chat/access-logs`; không có chức năng sửa/xóa. Giám sát dùng tải lịch sử có nhật ký, không subscribe queue riêng của cửa hàng. Xem [tài liệu tổng hợp](docs/TAI_LIEU_TONG_HOP_DU_AN.md).

## Profile và dịch vụ ngoài

`dev`: không cache template, cho phép chứng chỉ SQL Server tự ký tại máy local, có thể bật seed demo rõ ràng. `prod`: cache template, cookie session Secure, tắt demo/DevTools và yêu cầu cấu hình Cloudinary. Triển khai production qua HTTPS; URL SQL Server cần chứng chỉ hợp lệ. Đặt giới hạn request cho endpoint đăng nhập tại reverse proxy khi public website.

OTP mặc định gửi qua Brevo API (`MAIL_PROVIDER=brevo`), không dùng Gmail App Password. Cấu hình người gửi bằng `MAIL_FROM_EMAIL` và `BREVO_API_KEY`; người nhận lấy từ yêu cầu đăng ký/email của tài khoản, không cố định trong code. Chỉ khi chủ động chọn `MAIL_PROVIDER=smtp` mới dùng `SPRING_MAIL_USERNAME`/`SPRING_MAIL_PASSWORD`. MoMo mặc định chạy mô phỏng, không thu tiền thật. Cloudinary sử dụng ba biến `CLOUDINARY_*`. Profile dev có thể lưu ảnh hồ sơ/sản phẩm cục bộ khi chưa cấu hình Cloudinary; gửi ảnh chat yêu cầu Cloudinary. Không còn credential thật trong giá trị mặc định. Các khóa Cloudinary/database từng nằm trong Git cần được thay tại nhà cung cấp/database; việc sửa file hiện tại không xóa lịch sử Git.

OTP/API key không được in ra terminal/log. Ngoài kiểm thử, thiếu cấu hình mail hoặc gửi email thất bại sẽ báo lỗi, không báo thành công giả. Đăng ký gửi mail thất bại được giải phóng để thử lại; gửi lại OTP thất bại giữ mã cũ nếu còn hạn. Profile `test` bật `app.mail.mock-enabled=true` để test không gửi email ra ngoài; production ép tắt mô phỏng mail.

### Cấu hình OTP một lần, không điền App Password vào code

1. Tạo tài khoản Brevo, thiết lập/xác minh địa chỉ hoặc domain gửi theo [hướng dẫn gửi transactional email](https://developers.brevo.com/docs/send-a-transactional-email).
2. Tạo **API key** (không phải SMTP key) theo [hướng dẫn chính thức](https://developers.brevo.com/docs/api-key-authentication).
3. Trên Windows chạy `./scripts/configure-mail.ps1`: nhập email người gửi đã xác minh và API key tại dòng nhập được che. Script lưu key mã hóa Windows DPAPI vào `.tools/mail-local.json`, bị Git ignore; không sửa mã nguồn hay lưu key rõ.
4. Chạy `./scripts/sqlserver.ps1`. Script tự nạp cấu hình mail local khi chưa có biến môi trường ghi đè. Sau đó người dùng đăng ký bằng email của họ, website tự gửi OTP tới email đó; không cần cấu hình lại cho mỗi người nhận.

Nếu chạy từ IDE/server khác, đặt `BREVO_API_KEY`, `MAIL_FROM_EMAIL` và tùy chọn `MAIL_FROM_NAME` trong cấu hình chạy/secret manager. Sender cần được nhà cung cấp chấp nhận, nhưng không bắt khách cung cấp mật khẩu hoặc xác minh email của họ tại Brevo trước khi nhận OTP. Khôi phục/đổi mật khẩu chỉ gửi tới email gắn với tài khoản, không được đổi sang email của người khác trong request.

Hiện chưa có API key/người gửi được cấu hình tại máy kiểm tra, nên đã kiểm chứng payload/luồng/lỗi bằng mock, **chưa xác nhận thư đã tới hộp thư thật**. API trả 201 + messageId nghĩa là đã tiếp nhận gửi; giao thư còn phụ thuộc mailbox tồn tại, spam, giới hạn/tình trạng tài khoản dịch vụ. Không thể gửi mail thật nếu hoàn toàn không cấu hình một dịch vụ gửi có quyền xác thực.

`open-in-view=true` được giữ có chủ đích vì nhiều template hiện truy cập quan hệ JPA lazy khi render. Tắt ngay sẽ gây lỗi màn hình; muốn tắt cần chuyển các màn hình này sang DTO/projection. JWT authority/token service có transaction riêng cho truy cập dữ liệu xác thực.

## Kiểm thử

```powershell
.\scripts\maven.ps1 test
.\scripts\test-js.ps1
.\scripts\maven.ps1 -DskipTests package
```

Java test dùng H2 ở chế độ SQL Server, không ghi database SQL Server đang có. Test WebSocket thực tế cần được phép mở kết nối localhost. Mockito được nạp bằng Java agent để tránh tự attach bị treo. JavaScript test cần Node.js 22; trên máy hiện tại đã có runtime portable trong `.tools`.

Kết quả ngày 19/10/2026: Java 622 ca (616 đạt, 6 bỏ qua có chủ đích, không thất bại/lỗi); JavaScript 102/102 ca đạt. Kiểm tra sổ số dư SQL Server V10 chỉ đọc đạt khi chạy riêng; test Cloudinary live không nằm trong lượt chạy mặc định.

Phí giao hàng demo dùng **vùng do người mua chọn** (gần/xa/ngoài phạm vi) để đối chiếu biểu phí của chi nhánh; hệ thống không đo khoảng cách bản đồ và không suy từ chuỗi địa chỉ như “Quận 1”. Không thể đặt hàng khi chọn ngoài phạm vi hoặc khi không có biểu phí phù hợp. Đối soát tồn kho cũ: [báo cáo 19/10/2026](docs/DOI_SOAT_KHO_LECH_CHI_NHANH_20261019.md).

Script Maven tương thích Windows PowerShell 5.1 và PowerShell 7: kiểm tra `java -version` bằng process stream riêng để không coi stderr chứa thông tin phiên bản là `NativeCommandError`. Nếu Java mặc định là 26, script bỏ qua và chọn JDK 21 portable của dự án; không cần gỡ Java 26. Xem toolchain bằng `./scripts/maven.ps1 -MavenArguments @('--version')` (không dùng `-v`, có thể bị PowerShell hiểu là Verbose).

Test Cloudinary live chỉ chạy khi chủ động đặt `-Dcloudinary.live=true` và cung cấp credential thật; test tạo và xóa riêng ảnh kiểm thử. Trước khi nâng cấp SQL Server trên máy khác vẫn cần sao lưu, thử bản sao và kiểm tra dữ liệu sau migration; không suy từ phiên bản trong mã nguồn rằng database của máy đó đã được nâng cấp.

Đối chiếu yêu cầu, các thay đổi và kết quả kiểm thử: [báo cáo kỹ thuật](docs/PROJECT_REVIEW_20261016.md).

Đánh giá chạy thử và các việc còn lại: [báo cáo hệ thống](docs/PROJECT_REVIEW_20261016.md).

Thiết lập SQL Server và chạy ứng dụng: [hướng dẫn cài đặt](docs/HUONG_DAN_SETUP_VA_DANH_SACH_PROMPT_AI.md).

OTP gửi đúng người nhận, không điền Gmail App Password vào code: xem `scripts/configure-mail.ps1` và [tài liệu tổng hợp](docs/TAI_LIEU_TONG_HOP_DU_AN.md).
