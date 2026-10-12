param(
    [string]$FromEmail,
    [string]$FromName = 'Hoa Tuoi Sai Gon'
)
$ErrorActionPreference = 'Stop'
if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) {
    throw 'This local credential helper uses Windows DPAPI. On other systems configure BREVO_API_KEY and MAIL_FROM_EMAIL in a secret manager.'
}
if ([string]::IsNullOrWhiteSpace($FromEmail)) {
    $FromEmail = Read-Host 'Sender email already verified in Brevo (not the customer email)'
}
$FromEmail = $FromEmail.Trim()
try {
    $mailbox = [Net.Mail.MailAddress]::new($FromEmail)
    if ($mailbox.Address -ne $FromEmail -or $FromEmail.Contains([char]13) -or $FromEmail.Contains([char]10)) {
        throw 'Sender must be one email address without a display name or line breaks.'
    }
} catch { throw 'Enter one valid sender email address.' }
$projectRoot = Split-Path -Parent $PSScriptRoot
$credentialDirectory = Join-Path $projectRoot '.tools'
$credentialFile = Join-Path $credentialDirectory 'mail-local.json'
if (Test-Path -LiteralPath $credentialFile) {
    $confirmation = Read-Host 'An encrypted mail credential exists. Type REPLACE to replace it'
    if ($confirmation -cne 'REPLACE') { throw 'Existing mail configuration preserved.' }
}
$secureKey = Read-Host 'Brevo API key (hidden; no Gmail App Password needed)' -AsSecureString
try {
    if ($secureKey.Length -eq 0) { throw 'API key cannot be empty.' }
    if ($secureKey.Length -lt 20) {
        throw 'API key looks incomplete. Paste the entire key copied from Brevo; do not type the displayed * character. Existing configuration was not changed.'
    }
    $configuration = [ordered]@{
        Provider = 'brevo'
        FromEmail = $FromEmail
        FromName = $FromName
        ProtectedApiKey = ConvertFrom-SecureString $secureKey
    }
    if (!(Test-Path -LiteralPath $credentialDirectory)) { New-Item -ItemType Directory -Path $credentialDirectory | Out-Null }
    $configuration | ConvertTo-Json | Set-Content -LiteralPath $credentialFile -Encoding UTF8
} finally { $secureKey.Dispose() }
Write-Host 'Mail configuration saved with Windows DPAPI encryption, outside source control.'
Write-Host 'This saves configuration only; Brevo validates the API key and sender when an OTP is requested.'
Write-Host 'Start the website with .\scripts\sqlserver.ps1; customers only enter their own email.'
