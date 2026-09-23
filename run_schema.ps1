$connStr = "Server=localhost\SQLEXPRESS;Database=RentalShop;User Id=sa;Password=giahan;TrustServerCertificate=True;"
$conn = New-Object System.Data.SqlClient.SqlConnection($connStr)
$conn.Open()
Write-Host "Database Connection Status: " $conn.State

$sqlScript = Get-Content -Raw -Path "d:\LAPTRINH\LAPTRINHWEB\BAITAP\finalproject\schema_update.sql"
# Split by GO
$batches = $sqlScript -split "(?im)^\s*GO\s*$"

foreach ($batch in $batches) {
    $trimmed = $batch.Trim()
    if ($trimmed.Length -gt 0 -and $trimmed -notmatch "^USE\s+") {
        try {
            $cmd = $conn.CreateCommand()
            $cmd.CommandText = $trimmed
            $cmd.CommandTimeout = 30
            $cmd.ExecuteNonQuery() | Out-Null
            Write-Host "Executed batch successfully."
        } catch {
            Write-Host "Batch error: " $_.Exception.Message
        }
    }
}

$conn.Close()
Write-Host "Schema update finished."
