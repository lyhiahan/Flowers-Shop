[CmdletBinding()]
param(
    [ValidateSet('Aiven', 'Local')][string]$Database = 'Aiven',
    [ValidateRange(1024, 65535)][int]$Port = 8080,
    [string]$JavaHome,
    [string]$CaCertificate,
    [switch]$Init,
    [switch]$Check,
    [switch]$Migrate,
    [switch]$Tunnel,
    [string]$Cloudflared
)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$profile = if ($Database -eq 'Aiven') { 'aiven' } else { 'local-postgres' }
$configName = if ($Database -eq 'Aiven') { '.env.aiven' } else { '.env.local-postgres' }
$configPath = Join-Path $projectRoot $configName
if ($Init) {
    if (Test-Path -LiteralPath $configPath) { throw "$configName already exists; nothing was overwritten." }
    $template = Get-Content -LiteralPath (Join-Path $projectRoot ($configName + '.example')) -Raw -Encoding UTF8
    $keyBytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($keyBytes) } finally { $rng.Dispose() }
    $template = $template.Replace('JWT_SECRET=replace_with_base64_key', 'JWT_SECRET=' + [Convert]::ToBase64String($keyBytes))
    [Array]::Clear($keyBytes, 0, $keyBytes.Length)
    $stream = [IO.File]::Open($configPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
    try { $bytes = [Text.UTF8Encoding]::new($false).GetBytes($template); $stream.Write($bytes, 0, $bytes.Length) }
    finally { $stream.Dispose() }
    Write-Host "Created private $configName with a random JWT key. Fill database settings, then run .\start-web.ps1 -Database $Database -Check."
    return
}
# Read Java .properties values, including escapes; never print credential values.
$fileSettings = @{}
if (Test-Path -LiteralPath $configPath) {
    foreach ($line in Get-Content -LiteralPath $configPath -Encoding UTF8) {
        if ($line -match '^\s*([A-Z][A-Z0-9_]*)\s*=(.*)$') {
            $settingName = $Matches[1]; $encoded = $Matches[2].TrimStart()
            $fileSettings[$settingName] = [regex]::Replace($encoded, '\\(u[0-9A-Fa-f]{4}|.)', {
                param($match)
                $escaped = $match.Groups[1].Value
                if ($escaped -match '^u[0-9A-Fa-f]{4}$') { return [string][char][Convert]::ToInt32($escaped.Substring(1), 16) }
                switch -CaseSensitive ($escaped) {
                    't' { return "`t" }; 'r' { return "`r" }; 'n' { return "`n" }; 'f' { return [string][char]12 }
                    default { return $escaped }
                }
            })
        }
    }
}
function Get-Setting([string]$Name, [string]$Default = '') {
    $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if (![string]::IsNullOrWhiteSpace($value)) { return $value }
    if ($fileSettings.ContainsKey($Name)) { return $fileSettings[$Name] }
    return $Default
}
function Require-Setting([string]$Name) {
    $value = Get-Setting $Name
    if ([string]::IsNullOrWhiteSpace($value) -or $value -match '^(replace_|your_|pg-your-)') {
        throw "Configure $Name in $configName or the process environment; values are not printed."
    }
}
# Select an actual JDK 21; .tools is optional, JAVA_HOME or installed JDKs work.
$jdkCandidates = [Collections.Generic.List[string]]::new()
if ($JavaHome) { $jdkCandidates.Add($JavaHome) }
if ($env:JAVA_HOME) { $jdkCandidates.Add($env:JAVA_HOME) }
$javac = Get-Command javac.exe -ErrorAction SilentlyContinue
if ($javac) { $jdkCandidates.Add((Split-Path -Parent (Split-Path -Parent $javac.Source))) }
foreach ($parent in @((Join-Path $projectRoot '.tools'), 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Java', 'C:\Program Files\Microsoft')) {
    if (Test-Path -LiteralPath $parent) {
        Get-ChildItem -LiteralPath $parent -Directory -Filter 'jdk-21*' | ForEach-Object { $jdkCandidates.Add($_.FullName) }
    }
}
$selectedJdk = $null
foreach ($candidate in $jdkCandidates) {
    $java = Join-Path $candidate 'bin/java.exe'
    if (!(Test-Path -LiteralPath (Join-Path $candidate 'bin/javac.exe')) -or !(Test-Path -LiteralPath $java)) { continue }
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $java; $info.Arguments = '-version'; $info.UseShellExecute = $false
    $info.CreateNoWindow = $true; $info.RedirectStandardOutput = $true; $info.RedirectStandardError = $true
    $probe = $null
    try {
        $probe = [Diagnostics.Process]::Start($info)
        $stdout = $probe.StandardOutput.ReadToEndAsync(); $stderr = $probe.StandardError.ReadToEndAsync()
        if (!$probe.WaitForExit(10000)) { $probe.Kill(); $probe.WaitForExit(); continue }
        $version = $stdout.GetAwaiter().GetResult() + $stderr.GetAwaiter().GetResult()
        if ($probe.ExitCode -eq 0 -and $version -match 'version "21\.') { $selectedJdk = $candidate; break }
    } catch { continue } finally { if ($probe) { $probe.Dispose() } }
}
if (!$selectedJdk) { throw 'Install JDK 21 and set JAVA_HOME, or pass -JavaHome C:/path/to/jdk-21.' }
if ($Database -eq 'Aiven') {
    foreach ($name in @('AIVEN_HOST', 'AIVEN_PORT', 'AIVEN_DATABASE', 'AIVEN_USER', 'AIVEN_PASSWORD', 'JWT_SECRET')) { Require-Setting $name }
    $databasePort = 0
    if (![int]::TryParse((Get-Setting 'AIVEN_PORT'), [ref]$databasePort) -or $databasePort -lt 1 -or $databasePort -gt 65535) { throw 'AIVEN_PORT must be a port number between 1 and 65535.' }
    if (!$CaCertificate) { $CaCertificate = Get-Setting 'AIVEN_CA_CERT' './ca.pem' }
    if (![IO.Path]::IsPathRooted($CaCertificate)) { $CaCertificate = Join-Path $projectRoot $CaCertificate }
    if (!(Test-Path -LiteralPath $CaCertificate -PathType Leaf)) { throw 'Aiven CA certificate missing. Set AIVEN_CA_CERT or pass -CaCertificate.' }
    $CaCertificate = (Resolve-Path -LiteralPath $CaCertificate).Path.Replace('\', '/')
} else { Require-Setting 'LOCAL_DB_PASSWORD'; Require-Setting 'JWT_SECRET' }
try { $jwtBytes = [Convert]::FromBase64String((Get-Setting 'JWT_SECRET')) } catch { throw 'JWT_SECRET must be Base64 of at least 32 random bytes.' }
if ($jwtBytes.Length -lt 32) { throw 'JWT_SECRET must be Base64 of at least 32 random bytes.' }
[Array]::Clear($jwtBytes, 0, $jwtBytes.Length)
if ($Tunnel) {
    if (!$Cloudflared) { $command = Get-Command cloudflared.exe -ErrorAction SilentlyContinue; if ($command) { $Cloudflared = $command.Source } }
    if (!$Cloudflared -or !(Test-Path -LiteralPath $Cloudflared -PathType Leaf)) { throw 'Install cloudflared on PATH or pass -Cloudflared C:/path/cloudflared.exe.' }
    if (!(Get-Command node.exe -ErrorAction SilentlyContinue)) { throw 'Node.js is required for the optional phone payment tunnel.' }
    $Cloudflared = (Resolve-Path -LiteralPath $Cloudflared).Path.Replace('\', '/')
}
Write-Host "Configuration OK: JDK 21; profile=$profile; port=$Port; migrations=$($Migrate.IsPresent); tunnel=$($Tunnel.IsPresent)."
if ($Check) { Write-Host 'Configuration check only: no database connection, migration, import or application startup.'; return }
if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) { throw "Port $Port is in use. Stop the existing application or choose -Port 8081." }
$saved = @{}; $settings = @{}
Get-ChildItem Env: | Where-Object { $_.Name -match '^SPRING_(DATASOURCE|FLYWAY|JPA|CONFIG|PROFILES)_' } | ForEach-Object { $settings[$_.Name] = $null }
$settings['JAVA_HOME'] = $selectedJdk
$settings['PATH'] = (Join-Path $selectedJdk 'bin') + ';' + $env:PATH
$settings['SPRING_PROFILES_ACTIVE'] = $profile
$settings['SERVER_ADDRESS'] = '127.0.0.1'
$settings['SERVER_PORT'] = [string]$Port
$settings['APP_DEMO_DATA_ENABLED'] = 'false'
$settings['SPRING_FLYWAY_ENABLED'] = $Migrate.IsPresent.ToString().ToLowerInvariant()
$settings['SPRING_FLYWAY_BASELINE_ON_MIGRATE'] = 'false'
$settings['SPRING_JPA_HIBERNATE_DDL_AUTO'] = 'validate'
$settings['DEMO_PAYMENT_AUTO_TUNNEL_ENABLED'] = $Tunnel.IsPresent.ToString().ToLowerInvariant()
if ($Database -eq 'Aiven') { $settings['AIVEN_CA_CERT'] = $CaCertificate }
if ($Tunnel) { $settings['APP_DEMO_PAYMENT_AUTO_TUNNEL_CLOUDFLARED'] = $Cloudflared }
$mailCredentialFile = Join-Path $projectRoot '.tools/mail-local.json'
if (Test-Path -LiteralPath $mailCredentialFile) {
    try {
        $mailCredential = Get-Content -LiteralPath $mailCredentialFile -Raw | ConvertFrom-Json
        if ($mailCredential.Provider -eq 'brevo') {
            $secureMailKey = ConvertTo-SecureString $mailCredential.ProtectedApiKey
            $mailKeyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureMailKey)
            try { $settings['BREVO_API_KEY'] = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($mailKeyPointer) }
            finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($mailKeyPointer); $secureMailKey.Dispose() }
            $settings['MAIL_PROVIDER'] = 'brevo'
            if ($mailCredential.FromEmail) { $settings['MAIL_FROM_EMAIL'] = $mailCredential.FromEmail }
            if ($mailCredential.FromName) { $settings['MAIL_FROM_NAME'] = $mailCredential.FromName }
        }
    } catch {}
}
$result = 1
try {
    foreach ($name in $settings.Keys) {
        $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
        [Environment]::SetEnvironmentVariable($name, $settings[$name], 'Process')
    }
    Push-Location -LiteralPath $projectRoot
    try {
        Write-Host "Website: http://127.0.0.1:$Port | Database: $Database. No automatic import, reset or seed."
        & (Join-Path $projectRoot 'scripts/maven.ps1') -MavenArguments @('spring-boot:run', "-Dspring-boot.run.profiles=$profile")
        $result = $LASTEXITCODE
    } finally { Pop-Location }
} finally {
    foreach ($name in $saved.Keys) { [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process') }
    $fileSettings.Clear()
}
exit $result
