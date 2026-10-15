[CmdletBinding()]
param(
    [string]$Server = '127.0.0.1,53735',
    [string]$Database = 'RentalShop'
)

# Windows integrated authentication. SELECT-only audit; does not start the website,
# run migrations, move stock, delete rows, or change orders.
$ErrorActionPreference = 'Stop'
$connectionOptions = New-Object System.Data.SqlClient.SqlConnectionStringBuilder
$connectionOptions['Data Source'] = $Server
$connectionOptions['Initial Catalog'] = $Database
$connectionOptions['Integrated Security'] = $true
$connectionOptions['Encrypt'] = $true
$connectionOptions['TrustServerCertificate'] = $true
$connectionOptions['Connect Timeout'] = 15
$connectionOptions['Application Name'] = 'BranchInventoryReadOnlyAudit'
$connection = New-Object System.Data.SqlClient.SqlConnection $connectionOptions.ConnectionString
$command = $null
$adapter = $null
$results = New-Object System.Data.DataSet
try {
    $connection.Open()
    $command = $connection.CreateCommand()
    $command.CommandTimeout = 30
    $command.CommandText = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'sql/audit-branch-inventory.sql') -Raw -Encoding UTF8
    $adapter = New-Object System.Data.SqlClient.SqlDataAdapter $command
    [void]$adapter.Fill($results)
    $names = @('snapshot', 'stores', 'products', 'mismatches', 'orders', 'references')
    if ($results.Tables.Count -ne $names.Count) { throw 'Unexpected audit result shape.' }
    $report = [ordered]@{}
    for ($index = 0; $index -lt $names.Count; $index++) {
        $table = $results.Tables[$index]
        $report[$names[$index]] = @(foreach ($row in $table.Rows) {
            $record = [ordered]@{}
            foreach ($column in $table.Columns) {
                $value = $row[$column.ColumnName]
                $record[$column.ColumnName] = if ($value -is [DBNull]) { $null } else { $value }
            }
            [pscustomobject]$record
        })
    }
    $report | ConvertTo-Json -Depth 8
} finally {
    if ($null -ne $adapter) { $adapter.Dispose() }
    if ($null -ne $command) { $command.Dispose() }
    $results.Dispose()
    $connection.Dispose()
}
