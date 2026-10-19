[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^[0-9_]+$')][string]$Suffix,
    [ValidateSet('Prepare', 'Apply')][string]$Action = 'Prepare',
    [switch]$ApplyToRentalShop
)

# Delete only the 81 user-confirmed duplicate inventory rows. Prepare first restores
# a new copy from a verified backup and rehearses the exact same SQL transaction.
# Apply requires explicit -ApplyToRentalShop, rechecks both databases, takes a new
# verified backup, and keeps an audit table with every original inventory value.
$ErrorActionPreference = 'Stop'
$server = '127.0.0.1,53735'
$copyName = "CodexDupDeleteCopy_$Suffix"
$sql = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'sql/delete-confirmed-duplicate-inventory.sql') -Raw -Encoding UTF8

function Open-LocalDatabase([string]$name) {
    $connection = [System.Data.SqlClient.SqlConnection]::new(
        "Data Source=$server;Initial Catalog=$name;Integrated Security=True;Encrypt=True;TrustServerCertificate=True;Connect Timeout=15")
    $connection.Open()
    return $connection
}
function Scalar([System.Data.SqlClient.SqlConnection]$connection, [string]$query) {
    $command = $connection.CreateCommand()
    try { $command.CommandText = $query; return $command.ExecuteScalar() }
    finally { $command.Dispose() }
}
function Snapshot([string]$name) {
    $connection = Open-LocalDatabase $name
    try {
        $command = $connection.CreateCommand()
        try {
            $command.CommandText = @'
SELECT (SELECT TOP (1) version FROM dbo.flyway_schema_history WHERE success=1 ORDER BY installed_rank DESC),
       (SELECT COUNT_BIG(*) FROM dbo.orders),
       (SELECT COUNT_BIG(*) FROM dbo.order_details),
       (SELECT COUNT_BIG(*) FROM dbo.products),
       (SELECT COUNT_BIG(*) FROM dbo.transactions),
       (SELECT COUNT_BIG(*) FROM dbo.store_inventories),
       (SELECT COALESCE(SUM(CAST(quantity AS bigint)),0) FROM dbo.store_inventories),
       (SELECT status FROM dbo.orders WHERE id=23),
       (SELECT COUNT_BIG(*) FROM dbo.store_inventories si JOIN dbo.products p ON p.id=si.product_id
        WHERE p.store_id IS NOT NULL AND si.store_id<>p.store_id)
'@
            $reader = $command.ExecuteReader()
            try {
                if (!$reader.Read()) { throw "Cannot read snapshot: $name" }
                return [pscustomobject]@{
                    name=$name; version=$reader.GetString(0); orders=$reader.GetInt64(1)
                    details=$reader.GetInt64(2); products=$reader.GetInt64(3)
                    transactions=$reader.GetInt64(4); inventory_rows=$reader.GetInt64(5)
                    stock=$reader.GetInt64(6); order23=$reader.GetString(7)
                    mismatches=$reader.GetInt64(8)
                }
            } finally { $reader.Close() }
        } finally { $command.Dispose() }
    } finally { $connection.Dispose() }
}
function Require-Original([object]$snapshot) {
    if ($snapshot.version -ne '10' -or $snapshot.orders -ne 26 -or
        $snapshot.inventory_rows -ne 154 -or $snapshot.stock -ne 3339 -or
        $snapshot.mismatches -ne 81 -or $snapshot.order23 -ne 'CANCELLED') {
        throw "Original inventory snapshot changed: $($snapshot.name)"
    }
}
function Require-Cleaned([object]$snapshot) {
    if ($snapshot.version -ne '10' -or $snapshot.orders -ne 26 -or
        $snapshot.inventory_rows -ne 73 -or $snapshot.stock -ne 1336 -or
        $snapshot.mismatches -ne 0 -or $snapshot.order23 -ne 'CANCELLED') {
        throw "Cleanup result is unexpected: $($snapshot.name)"
    }
    $connection = Open-LocalDatabase $snapshot.name
    try {
        if ([int](Scalar $connection 'SELECT COUNT(*) FROM dbo.inventory_duplicate_deletion_20261019') -ne 81 -or
            [long](Scalar $connection 'SELECT SUM(CAST(original_quantity AS bigint)) FROM dbo.inventory_duplicate_deletion_20261019') -ne 2003) {
            throw "Audit table is incomplete: $($snapshot.name)"
        }
    } finally { $connection.Dispose() }
}
function Require-UnchangedBusiness([object]$before, [object]$after) {
    foreach ($field in @('orders','details','products','transactions','order23')) {
        if ($before.$field -ne $after.$field) { throw "Business data changed: $field" }
    }
}
function Esc([string]$value) { return $value.Replace("'", "''") }
function Backup-Source([System.Data.SqlClient.SqlConnection]$master, [string]$tag) {
    $root = [string](Scalar $master "SELECT CONVERT(nvarchar(4000),SERVERPROPERTY('InstanceDefaultBackupPath'))")
    if ([string]::IsNullOrWhiteSpace($root)) { throw 'SQL Server backup path is unavailable.' }
    $path = Join-Path $root "RentalShop_before_duplicate_delete_${Suffix}_$tag.bak"
    $safe = Esc $path
    if ([int](Scalar $master "DECLARE @exists int; EXEC master.dbo.xp_fileexist N'$safe', @exists OUTPUT; SELECT @exists") -ne 0) {
        throw "Backup already exists; do not overwrite: $path"
    }
    $command = $master.CreateCommand()
    try {
        $command.CommandTimeout = 180
        $command.CommandText = "BACKUP DATABASE [RentalShop] TO DISK=N'$safe' WITH COPY_ONLY,CHECKSUM; RESTORE VERIFYONLY FROM DISK=N'$safe' WITH CHECKSUM;"
        [void]$command.ExecuteNonQuery()
    } finally { $command.Dispose() }
    Write-Host "Verified fresh backup: $path"
    return $path
}
function Invoke-Deletion([string]$name) {
    $connection = Open-LocalDatabase $name
    try {
        $command = $connection.CreateCommand()
        try {
            $command.CommandTimeout = 120
            $command.CommandText = $sql
            $reader = $command.ExecuteReader()
            try {
                if (!$reader.Read()) { throw "Deletion returned no result: $name" }
                $result = [pscustomobject]@{
                    name=$reader.GetString(0); archived=$reader.GetInt32(1)
                    units=$reader.GetInt64(2); remaining_rows=$reader.GetInt32(3)
                    remaining_stock=$reader.GetInt64(4)
                }
            } finally { $reader.Close() }
            if ($result.name -ne $name -or $result.archived -ne 81 -or
                $result.units -ne 2003 -or $result.remaining_rows -ne 73 -or
                $result.remaining_stock -ne 1336) { throw "Deletion result mismatch: $name" }
            return $result
        } finally { $command.Dispose() }
    } finally { $connection.Dispose() }
}

