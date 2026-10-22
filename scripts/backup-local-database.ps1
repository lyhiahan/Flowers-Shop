param(
    [Parameter(Mandatory=$true)][ValidatePattern('^[A-Za-z][A-Za-z0-9_]*$')][string]$DatabaseName,
    [string]$Server='127.0.0.1,1433'
)
$ErrorActionPreference='Stop'
$options=[System.Data.SqlClient.SqlConnectionStringBuilder]::new()
$options['Data Source']=$Server;$options['Initial Catalog']='master';$options['Integrated Security']=$true
$options['Encrypt']=$true;$options['TrustServerCertificate']=$true;$options['Connect Timeout']=5
$connection=[System.Data.SqlClient.SqlConnection]::new($options.ConnectionString)
try {
    $connection.Open();$command=$connection.CreateCommand();$command.CommandTimeout=120
    $command.CommandText="SELECT CONVERT(nvarchar(4000),SERVERPROPERTY('InstanceDefaultBackupPath'))"
    $directory=[string]$command.ExecuteScalar()
    $path=Join-Path $directory ($DatabaseName+'_before_remediation_'+(Get-Date -Format 'yyyyMMdd_HHmmss')+'.bak')
    if(Test-Path -LiteralPath $path){throw 'Backup file already exists. Refusing to overwrite.'}
    $command.CommandText="BACKUP DATABASE [$DatabaseName] TO DISK=N'$($path.Replace("'","''"))' WITH COPY_ONLY,CHECKSUM"
    [void]$command.ExecuteNonQuery()
    $command.CommandText="RESTORE VERIFYONLY FROM DISK=N'$($path.Replace("'","''"))' WITH CHECKSUM"
    [void]$command.ExecuteNonQuery()
    Write-Output "Verified backup: $path"
}finally{$connection.Dispose()}
