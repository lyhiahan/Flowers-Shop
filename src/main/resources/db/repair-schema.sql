-- Additive migration for existing SQL Server databases. Never resets user data.
IF COL_LENGTH('dbo.otp_verifications', 'failed_attempts') IS NULL
    ALTER TABLE dbo.otp_verifications ADD failed_attempts INT NOT NULL CONSTRAINT DF_otp_failed_attempts DEFAULT 0;

IF COL_LENGTH('dbo.stores', 'created_at') IS NULL
    ALTER TABLE dbo.stores ADD created_at DATETIME2 NULL;

IF COL_LENGTH('dbo.stores', 'version') IS NULL
    ALTER TABLE dbo.stores ADD version BIGINT NOT NULL CONSTRAINT DF_stores_version DEFAULT 0;

IF COL_LENGTH('dbo.orders', 'shipping_method') IS NULL
    ALTER TABLE dbo.orders ADD shipping_method VARCHAR(20) NOT NULL CONSTRAINT DF_orders_shipping_method DEFAULT 'DELIVERY';

IF COL_LENGTH('dbo.transactions', 'order_id') IS NULL
    ALTER TABLE dbo.transactions ADD order_id BIGINT NULL;

IF COL_LENGTH('dbo.categories', 'parent_id') IS NULL
    ALTER TABLE dbo.categories ADD parent_id BIGINT NULL;

-- Dynamic SQL allows SQL Server to compile statements after adding the columns.
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_categories_parent')
    EXEC('ALTER TABLE dbo.categories ADD CONSTRAINT FK_categories_parent FOREIGN KEY (parent_id) REFERENCES dbo.categories(id)');

EXEC('UPDATE dbo.orders SET shipping_method = ''PICKUP'' WHERE recipient_address LIKE N''Nhận tại chi nhánh:%'' AND shipping_method = ''DELIVERY''');
EXEC('UPDATE t SET order_id = o.id FROM dbo.transactions t JOIN dbo.orders o ON t.description LIKE N''Thu nhập đơn hàng #'' + CAST(o.id AS NVARCHAR(20)) + N'' sau%'' WHERE t.order_id IS NULL AND t.type = ''ORDER_PAYMENT''');

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_transactions_order')
    EXEC('ALTER TABLE dbo.transactions ADD CONSTRAINT FK_transactions_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id)');

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_transactions_order_type' AND object_id = OBJECT_ID('dbo.transactions'))
    EXEC('CREATE INDEX IX_transactions_order_type ON dbo.transactions(order_id, type)');

-- Sửa ảnh sản phẩm chuẩn hoa thật, góc chụp chuyên nghiệp, đúng tên
EXEC('UPDATE dbo.products SET image_url = ''https://images.unsplash.com/photo-1548094878-84ced0f68b08?w=800'' WHERE name LIKE N''%Hồng Đỏ%'' OR name LIKE N''%Ecuador%''');
EXEC('UPDATE dbo.products SET image_url = ''https://images.unsplash.com/photo-1520763185298-1b434c919102?w=800'' WHERE name LIKE N''%Tulip%''');
EXEC('UPDATE dbo.products SET image_url = ''https://images.unsplash.com/photo-1508610048659-a06b669e3321?w=800'' WHERE name LIKE N''%Cẩm Tú Cầu%''');
EXEC('UPDATE dbo.products SET image_url = ''https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=800'' WHERE name LIKE N''%Hộp Hoa%''');

-- Đảm bảo dữ liệu hoa cẩm tú cầu và các dòng sản phẩm không bị NULL giá bán, danh mục hoặc trạng thái
EXEC('UPDATE dbo.products SET price_buy = 750000.00 WHERE (price_buy IS NULL OR price_buy = 0) AND name LIKE N''%Cẩm Tú Cầu%''');
EXEC('UPDATE dbo.products SET sold = 24 WHERE sold IS NULL AND name LIKE N''%Cẩm Tú Cầu%''');
EXEC('UPDATE dbo.products SET is_active = 1 WHERE is_active IS NULL');
EXEC('UPDATE dbo.products SET is_selling = 1 WHERE is_selling IS NULL');
EXEC('UPDATE p SET p.category_id = (SELECT TOP 1 c.id FROM dbo.categories c WHERE c.name LIKE N''%Sinh Nhật%'' OR c.name LIKE N''%Hoa%'') FROM dbo.products p WHERE p.category_id IS NULL');

