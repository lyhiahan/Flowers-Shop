# Kế hoạch hoàn thiện đồ án trước buổi bảo vệ 29/10/2026

Lập ngày 28/10/2026, đối chiếu source `33d489c` và rubric nhóm cung cấp. Đây là kế hoạch, không phải báo cáo các việc đã làm xong. Lượt này chỉ tìm lịch sử AI và lập tài liệu, không sửa ứng dụng, restore database hay push Git.

## 1. Mục tiêu chốt bản

Đồ án đã có đủ stack và nghiệp vụ chính. Ưu tiên làm bản có thể cài lại, demo khép kín, giải thích và bàn giao được; không thêm framework hoặc tích hợp lớn trước buổi báo cáo.

- Một commit chốt, một DB demo, một DOCX chính thức; ảnh/video/source phải thống nhất.
- Ưu tiên PostgreSQL local cho demo, mô tả Aiven là lựa chọn khác. Không cam kết cả hai SQL tương thích 100% nếu chỉ restore thử một loại.
- MANAGER/STAFF/ADMIN không mua hàng; ví cửa hàng và đơn chi nhánh vẫn hoạt động. Không thay migration/xóa dữ liệu để vượt lỗi.
- Phân biệt kết quả thật, mock/demo và chưa xác minh. COD/MoMo demo không tự vi phạm đề tài; cần công bố rõ.
- Bằng chứng AI phải có nguồn. Không dựng prompt, commit hoặc tác giả; không đồng nhất câu trả lời AI được dán lại với prompt do người dùng tự soạn.

## 2. Cổng nghiệm thu

| Cổng | Điều kiện qua | Minh chứng |
|---|---|---|
| G0 — đóng băng | Phiên bản, cấu hình và dữ liệu demo được xác định; phân loại thay đổi đang làm | Mã commit, manifest file, danh sách mock/giới hạn, phương án rollback |
| G1 — cài mới | Source sạch + DB thử nghiệm mới → import → startup → đăng nhập bốn role | Lệnh/log không secret, host/port/profile, kết quả HTTP/login |
| G2 — nghiệp vụ | Một đơn đi trọn luồng; quyền, phạm vi chi nhánh, tồn và hoàn tiền đúng | Ma trận kiểm thử mục 4; kết quả thực tế và lỗi còn lại |
| G3 — tích hợp | Có thao tác mới chứng minh JWT, WebSocket, Cloudinary; mail thật nếu công bố | Video/ảnh và file/method tương ứng; chế độ cấu hình |
| G4 — hồ sơ | Đủ DOCX, source, SQL, setup, datasheet AI, video, GitHub | File mở được, đúng phiên bản, không chứa dữ liệu riêng không cần thiết |
| G5 — nộp/vấn đáp | Drive/sheet theo số nhóm; nộp trước giờ nhóm 30 phút; từng người giải thích phần mình | Link truy cập được và ma trận phân công có xác nhận |

Không nhận đồ án hoàn thiện chỉ dựa vào tổng số test nếu G1/G2/G4 chưa qua.

## 3. Backlog thực hiện

Nhóm điền người phụ trách thật vào từng vai trò dưới đây. Thời lượng là ước lượng tập trung, không gồm chờ mạng/upload. Các thành viên có thể làm song song các nhánh setup và báo cáo; phải đồng bộ bản chốt.

