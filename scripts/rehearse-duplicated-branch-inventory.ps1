[CmdletBinding()]
param()

# Disposable V10 copy only. This script cannot connect to RentalShop.
$ErrorActionPreference = 'Stop'
$database = 'CodexCatalogCopy_20261019_0740'
$connection = [System.Data.SqlClient.SqlConnection]::new(
    "Data Source=127.0.0.1,53735;Initial Catalog=$database;Integrated Security=True;Encrypt=True;TrustServerCertificate=True;Connect Timeout=15")
try {
    $connection.Open()
    $command = $connection.CreateCommand()
    try {
        $command.CommandTimeout = 120
        $command.CommandText = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'sql/quarantine-duplicated-branch-inventory.sql') -Raw -Encoding UTF8
        $reader = $command.ExecuteReader()
        try {
            if (!$reader.Read()) { throw 'Cleanup did not return a verification row.' }
            $actualDatabase = $reader.GetString(0)
            $rows = $reader.GetInt32(1)
            $quantity = $reader.GetInt64(2)
            $remaining = $reader.GetInt64(3)
        } finally { $reader.Close() }
        if ($actualDatabase -ne $database -or $rows -ne 81 -or $quantity -ne 2003 -or $remaining -ne 1336) {
            throw 'Copy rehearsal returned unexpected totals.'
        }
        Write-Output "Copy rehearsal passed: $rows duplicate rows quarantined, $quantity units archived, $remaining raw units remain. RentalShop not changed."
    } finally { $command.Dispose() }
} finally { $connection.Dispose() }
