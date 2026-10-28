param(
    [Parameter(Mandatory=$true)][ValidatePattern('^[A-Za-z][A-Za-z0-9_]*$')][string]$DatabaseName,
    [ValidateSet('Run','Validate','Migrate')][string]$Mode='Run',
    [ValidatePattern('^[A-Za-z0-9.:-]+$')][string]$Server='127.0.0.1:1433',
    [ValidateRange(1024,65535)][int]$Port=8080
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$saved=@{}
$settings=@{}
$envPath=Join-Path $root '.env'
if(Test-Path -LiteralPath $envPath) {
    foreach($line in Get-Content -LiteralPath $envPath) {
        if($line -match '^\s*([A-Z][A-Z0-9_]*)\s*=(.*)$') {
            $key=$Matches[1];$value=$Matches[2].Trim()
            if(($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))){$value=$value.Substring(1,$value.Length-2)}
            if([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($key,'Process'))){$settings[$key]=$value}
        }
    }
}
$settings['SPRING_PROFILES_ACTIVE']='dev'
$settings['SPRING_DATASOURCE_URL']="jdbc:sqlserver://$Server;databaseName=$DatabaseName;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true;loginTimeout=15;"
$settings['SPRING_FLYWAY_ENABLED']='true'
$settings['SPRING_FLYWAY_BASELINE_ON_MIGRATE']='false'
$settings['APP_DEMO_DATA_ENABLED']='false'
$settings['DEMO_PAYMENT_AUTO_TUNNEL_ENABLED']='false'
$settings['SERVER_ADDRESS']='127.0.0.1'
$settings['SERVER_PORT']=[string]$Port
try {
    foreach($key in $settings.Keys){$saved[$key]=[Environment]::GetEnvironmentVariable($key,'Process');[Environment]::SetEnvironmentVariable($key,$settings[$key],'Process')}
    if([string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_USERNAME) -or [string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_PASSWORD)) {throw 'Configure SQL credentials in .env or environment variables.'}
    Write-Host "Mode=$Mode | SQL=$Server | Database=$DatabaseName | Flyway enabled"
    $arguments= switch($Mode) {
        'Validate' {@('compile','flyway:validate','-B')}
        'Migrate' {@('compile','flyway:migrate','-B')}
        'Run' {@('spring-boot:run','-Dspring-boot.run.profiles=dev',"-Dspring-boot.run.arguments=--app.demo-payment.base-url=http://localhost:$Port --app.demo-payment.auto-tunnel.enabled=false")}
    }
    & (Join-Path $PSScriptRoot 'maven.ps1') -MavenArguments $arguments
    $result=$LASTEXITCODE
}finally{foreach($key in $saved.Keys){[Environment]::SetEnvironmentVariable($key,$saved[$key],'Process')}}
exit $result
