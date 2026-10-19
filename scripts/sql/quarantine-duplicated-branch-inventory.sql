-- One-time, reversible cleanup of confirmed legacy duplicate stock.
-- Keep row IDs for old order references; preserve original values in the audit table.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF DB_NAME() NOT IN (N'RentalShop', N'CodexCatalogCopy_20261019_0740')
    THROW 51000, 'Unexpected database for duplicate-stock cleanup.', 1;
IF (SELECT TOP (1) version FROM dbo.flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC) <> '10'
    THROW 51001, 'Flyway V10 is required.', 1;
IF OBJECT_ID(N'dbo.inventory_duplicate_cleanup_20261019', N'U') IS NOT NULL
    THROW 51002, 'Cleanup audit table already exists; do not rerun.', 1;
IF (SELECT COUNT(*) FROM dbo.store_inventories) <> 154
   OR (SELECT COALESCE(SUM(CAST(quantity AS bigint)), 0) FROM dbo.store_inventories) <> 3339
   OR (SELECT COUNT(*) FROM dbo.orders) <> 26
    THROW 51003, 'Business snapshot changed; re-audit before cleanup.', 1;
IF (SELECT COUNT(*) FROM dbo.store_inventories si JOIN dbo.products p ON p.id = si.product_id
    WHERE p.store_id IS NOT NULL AND si.store_id <> p.store_id) <> 81
   OR (SELECT COALESCE(SUM(CAST(si.quantity AS bigint)), 0) FROM dbo.store_inventories si
       JOIN dbo.products p ON p.id = si.product_id
       WHERE p.store_id IS NOT NULL AND si.store_id <> p.store_id) <> 2003
    THROW 51004, 'Duplicate-stock rows changed; re-audit before cleanup.', 1;
IF EXISTS (SELECT 1 FROM dbo.store_inventories si JOIN dbo.products p ON p.id = si.product_id
           WHERE p.store_id IS NOT NULL AND si.store_id <> p.store_id
             AND (si.quantity < 0 OR NOT EXISTS
                  (SELECT 1 FROM dbo.store_inventories owner_stock
                   WHERE owner_stock.product_id = p.id AND owner_stock.store_id = p.store_id)))
    THROW 51005, 'A duplicate lacks valid owner stock or has negative quantity.', 1;

CREATE TABLE dbo.inventory_duplicate_cleanup_20261019 (
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

INSERT INTO dbo.inventory_duplicate_cleanup_20261019
    (inventory_id, product_id, duplicate_store_id, owner_store_id,
     original_quantity, original_is_selling, original_is_deleted, original_version)
SELECT si.id, si.product_id, si.store_id, p.store_id,
       si.quantity, si.is_selling, si.is_deleted, si.version
FROM dbo.store_inventories si
JOIN dbo.products p ON p.id = si.product_id
WHERE p.store_id IS NOT NULL AND si.store_id <> p.store_id;

IF @@ROWCOUNT <> 81 THROW 51006, 'Audit capture was incomplete.', 1;

UPDATE si
SET si.quantity = 0, si.is_selling = 0, si.is_deleted = 1,
    si.version = COALESCE(si.version, 0) + 1
FROM dbo.store_inventories si
JOIN dbo.inventory_duplicate_cleanup_20261019 audit ON audit.inventory_id = si.id
WHERE si.quantity = audit.original_quantity
  AND (si.version = audit.original_version OR (si.version IS NULL AND audit.original_version IS NULL));

IF @@ROWCOUNT <> 81 THROW 51007, 'Not all duplicate rows were quarantined.', 1;
IF (SELECT COALESCE(SUM(CAST(quantity AS bigint)), 0) FROM dbo.store_inventories) <> 1336
   OR EXISTS (SELECT 1 FROM dbo.store_inventories si
              JOIN dbo.inventory_duplicate_cleanup_20261019 audit ON audit.inventory_id = si.id
              WHERE si.quantity <> 0 OR si.is_selling <> 0 OR si.is_deleted <> 1)
    THROW 51008, 'Post-cleanup inventory verification failed.', 1;

COMMIT TRANSACTION;
SELECT DB_NAME() AS database_name,
       (SELECT COUNT(*) FROM dbo.inventory_duplicate_cleanup_20261019) AS quarantined_rows,
       (SELECT SUM(CAST(original_quantity AS bigint)) FROM dbo.inventory_duplicate_cleanup_20261019) AS quarantined_quantity,
       (SELECT SUM(CAST(quantity AS bigint)) FROM dbo.store_inventories) AS remaining_raw_quantity;
