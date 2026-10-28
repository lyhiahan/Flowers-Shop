# KẾ HOẠCH VÀ KỊCH BẢN TỔNG HỢP QUAY VIDEO BÁO CÁO ĐỒ ÁN
## ĐỒ ÁN: WEBSITE BÁN HÀNG THEO MÔ HÌNH CHUỖI CỬA HÀNG HOA TƯƠI
### DỰ ÁN: HOA TƯƠI SÀI GÒN (BLOOM FLOWER CHAIN)
### Môn học: Lập trình Web — Học kỳ 1 (2026 - 2027)

---

> **MỤC ĐÍCH TÀI LIỆU**:
> Cung cấp kế hoạch chuẩn bị kỹ thuật và kịch bản phân cảnh chi tiết (Storyboard / Video Script) từng giây để nhóm sinh viên quay video demo nộp bài theo đúng yêu cầu:
> 1. Video quay thao tác thực tế các chức năng chính của hệ thống trên trình duyệt.
> 2. Phải có mặt (camera/webcam) của các thành viên trong nhóm.
> 3. Từng thành viên giới thiệu về bản thân và các chức năng mình tham gia xây dựng.
> 4. Kịch bản được module hóa thành các phân cảnh độc lập (`[Thành viên A]`, `[Thành viên B]`, `[Thành viên C]`, `[Thành viên D]`) để nhóm dễ dàng phân chia.

---

## MỤC LỤC KỊCH BẢN

