-- READ ONLY. Run against the intended database after checking the first result set.
-- No stock transfer, deletion, order update, or automatic repair.
-- A mismatch means products.store_id is not null and differs from inventory.store_id.
SET NOCOUNT ON;

SELECT DB_NAME() AS database_name, @@SERVERNAME AS server_name,
       SYSDATETIMEOFFSET() AS observed_at,
       (SELECT COUNT(*) FROM dbo.store_inventories) AS inventory_rows,
       (SELECT SUM(CAST(quantity AS bigint)) FROM dbo.store_inventories) AS all_quantity,
       (SELECT COUNT(*) FROM dbo.store_inventories WHERE quantity < 0) AS negative_rows;

SELECT id, name FROM dbo.stores ORDER BY id;

SELECT p.id, p.name, p.store_id AS owner_store_id, p.is_active, p.is_selling, p.is_deleted
FROM dbo.products p
WHERE EXISTS (SELECT 1 FROM dbo.store_inventories si
              WHERE si.product_id = p.id AND si.store_id <> p.store_id)
ORDER BY p.id;

SELECT si.id AS inventory_id, si.store_id, si.product_id, si.quantity,
       si.version, si.is_selling, si.is_deleted, p.store_id AS owner_store_id,
       own.id AS owner_inventory_id, own.quantity AS owner_quantity,
       (SELECT COUNT(DISTINCT o.id)
        FROM dbo.orders o JOIN dbo.order_details d ON d.order_id = o.id
        WHERE d.product_id = si.product_id
          AND (o.pickup_store_id = si.store_id OR o.return_store_id = si.store_id)) AS linked_orders,
       (SELECT COUNT(DISTINCT o.id)
        FROM dbo.orders o JOIN dbo.order_details d ON d.order_id = o.id
        WHERE d.product_id = si.product_id
          AND (o.pickup_store_id = si.store_id OR o.return_store_id = si.store_id)
          AND o.status NOT IN ('CANCELLED', 'COMPLETED', 'REFUNDED')) AS unsettled_orders
FROM dbo.store_inventories si
JOIN dbo.products p ON p.id = si.product_id
LEFT JOIN dbo.store_inventories own ON own.product_id = p.id AND own.store_id = p.store_id
WHERE p.store_id IS NOT NULL AND si.store_id <> p.store_id
ORDER BY si.store_id, si.product_id, si.id;

-- DELIVERED is conservatively considered unsettled here, not assumed final.
-- Keep returned/rental and refund-related orders visible for manual review.
SELECT DISTINCT o.id AS order_id, o.status, o.order_type, o.pickup_store_id,
       o.return_store_id, d.id AS detail_id, d.product_id, d.quantity,
       p.store_id AS product_owner_store_id, o.payment_status, o.version
FROM dbo.orders o
JOIN dbo.order_details d ON d.order_id = o.id
JOIN dbo.products p ON p.id = d.product_id
WHERE EXISTS (SELECT 1 FROM dbo.store_inventories si
              WHERE si.product_id = d.product_id AND si.store_id <> p.store_id
                AND (si.store_id = o.pickup_store_id OR si.store_id = o.return_store_id))
   OR o.id = 23
ORDER BY o.id, d.id;

SELECT OBJECT_NAME(fk.parent_object_id) AS referencing_table,
       fk.name AS foreign_key_name
FROM sys.foreign_keys fk
WHERE fk.referenced_object_id = OBJECT_ID('dbo.store_inventories');
