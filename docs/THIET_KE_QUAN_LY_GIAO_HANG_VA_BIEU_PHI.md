# THIẾT KẾ HỆ THỐNG QUẢN LÝ GIAO HÀNG & BIỂU PHÍ THEO CHI NHÁNH
*(Dành cho Quản trị viên và Đội ngũ phát triển FlowerShop)*

---

## 1. TỔNG QUAN KIẾN TRÚC & PHÂN TÁCH 3 PHẦN

Để giải quyết triệt để bài toán tính phí vận chuyển linh hoạt theo từng đơn hàng, khoảng cách và khu vực địa lý, hệ thống quản lý giao hàng của FlowerShop được thiết kế phân tách rõ ràng thành **3 phần độc lập**:

```
+-----------------------------------------------------------------------------------+
|                            HỆ THỐNG GIAO HÀNG FLOWERSHOP                           |
+-----------------------------------------------------------------------------------+
|  1. ĐƠN VỊ VẬN CHUYỂN      |  2. BIỂU PHÍ GIAO HÀNG      |  3. PHÍ GIAO CỦA ĐƠN   |
|  (Bên giao hàng)           |  (Quy tắc & Cấu hình)       |  (Dữ liệu thực tế)     |
|  - Tên đối tác / Đội xe    |  - Điểm gửi (Chi nhánh)     |  - Số tiền chốt đơn    |
|  - Hotline, người liên hệ  |  - Khu vực nhận             |  - Lưu bất biến vào DB |
|  - Phương tiện (xe máy/tải)|  - Loại dịch vụ (4h / 2h)   |  - Không bị ảnh hưởng  |
|  - Trạng thái hoạt động    |  - Mức phí + Phụ phí kệ hoa |    khi Admin đổi giá   |
+-----------------------------------------------------------------------------------+
```

### Bảng so sánh 3 phần quản lý

| Phần quản lý | Trọng tâm & Nội dung | Vai trò trong hệ thống |
| :--- | :--- | :--- |
| **Đơn vị vận chuyển** | Tên đơn vị, hotline hỗ trợ, phạm vi phục vụ, phương tiện chuyên dụng, trạng thái hoạt động. | Quản lý danh bạ đối tác vận chuyển (Ahamove, GrabExpress, Đội shipper nội bộ). **Không ép 1 mức phí cố định cho toàn bộ đơn hàng.** |
| **Biểu phí giao hàng** | Ma trận cấu hình: **Điểm gửi (Chi nhánh)** → **Khu vực nhận** → **Dịch vụ giao** (Tiêu chuẩn / Hỏa tốc) → **Mức phí cơ bản**, **Phụ phí kệ hoa lớn**, **Thời gian dự kiến**. | Căn cứ định giá cước tự động khi khách hàng nhập địa chỉ nhận hoa và chọn dịch vụ. |
| **Phí giao hàng của đơn** | Số tiền thực tế được tính toán tại thời điểm đặt đơn và lưu trực tiếp vào trường `shippingFee` của bảng `orders`. | Đảm bảo tính toàn vẹn kế toán: Khách thanh toán đúng số tiền đã cam kết; Admin thay đổi biểu phí sau đó tuyệt đối không làm sai lệch đơn cũ. |

---

## 2. MA TRẬN CẤU HÌNH BIỂU PHÍ GIAO HÀNG (CHI NHÁNH → KHU VỰC)

Website hoa tươi có đặc thù: Mỗi chi nhánh chuẩn bị hoa và giao trong bán kính tối ưu để giữ độ tươi của hoa. Do đó, biểu phí được cấu hình theo từng chi nhánh:

### Bảng biểu phí mẫu thực tế

| Điểm gửi (Chi nhánh) | Khu vực nhận (Bán kính) | Dịch vụ giao | Thời gian dự kiến | Mức phí cơ bản | Phụ phí kệ hoa / hàng lớn | Ghi chú vận hành |
| :--- | :--- | :--- | :--- | :---: | :---: | :--- |
| **Chi nhánh Bến Thành** | Khu vực gần chi nhánh (< 5 km) | **Tiêu chuẩn** | 4 – 6 giờ | **25.000 ₫** | + 20.000 ₫ | Xe máy có túi giữ ẩm hoa tươi |
| **Chi nhánh Bến Thành** | Khu vực nội thành xa hơn (5 – 15 km) | **Tiêu chuẩn** | 4 – 6 giờ | **40.000 ₫** | + 25.000 ₫ | Đóng thùng xốp bảo vệ cánh hoa |
| **Chi nhánh Bến Thành** | Khu vực gần chi nhánh (< 5 km) | **Hỏa tốc** | Trong 2 giờ | **50.000 ₫** | + 30.000 ₫ | Ưu tiên cắm và giao ngay trong lồng kính |
| **Chi nhánh Bến Thành** | Khu vực xa hơn (5 – 15 km) | **Hỏa tốc** | Trong 2 giờ | **70.000 ₫** | + 35.000 ₫ | Shipper chuyên biệt giao hoa & bánh kem |
| **Chi nhánh Phú Mỹ Hưng** | Khu vực Quận 7 & lân cận (< 5 km) | **Tiêu chuẩn** | 4 – 6 giờ | **25.000 ₫** | + 20.000 ₫ | Giao nhanh khu vực Nam Sài Gòn |
| **Chi nhánh Cao Lãnh** | Nội ô TP. Cao Lãnh (< 6 km) | **Tiêu chuẩn** | 3 – 5 giờ | **20.000 ₫** | + 15.000 ₫ | Đội xe nội bộ giao tận nhà |

