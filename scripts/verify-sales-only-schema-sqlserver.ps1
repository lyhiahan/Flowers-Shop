param([string]$ServerInstance='.\SQLEXPRESS01')
# Only creates and drops GUID-named test databases owned by this invocation.
$ErrorActionPreference='Stop'
$instance=$ServerInstance
function Open-Db([string]$database){
    $c=[Data.SqlClient.SqlConnection]::new("Data Source=$instance;Initial Catalog=$database;Integrated Security=True;Encrypt=True;TrustServerCertificate=True;Pooling=False;Connect Timeout=10")
    $c.Open();return $c
}
function Execute($connection,[string]$sql,$transaction=$null){
    $q=$connection.CreateCommand();$q.CommandTimeout=60;$q.CommandText=$sql;$q.Transaction=$transaction
    try{return $q.ExecuteNonQuery()}finally{$q.Dispose()}
}
function Scalar($connection,[string]$sql){
    $q=$connection.CreateCommand();$q.CommandTimeout=15;$q.CommandText=$sql
    try{return $q.ExecuteScalar()}finally{$q.Dispose()}
}
$master=Open-Db 'master';$owned=@();$connection=$null
$baseline=Get-Content src/main/resources/db/migration/sqlserver/V1__initial_schema.sql -Raw -Encoding UTF8
$upgrade=Get-Content src/main/resources/db/migration/sqlserver/V18__sales_only_schema.sql -Raw -Encoding UTF8
try {
    foreach($mode in @('fresh','guard')){
        $name='SalesOnlyAudit_'+[guid]::NewGuid().ToString('N')
        [void](Execute $master ('CREATE DATABASE ['+$name+']'));$owned+=$name
        $connection=Open-Db $name
        [void](Execute $connection $baseline)
        if($mode -eq 'fresh'){
            [void](Execute $connection "ALTER TABLE orders ADD CONSTRAINT test_payment_status CHECK(payment_status IN ('UNPAID','PAID','REFUNDED')); ALTER TABLE products ADD CONSTRAINT test_removed_column_check CHECK(deposit_fee >= 0 AND price_rent_per_day >= 0); INSERT INTO categories(name) VALUES(N'Hoa'); INSERT INTO products(name,category_id,import_price,price_buy,price_rent_per_day,deposit_fee,is_active,is_selling,sold) VALUES(N'Hoa',1,100000,350000,7000,50000,1,1,8);")
            $tx=$connection.BeginTransaction()
            try{[void](Execute $connection $upgrade $tx);$tx.Commit()}catch{try{$tx.Rollback()}catch{};throw}
            if((Scalar $connection 'SELECT price_buy FROM products WHERE id=1') -ne 350000){throw 'Price changed'}
            if((Scalar $connection 'SELECT sold FROM products WHERE id=1') -ne 8){throw 'Sold changed'}
            if((Scalar $connection "SELECT COUNT(*) FROM sys.check_constraints WHERE name='test_payment_status'") -ne 1){throw 'Unrelated financial constraint removed'}
            if([decimal](Scalar $connection "SELECT JSON_VALUE(payload,'$.price_rent_per_day') FROM schema_change_archive WHERE table_name='products' AND row_id=1") -ne 7000){throw 'Archive invalid'}
            if((Scalar $connection "SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID('dbo.products') AND name IN ('price_rent_per_day','deposit_fee')") -ne 0){throw 'Old columns remain'}
            $rejected=$false
            try{[void](Execute $connection "INSERT INTO promotions(name,discount_percentage,apply_type) VALUES(N'Invalid',5,'RENT')")}catch{$rejected=$true}
            if(!$rejected){throw 'Unsupported type accepted'}
            Write-Output 'PASS SQL Server: migration removes fields, archives values, preserves price/sold and rejects unsupported types.'
        }else{
            [void](Execute $connection "INSERT INTO promotions(name,discount_percentage,apply_type) VALUES(N'Historical',5,'RENT')")
            $tx=$connection.BeginTransaction();$blocked=$false
            try{[void](Execute $connection $upgrade $tx);$tx.Commit()}catch{$blocked=$true;try{$tx.Rollback()}catch{}}
            if(!$blocked){throw 'Historical guard did not stop migration'}
            if((Scalar $connection "SELECT apply_type FROM promotions") -ne 'RENT'){throw 'History changed'}
            if((Scalar $connection "SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID('dbo.products') AND name='deposit_fee'") -ne 1){throw 'Guard did not preserve schema'}
            Write-Output 'PASS SQL Server: unresolved historical data stops migration and preserves original schema/data.'
        }
        $connection.Dispose();$connection=$null
    }
}finally{
    if($connection){$connection.Dispose()}
    foreach($name in $owned){
        if($name -notmatch '^SalesOnlyAudit_[0-9a-f]{32}$'){throw 'Unexpected test database name; cleanup refused'}
        [void](Execute $master ('DROP DATABASE ['+$name+']'))
    }
    $master.Dispose()
}
