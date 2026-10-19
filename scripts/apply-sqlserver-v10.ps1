[CmdletBinding()]
param([ValidatePattern('^[0-9_]+$')][string]$Suffix = '20261019_0800')

# Apply only the V9/V10 schema already verified on a disposable copy.
# Never delete/replace a backup, copy, business row, or order.
$ErrorActionPreference = 'Stop'
$server = '127.0.0.1,53735'
$copyName = 'CodexCatalogCopy_20261019_0740'
function Open-LocalDatabase([string]$name) {
    $connection = [System.Data.SqlClient.SqlConnection]::new(
        "Data Source=$server;Initial Catalog=$name;Integrated Security=True;Encrypt=True;TrustServerCertificate=True;Connect Timeout=15")
    $connection.Open()
    return $connection
}
function Get-Snapshot([string]$name) {
    $connection = Open-LocalDatabase $name
    try {
        $command = $connection.CreateCommand()
        $command.CommandText = @'
SELECT (SELECT TOP (1) version FROM dbo.flyway_schema_history WHERE success=1 ORDER BY installed_rank DESC) AS version,
       (SELECT COUNT_BIG(*) FROM dbo.orders) AS orders,
       (SELECT COUNT_BIG(*) FROM dbo.store_inventories) AS inventory_rows,
       (SELECT COALESCE(SUM(CAST(quantity AS bigint)),0) FROM dbo.store_inventories) AS stock,
       (SELECT COUNT_BIG(*) FROM dbo.transactions) AS transactions,
       (SELECT COUNT_BIG(*) FROM dbo.orders WHERE payment_method='MOMO_DEMO') AS demo_orders
'@
        $reader = $command.ExecuteReader()
        try {
            if (!$reader.Read()) { throw "No snapshot from $name" }
            return [pscustomobject]@{
                database=$name; version=$reader.GetString(0); orders=$reader.GetInt64(1)
                inventory_rows=$reader.GetInt64(2); stock=$reader.GetInt64(3)
                transactions=$reader.GetInt64(4); demo_orders=$reader.GetInt64(5)
            }
        } finally { $reader.Close() }
    } finally { $connection.Dispose() }
}

$source = Get-Snapshot 'RentalShop'
$copy = Get-Snapshot $copyName
if ($source.version -ne '8' -or $copy.version -ne '10') { throw 'Expected source V8 and verified copy V10.' }
foreach ($field in @('orders','inventory_rows','stock','transactions','demo_orders')) {
    if ($source.$field -ne $copy.$field) { throw "Source changed after copy: $field" }
}
if ($source.demo_orders -ne 0) { throw 'Demo payment orders need separate backfill review.' }

$master = Open-LocalDatabase 'master'
try {
    $command = $master.CreateCommand()
    $command.CommandTimeout = 180
    $command.CommandText = "SELECT CONVERT(nvarchar(4000),SERVERPROPERTY('InstanceDefaultBackupPath'))"
    $backupRoot = [string]$command.ExecuteScalar()
    $backup = Join-Path $backupRoot "RentalShop_before_v10_$Suffix.bak"
    $safeBackup = $backup.Replace("'", "''")
    $command.CommandText = "DECLARE @exists int; EXEC master.dbo.xp_fileexist N'$safeBackup', @exists OUTPUT; SELECT @exists"
    if ([int]$command.ExecuteScalar() -ne 0) { throw "Backup target already exists: $backup" }
    $command.CommandText = "BACKUP DATABASE [RentalShop] TO DISK=N'$safeBackup' WITH COPY_ONLY,CHECKSUM; RESTORE VERIFYONLY FROM DISK=N'$safeBackup' WITH CHECKSUM;"
    [void]$command.ExecuteNonQuery()
    Write-Output "Fresh backup verified: $backup"
} finally { $master.Dispose() }

$names = @('SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD')
$previous = @{}
foreach ($name in $names) { $previous[$name] = [Environment]::GetEnvironmentVariable($name,'Process') }
try {
    $credential = Get-Content -LiteralPath (Join-Path $PSScriptRoot '../.tools/sqlserver-local.json') -Raw | ConvertFrom-Json
    if ($credential.Username -ne 'rental_user') { throw 'Unexpected SQL login.' }
    $secure = ConvertTo-SecureString $credential.ProtectedPassword
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { $env:SPRING_DATASOURCE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer); $secure.Dispose() }
    $env:SPRING_DATASOURCE_USERNAME = $credential.Username
    $env:SPRING_DATASOURCE_URL = 'jdbc:sqlserver://127.0.0.1:53735;databaseName=RentalShop;encrypt=true;trustServerCertificate=true;loginTimeout=15;'
    & (Join-Path $PSScriptRoot 'maven.ps1') -MavenArguments @('compile','flyway:migrate','-B')
    if ($LASTEXITCODE -ne 0) { throw 'Migration failed; inspect Flyway history before retrying.' }
} finally {
    foreach ($name in $names) {
        if ($null -eq $previous[$name]) { Remove-Item -LiteralPath ("Env:\$name") -ErrorAction SilentlyContinue }
        else { [Environment]::SetEnvironmentVariable($name,$previous[$name],'Process') }
    }
}

$after = Get-Snapshot 'RentalShop'
if ($after.version -ne '10') { throw "Unexpected version after migration: $($after.version)" }
foreach ($field in @('orders','inventory_rows','stock','transactions','demo_orders')) {
    if ($source.$field -ne $after.$field) { throw "Unexpected business count change: $field" }
}
$connection = Open-LocalDatabase 'RentalShop'
try {
    $command = $connection.CreateCommand()
    $command.CommandText = @'
SELECT CASE WHEN COL_LENGTH('dbo.stores','pending_balance') IS NOT NULL
 AND COL_LENGTH('dbo.orders','funds_status') IS NOT NULL
 AND COL_LENGTH('dbo.orders','funds_amount') IS NOT NULL
 AND COL_LENGTH('dbo.orders','commission_rate_snapshot') IS NOT NULL
 AND COL_LENGTH('dbo.transactions','wallet_bucket') IS NOT NULL THEN 1 ELSE 0 END
'@
    if ([int]$command.ExecuteScalar() -ne 1) { throw 'One or more V10 columns are missing.' }
} finally { $connection.Dispose() }
Write-Output 'RentalShop V10 verified; orders, inventory, quantities and transactions unchanged.'
