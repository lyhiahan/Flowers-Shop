# HƯỚNG DẪN CHI TIẾT KIỂM THỬ TOÀN BỘ CHỨC NĂNG HỆ THỐNG
## Chuỗi Cửa Hàng Hoa Tươi & Điện Hoa Sài Gòn (Bloom Flower Chain)
### *"Nở Trọn Yêu Thương, Thắm Trọn Khoảnh Khắc"*

Tài liệu này hướng dẫn chi tiết từng bước (Step-by-Step) cách kiểm tra và nghiệm thu toàn bộ các chức năng của hệ thống trên giao diện web thực tế (Chrome / Edge / Firefox) theo đúng Đề cương khảo sát phân tích hệ thống và Rubric đánh giá.

---

## 1. THÔNG TIN KHỞI ĐỘNG & TÀI KHOẢN ĐĂNG NHẬP

- **Địa chỉ trang web**: [http://localhost:8080](http://localhost:8080)
- **Mật khẩu chung cho tất cả tài khoản**: `123456`

### Danh sách tài khoản thử nghiệm cho 4 vai trò (Roles):

| Vai Trò (Actor / Role) | Tên Đăng Nhập | Mật Khẩu | Trang Đích Tự Động Khi Đăng Nhập |
|:---|:---|:---:|:---|
| **1. Khách vãng lai (Guest)** | *(Không cần đăng nhập)* | - | [http://localhost:8080/](http://localhost:8080/) |
| **2. Khách hàng (Customer)** | `khachhang` | `123456` | [http://localhost:8080/](http://localhost:8080/) |
| **3. Quản lý chi nhánh (Manager)** | `manager_q1` | `123456` | [http://localhost:8080/manager/dashboard](http://localhost:8080/manager/dashboard) |
| **4. Nhân viên giao hàng (Shipper)** | `shipper_nhanh` | `123456` | [http://localhost:8080/shipper/orders](http://localhost:8080/shipper/orders) |
| **5. Quản trị viên (Admin)** | `admin` | `123456` | [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard) |

*(Lưu ý: Role `STAFF` đã được loại bỏ hoàn toàn khỏi toàn bộ hệ thống).*

---

## 2. KỊCH BẢN KIỂM THỬ CHI TIẾT THEO TỪNG VAI TRÒ

---

### VAI TRÒ 1: KHÁCH VÃNG LAI (GUEST - KHÔNG CẦN ĐĂNG NHẬP)

Khách vãng lai là người dùng truy cập website chưa có tài khoản hoặc chưa đăng nhập.

#### Bước 1.1: Xem Trang chủ & Danh mục Bán Chạy (sold >= 10)
- **Thao tác**: Mở trình duyệt truy cập [http://localhost:8080/](http://localhost:8080/)
- **Kỳ vọng hiển thị**:
  1. Header, banner hoa tươi sắc nét, thanh tìm kiếm từ khóa.
  2. Mục **"Top Hoa Bán Chạy Nhất Chuỗi (Đã Bán > 10)"**:
     - Hiển thị danh sách các mẫu hoa có lượng bán từ 10 sản phẩm trở lên.
     - Sắp xếp theo thứ tự giảm dần của số lượng bán (`sold DESC` như: 125, 110, 95, 88...).
     - Mỗi sản phẩm có huy hiệu màu đỏ nổi bật `Đã bán: X`.

#### Bước 1.2: Tìm kiếm mẫu hoa theo từ khóa
- **Thao tác**: Nhập chữ `Lan` hoặc `Hồng` hoặc `Hoa` vào ô tìm kiếm ở đầu trang rồi bấm nút **Tìm Kiếm** (hoặc truy cập [http://localhost:8080/?keyword=Lan](http://localhost:8080/?keyword=Lan)).
- **Kỳ vọng**: Danh sách sản phẩm lập tức lọc ra các mẫu hoa có tên hoặc mô tả chứa từ khóa.

#### Bước 1.3: Lọc theo Danh mục hoa (Thanh Chọn Nhanh Shopee-Style & Bộ Lọc)
- **Thao tác**: 
  1. Ngay bên dưới thanh tìm kiếm ở đầu trang (phong cách Shopee), bấm trực tiếp vào một danh mục mong muốn (ví dụ: *Hoa Bó Tươi Nghệ Thuật*, *Giỏ & Hộp Hoa Cao Cấp*, *Lan Hồ Điệp Hoàng Gia*, *Phụ Kiện Hoa & Khung Sự Kiện*...).
  2. Hoặc cuộn xuống mục bộ lọc danh mục và bấm vào các thẻ danh mục (Pills).
- **Kỳ vọng**: 
  - Danh mục được chọn lập tức sáng viền/gạch chân vàng nổi bật (`active`).
  - Danh sách sản phẩm bên dưới được lọc chuẩn xác 100% các sản phẩm thuộc danh mục do Admin quản lý.

#### Bước 1.4: Sắp xếp theo giá (Tăng dần / Giảm dần)
- **Thao tác**: 
  - Chọn sắp xếp **"Giá tăng dần"** (hoặc link [http://localhost:8080/?sort=price_asc](http://localhost:8080/?sort=price_asc)): Các mẫu hoa có giá thấp sẽ đứng đầu.
  - Chọn sắp xếp **"Giá giảm dần"** (hoặc link [http://localhost:8080/?sort=price_desc](http://localhost:8080/?sort=price_desc)): Các kệ hoa/lẵng hoa cao cấp giá cao sẽ đứng đầu.

#### Bước 1.5: Xem Tabs Top 20 & Phân trang
- **Thao tác**:
  - Bấm vào tab **"🔥 Top 20 Bán Chạy Nhất"** ([http://localhost:8080/?tab=bestseller](http://localhost:8080/?tab=bestseller)).
  - Bấm vào tab **"✨ 20 Mẫu Mới Nhất"** ([http://localhost:8080/?tab=newest](http://localhost:8080/?tab=newest)).
  - Bấm vào các số trang `1`, `2` ở cuối danh sách sản phẩm để kiểm tra phân trang.
- **Kỳ vọng**: Dữ liệu chuyển trang mượt mà, đúng số lượng sản phẩm mỗi trang.

#### Bước 1.6: Xem Chi tiết mẫu hoa
- **Thao tác**: Bấm vào xem một mẫu hoa bất kỳ (ví dụ: [http://localhost:8080/product/1](http://localhost:8080/product/1)).
- **Kỳ vọng**: Hiển thị ảnh mẫu hoa, giá mua đứt, đơn giá thuê theo ngày, tiền cọc hoàn lại, thông tin chi nhánh cung cấp, đánh giá sao của khách hàng.

---

### VAI TRÒ 2: KHÁCH HÀNG (CUSTOMER)

#### Bước 2.1: Đăng nhập tài khoản Khách Hàng
- **Thao tác**: Bấm **Đăng Nhập** ở góc trên bên phải trang chủ, điền:
  - Tên đăng nhập: `khachhang`
  - Mật khẩu: `123456`
- **Kỳ vọng**: Đăng nhập thành công, góc phải hiển thị tên khách hàng và số lượng giỏ hàng.

#### Bước 2.2: Thêm vào giỏ hàng (+1) và tự động chuyển đến giỏ hàng
- **Thao tác**: 
  1. Tại trang chi tiết một mẫu hoa (ví dụ: `/product/1`), chọn loại đơn: **"Thuê Sự Kiện"** hoặc **"Mua Luôn"**.
  2. Chọn ngày bắt đầu thuê và ngày kết thúc thuê (nếu chọn Thuê).
  3. Bấm nút **"Thêm Vào Giỏ Hàng"**.
- **Kỳ vọng**:
  - Số lượng giỏ hàng trên header tự động tăng lên (+1).
  - Hệ thống tự động chuyển thẳng người dùng vào trang Giỏ Hàng ([http://localhost:8080/cart](http://localhost:8080/cart)).

#### Bước 2.3: Chọn lại số lượng, chọn sản phẩm và thanh toán trong giỏ hàng
- **Thao tác**: Tại trang Giỏ hàng ([http://localhost:8080/cart](http://localhost:8080/cart)):
  1. Thử bấm nút `+` hoặc `-` để tăng/giảm số lượng sản phẩm -> Giá tiền tự động tính lại.
  2. Bấm nút **"Tiến Hành Đặt Hàng"** để sang trang Thanh Toán.

#### Bước 2.4: Kiểm tra trang Thanh Toán (Checkout)
- **Thao tác**: Tại trang [http://localhost:8080/checkout](http://localhost:8080/checkout), quan sát phần tóm tắt thanh toán:
- **Kỳ vọng hiển thị rõ ràng**:
  1. **Đơn giá thuê 1 ngày** (nếu là đơn thuê).
  2. **Số ngày thuê**.
  3. **Tiền cọc đảm bảo hoàn lại** (Chính sách cọc khung/chân kệ hoa).
  4. **Tổng thành tiền thanh toán**.
  5. Ô chọn **Mã Voucher / Giảm Giá** (thử nhập `FLOWER10` hoặc bấm gợi ý có sẵn).
  6. Ô chọn địa chỉ giao nhận hoa và thiệp lời chúc mừng.
  7. Chọn hình thức: **Thanh toán khi nhận hàng (COD)** hoặc **Chuyển khoản / VNPAY**.
  8. Bấm **"Xác Nhận Đặt Đơn Hoa"** -> Hệ thống tạo đơn thành công và chuyển vào trang Lịch sử đơn hàng.

#### Bước 2.5: Kiểm tra Lịch sử đơn hàng & Hủy đơn
- **Thao tác**: Vào trang [http://localhost:8080/customer/orders](http://localhost:8080/customer/orders).
- **Kỳ vọng**:
  - Thấy đơn hoa vừa đặt với mã số đơn, ngày giờ, tổng tiền và trạng thái `PENDING (Chờ xác nhận)`.
  - Có nút **"Hủy Đơn Hàng"**: Bấm vào để thử hủy đơn -> Đơn lập tức chuyển sang `CANCELLED (Đã hủy)`.

#### Bước 2.6: Kiểm tra ràng buộc Đánh Giá (Review >= 50 ký tự)
- **Thao tác kiểm tra tính hợp lệ của bài đánh giá theo Rubric**:
  1. Vào trang chi tiết hoa [http://localhost:8080/product/1](http://localhost:8080/product/1), cuộn xuống mục **"Viết Đánh Giá Của Bạn"**.
  2. **Thử đánh giá ngắn (< 50 ký tự)**: Nhập `"Hoa dep lam shop"` rồi bấm gửi -> **Hệ thống chặn lại và báo lỗi đỏ**: *"Nội dung nhận xét tối thiểu 50 ký tự để đảm bảo chất lượng đánh giá!"*.
  3. **Đánh giá chuẩn (>= 50 ký tự)**: Nhập nội dung dài trên 50 ký tự (Ví dụ: *"Mẫu hoa tươi rất đẹp và thơm ngát, cánh hoa cứng cáp giao đúng giờ hẹn, cảm ơn shop rất nhiều!"*) rồi bấm gửi -> **Hệ thống chấp thuận và hiển thị ngay trên danh sách đánh giá**.

#### Bước 2.7: Kiểm tra Danh sách Yêu Thích & Đã Xem Gần Đây
- **Thao tác**:
  - Bấm nút icon trái tim ở một sản phẩm -> Vào [http://localhost:8080/customer/wishlist](http://localhost:8080/customer/wishlist) kiểm tra danh sách yêu thích.
  - Vào [http://localhost:8080/customer/recently-viewed](http://localhost:8080/customer/recently-viewed) kiểm tra các mẫu hoa đã xem gần đây.

---

### VAI TRÒ 3: QUẢN LÝ CHI NHÁNH (MANAGER / VENDOR)

#### Bước 3.1: Đăng nhập Quản Lý Chi Nhánh
- **Thao tác**: Đăng xuất tài khoản khách hàng, sau đó đăng nhập:
  - Tên đăng nhập: `manager_q1`
  - Mật khẩu: `123456`
- **Kỳ vọng**: Hệ thống **tự động chuyển hướng** vào trang Quản Trị Chi Nhánh: [http://localhost:8080/manager/dashboard](http://localhost:8080/manager/dashboard).

#### Bước 3.2: Kiểm tra Quản lý đơn hàng & Điều phối Shipper
- **Thao tác**: Vào menu **"Đơn Hàng Chi Nhánh"** ([http://localhost:8080/manager/orders](http://localhost:8080/manager/orders)).
- **Kỳ vọng**:
  - Hiển thị danh sách các đơn hoa khách đặt tại chi nhánh Quận 1.
  - Có các nút cập nhật trạng thái: **Xác nhận đơn (CONFIRMED)**.
  - Có ô dropdown chọn và gán tài xế giao hoa (**Shipper Nhanh**) để đi giao hàng cho khách.

#### Bước 3.3: Kiểm tra Quản lý Mẫu Hoa (Giá bán >= Giá nhập)
- **Thao tác**: Vào menu **"Kho Mẫu Hoa"** ([http://localhost:8080/manager/products](http://localhost:8080/manager/products)).
- **Kỳ vọng**:
  - Danh sách mẫu hoa của chi nhánh, số lượng tồn kho, giá bán, giá thuê.
  - Bấm **"Thêm Mẫu Hoa Mới"**: Thử nhập giá bán nhỏ hơn giá nhập (Ví dụ: Nhập 100.000đ nhưng Bán 50.000đ) -> Hệ thống chặn lại không cho tạo vì vi phạm quy định chống bán lỗ của hệ thống.

#### Bước 3.4: Kiểm tra Ví Doanh Thu & Lệnh Rút Tiền
- **Thao tác**: Vào menu **"Ví Doanh Thu"** ([http://localhost:8080/manager/wallet](http://localhost:8080/manager/wallet)).
- **Kỳ vọng**:
  - Hiển thị số dư ví điện tử của cửa hàng, tổng tiền bán hoa đã thu.
  - Có form tạo yêu cầu rút tiền về tài khoản ngân hàng (Vietcombank, MB Bank...).

#### Bước 3.5: Xem Báo Cáo & Danh sách mạng lưới chi nhánh
- **Thao tác**:
  - Vào **"Thống Kê Doanh Thu"** ([http://localhost:8080/manager/analytics](http://localhost:8080/manager/analytics)): Xem biểu đồ doanh thu chi nhánh.
  - Vào **"Mạng Lưới Chi Nhánh"** ([http://localhost:8080/manager/stores](http://localhost:8080/manager/stores)): Xem danh sách tất cả các chi nhánh khác trong chuỗi Bloom Flower để hỗ trợ khách mượn chi nhánh A trả hoa chi nhánh B.

---

### VAI TRÒ 4: NHÂN VIÊN GIAO HÀNG (SHIPPER)

#### Bước 4.1: Đăng nhập Nhân Viên Giao Hàng
- **Thao tác**: Đăng xuất, sau đó đăng nhập:
  - Tên đăng nhập: `shipper_nhanh`
  - Mật khẩu: `123456`
- **Kỳ vọng**: Hệ thống **tự động chuyển thẳng** vào trang giao hàng của Shipper: [http://localhost:8080/shipper/orders](http://localhost:8080/shipper/orders).

#### Bước 4.2: Kiểm tra Quản lý đơn giao & Tiền COD đã thu
- **Thao tác**: Tại trang [http://localhost:8080/shipper/orders](http://localhost:8080/shipper/orders), quan sát:
  1. **3 Thẻ thống kê đầu trang**:
     - *Đơn đang đi giao*
     - *Đã giao thành công*
     - *Tiền COD đã thu nộp về shop*
  2. **Thông tin chi tiết từng đơn hàng**:
     - Địa chỉ lấy hoa tại cửa hàng chi nhánh.
     - Địa chỉ nhận hoa của khách hàng và nút bấm gọi điện thoại (`tel:09...`).
     - Mẫu hoa cần giao và lời chúc trên thiệp.
     - Số tiền cần thu hộ (COD).
  3. **Thao tác giao hàng**: Bấm nút **"Đã Giao Tận Tay"** -> Trạng thái đơn lập tức chuyển thành `DELIVERED (Giao thành công)`, tiền COD tự động cộng vào thống kê.

---

### VAI TRÒ 5: QUẢN TRỊ VIÊN TOÀN HỆ THỐNG (ADMIN)

#### Bước 5.1: Đăng nhập Quản Trị Viên (Admin)
- **Thao tác**: Đăng xuất, sau đó đăng nhập:
  - Tên đăng nhập: `admin`
  - Mật khẩu: `123456`
- **Kỳ vọng**: Hệ thống **tự động chuyển hướng** vào trang quản trị tối cao: [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard).

#### Bước 5.2: Kiểm tra Dashboard 3 Biểu Đồ & Đa Chu Kỳ (Giờ, Ngày, Tháng, Năm)
- **Thao tác**: Tại trang [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard):
  1. Quan sát các nút chọn thời gian: **Theo Giờ**, **Theo Ngày**, **Theo Tháng**, **Theo Năm**. Thử bấm lần lượt từng nút.
  2. Quan sát nút chọn dạng biểu đồ: **Đường (Line)**, **Cột (Bar)**, **Tròn (Pie)**.
- **Kỳ vọng**:
  - **Biểu đồ 1**: Thống kê doanh thu bán & thuê hoa toàn sàn.
  - **Biểu đồ 2**: Thống kê số lượng người dùng mới gia nhập hệ thống.
  - **Biểu đồ 3**: Thống kê số lượng cửa hàng / chi nhánh mới gia nhập chuỗi.
  - Dữ liệu và biểu đồ tự động cập nhật ngay lập tức theo chu kỳ và dạng biểu đồ được chọn.

#### Bước 5.3: Quản lý người dùng (Khóa / Mở Khóa tài khoản)
- **Thao tác**: Vào menu **"Quản Lý Người Dùng"** ([http://localhost:8080/admin/users](http://localhost:8080/admin/users)).
- **Kỳ vọng**:
  - Hiển thị danh sách tất cả người dùng trong hệ thống (Customer, Manager, Shipper).
  - Có nút gạt / nút bấm **"Khóa Tài Khoản"** và **"Mở Khóa"**.
  - Thử bấm khóa một tài khoản: Trạng thái chuyển sang `Bị khóa`, người dùng đó sẽ không thể đăng nhập cho đến khi được Admin mở khóa lại.

#### Bước 5.4: Quản lý cửa hàng (Cấp phép / Cấm cửa hàng)
- **Thao tác**: Vào menu **"Quản Lý Cửa Hàng"** ([http://localhost:8080/admin/stores](http://localhost:8080/admin/stores)).
- **Kỳ vọng**:
  - Hiển thị danh sách các chi nhánh trong chuỗi.
  - Có nút **"Cấm Hoạt Động"** / **"Cấp Phép"**: Khi cấm, chi nhánh đó sẽ không thể đăng bán mẫu hoa mới trên sàn.

#### Bước 5.5: Quản lý mẫu hoa toàn chuỗi
- **Thao tác**: Vào menu **"Duyệt Mẫu Hoa"** ([http://localhost:8080/admin/products-manage](http://localhost:8080/admin/products-manage)).
- **Kỳ vọng**: Xem toàn bộ các mẫu hoa của tất cả chi nhánh, có quyền cấp phép hoặc cấm hiển thị mẫu hoa vi phạm.

#### Bước 5.6: Giám sát toàn bộ đơn hàng
- **Thao tác**: Vào menu **"Toàn Chuỗi Đơn Hàng"** ([http://localhost:8080/admin/orders-manage](http://localhost:8080/admin/orders-manage)).
- **Kỳ vọng**: Xem tất cả các đơn hàng của mọi chi nhánh, có thể theo dõi và cập nhật trạng thái đơn hàng khi cần can thiệp xử lý khiếu nại.

#### Bước 5.7: Cấu hình các phân hệ nghiệp vụ hệ thống
Admin có thể kiểm tra đầy đủ các màn hình cấu hình danh mục và chính sách sàn:
1. **Danh mục hoa**: [http://localhost:8080/admin/categories](http://localhost:8080/admin/categories) (Thêm mới, sửa danh mục).
2. **Hoa hồng chiết khấu sàn**: [http://localhost:8080/admin/commissions](http://localhost:8080/admin/commissions) (Cấu hình % chiết khấu từng gói).
3. **Đơn vị vận chuyển & Cước phí**: [http://localhost:8080/admin/deliveries](http://localhost:8080/admin/deliveries).
4. **Mã giảm giá sàn (Voucher)**: [http://localhost:8080/admin/vouchers](http://localhost:8080/admin/vouchers) (Tạo voucher giảm % hoặc Freeship toàn sàn).
5. **Cấp bậc khách hàng (User Levels)**: [http://localhost:8080/admin/user-levels](http://localhost:8080/admin/user-levels) (Đồng, Bạc, Vàng, Kim Cương).
6. **Cấp bậc chi nhánh (Store Levels)**: [http://localhost:8080/admin/store-levels](http://localhost:8080/admin/store-levels).
7. **Phong cách mẫu hoa**: [http://localhost:8080/admin/styles](http://localhost:8080/admin/styles).
8. **Sổ cái tài chính & Dòng tiền**: [http://localhost:8080/admin/transactions](http://localhost:8080/admin/transactions).

---

### VAI TRÒ 6: BẢO MẬT PHÂN QUYỀN RBAC & XÁC NHẬN LOẠI BỎ ROLE STAFF

- **Mục tiêu**: Xác nhận rằng vai trò `STAFF` đã bị loại bỏ hoàn toàn, hệ thống chỉ còn 4 vai trò chuẩn (`ADMIN`, `MANAGER`, `CUSTOMER`, `SHIPPER`).
- **Thao tác kiểm tra**:
  1. Đăng xuất khỏi mọi tài khoản.
  2. Vào trang đăng nhập [http://localhost:8080/login](http://localhost:8080/login).
  3. Thử đăng nhập tài khoản cũ `staff_q1` với mật khẩu `123456`.
- **Kỳ vọng**: Hệ thống từ chối đăng nhập và báo lỗi đăng nhập không thành công, không thể truy cập bất kỳ tài nguyên nào.

---

## 3. TỔNG KẾT BẢNG ĐÁNH GIÁ CHỨC NĂNG

| STT | Chức năng kiểm thử | Kết quả đạt được | Trạng thái |
|:---:|:---|:---|:---:|
| 1 | Khách vãng lai xem hoa bán chạy `sold >= 10` xếp giảm dần | Hiển thị chính xác với nhãn `Đã bán: X` | **ĐẠT** |
| 2 | Tìm kiếm, lọc theo danh mục, sắp xếp giá tăng/giảm | Bộ lọc hoạt động nhanh, chính xác | **ĐẠT** |
| 3 | Tabs Top 20 (Bán chạy, Mới nhất, Đánh giá cao) & Phân trang | Hoạt động mượt mà | **ĐẠT** |
| 4 | Giỏ hàng (+1 số lượng), tự động chuyển đến giỏ hàng | Hoạt động tức thì | **ĐẠT** |
| 5 | Trang thanh toán hiển thị số ngày thuê, đơn giá, cọc, tổng tiền | Đầy đủ thông tin minh bạch | **ĐẠT** |
| 6 | Đặt hàng, theo dõi lịch sử và hủy đơn | Tạo đơn & hủy đơn chuẩn xác | **ĐẠT** |
| 7 | Kiểm tra ràng buộc độ dài đánh giá `>= 50 ký tự` | Dưới 50 ký tự bị chặn, trên 50 ký tự được lưu | **ĐẠT** |
| 8 | Quản lý chi nhánh nhận đơn, điều phối Shipper | Gán tài xế giao hoa thành công | **ĐẠT** |
| 9 | Quản lý mẫu hoa kiểm tra giá bán >= giá nhập | Chặn bán lỗ thành công | **ĐẠT** |
| 10 | Shipper xem đơn, tiền COD, xác nhận đã giao | Cập nhật DELIVERED & tính COD chuẩn | **ĐẠT** |
| 11 | Admin đăng nhập tự động vào `/admin/dashboard` | Chuyển hướng chính xác | **ĐẠT** |
| 12 | Thống kê 3 biểu đồ theo 4 chu kỳ (Giờ, Ngày, Tháng, Năm) & 3 dạng | Trực quan hóa dữ liệu với Chart.js | **ĐẠT** |
| 13 | Quản lý người dùng, khóa/mở khóa tài khoản | Hoạt động tốt | **ĐẠT** |
| 14 | Quản lý, cấp phép / cấm cửa hàng và mẫu hoa | Hoạt động tốt | **ĐẠT** |
| 15 | Loại bỏ hoàn toàn role STAFF | Bảo mật RBAC 4 Roles chuẩn hóa | **ĐẠT** |