| ID | Ưu tiên | Công việc | Phụ thuộc | Phụ trách đề xuất | Ước lượng | Nghiệm thu |
|---|---|---|---|---|---|---|
| P01 | P0 | Chốt commit; thống kê working tree; chọn cấu hình/DB demo; dừng thêm tính năng | Không | Điều phối | 15–20 phút | G0; không đưa secret vào manifest |
| P02 | P0 | Hoàn thiện setup từ source sạch: JDK21, lệnh build/run có trong Git, env mẫu, host/port/database/profile | P01 | Backend/setup | 30–45 phút | Người khác không cần launcher riêng đang ignore vẫn chạy được |
| P03 | P0 | Restore PostgreSQL SQL mới vào DB rỗng tách biệt; kiểm tra FK/sequence, phối hợp Flyway, startup/login | P01/P02 | Backend/setup | 45–60 phút | G1; DB đang dùng không bị thay đổi |
| P04 | P0 | Chạy Java/JS tại commit chốt, kiểm tra render đơn/ví vừa đổi và whitespace; chỉ sửa lỗi có ảnh hưởng | P01 | Backend/UI | 30–60 phút | Kết quả gắn đúng commit; không dùng lại kết quả 875 Java lịch sử |
| P05 | P0 | Rehearsal đơn hàng, kho, tiền, quyền theo mục 4 | P03/P04 | Kiểm thử/nghiệp vụ | 45–60 phút | G2; ghi rõ bước tái hiện mọi lỗi còn lại |
| P06 | P0 | Demo JWT API, chat hai cửa sổ, ảnh mới Cloudinary và OTP; chuẩn bị COD/local khi tunnel lỗi | P02/P05 | Tích hợp | 30–45 phút | G3 hoặc ghi đúng phần chưa hoạt động |
| P07 | P0 | Chọn DOCX chính thức; giải thích source/UI từng nhóm chức năng; cập nhật role/profile, mục lục; kiểm tra render | P01, kết quả P05/P06 | Báo cáo/UI | 60–90 phút | G4 phần Word; ảnh đọc được và không mâu thuẫn source |
| P08 | P0 | Đối chiếu prompt khôi phục với app; xác nhận người, commit, phần sửa AI và test thật | Kết quả khôi phục/P01 | Từng thành viên | 30–45 phút | Datasheet có nguồn, phân biệt nguyên văn/tổng hợp, không tự nhận làm chủ 100% |
| P09 | P0 | Ma trận phân công và tập vấn đáp: mỗi người một luồng chính, một nhánh lỗi | P05/P08 | Tất cả thành viên | 30–45 phút | Giải thích được controller/service/repository, quyền và transaction/test |
| P10 | P0 | Quay video đủ mặt, giới thiệu và phần đóng góp từng người trên đúng bản chốt | P05/P06/P07 | Tất cả thành viên | 45–60 phút | Hình/tiếng rõ, thao tác có kết quả, không lộ secret |
| P11 | P0 | Đóng gói, upload Drive, link GitHub, nhân bản sheet, tự chấm theo bằng chứng | P07–P10 | Điều phối | 20–30 phút + upload | G5, kiểm tra link bằng người xem khác |
| P12 | P1 | Mobile, form sai, trạng thái rỗng, focus bàn phím, menu bốn role | P05 | UI | 30–45 phút | Trang demo chính dễ dùng, không có nút mời thao tác bị cấm |
| P13 | P1 | Chuẩn bị ứng viên sáng tạo và bằng chứng, không tính trùng công nghệ bắt buộc | P05/P09 | Nghiệp vụ/báo cáo | 20–30 phút | Mỗi mục có vấn đề, giải pháp, code/test/demo |
| P14 | P2 | Restore SQL Server, tải đồng thời, phân tích pool/N+1, nhiều instance | Sau P0 | Backend | Theo phát hiện | Đưa vào hướng phát triển nếu chưa kịp, không nhận đã hoàn tất |

P0 là việc ảnh hưởng trực tiếp tới vận hành và hạng mục nộp. P1 làm sau khi P0 có tiến triển chắc chắn. P2 không chặn bảo vệ nếu không phải phạm vi nhóm cam kết.

## 4. Ma trận kiểm thử tối thiểu

