-- Run on the application's SQL Server test database first, before deploying this code.
-- Additive, repeatable migration; no guessed historical hours or product selling states.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.orders','delivery_time') IS NULL ALTER TABLE dbo.orders ADD delivery_time TIME NULL;
IF OBJECT_ID('dbo.store_memberships', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.store_memberships (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        store_id BIGINT NOT NULL REFERENCES dbo.stores(id),
        user_id BIGINT NOT NULL REFERENCES dbo.users(id),
        access_role VARCHAR(16) NOT NULL CHECK(access_role IN ('PARTNER','STAFF')),
        active BIT NOT NULL DEFAULT 1,
        joined_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT uq_store_membership UNIQUE(store_id,user_id)
    );
    CREATE INDEX ix_membership_user_active ON dbo.store_memberships(user_id,active,store_id);
END;
INSERT INTO dbo.store_memberships(store_id,user_id,access_role,active)
SELECT u.store_id,u.id,CASE WHEN u.role='STAFF' THEN 'STAFF' ELSE 'PARTNER' END,1
FROM dbo.users u JOIN dbo.stores s ON s.id=u.store_id
WHERE u.role IN ('STAFF','VENDOR','MANAGER') AND (s.owner_id IS NULL OR s.owner_id<>u.id)
AND NOT EXISTS(SELECT 1 FROM dbo.store_memberships m WHERE m.user_id=u.id AND m.store_id=u.store_id);
IF COL_LENGTH('dbo.users','created_at') IS NULL ALTER TABLE dbo.users ADD created_at DATETIME2 NULL;
IF COL_LENGTH('dbo.users','creation_time_known') IS NULL ALTER TABLE dbo.users ADD creation_time_known BIT NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.stores','featured_image_public_ids') IS NULL ALTER TABLE dbo.stores ADD featured_image_public_ids NVARCHAR(MAX) NULL;
IF COL_LENGTH('dbo.stores','creation_request_key') IS NULL ALTER TABLE dbo.stores ADD creation_request_key VARCHAR(80) NULL;
IF COL_LENGTH('dbo.transactions','request_key') IS NULL ALTER TABLE dbo.transactions ADD request_key VARCHAR(80) NULL;
-- Dynamic SQL compiles against the columns added above on the first run as well.
EXEC(N'UPDATE dbo.users SET created_at=CAST(created_date AS datetime2), creation_time_known=0 WHERE created_at IS NULL AND created_date IS NOT NULL;');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='ux_store_creation_request' AND object_id=OBJECT_ID('dbo.stores'))
    EXEC(N'CREATE UNIQUE INDEX ux_store_creation_request ON dbo.stores(creation_request_key) WHERE creation_request_key IS NOT NULL;');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='ux_transaction_request' AND object_id=OBJECT_ID('dbo.transactions'))
    EXEC(N'CREATE UNIQUE INDEX ux_transaction_request ON dbo.transactions(request_key) WHERE request_key IS NOT NULL;');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='ix_user_created_at' AND object_id=OBJECT_ID('dbo.users'))
    EXEC(N'CREATE INDEX ix_user_created_at ON dbo.users(created_at DESC,id DESC);');
IF OBJECT_ID('dbo.product_style_values', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.product_style_values (
        product_id BIGINT NOT NULL REFERENCES dbo.products(id),
        style_value_id BIGINT NOT NULL REFERENCES dbo.style_values(id),
        CONSTRAINT pk_product_style_values PRIMARY KEY(product_id,style_value_id)
    );
    CREATE INDEX ix_product_style_value ON dbo.product_style_values(style_value_id,product_id);
END;
COMMIT;
