-- Recover only exact lossy CP1252 versions of known generated titles.
-- Custom titles and already-correct Unicode must remain untouched.
DECLARE @Templates TABLE (
    notification_type varchar(50),
    prefix nvarchar(150),
    suffix nvarchar(100),
    include_order_id bit
);
INSERT INTO @Templates VALUES
('ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'', 0),
('ORDER_NEW', N'🔔 Đơn hoa mới #', N'', 1),
('ORDER_CONFIRMED', N'✅ Đơn hoa #', N' đã được xác nhận', 1),
('ORDER_PREPARING', N'🌺 Đang cắm hoa đơn #', N'', 1),
('ORDER_DELIVERING', N'🚀 Đơn hoa #', N' đang được giao', 1),
('ORDER_DELIVERED', N'🎉 Giao hoa thành công đơn #', N'', 1),
('ORDER_COMPLETED', N'✨ Đơn hoa #', N' đã hoàn tất', 1),
('ORDER_CANCELLED', N'❌ Đơn hoa #', N' đã bị hủy', 1),
('ORDER_CANCELLED', N'⚠️ Đơn hoa #', N' đã bị hủy', 1),
('REFUND_COMPLETED', N'💰 Hoàn tiền thành công đơn #', N'', 1);

UPDATE n SET title = canonical.title
FROM dbo.notifications n
JOIN @Templates t ON t.notification_type = n.type
CROSS APPLY (VALUES (
    t.prefix + CASE WHEN t.include_order_id = 1
        THEN CONVERT(nvarchar(20), n.related_order_id) ELSE N'' END + t.suffix
)) canonical(title)
CROSS APPLY (VALUES (
    CONVERT(nvarchar(200), CONVERT(varchar(200), canonical.title COLLATE SQL_Latin1_General_CP1_CI_AS))
)) legacy(title)
WHERE n.related_order_id IS NOT NULL
  AND CHARINDEX(N'?', n.title) > 0
  AND DATALENGTH(n.title) = DATALENGTH(legacy.title)
  AND n.title COLLATE Latin1_General_100_BIN2 = legacy.title COLLATE Latin1_General_100_BIN2;