| Mã | Thao tác | Kết quả cần đạt |
|---|---|---|
| T01 | USER chọn chi nhánh A, thêm/sửa/xóa giỏ, đặt COD | Server tính đúng giá/phí/voucher; tạo một đơn; kho/giỏ đổi đúng |
| T02 | STAFF A xử lý đơn; thử thao tác quản lý/admin | Chỉ đúng quyền và đúng bước trạng thái |
| T03 | MANAGER A dùng đơn/kho/ví cửa hàng; thử cart/checkout/ví khách | Nghiệp vụ cửa hàng hoạt động; mua bị chặn server và không được mời qua UI |
| T04 | Nhân sự A đổi ID sang dữ liệu chi nhánh B | Bị chặn, không lộ hoặc sửa dữ liệu B |
| T05 | ADMIN dùng quản trị rồi thử mua hàng | Quản trị đúng quyền; không trở thành khách mua |
| T06 | Hai khách tranh món còn một; sửa giá/số lượng request | Không âm kho; server giữ giá đúng; request sai không tạo giao dịch sai |
| T07 | Hủy/hoàn đơn rồi gửi lại thao tác | Chỉ hoàn kho/tiền một lần theo điều kiện nghiệp vụ |
| T08 | Thanh toán demo thành công/hủy/hết hạn/thao tác lặp | Đơn và sổ tiền nhất quán; nhãn demo rõ |
| T09 | Lấy JWT, gọi API có/không token, sai quyền; thu hồi/đổi role | 200/401/403 đúng ngữ cảnh; quyền cập nhật theo thiết kế |
| T10 | Hai phiên khác nhau chat/nhận thông báo | Sự kiện đến đúng người/chi nhánh; không rò hội thoại |
| T11 | Upload ảnh mới và thử file không hợp lệ | Lên Cloudinary nếu công bố cloud; không coi fallback local là cloud |
| T12 | Đăng ký/quên mật khẩu, OTP sai/hết hạn/gửi lại | Đúng luồng/giới hạn; không log OTP/secret vào hồ sơ |
| T13 | Mobile, hết hàng, danh sách trống, lỗi validation | Nội dung đọc được, nút đúng quyền, thông báo xử lý được |
| T14 | Restart local, đăng nhập/xem đơn lại | Dữ liệu bền, không seed/reset ngoài ý muốn; ghi rõ giới hạn session demo |

T06/T07 cần dữ liệu thử riêng và bản sao phù hợp; không phá dữ liệu cần quay video. Không sửa test chỉ để hợp thức hóa hành vi sai. Tồn kho dùng polling không được trình bày thành WebSocket.

## 5. Hồ sơ và phân công

Ma trận nhóm cần điền: `Thành viên | Chức năng thực làm | File/method | Commit xác minh | Test | Prompt nguồn | Phần trình bày | Người đối chiếu`. Không gán người từ tên tài khoản máy hoặc số commit; cần xác nhận alias/merge nếu dùng lịch sử Git.

Mỗi nhóm chức năng trong Word cần: nhu cầu/vai trò → sơ đồ hoạt động hoặc sequence → hình diễn giải source quan trọng → UI → kịch bản thành công/lỗi → giới hạn. Không cần chụp mọi dòng code. Bản thực nghiệm mới có 68 ảnh nhúng; vẫn cần xem ảnh source có đọc được, các nhãn Aiven/local, role và số trang/mục lục có đúng không.

Gói nộp gồm:

- README vào cửa, source đúng commit và link GitHub;
- SQL đúng hệ quản trị đã restore thử, setup có lệnh thực thi thật, env mẫu không secret;
- một DOCX chính thức có kiến trúc/hoạt động/source/UI, hướng dẫn setup và danh sách prompt;
- datasheet AI nếu nhóm sử dụng, video đủ mặt và phần giới thiệu/đóng góp từng thành viên;
- link Drive theo số nhóm và sheet mẫu đã nhân bản/điền/tự chấm.

Không đưa `.env` thật, cookie/token, PostgreSQL data directory hay raw chat/database ứng dụng vào gói nộp. SQL mới có thao tác xóa schema/bảng: chỉ hướng dẫn restore vào DB thử nghiệm riêng; không khẳng định một cú click an toàn cho DB đang dùng. Không repair checksum/xóa Flyway history để vượt lỗi.

## 6. Lịch dự kiến và mốc dừng

