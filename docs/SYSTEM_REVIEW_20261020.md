# Báo cáo kiểm tra hệ thống — 20/10/2026

Phạm vi: mã nguồn tại commit `8fbad02`, cấu hình trong workspace, test Java/JavaScript, và truy vấn chỉ đọc database `RentalShop` tại SQL Server cục bộ `127.0.0.1:1433`. Số liệu database được lấy khoảng 20:45–20:48, UTC+7. File `start-web.ps1` đã tồn tại ở trạng thái untracked trước khi kiểm tra và được giữ nguyên.

## 1. Kết luận tổng quan

Hệ thống là **website bán hoa đa chi nhánh**, dù tên thư mục còn là Rental-Retail-Shop. Có ba nhóm chức năng chính: khách hàng, quản lý chi nhánh và quản trị viên. Kiến trúc phân lớp, xử lý giao dịch và bộ test là những nền tảng tốt.

Tuy nhiên, **chưa nên coi hệ thống sẵn sàng triển khai bán hàng thật**. Đợt kiểm tra phát hiện ba vấn đề ưu tiên cao về thanh toán giả lập, XSS trong giỏ hàng và phiên đăng nhập sau đổi mật khẩu; thêm vấn đề dữ liệu nhạy cảm trong thông báo WebSocket và sự không đồng nhất giữa database hiện tại với mã nguồn/tài liệu.

Báo cáo phân biệt ba loại bằng chứng: kết quả test tự động, tái hiện cô lập bằng mã thật với dữ liệu giả, và số liệu SELECT từ SQL Server. Không suy kết quả test trên H2 thành kết quả nghiệm thu toàn bộ database thật.

## 2. Cấu trúc và các phân hệ

| Thành phần | Hiện trạng | Nhận xét |
| --- | --- | --- |
| Backend | Java 21, Spring Boot 4.1.1 | Controller → Service → Repository; có DTO, entity, enum và xử lý lỗi chung |
| Giao diện | Thymeleaf, Bootstrap, JavaScript | Trang khách hàng, manager, admin; nhiều đoạn JavaScript còn nằm trong HTML |
| Dữ liệu | SQL Server, JPA/Hibernate, Flyway | Migration SQL và Java; `ddl-auto=none` trong cấu hình chính |
| Xác thực web | Session, BCrypt, Spring Security, CSRF | Có kiểm tra lại trạng thái/quyền tài khoản ở mỗi request |
| API | JWT access/refresh, phiên token trong DB | Có xoay refresh token, phát hiện dùng lại và kiểm tra hash thông tin xác thực |
| Realtime | WebSocket/STOMP/SockJS | Có kiểm tra quyền subscribe/send theo người dùng và chi nhánh |
| Tích hợp | Cloudinary, email OTP, MoMo mock/sandbox | Chưa xác nhận dịch vụ bên ngoài bằng giao dịch thật trong đợt này |

Quy mô nguồn hiện tại: 246 file Java chính, 77 template HTML, 73 file test Java và 13 file test JavaScript. Số file thể hiện quy mô kiểm tra, không thể hiện tỷ lệ bao phủ.

Các nhóm nghiệp vụ đã rà soát:

- Khách hàng: đăng ký/OTP, đăng nhập, hồ sơ/địa chỉ, danh mục/sản phẩm, giỏ hàng, báo giá, checkout, đơn hàng, thanh toán, hủy/hoàn tiền, đánh giá, theo dõi và chat.
- Quản lý: chọn chi nhánh, sản phẩm và kho, đơn hàng, thông tin cửa hàng, nhân sự, ví/rút tiền và báo cáo.
- Admin: người dùng, cửa hàng, sản phẩm, đơn, danh mục/thuộc tính, cấp độ, hoa hồng, vận chuyển, voucher, giao dịch, chat giám sát và thống kê.

## 3. Kết quả kiểm thử lần này

