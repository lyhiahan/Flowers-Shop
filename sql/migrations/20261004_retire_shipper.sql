-- Keep old accounts, requests and order links. Never delete historical rows.
IF COL_LENGTH('dbo.users', 'retired_delivery_account') IS NULL
    ALTER TABLE dbo.users ADD retired_delivery_account BIT NULL;

EXEC('UPDATE dbo.users SET is_active = 0, retired_delivery_account = 1 WHERE role = ''SHIPPER'' OR retired_delivery_account = 1');

IF OBJECT_ID('dbo.role_requests', 'U') IS NOT NULL
    UPDATE dbo.role_requests
    SET status = 'REJECTED', rejection_reason = N'Vai trò giao hàng đã được ngừng sử dụng.'
    WHERE target_role = 'SHIPPER' AND status = 'PENDING';