$sourceBefore = Snapshot 'RentalShop'
Require-Original $sourceBefore
$master = Open-LocalDatabase 'master'
try {
    if ($Action -eq 'Prepare') {
        if ((Scalar $master "SELECT DB_ID(N'$copyName')") -isnot [DBNull]) { throw "Copy already exists: $copyName" }
        $backup = Backup-Source $master 'prepare'
        $safeBackup = Esc $backup
        $fileList = [System.Data.DataTable]::new()
        $command = $master.CreateCommand()
        try {
            $command.CommandText = "RESTORE FILELISTONLY FROM DISK=N'$safeBackup'"
            $adapter = [System.Data.SqlClient.SqlDataAdapter]::new($command)
            try { [void]$adapter.Fill($fileList) } finally { $adapter.Dispose() }
        } finally { $command.Dispose() }
        if ($fileList.Rows.Count -ne 2 -or @($fileList.Rows | Where-Object Type -eq 'D').Count -ne 1 -or
            @($fileList.Rows | Where-Object Type -eq 'L').Count -ne 1) {
            throw 'Expected exactly one data file and one log file; copy not restored.'
        }
        $dataRoot = [string](Scalar $master "SELECT CONVERT(nvarchar(4000),SERVERPROPERTY('InstanceDefaultDataPath'))")
        $logRoot = [string](Scalar $master "SELECT CONVERT(nvarchar(4000),SERVERPROPERTY('InstanceDefaultLogPath'))")
        if ([string]::IsNullOrWhiteSpace($dataRoot) -or [string]::IsNullOrWhiteSpace($logRoot)) {
            throw 'SQL Server data/log paths are unavailable.'
        }
        $dataPath = Join-Path $dataRoot "$copyName.mdf"
        $logPath = Join-Path $logRoot "$copyName.ldf"
        foreach ($path in @($dataPath,$logPath)) {
            $safePath = Esc $path
            if ([int](Scalar $master "DECLARE @exists int; EXEC master.dbo.xp_fileexist N'$safePath', @exists OUTPUT; SELECT @exists") -ne 0) {
                throw "Copy file already exists; do not overwrite: $path"
            }
        }
        $dataLogical = Esc ([string]($fileList.Rows | Where-Object Type -eq 'D' | Select-Object -First 1).LogicalName)
        $logLogical = Esc ([string]($fileList.Rows | Where-Object Type -eq 'L' | Select-Object -First 1).LogicalName)
        $safeDataPath = Esc $dataPath
        $safeLogPath = Esc $logPath
        $command = $master.CreateCommand()
        try {
            $command.CommandTimeout = 180
            $command.CommandText = "RESTORE DATABASE [$copyName] FROM DISK=N'$safeBackup' WITH MOVE N'$dataLogical' TO N'$safeDataPath', MOVE N'$logLogical' TO N'$safeLogPath', RECOVERY;"
            [void]$command.ExecuteNonQuery()
        } finally { $command.Dispose() }
        $copyBefore = Snapshot $copyName
        Require-Original $copyBefore
        Require-UnchangedBusiness $sourceBefore $copyBefore
        $result = Invoke-Deletion $copyName
        $copyAfter = Snapshot $copyName
        Require-Cleaned $copyAfter
        Require-UnchangedBusiness $copyBefore $copyAfter
        $sourceAfter = Snapshot 'RentalShop'
        Require-Original $sourceAfter
        Require-UnchangedBusiness $sourceBefore $sourceAfter
        Write-Output "Copy rehearsal passed: $($result.archived) rows/$($result.units) units deleted from $copyName; RentalShop unchanged."
    } else {
        if (!$ApplyToRentalShop) { throw 'Explicit -ApplyToRentalShop is required.' }
        $copyBefore = Snapshot $copyName
        Require-Cleaned $copyBefore
        Require-UnchangedBusiness $sourceBefore $copyBefore
        [void](Backup-Source $master 'apply')
        $result = Invoke-Deletion 'RentalShop'
        $sourceAfter = Snapshot 'RentalShop'
        Require-Cleaned $sourceAfter
        Require-UnchangedBusiness $sourceBefore $sourceAfter
        Write-Output "RentalShop verified: $($result.archived) duplicate rows/$($result.units) units deleted; orders and products unchanged."
    }
} finally { $master.Dispose() }
