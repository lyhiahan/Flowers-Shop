-- Optional manual rollback of this demo-only batch. Never undo later payments/refunds.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF DB_NAME()<>'RentalShop' AND DB_NAME() NOT LIKE 'CodexWebPreview[_]%'
    THROW 51100,'Unexpected rollback database.',1;
IF OBJECT_ID('dbo.demo_reconciliation_rollback_20261021') IS NOT NULL
    THROW 51101,'Rollback was already recorded.',1;
IF (SELECT COUNT(*) FROM dbo.demo_payment_reconciliation_20261021)<>15
 OR (SELECT COUNT(*) FROM dbo.demo_inventory_reconciliation_20261021)<>22
    THROW 51102,'Unexpected audit snapshot.',1;
IF EXISTS(SELECT 1 FROM dbo.demo_payment_reconciliation_20261021 a LEFT JOIN dbo.payments p ON p.id=a.payment_id
          WHERE p.id IS NULL OR p.status<>'SUCCESS' OR p.amount<>a.amount OR p.transaction_code<>a.transaction_code OR p.refunded_at IS NOT NULL)
    THROW 51103,'A reconciled payment changed after the batch. Review manually.',1;
IF EXISTS(SELECT 1 FROM dbo.demo_inventory_reconciliation_20261021 a LEFT JOIN dbo.store_inventories si ON si.id=a.inventory_id
          WHERE si.id IS NULL OR si.quantity<>a.original_quantity OR si.version<>COALESCE(a.original_version,0)+1 OR si.is_deleted<>1 OR si.is_selling<>0)
    THROW 51104,'Quarantined inventory changed after the batch. Review manually.',1;
DELETE p FROM dbo.payments p JOIN dbo.demo_payment_reconciliation_20261021 a ON a.payment_id=p.id;
IF @@ROWCOUNT<>15 THROW 51105,'Incomplete receipt rollback.',1;
UPDATE si SET is_selling=a.original_is_selling,is_deleted=a.original_is_deleted,version=a.original_version
FROM dbo.store_inventories si JOIN dbo.demo_inventory_reconciliation_20261021 a ON a.inventory_id=si.id;
IF @@ROWCOUNT<>22 THROW 51106,'Incomplete inventory rollback.',1;
CREATE TABLE dbo.demo_reconciliation_rollback_20261021(recorded_at datetimeoffset NOT NULL, receipts_removed int NOT NULL, inventory_rows_restored int NOT NULL);
INSERT dbo.demo_reconciliation_rollback_20261021 VALUES(SYSDATETIMEOFFSET(),15,22);
COMMIT TRANSACTION;
