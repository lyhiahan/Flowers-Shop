# Đối soát tồn kho lệch chi nhánh — 15/10/2026

## Phạm vi và kết luận

Đã đọc database **RentalShop**, SQL Server **LAPTOP-N9Q3N4DV\SQLEXPRESS**, địa chỉ
`127.0.0.1,53735`. Snapshot đầu: **2026-10-15T14:44:40.138494+07:00**.
Chạy lại script lúc 14:48 cùng ngày cho cùng số dòng và tổng lượng lệch.

**Chỉ đối soát: không UPDATE/DELETE/INSERT, không chuyển kho, không đổi chủ sản phẩm,
không đổi trạng thái đơn, không chạy migration, không tác động thanh toán.**
Chưa tạo backup mới vì chưa thực hiện sửa dữ liệu. Phải backup và kiểm chứng khả năng
khôi phục trước bước xử lý dữ liệu nếu được duyệt.

Một dòng lệch là `store_inventories.store_id <> products.store_id`, khi sản phẩm có
chi nhánh sở hữu cụ thể. Sản phẩm chung (`products.store_id IS NULL`) không bị xem là lệch.

- Tổng kho: **153 dòng / 3.319 đơn vị** (tổng thô, không phải lượng bán được).
- Kho lệch: **81 dòng / 2.003 đơn vị**, thuộc **27 mẫu sản phẩm**.
- Phần không lệch: **72 dòng / 1.316 đơn vị** theo phép loại trừ; chưa áp thêm điều kiện
  mở bán, xóa mềm, đóng cửa nên không đồng nhất với KPI bán được.
- Không có số lượng âm. Không có sản phẩm lệch bị thiếu dòng kho ở chi nhánh sở hữu.
- Không thấy khóa ngoại trực tiếp tham chiếu `store_inventories`. Điều này **không**
  có nghĩa được xóa an toàn: hoàn kho tìm dòng theo cặp chi nhánh + sản phẩm.

## Tổng hợp theo chi nhánh đang giữ kho lệch

| Mã chi nhánh | Chi nhánh | Số dòng lệch | Đơn vị tồn lệch |
| --- | --- | --- | --- |
| 1 | Chi Nhánh Bến Thành (Quận 1) | 18 | 428 |
| 2 | Chi Nhánh Phú Mỹ Hưng (Quận 7) | 18 | 450 |
| 3 | Chi Nhánh Làng Đại Học (TP. Thủ Đức) | 18 | 450 |
| 7 | Tiệm Hoa Sài Gòn - Chi Nhánh Cao Lãnh | 27 | 675 |

## Phân loại để quyết định

| Nhóm | Số dòng | Số lượng | Hướng xử lý đề xuất |
| --- | --- | --- | --- |
| A — chưa thấy đơn liên quan | 77 | 1925 | Ứng viên lưu trữ sau khi xác nhận là dữ liệu demo; không tự cộng vào kho chủ |
| B — còn đơn chưa kết thúc | 3 | 53 | Giữ nguyên dòng kho để đơn cũ tiếp tục xử lý/hoàn kho |
| C — có lịch sử đơn đã hủy | 1 | 25 | Giữ nguyên để bảo toàn dấu vết đơn #22 và #23 |

Nhóm A đều có `quantity=25`, `version=0`. Đây là dấu hiệu dữ liệu khởi tạo lặp,
**không phải bằng chứng chắc chắn rằng 1.925 đơn vị không có thật**. Version có thể bị
bỏ qua khi sửa SQL thủ công. Chưa có chứng từ kiểm kê để xác nhận vị trí vật lý.

Cả 27 mẫu đã có dòng kho chính chủ. Nếu cộng 2.003 đơn vị lệch sang các dòng đó,
lượng kho chính chủ tăng mà chưa có chứng từ chuyển kho; không thực hiện tự động.

### Đơn hàng liên quan

| Đơn | Trạng thái | Chi nhánh xuất | Sản phẩm | Chi nhánh sở hữu | Số lượng đặt | Trạng thái thanh toán |
| --- | --- | --- | --- | --- | --- | --- |
| 18 | CONFIRMED | 1 | 21 | 2 | 1 | PAID |
| 19 | PENDING | 1 | 22 | 2 | 1 | UNPAID |
| 20 | PENDING | 1 | 25 | 2 | 20 | UNPAID |
| 22 | CANCELLED | 1 | 26 | 2 | 15 | UNPAID |
| 23 | CANCELLED | 1 | 26 | 2 | 15 | UNPAID |