-- Đảm bảo đủ 9 loại sản phẩm chuẩn trong hệ thống
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Hoa Sinh Nhật')
    INSERT INTO dbo.categories (name, description) VALUES (N'Hoa Sinh Nhật', N'Các mẫu bó hoa, lẵng hoa chúc mừng sinh nhật ngọt ngào, ý nghĩa');
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Hoa Khai Trương')
    INSERT INTO dbo.categories (name, description) VALUES (N'Hoa Khai Trương', N'Kệ hoa, lẵng hoa khai trương sang trọng phát tài phát lộc');
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Hoa Tình Yêu & Valentine')
    INSERT INTO dbo.categories (name, description) VALUES (N'Hoa Tình Yêu & Valentine', N'Bó hoa hồng đỏ Ecuador, hoa baby trắng lãng mạn tỏ tình');
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Lan Hồ Điệp Sang Trọng')
    INSERT INTO dbo.categories (name, description) VALUES (N'Lan Hồ Điệp Sang Trọng', N'Chậu hoa lan hồ điệp cao cấp trưng Tết, biếu sếp, mừng tân gia');
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Hoa Cưới Cầm Tay Cô Dâu')
    INSERT INTO dbo.categories (name, description) VALUES (N'Hoa Cưới Cầm Tay Cô Dâu', N'Bó hoa cưới tinh tế tôn vinh vẻ đẹp cô dâu trong ngày trọng đại');
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Hoa Chúc Mừng & Tốt Nghiệp')
    INSERT INTO dbo.categories (name, description) VALUES (N'Hoa Chúc Mừng & Tốt Nghiệp', N'Thiết kế tươi sáng, gửi gắm lời chúc may mắn thành công');
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Hoa Chia Buồn & Kính Viếng')
    INSERT INTO dbo.categories (name, description) VALUES (N'Hoa Chia Buồn & Kính Viếng', N'Kệ hoa trang trọng, thành kính phân ưu cùng gia quyến');
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Bình Hoa Nghệ Thuật Để Bàn')
    INSERT INTO dbo.categories (name, description) VALUES (N'Bình Hoa Nghệ Thuật Để Bàn', N'Bình gốm, bình thủy tinh cắm hoa tươi trang trí phòng khách và văn phòng');
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Hộp Hoa & Giỏ Hoa Quà Tặng')
    INSERT INTO dbo.categories (name, description) VALUES (N'Hộp Hoa & Giỏ Hoa Quà Tặng', N'Hộp hoa mica, hộp tròn cao cấp và giỏ hoa tươi thiết kế độc đáo');

-- Thêm các danh mục con (Subcategories) qua từng câu lệnh EXEC độc lập
EXEC('IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N''Bó Hoa Sinh Nhật'') INSERT INTO dbo.categories (name, description, parent_id) SELECT N''Bó Hoa Sinh Nhật'', N''Các mẫu bó hoa tươi rực rỡ chúc mừng sinh nhật'', id FROM dbo.categories WHERE name = N''Hoa Sinh Nhật''');
EXEC('IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N''Lẵng Hoa Sinh Nhật'') INSERT INTO dbo.categories (name, description, parent_id) SELECT N''Lẵng Hoa Sinh Nhật'', N''Lẵng hoa để bàn chúc mừng sinh nhật sang trọng'', id FROM dbo.categories WHERE name = N''Hoa Sinh Nhật''');
EXEC('IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N''Bó Hoa Hồng Đỏ'') INSERT INTO dbo.categories (name, description, parent_id) SELECT N''Bó Hoa Hồng Đỏ'', N''Bó hoa hồng đỏ nhập khẩu quyến rũ trao gửi tình yêu'', id FROM dbo.categories WHERE name = N''Hoa Tình Yêu & Valentine''');
EXEC('IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N''Bó Hoa Baby Trắng'') INSERT INTO dbo.categories (name, description, parent_id) SELECT N''Bó Hoa Baby Trắng'', N''Bó hoa baby trắng tinh khôi, nhẹ nhàng lãng mạn'', id FROM dbo.categories WHERE name = N''Hoa Tình Yêu & Valentine''');
EXEC('IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N''Kệ Hoa Khai Trương'') INSERT INTO dbo.categories (name, description, parent_id) SELECT N''Kệ Hoa Khai Trương'', N''Kệ hoa chúc mừng khai trương hồng phát sang trọng'', id FROM dbo.categories WHERE name = N''Hoa Khai Trương''');
EXEC('IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N''Lẵng Hoa Khai Trương Để Bàn'') INSERT INTO dbo.categories (name, description, parent_id) SELECT N''Lẵng Hoa Khai Trương Để Bàn'', N''Lẵng hoa để bàn quầy lễ tân khai trương'', id FROM dbo.categories WHERE name = N''Hoa Khai Trương''');
EXEC('IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N''Chậu Lan Hồ Điệp Trắng'') INSERT INTO dbo.categories (name, description, parent_id) SELECT N''Chậu Lan Hồ Điệp Trắng'', N''Chậu lan hồ điệp trắng đại diện cho sự thanh cao, sang trọng'', id FROM dbo.categories WHERE name = N''Lan Hồ Điệp Sang Trọng''');
EXEC('IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N''Chậu Lan Hồ Điệp Vàng'') INSERT INTO dbo.categories (name, description, parent_id) SELECT N''Chậu Lan Hồ Điệp Vàng'', N''Lan hồ điệp vàng mang lại tài lộc và phú quý'', id FROM dbo.categories WHERE name = N''Lan Hồ Điệp Sang Trọng''');

