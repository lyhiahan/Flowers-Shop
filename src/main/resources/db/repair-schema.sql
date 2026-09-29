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
