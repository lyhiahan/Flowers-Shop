param([string]$Server = '127.0.0.1,1433')
$ErrorActionPreference = 'Stop'
if ($Server -notmatch '^(127\.0\.0\.1|localhost)(,[0-9]{1,5})?$') {
    throw 'This test only accepts a local SQL Server, never the shared database.'
}
$root = Split-Path -Parent $PSScriptRoot
$database = 'CodexCustomerWallet_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '_' + $PID
if ($database -notmatch '^CodexCustomerWallet_[0-9_]+$') { throw 'Invalid disposable database name.' }
$options = [System.Data.SqlClient.SqlConnectionStringBuilder]::new()
$options['Data Source'] = $Server
$options['Initial Catalog'] = 'master'
$options['Integrated Security'] = $true
$options['Encrypt'] = $true
$options['TrustServerCertificate'] = $true
$options['Connect Timeout'] = 5
$connection = [System.Data.SqlClient.SqlConnection]::new($options.ConnectionString)
$created = $false
function Execute-Sql([string]$Sql) {
    $command = $connection.CreateCommand()
    try { $command.CommandTimeout = 30; $command.CommandText = $Sql; [void]$command.ExecuteNonQuery() }
    finally { $command.Dispose() }
}
function Assert-Rejected([string]$Sql, [int[]]$Numbers) {
    try { Execute-Sql $Sql }
    catch [System.Data.SqlClient.SqlException] {
        if ($Numbers -contains $_.Exception.Number) { return }
        throw
    }
    throw 'The database accepted an invalid wallet operation.'
}
try {
    $connection.Open()
    Execute-Sql "CREATE DATABASE [$database]"
    $created = $true
    $connection.ChangeDatabase($database)
    Execute-Sql (Get-Content -LiteralPath (Join-Path $root 'src/main/resources/db/migration/sqlserver/V1__initial_schema.sql') -Raw)
    Execute-Sql (Get-Content -LiteralPath (Join-Path $root 'src/main/resources/db/migration/sqlserver/V17__customer_demo_wallet.sql') -Raw)
    Execute-Sql @'
INSERT INTO dbo.users(username,password,email,full_name,role,is_email_active)
VALUES('wallet-schema-test','unused','wallet-schema@test.invalid',N'Wallet schema test','USER',0);
INSERT INTO dbo.stores(name,address,version) VALUES(N'Wallet schema branch',N'Test only',0);
INSERT INTO dbo.orders(user_id,pickup_store_id,order_type,status,payment_method,payment_status,total_amount,version)
VALUES(1,1,'BUY','PENDING','WALLET_DEMO','PAID',100000,0);
INSERT INTO dbo.payments(order_id,method,status,amount) VALUES(1,'WALLET_DEMO','SUCCESS',100000);
INSERT INTO dbo.customer_wallets(user_id,balance,version) VALUES(1,100000,0);
INSERT INTO dbo.customer_wallet_entries(user_id,order_id,type,amount,balance_after,created_at)
VALUES(1,1,'REFUND',100000,100000,SYSDATETIME());
'@
    Assert-Rejected 'INSERT INTO dbo.customer_wallets(user_id,balance,version) VALUES(1,0,0)' @(2627,2601)
    Assert-Rejected 'UPDATE dbo.customer_wallets SET balance=-1 WHERE user_id=1' @(547)
    Assert-Rejected "INSERT INTO dbo.customer_wallet_entries(user_id,order_id,type,amount,balance_after,created_at) VALUES(1,1,'REFUND',100000,200000,SYSDATETIME())" @(2627,2601)
    Assert-Rejected "INSERT INTO dbo.customer_wallet_entries(user_id,order_id,type,amount,balance_after,created_at) VALUES(1,1,'PAYMENT',0,0,SYSDATETIME())" @(547)
    Assert-Rejected "INSERT INTO dbo.customer_wallet_entries(user_id,order_id,type,amount,balance_after,created_at) VALUES(1,1,'TOPUP',100000,200000,SYSDATETIME())" @(547)
    Assert-Rejected "INSERT INTO dbo.payments(order_id,method,status,amount) VALUES(1,'ARBITRARY','SUCCESS',100000)" @(547)
    Write-Output 'PASS: V17 SQL Server DDL, WALLET_DEMO payment, unique wallet/refund, nonnegative balance, positive amount and allowed entry types.'
} finally {
    if ($created) {
        $connection.ChangeDatabase('master')
        Execute-Sql "ALTER DATABASE [$database] SET SINGLE_USER WITH ROLLBACK IMMEDIATE"
        Execute-Sql "DROP DATABASE [$database]"
        Write-Output "Removed disposable database: $database"
    }
    $connection.Dispose()
}
