[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^[0-9_]+$')][string]$Suffix,
    [switch]$ApplyToRentalShop
)

# User-confirmed legacy duplicates. Require a completed copy rehearsal, then back up and update RentalShop.
# The audit table retains every original value; no inventory/order row is deleted.
$ErrorActionPreference = 'Stop'
if (!$ApplyToRentalShop) { throw 'Source update is disabled; explicit -ApplyToRentalShop is required after approval.' }
$server = '127.0.0.1,53735'
$copyName = 'CodexCatalogCopy_20261019_0740'
$sql = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'sql/quarantine-duplicated-branch-inventory.sql') -Raw -Encoding UTF8

function Open-LocalDatabase([string]$name) {
    $connection = [System.Data.SqlClient.SqlConnection]::new(
        "Data Source=$server;Initial Catalog=$name;Integrated Security=True;Encrypt=True;TrustServerCertificate=True;Connect Timeout=15")
    $connection.Open()
    return $connection
}
function Get-Scalar([System.Data.SqlClient.SqlConnection]$connection, [string]$query) {
    $command = $connection.CreateCommand()
    try { $command.CommandText = $query; return $command.ExecuteScalar() }
    finally { $command.Dispose() }
}
function Invoke-Cleanup([string]$database) {
    $connection = Open-LocalDatabase $database
    try {
        $command = $connection.CreateCommand()
        try {
            $command.CommandTimeout = 120
            $command.CommandText = $sql
            $reader = $command.ExecuteReader()
            try {
                if (!$reader.Read()) { throw "Cleanup returned no verification row: $database" }
                $result = [pscustomobject]@{
                    database = $reader.GetString(0)
                    rows = $reader.GetInt32(1)
                    removed_duplicate_quantity = $reader.GetInt64(2)
                    remaining_raw_quantity = $reader.GetInt64(3)
                }
            } finally { $reader.Close() }
            if ($result.database -ne $database -or $result.rows -ne 81 -or
                $result.removed_duplicate_quantity -ne 2003 -or $result.remaining_raw_quantity -ne 1336) {
                throw "Unexpected cleanup result: $database"
            }
            return $result
        } finally { $command.Dispose() }
    } finally { $connection.Dispose() }
}

$copy = Open-LocalDatabase $copyName
try {
    if ([int](Get-Scalar $copy "SELECT COUNT(*) FROM dbo.orders") -ne 26) { throw 'Copy order count changed.' }
    if ((Get-Scalar $copy "SELECT OBJECT_ID(N'dbo.inventory_duplicate_cleanup_20261019', N'U')") -is [DBNull]) {
        throw 'Run the copy-only rehearsal first.'
    }
    if ([int](Get-Scalar $copy 'SELECT COUNT(*) FROM dbo.inventory_duplicate_cleanup_20261019') -ne 81 -or
        [long](Get-Scalar $copy 'SELECT SUM(CAST(original_quantity AS bigint)) FROM dbo.inventory_duplicate_cleanup_20261019') -ne 2003 -or
        [long](Get-Scalar $copy 'SELECT SUM(CAST(quantity AS bigint)) FROM dbo.store_inventories') -ne 1336) {
        throw 'Copy rehearsal does not match the confirmed snapshot.'
    }
} finally { $copy.Dispose() }
Write-Output 'Verified completed copy rehearsal: 81 rows and 2,003 duplicate units.'

$source = Open-LocalDatabase 'RentalShop'
try {
    $sourceOrderCount = [int](Get-Scalar $source 'SELECT COUNT(*) FROM dbo.orders')
    $sourceOrder23 = [string](Get-Scalar $source 'SELECT status FROM dbo.orders WHERE id=23')
    if ($sourceOrderCount -ne 26 -or $sourceOrder23 -ne 'CANCELLED') {
        throw 'Source orders changed; stop before the source backup.'
    }
    $command = $source.CreateCommand()
    try {
        $command.CommandText = "SELECT CONVERT(nvarchar(4000), SERVERPROPERTY('InstanceDefaultBackupPath'))"
        $backupRoot = [string]$command.ExecuteScalar()
        $backup = Join-Path $backupRoot "RentalShop_before_duplicate_cleanup_$Suffix.bak"
        $safeBackup = $backup.Replace("'", "''")
        $command.CommandText = "DECLARE @exists int; EXEC master.dbo.xp_fileexist N'$safeBackup', @exists OUTPUT; SELECT @exists"
        if ([int]$command.ExecuteScalar() -ne 0) { throw "Backup target already exists: $backup" }
        $command.CommandTimeout = 180
        $command.CommandText = "BACKUP DATABASE [RentalShop] TO DISK=N'$safeBackup' WITH COPY_ONLY,CHECKSUM; RESTORE VERIFYONLY FROM DISK=N'$safeBackup' WITH CHECKSUM;"
        [void]$command.ExecuteNonQuery()
        Write-Output "Source backup verified: $backup"
    } finally { $command.Dispose() }
} finally { $source.Dispose() }

$sourceResult = Invoke-Cleanup 'RentalShop'
$source = Open-LocalDatabase 'RentalShop'
try {
    if ([int](Get-Scalar $source 'SELECT COUNT(*) FROM dbo.orders') -ne $sourceOrderCount -or
        [string](Get-Scalar $source 'SELECT status FROM dbo.orders WHERE id=23') -ne $sourceOrder23) {
        throw 'Order count/status changed after cleanup.'
    }
} finally { $source.Dispose() }
Write-Output "RentalShop cleanup verified: $($sourceResult.rows) rows quarantined, $($sourceResult.remaining_raw_quantity) raw units remain; orders unchanged."