Chưa có giờ báo cáo riêng của nhóm. Gọi thời điểm đó là `T_báo_cáo`; hạn chính thức là **T_báo_cáo trừ 30 phút**. Đặt mốc nội bộ sớm hơn ít nhất 30–60 phút để upload và kiểm tra quyền. Không mặc định mọi nhóm phải nộp lúc 6:30.

1. **0–20 phút:** P01, điền người phụ trách, xác nhận giờ nhóm.
2. **20–120 phút:** nhánh setup/restore/test và nhánh Word/prompt làm song song.
3. **120–180 phút:** rehearsal G2/G3; chỉ sửa lỗi chặn demo và cập nhật hồ sơ theo kết quả.
4. **180–240 phút:** tập vấn đáp, quay video, chốt tài liệu; thêm buffer nếu phải quay riêng.
5. **Sau đó:** đóng gói/upload/kiểm tra link. Thay đổi code sau quay phải kiểm tra tác động và quay lại đoạn liên quan.

Khoảng bốn giờ chỉ khả thi khi các thành viên làm song song và không gặp lỗi lớn, không phải cam kết xong toàn bộ backlog. Nếu còn dưới hai giờ, bỏ làm đẹp/thêm tính năng, ưu tiên cài chạy, luồng demo, hồ sơ bắt buộc, video và nộp. Không bỏ kiểm tra quyền hoặc gọi chức năng lỗi là hoàn thành.

## 7. Nội dung sáng tạo có thể bảo vệ

Không tính Spring Boot/Thymeleaf/Bootstrap/JPA/JWT/WebSocket/Cloudinary và CRUD thành từng nội dung sáng tạo vì đã thuộc yêu cầu/chương trình. Các ứng viên để nhóm đối chiếu với giảng viên:

- Chống bán vượt kho, thứ tự khóa và kiểm tra cạnh tranh.
- Sổ tiền/giữ tiền/hoàn tiền/hoa hồng với chống ghi nhận lặp.
- Quyền theo chi nhánh, chống IDOR và audit hội thoại.
- Refresh/revocation, cập nhật quyền token sau thay đổi tài khoản, vượt phát JWT cơ bản.
- Xác nhận thanh toán demo qua điện thoại và giới hạn đường dẫn tunnel.
- Chuyển tồn giữa chi nhánh có trạng thái/kiểm soát nhất quán.
- Mẫu sản phẩm chung, tồn riêng và lọc chi nhánh đóng/ngừng bán.

Không cam kết các mục được tính độc lập hoặc đủ 15/15. Mỗi người cần giải thích được lựa chọn thiết kế và giới hạn. Quản lý nguyên liệu hoa/hao hụt, giao vận thật, nhiều instance và nguyên nhân sự cố pool là hướng phát triển nếu chưa được giải quyết.

## 8. Kết quả khôi phục AI phục vụ kế hoạch

Xem `docs/KET_QUA_KHOI_PHUC_PROMPT_20261028.md` và CSV 15 dòng `docs/MINH_CHUNG_PROMPT_KHOI_PHUC_20261028.csv`. Bản đầy đủ 87 bản ghi đã che nằm riêng ở `.tools/prompt-recovery-20261028/`, được ignore.

Phân hạng nguồn: JSONL role=user là lời nhắn người dùng; SQLite/Protobuf Antigravity cần đối chiếu UI; brain/walkthrough là kết quả AI; prompt tổng hợp phải giữ nhãn tổng hợp. Một lời nhắn có thể chứa phản hồi AI được người dùng dán lại, không tự chứng minh tác giả. Không suy ra model/người nhập/commit từ tên app.

Nhóm cần chọn 8–15 ví dụ có ý nghĩa: prompt thật → AI đề xuất → nhóm phát hiện/sửa gì → commit → test/demo. Chưa tìm thấy export ChatGPT web trong phạm vi đã quét; nguồn OpenAI đã khôi phục là Codex. Các phần chưa kiểm chứng để trống, không tự điền thành tích.
