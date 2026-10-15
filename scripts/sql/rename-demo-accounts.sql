-- Deliberately match known demo identities, not arbitrary registered customers.
-- Caller supplies @Apply and owns the transaction/backup.
DECLARE @Names TABLE (
    old_username varchar(50), old_name nvarchar(100), role varchar(20),
    new_username varchar(50), new_name nvarchar(100)
);
INSERT INTO @Names VALUES
('admin', N'Quản Trị Viên Chuỗi Hoa Tươi', 'ADMIN', 'nguyenthanhminh', N'Nguyễn Thanh Minh'),
('admin', N'Quản Trị Viên Toàn Hệ Thống', 'ADMIN', 'nguyenthanhminh', N'Nguyễn Thanh Minh'),
('manager_q1', N'Nguyễn Quản Lý Cửa Hàng Q1', 'MANAGER', 'nguyenquanghuy', N'Nguyễn Quang Huy'),
('manager_q1', N'Nguyễn Quản Lý Q1', 'MANAGER', 'nguyenquanghuy', N'Nguyễn Quang Huy'),
('khachhang', N'Lê Minh Hoa', 'USER', 'leminhhoa', N'Lê Minh Hoa'),
('khachhang', N'Lê Khách Hàng', 'USER', 'leminhhoa', N'Lê Minh Hoa'),
('khachhang2', N'Trần Thị Hoa', 'USER', 'tranthihoa', N'Trần Thị Hoa'),
('khachhang_q7', N'Nguyễn Ngọc Bích Trâm', 'USER', 'nguyenbichtram', N'Nguyễn Ngọc Bích Trâm'),
('khachhang_dn', N'Công Ty Cổ Phần Công Nghệ Nam Sài Gòn', 'USER', 'namsaigon', N'Công Ty Cổ Phần Công Nghệ Nam Sài Gòn'),
('vendor_flower', N'Trần Đối Tác Vườn Hoa Đà Lạt', 'MANAGER', 'tranduclong', N'Trần Đức Long'),
('vendor', N'Trần Đối Tác (Vendor)', 'MANAGER', 'tranminhtuan', N'Trần Minh Tuấn'),
('staff_q7_flt', N'Nguyễn Thị Mai Lan - Nghệ Nhân Cắm Hoa Chính', 'MANAGER', 'nguyenmailan', N'Nguyễn Thị Mai Lan'),
('staff_q7_order', N'Trần Bảo Phúc - Tiếp Nhận & Điều Phối Đơn Hàng', 'MANAGER', 'tranbaophuc', N'Trần Bảo Phúc'),
('staff_q7_inv', N'Lê Hoàng Khôi - Thủ Kho & Kiểm Soát Hoa Tươi', 'MANAGER', 'lehoangkhoi', N'Lê Hoàng Khôi');

DECLARE @Targets TABLE (id bigint PRIMARY KEY, old_username varchar(50), new_username varchar(50), new_name nvarchar(100));
INSERT INTO @Targets
SELECT u.id, u.username, m.new_username, m.new_name
FROM dbo.users u JOIN @Names m
ON u.username=m.old_username AND u.full_name=m.old_name AND u.role=m.role;

IF EXISTS (SELECT 1 FROM @Targets t JOIN dbo.users u ON u.username=t.new_username AND u.id<>t.id)
    THROW 50001, 'A requested username already belongs to another account; no accounts were renamed.', 1;

IF @Apply=1
    UPDATE u SET username=t.new_username, full_name=t.new_name
    FROM dbo.users u JOIN @Targets t ON t.id=u.id;

SELECT id, old_username, new_username, new_name FROM @Targets ORDER BY id;