| Hạng mục | Kết quả | Giới hạn |
| --- | --- | --- |
| Java toàn bộ | **622 ca: 616 đạt, 0 thất bại, 0 lỗi, 6 bỏ qua; BUILD SUCCESS** | Profile test/H2; gồm HTTP smoke và WebSocket session integration với máy chủ localhost |
| JavaScript | **102/102 đạt**, không bỏ qua | Bao gồm gateway/tunnel demo và các hành vi JavaScript được đặc tả trong test |
| Cấu hình Cloudinary cục bộ | **10/10 kiểm tra đạt** | Credential và Maven giả; không xác nhận upload vào tài khoản Cloudinary thật |
| Tái hiện phiên cũ | Xác nhận vấn đề | Gọi `UserServiceImpl.changePassword` rồi chạy `AccountStatusFilter`; repository và servlet giả lập |
| Tái hiện MoMo prod/mock | Xác nhận vấn đề | Đọc cấu hình prod thật, gọi service thật với repository giả; đơn chuyển PAID |
| Tái hiện dữ liệu thông báo | Xác nhận vấn đề | Converter JSON của Spring serialize cả `user.password` và `user.otpCode` giả |
| Kiểm tra đường đi XSS | Xác nhận đầu vào HTML đi nguyên vẹn vào `innerHTML` | Chạy hàm JavaScript thật với DOM stub; chưa thực thi payload trên trình duyệt |
| SQL Server | Kết nối và truy vấn chỉ đọc thành công | Không chạy migration, seed, sửa kho, hủy/hoàn đơn hay thay dữ liệu thật |

Lệnh chính:

```powershell
.\scripts\maven.ps1 -MavenArguments @('test','-B')
.\scripts\test-js.ps1
.\scripts\test-cloudinary-env.ps1
.\scripts\audit-branch-inventory.ps1 -Server '127.0.0.1,1433'
```

Java mặc định trên PATH là **Java 8**, trong khi ứng dụng cần Java 21. Script `scripts/maven.ps1` chọn được `.tools/jdk-21`; đây là cách đã dùng để kiểm tra.

Lần chạy Java hoàn tất lúc **20:50:24 UTC+7**, mất 6 phút 32 giây. Sáu ca bỏ qua gồm: Cloudinary live (1), audit dòng tiền SQL Server live (1), catalog manager SQL Server live (1), migration catalog live (1), migration SQL Server live (2). Đây là các phép thử cần bật riêng/tài nguyên ngoài; không tính là đã đạt. Các phép SELECT SQL Server trong báo cáo được thực hiện riêng, không thay thế sáu test này.

Bộ test hiện có đạt không phủ định F01–F07: các trường hợp mới về profile prod/mock, phiên cũ và JSON nhạy cảm được xác minh riêng bằng harness trong `target`, chưa được thêm vào bộ test hồi quy của dự án.

## 4. Phát hiện theo mức ưu tiên

P1: ưu tiên sửa trước khi triển khai thực tế. P2: cần xử lý trước nghiệm thu đầy đủ hoặc khi vận hành với dữ liệu thật. Mức ưu tiên xét hậu quả trong môi trường thực tế; các chức năng giả lập vẫn có mục đích hợp lệ trong bản demo.

### F01 — P1: Production vẫn có thể xác nhận thanh toán MoMo giả lập

**Bằng chứng:** [application.properties:74](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/resources/application.properties:74), [MomoServiceImpl.java:142](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/java/com/finalproject/service/impl/MomoServiceImpl.java:142), [CheckoutController.java:161](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/java/com/finalproject/controller/customer/CheckoutController.java:161).

- `momo.mode` mặc định là `mock`; cấu hình prod không ghi đè giá trị này.
- Endpoint `POST /checkout/momo/{orderId}/simulate` kiểm tra chủ đơn; service chỉ yêu cầu mode mock, không chặn profile prod.
- UI checkout trong prod chỉ liệt kê COD, nhưng backend vẫn chấp nhận `paymentMethod=MOMO` nếu gửi request trực tiếp. Luồng kiểm tra ở backend chỉ chặn `MOMO_DEMO` khi demo bị tắt.
- Tái hiện cô lập: khi active profile là prod và không cấu hình MOMO_MODE, mode vẫn resolve thành mock; service chuyển order sang PAID và payment sang SUCCESS.