> [!NOTE]
> **Quy tắc phụ phí hàng cồng kềnh (Bulky Surcharge):**
> Khi khách đặt Kệ hoa khai trương 2 tầng, Lẵng hoa chia buồn lớn hoặc Chậu hoa lan hồ điệp nhiều cành cần phương tiện riêng (xe ba gác hoặc xe bán tải thùng kín), hệ thống tự động cộng phụ phí tương ứng vào cước cơ bản để bù chi phí vận chuyển của đối tác.

---

## 3. LUỒNG TÍNH PHÍ VÀ ĐẶT HÀNG CỦA KHÁCH HÀNG (CHECKOUT FLOW)

Quy trình xử lý tính cước giao hàng diễn ra tự động qua 6 bước khép kín:

```
[Khách nhập địa chỉ & thời gian]
               │
               ▼
[Hệ thống xác định chi nhánh thực hiện đơn]
               │
               ▼
[Tra cứu biểu phí: Có phục vụ khu vực này không?]
      ├── KHÔNG ──► Báo lỗi: "Chưa hỗ trợ giao đến khu vực này"
      │
     CÓ
      │
      ▼
[Hiển thị các dịch vụ khả dụng: Tiêu chuẩn, Hỏa tốc...]
               │
               ▼
[Khách chọn dịch vụ -> Cập nhật tổng tiền tức thì (Subtotal + Shipping - Voucher)]
               │
               ▼
[Xác nhận đặt hàng -> Lưu bất biến cước phí vào Order.shippingFee]
```

### Chi tiết các bước thực hiện:

1. **Khách nhập địa chỉ và chọn ngày giờ nhận:**
   - Khách điền địa chỉ giao hàng và hẹn ngày giờ nhận mong muốn.
2. **Hệ thống xác định chi nhánh thực hiện đơn:**
   - Căn cứ vào vị trí địa chỉ nhận và tình trạng tồn kho (`storeInventory`) của các chi nhánh đang mở cửa (`isOpen == true`).
3. **Hiển thị dịch vụ giao hàng phù hợp:**
   - Hệ thống lọc ra các gói cước được cấu hình cho chi nhánh này.
   - Nếu địa chỉ **nằm ngoài phạm vi phục vụ** của toàn bộ biểu phí: Hệ thống báo thông báo rõ ràng: `“Chưa hỗ trợ giao đến khu vực này. Quý khách vui lòng chọn nhận hoa tại quầy hoặc liên hệ hotline để được hỗ trợ.”` (Tuyệt đối không mặc định cước bằng 0 để tránh bán lỗ chi phí vận chuyển).
4. **Khách chọn gói dịch vụ:**
   - Khi khách bấm chọn *Tiêu chuẩn (25.000 ₫)* hoặc *Hỏa tốc (50.000 ₫)*, giao diện Checkout gửi yêu cầu tính giá `/checkout/quote` và cập nhật ngay tổng tiền hiển thị trên màn hình.
5. **Cập nhật động khi thay đổi thông tin:**
   - Nếu khách đổi địa chỉ sang quận khác, đổi chi nhánh nhận hoặc thêm vào giỏ một kệ hoa lớn, hệ thống tự động chạy lại phép tính và cập nhật phí mới.
6. **Bảo toàn dữ liệu khi lưu đơn hàng (Immutability):**
   - Khi đơn hàng được tạo, số tiền cước vận chuyển được lưu cố định vào cột `orders.shipping_fee`.
   - Về sau, nếu Admin cập nhật giá biểu phí trên trang quản trị, các đơn hàng đã đặt trong quá khứ **hoàn toàn không bị ảnh hưởng**, bảo đảm đối soát kế toán luôn khớp 100%.

---

## 4. XỬ LÝ ĐƠN HÀNG GIAO TỪ NHIỀU CHI NHÁNH (SPLIT ORDERS)

Trong trường hợp khách hàng mua các sản phẩm hoa mà **không một chi nhánh đơn lẻ nào có đủ hàng tồn kho**, hệ thống sẽ chia giỏ hàng thành nhiều đơn hàng con theo từng chi nhánh:

- **Mỗi chi nhánh = 1 chuyến giao hàng riêng biệt.**
- **Biểu phí được tính riêng cho từng chuyến:**
  - Ví dụ:
    - Chuyến 1 (từ Chi nhánh Bến Thành giao Quận 1): 25.000 ₫
    - Chuyến 2 (từ Chi nhánh Phú Mỹ Hưng giao Quận 1): 40.000 ₫
    - **Tổng cước vận chuyển hiển thị cho khách:** `65.000 ₫` (Kèm bảng kê chi tiết từng chuyến để khách nắm rõ nguyên nhân phát sinh).

---

## 5. CẤU TRÚC GIAO DIỆN QUẢN TRỊ ADMIN

- **Tên menu Sidebar:** Đổi thành **“Quản lý giao hàng”** (Icon `bi-truck`), đường dẫn `/admin/deliveries`.
- **Bên trong gồm 2 Tab chuyên biệt:**
  - **Tab 1: Đơn vị vận chuyển:**
    - Danh sách các đối tác giao hàng (GrabExpress, Ahamove, Giao hàng tiêu chuẩn, Shipper nội bộ).
    - Quản lý tên đối tác, hotline, phạm vi phục vụ, phương tiện giao hoa, trạng thái hoạt động.
  - **Tab 2: Khu vực & Biểu phí:**
    - Bộ lọc theo Chi nhánh gửi và Loại dịch vụ.
    - Bảng biểu phí đa tầng (Chi nhánh → Khu vực → Dịch vụ → Phí cơ bản → Phụ phí kệ hoa → Thời gian).
    - Thao tác nhanh: Thêm biểu phí mới, Bật/Tắt áp dụng, Sửa hoặc Xóa biểu phí.

---
*Tài liệu kiến trúc hệ thống giao hàng và vận chuyển FlowerShop - Phiên bản chuẩn hóa 2026.*
