# Cập nhật form rút tiền — 04/10/2026

Đã sửa form `/manager/wallet` theo giao diện xanh của trang quản lý: label liên kết trường nhập, đánh dấu bắt buộc, hướng dẫn số tiền/số tài khoản, chọn ngân hàng rõ ràng và nút tiếng Việt. Modal có vùng nội dung cuộn, header/footer giữ trong khung trên desktop và mobile.

Luồng nhập và xác nhận nằm trong một modal. Bước xác nhận hiển thị tiền, ngân hàng, số tài khoản và tên chủ tài khoản; Quay lại giữ dữ liệu, Hủy không gửi yêu cầu. Đóng modal xóa mật khẩu. Chỉ xác nhận mới cho phép POST; nút gửi bị khóa khi đang gửi để tránh bấm lặp.

`store-confirm.js` cung cấp bộ validation inline dùng chung cho form rút tiền và các form có `data-confirm` ở sản phẩm/profile. Giữ constraint HTML, hiển thị lỗi tiếng Việt, focus trường lỗi đầu tiên và gắn `aria-describedby`/`aria-invalid`. Nút xác nhận chung đổi sang Xác nhận/Hủy. Phạm vi này chưa thay toàn bộ validator riêng của các form khác trong repo.

Validation rút tiền kiểm tra tối thiểu 50.000 ₫, không vượt số dư, tối đa hai chữ số thập phân, số tài khoản 6–30 chữ số, tên chủ tài khoản và mật khẩu bắt buộc. Không sửa quy tắc phân quyền/transaction/backend đã có.

## Kiểm chứng

- Maven: `StoreManagementEwalletUseCaseTests,StoreUseCaseAuditTests` — **33 đạt, 0 thất bại/lỗi/bỏ qua**. Log: `target/wallet-form-tests.log`.
- `node scripts/test-store-wallet.cjs`: review không POST, quay lại giữ dữ liệu, kiểm tra lại lúc xác nhận, chặn submit lặp và xóa mật khẩu khi đóng đều đạt.
- `node scripts/check-frontend.cjs`: 64 template và JavaScript, 0 lỗi. `git diff --check` đạt.
- Browser demo H2 cổng 8081: form trống, dưới mức tối thiểu, vượt số dư, tài khoản sai, thông tin hợp lệ sang review, quay lại và hủy/xóa mật khẩu; form sản phẩm dùng chung hiển thị lỗi và xác nhận/hủy tiếng Việt. Không phát sinh giao dịch rút tiền qua browser.
- Kiểm tra màn hình 390 × 844: modal nằm trong viewport, nội dung cuộn và các nút thao tác truy cập được. Đã khôi phục viewport mặc định và dừng server demo.

Ảnh: [form xác nhận mới](WALLET_FORM_FIXED.png). Dữ liệu trong ảnh là dữ liệu demo.