**Ảnh hưởng:** khách hàng có phiên hợp lệ và CSRF token có thể xác nhận thanh toán thử nghiệm cho đơn của chính mình. Điều này có thể làm sai việc giao hàng, ghi nhận doanh thu và ví nếu môi trường đó được sử dụng bán hàng thật. CSRF/chủ đơn không ngăn được khách tự gọi chức năng cho đơn mình.

**Đề xuất:** chặn tất cả luồng giả lập theo profile ở service; áp dụng danh sách phương thức được phép ở backend checkout. Cấu hình prod phải fail fast hoặc vô hiệu hóa MoMo khi chưa có chế độ thanh toán được phê duyệt. MoMo hiện chỉ hỗ trợ mock/sandbox, chưa có chế độ live.

### F02 — P1: Tên cửa hàng được chèn vào innerHTML trong giỏ hàng

**Bằng chứng:** [cart.html:403](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/resources/templates/customer/cart.html:403), [cart.html:680](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/resources/templates/customer/cart.html:680), [ManagerStoreController.java:108](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/java/com/finalproject/controller/manager/ManagerStoreController.java:108).

Tên chi nhánh được lưu với kiểm tra độ dài, đi qua thuộc tính `data-store`, rồi `getAttribute()` lấy lại chuỗi và nối vào `innerHTML` của modal xóa giỏ. Escape HTML ban đầu của Thymeleaf không bảo vệ lần diễn giải HTML thứ hai này.

**Điều kiện:** người có quyền sửa tên cửa hàng đặt tên chứa HTML có event handler; khách có hàng của chi nhánh đó và mở xác nhận xóa. Chuỗi thử nghiệm `<img src=x onerror="alert(1)">` đi nguyên vẹn vào sink trong kiểm tra cô lập. Chưa chạy payload trên trình duyệt thật hoặc ghi tên độc hại vào database.

**Ảnh hưởng:** đường đi cho stored/DOM XSS trong phiên khách mua hàng. Cookie HttpOnly không ngăn mã cùng nguồn gửi request bằng phiên nạn nhân.

**Đề xuất:** tạo icon riêng rồi gán tên bằng `textContent`/text node; rà soát các chỗ tương tự có dữ liệu động. Thêm kiểm tra đầu ra để bảo đảm tên có ký tự HTML được hiển thị như văn bản.

### F03 — P1: Đổi hoặc khôi phục mật khẩu chưa thu hồi phiên web cũ

**Bằng chứng:** [UserServiceImpl.java:95](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/java/com/finalproject/service/impl/UserServiceImpl.java:95), [AuthServiceImpl.java:292](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/java/com/finalproject/service/impl/AuthServiceImpl.java:292), [AccountStatusFilter.java:24](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/java/com/finalproject/security/AccountStatusFilter.java:24).

Service đổi/reset mật khẩu cập nhật hash trong DB. Filter của session chỉ kiểm tra active/khóa/ngừng vai trò và làm mới quyền; không kiểm tra credential version hoặc thu hồi các phiên đã tạo trước đó.

**Tái hiện:** tạo Authentication/session theo mật khẩu cũ, gọi service đổi mật khẩu thành công, sau đó request qua filter vẫn tiếp tục và session vẫn authenticated. Kiểm tra dùng mã thật với repository giả; không phải phép thử hai trình duyệt trên SQL Server.

**Ảnh hưởng:** phiên bị lấy cắp vẫn có thể tồn tại sau khi chủ tài khoản đổi/reset mật khẩu. JWT đã có kiểm tra credential hash; phần session cần cơ chế tương ứng.

**Đề xuất:** lưu phiên bản thông tin xác thực và đối chiếu ở mỗi request, hoặc quản lý/thu hồi toàn bộ session của tài khoản khi mật khẩu thay đổi. Kiểm tra thêm tình huống đổi mật khẩu từ thiết bị khác và reset qua OTP.

### F04 — P2: WebSocket thông báo serialize entity chứa dữ liệu xác thực

