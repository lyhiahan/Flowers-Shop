import re

file_path = 'docs/HUONG_DAN_SETUP_VA_DANH_SACH_PROMPT_AI.md'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update Table of Contents: Replace the 2.xx subitems
old_toc = """- [PHẦN 2: DANH SÁCH TOÀN BỘ PROMPT NHÓM ĐÃ SỬ DỤNG VỚI AI](#phan-2-prompts)
  - [2.1. Nhóm Prompt: Khởi tạo dự án & Kiến trúc Spring Boot MVC](#21-kien-truc)
  - [2.2. Nhóm Prompt: Thiết kế CSDL JPA & Mô hình Chuỗi chi nhánh](#22-csdl)
  - [2.3. Nhóm Prompt: Bảo mật kép (Session CSRF + Stateless JWT)](#23-bao-mat)
  - [2.4. Nhóm Prompt: Xác thực OTP qua Email & Bảo vệ thông tin mật](#24-otp)
  - [2.5. Nhóm Prompt: Đặt hàng, Trừ tồn kho tức thời & Khóa bi quan](#25-dat-hang)
  - [2.6. Nhóm Prompt: Vòng đời đơn hàng & Khóa tự hủy khi cắm hoa](#26-vong-doi)
  - [2.7. Nhóm Prompt: Phân hệ Quản lý chi nhánh (UC_017 - UC_026)](#27-manager)
  - [2.8. Nhóm Prompt: Realtime Chat STOMP WebSocket & Giám sát Admin](#28-chat)
  - [2.9. Nhóm Prompt: Tích hợp Đám mây Cloudinary Upload Media an toàn](#29-cloudinary)
  - [2.10. Nhóm Prompt: Tinh chỉnh Giao diện Bootstrap 5 & Thẻ KPI cùng 1 hàng](#210-giao-dien)
  - [2.11. Nhóm Prompt: Dropdown chọn thuộc tính hoa & Chuẩn hóa tên sản phẩm](#211-dropdown)
  - [2.12. Nhóm Prompt: Gỡ lỗi (Upload 413, Bảo toàn thuộc tính cũ) & Sinh hình sơ đồ](#212-go-loi)"""

new_toc = """- [PHẦN 2: DANH SÁCH TOÀN BỘ PROMPT NHÓM ĐÃ SỬ DỤNG VỚI AI](#phan-2-prompts)
  - [Danh sách chi tiết từ Prompt 1 đến Prompt 20](#phan-2-prompts)"""

content = content.replace(old_toc, new_toc)

# 2. Remove lines like '<a name="2x-..."></a>\n### 2.x. Nhóm Prompt: ...\n'
content = re.sub(r'<a name=\"2\d+-[^\"]+\"></a>\s*\n+### 2\.\d+\. Nhóm Prompt:[^\n]+\n*', '', content)

# 3. Change '#### Prompt (\d+):' to '### Prompt \1:'
content = re.sub(r'#### Prompt (\d+):', r'### Prompt \1:', content)

# 4. In section 2 description, update the sentence mentioning 12 groups
content = content.replace('được phân theo 12 nhóm chuyên đề kỹ thuật trong suốt quá trình phát triển dự án:', 'trong suốt quá trình phát triển dự án:')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print('Updated HUONG_DAN_SETUP_VA_DANH_SACH_PROMPT_AI.md successfully!')
