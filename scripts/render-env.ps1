# Export existing local credentials to a private file for Render's Add from .env.
# This script does not change the local application profile or send mail.
param()
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$exportPath = Join-Path $projectDirectory '.env.render'

function Read-LocalProperties([string]$filePath) {
    $values = @{}
    if (!(Test-Path -LiteralPath $filePath)) { return $values }
    foreach ($line in [IO.File]::ReadAllLines($filePath)) {
        if ($line -match '^\s*([A-Z][A-Z0-9_]*)\s*[=:](.*)$') {
            $name = $matches[1]
            $value = $matches[2].Trim()
            if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            # java.util.Properties escapes these characters when saving .env.aiven.
            $values[$name] = [regex]::Replace($value, '\\([\\=:#! ])', '$1')
        }
    }
    return $values
}

$database = Read-LocalProperties (Join-Path $projectDirectory '.env.aiven')
$local = Read-LocalProperties (Join-Path $projectDirectory '.env')
$exportValues = [ordered]@{
    SPRING_PROFILES_ACTIVE = 'aiven'
    SERVER_ADDRESS = '0.0.0.0'
    SERVER_FORWARD_HEADERS_STRATEGY = 'framework'
    SESSION_COOKIE_SECURE = 'true'
    AIVEN_HOST = $database['AIVEN_HOST']
    AIVEN_PORT = $database['AIVEN_PORT']
    AIVEN_DATABASE = $database['AIVEN_DATABASE']
    AIVEN_USER = $database['AIVEN_USER']
    AIVEN_PASSWORD = $database['AIVEN_PASSWORD']
    AIVEN_SSLMODE = 'require'
    JWT_SECRET = $database['JWT_SECRET']
    DEMO_PAYMENT_AUTO_TUNNEL_ENABLED = 'false'
    CLOUDINARY_CLOUD_NAME = $local['CLOUDINARY_CLOUD_NAME']
    CLOUDINARY_API_KEY = $local['CLOUDINARY_API_KEY']
    CLOUDINARY_API_SECRET = $local['CLOUDINARY_API_SECRET']
    MAIL_PROVIDER = 'brevo'
    MAIL_FROM_EMAIL = $local['MAIL_FROM_EMAIL']
    MAIL_FROM_NAME = 'Hoa Tươi Sài Gòn'
    BREVO_API_KEY = $local['BREVO_API_KEY']
}

$mailPath = Join-Path $projectDirectory '.tools/mail-local.json'
if (Test-Path -LiteralPath $mailPath) {
    $mail = [IO.File]::ReadAllText($mailPath) | ConvertFrom-Json
    if ($mail.Provider -eq 'brevo' -and $mail.ProtectedApiKey -and $mail.FromEmail) {
        $secureKey = ConvertTo-SecureString $mail.ProtectedApiKey
        $keyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
        try {
            $exportValues['BREVO_API_KEY'] = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPointer)
            $exportValues['MAIL_FROM_EMAIL'] = $mail.FromEmail
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPointer)
            $secureKey.Dispose()
        }
    }
}

foreach ($name in $exportValues.Keys) {
    if ([string]::IsNullOrWhiteSpace($exportValues[$name])) {
        throw "Missing local configuration: $name. No deployment file was written."
    }
    if ($exportValues[$name] -match '[\r\n]') { throw "Invalid multiline value: $name" }
}

# Restrict access before writing credentials, including when replacing an export.
if (!(Test-Path -LiteralPath $exportPath)) { New-Item -ItemType File -Path $exportPath | Out-Null }
$identity = [Security.Principal.WindowsIdentity]::GetCurrent().User
$fileSecurity = New-Object Security.AccessControl.FileSecurity
$fileSecurity.SetOwner($identity)
$fileSecurity.SetAccessRuleProtection($true, $false)
$fileSecurity.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule($identity, 'FullControl', 'Allow')))
Set-Acl -LiteralPath $exportPath -AclObject $fileSecurity
$lines = foreach ($name in $exportValues.Keys) { "$name=$($exportValues[$name])" }
[IO.File]::WriteAllLines($exportPath, [string[]]$lines, (New-Object Text.UTF8Encoding($false)))
Write-Output 'Created private .env.render from existing local Aiven, Cloudinary and Brevo settings. No email was sent.'
