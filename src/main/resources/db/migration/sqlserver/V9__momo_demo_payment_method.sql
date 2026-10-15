-- Replace the generated enum check without changing existing payment data.
DECLARE @constraint sysname;
DECLARE @statement nvarchar(max);
DECLARE payment_method_checks CURSOR LOCAL FAST_FORWARD FOR
SELECT name FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID(N'dbo.payments')
  AND definition LIKE '%[[]method[]]%';
OPEN payment_method_checks;
FETCH NEXT FROM payment_method_checks INTO @constraint;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @statement = N'ALTER TABLE dbo.payments DROP CONSTRAINT ' + QUOTENAME(@constraint);
    EXEC sys.sp_executesql @statement;
    FETCH NEXT FROM payment_method_checks INTO @constraint;
END;
CLOSE payment_method_checks;
DEALLOCATE payment_method_checks;
ALTER TABLE dbo.payments WITH CHECK ADD CONSTRAINT CK_payments_method
CHECK (method IN ('COD', 'VNPAY', 'MOMO', 'MOMO_DEMO', 'CASH', 'BANK_TRANSFER'));
