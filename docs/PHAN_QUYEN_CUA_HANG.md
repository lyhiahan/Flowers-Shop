# Vai trò và phạm vi cửa hàng

Quy định được xác nhận ngày 24/10/2026. Cửa hàng là chi nhánh hoạt động, không phải một vai trò tài khoản. Chủ chuỗi dùng ADMIN; người phụ trách một chi nhánh dùng MANAGER; nhân viên của chi nhánh dùng STAFF.

| Công việc | Admin / Chủ chuỗi | Manager / Quản lý chi nhánh | Staff / Nhân viên | Khách hàng |
|---|---|---|---|---|
| Phạm vi làm việc | Toàn chuỗi | Một chi nhánh được phân công | Một chi nhánh được phân công | Đơn của chính mình |
| Mua hoa, giỏ hàng, checkout và ví khách hàng | Không | Không | Không | Có |
| Xem, xác nhận, chuẩn bị và giao đơn | Có | Có | Có, trong chi nhánh | Theo dõi đơn của mình |
| Giao hoa và ghi nhận tiền COD tại quầy | Có | Có | Có, qua thao tác hoàn tất đơn | Không |
| Hủy đơn từ cửa hàng, xác nhận chuyển khoản, hoàn tiền | Có | Có | Không | Chỉ yêu cầu/hủy đơn của mình theo điều kiện |
| Trả lời khách qua chat chi nhánh | Giám sát chat khách–chi nhánh; hỗ trợ toàn chuỗi | Có | Có | Nhắn tin với cửa hàng |
| Chat hỗ trợ nội bộ với admin | Có | Có | Không | Không |
| Quản lý sản phẩm, giá và tồn kho | Có | Có | Không | Không |
| Thêm/gỡ nhân viên | Có | Quản lý phụ trách chi nhánh | Không | Không |
| Xem báo cáo, ví và yêu cầu rút tiền | Có | Có, trong chi nhánh phụ trách | Không | Không |
| Cấu hình cửa hàng, khuyến mãi | Có | Có | Không | Không |

## Thêm nhân viên

Cập nhật 28/10/2026: MANAGER không còn được cấp quyền USER. Quản lý dùng `/manager/profile`, `/manager/orders` và `/manager/wallet` cho công việc chi nhánh; các trang `/customer/**`, giỏ hàng và checkout chỉ dành cho khách hàng. Session cũ được làm mới quyền ở request tiếp theo, JWT đọc lại quyền từ database. Dữ liệu cũ gán đồng thời MANAGER và USER không cấp quyền mua hàng; không cần xóa đơn, giỏ hoặc sổ ví lịch sử để áp dụng quy định mới.

Xác minh 28/10: Java 875 ca (863 đạt, 12 skipped, 0 lỗi), JS 185/185 đạt. Trên web PostgreSQL local, MANAGER vào dashboard/sản phẩm/đơn/ví cửa hàng/hồ sơ/thông báo trả 200; cart/checkout/đơn cá nhân/ví khách/hồ sơ khách trả 403; JWT chỉ nhận MANAGER. Hai alias xem sản phẩm của guest/USER/STAFF/MANAGER/ADMIN đều render đúng quyền. Log local: `target/manager-shopping-java-final.log`, `target/manager-shopping-js.log`, `target/manager-permission-live-check.json`, `target/staff-product-live-check.json`.

1. Quản lý mở **Nhân viên → Thêm nhân viên**.
2. Chọn tài khoản **Nhân viên cửa hàng** chưa có chi nhánh, hoặc tạo tài khoản mới bằng tên đăng nhập, email và mật khẩu.
3. Vai trò cố định là **Nhân viên cửa hàng**. Biểu mẫu này không cấp quyền Manager/Admin và không chuyển khách hàng thành nhân viên.
4. Nhân viên đăng nhập sẽ vào `/staff/orders`, xem đơn và dùng nút Nhắn tin để hỗ trợ khách.
5. Muốn chuyển nhân viên sang chi nhánh khác, gỡ phân công cũ rồi thêm vào chi nhánh mới. Gỡ phân công không xóa tài khoản hay lịch sử đơn/chat.

Admin cũng có thể tạo tài khoản STAFF tại **Người dùng**. Tài khoản chưa được phân công sẽ được hướng dẫn liên hệ quản lý, chưa có quyền xử lý đơn của chi nhánh nào.

## Tài khoản cũ và triển khai

Migration Flyway **V16** bổ sung STAFF vào ràng buộc vai trò. Các tài khoản MANAGER đã gán chi nhánh/liên kết nhân sự nhưng không đứng tên phụ trách bất kỳ cửa hàng nào sẽ được chuyển sang STAFF. Migration giữ tài khoản admin, quản lý phụ trách, tài khoản quản lý chưa phân công, trạng thái khóa/kích hoạt và toàn bộ dữ liệu nghiệp vụ.

Trong schema cũ, `stores.owner_id` vẫn lưu người phụ trách chi nhánh để giữ các quan hệ đang dùng; giao diện gọi đúng là **Quản lý chi nhánh**, không gọi là chủ sở hữu toàn chuỗi.

Triển khai đồng bộ mã mới và V16; khởi động lại ứng dụng. Các máy thành viên phải cập nhật mã trước khi dùng chung database có STAFF. Phiên đăng nhập đọc lại quyền từ database; việc gỡ quyền Manager có hiệu lực ở các request tiếp theo. Chat kiểm tra lại chi nhánh khi gửi/đăng ký kênh.

## Các bước tự test

