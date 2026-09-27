# HƯỚNG DẪN CHI TIẾT KIỂM THỬ TOÀN BỘ CHỨC NĂNG HỆ THỐNG
## Chuỗi Cửa Hàng Hoa Tươi & Điện Hoa Sài Gòn (Bloom Flower Chain)
### *"Nở Trọn Yêu Thương, Thắm Trọn Khoảnh Khắc"*

Tài liệu này hướng dẫn chi tiết từng bước (Step-by-Step) cách kiểm tra và nghiệm thu toàn bộ các chức năng của hệ thống trên giao diện web thực tế (Chrome / Edge / Firefox) theo đúng Đề cương đặc tả yêu cầu chức năng hệ thống bán hoa tươi (100% mô hình Bán hoa - Không cho thuê).

---

## 1. THÔNG TIN KHỞI ĐỘNG & TÀI KHOẢN ĐĂNG NHẬP

- **Địa chỉ trang web**: [http://localhost:8080](http://localhost:8080)
- **Mật khẩu chung cho tất cả tài khoản**: `123456`

### Danh sách tài khoản thử nghiệm theo các vai trò:

| Vai Trò (Actor / Role) | Tên Đăng Nhập | Mật Khẩu | Trang Đích Tự Động Khi Đăng Nhập |
|:---|:---|:---:|:---|
| **1. Khách vãng lai (Guest)** | *(Không cần đăng nhập)* | - | [http://localhost:8080/](http://localhost:8080/) |
| **2. Người mua (Customer)** | `khachhang` | `123456` | [http://localhost:8080/](http://localhost:8080/) |
| **3. Người bán (Vendor / Manager)** | `manager_q1` | `123456` | [http://localhost:8080/manager/dashboard](http://localhost:8080/manager/dashboard) |
| **4. Quản trị viên (Admin)** | `admin` | `123456` | [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard) |
| **5. Nhân viên giao hàng (Shipper)** | `shipper_nhanh` | `123456` | [http://localhost:8080/shipper/orders](http://localhost:8080/shipper/orders) |

---

## 2. KỊCH BẢN KIỂM THỬ CHI TIẾT THEO TỪNG VAI TRÒ

---

### PHẦN 3.1.1: PHÍA KHÁCH VÃNG LAI (GUEST)

Khách vãng lai là người dùng truy cập website khi chưa đăng nhập.

| STT | Chức Năng | Mô Tả | Đường Dẫn / Cách Kiểm Thử |
|:---:|:---|:---|:---|
| **1** | **Đăng ký (UC_001 - Sign up)** | Đăng ký tạo tài khoản người dùng | **Main Flow**:<br>1. Vào hệ thống website [http://localhost:8080/](http://localhost:8080/)<br>2. Chọn nút **Sign in** (Đăng Nhập) trên header<br>3. Chọn chức năng **Sign up** (Đăng ký ngay)<br>4. Nhập đầy đủ thông tin tài khoản (Tên đăng nhập, Mật khẩu, Họ tên, Email, SĐT, Địa chỉ)<br>5. Bấm vào nút **Sign up**<br>6. Hiển thị hộp thoại xác nhận thông tin (Confirm Modal)<br>7. Bấm nút **Confirm**<br>8. Thông báo đăng ký thành công (chuyển sang bước xác thực OTP kích hoạt tài khoản)<br><br>**Alternative Flow**:<br>7a. Bấm nút **Cancel**<br>8a. Hủy thao tác (đóng modal xác nhận, giữ nguyên form đã nhập)<br><br>**Exception Flow**:<br>8b. Thông tin tài khoản không hợp lệ, email hoặc số điện thoại đã tồn tại<br>8b1. Hiển thị thông báo đăng ký thất bại màu đỏ rõ ràng. |
| **2** | **Tìm kiếm/Xem sản phẩm (UC_005 - Search/View products)** | Tìm kiếm, xem danh sách, chi tiết sản phẩm hoa | **Main Flow**:<br>1. Vào hệ thống website [http://localhost:8080/](http://localhost:8080/)<br>2. Chọn chức năng tìm kiếm sản phẩm trên thanh tìm kiếm (header search bar)<br>3. Nhập từ khóa (ví dụ: `Hồng`, `Tulip`, `Hướng Dương`)<br>4. Nhấn Enter hoặc bấm nút hình kính lúp (Tìm Kiếm)<br>5. Chuyển đến trang tìm kiếm sản phẩm [http://localhost:8080/products](http://localhost:8080/products), hiển thị danh sách sản phẩm theo từ khóa<br>6. Bấm nút **All filters (Tất cả bộ lọc)**: Mở panel bộ lọc, thay đổi sắp xếp giá (Thấp &rarr; Cao, Cao &rarr; Thấp), danh mục tùy ý, danh sách sản phẩm được tự động cập nhật<br>7. Chọn nút **Next (Sau)** hoặc **Previous (Trước)** hoặc số trang bất kỳ để chuyển trang phân trang<br>8. Chọn vào một sản phẩm bất kỳ, hệ thống chuyển đến trang chi tiết sản phẩm tương ứng ([http://localhost:8080/products/1](http://localhost:8080/products/1)).<br><br>**Alternative Flow (Theo Danh Mục / Discover)**:<br>1a. Vào trang chủ [http://localhost:8080/](http://localhost:8080/)<br>2a. Tại mục **Khám Phá Danh Mục (Discover)**, chọn category muốn xem<br>3-5a. Chuyển đến trang sản phẩm theo category, hiển thị các category con / phong cách và danh sách hoa thuộc category đã chọn<br>3-5b. Tiếp tục bấm chọn category con muốn xem để thu hẹp kết quả<br>3-5c. Chọn category cha hoặc "Trang Chủ (Home)" ở phần Breadcrumb để quay lại. |
| **3** | **Tìm kiếm/Xem cửa hàng (UC_006 - Search/View stores)** | Tìm kiếm, xem danh sách cửa hàng và tham quan cửa hàng | **Main Flow**:<br>1. Vào hệ thống website [http://localhost:8080/](http://localhost:8080/)<br>2. Chọn chức năng tìm kiếm theo cửa hàng trên thanh tìm kiếm (chọn dropdown `🏬 Cửa hàng`)<br>3. Nhập từ khóa (ví dụ: `Quận 1`, `Bến Thành`, `Quận 7`)<br>4. Nhấn Enter hoặc bấm nút hình kính lúp<br>5. Chuyển đến trang tìm kiếm cửa hàng [http://localhost:8080/stores](http://localhost:8080/stores), hiển thị danh sách cửa hàng theo từ khóa<br>6. Chọn **Next (Sau)**, **Previous (Trước)** hoặc số trang bất kỳ ở phần phân trang để chuyển trang<br>7. Chọn vào một cửa hàng bất kỳ, chuyển đến trang chủ của cửa hàng ([http://localhost:8080/stores/1](http://localhost:8080/stores/1))<br>8. Trên thanh điều hướng của cửa hàng, chọn **Collection (Bộ Sưu Tập)**, xem danh sách sản phẩm cửa hàng.<br><br>**Alternative Flow**:<br>8a. Chọn **Review & Rating (Đánh Giá)**: Xem điểm sao trung bình và danh sách các đánh giá của khách hàng về cửa hàng<br>8b. Chọn **About (Thông Tin Cửa Hàng)**: Xem giới thiệu chi nhánh, địa chỉ, hotline, thời gian mở cửa và cam kết chất lượng<br>8c. Sử dụng thanh tìm kiếm sản phẩm trong cửa hàng (`Tìm mẫu hoa trong cửa hàng này...`) để tìm kiếm sản phẩm tại chi nhánh đó. |
| **4** | **Xem/Tìm kiếm người dùng** | Tìm kiếm, xem danh sách người dùng và tham quan trang người dùng | - Truy cập link **Người Dùng** trên thanh điều hướng [http://localhost:8080/users](http://localhost:8080/users).<br>- Tìm kiếm người dùng theo họ tên, username, email.<br>- Bấm xem hồ sơ công khai [http://localhost:8080/users/2](http://localhost:8080/users/2): Xem avatar, ảnh bìa, ngày tham gia và giới thiệu bản thân. |

---

### PHẦN 3.1.2: PHÍA NGƯỜI MUA (CUSTOMER)

Đăng nhập tài khoản `khachhang` / `123456`.

| STT | Chức Năng | Mô Tả | Đường Dẫn / Cách Kiểm Thử |
|:---:|:---|:---|:---|
| **1** | **Đăng nhập (UC_002 - Sign in)** | Đăng nhập vào hệ thống (Customer, Vendor, Admin) | **Main Flow**:<br>1. Vào hệ thống website [http://localhost:8080/](http://localhost:8080/)<br>2. Chọn nút **Sign in** (Đăng Nhập) trên thanh điều hướng<br>3. Nhập email (hoặc số điện thoại, hoặc username) và mật khẩu<br>4. Bấm vào nút **Sign in (Đăng Nhập)**<br>5. Đăng nhập thành công<br>6. Hệ thống dẫn đến trang tương ứng:<br>   - Khách hàng (Customer): Giao diện mua hoa trang chủ [http://localhost:8080/](http://localhost:8080/)<br>   - Quản trị viên (Admin): Trang quản lý hệ thống [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard)<br>   - Quản lý / Người bán (Vendor): Trang quản trị chi nhánh [http://localhost:8080/manager/dashboard](http://localhost:8080/manager/dashboard)<br><br>**Exception Flow**:<br>5a. Email hoặc điện thoại không hợp lệ, mật khẩu không trùng khớp<br>5a1. Đăng nhập thất bại<br>6a. Hiển thị thông báo màu đỏ: *"Đăng nhập thất bại: Email, số điện thoại hoặc mật khẩu không chính xác!"* |
| **2** | **Đăng xuất (UC_003 - Sign out)** | Đăng xuất khỏi hệ thống (Customer, Vendor, Admin) | **Main Flow**:<br>1. Chọn biểu tượng **Account** (`<i class="bi bi-person-circle"></i>`) trên Header hoặc Sidebar, hiển thị dropdown menu<br>2. Chọn nút **Sign out (Đăng Xuất)**<br>3. Hiển thị hộp thoại modal yêu cầu xác nhận: *"Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?"*<br>4. Bấm nút **Confirm (Xác Nhận)**<br>5. Đăng xuất thành công, hệ thống hủy phiên đăng nhập và trở về giao diện dành cho Khách (Guest) tại [http://localhost:8080/](http://localhost:8080/)<br><br>**Alternative Flow**:<br>4a. Bấm nút **Cancel (Hủy)**<br>5a. Hủy thao tác (đóng modal, tiếp tục duy trì đăng nhập bình thường). |
| **3** | **Khôi phục mật khẩu (UC_004 - Password Recovery)** | Tạo mật khẩu mới khi người dùng quên mật khẩu | **Main Flow**:<br>1. Vào hệ thống website [http://localhost:8080/](http://localhost:8080/)<br>2. Chọn nút **Sign in (Đăng Nhập)** trên thanh header<br>3. Nhập email tài khoản (ví dụ: `khachhang@gmail.com`)<br>4. Chọn liên kết **Forgot password? Send email**<br>5. Hệ thống xác thực email và gửi email yêu cầu tạo mật khẩu mới về email người dùng (đồng thời hiển thị mã OTP và liên kết tạo mật khẩu mới trên màn hình console)<br>6. Người dùng kiểm tra email và chọn liên kết **Change password** (hoặc truy cập link có sẵn mã OTP: `http://localhost:8080/reset-password?email=...&otpCode=...`)<br>7. Hệ thống chuyển đến trang tạo mật khẩu mới<br>8. Hiển thị form tạo mật khẩu mới với email và mã xác thực đã được nạp sẵn<br>9. Nhập mật khẩu mới (tối thiểu 6 ký tự)<br>10. Bấm **Save (Lưu Mật Khẩu Mới)**<br>11. Thay đổi thành công: Hiển thị thông báo *"Thay đổi mật khẩu thành công!"*<br>12. Chọn nút **Back to Login** hoặc **Quay lại trang chủ** để tiếp tục sử dụng website.<br><br>**Post-conditions**:<br>- Thành công: Mật khẩu mới được cập nhật vào database (BCrypt), đăng nhập bằng mật khẩu mới thành công.<br>- Thất bại: Nếu mã OTP sai hoặc mật khẩu mới dưới 6 ký tự, hiển thị thông báo lỗi và mật khẩu mới không được tạo. |
| **4** | **Quản lý tài khoản cá nhân** | Thay đổi mật khẩu tài khoản | Vào menu cá nhân -> Chọn **Đổi Mật Khẩu** ([http://localhost:8080/customer/change-password](http://localhost:8080/customer/change-password)), nhập mật khẩu cũ và mật khẩu mới -> Xác nhận đổi. |
| **5** | **Quản lý thông tin cá nhân** | Xem, sửa thông tin cá nhân, ảnh đại diện, ảnh bìa | Vào **Hồ Sơ Cá Nhân** ([http://localhost:8080/customer/profile](http://localhost:8080/customer/profile)): Xem và cập nhật họ tên, số điện thoại, giới thiệu, tải lên ảnh đại diện (avatar) và ảnh bìa (cover). |
| **6** | **Quản lý địa chỉ cá nhân** | Xem, sửa, xóa địa chỉ cá nhân | Tại trang [http://localhost:8080/customer/profile](http://localhost:8080/customer/profile), phần **Sổ Địa Chỉ Giao Hàng**: Thêm địa chỉ mới, bấm nút **Sửa** để chỉnh sửa thông tin địa chỉ hoặc nút **Xóa** địa chỉ không dùng. |
| **7** | **Quản lý đơn hàng** | Xem lịch sử mua hàng, theo dõi trạng thái, hủy đơn hàng | Vào **Đơn Mua Của Tôi** ([http://localhost:8080/customer/orders](http://localhost:8080/customer/orders)): Xem các đơn đã đặt, theo dõi trạng thái đơn hàng (Chờ xác nhận, Đang cắm hoa, Đang giao, Đã giao, Hoàn thành). Khi đơn ở trạng thái Chờ xác nhận, bấm nút **"Hủy Đơn Hàng"**. |
| **8** | **Quản lý giỏ hàng** | Xem, thêm, cập nhật sản phẩm trong giỏ hàng | - Tại trang chi tiết sản phẩm: Chọn số lượng, bấm **Thêm Vào Giỏ Hàng**.<br>- Tại trang Giỏ hàng ([http://localhost:8080/cart](http://localhost:8080/cart)): Xem danh sách, bấm `+` hoặc `-` để cập nhật số lượng, bấm xóa sản phẩm. |
| **9** | **Đặt hàng** | Đặt hàng, thanh toán hoa | - Bấm **Tiến Hành Thanh Toán** từ giỏ hàng sang [http://localhost:8080/checkout](http://localhost:8080/checkout).<br>- **Hình thức nhận hàng**:<br>  + *Giao hàng tận nơi*: Nhập địa chỉ nhận, hệ thống tự động điều phối chi nhánh gần nhất.<br>  + *Đến lấy trực tiếp*: Chọn chi nhánh mong muốn đến nhận hoa.<br>- **Lời chúc đi kèm**: Có thể nhập lời nhắn thiệp (cho phép để trống).<br>- Chọn hình thức thanh toán (COD / VNPay / Chuyển khoản) -> Bấm **Xác Nhận Đặt Hoa**. |
| **10** | **Phản hồi, đánh giá** | Phản hồi, đánh giá sản phẩm đã mua | Sau khi đơn hoàn thành, vào chi tiết đơn hàng hoặc trang sản phẩm để đánh giá sao (1-5 sao) và viết nhận xét (yêu cầu tối thiểu 50 ký tự) -> Gửi đánh giá. |
| **11** | **Theo dõi sản phẩm** | Theo dõi, sưu tầm các sản phẩm yêu thích | - Tại trang chi tiết sản phẩm [http://localhost:8080/product/1](http://localhost:8080/product/1), bấm nút **"Thêm vào Yêu thích"**.<br>- Xem danh sách tại [http://localhost:8080/customer/wishlist](http://localhost:8080/customer/wishlist). |
| **12** | **Theo dõi cửa hàng** | Theo dõi, sưu tầm các cửa hàng yêu thích | - Tại trang chi tiết cửa hàng [http://localhost:8080/stores/1](http://localhost:8080/stores/1), bấm nút **"Theo Dõi Cửa Hàng"**.<br>- Xem danh sách các cửa hàng đang theo dõi tại [http://localhost:8080/customer/followed-stores](http://localhost:8080/customer/followed-stores). |

---

### PHẦN 3.1.3: PHÍA NGƯỜI BÁN (VENDOR / MANAGER)

Đăng nhập tài khoản `manager_q1` / `123456`.

| STT | Chức Năng | Mô Tả | Đường Dẫn / Cách Kiểm Thử |
|:---:|:---|:---|:---|
| **1** | **Tạo cửa hàng** | Tạo cửa hàng mới cho phép kinh doanh trong hệ thống | Truy cập [http://localhost:8080/manager/stores/create](http://localhost:8080/manager/stores/create): Nhập tên cửa hàng, hotline, địa chỉ, ảnh đại diện, ảnh bìa, ảnh đặc trưng -> Bấm **Đăng Ký Chi Nhánh**. |
| **2** | **Quản lý các cửa hàng cá nhân** | Xem danh sách các cửa hàng sở hữu và hợp tác | Truy cập [http://localhost:8080/manager/stores](http://localhost:8080/manager/stores): Xem danh sách các cửa hàng với huy hiệu rõ ràng: `Sở hữu` (cửa hàng do tài khoản làm chủ) hoặc `Hợp tác` (chi nhánh tham gia quản lý, điều hành). |
| **3** | **Quản lý thông tin cửa hàng** | Xem, chỉnh sửa thông tin cửa hàng, ảnh đại diện, ảnh bìa, ảnh đặc trưng | Tại danh sách cửa hàng, bấm **"Chỉnh Sửa"** ([http://localhost:8080/manager/stores/edit/1](http://localhost:8080/manager/stores/edit/1)): Cập nhật tên shop, hotline, địa chỉ, giới thiệu, ảnh đại diện (avatar), ảnh bìa (cover), ảnh đặc trưng (featured images) và trạng thái mở cửa đón khách (`isOpen`). |
| **4** | **Quản lý nhân viên cửa hàng** | Xem, thêm, xóa nhân viên (đối tác) của cửa hàng | Truy cập [http://localhost:8080/manager/staff](http://localhost:8080/manager/staff):<br>- Xem danh sách nhân sự tại chi nhánh.<br>- Bấm **"Thêm Nhân Viên Mới"**: Nhập username, họ tên, SĐT, chọn vai trò (`Quản lý / Đối tác` hoặc `Nhân viên giao hàng`).<br>- Bấm nút **"Xóa Khỏi Shop"** để xóa nhân viên khỏi chi nhánh. |
| **5** | **Quản lý đơn hàng của cửa hàng** | Xem, cập nhật trạng thái, theo dõi trạng thái các đơn hàng của cửa hàng | Truy cập [http://localhost:8080/manager/orders](http://localhost:8080/manager/orders):<br>- Lọc đơn theo tab: Chờ xác nhận, Đã nhận, Đang cắm hoa, Đang giao, Đã giao, Hoàn thành, Đã hủy.<br>- Bấm nút cập nhật tiến độ: **Nhận Đơn** &rarr; **Cắm Hoa** &rarr; **Giao Shipper** (chọn tài xế) &rarr; **Hoàn Tất**. |
| **6** | **Quản lý sản phẩm của cửa hàng** | Xem, thêm, lưu trữ (hoặc kinh doanh), sửa sản phẩm của cửa hàng | Truy cập [http://localhost:8080/manager/products](http://localhost:8080/manager/products):<br>- Xem danh sách mẫu hoa của shop.<br>- Bấm **"Thêm Mẫu Hoa Mới"**: Điền tên, danh mục, giá bán, link ảnh, mô tả ý nghĩa hoa.<br>- Bấm nút **"Sửa"**: Chỉnh sửa thông tin, giá bán mẫu hoa.<br>- Bấm nút **"Lưu Trữ" / "Kinh Doanh"**: Chuyển đổi linh hoạt giữa trạng thái kinh doanh mở bán và trạng thái lưu trữ tạm ẩn khỏi gian hàng. |
| **7** | **Quản lý ví điện tử của cửa hàng** | Xem lịch sử giao dịch của cửa hàng, rút tiền | Truy cập [http://localhost:8080/manager/wallet](http://localhost:8080/manager/wallet):<br>- Xem số dư ví doanh thu cửa hàng và biến động dòng tiền.<br>- Bấm **"Rút Tiền"**: Điền số tiền cần rút, tên ngân hàng, số tài khoản, tên chủ thẻ -> Gửi yêu cầu rút tiền thành công. |
| **8** | **Thống kê doanh thu** | Thống kê doanh thu theo sản phẩm, đơn hàng của cửa hàng, theo từng loại thời gian (giờ, ngày, tháng, năm), theo loại biểu đồ (đường, cột, tròn) | Truy cập [http://localhost:8080/manager/analytics](http://localhost:8080/manager/analytics):<br>- **Phân loại**: Bấm chọn **Theo Đơn Hàng** hoặc **Theo Sản Phẩm**.<br>- **Thời gian**: Bấm chọn **Theo Giờ**, **Theo Ngày**, **Theo Tháng**, **Theo Năm**.<br>- **Loại biểu đồ**: Bấm nút chuyển đổi qua lại giữa biểu đồ **Cột (Bar)**, **Đường (Line)** và **Tròn (Pie/Doughnut)**. |

---

### PHẦN 3.1.4: PHÍA QUẢN TRỊ VIÊN (ADMIN)

Đăng nhập tài khoản `admin` / `123456`.

| STT | Chức Năng | Mô Tả | Đường Dẫn / Cách Kiểm Thử |
|:---:|:---|:---|:---|
| **1** | **Thống kê doanh thu hệ thống** | Thống kê doanh thu toàn sàn theo giờ, ngày, tháng, năm; biểu đồ đường, cột, tròn | [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard): Chọn khung giờ/ngày/tháng/năm và đổi loại biểu đồ. |
| **2** | **Thống kê người dùng mới** | Thống kê số lượng người tham gia theo giờ, ngày, tháng, năm; biểu đồ đường, cột, tròn | [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard): Xem biểu đồ tăng trưởng thành viên mới. |
| **3** | **Thống kê cửa hàng mới** | Thống kê số cửa hàng gia nhập chuỗi theo giờ, ngày, tháng, năm; biểu đồ đường, cột, tròn | [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard): Xem biểu đồ phát triển mạng lưới chi nhánh. |
| **4** | **Quản lý người dùng hệ thống** | Xem danh sách người dùng toàn hệ thống | [http://localhost:8080/admin/users](http://localhost:8080/admin/users): Xem họ tên, email, vai trò, số điện thoại người dùng. |
| **5** | **Quản lý cửa hàng trong hệ thống** | Xem danh sách, cấp phép (hoặc cấm) cửa hàng trong hệ thống | [http://localhost:8080/admin/stores](http://localhost:8080/admin/stores): Bấm nút **Cấp Phép** / **Cấm Hoạt Động** cửa hàng. |
| **6** | **Quản lý sản phẩm trong hệ thống** | Xem danh sách, cấp phép (hoặc cấm) sản phẩm trong hệ thống | [http://localhost:8080/admin/products](http://localhost:8080/admin/products): Bấm nút **Duyệt Bán** / **Cấm Bán** mẫu hoa. |
| **7** | **Quản lý đơn hàng trong hệ thống** | Xem danh sách đơn hàng toàn chuỗi | [http://localhost:8080/admin/orders](http://localhost:8080/admin/orders): Theo dõi tổng giá trị, tiến độ đơn hàng và chi nhánh phục vụ. |
| **8** | **Quản lý cấp độ người dùng** | Xem, thêm, xóa, sửa cấp bậc thành viên | [http://localhost:8080/admin/user-levels](http://localhost:8080/admin/user-levels): Thêm cấp độ (Đồng, Bạc, Vàng, Kim Cương), chỉnh sửa điều kiện điểm số, xóa cấp độ. |
| **9** | **Quản lý cấp độ cửa hàng** | Xem, thêm, xóa, sửa cấp bậc cửa hàng | [http://localhost:8080/admin/store-levels](http://localhost:8080/admin/store-levels): Quản lý thứ hạng chi nhánh theo doanh thu và uy tín. |
| **10** | **Quản lý loại sản phẩm** | Xem, thêm, xóa, sửa danh mục hoa | [http://localhost:8080/admin/categories](http://localhost:8080/admin/categories): Thêm danh mục mới, bấm nút **Sửa** (cập nhật tên, icon), bấm nút **Xóa** danh mục. |
| **11** | **Quản lý thuộc tính sản phẩm** | Xem, thêm, xóa, sửa các đặc tính mẫu hoa | [http://localhost:8080/admin/styles](http://localhost:8080/admin/styles): Quản lý phong cách (Cổ điển, Hiện đại, Tối giản, Hàn Quốc...). |
| **12** | **Quản lý hình thức vận chuyển** | Xem, thêm, xóa, sửa đơn vị vận chuyển | [http://localhost:8080/admin/deliveries](http://localhost:8080/admin/deliveries): Cài đặt đơn vị giao hoa (Hỏa tốc 2H, Tiết kiệm, Grab...). |
| **13** | **Quản lý mức hoa hồng** | Xem, thêm, xóa, sửa tỷ lệ chiết khấu sàn | [http://localhost:8080/admin/commissions](http://localhost:8080/admin/commissions): Thiết lập mức hoa hồng áp dụng cho các chi nhánh. |
| **14** | **Quản lý giao dịch hệ thống** | Xem nhật ký các giao dịch dòng tiền toàn sàn | [http://localhost:8080/admin/transactions](http://localhost:8080/admin/transactions): Kiểm soát lịch sử nạp tiền, rút tiền, thanh toán đơn hàng. |

---

### PHẦN 3.1.5: PHÍA NHÂN VIÊN GIAO HÀNG (SHIPPER)

Đăng nhập tài khoản `shipper_nhanh` / `123456`.

| STT | Chức Năng | Mô Tả | Đường Dẫn / Cách Kiểm Thử |
|:---:|:---|:---|:---|
| **1** | **Xem đơn hàng được điều phối** | Xem danh sách các đơn hoa tươi được cửa hàng phân công đi giao | Truy cập [http://localhost:8080/shipper/orders](http://localhost:8080/shipper/orders): Xem tên người nhận, số điện thoại, địa chỉ giao hàng, lời chúc thiệp kèm theo. |
| **2** | **Cập nhật tiến độ giao hàng** | Nhận đơn đi giao & xác nhận giao hoa thành công | Bấm **"Bắt Đầu Giao"** (chuyển trạng thái đơn sang `DELIVERING`) &rarr; Bấm **"Đã Giao Thành Công"** (chuyển sang `DELIVERED`). |

---

## 3. TỔNG KẾT NGHIỆM THU

Hệ thống đã hoàn thiện 100% đầy đủ tất cả các yêu cầu chức năng theo đúng Đề cương khảo sát và Rubric đánh giá:
1. **Phía Khách vãng lai (Guest)**: 4/4 chức năng (Đăng ký, Xem/Tìm sản phẩm, Xem/Tìm cửa hàng & tham quan gian hàng, Xem/Tìm người dùng & xem hồ sơ).
2. **Phía Người mua (Customer)**: 12/12 chức năng (Đăng nhập, Đăng xuất, Quên mật khẩu, Đổi mật khẩu, Quản lý hồ sơ & ảnh đại diện/bìa, Quản lý địa chỉ [thêm/sửa/xóa], Quản lý đơn & hủy đơn, Giỏ hàng, Đặt hàng [Giao tận nơi tự động xếp chi nhánh gần nhất / Đến lấy tự chọn chi nhánh], Đánh giá &ge; 50 ký tự, Sưu tầm sản phẩm, Theo dõi cửa hàng).
3. **Phía Người bán (Vendor / Manager)**: 8/8 chức năng (Tạo cửa hàng, Quản lý cửa hàng sở hữu & hợp tác, Quản lý thông tin & ảnh đại diện/bìa/ảnh đặc trưng, Quản lý nhân viên [xem/thêm/xóa], Quản lý đơn hàng [nhận/cắm hoa/giao shipper/hoàn tất], Quản lý sản phẩm [thêm/sửa/kinh doanh/lưu trữ], Quản lý ví & rút tiền, Thống kê doanh thu theo đơn hàng & theo sản phẩm, theo giờ/ngày/tháng/năm, đa biểu đồ đường/cột/tròn).
4. **Phía Quản trị viên (Admin)**: 14/14 chức năng (3 Thống kê đa chu kỳ/biểu đồ + 11 Module Quản trị CRUD hoàn chỉnh).
5. **Phía Nhân viên giao hàng (Shipper)**: Tiếp nhận đơn và cập nhật tiến độ giao hoa.
