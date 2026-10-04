-- SQL Server: additive migration; existing orders retain a zero shipping fee.
-- Apply before deploying if Hibernate schema updates are disabled.
IF COL_LENGTH('orders', 'shipping_fee') IS NULL
    ALTER TABLE orders ADD shipping_fee DECIMAL(18,2) NULL CONSTRAINT DF_orders_shipping_fee DEFAULT 0 WITH VALUES;
IF COL_LENGTH('orders', 'delivery_unit_id') IS NULL
    ALTER TABLE orders ADD delivery_unit_id BIGINT NULL;