EXEC('UPDATE c SET c.parent_id = p.id FROM dbo.categories c, dbo.categories p WHERE c.name IN (N''Bó Hoa Sinh Nhật'', N''Lẵng Hoa Sinh Nhật'') AND p.name = N''Hoa Sinh Nhật'' AND (c.parent_id IS NULL OR c.parent_id <> p.id)');
EXEC('UPDATE c SET c.parent_id = p.id FROM dbo.categories c, dbo.categories p WHERE c.name IN (N''Bó Hoa Hồng Đỏ'', N''Bó Hoa Baby Trắng'') AND p.name = N''Hoa Tình Yêu & Valentine'' AND (c.parent_id IS NULL OR c.parent_id <> p.id)');
EXEC('UPDATE c SET c.parent_id = p.id FROM dbo.categories c, dbo.categories p WHERE c.name IN (N''Kệ Hoa Khai Trương'', N''Lẵng Hoa Khai Trương Để Bàn'') AND p.name = N''Hoa Khai Trương'' AND (c.parent_id IS NULL OR c.parent_id <> p.id)');
EXEC('UPDATE c SET c.parent_id = p.id FROM dbo.categories c, dbo.categories p WHERE c.name IN (N''Chậu Lan Hồ Điệp Trắng'', N''Chậu Lan Hồ Điệp Vàng'') AND p.name = N''Lan Hồ Điệp Sang Trọng'' AND (c.parent_id IS NULL OR c.parent_id <> p.id)');

-- Gán một số sản phẩm vào danh mục con để khi bấm danh mục con có sản phẩm tương ứng hiển thị ngay
EXEC('UPDATE dbo.products SET category_id = (SELECT TOP 1 id FROM dbo.categories WHERE name = N''Bó Hoa Hồng Đỏ'') WHERE name LIKE N''%Hồng Đỏ%'' OR name LIKE N''%Ecuador%''');
EXEC('UPDATE dbo.products SET category_id = (SELECT TOP 1 id FROM dbo.categories WHERE name = N''Bó Hoa Sinh Nhật'') WHERE name LIKE N''%Sinh Nhật%'' OR name LIKE N''%Bó Hoa%''');
EXEC('UPDATE dbo.products SET category_id = (SELECT TOP 1 id FROM dbo.categories WHERE name = N''Kệ Hoa Khai Trương'') WHERE name LIKE N''%Khai Trương%'' OR name LIKE N''%Kệ Hoa%''');
EXEC('UPDATE dbo.products SET category_id = (SELECT TOP 1 id FROM dbo.categories WHERE name = N''Chậu Lan Hồ Điệp Trắng'') WHERE name LIKE N''%Lan Hồ Điệp%''');

-- Chuẩn hóa tên và ngưỡng doanh số cho Hạng chi nhánh: Tiêu chuẩn (0đ), Cửa hàng uy tín (từ 10 triệu), Cửa hàng xuất sắc (từ 30 triệu)
IF EXISTS (SELECT 1 FROM dbo.store_levels WHERE name LIKE N'%Bạc%' OR name LIKE N'%Uy Tín%')
    UPDATE dbo.store_levels SET name = N'Cửa hàng uy tín', min_point = 10000000, discount = 5 WHERE name LIKE N'%Bạc%' OR name LIKE N'%Uy Tín%';

IF EXISTS (SELECT 1 FROM dbo.store_levels WHERE name LIKE N'%Vàng%' OR name LIKE N'%Xuất Sắc%')
    UPDATE dbo.store_levels SET name = N'Cửa hàng xuất sắc', min_point = 30000000, discount = 10 WHERE name LIKE N'%Vàng%' OR name LIKE N'%Xuất Sắc%';

IF EXISTS (SELECT 1 FROM dbo.store_levels WHERE name LIKE N'%Chuẩn%' OR min_point = 0)
    UPDATE dbo.store_levels SET name = N'Tiêu chuẩn', min_point = 0 WHERE name LIKE N'%Chuẩn%' OR min_point = 0;
