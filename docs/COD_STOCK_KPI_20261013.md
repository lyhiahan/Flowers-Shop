# COD, tồn kho và hủy đơn trước giao hàng

## Quy tắc

- Đặt hàng thành công trừ `store_inventories.quantity` tại chi nhánh thực sự xử lý đơn, ngay trong transaction tạo đơn. COD vẫn có payment `PENDING` / trạng thái thanh toán `UNPAID`, nhưng đã giữ hàng bằng cách giảm số lượng còn lại.
- Chuyển sang xác nhận, chuẩn bị hoặc giao hàng không trừ kho lần nữa.
- Khách được hủy ở `PENDING`, `CONFIRMED`, `PREPARING`. Khi đã bắt đầu giao hoặc kết thúc đơn thì bị chặn.
- Hủy hoàn kho về đúng chi nhánh, đúng số lượng, đúng một lần. Gửi lại yêu cầu hủy không cộng thêm hàng. Đặt thất bại rollback, không tạo đơn thành công hoặc làm mất hàng.
- COD chưa thu tiền không phải doanh thu đã ghi nhận. Đơn đã thanh toán hủy trước giao chuyển sang yêu cầu hoàn tiền, không tự giả định đã hoàn tiền.

## Phần hiển thị được sửa

Thẻ cũ **Sản phẩm mới trong kỳ** đếm số mẫu sản phẩm tạo trong khoảng ngày, không đếm số lượng còn trong kho. Giữ chỉ số này để không thay đổi ý nghĩa bộ lọc báo cáo; thêm thẻ **Tồn kho còn lại** trên dashboard quản lý và admin.

KPI mới lấy tổng quantity của sản phẩm chưa xóa: riêng chi nhánh đang xem với quản lý, toàn chuỗi với admin. Đây là ảnh chụp tồn kho hiện tại, bao gồm hàng của sản phẩm/cửa hàng tạm ngừng bán, không lọc theo ngày tạo sản phẩm hay ngày đặt đơn. Trang chủ tiếp tục chỉ hiển thị hàng có thể mua tại chi nhánh đang hoạt động/mở cửa, vì vậy tổng trang chủ có thể khác tổng kho.

KPI tải lại qua GET mỗi 5 giây khi trang hiển thị; lấy số mới khi quay lại tab. Các endpoint không cache, kiểm tra role và quyền chi nhánh. Lỗi mạng giữ số liệu lần gần nhất và hiện cảnh báo, không đổi thành 0.

Không thêm thao tác trừ kho lần thứ hai vào checkout vì luồng tạo đơn đã thực hiện việc này trước đó. Thay đổi lần này làm rõ số liệu trên dashboard và kiểm chứng luồng đặt/hủy.

## Bổ sung cho trang khách hàng

Lượt sửa tiếp theo bổ sung KPI **Sản phẩm còn hàng** trên trang chủ/danh sách, độc lập số mẫu dùng để phân trang. Tính toàn bộ sản phẩm được phép bán ở các chi nhánh đang mở (hoặc riêng chi nhánh đang chọn), không chỉ 8 thẻ của trang hiện tại. Các bộ lọc từ khóa/danh mục không thay đổi phạm vi toàn chuỗi/chi nhánh được ghi rõ trên KPI.

`GET /api/public/catalog-stock` trả tổng số lượng còn có thể mua và tồn kho/chi nhánh mua cho tối đa 32 ID sản phẩm. Không trả dữ liệu khách hàng, đơn hàng hay lượng kho sản phẩm riêng/ngừng bán. Phản hồi không cache. Trang khách hàng dùng endpoint này để cập nhật KPI, nhãn Còn/Hết hàng, khóa/mở nút mua và chọn lại chi nhánh có hàng cho sản phẩm toàn chuỗi. Tự tải mỗi 5 giây và khi quay lại tab/trang từ back-forward cache; lỗi mạng giữ số liệu cuối cùng kèm cảnh báo. Không tải lại toàn trang và không ghi đè số lượng bằng dữ liệu tự trừ ở trình duyệt.

Kiểm thử mới xác nhận KPI/API khách hàng sau các request đặt COD/hủy đã commit, cùng trạng thái mỗi thẻ. Kiểm tra tổng ngoài trang đầu, lọc chi nhánh, sản phẩm chưa duyệt/đã xóa/ngừng bán, giới hạn tham số và tổng vượt 32 bit. JavaScript kiểm tra cả hoa bán chạy và thẻ danh sách trùng ID, hết hàng/hoàn hàng, nút mua đang gửi và quay lại trang.

Kết quả hồi quy sau bổ sung khách hàng: **454 ca Java, 451 đạt, 0 failures/errors, 3 bỏ qua** (Cloudinary live và 2 SQL Server migration opt-in). **25/25 ca JavaScript đạt**; `maven package` thành công. Đặt/hủy dùng H2 tách biệt, không thực hiện các thao tác này lên đơn hàng thật.

## Kiểm thử bổ sung

- `CodStockKpiTests`: HTTP checkout COD, 10 → 8; dashboard chi nhánh/toàn chuỗi và trang chủ cập nhật; hủy ở cả ba trạng thái trước giao trả 8 → 10, kể cả yêu cầu hủy lặp; giao/hoàn tất không trừ lần nữa và chặn hủy; đặt lặp/đặt thiếu hàng; quyền xem; bộ lọc ngày; tổng vượt số nguyên 32 bit và kho rỗng.
- `CodCheckoutCommitTests`: không có outer transaction của test; tạo dữ liệu, đặt COD, đọc kho, hủy và đọc lại qua các transaction độc lập để xác nhận thay đổi đã commit.
- `stock-kpi.test.cjs`: polling, số liệu lỗi, lỗi mạng/khôi phục, chống request chồng nhau, tab ẩn, hủy request khi rời trang, khôi phục trang từ back-forward cache.

