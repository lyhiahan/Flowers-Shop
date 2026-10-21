-- Delivery is selected at checkout. Keep the oldest row ID and latest nonblank
-- greeting/date hint while preserving all units of each BUY product and branch.
SELECT cart_id, product_id, store_id, MIN(id) AS keep_id,
       SUM(CAST(quantity AS bigint)) AS total_quantity
INTO #cart_line_groups
FROM dbo.cart_items
WHERE order_type = 'BUY'
GROUP BY cart_id, product_id, store_id
HAVING COUNT(*) > 1;

UPDATE kept
SET quantity = groups.total_quantity,
    delivery_date = latest_date.delivery_date,
    greeting_message = latest_message.greeting_message
FROM dbo.cart_items kept
JOIN #cart_line_groups groups ON groups.keep_id = kept.id
OUTER APPLY (
    SELECT TOP 1 line.delivery_date
    FROM dbo.cart_items line
    WHERE line.cart_id = groups.cart_id AND line.product_id = groups.product_id
      AND line.store_id = groups.store_id AND line.order_type = 'BUY'
      AND line.delivery_date IS NOT NULL
    ORDER BY line.id DESC
) latest_date
OUTER APPLY (
    SELECT TOP 1 line.greeting_message
    FROM dbo.cart_items line
    WHERE line.cart_id = groups.cart_id AND line.product_id = groups.product_id
      AND line.store_id = groups.store_id AND line.order_type = 'BUY'
      AND NULLIF(LTRIM(RTRIM(line.greeting_message)), N'') IS NOT NULL
    ORDER BY line.id DESC
) latest_message;

DELETE line
FROM dbo.cart_items line
JOIN #cart_line_groups groups
  ON line.cart_id = groups.cart_id AND line.product_id = groups.product_id
 AND line.store_id = groups.store_id
WHERE line.order_type = 'BUY' AND line.id <> groups.keep_id;

DROP TABLE #cart_line_groups;
