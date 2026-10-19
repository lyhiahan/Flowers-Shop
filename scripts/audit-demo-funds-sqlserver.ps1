[CmdletBinding()]
param()

# Read-only audit of the local RentalShop V10 payment-hold ledger.
$ErrorActionPreference = 'Stop'
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
    $env:SPRING_DATASOURCE_URL = 'jdbc:sqlserver://127.0.0.1:53735;databaseName=RentalShop;encrypt=true;trustServerCertificate=true;loginTimeout=15;'
    & (Join-Path $PSScriptRoot 'maven.ps1') -MavenArguments @('test', '-B', '-q', '-Dtest=DemoFundsSqlServerAuditTests', '-Ddemo.funds.live=true')
    if ($LASTEXITCODE -ne 0) { throw 'Read-only demo funds audit failed.' }
    Write-Output 'RentalShop V10 demo funds ledger audit passed; no business rows changed.'
} finally {
    foreach ($name in $names) {
        if ($null -eq $previous[$name]) { Remove-Item -LiteralPath ("Env:\$name") -ErrorAction SilentlyContinue }
        else { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
    }
}
