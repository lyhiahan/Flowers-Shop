[CmdletBinding()]
param([ValidatePattern('^[0-9_]+$')][string]$Suffix)

# Migrates only a pre-provisioned disposable copy. Never targets RentalShop.
$ErrorActionPreference = 'Stop'
$database = 'CodexCatalogCopy_' + $Suffix
$server = '127.0.0.1,53735'
$connection = [System.Data.SqlClient.SqlConnection]::new(
    "Data Source=$server;Initial Catalog=master;Integrated Security=True;Encrypt=True;TrustServerCertificate=True;Connect Timeout=15")
try {
    $connection.Open()
    $command = $connection.CreateCommand()
    $command.CommandText = 'SELECT CONVERT(nvarchar(4000), SERVERPROPERTY(''InstanceDefaultBackupPath''))'
    $backupRoot = [string]$command.ExecuteScalar()
    $backupPath = Join-Path $backupRoot "RentalShop_before_branch_catalog_$Suffix.bak"
    $safeBackup = $backupPath.Replace("'", "''")
    $command.CommandText = "DECLARE @present int; EXEC master.dbo.xp_fileexist N'$safeBackup', @present OUTPUT; SELECT @present"
    if ([int]$command.ExecuteScalar() -ne 1) { throw "Verified backup not found: $backupPath" }
    $command.CommandText = "SELECT DB_ID(N'$database')"
    if ($command.ExecuteScalar() -is [DBNull]) { throw "Disposable copy does not exist: $database" }
    foreach ($name in @('RentalShop', $database)) {
        $copy = [System.Data.SqlClient.SqlConnection]::new(
            "Data Source=$server;Initial Catalog=$name;Integrated Security=True;Encrypt=True;TrustServerCertificate=True;Connect Timeout=15")
        try {
            $copy.Open()
            $versionCommand = $copy.CreateCommand()
            $versionCommand.CommandText = 'SELECT TOP (1) version FROM dbo.flyway_schema_history WHERE success=1 ORDER BY installed_rank DESC'
            $version = [string]$versionCommand.ExecuteScalar()
            if ($name -eq 'RentalShop' -and $version -ne '8') { throw "Source version changed ($version); stop and re-audit." }
            if ($name -eq $database -and $version -notin @('8', '10')) { throw "Unexpected copy version: $version" }
        } finally { $copy.Dispose() }
    }
} finally { $connection.Dispose() }

$names = @('SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD')
$previous = @{}
foreach ($name in $names) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    $credential = Get-Content -LiteralPath (Join-Path $PSScriptRoot '../.tools/sqlserver-local.json') -Raw | ConvertFrom-Json
    if ($credential.Username -ne 'rental_user') { throw 'Unexpected SQL login.' }
    $secure = ConvertTo-SecureString $credential.ProtectedPassword
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { $env:SPRING_DATASOURCE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer); $secure.Dispose() }
    $env:SPRING_DATASOURCE_USERNAME = $credential.Username
    $env:SPRING_DATASOURCE_URL = "jdbc:sqlserver://127.0.0.1:53735;databaseName=$database;encrypt=true;trustServerCertificate=true;loginTimeout=15;"
    & (Join-Path $PSScriptRoot 'maven.ps1') -MavenArguments @('compile', 'flyway:migrate', '-B')
    if ($LASTEXITCODE -ne 0) { throw 'Copy migration failed.' }
    & (Join-Path $PSScriptRoot 'maven.ps1') -MavenArguments @('test', '-B', '-Dtest=ManagerCatalogSqlServerLiveTests,ProductCatalogMigrationLiveTests', '-Dcatalog.sqlserver.live=true')
    if ($LASTEXITCODE -ne 0) { throw 'Copy schema tests failed.' }
    Write-Output "Flyway V10 and entity/catalog checks passed on $database; RentalShop remains unchanged."
} finally {
    foreach ($name in $names) {
        if ($null -eq $previous[$name]) { Remove-Item -LiteralPath ("Env:\$name") -ErrorAction SilentlyContinue }
        else { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
    }
}
