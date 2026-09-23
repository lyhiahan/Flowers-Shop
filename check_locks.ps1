$connStr = "Server=localhost\SQLEXPRESS;Database=master;User Id=sa;Password=giahan;TrustServerCertificate=True;"
$conn = New-Object System.Data.SqlClient.SqlConnection($connStr)
$conn.Open()
$cmd = $conn.CreateCommand()
$cmd.CommandText = "SELECT spid, dbid, login_time, program_name, hostname FROM sys.sysprocesses WHERE dbid = DB_ID('RentalShop')"
$reader = $cmd.ExecuteReader()
while ($reader.Read()) {
    Write-Host "SPID:" $reader["spid"] "Prog:" $reader["program_name"] "Host:" $reader["hostname"]
}
$reader.Close()
$conn.Close()
