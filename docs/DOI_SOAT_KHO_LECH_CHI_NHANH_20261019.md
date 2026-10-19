# Đối soát kho lệch chi nhánh — 19/10/2026

Báo cáo này bắt đầu từ truy vấn **chỉ đọc** trên `RentalShop` lúc 07:38 (giờ Việt Nam) bằng `scripts/audit-branch-inventory.ps1`. Các số liệu trong bảng dưới là **snapshot trước khi xóa**. Bản sao lưu `RentalShop_before_branch_catalog_20261019_0740.bak` đã được SQL Server xác minh và bản sao `CodexCatalogCopy_20261019_0740` đã được tạo để thử migration.

Sau khi nâng `RentalShop` lên V10 và sao lưu mới, truy vấn lại lúc 08:00 xác nhận các số liệu kho bên dưới **không đổi**. Chưa có chứng từ hay kết quả kiểm kê để xác định 2.003 đơn vị là hàng thật hay dữ liệu lịch sử, nên không tự ý chuyển/xóa.

Người dùng sau đó xác nhận 81 dòng là **dữ liệu kho cũ bị nhân bản**. Phương án đặt về 0/xóa mềm được diễn tập trước trên `CodexCatalogCopy_20261019_0740`; lần sau người dùng yêu cầu **xóa hẳn các dòng nhân bản**. Đã tạo backup mới, diễn tập đúng lệnh xóa trên `CodexDupDeleteCopy_20261019_0856`, rồi áp dụng lên `RentalShop` sau một backup mới nữa. Mỗi dòng gốc được lưu trong bảng `dbo.inventory_duplicate_deletion_20261019` và trong backup để có thể khôi phục có kiểm soát.

**Trạng thái sau xóa:** `RentalShop` còn **73 dòng tồn kho / 1.336 đơn vị**, **0 dòng lệch chi nhánh**, **0 dòng tồn âm**. Bảng audit lưu **81 dòng / 2.003 đơn vị** đã xóa. **26 đơn và 28 chi tiết đơn được giữ nguyên; đơn `#23` vẫn `CANCELLED`.** Hai backup đã được SQL Server xác minh: `RentalShop_before_duplicate_delete_20261019_0856_prepare.bak` và `RentalShop_before_duplicate_delete_20261019_0856_apply.bak` trong thư mục Backup của instance `SQLEXPRESS`.

| Chỉ số | Kết quả |
| --- | ---: |
| Tổng dòng tồn kho / tổng số lượng | 154 / 3.339 |
| Dòng lệch chi nhánh / số lượng | 81 / 2.003 |
| Dòng lệch có đơn liên quan / có đơn chưa kết thúc | 4 / 3 |
| Dòng lệch đã có bản ghi kho tại chi nhánh chủ sở hữu sản phẩm | 81 |
| Dòng tồn kho âm | 0 |

| Chi nhánh đang giữ dòng lệch (ID) | Số dòng | Số lượng |
| ---: | ---: | ---: |
| 1 | 18 | 428 |
| 2 | 18 | 450 |
| 3 | 18 | 450 |
| 7 | 27 | 675 |

Các dòng cần ưu tiên xác minh thủ công: kho `#99` (sản phẩm `#21`, 24 đơn vị, đơn `#18` còn xử lý), `#102` (sản phẩm `#22`, 24 đơn vị, đơn `#19` chờ), `#111` (sản phẩm `#25`, 5 đơn vị, đơn `#20` chờ), và `#114` (sản phẩm `#26`, 25 đơn vị, liên quan các đơn đã hủy `#22`, `#23`). 77 dòng còn lại không có đơn được liên kết theo truy vấn hiện tại; điều đó **không chứng minh** chúng là hàng dư vì có thể thiếu chứng từ nhập/chuyển kho. Đơn `#23` đang ở trạng thái `CANCELLED`; giữ nguyên đơn theo yêu cầu.

Không chuyển/gộp 2.003 đơn vị vào chi nhánh chủ sở hữu vì mỗi sản phẩm đã có dòng kho ở đó. Ba dòng từng liên quan đơn chưa kết thúc; mã nguồn đã được điều chỉnh để khi hủy các đơn cũ không hoàn hàng vào dòng kho ở chi nhánh không sở hữu sản phẩm. Khi chạy website sau thay đổi database, cần dùng **mã nguồn mới** có sửa đổi này. Lệnh xóa có điều kiện chặn chạy lại trên cùng database; không chạy thủ công `DELETE` rộng theo mỗi điều kiện lệch chi nhánh.

Chạy lại báo cáo chỉ đọc trước mỗi quyết định:

```powershell
.\scripts\audit-branch-inventory.ps1 > .\branch-inventory-audit.json
```

Không chạy lệnh UPDATE/DELETE dựa chỉ trên tiêu chí `products.store_id <> store_inventories.store_id`.
