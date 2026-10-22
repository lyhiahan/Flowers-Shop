param(
    [ValidateSet('Rehearse','Apply')][string]$Mode='Rehearse',
    [Parameter(Mandatory=$true)][ValidatePattern('^(RentalShop|CodexWebPreview_[A-Za-z0-9_]+)$')][string]$DatabaseName,
    [string]$Server='127.0.0.1,1433'
)
$ErrorActionPreference='Stop'
$options=[System.Data.SqlClient.SqlConnectionStringBuilder]::new()
$options['Data Source']=$Server;$options['Initial Catalog']=$DatabaseName;$options['Integrated Security']=$true
$options['Encrypt']=$true;$options['TrustServerCertificate']=$true;$options['Connect Timeout']=5
$connection=[System.Data.SqlClient.SqlConnection]::new($options.ConnectionString)
try {
    $connection.Open()
    if($connection.Database -cne $DatabaseName){throw 'Unexpected target database.'}
    $command=$connection.CreateCommand();$command.CommandTimeout=120
    if($Mode -eq 'Apply') {
        $command.CommandText="SELECT CONVERT(nvarchar(4000),SERVERPROPERTY('InstanceDefaultBackupPath'))"
        $directory=[string]$command.ExecuteScalar()
        $path=Join-Path $directory ($DatabaseName+'_before_demo_reconciliation_'+(Get-Date -Format 'yyyyMMdd_HHmmss')+'.bak')
        if(Test-Path -LiteralPath $path){throw 'Backup already exists. Refusing to overwrite.'}
        $command.CommandText="BACKUP DATABASE [$DatabaseName] TO DISK=N'$($path.Replace("'","''"))' WITH COPY_ONLY,CHECKSUM"
        [void]$command.ExecuteNonQuery()
        $command.CommandText="RESTORE VERIFYONLY FROM DISK=N'$($path.Replace("'","''"))' WITH CHECKSUM"
        [void]$command.ExecuteNonQuery()
        Write-Output "Verified backup: $path"
    }
    $sql=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'sql/reconcile-confirmed-demo-20261021.sql') -Raw
    if($Mode -eq 'Rehearse'){$sql=$sql.Replace('COMMIT TRANSACTION;','ROLLBACK TRANSACTION;')}
    $command.CommandText=$sql
    $data=[System.Data.DataTable]::new();$reader=$command.ExecuteReader();$data.Load($reader);$reader.Close()
    $data | Format-Table -AutoSize
    Write-Output "Mode=$Mode | Database=$DatabaseName"
} finally {$connection.Dispose()}
