# BÁO CÁO TOÀN DIỆN ĐỒ ÁN MÔN LẬP TRÌNH WEB
## HỆ THỐNG WEBSITE THƯƠNG MẠI ĐIỆN TỬ BÁN HOA THEO MÔ HÌNH CHUỖI CỬA HÀNG
### DỰ ÁN: HOA TƯƠI SÀI GÒN

---

> **Tài liệu thuyết minh đồ án:**
> Báo cáo này được biên soạn chuẩn mực phục vụ bảo vệ đồ án môn Lập trình Web. Nội dung bám sát 100% mã nguồn thực tế của hệ thống, phản ánh chính xác kiến trúc đa tầng Spring Boot, cơ chế phân quyền, xử lý giao dịch cơ sở dữ liệu quan hệ, các kịch bản trình diễn thực nghiệm và bộ câu hỏi phản biện chuyên sâu.

---

## MỤC LỤC
1. [Phần 1: Giới thiệu Đề tài & Khảo sát Thực tế](#phan-1)
2. [Phần 2: Kiến trúc Hệ thống & Ngăn xếp Công nghệ](#phan-2)
3. [Phần 3: Phân tích Thiết kế & Đặc tả Vai trò Nghiệp vụ](#phan-3)
4. [Phần 4: Các Điểm nhấn Kỹ thuật Nổi bật](#phan-4)
5. [Phần 5: Kịch bản Thuyết trình & Trình diễn Thực tế](#phan-5)
6. [Phần 6: Danh sách Tài khoản & Dữ liệu Thử nghiệm](#phan-6)
7. [Phần 7: Bộ Câu hỏi Phản biện Giảng viên & Hướng dẫn Trả lời](#phan-7)
8. [Phần 8: Kết luận & Hướng phát triển Đề tài](#phan-8)
9. [Tài liệu tham khảo](#tai-lieu-tham-khao)

---

<a name="phan-1"></a>
## PHẦN 1: GIỚI THIỆU ĐỀ TÀI & KHẢO SÁT THỰC TẾ

### 1.1. Tổng quan về mô hình chuỗi cửa hàng và thương mại điện tử

#### 1.1.1. Đặc điểm quản lý chuỗi cửa hàng hoa tươi
Chuỗi cửa hàng hoa tươi là mô hình gồm nhiều chi nhánh tại các quận khác nhau, cùng hoạt động dưới sự điều hành chung của một ban quản lý. Mô hình này là sự kết hợp giữa việc quản lý tập trung toàn chuỗi và phục vụ khách hàng trực tiếp tại từng cửa hàng. Ở cấp độ toàn chuỗi, người quản lý chịu trách nhiệm xây dựng thương hiệu, thiết lập danh mục mẫu hoa chuẩn, ban hành giá bán chung và theo dõi chất lượng dịch vụ. Ngược lại, những yếu tố thay đổi mỗi ngày như lượng hoa tươi còn trong kho, thợ cắm hoa sẵn có và phạm vi nhận giao hàng lại gắn liền với từng cửa hàng cụ thể. Sự kết hợp này đòi hỏi hệ thống trang web phải phân rõ ranh giới dữ liệu, bảo đảm các chi nhánh không nhìn thấy hay can thiệp vào kho hoa và doanh thu của nhau.

Từ yêu cầu trên, hệ thống của đề tài tách riêng thông tin sản phẩm và thông tin kho hàng thay vì gộp chung làm một. Thông tin sản phẩm là phần mô tả chung trên toàn hệ thống, bao gồm tên gọi, hình ảnh mẫu, giá niêm yết và kiểu dáng hoa do ban quản lý duyệt. Trong khi đó, dữ liệu kho của từng chi nhánh phản ánh đúng số lượng hoa mà cửa hàng đang có và trạng thái sẵn sàng phục vụ tại thời điểm đó. Cách tổ chức này giúp trang web luôn giữ được danh mục hoa đồng bộ, đẹp mắt, đồng thời bảo đảm mỗi đơn đặt hàng của khách luôn được kiểm tra chính xác theo lượng hoa thực tế tại cửa hàng trực tiếp nhận làm hoa.

#### 1.1.2. Thương mại điện tử trong kinh doanh hoa tươi
Kênh thương mại điện tử giúp khách hàng dễ dàng tiếp cận danh mục mẫu hoa và gửi yêu cầu đặt mua nhanh chóng mà không cần phải đến trực tiếp cửa hàng. Đối với hoa tươi, một đơn đặt hàng không chỉ dừng lại ở việc chọn sản phẩm, mà luôn gắn liền với các thông tin riêng biệt như ngày giờ nhận hoa, hình thức nhận tại quầy hay giao tận nơi, thông tin liên lạc của người nhận và nội dung lời chúc ghi trên thiệp. Trang web vì thế vừa phải mô tả trực quan kiểu dáng và hình ảnh của mẫu hoa, vừa phải thu thập đầy đủ những chi tiết dặn dò này để cửa hàng có thể chuẩn bị đúng mẫu và giao hoa đúng theo mong muốn của người mua.

Về mặt xử lý giao dịch, giá cả và số lượng hoa hiển thị trên màn hình người mua chỉ mang tính tham khảo cho đến khi đơn hàng được máy chủ xác nhận thành công. Một hệ thống bán hoa hiệu quả phải giải quyết cùng lúc hai mục tiêu: vừa tạo sự thuận tiện, dễ dàng cho khách hàng khi mua sắm, vừa giữ được sự chính xác tuyệt đối trong khâu quản lý của cửa hàng. Nếu trang web chỉ chăm chút cho giao diện bên ngoài mà thiếu đi các bước kiểm tra chặt chẽ ở phía sau, những tình huống như bán quá số lượng hoa thực có trong kho hay ghi nhận sai lệch tiền bạc vẫn hoàn toàn có thể xảy ra.

---

### 1.2. Khảo sát một số website tương tự và so sánh

Hai trang web được lựa chọn để khảo sát thực tế là **Hoa Yêu Thương** và **Flower Corner** vì đây là những đơn vị cung cấp dịch vụ đặt hoa tươi trực tuyến phổ biến hiện nay. Quá trình đối chiếu dựa trên các giao diện công khai ghi nhận vào ngày 28/10/2026, tập trung vào cách sắp xếp danh mục sản phẩm, phương thức tiếp nhận thông tin giao nhận và cách hỗ trợ khách hàng, nhằm rút ra bài học kinh nghiệm để xây dựng đề tài [1], [2].

#### 1.2.1. Website Hoa Yêu Thương
Trang web Hoa Yêu Thương sắp xếp các mẫu hoa rất phong phú theo nhiều tiêu chí như chủ đề sự kiện, đối tượng tặng, kiểu dáng, loại hoa và tông màu chủ đạo; đồng thời có riêng trang hỏi đáp hướng dẫn chi tiết các bước đặt hàng, giao nhận và tra cứu đơn hoa. Điểm mạnh của cách làm này là danh mục bám sát thói quen tìm kiếm của người mua, giúp khách dễ dàng chọn hoa bắt đầu từ một dịp tặng cụ thể thay vì phải nhớ chính xác tên của mẫu hoa. Tuy nhiên, việc có quá nhiều lớp phân loại cũng đòi hỏi thanh điều hướng và bộ lọc tìm kiếm phải thật rõ ràng, để người mua luôn nhận biết được mình đang xem các mẫu hoa theo những tiêu chí lọc nào [1].

```text
[CHÈN HÌNH 1.1 TẠI ĐÂY]
(Chèn ảnh chụp giao diện trang chủ hoặc danh mục của hoayeuthuong.com ghi nhận ngày 28/10/2026)
```
*Hình 1.1. Giao diện khảo sát website Hoa Yêu Thương*

#### 1.2.2. Website Flower Corner
Trang web Flower Corner phân chia hoa theo các nhóm chính như hoa sinh nhật, hoa khai trương, hoa theo chủ đề và một số dòng hoa phổ biến; đồng thời công khai rõ ràng các chính sách thanh toán, hướng dẫn nhận hoa và xử lý khiếu nại. Ưu điểm nổi bật là các thông tin dịch vụ này được bố trí rất thuận tiện ngay trong quá trình khách xem và lựa chọn mẫu hoa. Dưới góc nhìn thiết kế của đề tài, điểm cần lưu ý là người mua đôi khi phải mở nhiều trang hướng dẫn khác nhau mới nắm hết được các quy định giao nhận; vì vậy, những điều kiện quan trọng của đơn hàng nên được tóm tắt đầy đủ và rõ ràng ngay tại bước xác nhận thanh toán [2].

```text
[CHÈN HÌNH 1.2 TẠI ĐÂY]
(Chèn ảnh chụp giao diện flowercorner.vn thể hiện danh mục và lối vào thông tin giao hàng ghi nhận ngày 28/10/2026)
```
*Hình 1.2. Giao diện khảo sát website Flower Corner*

#### 1.2.3. Bảng so sánh và bài học thiết kế cho hệ thống

| Tiêu chí so sánh | Hoa Yêu Thương [1] | Flower Corner [2] | Hệ thống của Đề tài (Hoa Tươi Sài Gòn) |
| :--- | :--- | :--- | :--- |
| **Quản lý kho hoa** | Bán tập trung một đầu mối, chia việc ngầm cho các cửa hàng phía sau | Bán tập trung, chia đơn về xưởng theo địa chỉ nhận của khách | **Tách biệt rõ ràng:** Ban quản lý giữ mẫu hoa chuẩn, từng chi nhánh quản lý kho hoa thực tế của mình |
| **Chọn chi nhánh mua hoa** | Khách hàng không được chọn chi nhánh cắm hoa | Khách hàng không được chọn chi nhánh theo ý muốn | **Khách tự chọn chi nhánh gần nhất:** Cho phép lọc xem hoa theo từng chi nhánh để nhận hoa nhanh trong 1 đến 2 giờ |
| **Kiểm tra kho lúc mua** | Dùng số lượng ước lượng, thiếu hoa thì gọi khách xin đổi sau | Kiểm tra lượng hoa thủ công tại xưởng | **Khóa giữ hoa tức thời:** Trừ bớt kho hoa của chi nhánh ngay lúc thanh toán, triệt tiêu lỗi bán quá số lượng có sẵn |
| **Trao đổi mẫu hoa** | Dùng số tổng đài hoặc khung chat gắn ngoài | Dẫn sang ứng dụng nhắn tin mạng xã hội bên ngoài | **Khung chat có sẵn trên web:** Cho phép thợ gửi ảnh hoa thật đã cắm xong cho khách duyệt trước khi đem giao |
| **Quy định hủy đơn** | Gọi điện thoại lên tổng đài xin hủy thủ công | Gọi điện thoại nhờ nhân viên trực hủy bằng tay | **Tự động khóa hủy đơn:** Khách được tự hủy khi đơn mới tạo; khi tiệm đã chuyển sang cắm hoa thì hệ thống tự khóa nút hủy |

---

### 1.3. Mục tiêu xây dựng của đề tài
Từ việc nghiên cứu mô hình kinh doanh và khảo sát thực tiễn, đề tài đặt mục tiêu xây dựng trọn vẹn website thương mại điện tử Hoa Tươi Sài Gòn đáp ứng:
1. **Mô hình chuỗi đa chi nhánh:** Quản lý độc lập kho hoa, nhân sự, đơn hàng và doanh thu của từng chi nhánh; ban quản lý toàn chuỗi điều phối danh mục mẫu hoa chuẩn.
2. **Khóa giữ kho tức thời:** Ứng dụng khóa bi quan trong cơ sở dữ liệu để loại bỏ 100% tình trạng bán vượt quá số lượng hoa có sẵn khi nhiều khách cùng mua một mẫu hoa.
3. **Bảo vệ chi phí nguyên liệu hoa tươi:** Tự động khóa quyền hủy đơn của khách khi chi nhánh đã chuyển sang trạng thái bắt đầu cắm hoa.
4. **Bảo mật hai tầng:** Kết hợp quản lý phiên làm việc cùng mã bảo vệ biểu mẫu cho giao diện web, đi kèm mã xác thực độc lập cho các cổng giao diện lập trình.
5. **Giao tiếp thời gian thực:** Tích hợp tính năng trò chuyện trực tiếp giữa khách hàng và chi nhánh phụ trách, tự động đính kèm thẻ mẫu hoa đang xem để tiện trao đổi.
6. **Kiểm soát dòng tiền minh bạch:** Quản lý ví tiền của khách hàng và ví doanh thu của từng chi nhánh với cơ chế đối soát rõ ràng.

---

<a name="phan-2"></a>
## PHẦN 2: KIẾN TRÚC HỆ THỐNG & NGĂN XẾP CÔNG NGHỆ

### 2.1. Kiến trúc phân tầng ứng dụng
Hệ thống được thiết kế theo mô hình phân tầng chặt chẽ nhằm phân định ranh giới trách nhiệm rõ ràng:

```text
┌────────────────────────────────────────────────────────────────────────┐
│ 1. TẦNG GIAO DIỆN NGƯỜI DÙNG (PRESENTATION LAYER)                       │
│    Thymeleaf Template Engine, Bootstrap 5.3.3, Vanilla JavaScript      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 2. TẦNG BẢO MẬT KÉP (DUAL SECURITY GATEWAY)                            │
│    - Web MVC: Phiên làm việc (Session Cookie) + Bảo vệ biểu mẫu CSRF   │
│    - REST API: Mã xác thực Bearer JWT + Cơ chế xoay vòng Refresh Token │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 3. TẦNG ĐIỀU PHỐI (CONTROLLER LAYER)                                   │
│    CustomerController, StaffController, ManagerController,             │
│    AdminController, ChatWebSocketController, RestApiController         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 4. TẦNG NGHIỆP VỤ (SERVICE & BUSINESS LOGIC LAYER)                     │
│    OrderService (Khóa bi quan), OrderWorkflowService (Vòng đời đơn),   │
│    OrderFundsService (Ví chi nhánh), CustomerWalletService (Ví khách), │
│    StoreAccessService (Kiểm soát chi nhánh), CloudinaryService         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 5. TẦNG TRUY XUẤT & LƯU TRỮ DỮ LIỆU (PERSISTENCE LAYER)                │
│    Spring Data JPA (Hibernate), PostgreSQL / SQL Server, Flyway        │
└────────────────────────────────────────────────────────────────────────┘
```

### 2.2. Bảng đối chiếu công nghệ sử dụng trong dự án

| Thành phần | Công nghệ | Phiên bản / Đặc tính | Vai trò trong hệ thống |
|:---|:---|:---|:---|
| **Nền tảng thực thi** | Java SDK | JDK 21 LTS | Tận dụng hiệu năng cao của phiên bản Java dài hạn và tính năng luồng ảo. |
| **Khung phát triển** | Spring Boot | 4.1.1 | Quản lý phụ thuộc tự động, khởi tạo ứng dụng và cấu hình các thành phần. |
| **Dựng giao diện** | Thymeleaf | Thymeleaf Spring 6 | Tạo trang HTML tại máy chủ an toàn, tích hợp kiểm tra quyền trực tiếp trên giao diện. |
| **Khung hiển thị** | Bootstrap & Icons | Bootstrap 5.3.3 | Thiết kế giao diện tương thích, bố cục co giãn tốt trên máy tính và điện thoại. |
| **Cơ sở dữ liệu chính** | PostgreSQL (Aiven Cloud) | PostgreSQL 18.6 (SSL verify-full) | Lưu trữ chính khi chạy ứng dụng (profile `aiven`), kiểm soát truy cập an toàn. |
| **Cơ sở dữ liệu phát triển** | Microsoft SQL Server | SQLEXPRESS / FlowersShop | Môi trường phát triển nội bộ cục bộ (profile `dev`). |
| **Quản lý dữ liệu** | Spring Data JPA | Hibernate 7.x | Ánh xạ đối tượng quan hệ, thực thi lệnh khóa bi quan khi trừ số lượng kho. |
| **Quản lý cấu trúc bảng** | Flyway | Flyway Core | Quản lý lịch sử nâng cấp bảng dữ liệu (SQL Server V1–V17; PostgreSQL baseline V1). |
| **Bảo mật hệ thống** | Spring Security | Spring Security 6.x | Phân quyền hai lớp: Phiên làm việc cho web Thymeleaf, mã JWT cho các cổng API. |
| **Truyền tin thời gian thực** | Spring WebSocket | STOMP over SockJS | Đẩy tin nhắn trò chuyện và cập nhật thông báo tức thời giữa khách và chi nhánh. |
| **Lưu trữ đám mây** | Cloudinary CDN | Cloudinary Java SDK | Tải lên và quản lý ảnh sản phẩm, ảnh đại diện và ảnh gửi trong khung chat. |
| **Dịch vụ thư điện tử** | Brevo API / JavaMail | REST API / SMTP | Gửi thư điện tử chứa mã xác thực 6 chữ số khi khách hàng quên mật khẩu. |

---

<a name="phan-3"></a>
## PHẦN 3: PHÂN TÍCH THIẾT KẾ & ĐẶC TẢ VAI TRÒ NGHIỆP VỤ

### 3.1. Phân rã các nhóm chức năng hệ thống
Hệ thống được tổ chức thành sáu nhóm chức năng chính gắn kết chặt chẽ với nhau:
1. **Truy cập và xác thực:** Quản lý việc đăng ký, đăng nhập và khôi phục mật khẩu qua mã OTP gửi về thư điện tử, cung cấp cơ chế phiên làm việc cho người dùng duyệt web và cấp mã JWT cho các cổng giao tiếp API.
2. **Tra cứu sản phẩm công khai:** Cho phép khách hàng tìm kiếm mẫu hoa, lọc theo nhiều tiêu chí như dịp tặng, kiểu dáng, mức giá và lọc theo từng chi nhánh đang mở cửa để tra cứu số lượng hoa có sẵn mà không bắt buộc tạo tài khoản.
3. **Mua hàng và chăm sóc khách hàng:** Xử lý việc quản lý giỏ hàng, đặt hàng chọn ngày giờ nhận và vùng cự ly giao hoa, tích hợp các phương thức thanh toán tiền mặt, chuyển khoản, ví tiền và thanh toán qua điện thoại, đồng thời cho phép khách theo dõi tiến trình đơn và tự hủy đơn khi cửa hàng chưa bắt đầu cắm hoa.
4. **Vận hành chi nhánh:** Phục vụ trực tiếp cho nhân viên cắm hoa và quản lý tiệm trong việc tiếp nhận đơn hoa, bấm xác nhận bắt đầu cắm hoa để kích hoạt quy tắc bảo vệ đơn hàng, cập nhật số lượng hoa trong kho và theo dõi số dư ví tiền của cửa hàng.
5. **Quản trị dữ liệu nền:** Cung cấp công cụ cho quản trị viên toàn chuỗi quản lý danh mục mẫu hoa chuẩn, điều phối mạng lưới các chi nhánh, phân công nhân sự quản lý, cấu hình tỷ lệ hoa hồng và ban hành biểu phí vận chuyển thống nhất.
6. **Hỗ trợ, thông báo và thống kê:** Thiết lập kênh trò chuyện trực tiếp giữa khách hàng và thợ cắm hoa có đính kèm thẻ thông tin sản phẩm, gửi thông báo trạng thái đơn hàng tức thời qua WebSocket, đồng thời cung cấp các bảng thống kê doanh thu và nhật ký giám sát hoạt động cho cấp quản lý.

Sự phân nhóm này giúp liên kết chặt chẽ từng tác nhân sử dụng với chuỗi nghiệp vụ thực tế của chuỗi tiệm hoa, thay vì nhìn nhận các thao tác trên giao diện như những yêu cầu chức năng rời rạc.

### 3.2. Đặc tả vai trò nghiệp vụ theo tác nhân
Hệ thống phân định ranh giới chức năng rõ ràng cho 5 nhóm vai trò sử dụng:

```text
                       ┌─────────────────────────┐
                       │   HOA TƯƠI SÀI GÒN      │
                       └────────────┬────────────┘
         ┌──────────────────┬───────┴────────┬──────────────────┐
         ▼                  ▼                ▼                  ▼
┌─────────────────┐ ┌───────────────┐ ┌───────────────┐ ┌────────────────┐
│ Khách vãng lai  │ │  Khách hàng   │ │ Nhân viên tiệm│ │ Quản lý tiệm   │
│   (Anonymous)   │ │    (USER)     │ │    (STAFF)    │ │   (MANAGER)    │
└─────────────────┘ └───────────────┘ └───────┬───────┘ └───────┬────────┘
                                              └────────┬────────┘
                                                       ▼
                                              ┌────────────────┐
                                              │ Quản trị viên  │
                                              │    (ADMIN)     │
                                              └────────────────┘
```

### 3.1. Phân hệ Khách vãng lai (Anonymous)
- **Duyệt xem mẫu hoa theo dịp:** Xem các bộ sưu tập hoa sinh nhật, hoa khai trương, hoa chúc mừng, hoa tình yêu và hoa chia buồn.
- **Lọc theo chi nhánh đang mở cửa:** Chọn xem các mẫu hoa đang có sẵn tại từng chi nhánh tiệm hoa cụ thể.
- **Xem chi tiết mẫu hoa:** Xem bộ ảnh chụp nhiều góc, giá niêm yết, kiểu dáng, chủng loại hoa và đánh giá của khách đã mua.
- **Không tạo tài khoản rác:** Cho phép xem thông tin và thêm hoa vào giỏ hàng tạm thời mà không ép buộc phải đăng ký tài khoản.

### 3.2. Phân hệ Khách hàng (`ROLE_USER`)
- **Quản lý tài khoản:** Đăng ký, đăng nhập, cập nhật thông tin cá nhân, đổi mật khẩu và khôi phục mật khẩu qua mã xác thực gửi về thư điện tử.
- **Đặt hoa và chọn chi nhánh phục vụ:** Chọn tiệm hoa gần nơi nhận nhất để rút ngắn thời gian giao hoa và kiểm tra đúng lượng hoa còn trong kho.
- **Cung cấp thông tin đặt hoa chi tiết:** Chọn ngày giờ nhận hoa, chọn nhận tại quầy hoặc giao tận nơi theo các vùng cự ly, nhập tên cùng số điện thoại người nhận và lời chúc in trên thiệp.
- **Quyền tự hủy đơn hàng hợp lý:** Khách hàng được tự bấm hủy đơn khi đơn còn ở bước chờ tiệm nhận hoặc đã duyệt. Khi tiệm hoa bấm bắt đầu cắm hoa, hệ thống tự động khóa tính năng hủy đơn.
- **Nhắn tin trực tiếp với chi nhánh:** Bấm nút nhắn tin từ trang chi tiết mẫu hoa, hệ thống tự động ghim hình ảnh và thông tin của mẫu hoa đó vào cuộc trò chuyện để trao đổi cùng người thợ.

### 3.3. Phân hệ Nhân viên cắm hoa tại chi nhánh (`ROLE_STAFF`)
- **Tiếp nhận đơn hoa tại chi nhánh:** Xem danh sách các đơn hoa được giao cho chi nhánh mình phụ trách.
- **Chuyển bước chuẩn bị hoa:** Bấm xác nhận bắt đầu chuẩn bị hoa khi bắt tay vào cắm hoa, kích hoạt quy tắc bảo vệ đơn hàng của tiệm.
- **Bàn giao hoa và cập nhật trạng thái:** Cập nhật tiến độ giao hoa cho đến khi hoa được chuyển thành công tới tay người nhận.
- **Trao đổi với khách hàng:** Trò chuyện trực tiếp với khách mua hoa qua khung chat, chụp ảnh bó hoa thật vừa cắm xong gửi cho khách xem trước khi đi giao.

### 3.4. Phân hệ Quản lý chi nhánh (`ROLE_MANAGER`)
- **Bảng theo dõi hoạt động:** Theo dõi doanh thu trong ngày, số lượng đơn hoa cần xử lý và số lượng hoa còn trong kho.
- **Quản lý kho hoa chi nhánh (`StoreInventory`):** Cập nhật số lượng hoa thực tế có trong kho lạnh của tiệm, bật hoặc tắt trạng thái nhận đơn đối với từng mẫu hoa.
- **Quản lý nhân viên chi nhánh:** Theo dõi danh sách nhân viên cắm hoa và giao hoa thuộc quyền quản lý của chi nhánh.
- **Theo dõi ví tiền chi nhánh (`OrderFundsService`):** Quản lý số tiền đang chờ duyệt sau khi hoàn thành đơn và số tiền khả dụng có thể rút hoặc đối soát.
- **Cấu hình biểu phí vận chuyển chi nhánh:** Thiết lập mức phí giao hoa theo từng vùng cự ly xung quanh cửa hàng.

### 3.5. Phân hệ Quản trị viên toàn chuỗi (`ROLE_ADMIN`)
- **Quản lý danh mục mẫu hoa chuẩn:** Tạo mới, chỉnh sửa thông tin mô tả, cập nhật hình ảnh và gán danh mục chính cùng danh mục phụ cho các mẫu hoa trên toàn hệ thống.
- **Quản lý mạng lưới chi nhánh:** Mở chi nhánh tiệm hoa mới, cập nhật thông tin liên hệ và tạm ngưng hoặc kích hoạt hoạt động của các chi nhánh.
- **Bổ nhiệm nhân sự quản lý:** Phân công tài khoản quản lý và nhân viên cho từng chi nhánh thông qua bảng quan hệ thành viên.
- **Giám sát hội thoại chỉ đọc:** Xem các cuộc trao đổi giữa khách hàng và các chi nhánh để kiểm soát chất lượng phục vụ.
- **Nhật ký giám sát bất biến (`chat_access_logs`):** Tự động ghi lại thời điểm và danh tính người quản trị mỗi khi mở xem nội dung hội thoại của chi nhánh, bảo đảm tính minh bạch tuyệt đối.

---

<a name="phan-4"></a>
## PHẦN 4: CÁC ĐIỂM NHẤN KỸ THUẬT NỔI BẬT

### 4.1. Khóa giữ kho tức thời chống bán vượt quá số lượng
- **Vấn đề thực tế:** Khi một mẫu giỏ hoa chỉ còn đủ nguyên liệu để cắm 1 giỏ duy nhất tại tiệm Quận 1, nếu 2 khách hàng cùng bấm đặt mua trong cùng một khoảnh khắc, việc không có cơ chế khóa sẽ khiến cả 2 đơn đều thành công, dẫn đến tình trạng kho bị âm hoặc tiệm không có hoa giao cho khách.
- **Giải pháp kỹ thuật:** Hệ thống áp dụng cơ chế khóa bi quan trong Spring Data JPA:
  ```java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT si FROM StoreInventory si WHERE si.store.id = :storeId AND si.product.id = :productId")
  Optional<StoreInventory> findByStoreIdAndProductIdForUpdate(Long storeId, Long productId);
  ```
- **Quy trình hoạt động:** Khi giao dịch đặt hàng diễn ra, câu lệnh truy vấn sẽ khóa dòng dữ liệu kho tương ứng. Yêu cầu thứ hai phải đợi cho đến khi yêu cầu thứ nhất hoàn tất việc kiểm tra và trừ kho. Nếu lượng hoa còn lại bằng không, yêu cầu thứ hai lập tức nhận được thông báo hết hàng, triệt tiêu hoàn toàn lỗi bán quá số lượng có sẵn.

### 4.2. Bảo vệ chi phí hoa tươi trong quy trình xử lý đơn
- Hoa tươi sau khi đã cắt tỉa cành lá và cắm vào mút xốp sẽ không thể tháo rời ra để cắm lại cho đơn khác mà không làm dập nát hoa.
- Hệ sinh thái nghiệp vụ của hệ thống (`OrderWorkflowService`) quy định:
  - Khi đơn hàng ở trạng thái mới tạo hoặc đã duyệt: Khách hàng có quyền tự bấm hủy đơn, hệ thống sẽ tự động cộng lại số lượng hoa vào kho của chi nhánh.
  - Khi nhân viên chi nhánh bấm chuyển sang trạng thái bắt đầu cắm hoa (`PREPARING`): Nút hủy của khách lập tức bị khóa. Mọi yêu cầu hủy gửi từ giao diện đều bị tầng nghiệp vụ từ chối.
  - Chỉ quản lý chi nhánh hoặc người quản trị toàn chuỗi mới có quyền hủy đơn trong trường hợp bất khả kháng và bắt buộc phải nhập lý do giải trình để lưu vết.

### 4.3. Kiến trúc bảo mật kép phối hợp
Hệ thống triển khai song song hai chuỗi kiểm soát bảo mật độc lập:
- **Chuỗi thứ nhất dành cho giao diện web:** Quản lý thông tin đăng nhập qua phiên làm việc với mã nhận diện lưu trong cookie có cờ bảo vệ an toàn, đồng thời tự động kiểm tra mã bảo vệ biểu mẫu nhằm chống lại các hành vi giả mạo yêu cầu từ trang web độc hại.
- **Chuỗi thứ hai dành cho các cổng giao tiếp API:** Hoạt động hoàn toàn không lưu phiên làm việc, xác thực thông qua mã Bearer JWT gửi kèm trên phần đầu của yêu cầu mạng.
- **Cơ chế xoay vòng mã làm mới:** Mỗi khi cấp mới mã truy cập, hệ thống tự động sinh ra mã làm mới mới và thu hồi mã cũ, kịp thời phát hiện và vô hiệu hóa tài khoản nếu phát hiện mã cũ bị sao chép trái phép.

### 4.4. Kênh truyền tin thời gian thực phân quyền theo chi nhánh
- Kênh liên lạc qua giao thức STOMP trên nền WebSocket được kiểm soát chặt chẽ thông qua tầng kiểm tra quyền chi nhánh (`StoreAccessService`).
- Nhân viên của một chi nhánh chỉ được phép lắng nghe và nhận tin nhắn gửi về hòm thư của chính chi nhánh mình phụ trách, ngăn chặn việc nhân viên ở tiệm hoa này đọc trộm tin nhắn của tiệm hoa khác.
- Tự động gắn thẻ thông tin mẫu hoa: Khi khách hàng nhấn nút nhắn tin từ trang chi tiết sản phẩm, khung trò chuyện tự động mang theo ảnh đại diện, tên mẫu hoa và giá bán để người thợ nắm bắt ngay nhu cầu mà không cần hỏi lại khách.

### 4.5. Phân loại danh mục kép linh hoạt
- **Danh mục chính:** Phân loại theo kiểu dáng sản phẩm như Bó hoa, Giỏ hoa, Lẵng hoa hay Kệ hoa, phục vụ cho công tác kiểm kê và hạch toán tại các chi nhánh.
- **Danh mục phụ:** Phân loại theo dịp tặng như Hoa Sinh Nhật, Hoa Khai Trương, Hoa Tình Yêu hay Hoa Chia Buồn. Một bó hoa hồng đỏ vừa có thể thuộc danh mục Hoa Sinh Nhật, vừa xuất hiện trong danh mục Hoa Tình Yêu, giúp khách hàng tìm kiếm thuận tiện mà không cần tạo lặp dữ liệu sản phẩm trong kho.

### 4.6. Kiểm soát cấu trúc cơ sở dữ liệu với Flyway Migration
- Tuyệt đối không để công cụ tự động thay đổi cấu trúc bảng khi khởi động ứng dụng trên môi trường thực tế nhằm bảo đảm an toàn dữ liệu.
- Quản lý toàn bộ lịch sử thay đổi bảng thông qua các tệp nâng cấp có đánh số phiên bản rõ ràng: cơ sở dữ liệu phát triển SQL Server có chuỗi nâng cấp từ V1 đến V17; cơ sở dữ liệu chính PostgreSQL sở hữu một phiên bản khởi đầu độc lập chứa toàn bộ cấu trúc bảng chuẩn hiện hành.

---

<a name="phan-5"></a>
## PHẦN 5: KỊCH BẢN THUYẾT TRÌNH & TRÌNH DIỄN THỰC TẾ

### Phân bổ thời gian thuyết trình (12 - 15 phút):
1. **Phút 00 - 02:** Giới thiệu mục tiêu đề tài, tính đặc thù của chuỗi hoa tươi và kiến trúc 5 tầng.
2. **Phút 02 - 05:** Trình diễn phía Khách hàng: Lọc hoa theo chi nhánh, chọn vùng giao hoa, ghi lời chúc và đặt đơn.
3. **Phút 05 - 08:** Trình diễn phía Quản lý: Kiểm chứng trừ kho tức thời, bấm cắm hoa và chứng minh khóa quyền hủy đơn của khách.
4. **Phút 08 - 11:** Trình diễn kết nối thời gian thực: Khách nhắn tin với chi nhánh kèm thẻ hoa, Admin giám sát kèm nhật ký truy cập.
5. **Phút 11 - 13:** Báo cáo kiểm thử tự động, tổng kết kết quả và mở phần hỏi đáp.

### Kịch bản chi tiết từng bước:
- **Bước 1: Trình diễn Khách hàng đặt hoa (Trình duyệt thứ nhất)**
  1. Mở trang chủ `http://localhost:8080/`, chọn xem mẫu hoa của Chi nhánh Quận 1.
  2. Bấm vào xem chi tiết một mẫu hoa, đăng nhập tài khoản khách hàng `khachhang` (mật khẩu `123456`).
  3. Thêm hoa vào giỏ và tiến hành thanh toán: Chọn hình thức giao tận nơi, chọn vùng cự ly gần để tính phí vận chuyển, nhập nội dung thiệp chúc mừng và bấm xác nhận đặt mua.
  4. Đơn hàng mới được tạo thành công ở trạng thái chờ xác nhận.

- **Bước 2: Trình diễn Quản lý chi nhánh & Chứng minh trừ kho (Trình duyệt thứ hai)**
  1. Đăng nhập tài khoản Quản lý Quận 1: `manager_q1` (mật khẩu `123456`).
  2. Mở trang quản lý kho hoa: Chỉ ra số lượng hoa của mẫu vừa đặt đã bị trừ ngay một đơn vị.
  3. Mở trang quản lý đơn hàng: Bấm xác nhận đơn, sau đó bấm tiếp nút bắt đầu cắm hoa.
  4. Quay lại màn hình của khách hàng, tải lại trang chi tiết đơn: Nút hủy đơn của khách đã bị khóa hoàn toàn kèm thông báo đơn hoa đang được cắm.

- **Bước 3: Trình diễn trò chuyện trực tiếp kèm thẻ mẫu hoa**
  1. Tại màn hình khách hàng, bấm nút nhắn tin với cửa hàng tại trang chi tiết mẫu hoa.
  2. Khung chat hiện ra thẻ mẫu hoa, khách gửi tin nhắn dặn dò đổi màu giấy gói.
  3. Màn hình quản lý chi nhánh nhận ngay tin nhắn trong tích tắc mà không cần tải lại trang.
  4. Mở tab quản trị viên với tài khoản `admin` (mật khẩu `123456`), vào mục giám sát hội thoại để xem nội dung chat ở chế độ chỉ đọc, sau đó mở trang nhật ký để thấy dòng ghi nhận lượt truy cập của quản trị viên.

---

<a name="phan-6"></a>
## PHẦN 6: DANH SÁCH TÀI KHOẢN & DỮ LIỆU THỬ NGHIỆM

Tất cả các tài khoản thử nghiệm trên hệ thống đều sử dụng mật khẩu chung: `123456`

| Vai trò | Tên đăng nhập | Mật khẩu | Chi nhánh / Nhiệm vụ | Đường dẫn truy cập |
|:---|:---|:---:|:---|:---|
| **Khách vãng lai** | *(Không cần đăng nhập)* | — | Duyệt xem toàn bộ hệ thống | `http://localhost:8080/` |
| **Khách hàng (`USER`)** | `khachhang` | `123456` | Mua hoa, giỏ hàng, theo dõi đơn | `http://localhost:8080/profile` |
| **Nhân viên tiệm (`STAFF`)** | `staff_q1` | `123456` | Cắm hoa, giao hàng tại Chi nhánh Quận 1 | `http://localhost:8080/staff/orders` |
| **Quản lý tiệm (`MANAGER`)** | `manager_q1` | `123456` | Quản lý kho hoa, đơn và ví Chi nhánh Q1 | `http://localhost:8080/manager/dashboard` |
| **Quản trị viên (`ADMIN`)** | `admin` | `123456` | Quản trị toàn bộ chuỗi tiệm hoa | `http://localhost:8080/admin/dashboard` |

### Mạng lưới các chi nhánh trên hệ thống:
1. **Chi Nhánh Bến Thành (Quận 1):** `123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM` - Hotline: `0281111222`
2. **Chi Nhánh Phú Mỹ Hưng (Quận 7):** `456 Nguyễn Thị Thập, Phường Tân Hưng, Quận 7, TP.HCM` - Hotline: `0283333444`
3. **Chi Nhánh Làng Đại Học (TP. Thủ Đức):** `789 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức` - Hotline: `0285555666`
4. **Chi Nhánh Cao Lãnh:** `112 An Bình, TP. Cao Lãnh, Tỉnh Đồng Tháp` - Hotline: `08730123123`

---

<a name="phan-7"></a>
## PHẦN 7: BỘ CÂU HỎI PHẢN BIỆN GIẢNG VIÊN & HƯỚNG DẪN TRẢ LỜI

#### Câu 1: Tại sao nhóm lại xây dựng cả cơ chế phiên làm việc và mã JWT trong cùng một dự án?
> **Hướng dẫn trả lời:**
> Hệ thống áp dụng kiến trúc bảo mật hai tầng để tối ưu cho từng mục đích sử dụng:
> - Đối với người dùng duyệt web qua trình duyệt với Thymeleaf, nhóm sử dụng phiên làm việc kết hợp mã bảo vệ biểu mẫu CSRF để ngăn chặn tối đa nguy cơ giả mạo yêu cầu từ trang web độc hại.
> - Đối với các cổng giao tiếp API độc lập, nhóm sử dụng mã xác thực Bearer JWT không lưu phiên để sẵn sàng phục vụ cho việc tích hợp ứng dụng di động hoặc các dịch vụ đối tác sau này mà không làm tăng tải bộ nhớ của máy chủ.

#### Câu 2: Làm thế nào để giải quyết tình trạng hai người cùng bấm mua một mẫu hoa cuối cùng trong kho?
> **Hướng dẫn trả lời:**
> Nhóm xử lý bài toán này bằng cơ chế khóa bi quan trong Spring Data JPA thông qua chỉ dẫn `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Khi khách hàng thực hiện thanh toán trong giao dịch, dòng dữ liệu kho tương ứng của chi nhánh sẽ được khóa lại. Yêu cầu của người mua thứ hai buộc phải chờ cho đến khi giao dịch của người thứ nhất hoàn tất. Nếu người thứ nhất đã mua hết số lượng, người thứ hai sẽ nhận được thông báo hết hàng ngay lập tức, loại bỏ hoàn toàn nguy cơ bán vượt quá số lượng có sẵn.

#### Câu 3: Hệ thống phòng chống tấn công chèn câu lệnh SQL và giả mạo yêu cầu như thế nào?
> **Hướng dẫn trả lời:**
> - Về phòng chống chèn câu lệnh SQL: Nhóm sử dụng Spring Data JPA và Hibernate với cơ chế truyền tham số an toàn trong các câu truy vấn, tuyệt đối không cộng nối chuỗi câu lệnh SQL thô.
> - Về phòng chống giả mạo yêu cầu: Hệ thống bảo mật tự động chèn mã bảo vệ ẩn vào tất cả các biểu mẫu gửi dữ liệu. Máy chủ sẽ kiểm tra tính hợp lệ của mã này trước khi xử lý yêu cầu, ngăn chặn các hành vi gửi yêu cầu giả mạo từ trang web khác.

#### Câu 4: Kênh truyền WebSocket STOMP khác gì so với việc gửi yêu cầu kiểm tra định kỳ và cách phân quyền tin nhắn ra sao?
> **Hướng dẫn trả lời:**
> Việc liên tục gửi yêu cầu sau mỗi vài giây gây tốn băng thông và tăng tải không cần thiết cho máy chủ. Ngược lại, WebSocket thiết lập một kênh kết nối liên tục hai chiều duy nhất. Về phân quyền, mỗi chi nhánh chỉ được phép đăng ký lắng nghe hòm thư của chính chi nhánh mình. Tầng nghiệp vụ luôn kiểm tra quyền quản lý chi nhánh trước khi cho phép nhận hoặc gửi tin, bảo đảm tính bảo mật giữa các chi nhánh.

#### Câu 5: Tại sao hệ thống lại thiết kế phân loại một danh mục chính và nhiều danh mục phụ?
> **Hướng dẫn trả lời:**
> Đây là thiết kế tối ưu trong các hệ thống thương mại điện tử hiện đại. Danh mục chính phân loại theo kiểu dáng như Bó hoa, Giỏ hoa hay Kệ hoa để phục vụ cho việc kiểm kê kho hàng và quản lý sổ sách. Danh mục phụ phân loại theo dịp tặng như Hoa Sinh Nhật hay Hoa Khai Trương để đáp ứng thói quen tìm kiếm của người mua. Thiết kế này giúp một mẫu hoa có thể xuất hiện ở nhiều dịp tặng khác nhau mà không phải sao chép lặp lại dữ liệu sản phẩm trong kho.

#### Câu 6: Làm thế nào để tải ảnh lên Cloudinary an toàn mà không làm lộ mã bí mật?
> **Hướng dẫn trả lời:**
> Quy trình tải ảnh được xử lý hoàn toàn tại máy chủ. Trình duyệt gửi tệp ảnh lên máy chủ Spring Boot qua kết nối an toàn. Máy chủ đọc mã khóa bí mật từ biến môi trường của hệ thống, thực hiện ký xác thực và truyền tệp lên dịch vụ lưu trữ đám mây. Giao diện phía người dùng hoàn toàn không tiếp cận mã bí mật mà chỉ nhận lại đường dẫn hình ảnh an toàn do máy chủ trả về.

#### Câu 7: Cơ chế xóa mềm sản phẩm tại chi nhánh hoạt động như thế nào?
> **Hướng dẫn trả lời:**
> Khi chi nhánh ngưng kinh doanh một mẫu hoa, hệ thống không xóa hẳn dòng dữ liệu trong cơ sở dữ liệu vì sẽ làm hỏng liên kết đến các đơn hàng cũ trong lịch sử. Thay vào đó, hệ thống sử dụng cờ trạng thái để ẩn mẫu hoa khỏi giao diện mua sắm của chi nhánh, vừa đáp ứng yêu cầu ngừng bán vừa bảo toàn nguyên vẹn lịch sử đơn hàng và doanh thu đã ghi nhận.

#### Câu 8: Tại sao lại tự động khóa quyền hủy đơn của khách khi đơn chuyển sang bước bắt đầu cắm hoa?
> **Hướng dẫn trả lời:**
> Đây là quy tắc nghiệp vụ đặc thù để bảo vệ cửa hàng hoa tươi. Khi nhân viên chi nhánh chuyển trạng thái sang bắt đầu cắm hoa, hoa tươi đã được cắt cành và cắm vào mút xốp theo mẫu. Nếu khách hủy đơn vào lúc này, cửa hàng sẽ chịu thiệt hại về nguyên liệu vì hoa cắm dở không thể cắm lại cho đơn khác. Do đó, hệ thống tự động khóa nút hủy đơn của khách và chỉ cho phép quản lý chi nhánh hủy trong trường hợp bất khả kháng kèm theo lý do giải trình.

#### Câu 9: Tại sao nhóm lại sử dụng Flyway để quản lý bảng dữ liệu thay vì để công cụ tự tạo bảng?
> **Hướng dẫn trả lời:**
> Việc để công cụ tự động thay đổi bảng dữ liệu rất dễ làm sai lệch cấu trúc hoặc làm mất dữ liệu khi đưa lên môi trường thực tế. Flyway giúp quản lý mọi thay đổi của cơ sở dữ liệu thông qua các tệp kịch bản có đánh số phiên bản tuần tự, toàn bộ lịch sử nâng cấp được lưu lại minh bạch và cho phép tự động hóa quy trình triển khai phần mềm một cách an toàn.

#### Câu 10: Chỉ số tồn kho trên trang điều hành của quản lý được cập nhật như thế nào?
> **Hướng dẫn trả lời:**
> Trên trang điều hành, chỉ số tồn kho được kết hợp cơ chế kiểm tra định kỳ tự động và nhận thông báo khi có đơn hàng mới phát sinh. Nhờ đó, ngay khi có khách đặt mua hoặc kho được nhập thêm hoa mới, các chỉ số trên màn hình của người quản lý sẽ tự động cập nhật số liệu mới mà không cần phải bấm tải lại toàn bộ trang web.

---

<a name="phan-8"></a>
## PHẦN 8: KẾT LUẬN & HƯỚNG PHÁT TRIỂN ĐỀ TÀI

### 8.1. Kết quả đạt được
1. **Hoàn thành toàn diện chuẩn mực môn học Lập trình Web:** Xây dựng hệ thống vững chắc từ kiến trúc đa tầng MVC, xử lý cơ sở dữ liệu quan hệ, cơ chế bảo mật hai tầng đến kênh truyền tin thời gian thực hiện đại.
2. **Nghiệp vụ thực tế sâu sắc và chính xác:** Giải quyết trọn vẹn bài toán vận hành chuỗi hoa tươi với cơ chế trừ kho tức thời, quản lý theo chi nhánh, bảo vệ hoa tươi trong quy trình đơn hàng và giám sát trao đổi khách hàng minh bạch.
3. **Mã nguồn sạch và độ tin cậy cao:** Các lớp xử lý nghiệp vụ được tinh gọn, cấu trúc giao diện phân tách rõ ràng và toàn bộ hệ thống được bảo đảm chất lượng thông qua hệ thống kiểm thử tự động toàn diện.

### 8.2. Hướng mở rộng trong tương lai
- **Tích hợp cổng thanh toán trực tuyến quốc gia:** Kết nối thêm các cổng thanh toán điện tử phổ biến với cơ chế xác thực thông báo thanh toán tự động.
- **Ứng dụng nhận diện mẫu hoa:** Phát triển tính năng hỗ trợ người mua tải ảnh hoa để hệ thống tự động tìm kiếm mẫu hoa tương đồng tại chi nhánh gần nhất.
- **Mở rộng ứng dụng trên thiết bị di động:** Kết nối trực tiếp vào các cổng giao tiếp API đã được xây dựng sẵn để phát triển ứng dụng di động cho khách hàng và nhân viên giao hoa.

---

<a name="tai-lieu-tham-khao"></a>
## TÀI LIỆU THAM KHẢO

- **[1] Hoa Yêu Thương**, *Hệ thống đặt hoa trực tuyến và điện hoa toàn quốc*, tham khảo ngày 28/10/2026 tại: `https://hoayeuthuong.com/`.
- **[2] Flower Corner**, *Dịch vụ đặt hoa tươi và quà tặng giao nhanh*, tham khảo ngày 28/10/2026 tại: `https://flowercorner.vn/`.
- **[3] Craig Walls**, *Spring in Action, Sixth Edition*, Manning Publications, 2022.
- **[4] Thymeleaf Team**, *Tutorial: Using Thymeleaf*, tài liệu trực tuyến tại: `https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html`.
- **[5] Bootstrap Team**, *Bootstrap 5 Documentation*, tài liệu trực tuyến tại: `https://getbootstrap.com/docs/5.3/`.
- **[6] Vlad Mihalcea**, *High-Performance Java Persistence*, Leanpub, 2016.
- **[7] PostgreSQL Global Development Group**, *PostgreSQL Documentation*, tài liệu trực tuyến tại: `https://www.postgresql.org/docs/`.
- **[8] Spring Framework Documentation**, *WebSocket, STOMP Messaging Architecture*, tài liệu trực tuyến tại: `https://docs.spring.io/spring-framework/reference/web/websocket.html`.
- **[9] Laurentiu Spilca**, *Spring Security in Action, Second Edition*, Manning Publications, 2023.
- **[10] Cloudinary**, *Cloudinary Java SDK Documentation*, tài liệu trực tuyến tại: `https://cloudinary.com/documentation/java_integration`.
- **[11] Oracle**, *JavaServer Pages (JSP) and JSTL Technology Syntax Reference*, Oracle Technology Network.
- **[12] OpenSymphony**, *SiteMesh Web Page Layout System Documentation*.
- **[13] Internet Engineering Task Force (IETF)**, *RFC 7519: JSON Web Token (JWT)*, 2015.
- **[14] Martin Fowler**, *Patterns of Enterprise Application Architecture*, Addison-Wesley, 2002.
- **[15] Nhóm phát triển đề tài**, *Mã nguồn và tài liệu kiến trúc hệ thống Hoa Tươi Sài Gòn*, cập nhật ngày 28/10/2026.
- **[16] Nhóm phát triển đề tài**, *Hướng dẫn kiểm thử và bằng chứng thực nghiệm hệ thống*, cập nhật ngày 28/10/2026.
