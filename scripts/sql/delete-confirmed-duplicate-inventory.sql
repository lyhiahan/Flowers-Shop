-- One-time deletion of user-confirmed duplicated branch inventory.
-- Preserve every original row in the audit table and a verified database backup.
-- Products, owner stock, orders and order details are never deleted.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF DB_NAME() <> N'RentalShop' AND DB_NAME() NOT LIKE N'CodexDupDeleteCopy[_]20261019[_]%'
    THROW 51100, 'Unexpected database for duplicate-inventory deletion.', 1;
IF (SELECT TOP (1) version FROM dbo.flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC) <> '10'
    THROW 51101, 'Flyway V10 is required.', 1;
IF OBJECT_ID(N'dbo.inventory_duplicate_deletion_20261019', N'U') IS NOT NULL
    THROW 51102, 'Deletion audit table already exists; do not run twice.', 1;
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE referenced_object_id = OBJECT_ID(N'dbo.store_inventories'))
    THROW 51103, 'Inventory has foreign-key references; deletion is unsafe.', 1;

DECLARE @locked_rows int;
SELECT @locked_rows = COUNT(*) FROM dbo.store_inventories WITH (TABLOCKX, HOLDLOCK);
IF @locked_rows <> 154
   OR (SELECT COALESCE(SUM(CAST(quantity AS bigint)), 0) FROM dbo.store_inventories) <> 3339
   OR (SELECT COUNT(*) FROM dbo.orders) <> 26
   OR NOT EXISTS (SELECT 1 FROM dbo.orders WHERE id = 23 AND status = 'CANCELLED')
    THROW 51104, 'Business snapshot changed; stop and re-audit.', 1;
IF (SELECT COUNT(*) FROM dbo.store_inventories si JOIN dbo.products p ON p.id = si.product_id
    WHERE p.store_id IS NOT NULL AND si.store_id <> p.store_id) <> 81
   OR (SELECT COALESCE(SUM(CAST(si.quantity AS bigint)), 0) FROM dbo.store_inventories si
       JOIN dbo.products p ON p.id = si.product_id
       WHERE p.store_id IS NOT NULL AND si.store_id <> p.store_id) <> 2003
    THROW 51105, 'Duplicate inventory changed; stop and re-audit.', 1;
IF EXISTS (SELECT 1 FROM dbo.store_inventories si JOIN dbo.products p ON p.id = si.product_id
           WHERE p.store_id IS NOT NULL AND si.store_id <> p.store_id
             AND (si.quantity < 0 OR NOT EXISTS
                  (SELECT 1 FROM dbo.store_inventories owner_stock
                   WHERE owner_stock.product_id = p.id AND owner_stock.store_id = p.store_id)))
    THROW 51106, 'A duplicate is negative or lacks owner stock.', 1;

CREATE TABLE dbo.inventory_duplicate_deletion_20261019 (
    inventory_id bigint NOT NULL PRIMARY KEY,
    product_id bigint NOT NULL,
    duplicate_store_id bigint NOT NULL,
    owner_store_id bigint NOT NULL,
    original_quantity int NOT NULL,
    original_is_selling bit NOT NULL,
    original_is_deleted bit NOT NULL,
    original_version bigint NULL,
    recorded_at datetimeoffset NOT NULL DEFAULT SYSDATETIMEOFFSET()
);

INSERT INTO dbo.inventory_duplicate_deletion_20261019
    (inventory_id, product_id, duplicate_store_id, owner_store_id,
     original_quantity, original_is_selling, original_is_deleted, original_version)
SELECT si.id, si.product_id, si.store_id, p.store_id,
       si.quantity, si.is_selling, si.is_deleted, si.version
FROM dbo.store_inventories si
JOIN dbo.products p ON p.id = si.product_id
WHERE p.store_id IS NOT NULL AND si.store_id <> p.store_id;

IF @@ROWCOUNT <> 81 THROW 51107, 'Audit capture was incomplete.', 1;

DELETE si
FROM dbo.store_inventories si
JOIN dbo.inventory_duplicate_deletion_20261019 audit ON audit.inventory_id = si.id
WHERE si.quantity = audit.original_quantity
  AND (si.version = audit.original_version OR (si.version IS NULL AND audit.original_version IS NULL));

IF @@ROWCOUNT <> 81 THROW 51108, 'Not all duplicated rows were deleted.', 1;
IF (SELECT COUNT(*) FROM dbo.store_inventories) <> 73
   OR (SELECT COALESCE(SUM(CAST(quantity AS bigint)), 0) FROM dbo.store_inventories) <> 1336
   OR EXISTS (SELECT 1 FROM dbo.store_inventories si JOIN dbo.products p ON p.id = si.product_id
              WHERE p.store_id IS NOT NULL AND si.store_id <> p.store_id)
    THROW 51109, 'Post-deletion inventory verification failed.', 1;

COMMIT TRANSACTION;
SELECT DB_NAME() AS database_name,
       (SELECT COUNT(*) FROM dbo.inventory_duplicate_deletion_20261019) AS archived_rows,
       (SELECT SUM(CAST(original_quantity AS bigint)) FROM dbo.inventory_duplicate_deletion_20261019) AS archived_quantity,
       (SELECT COUNT(*) FROM dbo.store_inventories) AS remaining_rows,
       (SELECT SUM(CAST(quantity AS bigint)) FROM dbo.store_inventories) AS remaining_quantity;
