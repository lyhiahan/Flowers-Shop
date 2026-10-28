param(
    [ValidateSet('Plan','Rehearse','Apply')][string]$Mode='Plan',
    [string]$ServerInstance='.\SQLEXPRESS',
    [string]$DatabaseName='RentalShop'
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
# Unicode SQL literals do not repair a source file that was already decoded as ANSI.
$legacy=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'sql/seed-realistic-reviews.sql') -Raw -Encoding UTF8
$block=[regex]::Match($legacy,'(?s)INSERT INTO #SampleComments.*?VALUES(.*?);').Groups[1].Value
$known=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$ansi=[Text.Encoding]::GetEncoding(1252)
foreach($match in [regex]::Matches($block,"N'((?:[^']|'')*)'")) {
    $text=$match.Groups[1].Value.Replace("''", "'")
    [void]$known.Add($text)
    for($round=0;$round -lt 2;$round++){$text=$ansi.GetString([Text.Encoding]::UTF8.GetBytes($text));[void]$known.Add($text)}
}
if($known.Count -lt 12){throw 'Legacy fixture signatures missing; no reviews changed.'}
$comments=@(
    @{Stars=1;Text='Phần phối màu chưa hài hòa, hoa phụ che mất điểm nhấn chính. Mẫu này cần chỉnh lại bố cục trước khi dùng cho dịp quan trọng.'},
    @{Stars=2;Text='Mẫu có ý tưởng đẹp nhưng phần hoa chính còn thưa, tổng thể chưa cân xứng với giá. Mình mong có thêm ảnh rõ để dễ chọn kích thước.'},
    @{Stars=2;Text='Tông màu trên ảnh khá thu hút, tuy nhiên phần lá phụ nhiều nên nhìn hơi rối. Nếu giảm bớt lá và chỉnh tỷ lệ hoa chính sẽ phù hợp hơn.'},
    @{Stars=2;Text='Bố cục hiện tại chưa làm nổi bật loại hoa chủ đạo. Cách phối màu và kích thước thực tế cần được mô tả rõ hơn trước khi khách đặt.'},
    @{Stars=3;Text='Tổng thể ổn và màu hoa dễ nhìn, nhưng thiết kế chưa có nhiều điểm nhấn. Nên bổ sung góc chụp bên cạnh để hình dung rõ độ đầy của mẫu.'},
    @{Stars=3;Text='Mẫu phù hợp làm quà đơn giản, mức giá cần cân nhắc với số lượng hoa chính. Mình thích phần phối màu nhưng muốn bố cục gọn gàng hơn.'},
    @{Stars=3;Text='Kiểu dáng khá dễ sử dụng, còn phần hoa phụ hơi nhiều so với sở thích của mình. Nếu có lựa chọn thay màu hoặc giảm lá thì sẽ dễ đặt hơn.'},
    @{Stars=3;Text='Hoa chủ đạo nổi bật nhưng tỷ lệ các lớp hoa chưa thật đều. Thiết kế ở mức vừa phải; thông tin chiều cao và chiều rộng nên rõ ràng hơn.'},
    @{Stars=3;Text='Màu sắc nhẹ nhàng, hợp trang trí bàn nhỏ. Ảnh hiện tại chưa thể hiện hết kích thước nên mình vẫn cần tư vấn thêm trước khi chọn mẫu.'},
    @{Stars=4;Text='Cách phối màu hài hòa, phần hoa chính có điểm nhấn và hình thức khá gọn. Nếu ảnh cận cảnh sắc nét hơn thì việc chọn mẫu sẽ thuận tiện hơn.'},
    @{Stars=4;Text='Thiết kế trang nhã và dễ phối với nhiều không gian. Mình thích bố cục chung, chỉ muốn điều chỉnh nhẹ phần hoa phụ để mẫu thoáng hơn.'},
    @{Stars=4;Text='Tông màu cân đối, hình dáng phù hợp làm quà tặng. Thông tin mẫu khá dễ hiểu; nên có thêm lựa chọn kích thước để phù hợp nhiều mức chi phí.'},
    @{Stars=4;Text='Các lớp hoa được phối khá mềm mại, điểm nhấn chính rõ ràng. Mẫu sẽ hoàn thiện hơn nếu có thêm một góc ảnh cho thấy phần chân hoặc đế.'},
    @{Stars=4;Text='Phần hoa chủ đạo và màu phụ kết hợp đẹp, tổng thể nhìn thanh lịch. Mình đánh giá tốt thiết kế này nhưng vẫn muốn có thêm thông số kích thước.'},
    @{Stars=4;Text='Bố cục gọn, màu sắc không quá rực nên dễ sử dụng cho nhiều dịp. Có thể thêm một lựa chọn phối màu khác để người mua dễ so sánh hơn.'},
    @{Stars=5;Text='Tông màu và điểm nhấn chính được phối rất cân đối, tổng thể trang nhã. Mẫu này có nét riêng và phù hợp với phong cách quà tặng mình đang tìm.'},
    @{Stars=5;Text='Bố cục có chiều sâu, hoa chính nổi bật mà không bị phần lá phụ lấn át. Mình thích sự hài hòa giữa màu sắc, hình dáng và cách sắp xếp.'},
    @{Stars=5;Text='Thiết kế tinh tế, các nhóm hoa liên kết tự nhiên và tạo điểm nhấn rõ ràng. Đây là một lựa chọn đẹp cho người thích phong cách nhẹ nhàng.'}
)
function Read-Table($connection,$transaction,[string]$sql){
    $command=$connection.CreateCommand();$command.Transaction=$transaction;$command.CommandTimeout=15;$command.CommandText=$sql
    $table=[Data.DataTable]::new();$reader=$command.ExecuteReader()
    try{$table.Load($reader)}finally{$reader.Dispose();$command.Dispose()}
    return ,$table
}
function Business-State($connection,$transaction){
    $table=Read-Table $connection $transaction @'
SELECT (SELECT COUNT(*) FROM orders) AS orders_count,
       (SELECT CHECKSUM_AGG(BINARY_CHECKSUM(id,status,payment_status,total_amount)) FROM orders) AS orders_hash,
       (SELECT SUM(CAST(sold AS bigint)) FROM products) AS sold,
       (SELECT SUM(CAST(quantity AS bigint)) FROM store_inventories) AS stock,
       (SELECT SUM(e_wallet) FROM stores) AS store_money,
       (SELECT SUM(balance) FROM customer_wallets) AS wallet_money,
       (SELECT COUNT(*) FROM payments) AS payments,
       (SELECT COUNT(*) FROM transactions) AS transactions,
       (SELECT COUNT(*) FROM reviews) AS reviews
'@
    return (($table.Rows[0].ItemArray | ForEach-Object{[string]$_}) -join '|')
}
$builder=[Data.SqlClient.SqlConnectionStringBuilder]::new()
$builder['Data Source']=$ServerInstance;$builder['Initial Catalog']=$DatabaseName;$builder['Integrated Security']=$true
$builder['Encrypt']=$true;$builder['TrustServerCertificate']=$true;$builder['Connect Timeout']=10
$connection=[Data.SqlClient.SqlConnection]::new($builder.ConnectionString);$transaction=$null
try {
    $connection.Open();$transaction=$connection.BeginTransaction([Data.IsolationLevel]::Serializable)
    $rows=Read-Table $connection $transaction 'SET LOCK_TIMEOUT 5000; SELECT r.id,r.user_id,r.product_id,r.store_id,r.order_id,r.rating,r.content,r.updated_at,p.name AS product_name FROM reviews r JOIN products p ON p.id=r.product_id ORDER BY r.product_id,r.id;'
    $targets=@($rows.Rows | Where-Object{ $_.IsNull('order_id') -and ($known.Contains([string]$_.content) -or ([string]$_.content).StartsWith('[Đánh giá mẫu] ')) })
    Write-Output ('Matched sample reviews: '+$targets.Count+'; others preserved: '+($rows.Rows.Count-$targets.Count))
    if($Mode -eq 'Plan' -or $targets.Count -eq 0){$transaction.Rollback();$transaction=$null;return}
    $before=Business-State $connection $transaction
    $backupDirectory=Join-Path $root '.tools/demo-review-backups'
    New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
    $backupPath=Join-Path $backupDirectory ('local-review-repair-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.json')
    $snapshot=@{
        Database=$DatabaseName
        Reviews=@($targets | ForEach-Object{@{Id=[long]$_.id;UserId=[long]$_.user_id;ProductId=[long]$_.product_id;Rating=[int]$_.rating;Content=[string]$_.content;UpdatedAt=if($_.IsNull('updated_at')){$null}else{([datetime]$_.updated_at).ToString('o')}}})
        Products=@((Read-Table $connection $transaction 'SELECT id,rating FROM products').Rows | ForEach-Object{@{Id=[long]$_.id;Rating=if($_.IsNull('rating')){$null}else{[double]$_.rating}}})
        Stores=@((Read-Table $connection $transaction 'SELECT id,rating FROM stores').Rows | ForEach-Object{@{Id=[long]$_.id;Rating=if($_.IsNull('rating')){$null}else{[double]$_.rating}}})
    }
    if($Mode -eq 'Apply'){$snapshot | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $backupPath -Encoding UTF8}
    $starCycle=@(5,4,3,2,4,3,5,4,3,2,4,1,3,4,5,3,4,2)
    $counts=@{};$index=0
    foreach($row in $targets){
        $stars=$starCycle[$index % $starCycle.Count]
        $variants=@($comments | Where-Object{$_.Stars -eq $stars})
        $sample=$variants[([int][Math]::Floor($index/$starCycle.Count)+[int]$row.product_id) % $variants.Count]
        $text='[Đánh giá mẫu] '+[string]$row.product_name+': '+$sample.Text
        if($text.Length -lt 50 -or $text.Length -gt 1000 -or $text -cmatch '[ÃÄÆ]|\uFFFD'){throw 'Invalid Unicode sample content; rollback.'}
        $command=$connection.CreateCommand();$command.Transaction=$transaction
        $command.CommandText='UPDATE reviews SET rating=@stars,content=@content,updated_at=GETDATE() WHERE id=@id AND order_id IS NULL;'
        [void]$command.Parameters.Add('@stars',[Data.SqlDbType]::Int);$command.Parameters['@stars'].Value=$sample.Stars
        [void]$command.Parameters.Add('@content',[Data.SqlDbType]::NVarChar,1000);$command.Parameters['@content'].Value=$text
        [void]$command.Parameters.Add('@id',[Data.SqlDbType]::BigInt);$command.Parameters['@id'].Value=[long]$row.id
        if($command.ExecuteNonQuery() -ne 1){throw 'Review changed concurrently; rollback.'}
        $command.Dispose();$counts[$sample.Stars]=1+$counts[$sample.Stars];$index++
    }
    $command=$connection.CreateCommand();$command.Transaction=$transaction;$command.CommandText=@'
UPDATE p SET rating=x.average_rating FROM products p CROSS APPLY (SELECT ROUND(AVG(CAST(rating AS decimal(10,4))),1) AS average_rating FROM reviews WHERE product_id=p.id) x WHERE x.average_rating IS NOT NULL;
UPDATE s SET rating=x.average_rating FROM stores s CROSS APPLY (SELECT ROUND(AVG(CAST(rating AS decimal(10,4))),1) AS average_rating FROM reviews WHERE store_id=s.id) x WHERE x.average_rating IS NOT NULL;
'@
    [void]$command.ExecuteNonQuery();$command.Dispose()
    if($before -ne (Business-State $connection $transaction)){throw 'Business data invariant failed; rollback.'}
    foreach($star in ($counts.Keys | Sort-Object)){Write-Output ($star.ToString()+' stars: '+$counts[$star])}
    if($Mode -eq 'Apply'){$transaction.Commit();Write-Output ('Applied Unicode/diversity repair. Backup: '+$backupPath)}
    else{$transaction.Rollback();Write-Output 'Rehearsal passed; all changes rolled back.'}
    $transaction=$null
}finally{if($transaction){$transaction.Rollback()};$connection.Dispose()}
