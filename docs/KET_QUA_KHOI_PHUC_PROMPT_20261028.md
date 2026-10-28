# Kết quả tìm và khôi phục lịch sử AI liên quan đồ án

Ngày thực hiện: 28/10/2026. Chỉ đọc nguồn hội thoại trên máy; không thay đổi cơ sở dữ liệu hội thoại, không đăng nhập tài khoản, không truy cập cookie/token và không gửi dữ liệu ra dịch vụ khác.

## 1. Kết quả có thể sử dụng

Đã lọc được **87 bản ghi lời nhắn người dùng liên quan repo**: **47 từ Codex**, **40 từ Antigravity**. Đây không đồng nghĩa 87 prompt độc lập do người dùng tự soạn: một số lời nhắn là câu hỏi, báo lỗi, yêu cầu Git, nội dung dán lại từ AI hoặc rubric. Không lấy mọi bản ghi làm minh chứng sinh code.

- **15 yêu cầu/câu hỏi tiêu biểu** đã được chọn vào `docs/MINH_CHUNG_PROMPT_KHOI_PHUC_20261028.csv`. Có prompt, công cụ nguồn, timestamp, tên tệp và vị trí bản ghi; chưa tự gán thành viên, commit hoặc kết quả.
- Bản đầy đủ đã che thông tin nhạy cảm nằm ở `.tools/prompt-recovery-20261028/PROMPTS_DA_KHOI_PHUC.md`, cùng CSV/JSONL và inventory. Thư mục `.tools/` được ignore; không tự đưa lịch sử riêng lên Git.
- Chưa tìm thấy bản export **ChatGPT web** trong phạm vi quét. Không đổi nhãn Codex thành ChatGPT; cũng không kết luận tài khoản ChatGPT không có lịch sử.

## 2. Nguồn đã tìm thấy

| Nguồn | Kết quả | Cách diễn giải |
|---|---|---|
| `%USERPROFILE%/.codex/sessions/` | Có JSONL và JSONL nén Zstandard | Đọc `response_item` có `role=user`, đối chiếu `cwd` repo; có cả session fork chứa lịch sử lặp |
| `%USERPROFILE%/.codex/archived_sessions/` | Có session cũ của repo | Khôi phục được lịch sử giai đoạn đường dẫn repo còn ở `web/Rental-Retail-Shop` |
| `%USERPROFILE%/.gemini/antigravity/conversations/` | Có SQLite và tệp `.pb` | SQLite đọc được; không tự suy diễn nội dung các `.pb` nhị phân chưa đọc được |
| `%USERPROFILE%/.gemini/antigravity-ide/conversations/` | Có SQLite của các phiên làm việc đúng đồ án | Trích trường lời nhắn, giữ `steps.idx`, loại hội thoại của dự án khác |
| `%USERPROFILE%/.gemini/antigravity*/brain/` | Có báo cáo, implementation plan, walkthrough liên quan repo | Đây chủ yếu là kết quả AI tạo, không thay thế prompt gốc |
| Downloads, `D:/Download`, Documents, Desktop | Không thấy `conversations.json`/`chat.html`; kiểm tra mục lục 12 ZIP cũng không thấy | Phạm vi tìm export có giới hạn; chưa kiểm tra mọi ổ đĩa, tài khoản cloud hay máy thành viên khác |

Không đọc kho xác thực hoặc cache trình duyệt để cố lấy lịch sử ChatGPT. Các tệp đính kèm được nhắc trong lời nhắn chỉ được giữ tham chiếu; lượt này không khôi phục toàn bộ ảnh hoặc nội dung tài liệu đính kèm.

## 3. Thống kê và phương pháp

1. Quét 42 tệp Codex JSONL/nén; có 6 tệp liên quan repo.
2. Đọc 203 SQLite Antigravity bằng kết nối `mode=ro`. Bước dò rộng tìm 27 database có nhắc đường dẫn/tên repo, nhưng có dự án khác nhắc chéo tới repo này.
3. Sau đối chiếu ngữ cảnh tool/artifact, chỉ giữ 8 hội thoại Antigravity phù hợp. Không dùng việc có một chuỗi “Rental-Retail-Shop” làm điều kiện đủ.
4. Thu thập ban đầu 452 bản ghi; khử lịch sử lặp còn 174 ứng viên. Loại 87 ứng viên do khác dự án hoặc là tín hiệu tiếp tục/trả lời giao diện, còn 87 bản ghi phạm vi đồ án.
5. Có 232 tệp `.pb` nhị phân trong các kho được kiểm kê; chưa giải mã/khôi phục các tệp này. Không nhận kết quả hiện có là toàn bộ lịch sử AI trên máy.

