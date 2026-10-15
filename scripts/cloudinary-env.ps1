# Parse only Cloudinary settings as literal data; never execute a .env file.
function Get-LocalCloudinarySettings {
    param([Parameter(Mandatory = $true)][string]$Path)

    $settings = @{}
    if (!(Test-Path -LiteralPath $Path -PathType Leaf)) { return $settings }
    $allowedNames = @('CLOUDINARY_CLOUD_NAME', 'CLOUDINARY_API_KEY', 'CLOUDINARY_API_SECRET')
    $lineNumber = 0
    foreach ($line in [IO.File]::ReadAllLines($Path)) {
        $lineNumber++
        $trimmed = $line.Trim()
        if (!$trimmed -or $trimmed.StartsWith('#')) { continue }
        $separator = $trimmed.IndexOf('=')
        if ($separator -lt 0) {
            if ($trimmed -in $allowedNames) {
                throw "Missing '=' for Cloudinary setting on .env line $lineNumber."
            }
            continue
        }
        $name = $trimmed.Substring(0, $separator).Trim()
        if ($name -notin $allowedNames) { continue }
        if ($settings.ContainsKey($name)) {
            throw "Duplicate Cloudinary setting on .env line $lineNumber."
        }
        $value = $trimmed.Substring($separator + 1).Trim()
        if ($value.StartsWith('"') -or $value.StartsWith("'")) {
            $quote = $value.Substring(0, 1)
            if ($value.Length -lt 2 -or !$value.EndsWith($quote)) {
                throw "Unclosed quoted Cloudinary setting on .env line $lineNumber."
            }
            $value = $value.Substring(1, $value.Length - 2)
        }
        $settings[$name] = $value
    }
    return $settings
}

function Import-LocalCloudinarySettings {
    param([Parameter(Mandatory = $true)][string]$Path)

    $settings = Get-LocalCloudinarySettings -Path $Path
    foreach ($name in $settings.Keys) {
        # An explicit terminal/IDE environment always wins over the local file.
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, 'Process'))) {
            [Environment]::SetEnvironmentVariable($name, $settings[$name], 'Process')
        }
    }
}
