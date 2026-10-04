SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.payments', 'momo_environment') IS NULL
 ALTER TABLE dbo.payments ADD momo_environment VARCHAR(16) NULL;
IF OBJECT_ID('dbo.momo_payment_attempts', 'U') IS NULL
 CREATE TABLE dbo.momo_payment_attempts (
  id BIGINT IDENTITY PRIMARY KEY,
  order_id BIGINT NOT NULL REFERENCES dbo.orders(id),
  gateway_order_id VARCHAR(100) NOT NULL UNIQUE,
  request_id VARCHAR(100) NOT NULL UNIQUE,
  environment VARCHAR(16) NOT NULL,
  amount NUMERIC(38,2) NOT NULL,
  order_info NVARCHAR(255) NOT NULL,
  extra_data VARCHAR(1000) NOT NULL,
  transaction_id VARCHAR(100) NULL,
  completed BIT NOT NULL DEFAULT 0,
  created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
 );
COMMIT;