**Bằng chứng:** [NotificationServiceImpl.java:45](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/java/com/finalproject/service/impl/NotificationServiceImpl.java:45), [Notification.java:17](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/java/com/finalproject/entity/Notification.java:17), [User.java:20](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/java/com/finalproject/entity/User.java:20).

Thông báo được đẩy bằng entity Notification, entity này có quan hệ `user`. User có getter cho password hash và trường otpCode cũ, không có quy tắc bỏ qua JSON tương ứng. Converter JSON mặc định của Spring đã serialize được cả hai trường trên dữ liệu giả.

**Ảnh hưởng:** payload realtime có thể chứa password hash, trường OTP cũ nếu có giá trị, cùng dữ liệu tài khoản không cần cho chuông thông báo. Quyền subscribe có giới hạn người dùng/admin, nên đợt kiểm tra này không kết luận khách bất kỳ đọc được hash người khác. REST `/api/notifications/latest` đã dùng map giới hạn trường và không có cùng đường đi này.

**Đề xuất:** dùng DTO thông báo tối thiểu giống REST, không đưa entity User vào payload; bảo vệ các trường nhạy cảm khỏi serialization như lớp phòng vệ bổ sung. Test phải kiểm tra nội dung JSON, không chỉ kiểm tra broker được gọi.

### F05 — P2: Database có 15 đơn PAID thiếu chứng từ thanh toán thành công khớp tiền

**Bằng chứng SQL:** tổng 18 đơn và 1 payment; 15 đơn PAID không có payment SUCCESS cùng số tiền. Trong đó có **2 đơn REFUND_REQUESTED** thiếu chứng từ như trên.

**Đối chiếu code:** [DemoOrderSeeder.java:240](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/java/com/finalproject/service/DemoOrderSeeder.java:240) tạo order/detail theo trạng thái PAID nhưng không tạo payment trong hàm này. Đây là một nguồn có thể tạo dữ liệu demo thiếu đồng bộ; không khẳng định mọi dòng hiện tại đều do seeder này tạo.

**Ảnh hưởng:** báo cáo có thể nhận trạng thái PAID trong khi sổ thanh toán không đối chiếu được; hàm [hoàn tiền:360](D:/uni/hk1nam3/web/Rental-Retail-Shop/src/main/java/com/finalproject/service/OrderWorkflowService.java:360) từ chối khi không có payment SUCCESS. Với hai đơn đang chờ hoàn nói trên, đây là trở ngại có thể xác định từ dữ liệu và điều kiện code, chưa thực hiện hoàn tiền thật.

**Đề xuất:** sửa fixture để tạo đủ payment/giao dịch liên quan; tách rõ dữ liệu mẫu và dữ liệu vận hành. Đối soát từng đơn cũ trước khi bổ sung dữ liệu, không tự tạo chứng từ thanh toán thật từ một cờ PAID.

### F06 — P2: Còn 22 dòng kho không thuộc chi nhánh sở hữu sản phẩm

**Bằng chứng SQL:** 68 dòng kho tổng cộng 1.225 đơn vị; **22 dòng/212 đơn vị**, liên quan 11 sản phẩm, có `inventory.store_id != product.store_id` khi sản phẩm có chi nhánh sở hữu. Không tìm thấy đơn liên kết các cặp lệch này theo truy vấn audit. Không có kho âm hoặc cặp store/product trùng khóa nghiệp vụ.

**Ảnh hưởng:** dữ liệu kho thô và tồn kho nghiệp vụ khác nhau. Tổng KPI theo điều kiện code hiện tại là **1.013**, không phải 1.225. Các truy vấn catalog/KPI đã loại kho lệch chi nhánh; không có bằng chứng các dòng này đang được cho phép bán sai chi nhánh qua luồng đã rà soát.

**Đề xuất:** xác định nguồn và ý nghĩa của 22 dòng, đối chiếu hàng thực tế, sao lưu rồi chọn cách xử lý có audit. Không lấy quyết định xử lý kho ngày trước áp dụng tự động cho database/máy hiện tại.

### F07 — P2: Database đã có migration V11 nhưng repository chỉ có đến V10