1. Đăng nhập Manager: mở Nhân viên, thấy các tab **Nhân viên cửa hàng** và **Quản lý chi nhánh**, cùng mô tả quyền.
2. Thêm tài khoản Staff mới; đăng xuất và đăng nhập Staff: vào **Bàn làm việc nhân viên**.
3. Tạo đơn tại chi nhánh đó bằng tài khoản khách: Staff thấy đơn, xác nhận, chuẩn bị hoa rồi giao/hoàn tất theo đúng thứ tự.
4. Staff mở URL `/manager/staff`, `/manager/products`, `/manager/wallet`, `/manager/analytics`, `/manager/profile`, `/admin/users`: phải bị từ chối.
5. Thử gửi yêu cầu cập nhật đơn thuộc chi nhánh khác: bị từ chối, đơn giữ nguyên trạng thái.
6. Thử hủy đơn, xác nhận chuyển khoản và hoàn tiền bằng Staff: không được thực hiện; chuyển cho Manager.
7. Khách nhắn chi nhánh: Staff đọc/trả lời được. Staff không được xem chat chi nhánh khác hoặc hỗ trợ nội bộ Admin–Manager.
8. Manager gỡ nhân viên khi người đó vẫn đăng nhập: Staff mất quyền xử lý đơn/chat của chi nhánh; lịch sử vẫn còn.
9. Thử thêm một nhân viên vào hai chi nhánh hoặc gửi vai trò giả `ADMIN`, `MANAGER`, `PARTNER` từ biểu mẫu thêm nhân viên: bị từ chối.
10. Kiểm tra Manager/Admin vẫn dùng được chức năng quản lý cũ; tài khoản khách không truy cập được `/staff/orders`.


## Kết quả trên máy cục bộ ngày 24/10/2026

- Đã sao lưu và kiểm tra bản sao lưu trước khi cập nhật; chạy V16 trên database thử nghiệm riêng rồi mới áp dụng vào `RentalShop` thật. Bản thử nghiệm đã được dọn; bản sao lưu vẫn giữ trong thư mục Backup của SQL Server.
- Sau cập nhật: 2 tài khoản MANAGER, 4 tài khoản STAFF. Các nhân viên được chuyển là `tranminhtuan`, `nguyenmailan`, `tranbaophuc`, `lehoangkhoi`; giữ nguyên mật khẩu và chi nhánh.
- Số đơn vẫn là 35; số cửa hàng vẫn là 4; tổng số dư ví cửa hàng vẫn là 50.965.000 đồng. Không sửa lịch sử đơn hoặc số dư trong tác vụ phân quyền.
- Các bộ kiểm thử Java về quyền, chat, phiên đăng nhập, JWT và migration đều đạt; 25 ca JavaScript chat đạt. Đã thử HTTP trên ứng dụng thật: Staff vào `/staff/orders` được, các trang quản lý trả 403; Manager mở tổng quan và nhân sự được.


## Các trang công việc Staff được bổ sung

Nhân viên có các trang Đơn hàng, Chi tiết đơn, Chuẩn bị hoa, Giao & nhận hoa và Tin nhắn khách hàng. Danh sách hỗ trợ tìm kiếm, lọc và phân trang; chi tiết hiển thị lịch hẹn, lời chúc, hoa cần chuẩn bị, giá trong đơn và số tiền cần thu. Khi xác nhận đã giao/hoàn tất đơn COD chưa thanh toán, phải xác nhận đã giao hoa và thu đủ tiền; gửi lại không ghi nhận tiền trùng. Đơn trả trước chưa xác nhận thanh toán chưa được giao/hoàn tất.

Hướng dẫn từng bước: [Kịch bản test Nhân viên](KICH_BAN_TEST_NHAN_VIEN.md).


## Admin xóa tài khoản

- Khách hàng còn đơn đang xử lý hoặc chờ hoàn tiền: chặn xóa; giữ nguyên tài khoản, thanh toán, đơn và tồn kho. Đã giao nhưng chưa hoàn tất cũng chưa được xóa. Đơn hủy đã thu tiền phải hoàn tiền trước.
- Nhân viên: thu hồi quyền đăng nhập, gỡ phân công đang hoạt động; giữ lịch sử xử lý đơn và tin nhắn. Đơn của khách tại cửa hàng vẫn được nhân viên khác/quản lý tiếp tục xử lý.
- Quản lý đang đứng tên phụ trách chi nhánh: phải phân công quản lý khác trước khi xóa. Sau thay thế, thu hồi quyền và giữ lịch sử như nhân viên.
- Nếu nhân viên/quản lý từng mua hàng, đơn mua cá nhân chưa xong cũng chặn xóa.
- Tài khoản có lịch sử được vô hiệu hóa và giữ hồ sơ tham chiếu; tài khoản khách chưa có lịch sử được xóa khỏi database. Không cho xóa Admin hoặc tự xóa tài khoản đang đăng nhập.
- Thông báo thành công chỉ ghi: **Đã xóa tài khoản [tên đăng nhập] thành công.**

Tự test: thử xóa khách có đơn chờ xác nhận/đang chuẩn bị/đang giao/chờ hoàn tiền, phải bị chặn và đơn không đổi. Hoàn tất đơn rồi thử lại, phải thành công và hóa đơn vẫn xem được. Thử xóa Staff đang đăng nhập, yêu cầu tiếp theo bị từ chối, đơn khách của chi nhánh vẫn còn. Thử xóa Manager trước và sau khi thay người phụ trách. Chỉ thực hiện bằng tài khoản thử nghiệm.

Kiểm tra ngày 24/10/2026: 36 ca tự động về xóa/khóa tài khoản và quản lý người dùng đạt. Đã khởi động lại ứng dụng và kiểm tra HTTP trang đăng nhập/quản lý người dùng trả 200; không xóa tài khoản trên database thật khi kiểm tra.
