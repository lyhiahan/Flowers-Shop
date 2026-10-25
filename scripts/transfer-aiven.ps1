param(
    [ValidateSet('Export','Rehearse','Plan','Import')][string]$Action = 'Plan',
    [string]$BackupFile,
    [switch]$UseLatestBackup,
    [switch]$SourceWritesPaused,
    [string]$JavaHome = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if ($UseLatestBackup) {
    if ($BackupFile -or $Action -eq 'Export') { throw 'Use -UseLatestBackup only instead of -BackupFile for an existing snapshot.' }
    $latest = Get-ChildItem -LiteralPath (Join-Path $root '.tools/aiven-migration/backups') -Filter '*.enc' -File |
        Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
    if (!$latest) { throw 'No encrypted snapshot found. Export one first.' }
    $BackupFile = $latest.FullName
    Write-Host "Selected backup: $($latest.Name)"
}
if (!$JavaHome -or !(Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) { throw 'Set JAVA_HOME to JDK 21 or newer.' }
if ($Action -eq 'Export' -or $Action -eq 'Import') {
    if (!$SourceWritesPaused) { throw 'Confirm the group has paused writes, then pass -SourceWritesPaused.' }
}
if ($Action -ne 'Export' -and !$BackupFile) { throw 'Pass -BackupFile with the encrypted snapshot path.' }
$java = Join-Path $JavaHome 'bin/java.exe'
$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
$mavenExecutable = if ($maven) { $maven.Source } else { Join-Path $root 'mvnw.cmd' }
$saved = @{}
$settings = @{ JAVA_HOME = $JavaHome }
$key = $null
$secure = $null
$pointer = [IntPtr]::Zero
$result = 1
try {
    Add-Type -AssemblyName System.Security
    if ($Action -eq 'Export') {
        $folder = Join-Path $root '.tools/aiven-migration/backups'
        [IO.Directory]::CreateDirectory($folder) | Out-Null
        $BackupFile = Join-Path $folder ('RentalShop-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8) + '.enc')
        $key = New-Object byte[] 32
        $random = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $random.GetBytes($key) } finally { $random.Dispose() }
        $protectedKey = [Security.Cryptography.ProtectedData]::Protect($key, $null, [Security.Cryptography.DataProtectionScope]::CurrentUser)
        $file = [IO.File]::Open($BackupFile + '.key.dpapi', [IO.FileMode]::CreateNew)
        try { $file.Write($protectedKey, 0, $protectedKey.Length) } finally { $file.Dispose() }
    } else {
        $BackupFile = (Resolve-Path -LiteralPath $BackupFile).Path
        $protectedKey = [IO.File]::ReadAllBytes($BackupFile + '.key.dpapi')
        $key = [Security.Cryptography.ProtectedData]::Unprotect($protectedKey, $null, [Security.Cryptography.DataProtectionScope]::CurrentUser)
        if ($Action -eq 'Rehearse') {
            $settings['MIGRATION_SNAPSHOT_FILE'] = $BackupFile
        } else {
        $settings['AIVEN_HOST'] = 'pg-2679153a-rental-retail-shop.c.aivencloud.com'
        $settings['AIVEN_PORT'] = '16325'
        $settings['AIVEN_DATABASE'] = 'defaultdb'
        $settings['AIVEN_USER'] = 'avnadmin'
        $settings['AIVEN_CA_CERT'] = (Resolve-Path -LiteralPath (Join-Path $root '../ca.pem')).Path.Replace('\','/')
        Write-Host 'Paste the Aiven Password with Shift+Insert or right-click. Do not paste the Service URI.'
        $secure = Read-Host 'Aiven password (hidden; not saved)' -AsSecureString
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
        if ([string]::IsNullOrWhiteSpace($password) -or $password.Length -eq 1 -or $password -match '[\x00-\x1F\x7F]') {
            throw 'Incomplete password input. Paste again using Shift+Insert or right-click.'
        }
        $settings['AIVEN_PASSWORD'] = $password
        $password = $null
        }
    }
    $settings['MIGRATION_BACKUP_KEY'] = [Convert]::ToBase64String($key)
    foreach ($name in $settings.Keys) {
        $saved[$name] = [Environment]::GetEnvironmentVariable($name,'Process')
        [Environment]::SetEnvironmentVariable($name,$settings[$name],'Process')
    }
    Push-Location -LiteralPath $root
    try {
        if ($Action -eq 'Rehearse') {
            Write-Host 'Rehearsing the encrypted snapshot ONLY on disposable local PostgreSQL. No Aiven connection.'
            & $mavenExecutable '-Pisolated-tests,postgresql-tests' '-Dtest=PostgreSqlTransferTests' '-Dmigration.rehearsal=true' test
            if ($LASTEXITCODE -ne 0) { throw 'Local rehearsal failed. Do not import to Aiven.' }
            $result = 0
        } else {
        & $mavenExecutable -Pisolated-tests compile dependency:build-classpath '-Dmdep.includeScope=runtime' '-Dmdep.outputFile=target/aiven-migration-classpath.txt' -q
        if ($LASTEXITCODE -ne 0) { throw 'Could not compile the transfer tool.' }
        $classpath = (Join-Path $root 'target/isolated-tests/classes') + [IO.Path]::PathSeparator + (Get-Content target/aiven-migration-classpath.txt -Raw).Trim()
        if ($Action -eq 'Export') {
            & $java --class-path $classpath com.finalproject.tools.DatabaseTransfer export $BackupFile
            if ($LASTEXITCODE -ne 0) { throw 'Source export failed. No source data was changed.' }
            Write-Host "Encrypted backup: $BackupFile"
            Write-Host 'Keep BOTH the .enc file and its .key.dpapi file. Decryption requires this Windows account on this computer.'
        } else {
            & $java --class-path $classpath com.finalproject.tools.DatabaseTransfer plan $BackupFile
            if ($LASTEXITCODE -ne 0) { throw 'Preflight refused the transfer. No target data changed.' }
            if ($Action -eq 'Import') {
                Write-Host 'This inserts old business data into EMPTY Aiven tables. It does not delete source data.'
                Write-Host 'Old sessions/OTP are not activated. Legacy data is kept in a restricted archive schema.'
                $confirmation = Read-Host 'Type IMPORT RentalShop to confirm'
                if ($confirmation -cne 'IMPORT RentalShop') { throw 'Import cancelled before any data writes.' }
                & $java --class-path $classpath com.finalproject.tools.DatabaseTransfer import $BackupFile
                if ($LASTEXITCODE -ne 0) { throw 'Import did not report success. Inspect the result before retrying; never clear tables to force a retry.' }
                Write-Host 'Transfer committed and every imported table reconciled. Do not resume the old database as the active system.'
            }
        }
        $result = 0
        }
    } finally { Pop-Location }
} finally {
    foreach ($name in $saved.Keys) { [Environment]::SetEnvironmentVariable($name,$saved[$name],'Process') }
    $settings['AIVEN_PASSWORD'] = $null
    $settings['MIGRATION_BACKUP_KEY'] = $null
    if ($key) { [Array]::Clear($key,0,$key.Length) }
    if ($pointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
    if ($secure) { $secure.Dispose() }
}
exit $result
