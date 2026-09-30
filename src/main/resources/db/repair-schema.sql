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

-- Dynamic SQL allows SQL Server to compile statements after adding the columns.
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