- **#18:** còn CONFIRMED và đã PAID. Chỉ ghi nhận trạng thái; không can thiệp thanh toán
  do thành viên khác phụ trách. Kho #99 cần giữ để xử lý đơn/hoàn kho nếu phát sinh.
- **#19 và #20:** còn PENDING. Giữ kho #102 và #111; không tự hủy hoặc đổi chi nhánh.
- **#23 hiện là CANCELLED**, khác ảnh cũ từng ở PENDING. Đây là trạng thái đọc từ DB,
  không phải thao tác trong đợt đối soát này. #22 cũng CANCELLED, cùng dùng kho #114.
- 53 đơn vị ở nhóm B là **số còn trong ba dòng kho**, không phải số lượng đang giữ
  cho đơn. Ba đơn #18–20 có tổng số lượng đặt 22. Không trừ thêm 22 một lần nữa.
- `OrderWorkflowService.cancel()` dùng kho của `pickupStore` để hoàn số lượng.
  Xóa/chuyển dòng #99, #102, #111 trước khi xử lý đơn có thể khiến hủy đơn báo
  “Không tìm thấy tồn kho của đơn hàng”.

## Phương án cần duyệt trước khi sửa dữ liệu

**Khuyến nghị: lưu trữ thay vì cộng/chuyển/xóa vật lý.**

1. Xác nhận nhóm A là dữ liệu demo thừa, không phải hàng tồn thực cần di chuyển.
2. Tạo backup SQL Server, thử khôi phục trên bản sao; lưu snapshot 81 dòng với ID,
   lượng, version và các cờ. Không dùng báo cáo Markdown thay cho backup.
3. Thử lưu trữ **77 dòng nhóm A** trên bản sao: giữ ID, chi nhánh, sản phẩm và lượng;
   ngừng bán/xóa mềm với nhật ký trước–sau. Không cộng lượng sang chi nhánh khác.
4. Giữ nguyên **4 dòng nhóm B/C**, đặc biệt dòng #114 liên quan đơn #23. Việc đối soát
   sau này cần phân biệt dòng legacy đang phục vụ đơn với dòng đã lưu trữ.
5. Trước khi áp dụng DB gốc: đọc lại ID/version/quantity/cờ và kiểm tra liên kết đơn.
   Nếu khác snapshot, dừng và đối soát lại; không chỉ dùng `WHERE store_id <> ...`.
6. Áp dụng trong transaction ở thời điểm không có thao tác đặt/hủy đơn; kiểm tra
   count, kho chính chủ, lượng bán hợp lệ, toàn bộ đơn và đơn #23 không bị thay đổi.
7. Chỉ chuyển kho nếu người dùng xác nhận đây là hàng có thật và có nguồn/đích/số lượng
   cụ thể; đó là phương án khác, không suy ra chỉ từ trường chủ sở hữu sản phẩm.

**Chưa áp dụng phương án trên; cần xác nhận trước khi lưu trữ dữ liệu.**

## Công cụ đối soát đã kiểm thử

Chạy từ thư mục gốc dự án, dùng Windows Integrated Authentication:

```powershell
.\scripts\audit-branch-inventory.ps1
```

Script trả JSON gồm database/thời điểm, chi nhánh, sản phẩm, 81 dòng kho, đơn liên
quan và khóa ngoại. Không đọc email, điện thoại hay địa chỉ khách. SQL chỉ có
`SET NOCOUNT` và `SELECT`; không khởi động ứng dụng và scheduler.

Nguồn:
- `scripts/audit-branch-inventory.ps1`
- `scripts/sql/audit-branch-inventory.sql`
- `src/main/java/com/finalproject/service/ProductAvailability.java`
- `src/main/java/com/finalproject/service/OrderWorkflowService.java`
- `src/main/java/com/finalproject/repository/StoreInventoryRepository.java`

KPI/catalog hiện có điều kiện loại sản phẩm thuộc chi nhánh khác. Không mở bán lại
những dòng lệch chỉ để tăng số sản phẩm hiển thị.

