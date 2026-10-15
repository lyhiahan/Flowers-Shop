param(
    [ValidateSet('Preview','Test','Apply')][string]$Action='Preview',
    [ValidatePattern('^(RentalShop|CodexCatalogCopy_[0-9_]+)$')][string]$DatabaseName='RentalShop'
)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Data
$connection=[System.Data.SqlClient.SqlConnection]::new('Data Source=127.0.0.1,53735;Initial Catalog=' + $DatabaseName + ';Integrated Security=True;Encrypt=True;TrustServerCertificate=True;Connect Timeout=10')
$connection.Open()
$transaction=$null
function Get-BusinessSnapshot {
    $command=$connection.CreateCommand(); $command.Transaction=$transaction
    $command.CommandText="SELECT t.name FROM sys.tables t JOIN sys.schemas s ON s.schema_id=t.schema_id WHERE s.name='dbo' ORDER BY t.name"
    $reader=$command.ExecuteReader(); $tables=@()
    try { while($reader.Read()) { $tables += $reader.GetString(0) } } finally { $reader.Close() }
    $snapshots=@{}
    foreach($table in $tables) {
        $safeTable=$table.Replace(']',']]')
        $command.CommandText="SELECT name FROM sys.columns WHERE object_id=OBJECT_ID(@table) ORDER BY column_id"
        $command.Parameters.Clear(); [void]$command.Parameters.AddWithValue('@table', 'dbo.' + $table)
        $reader=$command.ExecuteReader(); $columns=@()
        try {
            while($reader.Read()) {
                $column=$reader.GetString(0)
                if($table -ne 'users' -or $column -notin @('username','full_name')) { $columns += '[' + $column.Replace(']',']]') + ']' }
            }
        } finally { $reader.Close() }
        $command.CommandText="SELECT c.name FROM sys.indexes i JOIN sys.index_columns ic ON ic.object_id=i.object_id AND ic.index_id=i.index_id JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE i.object_id=OBJECT_ID(@table) AND i.is_primary_key=1 ORDER BY ic.key_ordinal"
        $reader=$command.ExecuteReader(); $keys=@()
        try { while($reader.Read()) { $keys += '[' + $reader.GetString(0).Replace(']',']]') + ']' } } finally { $reader.Close() }
        $order=if($keys.Count) { ' ORDER BY ' + ($keys -join ',') } else { '' }
        $command.Parameters.Clear()
        $command.CommandText='SELECT ' + ($columns -join ',') + ' FROM dbo.[' + $safeTable + ']' + $order + ' FOR JSON PATH, INCLUDE_NULL_VALUES'
        $reader=$command.ExecuteReader(); $data=[Text.StringBuilder]::new()
        try { while($reader.Read()) { [void]$data.Append($reader.GetString(0)) } } finally { $reader.Close() }
        $sha=[Security.Cryptography.SHA256]::Create()
        try { $snapshots[$table]=[Convert]::ToBase64String($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($data.ToString()))) }
        finally { $sha.Dispose() }
    }
    return $snapshots
}
try {
    if($Action -eq 'Apply' -and $DatabaseName -eq 'RentalShop') {
        $command=$connection.CreateCommand(); $command.CommandTimeout=120
        $command.CommandText="SELECT CONVERT(nvarchar(4000),SERVERPROPERTY('InstanceDefaultBackupPath'))"
        $backupPath=Join-Path ([string]$command.ExecuteScalar()) ('RentalShop_before_demo_names_' + [Guid]::NewGuid().ToString('N') + '.bak')
        $command.CommandText="BACKUP DATABASE [RentalShop] TO DISK=@path WITH COPY_ONLY,CHECKSUM; RESTORE VERIFYONLY FROM DISK=@path WITH CHECKSUM;"
        [void]$command.Parameters.AddWithValue('@path',$backupPath)
        [void]$command.ExecuteNonQuery()
        Write-Output "Backup verified: $backupPath"
    }
    $transaction=$connection.BeginTransaction([System.Data.IsolationLevel]::Serializable)
    $before=Get-BusinessSnapshot
    $command=$connection.CreateCommand(); $command.Transaction=$transaction
    $command.CommandText=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'sql/rename-demo-accounts.sql') -Raw -Encoding UTF8
    [void]$command.Parameters.Add('@Apply',[System.Data.SqlDbType]::Bit)
    $command.Parameters['@Apply'].Value=($Action -ne 'Preview')
    $table=[System.Data.DataTable]::new(); $table.Load($command.ExecuteReader())
    $after=Get-BusinessSnapshot
    foreach($name in $before.Keys) { if($before[$name] -ne $after[$name]) { throw "Unexpected business data change: $name; rolling back." } }
    if($Action -eq 'Apply') { $transaction.Commit() } else { $transaction.Rollback() }
    $transaction.Dispose(); $transaction=$null
    $table | Format-Table -AutoSize | Out-String -Width 180 | Write-Output
    Write-Output "$Action completed on $DatabaseName. Only matched demo usernames/display names can change; all other user fields and business tables verified unchanged. Test/Preview never commit."
} finally {
    if($null -ne $transaction) { $transaction.Rollback(); $transaction.Dispose() }
    $connection.Dispose()
}