Database ghi V11 `review image public id` thành công. Trong các vị trí migration cấu hình của source và `target/classes`, không có V11 tương ứng; Java migration cao nhất là V10 và SQL migration cao nhất là V9.

**Ảnh hưởng:** không thể tái dựng chính xác schema hiện tại chỉ từ checkout này; khó kiểm tra tương thích và trao đổi database giữa các máy. Báo cáo cũ mô tả V10 cũng không còn là trạng thái hiện tại.

**Giới hạn kết luận:** chưa chạy Flyway validate hay khởi động ứng dụng trên database thật. Không kết luận riêng migration tương lai này chắc chắn làm ứng dụng không khởi động; cách xử lý còn phụ thuộc cấu hình Flyway.

**Đề xuất:** tìm lại migration V11 nguyên bản và thay đổi code đi kèm, đối chiếu checksum/lịch sử với người sở hữu thay đổi; kiểm tra trên database bản sao. Không xóa dòng lịch sử, repair hay tạo một V11 khác theo phỏng đoán.

## 5. Snapshot dữ liệu hiện tại

| Chỉ số | Kết quả |
| --- | ---: |
| Người dùng | 11 |
| Cửa hàng | 7 |
| Sản phẩm | 46 |
| Đơn hàng | 18 |
| Payment | 1 |
| Dòng kho / tổng số lượng thô | 68 / 1.225 |
| Dòng kho lệch chi nhánh / số lượng | 22 / 212 |
| Tổng tồn kho hợp lệ theo điều kiện KPI | 1.013 |
| Kho âm / ví âm / pending balance âm | 0 / 0 / 0 |
| Nhóm store-product trùng / nhóm payment-order trùng | 0 / 0 |
| Giá sản phẩm không hợp lệ theo kiểm tra SQL về giá <= 0 / giá khuyến mãi > giá bán | 0 |
| Foreign key bị disabled hoặc không trusted | 0 |
| Nhóm trùng tên chi nhánh | 3 nhóm, mỗi nhóm 2 bản ghi |

Trùng tên cửa hàng là dấu hiệu cần đối chiếu dữ liệu demo, không tự nó chứng minh hai bản ghi phải gộp/xóa. Kiểm tra giá SQL ở bảng này không phải phép kiểm tra toàn bộ quy tắc số đồng nguyên.

## 6. Các điểm làm tốt và hạn chế kỹ thuật

Các biện pháp tốt đã nhìn thấy trong code:

- Checkout có transaction; khóa người mua, giỏ và kho; khóa kho theo thứ tự product ID. Giá và tồn kho được kiểm tra lại ở backend.
- Luồng chuyển trạng thái đơn được tập trung trong OrderWorkflowService; hạn chế khách hủy từ PREPARING; kiểm tra sở hữu đơn và quyền chi nhánh.
- Các thao tác ví có khóa, kiểm tra số dư và request key để chống thực hiện lặp. Dòng tiền MOMO_DEMO có trạng thái tiền giữ/chốt/hoàn riêng.
- JWT kiểm tra phiên token và hash mật khẩu, có xoay refresh token; phân quyền API và web được tách theo kiểu xác thực.
- WebSocket chặn gửi giả vào topic và kiểm tra subscribe theo user/chi nhánh; chat/notification có publish sau commit trong các luồng đã xem.
- Upload có giới hạn định dạng/kích thước và dọn ảnh theo kết quả transaction. Prod yêu cầu cấu hình Cloudinary.
- Flyway được dùng làm nguồn thay đổi schema chính; mặc định cấu hình chính không cho clean và không tự baseline database không rõ nguồn gốc.

Những cải tiến tiếp theo, chưa có bằng chứng tải thực tế để định lượng:

- `UserPublicController` lấy toàn bộ danh sách rồi cắt trang trong RAM; `size` không có giới hạn trên. Analytics cũng có nhiều chỗ tải danh sách entity để tính trong Java. Nên chuyển sang Pageable/projection/aggregate ở DB và giới hạn đầu vào.
- OSIV đang bật vì template truy cập quan hệ lazy. Cần đo truy vấn trên trang thật trước khi tắt; ưu tiên DTO và tránh N+1.
- Pending registration giữ nguyên RegisterRequest, gồm mật khẩu chưa hash, trong map RAM đến bước xác thực. Các hàm đăng ký/gửi lại OTP còn `synchronized` và gọi email trong khóa; nên hash sớm, dùng TTL rõ ràng và thu hẹp phạm vi khóa.
- OTP/voucher có chỗ dùng thời gian mặc định máy chủ trong khi báo cáo dùng Clock UTC+7. Nên thống nhất Clock để tránh sai hạn khi đổi timezone triển khai.
- Các script khởi động có giả định khác nhau: dev mặc định SQL cổng 53735; script sqlserver mặc định SQLEXPRESS; máy hiện tại chạy instance MSSQLSERVER ở 1433, SQLBrowser dừng. Cần hướng dẫn một cấu hình khởi chạy khớp môi trường.
- Hướng dẫn kiểm thử vẫn giữ nhiều phần Shipper/VNPay và mật khẩu demo lịch sử sau ghi chú cảnh báo; báo cáo cũ chứa đường dẫn tuyệt đối của máy khác. Nên tách tài liệu hiện hành khỏi tài liệu lưu trữ.
- Chưa thấy cấu hình CI trong `.github` của checkout này. Nên tự động chạy hai bộ test để tránh phụ thuộc kết quả chạy tay.

## 7. Giới hạn và thứ tự xử lý đề nghị

Chưa kiểm tra thủ công toàn bộ màn hình trên trình duyệt/điện thoại, chưa đo tải, chưa quét CVE/thư viện hoặc lịch sử bí mật Git, chưa xác nhận email/Cloudinary/MoMo bên ngoài bằng tài khoản thật. Không mở browser hay truy cập link bên ngoài theo quy tắc workspace. Không khởi động app trên RentalShop để tránh làm migration hoặc chạy tác vụ lịch trên dữ liệu hiện tại.

Thứ tự đề nghị:

1. Sửa F01–F03 và DTO thông báo F04; thêm kiểm tra hồi quy đúng tình huống đã tìm thấy.
2. Đồng bộ migration F07 và đối soát chứng từ F05 trên bản sao database trước.
3. Xác minh 22 dòng kho F06, cập nhật fixture và tài liệu theo database/máy thực tế.
4. Chạy nghiệm thu theo vai trò trên bản sao SQL Server: COD, MoMo demo, đặt/hủy cùng lúc, chuẩn bị đơn, hoàn tiền, rút tiền, chat đúng/sai chi nhánh, upload và báo cáo khoảng ngày.

## 8. Bằng chứng cục bộ

Các file dưới `target` là artifact kiểm tra, bị bỏ qua bởi Git và sẽ mất nếu chạy Maven clean:

- [Log Java](D:/uni/hk1nam3/web/Rental-Retail-Shop/target/review-20261020-java.log), [log JavaScript](D:/uni/hk1nam3/web/Rental-Retail-Shop/target/review-20261020-js.log), [kiểm tra cấu hình Cloudinary](D:/uni/hk1nam3/web/Rental-Retail-Shop/target/review-20261020-cloudinary.log).
- [Snapshot SQL tổng hợp](D:/uni/hk1nam3/web/Rental-Retail-Shop/target/review-20261020-db.json), [audit kho](D:/uni/hk1nam3/web/Rental-Retail-Shop/target/review-20261020-inventory-1433.json).
- [Tái hiện session](D:/uni/hk1nam3/web/Rental-Retail-Shop/target/review-20261020-session.log), [MoMo](D:/uni/hk1nam3/web/Rental-Retail-Shop/target/review-20261020-momo.log), [serialization thông báo](D:/uni/hk1nam3/web/Rental-Retail-Shop/target/review-20261020-notification.log), [đường đi XSS](D:/uni/hk1nam3/web/Rental-Retail-Shop/target/review-20261020-xss.log).

Đợt kiểm tra không sửa mã nguồn nghiệp vụ hoặc database, không commit và không push. Chỉ thêm báo cáo cùng artifact kiểm tra cục bộ.
