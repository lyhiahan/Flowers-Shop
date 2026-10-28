-- Remove obsolete business fields only after preserving their original values.

SET XACT_ABORT ON;

IF EXISTS (SELECT 1 FROM dbo.orders WHERE order_type <> 'BUY' OR status IN ('RENTING','RETURNED','OVERDUE')) OR EXISTS (SELECT 1 FROM dbo.order_details WHERE order_type <> 'BUY') OR EXISTS (SELECT 1 FROM dbo.cart_items WHERE order_type <> 'BUY') OR EXISTS (SELECT 1 FROM dbo.promotions WHERE apply_type <> 'BUY')
    THROW 51018, 'Unsupported historical transactions found. Export and reconcile them before the sales-only schema upgrade.', 1;

CREATE TABLE dbo.schema_change_archive (
    migration_version varchar(20) NOT NULL,
    table_name varchar(64) NOT NULL,
    row_id bigint NOT NULL,
    payload nvarchar(max) NOT NULL,
    archived_at datetime2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_schema_change_archive PRIMARY KEY (migration_version, table_name, row_id)
);

INSERT INTO dbo.schema_change_archive (migration_version,table_name,row_id,payload) SELECT '18','products',s.id,(SELECT s.[price_rent_per_day], s.[deposit_fee] FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) FROM dbo.products s;

INSERT INTO dbo.schema_change_archive (migration_version,table_name,row_id,payload) SELECT '18','cart_items',s.id,(SELECT s.[rent_start_date], s.[rent_end_date] FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) FROM dbo.cart_items s;

INSERT INTO dbo.schema_change_archive (migration_version,table_name,row_id,payload) SELECT '18','order_details',s.id,(SELECT s.[deposit_fee], s.[rent_start_date], s.[rent_end_date], s.[actual_return_date] FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) FROM dbo.order_details s;

INSERT INTO dbo.schema_change_archive (migration_version,table_name,row_id,payload) SELECT '18','orders',s.id,(SELECT s.[return_store_id], s.[total_deposit], s.[fine_amount] FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) FROM dbo.orders s;

DECLARE @drop nvarchar(max);

SET @drop=N'';
SELECT @drop=@drop+N'ALTER TABLE dbo.[products] DROP CONSTRAINT '+QUOTENAME(dc.name)+N';'
FROM sys.default_constraints dc JOIN sys.columns c ON c.object_id=dc.parent_object_id AND c.column_id=dc.parent_column_id
WHERE dc.parent_object_id=OBJECT_ID(N'dbo.products') AND c.name IN ('price_rent_per_day','deposit_fee');
SELECT @drop=@drop+N'ALTER TABLE dbo.[products] DROP CONSTRAINT '+QUOTENAME(cc.name)+N';'
FROM sys.check_constraints cc WHERE cc.parent_object_id=OBJECT_ID(N'dbo.products')
AND EXISTS (SELECT 1 FROM sys.sql_expression_dependencies d JOIN sys.columns c
 ON c.object_id=d.referenced_id AND c.column_id=d.referenced_minor_id
 WHERE d.referencing_id=cc.object_id AND c.object_id=cc.parent_object_id AND c.name IN ('price_rent_per_day','deposit_fee'));
SELECT @drop=@drop+N'ALTER TABLE dbo.[products] DROP CONSTRAINT '+QUOTENAME(fk.name)+N';'
FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fc ON fc.constraint_object_id=fk.object_id
JOIN sys.columns c ON c.object_id=fc.parent_object_id AND c.column_id=fc.parent_column_id
WHERE fk.parent_object_id=OBJECT_ID(N'dbo.products') AND c.name IN ('price_rent_per_day','deposit_fee');
EXEC sp_executesql @drop;
ALTER TABLE dbo.[products] DROP COLUMN [price_rent_per_day], [deposit_fee];

SET @drop=N'';
SELECT @drop=@drop+N'ALTER TABLE dbo.[cart_items] DROP CONSTRAINT '+QUOTENAME(dc.name)+N';'
FROM sys.default_constraints dc JOIN sys.columns c ON c.object_id=dc.parent_object_id AND c.column_id=dc.parent_column_id
WHERE dc.parent_object_id=OBJECT_ID(N'dbo.cart_items') AND c.name IN ('rent_start_date','rent_end_date');
SELECT @drop=@drop+N'ALTER TABLE dbo.[cart_items] DROP CONSTRAINT '+QUOTENAME(cc.name)+N';'
FROM sys.check_constraints cc WHERE cc.parent_object_id=OBJECT_ID(N'dbo.cart_items')
AND EXISTS (SELECT 1 FROM sys.sql_expression_dependencies d JOIN sys.columns c
 ON c.object_id=d.referenced_id AND c.column_id=d.referenced_minor_id
 WHERE d.referencing_id=cc.object_id AND c.object_id=cc.parent_object_id AND c.name IN ('rent_start_date','rent_end_date'));
