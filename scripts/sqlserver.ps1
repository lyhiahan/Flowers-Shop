param(
    [ValidateSet('run', 'info', 'validate', 'baseline', 'migrate')][string]$Action = 'run',
    [ValidatePattern('^[A-Za-z0-9_.\\-]+$')][string]$ServerInstance = '127.0.0.1\SQLEXPRESS',
    [ValidatePattern('^[A-Za-z0-9_]+$')][string]$DatabaseName = 'RentalShop',
    [string]$SqlUsername = 'rental_user'
)
$ErrorActionPreference = 'Stop'
$savedSettings = @{}
foreach ($name in @('SPRING_PROFILES_ACTIVE','SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD',
        'MAIL_PROVIDER','MAIL_FROM_EMAIL','MAIL_FROM_NAME','BREVO_API_KEY',
        'CLOUDINARY_CLOUD_NAME','CLOUDINARY_API_KEY','CLOUDINARY_API_SECRET')) {
    $savedSettings[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
try {
    . (Join-Path $PSScriptRoot 'cloudinary-env.ps1')
    Import-LocalCloudinarySettings -Path (Join-Path (Split-Path -Parent $PSScriptRoot) '.env')
    $mailCredentialFile = Join-Path (Split-Path -Parent $PSScriptRoot) '.tools/mail-local.json'
    if ([string]::IsNullOrWhiteSpace($env:BREVO_API_KEY) -and
            ([string]::IsNullOrWhiteSpace($env:MAIL_PROVIDER) -or $env:MAIL_PROVIDER -ieq 'brevo') -and
            (Test-Path -LiteralPath $mailCredentialFile)) {
        try {
            $mailCredential = Get-Content -LiteralPath $mailCredentialFile -Raw | ConvertFrom-Json
            if ($mailCredential.Provider -ne 'brevo') { throw 'Unsupported local mail provider.' }
            $secureMailKey = ConvertTo-SecureString $mailCredential.ProtectedApiKey
            if ($secureMailKey.Length -lt 20) {
                Write-Warning 'Local mail API key looks incomplete. Run scripts/configure-mail.ps1 and paste the entire API key, not the displayed * character.'
            }
            $mailKeyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureMailKey)
            try { $env:BREVO_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($mailKeyPointer) }
            finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($mailKeyPointer); $secureMailKey.Dispose() }
            if ([string]::IsNullOrWhiteSpace($env:MAIL_PROVIDER)) { $env:MAIL_PROVIDER = 'brevo' }
            if ([string]::IsNullOrWhiteSpace($env:MAIL_FROM_EMAIL)) { $env:MAIL_FROM_EMAIL = $mailCredential.FromEmail }
            if ([string]::IsNullOrWhiteSpace($env:MAIL_FROM_NAME)) { $env:MAIL_FROM_NAME = $mailCredential.FromName }
        } catch { Write-Warning 'Cannot load encrypted local mail configuration. Run scripts/configure-mail.ps1 or set mail environment variables.' }
    }
    $serverParts = $ServerInstance.Split('\')
    $connectionUrl = 'jdbc:sqlserver://' + $serverParts[0] + ';'
    if ($serverParts.Length -gt 1) { $connectionUrl += 'instanceName=' + $serverParts[1] + ';' }
    $connectionUrl += 'databaseName=' + $DatabaseName + ';encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true;loginTimeout=15;'
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    $env:SPRING_DATASOURCE_URL = $connectionUrl
    $env:SPRING_DATASOURCE_USERNAME = $SqlUsername
    if ([string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_PASSWORD)) {
        $credentialFile = Join-Path (Split-Path -Parent $PSScriptRoot) '.tools/sqlserver-local.json'
        $securePassword = $null
        if (Test-Path -LiteralPath $credentialFile) {
            try {
                $localCredential = Get-Content -LiteralPath $credentialFile -Raw | ConvertFrom-Json
                if ($localCredential.Username -eq $SqlUsername) {
                    $securePassword = ConvertTo-SecureString $localCredential.ProtectedPassword
                }
            } catch { Write-Warning 'Cannot decrypt the local SQL credential for this Windows user. Enter the password again.' }
        }
        if (!$securePassword) { $securePassword = Read-Host "SQL password for $SqlUsername (not saved)" -AsSecureString }
        $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        try { $env:SPRING_DATASOURCE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer); $securePassword.Dispose() }
    }
    Write-Host "SQL Server: $ServerInstance | Database: $DatabaseName | Action: $Action"
    if ($Action -eq 'run' -and $DatabaseName -eq 'RentalShop') {
        # Spring Boot runs pending Flyway migrations at startup. Do not let a routine
        # website start silently apply the payment-owned V9/V10 schema changes.
        $auditConnection = [System.Data.SqlClient.SqlConnection]::new(
            "Data Source=$ServerInstance;Initial Catalog=$DatabaseName;Integrated Security=True;Encrypt=True;TrustServerCertificate=True;Connect Timeout=15")
        try {
            $auditConnection.Open()
            $versionCommand = $auditConnection.CreateCommand()
            $versionCommand.CommandText = 'SELECT TOP (1) version FROM dbo.flyway_schema_history WHERE success=1 ORDER BY installed_rank DESC'
            $schemaVersion = [string]$versionCommand.ExecuteScalar()
            $parsedVersion = 0
            if (![int]::TryParse($schemaVersion, [ref]$parsedVersion) -or $parsedVersion -lt 10) {
                throw "RentalShop dang o Flyway V$schemaVersion. Khoi dong website se tu ap dung V9-V10; hay xac nhan voi thanh vien phu trach thanh toan va chay migration co chu dich sau khi sao luu."
            }
        } finally { $auditConnection.Dispose() }
    }
    if ($Action -in @('baseline','migrate')) {
        $confirmation = Read-Host "This updates $DatabaseName. Verify a backup first, then type the exact database name to continue"
        if ($confirmation -cne $DatabaseName) { throw 'Database operation cancelled; no migration performed.' }
    }
    $mavenScript = Join-Path $PSScriptRoot 'maven.ps1'
    if ($Action -eq 'run') { & $mavenScript 'spring-boot:run' '-Dspring-boot.run.profiles=dev' }
    else { & $mavenScript 'compile' ('flyway:' + $Action) }
    $scriptExitCode = $LASTEXITCODE
} finally {
    foreach ($name in $savedSettings.Keys) {
        if ($null -eq $savedSettings[$name]) {
            Remove-Item -LiteralPath ('Env:\' + $name) -ErrorAction SilentlyContinue
        } else {
            [Environment]::SetEnvironmentVariable($name, $savedSettings[$name], 'Process')
        }
    }
}
exit $scriptExitCode