## Danh mục 27 sản phẩm được đối soát

| Mã sản phẩm | Tên | Chi nhánh sở hữu |
| --- | --- | --- |
| 12 | Bó Hoa Hồng Đỏ Ecuador Nhập Khẩu 99 Bông 'Tình Yêu Vĩnh Cửu' | 1 |
| 13 | Bó Hoa Hướng Dương 'Mặt Trời Tỏa Sáng' | 1 |
| 14 | Kệ Hoa Khai Trương 'Hồng Phát Đại Cát' 2 Tầng | 1 |
| 15 | Giỏ Hoa Khai Trương 'Phát Tài Phát Lộc' Tone Cam Vàng | 1 |
| 16 | Hộp Hoa Tròn Sang Trọng 'Trái Tim Nồng Cháy' Hồng Đỏ | 1 |
| 17 | Chậu Lan Hồ Điệp 6 Cành Trắng Lưỡi Đỏ 'Thịnh Vượng' | 1 |
| 18 | Bình Hoa Hồng Ohara & Cúc Mẫu Đơn Để Bàn Sang Trọng | 1 |
| 19 | Bó Hoa Cưới Cầm Tay Cô Dâu Tone Trắng 'Nguyện Ước' | 1 |
| 20 | Kệ Hoa Chia Buồn 'Thành Kính Phân Ưu' Tone Trắng Tím | 1 |
| 21 | Hộp Hoa Sinh Nhật Tone Hồng Pastel 'Thiên Thần Ngọt Ngào' | 2 |
| 22 | Bó Hoa Cưới Cầm Tay Cô Dâu Tone Trắng Tinh Khôi | 2 |
| 23 | Bó Hoa Tulip Hà Lan Tone Hồng Pastel Hàn Quốc | 2 |
| 24 | Giỏ Hoa Sinh Nhật 'Thanh Xuân Rực Rỡ' Phối Hoa Baby | 2 |
| 25 | Kệ Hoa Khai Trương Hiện Đại Tone Đỏ Hồng 'Vạn Sự Hanh Thông' | 2 |
| 26 | Chậu Lan Hồ Điệp Vàng Hoàng Yến 5 Cành 'Kim Ngân Đắc Lộc' | 2 |
| 27 | Bình Hoa Cẩm Tú Cầu Xanh & Hoa Baby Decor Phòng Khách | 2 |
| 28 | Bó Hoa Baby Trắng Bồng Bềnh 'Giấc Mơ Tuyết Trắng' | 2 |
| 29 | Bó Hoa Hướng Dương Chúc Mừng Khởi Đầu Mới | 2 |
| 30 | Chậu Lan Hồ Điệp 5 Cành Tím Hoàng Gia | 3 |
| 31 | Bó Hoa Hướng Dương Tốt Nghiệp 'Khát Vọng Bay Cao' | 3 |
| 32 | Bó Hoa Cúc Tana Tinh Khôi Dễ Thương Tặng Sinh Nhật | 3 |
| 33 | Giỏ Hoa Gỗ Mộc Tone Pastel Chúc Mừng Thầy Cô & Bạn Bè | 3 |
| 34 | Hộp Hoa Mica Trong Suốt Tone Hồng 'Nắng Ban Mai' | 3 |
| 35 | Kệ Hoa Chúc Mừng Đại Hội & Sự Kiện Sinh Viên 1 Tầng | 3 |
| 36 | Bình Hoa Thủy Tinh Cắm Hoa Cúc Họa Mi Để Bàn Học | 3 |
| 37 | Bó Hoa Hồng Nhập Khẩu 20 Bông 'Tỏ Tình Lãng Mạn' | 3 |
| 38 | Kệ Hoa Khai Trương Câu Lạc Bộ Sinh Viên Rực Rỡ | 3 |

## Chi tiết đủ 81 dòng

A = chưa thấy đơn liên quan; B = còn đơn chưa kết thúc; C = chỉ thấy đơn đã kết thúc.
“Kho chủ” và “Tồn chủ” là dòng hiện có ở chi nhánh sở hữu; **không phải lệnh chuyển**.
Tất cả 81 dòng tại snapshot đều `is_selling=1`, `is_deleted=0`, nhưng không hợp lệ
để bán ở chi nhánh lệch theo quy tắc sở hữu hiện tại.