SELECT @drop=@drop+N'ALTER TABLE dbo.[cart_items] DROP CONSTRAINT '+QUOTENAME(fk.name)+N';'
FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fc ON fc.constraint_object_id=fk.object_id
JOIN sys.columns c ON c.object_id=fc.parent_object_id AND c.column_id=fc.parent_column_id
WHERE fk.parent_object_id=OBJECT_ID(N'dbo.cart_items') AND c.name IN ('rent_start_date','rent_end_date');
EXEC sp_executesql @drop;
ALTER TABLE dbo.[cart_items] DROP COLUMN [rent_start_date], [rent_end_date];

SET @drop=N'';
SELECT @drop=@drop+N'ALTER TABLE dbo.[order_details] DROP CONSTRAINT '+QUOTENAME(dc.name)+N';'
FROM sys.default_constraints dc JOIN sys.columns c ON c.object_id=dc.parent_object_id AND c.column_id=dc.parent_column_id
WHERE dc.parent_object_id=OBJECT_ID(N'dbo.order_details') AND c.name IN ('deposit_fee','rent_start_date','rent_end_date','actual_return_date');
SELECT @drop=@drop+N'ALTER TABLE dbo.[order_details] DROP CONSTRAINT '+QUOTENAME(cc.name)+N';'
FROM sys.check_constraints cc WHERE cc.parent_object_id=OBJECT_ID(N'dbo.order_details')
AND EXISTS (SELECT 1 FROM sys.sql_expression_dependencies d JOIN sys.columns c
 ON c.object_id=d.referenced_id AND c.column_id=d.referenced_minor_id
 WHERE d.referencing_id=cc.object_id AND c.object_id=cc.parent_object_id AND c.name IN ('deposit_fee','rent_start_date','rent_end_date','actual_return_date'));
SELECT @drop=@drop+N'ALTER TABLE dbo.[order_details] DROP CONSTRAINT '+QUOTENAME(fk.name)+N';'
FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fc ON fc.constraint_object_id=fk.object_id
JOIN sys.columns c ON c.object_id=fc.parent_object_id AND c.column_id=fc.parent_column_id
WHERE fk.parent_object_id=OBJECT_ID(N'dbo.order_details') AND c.name IN ('deposit_fee','rent_start_date','rent_end_date','actual_return_date');
EXEC sp_executesql @drop;
ALTER TABLE dbo.[order_details] DROP COLUMN [deposit_fee], [rent_start_date], [rent_end_date], [actual_return_date];

SET @drop=N'';
SELECT @drop=@drop+N'ALTER TABLE dbo.[orders] DROP CONSTRAINT '+QUOTENAME(dc.name)+N';'
FROM sys.default_constraints dc JOIN sys.columns c ON c.object_id=dc.parent_object_id AND c.column_id=dc.parent_column_id
WHERE dc.parent_object_id=OBJECT_ID(N'dbo.orders') AND c.name IN ('return_store_id','total_deposit','fine_amount');
SELECT @drop=@drop+N'ALTER TABLE dbo.[orders] DROP CONSTRAINT '+QUOTENAME(cc.name)+N';'
FROM sys.check_constraints cc WHERE cc.parent_object_id=OBJECT_ID(N'dbo.orders')
AND EXISTS (SELECT 1 FROM sys.sql_expression_dependencies d JOIN sys.columns c
 ON c.object_id=d.referenced_id AND c.column_id=d.referenced_minor_id
 WHERE d.referencing_id=cc.object_id AND c.object_id=cc.parent_object_id AND c.name IN ('return_store_id','total_deposit','fine_amount'));
SELECT @drop=@drop+N'ALTER TABLE dbo.[orders] DROP CONSTRAINT '+QUOTENAME(fk.name)+N';'
FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fc ON fc.constraint_object_id=fk.object_id
JOIN sys.columns c ON c.object_id=fc.parent_object_id AND c.column_id=fc.parent_column_id
WHERE fk.parent_object_id=OBJECT_ID(N'dbo.orders') AND c.name IN ('return_store_id','total_deposit','fine_amount');
EXEC sp_executesql @drop;
ALTER TABLE dbo.[orders] DROP COLUMN [return_store_id], [total_deposit], [fine_amount];