1. [KẾ HOẠCH KỸ THUẬT VÀ CHUẨN BỊ TRƯỚC KHI QUAY](#phan-1-chuan-bi)
2. [CẤU TRÚC PHÂN BỔ THỜI LƯỢNG VIDEO (15 - 18 PHÚT)](#phan-2-thoi-luong)
3. [KỊCH BẢN CHI TIẾT TỪNG PHÂN CẢNH (SCENE BY SCENE)](#phan-3-kich-ban-chi-tiet)
   - [Phân cảnh 1: Mở đầu & Giới thiệu thành viên nhóm (Webcam Facecam)](#scene-1)
   - [Phân cảnh 2: Luồng Khách hàng — Đăng ký, OTP Email Brevo & Khám phá Catalog](#scene-2)
   - [Phân cảnh 3: Luồng Khách hàng — Live Chat WebSocket STOMP & Đặt hàng Trừ kho tức thời](#scene-3)
   - [Phân cảnh 4: Luồng Khách hàng — Thanh toán đa kênh (MoMo / Ví hoa tươi)](#scene-4)
   - [Phân cảnh 5: Luồng Vận hành Chi nhánh — Quản lý chi nhánh (Manager)](#scene-5)
   - [Phân cảnh 6: Luồng Vận hành Chi nhánh — Nhân viên cắm hoa (Staff Workspace)](#scene-6)
   - [Phân cảnh 7: Luồng Quản trị Chuỗi — Quản trị viên (Admin), Phân quyền & Audit Log](#scene-7)
   - [Phân cảnh 8: Trình diễn REST API JWT & Bộ kiểm thử tự động (880 Tests)](#scene-8)
   - [Phân cảnh 9: Kết thúc & Tổng kết nghiệm thu](#scene-9)
4. [BẢNG TÀI KHOẢN VÀ DỮ LIỆU MẪU SỬ DỤNG TRONG VIDEO](#phan-4-tai-khoan)
5. [CHECKLIST PHÒNG TRÁNH LỖI KHI QUAY VIDEO](#phan-5-checklist)

---

<a name="phan-1-chuan-bi"></a>
# 1. KẾ HOẠCH KỸ THUẬT VÀ CHUẨN BỊ TRƯỚC KHI QUAY

### 1.1. Công cụ quay video & Bố cục khung hình
* **Phần mềm quay đề xuất:** OBS Studio (miễn phí, chất lượng cao) hoặc Google Meet / Zoom bật chế độ Record.
* **Bố cục khung hình (Layout):**
  - Khung hình chính (chiếm 80% diện tích): Màn hình trình duyệt thao tác website (độ phân giải Full HD 1080p: 1920x1080).
  - Khung hình webcam (chiếm 20% góc trên bên phải): Bật camera quay rõ khuôn mặt thành viên đang nói/thuyết minh.
  - Phân cảnh đầu và phân cảnh cuối: Chuyển sang khung hình toàn cảnh (Grid View) thấy rõ mặt của tất cả các thành viên trong nhóm.

### 1.2. Chuẩn bị môi trường & Trình duyệt
* Khởi động CSDL và ứng dụng Spring Boot trước khi quay:
  ```powershell
  .\start-web.ps1
  # Đảm bảo website truy cập trơn tru tại: http://127.0.0.1:8080
  ```
* Chuẩn bị sẵn **2 trình duyệt riêng biệt** (hoặc 1 trình duyệt thường + 1 trình duyệt ẩn danh/Incognito) để demo tương tác 2 chiều:
  - **Trình duyệt 1 (Google Chrome):** Đăng nhập tài khoản **Khách hàng (`local_customer`)**.
  - **Trình duyệt 2 (Microsoft Edge):** Đăng nhập tài khoản **Quản lý chi nhánh Q1 (`local_manager`)** và **Nhân viên (`local_staff`)**.
  *(Việc mở song song 2 trình duyệt giúp minh chứng tính năng Live Chat WebSocket và chuông thông báo đơn hàng Realtime nhảy số tức thì mà không cần tải lại trang).*

---

<a name="phan-2-thoi-luong"></a>
# 2. CẤU TRÚC PHÂN BỔ THỜI LƯỢNG VIDEO (15 - 18 PHÚT)

| Phân đoạn | Nội dung chính | Người thực hiện (Gợi ý) | Thời lượng |
|---|---|---|:---:|
| **Scene 1** | Giới thiệu đồ án, công nghệ cốt lõi và các thành viên nhóm | Tất cả thành viên bật camera | 2.0 phút |
| **Scene 2** | Khách hàng: Đăng ký nhận OTP Email Brevo thật, duyệt Catalog, lọc chi nhánh | `[Thành viên A]` | 2.5 phút |
| **Scene 3** | Khách hàng: Live Chat WebSocket đính kèm thẻ hoa, giỏ hàng, trừ tồn kho | `[Thành viên A/B]` | 2.5 phút |
| **Scene 4** | Khách hàng: Áp mã Voucher, Thanh toán MoMo Sandbox QR & Ví khách hàng | `[Thành viên B]` | 2.5 phút |
| **Scene 5** | Quản lý (Manager): Nhận đơn Realtime, trả lời chat, quản lý kho hoa & ví shop | `[Thành viên C]` | 2.5 phút |
| **Scene 6** | Nhân viên (Staff): Bàn làm việc Workspace, tiếp nhận & cắm hoa hoàn tất | `[Thành viên C]` | 1.5 phút |
| **Scene 7** | Quản trị (Admin): Dashboard chuỗi, duyệt shop, quản trị danh mục & Audit Log | `[Thành viên D]` | 2.0 phút |
| **Scene 8** | Tích hợp & Kiểm thử: Demo REST API JWT Bearer + chạy bộ 880 ca test tự động | `[Thành viên D]` | 1.5 phút |
| **Scene 9** | Tổng kết thành quả, đối chiếu Rubrics và lời cảm ơn Hội đồng chấm thi | Tất cả thành viên bật camera | 1.0 phút |
| **TỔNG CỘNG** | **Toàn bộ quy trình khép kín của đồ án** | **Cả nhóm** | **~18 phút** |

---

<a name="phan-3-kich-ban-chi-tiet"></a>
# 3. KỊCH BẢN CHI TIẾT TỪNG PHÂN CẢNH (SCENE BY SCENE)

---

<a name="scene-1"></a>
### 🎬 PHÂN CẢNH 1: MỞ ĐẦU & GIỚI THIỆU THÀNH VIÊN NHÓM
* **Thời lượng:** 00:00 - 02:00 (2 phút)
* **Khung hình:** Màn hình chính hiển thị slide bìa đồ án hoặc trang chủ website [http://localhost:8080](http://localhost:8080); góc webcam hiển thị đầy đủ các thành viên trong nhóm.
* **Lời thoại & Hành động:**

> **Trưởng nhóm (Đại diện mở đầu):**
> *"Kính chào Thầy/Cô và Hội đồng chấm thi môn Lập trình Web! Nhóm chúng em gồm [Số lượng] thành viên, hôm nay xin phép được trình bày và demo sản phẩm đồ án cuối khóa với đề tài: **Hệ thống website bán hàng theo mô hình chuỗi cửa hàng hoa tươi — Hoa Tươi Sài Gòn (Bloom Flower Chain)**.*
>
> *Đồ án của chúng em được xây dựng trên nền tảng công nghệ Java 21, Spring Boot 4.1.1, giao diện Thymeleaf kết hợp Bootstrap 5 theo phong cách Floral Theme mềm mại, quản lý cơ sở dữ liệu với JPA/Hibernate trên PostgreSQL và SQL Server, tích hợp các công nghệ thời gian thực WebSocket STOMP, bảo mật kép Session CSRF và JWT Bearer Token, dịch vụ đám mây Cloudinary lưu trữ hình ảnh và dịch vụ gửi mail Brevo OTP API.*
>
> *Sau đây, từng thành viên trong nhóm xin được tự giới thiệu và báo cáo các phần nghiệp vụ mình phụ trách."*

> **Lần lượt từng thành viên (Quay mặt vào webcam):**
> - **`[Thành viên A]`:** *"Em tên là [Họ và tên], MSSV [Mã số SV]. Trong đồ án này, em tham gia phụ trách: Thiết kế cơ sở dữ liệu Entity JPA, xây dựng giao diện khách hàng, tích hợp gửi email OTP kích hoạt tài khoản qua Brevo và chức năng giỏ hàng, lọc sản phẩm theo từng chi nhánh."*
> - **`[Thành viên B]`:** *"Em tên là [Họ và tên], MSSV [Mã số SV]. Em tham gia phụ trách: Xử lý nghiệp vụ đặt hàng (Order Workflow), cơ chế trừ tồn kho thời gian thực có khóa chống race condition (Pessimistic Lock), tích hợp cổng thanh toán MoMo Sandbox và hệ thống ví điện tử nội bộ."*
> - **`[Thành viên C]`:** *"Em tên là [Họ và tên], MSSV [Mã số SV]. Em tham gia phụ trách: Phân hệ Quản lý chi nhánh (Manager Workspace), bàn làm việc nhân viên (Staff Workspace), tích hợp Live Chat tư vấn hoa thời gian thực qua WebSocket STOMP và hệ thống chuông thông báo Realtime."*
> - **`[Thành viên D]`:** *"Em tên là [Họ và tên], MSSV [Mã số SV]. Em tham gia phụ trách: Cấu hình bảo mật Spring Security kép, triển khai REST API với JWT Stateless, tích hợp Cloudinary upload ảnh và thiết lập bộ kiểm thử tự động 880 ca test đảm bảo chất lượng hệ thống."*

---

<a name="scene-2"></a>
### 🎬 PHÂN CẢNH 2: LUỒNG KHÁCH HÀNG — ĐĂNG KÝ, OTP BREVO & DUYỆT SẢN PHẨM
* **Thời lượng:** 02:00 - 04:30 (2.5 phút)
* **Người trình bày:** `[Thành viên A]` (Bật webcam góc phải)
* **Thao tác màn hình:**
  1. Mở trang chủ [http://localhost:8080](http://localhost:8080), bấm nút **Đăng ký**.
  2. Nhập thông tin đăng ký với một email thật (ví dụ: hòm thư Gmail của thành viên).
  3. Bấm "Đăng ký" $\rightarrow$ Màn hình chuyển sang trang nhập mã OTP 6 chữ số.
  4. Mở tab Gmail kiểm tra hộp thư đến $\rightarrow$ Thấy ngay email gửi từ thương hiệu **Hoa Tươi Sài Gòn** (`lygiahan.786@gmail.com`) chứa mã OTP 6 số.
  5. Dán mã OTP vào web $\rightarrow$ Thông báo: *"Tài khoản kích hoạt thành công!"*.
  6. Đăng nhập vào hệ thống với tài khoản vừa tạo (hoặc tài khoản test `local_customer`).
  7. Quay về Trang chủ:
     - Giới thiệu giao diện Floral Theme với tone màu cành lá tự nhiên (`#355447`) và kem hồng pastel ấm áp.
     - Thử nghiệm thanh tìm kiếm và bấm mở ngăn kéo **"Tất cả bộ lọc" (Offcanvas Filter)**.
     - Chọn tiêu chí: Chi nhánh **"Quận 1 - Bến Thành"**, mức giá từ 500k - 1 triệu, dịp tặng **"Hoa Khai trương"**.
     - Bấm **"Áp dụng"** $\rightarrow$ Danh sách lập tức lọc các mẫu hoa có sẵn hàng tại kho Quận 1.
* **Lời thuyết minh mẫu:**
  > *"Trước tiên, em xin demo luồng đăng ký tài khoản với cơ chế bảo mật xác thực OTP qua Email. Khi người dùng đăng ký, hệ thống gọi trực tiếp Brevo API gửi mã OTP bảo mật 6 số có hiệu lực trong 5 phút. Sau khi nhập đúng mã OTP, tài khoản được kích hoạt trạng thái ACTIVE.*
  >
  > *Tại trang chủ, hệ thống được thiết kế theo Floral Theme đồng bộ. Điểm nổi bật là bộ lọc chi nhánh đa cấp: khách hàng có thể chọn xem mẫu hoa có sẵn hàng tại từng chi nhánh cụ thể. Cơ chế lọc hỗ trợ chọn nháp (Draft Filter), chỉ khi bấm 'Áp dụng' mới thực thi lọc, giúp tối ưu trải nghiệm người dùng."*

---

<a name="scene-3"></a>
### 🎬 PHÂN CẢNH 3: LIVE CHAT WEBSOCKET STOMP & ĐẶT HÀNG TRỪ KHO
* **Thời lượng:** 04:30 - 07:00 (2.5 phút)
* **Người trình bày:** `[Thành viên A/B]`
* **Thao tác màn hình:**
  1. Click vào một mẫu hoa cụ thể: ví dụ *"Bó hoa hồng đỏ Ohara Đà Lạt"*.
  2. Giới thiệu trang chi tiết sản phẩm:
     - Ảnh sắc nét tải từ Cloudinary (`flower_shop_...`).
     - Tồn kho hiển thị chính xác theo chi nhánh đang chọn.
     - Phần đánh giá sao thực tế kèm avatar khách hàng và ảnh chụp hoa thực nhận.
  3. Bấm nút **"Chat với cửa hàng"** (Icon bóng chat góc phải dưới):
     - Widget chat mở ra, tự động đính kèm thẻ mẫu hoa khách đang xem kèm giá tiền.
     - Nhập tin nhắn: *"Shop ơi mẫu này còn hoa tươi giao trong 2 tiếng tới Quận 1 không?"* $\rightarrow$ Bấm Gửi.
  4. Quay sang cửa sổ trình duyệt bên cạnh (Cửa sổ của Quản lý Q1):
     - Tin nhắn của khách xuất hiện tức thì qua WebSocket STOMP mà không cần F5!
     - Quản lý trả lời: *"Dạ chi nhánh Q1 luôn sẵn hoa mới về sáng nay ạ, bạn đặt hàng shop giao liền nhé!"*.
     - Tin nhắn phản hồi lập tức hiện trên màn hình của khách hàng.
  5. Khách hàng bấm **"Thêm vào giỏ hàng"** $\rightarrow$ Icon giỏ hàng trên header nhảy số tăng lên 1.
* **Lời thuyết minh mẫu:**
  > *"Hệ thống Live Chat được xây dựng trên công nghệ WebSocket STOMP qua thư viện SockJS. Khung chat tự động truyền ngữ cảnh sản phẩm hoa khách đang quan tâm, giúp nhân viên chi nhánh tư vấn chính xác. Dữ liệu tin nhắn được phân luồng độc lập theo từng chi nhánh và lưu vết kiểm toán chặt chẽ."*

---

<a name="scene-4"></a>
### 🎬 PHÂN CẢNH 4: THANH TOÁN ĐA KÊNH & TRỪ KHO CHỐNG RACE CONDITION
* **Thời lượng:** 07:00 - 09:30 (2.5 phút)
* **Người trình bày:** `[Thành viên B]`
* **Thao tác màn hình:**
  1. Bấm vào Giỏ hàng $\rightarrow$ Chọn **"Tiến hành đặt hoa"**.
  2. Tại trang Checkout:
     - Nhập thông tin người nhận, địa chỉ giao hoa, lời chúc thiệp tặng đính kèm.
     - Nhập mã giảm giá: `FLOWER10` $\rightarrow$ Hệ thống tự động giảm giá 10% tổng đơn hàng.
  3. Lựa chọn phương thức thanh toán:
     - Demo 1: Chọn **MoMo Sandbox QR** $\rightarrow$ Bấm Đặt hàng $\rightarrow$ Chuyển sang cổng MoMo test, hiển thị mã QR thanh toán chuẩn quốc tế.
     - Demo 2: Hoặc chọn **Ví hoa tươi nội bộ (Customer Wallet)** $\rightarrow$ Hệ thống kiểm tra số dư ví khả dụng, trừ tiền trực tiếp trên ví an toàn.
  4. Bấm Hoàn tất đơn hàng $\rightarrow$ Chuyển đến trang Xác nhận đơn hàng thành công với mã đơn `#...`.
  5. Mở CSDL hoặc trang chi tiết sản phẩm kiểm tra: Số lượng tồn kho của chi nhánh Quận 1 bị trừ ngay lập tức đúng bằng số lượng vừa mua.
* **Lời thuyết minh mẫu:**
  > *"Tại khâu thanh toán, hệ thống hỗ trợ đa kênh linh hoạt: từ COD, chuyển khoản ngân hàng, quét mã MoMo Sandbox đến thanh toán bằng Ví hoa tươi nội bộ. Điểm cốt lõi về kỹ thuật ở đây là cơ chế trừ kho an toàn: hệ thống áp dụng Pessimistic Write Lock (`SELECT FOR UPDATE`) ở tầng CSDL, đảm bảo dù nhiều khách hàng cùng bấm thanh toán bông hoa cuối cùng cùng một giây thì số lượng tồn kho cũng không bao giờ bị âm."*

---

<a name="scene-5"></a>
### 🎬 PHÂN CẢNH 5: LUỒNG VẬN HÀNH CHI NHÁNH — QUẢN LÝ (MANAGER)
* **Thời lượng:** 09:30 - 12:00 (2.5 phút)
* **Người trình bày:** `[Thành viên C]`
* **Thao tác màn hình:**
  1. Chuyển sang trình duyệt của Quản lý chi nhánh (`local_manager` - Chi nhánh Bến Thành Q1).
  2. Chỉ vào chuông thông báo trên Header: Icon chuông rung và nhảy số đếm thông báo có đơn hàng mới vừa được tạo.
  3. Vào menu **"Đơn hàng chi nhánh"**:
     - Thấy đơn hoa vừa đặt ở trạng thái `PENDING` (Chờ xác nhận).
     - Bấm duyệt đơn $\rightarrow$ Chuyển sang `CONFIRMED`.
     - Bấm chuyển tiếp sang trạng thái `PREPARING` (Đang cắm hoa).
  4. Quay lại trình duyệt của Khách hàng:
     - Nút "Hủy đơn hàng" của khách lập tức bị khóa/ẩn đi, vì shop đã bắt đầu cắt cành cắm hoa, khách không thể tự ý hủy ngang.
  5. Vào menu **"Quản lý kho hoa" (`/manager/inventory`)**:
     - Xem tồn kho thực tế, điều chỉnh số lượng tồn hoa hồng Ohara, thêm mẫu hoa mới của chi nhánh.
  6. Vào menu **"Ví cửa hàng" (`/manager/wallet`)**:
     - Xem số dư doanh thu vừa được cộng sau đơn hàng, xem lịch sử biến động số dư và demo tạo yêu cầu rút tiền về tài khoản ngân hàng.
  7. Chứng minh bảo mật IDOR:
     - Thử nhập URL đơn hàng của chi nhánh Quận 7 (`/manager/orders/99`) $\rightarrow$ Hệ thống chặn 403 Forbidden qua `StoreAccessService`!
* **Lời thuyết minh mẫu:**
  > *"Khu vực Quản lý chi nhánh được tách biệt hoàn toàn quyền hạn. Khi khách đặt hoa, thông báo nhảy số thời gian thực qua STOMP Topic. Khi đơn chuyển sang trạng thái PREPARING (Đang chuẩn bị hoa), hệ thống áp dụng ràng buộc nghiệp vụ không cho khách hủy đơn tùy tiện. Đồng thời, `StoreAccessService` kiểm soát chặt chẽ ranh giới chi nhánh, ngăn chặn triệt để lỗ hổng truy cập trái phép IDOR giữa các cửa hàng."*

---

<a name="scene-6"></a>
### 🎬 PHÂN CẢNH 6: LUỒNG BÀN LÀM VIỆC NHÂN VIÊN (STAFF WORKSPACE)
* **Thời lượng:** 12:00 - 13:30 (1.5 phút)
* **Người trình bày:** `[Thành viên C]`
* **Thao tác màn hình:**
  1. Đăng xuất và đăng nhập tài khoản Nhân viên (`local_staff`).
  2. Truy cập bàn làm việc nhân viên (`/staff/workspace`):
     - Giao diện được tối ưu tinh gọn dành riêng cho thợ cắm hoa và nhân viên quầy.
     - Hiển thị danh sách các đơn hoa cần cắm trong ngày, lời chúc thiệp mừng đi kèm.
  3. Nhân viên chọn đơn hoa `#...` vừa nhận $\rightarrow$ Bấm **"Đã cắm hoa xong & Đóng gói"** $\rightarrow$ Chuyển trạng thái sang `DELIVERING` (Đang giao).
  4. Bấm hoàn tất giao hoa $\rightarrow$ Chuyển sang `COMPLETED`.
  5. Chứng minh phân quyền: Nhân viên không thấy menu cấu hình cửa hàng hay ví tiền của Quản lý.
* **Lời thuyết minh mẫu:**
  > *"Phân hệ Staff Workspace được xây dựng riêng biệt giúp đội ngũ nhân viên cửa hàng tập trung vào nghiệp vụ cắm hoa và đóng gói đơn. Giao diện trực quan, lược bỏ các chức năng tài chính nhạy cảm của quản lý, đảm bảo tính phân quyền chặt chẽ trong chuỗi."*

---

<a name="scene-7"></a>
### 🎬 PHÂN CẢNH 7: LUỒNG QUẢN TRỊ TOÀN CHUỖI — QUẢN TRỊ VIÊN (ADMIN) & AUDIT LOG
* **Thời lượng:** 13:30 - 15:30 (2.0 phút)
* **Người trình bày:** `[Thành viên D]`
* **Thao tác màn hình:**
  1. Đăng nhập tài khoản Quản trị viên tối cao (`local_admin`).
  2. Xem **Admin Dashboard**:
     - Biểu đồ thống kê doanh thu toàn chuỗi, tổng số đơn hàng, số lượng mẫu hoa đang lưu hành trên toàn bộ 4 chi nhánh (Q1, Q7, Thủ Đức, Cao Lãnh).
  3. Quản lý danh mục hoa (`/admin/categories`):
     - Phân cấp danh mục cha/con (ví dụ: Hoa tình yêu $\rightarrow$ Hoa hồng, Hoa baby).
  4. Quản lý Voucher toàn chuỗi:
     - Tạo mã voucher mới, cấu hình giới hạn lượt dùng và ngày hết hạn.
  5. Quản lý kiểm toán bảo mật (**Audit Log**):
     - Mở trang `Chat Access Audits`: Hiển thị rõ danh sách các lần Admin hoặc Quản lý truy cập vào nội dung tin nhắn của khách hàng, ghi lại `timestamp`, `admin_id`, `conversation_id` để chống lạm quyền soi mói đời tư khách hàng.
* **Lời thuyết minh mẫu:**
  > *"Admin là vai trò cao nhất, có quyền quản trị toàn bộ các chi nhánh trong chuỗi hoa. Bên cạnh các chức năng CRUD danh mục và thống kê doanh thu, hệ thống của chúng em đặc biệt chú trọng đến an toàn thông tin với bảng `ChatAccessAudit`, ghi vết minh bạch mọi thao tác kiểm duyệt dữ liệu trò chuyện của người dùng."*

---

<a name="scene-8"></a>
### 🎬 PHÂN CẢNH 8: DEMO REST API VỚI JWT & BỘ KIỂM THỬ TỰ ĐỘNG (880 TESTS)
* **Thời lượng:** 15:30 - 17:00 (1.5 phút)
* **Người trình bày:** `[Thành viên D]`
* **Thao tác màn hình:**
  1. Mở Postman hoặc Swagger UI:
     - Gửi request `POST /api/auth/login` với tài khoản khách hàng $\rightarrow$ Nhận chuỗi `access_token` JWT chuẩn Bearer.
     - Gọi endpoint lấy danh sách sản phẩm `/api/products` không cần token $\rightarrow$ Trả JSON 200 OK.
     - Gọi endpoint bảo mật `/api/user/profile` không kèm token $\rightarrow$ Bị chặn 401 Unauthorized.
     - Đính kèm header `Authorization: Bearer <token>` $\rightarrow$ Trả dữ liệu JSON 200 OK thành công!
  2. Mở Terminal / PowerShell:
     - Chạy lệnh kiểm thử tự động toàn diện:
       ```powershell
       mvn test -Pisolated-tests
       ```
     - Chờ kết quả hiển thị dòng xanh rực rỡ:
       ```text
       [INFO] Tests run: 880, Failures: 0, Errors: 0, Skipped: 12
       [INFO] BUILD SUCCESS
       ```
     - Chạy tiếp bộ test JavaScript:
       ```powershell
       node --test src/test/js/*.test.cjs
       # 185 / 185 tests passed!
       ```
* **Lời thuyết minh mẫu:**
  > *"Để đáp ứng kiến trúc hiện đại, hệ thống tích hợp cơ chế bảo mật kép: Web MVC dùng Session Cookie + CSRF token, trong khi toàn bộ REST API `/api/**` hoạt động theo mô hình Stateless bảo vệ bằng JWT Bearer Token. Về chất lượng phần mềm, nhóm đã xây dựng bộ kiểm thử đồ sộ với 880 ca test Java và 185 ca test JavaScript, bao phủ từ Unit test, Integration test đến Concurrency test và UI test, đảm bảo mã nguồn đạt độ tin cậy cao nhất."*

---

<a name="scene-9"></a>
### 🎬 PHÂN CẢNH 9: TỔNG KẾT & LỜI CẢM ƠN
* **Thời lượng:** 17:00 - 18:00 (1.0 phút)
* **Khung hình:** Tất cả thành viên bật camera (Webcam Grid View).
* **Lời thoại & Hành động:**

> **Trưởng nhóm (Đại diện kết luận):**
> *"Kính thưa Thầy/Cô, qua video demo vừa rồi, nhóm chúng em đã trình bày trọn vẹn quy trình nghiệp vụ thực tế của hệ thống Website bán hoa tươi chuỗi — Hoa Tươi Sài Gòn trên cả 4 vai trò: Khách hàng, Quản lý chi nhánh, Nhân viên và Quản trị viên.
>
> Toàn bộ mã nguồn đã được đồng bộ trên GitHub, cơ sở dữ liệu đã được xuất ra 2 tệp script hoàn chỉnh cho PostgreSQL và SQL Server nộp kèm theo tài liệu hướng dẫn import 1 click và bảng dữ liệu Prompt AI hỗ trợ theo đúng chuẩn Rubrics của môn học.
>
> Nhóm chúng em xin chân thành cảm ơn Thầy/Cô đã theo dõi và tận tình giảng dạy, hướng dẫn chúng em trong suốt học kỳ vừa qua. Nhóm rất mong nhận được những lời nhận xét, góp ý quý báu của Thầy/Cô trong buổi bảo vệ sắp tới để hoàn thiện sản phẩm tốt hơn nữa. Chúng em xin chân thành cảm ơn!"*
>
> *(Tất cả thành viên cùng cúi chào cảm ơn).*

---

<a name="phan-4-tai-khoan"></a>
# 4. BẢNG TÀI KHOẢN VÀ DỮ LIỆU MẪU SỬ DỤNG TRONG VIDEO

Để quá trình quay video diễn ra trơn tru, nhóm sử dụng bộ tài khoản test chuẩn hóa (mật khẩu chung: `LocalTest123!`):

| Vai trò trong Video | Username | Password | Vai trò & Chi nhánh demo |
|---|---|---|---|
| **Khách hàng (Customer)** | `local_customer` | `LocalTest123!` | Khách mua hoa, test giỏ hàng, chat STOMP & ví khách |
| **Quản lý (Manager)** | `local_manager` | `LocalTest123!` | Quản lý chi nhánh Quận 1 (Bến Thành), duyệt đơn, xem ví shop |
| **Nhân viên (Staff)** | `local_staff` | `LocalTest123!` | Nhân viên chi nhánh Quận 1, cắm hoa tại `/staff/workspace` |
| **Quản trị viên (Admin)** | `local_admin` | `LocalTest123!` | Quản trị toàn hệ thống, duyệt shop, audit log |

* **Mã giảm giá demo:** `FLOWER10` (Giảm 10% cho đơn hàng).
* **Chi nhánh demo chính:** Chi nhánh Quận 1 (Bến Thành) và Chi nhánh Quận 7 (Phú Mỹ Hưng).
* **Email test nhận OTP thật:** Sử dụng chính hòm thư Gmail của thành viên nhóm.

---

<a name="phan-5-checklist"></a>
# 5. CHECKLIST PHÒNG TRÁNH LỖI KHI QUAY VIDEO

Để không phải quay đi quay lại nhiều lần, các thành viên cần kiểm tra kỹ 7 mục sau trước khi bấm nút Record:

- [ ] **1. Âm thanh & Micro:** Test thử mic 30 giây trước khi quay, đảm bảo âm thanh rõ ràng, không bị rè, không có tiếng ồn môi trường.
- [ ] **2. Ánh sáng & Camera:** Ngồi ở nơi đủ sáng, camera hướng thẳng khuôn mặt, không bị ngược sáng hoặc tối đen.
- [ ] **3. Trình duyệt sạch sẽ:** Đóng toàn bộ các tab cá nhân không liên quan (Facebook, YouTube, tin nhắn riêng tư), chỉ để lại các tab phục vụ demo đồ án.
- [ ] **4. Mở sẵn 2 trình duyệt:** Đăng nhập sẵn `local_customer` trên Chrome và `local_manager` trên Edge để tránh mất thời gian gõ mật khẩu lặp lại.
- [ ] **5. Kiểm tra kết nối Internet:** Đảm bảo mạng ổn định để Brevo gửi OTP về Gmail và Cloudinary nạp ảnh mượt mà.
- [ ] **6. Phóng to màn hình hợp lý:** Nhấn `Ctrl +` phóng to trình duyệt lên mức **110% - 125%** để Giảng viên xem video trên điện thoại hay laptop đều thấy rõ từng chữ, số tiền và mã đơn hàng.
- [ ] **7. Tinh thần & Tác phong:** Nói to, rõ ràng, dứt khoát, tự tin; phối hợp nhịp nhàng giữa lời nói và thao tác chuột.
