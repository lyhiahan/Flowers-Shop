[CmdletBinding()]
param(
    [ValidateSet('Import', 'Export')][string]$Mode = 'Import',
    [string]$UnlockKey
)
$ErrorActionPreference = 'Stop'

if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) {
    throw 'This local credential helper uses Windows DPAPI and PowerShell SecureString. On other systems configure BREVO_API_KEY directly in environment variables.'
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsDir = Join-Path $projectRoot '.tools'
$mailLocalPath = Join-Path $toolsDir 'mail-local.json'
$shareEncPath = Join-Path $toolsDir 'brevo-share.enc.json'
$shareKeyPath = Join-Path $toolsDir 'brevo-share-unlock.key'

function Parse-AesKey([string]$KeyString) {
    if ([string]::IsNullOrWhiteSpace($KeyString)) { return $null }
    $clean = $KeyString.Trim()

    # Try Base64
    try {
        $bytes = [Convert]::FromBase64String($clean)
        if ($bytes.Length -in @(16, 24, 32)) { return $bytes }
    } catch {}

    # Try Hex (32, 48, 64 chars)
    if ($clean -match '^[0-9A-Fa-f]{32}$|^[0-9A-Fa-f]{48}$|^[0-9A-Fa-f]{64}$') {
        $len = $clean.Length / 2
        $bytes = New-Object byte[] $len
        for ($i = 0; $i -lt $len; $i++) {
            $bytes[$i] = [Convert]::ToByte($clean.Substring($i * 2, 2), 16)
        }
        return $bytes
    }

    # Try comma/space separated numbers
    if ($clean -match '^\d+([,\s]+\d+)+$') {
        try {
            $nums = $clean -split '[,\s]+' | Where-Object { $_ -ne '' } | ForEach-Object { [byte]$_ }
            if ($nums.Count -in @(16, 24, 32)) { return [byte[]]$nums }
        } catch {}
    }

    # Try raw UTF-8 string of 16/24/32 chars
    $utfBytes = [Text.Encoding]::UTF8.GetBytes($clean)
    if ($utfBytes.Length -in @(16, 24, 32)) { return $utfBytes }

    return $null
}

if ($Mode -eq 'Import') {
    if (!(Test-Path -LiteralPath $shareEncPath)) {
        # Check fallback in project root or Downloads
        $altPath = Join-Path $projectRoot 'brevo-share.enc.json'
        if (Test-Path -LiteralPath $altPath) {
            Copy-Item -LiteralPath $altPath -Destination $shareEncPath -Force
        } else {
            throw "File '$shareEncPath' not found. Please place 'brevo-share.enc.json' in the .tools directory."
        }
    }

    $shareContent = Get-Content -LiteralPath $shareEncPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($shareContent.Format -ne 'brevo-team-share-v1') {
        throw "Unsupported share format: '$($shareContent.Format)'. Expected 'brevo-team-share-v1'."
    }
    if ($shareContent.Provider -ne 'brevo') {
        throw "Unsupported mail provider: '$($shareContent.Provider)'."
    }

    $rawKey = $UnlockKey
    if ([string]::IsNullOrWhiteSpace($rawKey) -and (Test-Path -LiteralPath $shareKeyPath)) {
        $rawKey = Get-Content -LiteralPath $shareKeyPath -Raw -Encoding UTF8
    }

    if ([string]::IsNullOrWhiteSpace($rawKey)) {
        $rawKey = Read-Host 'Enter the Brevo unlock key (brevo-share-unlock.key content)'
    }

    $aesKeyBytes = Parse-AesKey $rawKey
    if (!$aesKeyBytes) {
        throw "Invalid unlock key format. Expected 16, 24, or 32 bytes (in Base64, Hex, or comma-separated numbers)."
    }

    # Decrypt with AES Key
    $secureString = $null
    try {
        $secureString = ConvertTo-SecureString -String $shareContent.ProtectedApiKey -Key $aesKeyBytes
    } catch {
        throw "Decryption failed. Please verify that the unlock key matches this encrypted file. Error: $_"
    }

    # Re-encrypt with local Windows DPAPI
    $localProtectedApiKey = ConvertFrom-SecureString -SecureString $secureString
    $secureString.Dispose()

    # Preserve or check existing mail-local.json
    if (Test-Path -LiteralPath $mailLocalPath) {
        $confirm = Read-Host "A local mail configuration already exists in '$mailLocalPath'. Type REPLACE to overwrite it"
        if ($confirm -cne 'REPLACE') {
            throw 'Operation cancelled: Existing mail configuration preserved.'
        }
    }

    $localConfig = [ordered]@{
        Provider = $shareContent.Provider
        FromEmail = $shareContent.FromEmail
        FromName = if ($shareContent.FromName) { $shareContent.FromName } else { 'Hoa Tuoi Sai Gon' }
        ProtectedApiKey = $localProtectedApiKey
    }

    if (!(Test-Path -LiteralPath $toolsDir)) { New-Item -ItemType Directory -Path $toolsDir | Out-Null }
    $localConfig | ConvertTo-Json | Set-Content -LiteralPath $mailLocalPath -Encoding UTF8

    Write-Host "Success: Brevo mail configuration successfully imported into '$mailLocalPath'." -ForegroundColor Green
    Write-Host "Encrypted specifically for Windows user: $env:USERNAME" -ForegroundColor Cyan
    Write-Host "Sender email: $($shareContent.FromEmail) ($($localConfig.FromName))"
    Write-Host "You can now run .\scripts\sqlserver.ps1 -Action run or start-web.ps1." -ForegroundColor Green
}
elseif ($Mode -eq 'Export') {
    if (!(Test-Path -LiteralPath $mailLocalPath)) {
        throw "No local mail configuration found at '$mailLocalPath'. Run .\scripts\configure-mail.ps1 first."
    }

    $localConfig = Get-Content -LiteralPath $mailLocalPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $secureKey = ConvertTo-SecureString $localConfig.ProtectedApiKey

    # Generate random 32-byte (256-bit) AES key
    $aesKeyBytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($aesKeyBytes) } finally { $rng.Dispose() }

    # Encrypt using the key
    $shareProtectedApiKey = ConvertFrom-SecureString -SecureString $secureKey -Key $aesKeyBytes
    $secureKey.Dispose()

    $sharePayload = [ordered]@{
        Format = 'brevo-team-share-v1'
        Provider = $localConfig.Provider
        FromEmail = $localConfig.FromEmail
        FromName = $localConfig.FromName
        ProtectedApiKey = $shareProtectedApiKey
    }

    $base64Key = [Convert]::ToBase64String($aesKeyBytes)
    $sharePayload | ConvertTo-Json | Set-Content -LiteralPath $shareEncPath -Encoding UTF8
    $base64Key | Set-Content -LiteralPath $shareKeyPath -Encoding UTF8

    Write-Host "Success: Generated encrypted export files in '$toolsDir':" -ForegroundColor Green
    Write-Host "  1. brevo-share.enc.json (Encrypted configuration)"
    Write-Host "  2. brevo-share-unlock.key (Secret key to decrypt)"
    Write-Host "`nIMPORTANT: Share these two files via TWO SEPARATE channels (e.g. Email + Private Message)." -ForegroundColor Yellow
}