SET @drop=N'';
SELECT @drop=@drop+N'ALTER TABLE dbo.[orders] DROP CONSTRAINT '+QUOTENAME(name)+N';'
FROM sys.check_constraints cc WHERE cc.parent_object_id=OBJECT_ID(N'dbo.orders')
AND EXISTS (SELECT 1 FROM sys.sql_expression_dependencies d JOIN sys.columns c
 ON c.object_id=d.referenced_id AND c.column_id=d.referenced_minor_id
 WHERE d.referencing_id=cc.object_id AND c.object_id=cc.parent_object_id AND c.name=N'order_type');
EXEC sp_executesql @drop;
ALTER TABLE dbo.[orders] WITH CHECK ADD CONSTRAINT [CK_orders_order_type_sales] CHECK ([order_type] IN ('BUY'));

SET @drop=N'';
SELECT @drop=@drop+N'ALTER TABLE dbo.[order_details] DROP CONSTRAINT '+QUOTENAME(name)+N';'
FROM sys.check_constraints cc WHERE cc.parent_object_id=OBJECT_ID(N'dbo.order_details')
AND EXISTS (SELECT 1 FROM sys.sql_expression_dependencies d JOIN sys.columns c
 ON c.object_id=d.referenced_id AND c.column_id=d.referenced_minor_id
 WHERE d.referencing_id=cc.object_id AND c.object_id=cc.parent_object_id AND c.name=N'order_type');
EXEC sp_executesql @drop;
ALTER TABLE dbo.[order_details] WITH CHECK ADD CONSTRAINT [CK_order_details_order_type_sales] CHECK ([order_type] IN ('BUY'));

SET @drop=N'';
SELECT @drop=@drop+N'ALTER TABLE dbo.[cart_items] DROP CONSTRAINT '+QUOTENAME(name)+N';'
FROM sys.check_constraints cc WHERE cc.parent_object_id=OBJECT_ID(N'dbo.cart_items')
AND EXISTS (SELECT 1 FROM sys.sql_expression_dependencies d JOIN sys.columns c
 ON c.object_id=d.referenced_id AND c.column_id=d.referenced_minor_id
 WHERE d.referencing_id=cc.object_id AND c.object_id=cc.parent_object_id AND c.name=N'order_type');
EXEC sp_executesql @drop;
ALTER TABLE dbo.[cart_items] WITH CHECK ADD CONSTRAINT [CK_cart_items_order_type_sales] CHECK ([order_type] IN ('BUY'));

SET @drop=N'';
SELECT @drop=@drop+N'ALTER TABLE dbo.[promotions] DROP CONSTRAINT '+QUOTENAME(name)+N';'
FROM sys.check_constraints cc WHERE cc.parent_object_id=OBJECT_ID(N'dbo.promotions')
AND EXISTS (SELECT 1 FROM sys.sql_expression_dependencies d JOIN sys.columns c
 ON c.object_id=d.referenced_id AND c.column_id=d.referenced_minor_id
 WHERE d.referencing_id=cc.object_id AND c.object_id=cc.parent_object_id AND c.name=N'apply_type');
EXEC sp_executesql @drop;
ALTER TABLE dbo.[promotions] WITH CHECK ADD CONSTRAINT [CK_promotions_apply_type_sales] CHECK ([apply_type] IN ('BUY'));

SET @drop=N'';
SELECT @drop=@drop+N'ALTER TABLE dbo.[orders] DROP CONSTRAINT '+QUOTENAME(name)+N';'
FROM sys.check_constraints cc WHERE cc.parent_object_id=OBJECT_ID(N'dbo.orders')
AND EXISTS (SELECT 1 FROM sys.sql_expression_dependencies d JOIN sys.columns c
 ON c.object_id=d.referenced_id AND c.column_id=d.referenced_minor_id
 WHERE d.referencing_id=cc.object_id AND c.object_id=cc.parent_object_id AND c.name=N'status');
EXEC sp_executesql @drop;
ALTER TABLE dbo.[orders] WITH CHECK ADD CONSTRAINT [CK_orders_status_sales] CHECK ([status] IN ('PENDING','CONFIRMED','PREPARING','PICKED_UP','DELIVERING','DELIVERED','COMPLETED','CANCELLED','REFUND_REQUESTED','REFUNDED'));
