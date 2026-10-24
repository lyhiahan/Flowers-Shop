-- Demo credits are separate from store funds and cannot be cashed out.
-- V16 is already owned by the shared database's store-staff role migration.
CREATE TABLE dbo.customer_wallets (
    user_id bigint NOT NULL PRIMARY KEY,
    balance decimal(18,2) NOT NULL CONSTRAINT DF_customer_wallet_balance DEFAULT 0,
    version bigint NOT NULL CONSTRAINT DF_customer_wallet_version DEFAULT 0,
    CONSTRAINT FK_customer_wallet_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT CK_customer_wallet_balance CHECK (balance >= 0)
);
CREATE TABLE dbo.customer_wallet_entries (
    id bigint IDENTITY NOT NULL PRIMARY KEY,
    user_id bigint NOT NULL,
    order_id bigint NOT NULL,
    type varchar(12) NOT NULL,
    amount decimal(18,2) NOT NULL,
    balance_after decimal(18,2) NOT NULL,
    created_at datetime2(7) NOT NULL,
    CONSTRAINT FK_customer_wallet_entry_wallet FOREIGN KEY (user_id) REFERENCES dbo.customer_wallets(user_id),
    CONSTRAINT FK_customer_wallet_entry_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id),
    CONSTRAINT UK_customer_wallet_order_type UNIQUE (order_id,type),
    CONSTRAINT CK_customer_wallet_entry_type CHECK (type IN ('PAYMENT','REFUND')),
    CONSTRAINT CK_customer_wallet_entry_amount CHECK (amount > 0 AND balance_after >= 0)
);
CREATE INDEX IX_customer_wallet_history ON dbo.customer_wallet_entries(user_id,id);

DECLARE @constraint sysname;
DECLARE @statement nvarchar(max);
DECLARE payment_method_checks CURSOR LOCAL FAST_FORWARD FOR
SELECT name FROM sys.check_constraints
WHERE parent_object_id=OBJECT_ID(N'dbo.payments') AND definition LIKE '%method%';
OPEN payment_method_checks;
FETCH NEXT FROM payment_method_checks INTO @constraint;
WHILE @@FETCH_STATUS=0
BEGIN
    SET @statement=N'ALTER TABLE dbo.payments DROP CONSTRAINT '+QUOTENAME(@constraint);
    EXEC sys.sp_executesql @statement;
    FETCH NEXT FROM payment_method_checks INTO @constraint;
END;
CLOSE payment_method_checks;
DEALLOCATE payment_method_checks;
ALTER TABLE dbo.payments WITH CHECK ADD CONSTRAINT CK_payments_method
CHECK (method IN ('COD','VNPAY','MOMO','MOMO_DEMO','WALLET_DEMO','CASH','BANK_TRANSFER'));