Kết quả sau sửa ngày 13/10/2026: `maven package` kết thúc thành công; 448 ca Java, 445 đạt, 0 failures/errors, 3 bỏ qua (Cloudinary live và 2 SQL Server migration opt-in). 20/20 ca JavaScript đạt. Riêng nhóm COD/KPI có 11 ca và kiểm tra commit độc lập có 1 ca, đều đạt. Phép tổng hợp `SUM(CAST(quantity AS bigint))` đã chạy được bằng truy vấn chỉ đọc trên RentalShop tại SQLEXPRESS; lượt này không chạy lại migration hoặc đặt/hủy đơn trên database gốc.

## Cách tự demo

### Sửa chọn sai kho giữa trang chủ và checkout

Kiểm tra chỉ đọc trên RentalShop/SQLEXPRESS cho thấy đơn #23 đặt 15 đơn vị sản phẩm #26 nhưng ghi chi nhánh nhận hàng Quận 1 (`pickup_store_id = 1`). Sản phẩm này thuộc Quận 7 (`product.store_id = 2`). Kho Quận 1 còn 10, kho Quận 7 còn 15. Trang chủ chỉ tính kho của chi nhánh bán sản phẩm, trong khi luồng checkout cũ cho phép lấy kho khác. Vì vậy nhãn còn 15 không phải đơn giản do trình duyệt chưa cập nhật.

Theo lựa chọn của người dùng, giữ nguyên đơn #23 và toàn bộ tồn kho thật, chỉ sửa luồng đặt đơn mới. Không chuyển chi nhánh trên đơn, không hoàn/trừ kho thủ công, không giả hiển thị Quận 7 hết hàng. Quận 7 vẫn còn 15 cho đến khi có đơn mới giữ hàng tại đúng chi nhánh đó.

Quy tắc chung `ProductAvailability.supportsBranch` thống nhất giỏ hàng, kiểm tra pickup, báo giá và checkout với phạm vi bán của catalog. Dropdown pickup chỉ đưa ra chi nhánh phù hợp với toàn bộ sản phẩm được chọn. Giao tận nơi chỉ chọn chi nhánh phù hợp và đủ hàng. API kiểm tra kho/suggest chi nhánh không quảng bá kho ngoài phạm vi bán, sản phẩm ngừng bán hoặc chi nhánh đóng. Giỏ cũ gắn sai chi nhánh không được tăng số lượng; khách vẫn có thể xóa dòng và thêm lại. Không thay đổi luồng hủy/hoàn kho của đơn cũ.

`BranchStockConsistencyTests` kiểm tra chọn sai kho bị chặn trước khi tạo đơn/trừ hàng; đặt hết 15 tại chi nhánh bán làm API/trang chủ hiển thị 0 và Hết hàng; hủy trả đúng 15 một lần; quote/giao tận nơi dùng cùng chi nhánh; hết hàng không chuyển sang kho ngoài phạm vi; sản phẩm chung toàn chuỗi vẫn được lấy kho chi nhánh khác. Các thao tác đặt/hủy kiểm thử trên H2 tách biệt, không trên RentalShop gốc.

Kết quả hồi quy sau sửa chọn kho: `maven package` thành công lúc 09:13 ngày 13/10/2026; **474 ca Java trong 56 suite, 471 đạt, 0 failures/errors, 3 bỏ qua** (Cloudinary live và 2 SQL Server migration opt-in). **30/30 ca JavaScript đạt**. Sáu ca mới của `BranchStockConsistencyTests` đều đạt. Hai fixture hồi quy cũ được chỉnh để phản ánh đúng phạm vi bán: SKU chung toàn chuỗi trong ca chuyển kho, và thêm SKU riêng qua chính chi nhánh bán trước khi kiểm tra khóa cửa hàng; vẫn giữ các assertion nghiệp vụ. Không chạy lại migration hoặc tạo/hủy/chuyển đơn thật.

### Các bước

1. Dừng bản ứng dụng cũ bằng Ctrl+C trong terminal đang chạy nó; chạy lại `scripts/sqlserver.ps1`. Không cần reset database hoặc chạy lại migration chỉ để có KPI mới.
2. Chọn một sản phẩm còn hàng, ghi lại số lượng ở chi nhánh sẽ xử lý đơn. Mở dashboard chi nhánh đó ở tab/tài khoản quản lý riêng.
3. Khách đặt 2 sản phẩm bằng COD; số lượng còn lại phải giảm 2 ngay khi đơn thành công. Dashboard đang mở cập nhật trong lần polling kế tiếp; mở lại trang chủ sẽ đọc số mới.
4. Khách hủy khi đơn chờ xác nhận, đã xác nhận hoặc đang chuẩn bị; tồn kho tăng lại đúng 2. Gửi lại thao tác hủy không tăng thêm.
5. Với đơn khác, quản lý bắt đầu giao: kho không giảm lần nữa và khách không còn được hủy.

Đơn giao tận nơi được hệ thống chọn chi nhánh đáp ứng tồn kho/địa chỉ; cần xem chi nhánh trên đơn, không mặc định đó là chi nhánh trước đó đang mở ở tab quản lý. Kiểm thử tự động dùng H2 tách biệt. Việc kiểm tra tổng kho trên SQL Server gốc là chỉ đọc; không tạo/hủy đơn hoặc tự điều chỉnh dữ liệu thật của người dùng.