| ID kho lệch | Chi nhánh | Sản phẩm | Tồn lệch | Version | CN chủ | ID kho chủ | Tồn chủ | Số đơn liên quan | Nhóm |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 99 | 1 | 21 | 24 | 1 | 2 | 43 | 45 | 1 | B |
| 102 | 1 | 22 | 24 | 1 | 2 | 44 | 25 | 1 | B |
| 105 | 1 | 23 | 25 | 0 | 2 | 45 | 30 | 0 | A |
| 108 | 1 | 24 | 25 | 0 | 2 | 46 | 40 | 0 | A |
| 111 | 1 | 25 | 5 | 1 | 2 | 47 | 20 | 1 | B |
| 114 | 1 | 26 | 25 | 4 | 2 | 48 | 0 | 2 | C |
| 117 | 1 | 27 | 25 | 0 | 2 | 49 | 35 | 0 | A |
| 120 | 1 | 28 | 25 | 0 | 2 | 50 | 25 | 0 | A |
| 123 | 1 | 29 | 25 | 0 | 2 | 51 | 25 | 0 | A |
| 126 | 1 | 30 | 25 | 0 | 3 | 52 | 25 | 0 | A |
| 129 | 1 | 31 | 25 | 0 | 3 | 53 | 25 | 0 | A |
| 132 | 1 | 32 | 25 | 0 | 3 | 54 | 25 | 0 | A |
| 135 | 1 | 33 | 25 | 0 | 3 | 55 | 25 | 0 | A |
| 138 | 1 | 34 | 25 | 0 | 3 | 56 | 25 | 0 | A |
| 141 | 1 | 35 | 25 | 0 | 3 | 57 | 25 | 0 | A |
| 144 | 1 | 36 | 25 | 0 | 3 | 58 | 25 | 0 | A |
| 147 | 1 | 37 | 25 | 0 | 3 | 59 | 25 | 0 | A |
| 150 | 1 | 38 | 25 | 0 | 3 | 60 | 25 | 0 | A |
| 72 | 2 | 12 | 25 | 0 | 1 | 34 | 24 | 0 | A |
| 75 | 2 | 13 | 25 | 0 | 1 | 35 | 25 | 0 | A |
| 78 | 2 | 14 | 25 | 0 | 1 | 36 | 25 | 0 | A |
| 81 | 2 | 15 | 25 | 0 | 1 | 37 | 25 | 0 | A |
| 84 | 2 | 16 | 25 | 0 | 1 | 38 | 25 | 0 | A |
| 87 | 2 | 17 | 25 | 0 | 1 | 39 | 25 | 0 | A |
| 90 | 2 | 18 | 25 | 0 | 1 | 40 | 25 | 0 | A |
| 93 | 2 | 19 | 25 | 0 | 1 | 41 | 25 | 0 | A |
| 96 | 2 | 20 | 25 | 0 | 1 | 42 | 25 | 0 | A |
| 127 | 2 | 30 | 25 | 0 | 3 | 52 | 25 | 0 | A |
| 130 | 2 | 31 | 25 | 0 | 3 | 53 | 25 | 0 | A |
| 133 | 2 | 32 | 25 | 0 | 3 | 54 | 25 | 0 | A |
| 136 | 2 | 33 | 25 | 0 | 3 | 55 | 25 | 0 | A |
| 139 | 2 | 34 | 25 | 0 | 3 | 56 | 25 | 0 | A |
| 142 | 2 | 35 | 25 | 0 | 3 | 57 | 25 | 0 | A |
| 145 | 2 | 36 | 25 | 0 | 3 | 58 | 25 | 0 | A |
| 148 | 2 | 37 | 25 | 0 | 3 | 59 | 25 | 0 | A |
| 151 | 2 | 38 | 25 | 0 | 3 | 60 | 25 | 0 | A |
| 73 | 3 | 12 | 25 | 0 | 1 | 34 | 24 | 0 | A |
| 76 | 3 | 13 | 25 | 0 | 1 | 35 | 25 | 0 | A |
| 79 | 3 | 14 | 25 | 0 | 1 | 36 | 25 | 0 | A |
| 82 | 3 | 15 | 25 | 0 | 1 | 37 | 25 | 0 | A |
| 85 | 3 | 16 | 25 | 0 | 1 | 38 | 25 | 0 | A |
| 88 | 3 | 17 | 25 | 0 | 1 | 39 | 25 | 0 | A |
| 91 | 3 | 18 | 25 | 0 | 1 | 40 | 25 | 0 | A |
| 94 | 3 | 19 | 25 | 0 | 1 | 41 | 25 | 0 | A |
| 97 | 3 | 20 | 25 | 0 | 1 | 42 | 25 | 0 | A |
| 100 | 3 | 21 | 25 | 0 | 2 | 43 | 45 | 0 | A |
| 103 | 3 | 22 | 25 | 0 | 2 | 44 | 25 | 0 | A |
| 106 | 3 | 23 | 25 | 0 | 2 | 45 | 30 | 0 | A |
| 109 | 3 | 24 | 25 | 0 | 2 | 46 | 40 | 0 | A |
| 112 | 3 | 25 | 25 | 0 | 2 | 47 | 20 | 0 | A |
| 115 | 3 | 26 | 25 | 0 | 2 | 48 | 0 | 0 | A |
| 118 | 3 | 27 | 25 | 0 | 2 | 49 | 35 | 0 | A |
| 121 | 3 | 28 | 25 | 0 | 2 | 50 | 25 | 0 | A |
| 124 | 3 | 29 | 25 | 0 | 2 | 51 | 25 | 0 | A |
| 74 | 7 | 12 | 25 | 0 | 1 | 34 | 24 | 0 | A |
| 77 | 7 | 13 | 25 | 0 | 1 | 35 | 25 | 0 | A |
| 80 | 7 | 14 | 25 | 0 | 1 | 36 | 25 | 0 | A |
| 83 | 7 | 15 | 25 | 0 | 1 | 37 | 25 | 0 | A |
| 86 | 7 | 16 | 25 | 0 | 1 | 38 | 25 | 0 | A |
| 89 | 7 | 17 | 25 | 0 | 1 | 39 | 25 | 0 | A |
| 92 | 7 | 18 | 25 | 0 | 1 | 40 | 25 | 0 | A |
| 95 | 7 | 19 | 25 | 0 | 1 | 41 | 25 | 0 | A |
| 98 | 7 | 20 | 25 | 0 | 1 | 42 | 25 | 0 | A |
| 101 | 7 | 21 | 25 | 0 | 2 | 43 | 45 | 0 | A |
| 104 | 7 | 22 | 25 | 0 | 2 | 44 | 25 | 0 | A |
| 107 | 7 | 23 | 25 | 0 | 2 | 45 | 30 | 0 | A |
| 110 | 7 | 24 | 25 | 0 | 2 | 46 | 40 | 0 | A |
| 113 | 7 | 25 | 25 | 0 | 2 | 47 | 20 | 0 | A |
| 116 | 7 | 26 | 25 | 0 | 2 | 48 | 0 | 0 | A |
| 119 | 7 | 27 | 25 | 0 | 2 | 49 | 35 | 0 | A |
| 122 | 7 | 28 | 25 | 0 | 2 | 50 | 25 | 0 | A |
| 125 | 7 | 29 | 25 | 0 | 2 | 51 | 25 | 0 | A |
| 128 | 7 | 30 | 25 | 0 | 3 | 52 | 25 | 0 | A |
| 131 | 7 | 31 | 25 | 0 | 3 | 53 | 25 | 0 | A |
| 134 | 7 | 32 | 25 | 0 | 3 | 54 | 25 | 0 | A |
| 137 | 7 | 33 | 25 | 0 | 3 | 55 | 25 | 0 | A |
| 140 | 7 | 34 | 25 | 0 | 3 | 56 | 25 | 0 | A |
| 143 | 7 | 35 | 25 | 0 | 3 | 57 | 25 | 0 | A |
| 146 | 7 | 36 | 25 | 0 | 3 | 58 | 25 | 0 | A |
| 149 | 7 | 37 | 25 | 0 | 3 | 59 | 25 | 0 | A |
| 152 | 7 | 38 | 25 | 0 | 3 | 60 | 25 | 0 | A |

