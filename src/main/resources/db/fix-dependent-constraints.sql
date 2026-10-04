-- ============================================================
-- FIX: Drop DEFAULT & UNIQUE constraints that block Hibernate
-- ddl-auto=update from altering column types in SQL Server.
-- Run this ONCE manually via SSMS or sqlcmd, then restart app.
-- ============================================================

-- 1. orders.discount_amount  --> DEFAULT constraint
DECLARE @df1 NVARCHAR(256) = (
    SELECT dc.name FROM sys.default_constraints dc
    JOIN sys.columns c ON dc.parent_object_id = c.object_id
                      AND dc.parent_column_id = c.column_id
    JOIN sys.tables t  ON c.object_id = t.object_id
    WHERE t.name = 'orders' AND c.name = 'discount_amount'
);
IF @df1 IS NOT NULL
    EXEC('ALTER TABLE dbo.orders DROP CONSTRAINT [' + @df1 + ']');

-- 2. store_levels.discount  --> DEFAULT constraint
DECLARE @df2 NVARCHAR(256) = (
    SELECT dc.name FROM sys.default_constraints dc
    JOIN sys.columns c ON dc.parent_object_id = c.object_id
                      AND dc.parent_column_id = c.column_id
    JOIN sys.tables t  ON c.object_id = t.object_id
    WHERE t.name = 'store_levels' AND c.name = 'discount'
);
IF @df2 IS NOT NULL
    EXEC('ALTER TABLE dbo.store_levels DROP CONSTRAINT [' + @df2 + ']');

-- 3. user_levels.discount  --> DEFAULT constraint
DECLARE @df3 NVARCHAR(256) = (
    SELECT dc.name FROM sys.default_constraints dc
    JOIN sys.columns c ON dc.parent_object_id = c.object_id
                      AND dc.parent_column_id = c.column_id
    JOIN sys.tables t  ON c.object_id = t.object_id
    WHERE t.name = 'user_levels' AND c.name = 'discount'
);
IF @df3 IS NOT NULL
    EXEC('ALTER TABLE dbo.user_levels DROP CONSTRAINT [' + @df3 + ']');

-- 4. vouchers.min_order_value  --> DEFAULT constraint
DECLARE @df4 NVARCHAR(256) = (
    SELECT dc.name FROM sys.default_constraints dc
    JOIN sys.columns c ON dc.parent_object_id = c.object_id
                      AND dc.parent_column_id = c.column_id
    JOIN sys.tables t  ON c.object_id = t.object_id
    WHERE t.name = 'vouchers' AND c.name = 'min_order_value'
);
IF @df4 IS NOT NULL
    EXEC('ALTER TABLE dbo.vouchers DROP CONSTRAINT [' + @df4 + ']');

-- 5. styles.name  --> UNIQUE constraint
DECLARE @uq1 NVARCHAR(256) = (
    SELECT kc.name FROM sys.key_constraints kc
    JOIN sys.index_columns ic ON kc.unique_index_id = ic.index_id
                             AND kc.parent_object_id = ic.object_id
    JOIN sys.columns c ON ic.object_id = c.object_id
                      AND ic.column_id = c.column_id
    JOIN sys.tables t  ON c.object_id = t.object_id
    WHERE t.name = 'styles' AND c.name = 'name' AND kc.type = 'UQ'
);
IF @uq1 IS NOT NULL
    EXEC('ALTER TABLE dbo.styles DROP CONSTRAINT [' + @uq1 + ']');

PRINT 'Done: all blocking constraints removed. Restart Spring Boot app now.';
