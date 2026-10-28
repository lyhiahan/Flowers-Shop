param(
    [ValidateSet('Check', 'Run')][string]$Mode = 'Check',
    [ValidatePattern('^[A-Za-z0-9.-]+$')][string]$DatabaseHost = 'pg-2679153a-rental-retail-shop.c.aivencloud.com',
    [ValidateRange(1, 65535)][int]$DatabasePort = 16325,
    [ValidatePattern('^[A-Za-z_][A-Za-z0-9_]*$')][string]$DatabaseName = 'defaultdb',
    [string]$DatabaseUser = 'avnadmin',
    [string]$CaCertificate = (Join-Path (Split-Path -Parent $PSScriptRoot) 'ca.pem'),
    [ValidateRange(1024, 65535)][int]$Port = 8080,
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$SaveLocalConfiguration,
    [switch]$PasswordDialog
)
$ErrorActionPreference = 'Stop'
if ($SaveLocalConfiguration -and $Mode -ne 'Check') {
    throw 'Use -Mode Check -SaveLocalConfiguration for the one-time setup.'
}
$root = Split-Path -Parent $PSScriptRoot
$ca = (Resolve-Path -LiteralPath $CaCertificate -ErrorAction Stop).Path
if ([string]::IsNullOrWhiteSpace($JavaHome) -or !(Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) {
    throw 'Set JAVA_HOME to a JDK 21 or newer, or pass -JavaHome.'
}
$java = Join-Path $JavaHome 'bin/java.exe'
$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
$mavenExecutable = if ($maven) { $maven.Source } else { Join-Path $root 'mvnw.cmd' }
if ($Mode -eq 'Run' -and (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)) {
    throw "Port $Port is already in use. Stop the existing app yourself or select -Port 8081."
}

# Never inherit SQL Server overrides from a previous terminal session.
$saved = @{}
$settings = @{}
Get-ChildItem Env: | Where-Object { $_.Name -match '^SPRING_(DATASOURCE|FLYWAY|JPA|CONFIG|PROFILES)_' } | ForEach-Object {
    $settings[$_.Name] = $null
}
$settings['AIVEN_HOST'] = $DatabaseHost
$settings['AIVEN_PORT'] = [string]$DatabasePort
$settings['AIVEN_DATABASE'] = $DatabaseName
$settings['AIVEN_USER'] = $DatabaseUser
$settings['AIVEN_CA_CERT'] = $ca.Replace('\', '/')
$settings['SPRING_PROFILES_ACTIVE'] = 'aiven'
$settings['SERVER_ADDRESS'] = '127.0.0.1'
$settings['SERVER_PORT'] = [string]$Port
$settings['APP_DEMO_DATA_ENABLED'] = 'false'
$settings['AIVEN_PASSWORD'] = $env:AIVEN_PASSWORD
$settings['JWT_SECRET'] = $env:JWT_SECRET
$settings['JAVA_HOME'] = $JavaHome
$secret = $null
$pointer = [IntPtr]::Zero
$result = 1
try {
    if ([string]::IsNullOrWhiteSpace($settings['AIVEN_PASSWORD'])) {
        if ($PasswordDialog) {
            Add-Type -AssemblyName System.Windows.Forms
            $dialog = New-Object Windows.Forms.Form
            $dialog.Text = 'Aiven - private password setup'
            $dialog.ClientSize = New-Object Drawing.Size(460, 150)
            $dialog.StartPosition = 'CenterScreen'
            $dialog.FormBorderStyle = 'FixedDialog'
            $dialog.MaximizeBox = $false
            $dialog.MinimizeBox = $false
            $dialog.TopMost = $true
            $label = New-Object Windows.Forms.Label
            $label.Text = 'Paste the NEW Aiven database password. It will stay on this PC.'
            $label.SetBounds(15, 15, 430, 35)
            $passwordBox = New-Object Windows.Forms.TextBox
            $passwordBox.UseSystemPasswordChar = $true
            $passwordBox.SetBounds(15, 55, 430, 25)
            $ok = New-Object Windows.Forms.Button
            $ok.Text = 'Continue'
            $ok.SetBounds(250, 105, 95, 30)
            $ok.DialogResult = [Windows.Forms.DialogResult]::OK
            $cancel = New-Object Windows.Forms.Button
            $cancel.Text = 'Cancel'
            $cancel.SetBounds(350, 105, 95, 30)
            $cancel.DialogResult = [Windows.Forms.DialogResult]::Cancel
            $dialog.Controls.AddRange(@($label, $passwordBox, $ok, $cancel))
            $dialog.AcceptButton = $ok
            $dialog.CancelButton = $cancel
            try {
                if ($dialog.ShowDialog() -ne [Windows.Forms.DialogResult]::OK) { throw 'Setup cancelled. No credentials saved.' }
                $settings['AIVEN_PASSWORD'] = $passwordBox.Text
                $passwordBox.Clear()
            } finally { $dialog.Dispose() }
        } else {
            Write-Host 'Paste the copied Aiven password with Shift+Insert or right-click, then press Enter. Input stays hidden.'
            $prompt = if ($SaveLocalConfiguration) { 'Aiven password (hidden; saved locally only after successful verification)' } else { 'Aiven password (hidden; not saved)' }
            $secret = Read-Host $prompt -AsSecureString
            $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
            $settings['AIVEN_PASSWORD'] = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
        }
    }
    if ([string]::IsNullOrWhiteSpace($settings['AIVEN_PASSWORD'])) { throw 'Aiven password is required.' }
    if ($settings['AIVEN_PASSWORD'].Length -eq 1 -or $settings['AIVEN_PASSWORD'] -match '[\x00-\x1F\x7F]') {
        Write-Warning 'The password input is incomplete or contains a control key (possibly Ctrl+V). No connection attempted. Run again and paste using Shift+Insert or right-click.'
        exit 1
    }
    if ([string]::IsNullOrWhiteSpace($settings['JWT_SECRET'])) {
        $bytes = New-Object byte[] 32
        $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
        $settings['JWT_SECRET'] = [Convert]::ToBase64String($bytes)
        if ($Mode -eq 'Run') { Write-Host 'Using a temporary JWT key; API tokens expire after restart.' }
    }
    foreach ($key in $settings.Keys) {
        $saved[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
        [Environment]::SetEnvironmentVariable($key, $settings[$key], 'Process')
    }
    Push-Location -LiteralPath $root
    try {
        Write-Host "Checking TLS and login to $DatabaseHost`:$DatabasePort/$DatabaseName (read-only)..."
        & $mavenExecutable dependency:build-classpath '-Dmdep.includeScope=runtime' '-Dmdep.outputFile=target/aiven-runtime-classpath.txt' -q
        if ($LASTEXITCODE -ne 0) { throw 'Could not prepare JDBC driver.' }
        $classpath = (Get-Content -LiteralPath 'target/aiven-runtime-classpath.txt' -Raw).Trim()
        & $java --class-path $classpath (Join-Path $PSScriptRoot 'java/AivenConnectionCheck.java')
        if ($LASTEXITCODE -ne 0) {
            Write-Warning 'Aiven connection check failed. No database changes were made. See the message above, then retry.'
            exit 1
        }
        if ($SaveLocalConfiguration) {
            & $mavenExecutable -Pisolated-tests '-DskipTests' compile -q
            if ($LASTEXITCODE -ne 0) { throw 'Could not prepare local setup. No credentials saved.' }
            & $java --class-path (Join-Path $root 'target/isolated-tests/classes') com.finalproject.tools.AivenLocalSetup
            if ($LASTEXITCODE -ne 0) { throw 'Local setup failed. No database data was changed.' }
        }
        if ($Mode -eq 'Run') {
            Write-Host 'Starting with PostgreSQL migrations. This does NOT copy SQL Server data.'
            & $mavenExecutable spring-boot:run '-Dspring-boot.run.profiles=aiven'
            $result = $LASTEXITCODE
        } else { $result = 0 }
    } finally { Pop-Location }
} finally {
    foreach ($key in $saved.Keys) { [Environment]::SetEnvironmentVariable($key, $saved[$key], 'Process') }
    $settings['AIVEN_PASSWORD'] = $null
    if ($pointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
    if ($secret) { $secret.Dispose() }
}
exit $result
