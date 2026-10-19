# Đối soát kho lệch chi nhánh — 19/10/2026

Báo cáo này là kết quả truy vấn **chỉ đọc** trên `RentalShop` lúc 07:38 (giờ Việt Nam) bằng `scripts/audit-branch-inventory.ps1`. Chưa chuyển, gộp, sửa hoặc xóa tồn kho và đơn hàng. Bản sao lưu `RentalShop_before_branch_catalog_20261019_0740.bak` đã được SQL Server xác minh và bản sao `CodexCatalogCopy_20261019_0740` đã được tạo; bản sao này dành cho thử migration, không phải phương án sửa kho.

Sau khi nâng `RentalShop` lên V10 và sao lưu mới, truy vấn lại lúc 08:00 xác nhận các số liệu kho bên dưới **không đổi**. Chưa có chứng từ hay kết quả kiểm kê để xác định 2.003 đơn vị là hàng thật hay dữ liệu lịch sử, nên không tự ý chuyển/xóa.

Người dùng sau đó xác nhận 81 dòng là **dữ liệu kho cũ bị nhân bản**. Đã diễn tập phương án có thể hoàn tác trên `CodexCatalogCopy_20261019_0740`: lưu đầy đủ giá trị gốc vào `inventory_duplicate_cleanup_20261019`, rồi đặt 81 dòng về số lượng 0, ngừng bán và đánh dấu xóa mềm. Bản sao còn 1.336 đơn vị; số đơn không đổi. `RentalShop` thật vẫn còn 3.339 đơn vị vì thao tác thay đổi 2.003 đơn vị trên database thật bị chặn chờ phê duyệt riêng. Không chạy script cập nhật database thật khi chưa xác nhận tác động này.

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

Theo xác nhận của người dùng, không chuyển/gộp 2.003 đơn vị vào chi nhánh chủ sở hữu vì mỗi sản phẩm đã có dòng kho ở đó. Phương án diễn tập giữ nguyên 81 ID kho và toàn bộ đơn hàng, chỉ cô lập số lượng nhân bản. Ba dòng còn liên quan đơn chưa kết thúc; mã nguồn đã được điều chỉnh để khi hủy đơn cũ không hoàn hàng vào dòng kho ở chi nhánh không sở hữu sản phẩm. Cần triển khai mã nguồn mới trước hoặc cùng lúc với việc cô lập dữ liệu thật. Chỉ áp dụng lên database thật khi có phê duyệt riêng cho việc đưa 2.003 đơn vị về 0.

Chạy lại báo cáo chỉ đọc trước mỗi quyết định:

```powershell
.\scripts\audit-branch-inventory.ps1 > .\branch-inventory-audit.json
```

Không chạy lệnh UPDATE/DELETE dựa chỉ trên tiêu chí `products.store_id <> store_inventories.store_id`.
