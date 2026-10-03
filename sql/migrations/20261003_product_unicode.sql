-- Generated from ProductSeederService.java by scripts/build-product-unicode-repair.cjs.
-- Only restores unambiguous seed names whose preserved characters AND price match.
-- Does not delete products or change their price, stock, store, images or orders.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id=t.user_type_id
           WHERE c.object_id=OBJECT_ID('dbo.products') AND c.name='name' AND t.name <> 'nvarchar')
    ALTER TABLE dbo.products ALTER COLUMN name NVARCHAR(200) NOT NULL;

DECLARE @catalog TABLE (correct_name NVARCHAR(200) NOT NULL PRIMARY KEY, price DECIMAL(18,2) NOT NULL);
INSERT INTO @catalog (correct_name,price) VALUES
(N'Bó Hoa Hồng Đỏ Ecuador Nhập Khẩu 99 Bông ''Tình Yêu Vĩnh Cửu''', 1200000),
(N'Bó Hoa Hướng Dương ''Mặt Trời Tỏa Sáng''', 480000),
(N'Kệ Hoa Khai Trương ''Hồng Phát Đại Cát'' 2 Tầng', 1850000),
(N'Giỏ Hoa Khai Trương ''Phát Tài Phát Lộc'' Tone Cam Vàng', 950000),
(N'Hộp Hoa Tròn Sang Trọng ''Trái Tim Nồng Cháy'' Hồng Đỏ', 850000),
(N'Chậu Lan Hồ Điệp 6 Cành Trắng Lưỡi Đỏ ''Thịnh Vượng''', 2500000),
(N'Bình Hoa Hồng Ohara & Cúc Mẫu Đơn Để Bàn Sang Trọng', 780000),
(N'Bó Hoa Cưới Cầm Tay Cô Dâu Tone Trắng ''Nguyện Ước''', 890000),
(N'Kệ Hoa Chia Buồn ''Thành Kính Phân Ưu'' Tone Trắng Tím', 1150000),
(N'Hộp Hoa Sinh Nhật Tone Hồng Pastel ''Thiên Thần Ngọt Ngào''', 650000),
(N'Bó Hoa Cưới Cầm Tay Cô Dâu Tone Trắng Tinh Khôi', 890000),
(N'Bó Hoa Tulip Hà Lan Tone Hồng Pastel Hàn Quốc', 950000),
(N'Giỏ Hoa Sinh Nhật ''Thanh Xuân Rực Rỡ'' Phối Hoa Baby', 720000),
(N'Kệ Hoa Khai Trương Hiện Đại Tone Đỏ Hồng ''Vạn Sự Hanh Thông''', 1950000),
(N'Chậu Lan Hồ Điệp Vàng Hoàng Yến 5 Cành ''Kim Ngân Đắc Lộc''', 2300000),
(N'Bình Hoa Cẩm Tú Cầu Xanh & Hoa Baby Decor Phòng Khách', 690000),
(N'Bó Hoa Baby Trắng Bồng Bềnh ''Giấc Mơ Tuyết Trắng''', 520000),
(N'Bó Hoa Hướng Dương Chúc Mừng Khởi Đầu Mới', 620000),
(N'Chậu Lan Hồ Điệp 5 Cành Tím Hoàng Gia', 2200000),
(N'Bó Hoa Hướng Dương Tốt Nghiệp ''Khát Vọng Bay Cao''', 390000),
(N'Bó Hoa Cúc Tana Tinh Khôi Dễ Thương Tặng Sinh Nhật', 350000),
(N'Giỏ Hoa Gỗ Mộc Tone Pastel Chúc Mừng Thầy Cô & Bạn Bè', 550000),
(N'Hộp Hoa Mica Trong Suốt Tone Hồng ''Nắng Ban Mai''', 490000),
(N'Kệ Hoa Chúc Mừng Đại Hội & Sự Kiện Sinh Viên 1 Tầng', 850000),
(N'Bình Hoa Thủy Tinh Cắm Hoa Cúc Họa Mi Để Bàn Học', 280000),
(N'Bó Hoa Hồng Nhập Khẩu 20 Bông ''Tỏ Tình Lãng Mạn''', 580000),
(N'Kệ Hoa Khai Trương Câu Lạc Bộ Sinh Viên Rực Rỡ', 790000);

-- Escape existing LIKE metacharacters; only the lost '?' positions are wildcards.
SELECT p.id, CONVERT(NVARCHAR(200),p.name) AS previous_name, MIN(c.correct_name) AS restored_name
INTO #repairs
FROM dbo.products p JOIN @catalog c ON p.price_buy=c.price
 AND LEN(p.name)=LEN(c.correct_name)
 AND c.correct_name COLLATE Latin1_General_100_BIN2 LIKE
     REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(CONVERT(NVARCHAR(200),p.name),N'!',N'!!'),N'%',N'!%'),N'_',N'!_'),N'[',N'!['),N'?',N'_') COLLATE Latin1_General_100_BIN2 ESCAPE N'!'
WHERE CHARINDEX(N'?',p.name)>0
GROUP BY p.id,p.name HAVING COUNT(*)=1;

IF OBJECT_ID('dbo.product_name_unicode_repair_backup','U') IS NULL
    CREATE TABLE dbo.product_name_unicode_repair_backup (
        product_id BIGINT NOT NULL PRIMARY KEY,
        previous_name NVARCHAR(200) NOT NULL,
        restored_name NVARCHAR(200) NOT NULL,
        repaired_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
INSERT INTO dbo.product_name_unicode_repair_backup (product_id,previous_name,restored_name)
SELECT r.id,r.previous_name,r.restored_name FROM #repairs r
WHERE NOT EXISTS (SELECT 1 FROM dbo.product_name_unicode_repair_backup b WHERE b.product_id=r.id);
UPDATE p SET name=r.restored_name FROM dbo.products p JOIN #repairs r ON p.id=r.id;
SELECT COUNT(*) AS restored_product_names FROM #repairs;
SELECT p.id,p.name FROM dbo.products p JOIN #repairs r ON p.id=r.id ORDER BY p.id;
SELECT COUNT(*) AS unmatched_question_mark_names FROM dbo.products WHERE CHARINDEX(N'?',name)>0;
COMMIT TRANSACTION;