Đối với Codex, cấu trúc JSONL có trường vai trò rõ ràng. Đối với Antigravity, dữ liệu trong `step_payload` là Protobuf, chưa có schema chính thức trong lượt này: đã nhận diện `step_type=23`, trường `.30.19` chứa lời nhắn và đối chiếu với yêu cầu quen thuộc/ngữ cảnh repo. Vì vậy cần mở hội thoại tương ứng trong Antigravity kiểm tra lần cuối trước khi nộp minh chứng. Không gọi quá trình này là export chính thức của ứng dụng.

Timestamp Codex là thời gian bản ghi. Timestamp Antigravity lấy từ metadata có thể không phản ánh chính xác giờ gửi từng tin, nên CSV ghi chú rõ và không dùng để suy ra thời điểm commit. Tên app không chứng minh model Gemini cụ thể; tài khoản trên máy không tự chứng minh người nào trong nhóm gõ từng yêu cầu.

## 4. Ví dụ có ích cho phần AI trong báo cáo

| ID trong CSV | Công cụ | Nội dung nguyên văn ngắn | Cách dùng |
|---|---|---|---|
| WEB-010 | Antigravity | “chi nhánh này chưa có doanh số vậy sản phẩm bán chạy đang lấy từ đâu” | Ví dụ người dùng chất vấn tính đúng nghiệp vụ; cần nối với đoạn tính doanh số và commit sửa thực tế |
| WEB-012 | Antigravity | “web trên máy tôi đang bị lỗi mã hóa, và các trang con của manager đang bị lỗi thông tin tuột” | Minh chứng phản hồi lỗi UI; ảnh kết quả và commit phải đối chiếu thêm |
| WEB-023 | Codex | “giờ tôi upload ảnh từ những phần nào thì sẽ vào cloud” | Yêu cầu làm rõ phạm vi upload; nối với yêu cầu bổ sung ở WEB-024 |
| WEB-055 | Antigravity | “kiểm tra hoạt động của cloudinary” | Chứng minh có yêu cầu kiểm tra; tự câu này chưa chứng minh upload đã thành công |
| WEB-064 | Codex | “chạy bản nào thử trước đi, hãy thêm tính năng lọc thêm chi nhánh cho tính năng tìm kiếm sản phẩm” | Yêu cầu mở rộng tính năng; đối chiếu controller/specification/filter UI và test |
| WEB-078 | Codex | “bỏ quyền mua hàng của quản lý và chỉnh sửa các thành phần liên quan” | Thay đổi chính sách; nối với phân quyền server/session/JWT, UI và test |
| WEB-085 | Antigravity | “Chuẩn bị File Cơ sở dữ liệu (.sql) nộp kèm” | Minh chứng yêu cầu đóng gói; cần thêm kết quả restore độc lập |

Không biến cột “Cách dùng” thành kết quả đã đạt. Cũng không sử dụng chỉ các câu “kiểm tra repo”, “chạy web”, “push code” để chứng minh thành thạo AI sinh code.

## 5. Các bước đưa vào hồ sơ nộp

1. Mở CSV 15 dòng, từng thành viên chọn các dòng mình thực sự tham gia và xác nhận. Không gán người theo số commit hoặc tài khoản máy.
2. Mở lại nguồn có tên tệp/ID hội thoại, chụp tin nhắn tương ứng và phần phản hồi có liên quan. Với Antigravity, đối chiếu lại nội dung và ngày giờ trong UI.
3. Tìm commit và diff thật; ghi file/method đã sửa, phần AI đề xuất đúng/sai và nhóm đã điều chỉnh gì. Nếu không xác minh được commit, để “chưa xác định”.
4. Gắn một kết quả test/demo thật và giới hạn. Lịch sử prompt không chứng minh kiểm thử, hiệu quả hay mức làm chủ code.
5. Đưa các dòng được xác nhận vào datasheet nộp hoặc phụ lục Word. Giữ nhãn “nguyên văn đã che thông tin nhạy cảm”; bản tóm tắt phải ghi rõ là tóm tắt.

Các prompt chứa cấu hình nhạy cảm đã bị che toàn bộ hoặc một phần trong bản trích xuất. Không nộp raw session/database của ứng dụng. Bản CSV chọn lọc và ghi chú này chưa chứa lịch sử phản hồi AI đầy đủ; nếu cần minh chứng quá trình sửa code, phải bổ sung phần phản hồi và diff được rà soát.
