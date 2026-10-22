-- Only for the demo dataset explicitly confirmed by the user on 2026-10-21.
-- The PowerShell wrapper backs up before Apply, and rolls back for Rehearse.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF (SELECT TOP 1 version FROM dbo.flyway_schema_history WHERE success=1 ORDER BY installed_rank DESC) <> '15'
    THROW 51001, 'Migrate and validate schema V15 before reconciliation.', 1;
IF OBJECT_ID('dbo.demo_payment_reconciliation_20261021') IS NOT NULL OR OBJECT_ID('dbo.demo_inventory_reconciliation_20261021') IS NOT NULL
    THROW 51002, 'Reconciliation was already recorded. Audit before repeating.', 1;
IF (SELECT COUNT(*) FROM dbo.orders) <> 18
    THROW 51003, 'Order snapshot changed. Re-audit before reconciliation.', 1;
SELECT o.id INTO #missing_receipts FROM dbo.orders o WITH (UPDLOCK,HOLDLOCK)
WHERE o.payment_status='PAID' AND NOT EXISTS(SELECT 1 FROM dbo.payments p WHERE p.order_id=o.id AND p.status='SUCCESS' AND p.amount=o.total_amount);
IF (SELECT COUNT(*) FROM #missing_receipts) <> 15 OR EXISTS(SELECT 1 FROM dbo.payments p JOIN #missing_receipts m ON m.id=p.order_id)
    THROW 51004, 'Receipt snapshot changed or an existing payment needs manual reconciliation.', 1;
IF EXISTS(SELECT 1 FROM dbo.orders o JOIN #missing_receipts m ON m.id=o.id WHERE o.total_amount<=0 OR o.payment_method NOT IN ('COD','MOMO','VNPAY'))
    THROW 51005, 'Unexpected demo payment method or amount.', 1;
SELECT si.id INTO #misplaced FROM dbo.store_inventories si WITH(UPDLOCK,HOLDLOCK) JOIN dbo.products p ON p.id=si.product_id
WHERE p.store_id IS NOT NULL AND p.store_id<>si.store_id;
IF (SELECT COUNT(*) FROM #misplaced) <> 22 OR (SELECT SUM(CAST(si.quantity AS bigint)) FROM dbo.store_inventories si JOIN #misplaced m ON m.id=si.id) <> 212
    THROW 51006, 'Inventory snapshot changed. Re-audit first.', 1;
IF EXISTS(SELECT 1 FROM #misplaced m JOIN dbo.store_inventories si ON si.id=m.id JOIN dbo.order_details d ON d.product_id=si.product_id JOIN dbo.orders o ON o.id=d.order_id WHERE o.pickup_store_id=si.store_id OR o.return_store_id=si.store_id)
    THROW 51007, 'Misplaced inventory has order references. Review manually.', 1;
DECLARE @stock bigint=(SELECT SUM(CAST(quantity AS bigint)) FROM dbo.store_inventories);
DECLARE @wallet_checksum int=(SELECT CHECKSUM_AGG(BINARY_CHECKSUM(id,e_wallet,pending_balance,point)) FROM dbo.stores);
DECLARE @order_checksum int=(SELECT CHECKSUM_AGG(BINARY_CHECKSUM(id,status,payment_status,total_amount,pickup_store_id)) FROM dbo.orders);
CREATE TABLE dbo.demo_payment_reconciliation_20261021(
    payment_id bigint PRIMARY KEY, order_id bigint NOT NULL, amount decimal(18,2) NOT NULL,
    method varchar(20) NOT NULL, transaction_code varchar(255) NOT NULL,
    recorded_at datetimeoffset NOT NULL DEFAULT SYSDATETIMEOFFSET());
INSERT dbo.payments(order_id,method,amount,status,transaction_code,momo_environment,paid_at)
OUTPUT inserted.id,inserted.order_id,inserted.amount,inserted.method,inserted.transaction_code
INTO dbo.demo_payment_reconciliation_20261021(payment_id,order_id,amount,method,transaction_code)
SELECT o.id,o.payment_method,o.total_amount,'SUCCESS','DEMO-RECONCILE-'+CONVERT(varchar(20),o.id),
       CASE WHEN o.payment_method='MOMO' THEN 'mock' ELSE NULL END,o.created_at
FROM dbo.orders o JOIN #missing_receipts m ON m.id=o.id;
IF @@ROWCOUNT<>15 THROW 51008, 'Incomplete demo receipt reconciliation.',1;
CREATE TABLE dbo.demo_inventory_reconciliation_20261021(
    inventory_id bigint PRIMARY KEY, store_id bigint NOT NULL, product_id bigint NOT NULL, owner_store_id bigint NOT NULL,
    original_quantity int NOT NULL, original_is_selling bit NULL, original_is_deleted bit NULL, original_version bigint NULL,
    recorded_at datetimeoffset NOT NULL DEFAULT SYSDATETIMEOFFSET());
INSERT dbo.demo_inventory_reconciliation_20261021(inventory_id,store_id,product_id,owner_store_id,original_quantity,original_is_selling,original_is_deleted,original_version)
SELECT si.id,si.store_id,si.product_id,p.store_id,si.quantity,si.is_selling,si.is_deleted,si.version
FROM dbo.store_inventories si JOIN #misplaced m ON m.id=si.id JOIN dbo.products p ON p.id=si.product_id;
UPDATE si SET is_selling=0,is_deleted=1,version=COALESCE(si.version,0)+1
FROM dbo.store_inventories si JOIN #misplaced m ON m.id=si.id;
IF @@ROWCOUNT<>22 THROW 51009, 'Incomplete demo inventory quarantine.',1;
IF @stock<>(SELECT SUM(CAST(quantity AS bigint)) FROM dbo.store_inventories)
 OR @wallet_checksum<>(SELECT CHECKSUM_AGG(BINARY_CHECKSUM(id,e_wallet,pending_balance,point)) FROM dbo.stores)
 OR @order_checksum<>(SELECT CHECKSUM_AGG(BINARY_CHECKSUM(id,status,payment_status,total_amount,pickup_store_id)) FROM dbo.orders)
    THROW 51010, 'Stock quantities, wallets or orders changed unexpectedly.',1;
IF EXISTS(SELECT 1 FROM dbo.orders o WHERE o.payment_status='PAID' AND NOT EXISTS(SELECT 1 FROM dbo.payments p WHERE p.order_id=o.id AND p.status='SUCCESS' AND p.amount=o.total_amount))
    THROW 51011, 'Paid orders still lack matching demo receipts.',1;
SELECT (SELECT COUNT(*) FROM dbo.demo_payment_reconciliation_20261021) AS demo_receipts_added,
       (SELECT COUNT(*) FROM dbo.demo_inventory_reconciliation_20261021) AS stock_rows_quarantined,
       @stock AS total_quantity_preserved;
COMMIT TRANSACTION;
