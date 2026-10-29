param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (!$JavaHome) { throw 'Pass -JavaHome pointing to JDK 21.' }
$JavaHome = (Resolve-Path -LiteralPath $JavaHome).Path
$fixture = Join-Path $root ('target/start-web-fixture-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path (Join-Path $fixture 'scripts') -Force | Out-Null
foreach ($name in @('start-web.ps1', '.env.aiven.example', '.env.local-postgres.example')) {
    Copy-Item -LiteralPath (Join-Path $root $name) -Destination (Join-Path $fixture $name)
}
$launch = Join-Path $fixture 'start-web.ps1'
$record = Join-Path $fixture 'observed.json'
$mock = @'
param([string[]]$MavenArguments)
@{
    profile=$env:SPRING_PROFILES_ACTIVE
    datasource=$env:SPRING_DATASOURCE_URL
    seed=$env:APP_DEMO_DATA_ENABLED
    migrate=$env:SPRING_FLYWAY_ENABLED
    baseline=$env:SPRING_FLYWAY_BASELINE_ON_MIGRATE
    ddl=$env:SPRING_JPA_HIBERNATE_DDL_AUTO
    tunnel=$env:DEMO_PAYMENT_AUTO_TUNNEL_ENABLED
    arguments=$MavenArguments
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path (Split-Path -Parent $PSScriptRoot) 'observed.json')
exit [int]$env:START_WEB_TEST_EXITCODE
'@
$mock | Set-Content -LiteralPath (Join-Path $fixture 'scripts/maven.ps1') -Encoding UTF8
$saved = @{}
$keys = @('JAVA_HOME','JWT_SECRET','START_WEB_TEST_EXITCODE','SPRING_DATASOURCE_URL','SPRING_PROFILES_ACTIVE')
$keys += @(Get-ChildItem Env: | Where-Object { $_.Name -match '^(AIVEN_|LOCAL_DB_)' } | ForEach-Object { $_.Name })
foreach ($key in $keys | Select-Object -Unique) { $saved[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }
$passed = 0
function Assert-Check([bool]$Condition, [string]$Name) {
    if (!$Condition) { throw "FAIL: $Name" }
    $script:passed++; Write-Host "PASS: $Name"
}
try {
    foreach ($key in $saved.Keys) { [Environment]::SetEnvironmentVariable($key, $null, 'Process') }
    & $launch -Init
    $config = Join-Path $fixture '.env.aiven'
    $text = Get-Content -LiteralPath $config -Raw
    $key = ([regex]::Match($text, '(?m)^JWT_SECRET=(.+)$')).Groups[1].Value.Trim()
    Assert-Check ([Convert]::FromBase64String($key).Length -ge 32) 'Init generates a fresh JWT key without .tools'
    $before = (Get-FileHash -LiteralPath $config).Hash
    $refused = $false
    try { & $launch -Init } catch { $refused = $true }
    Assert-Check ($refused -and (Get-FileHash -LiteralPath $config).Hash -eq $before) 'Init refuses to overwrite existing configuration'
    $text = $text.Replace('pg-your-service.aivencloud.com','test.invalid').Replace('replace_with_service_port','5432').Replace('replace_with_private_password','test-only-not-a-real-password')
    $text | Set-Content -LiteralPath $config -Encoding UTF8
    'fixture only; Check does not verify TLS' | Set-Content -LiteralPath (Join-Path $fixture 'ca.pem')
    & $launch -Check -JavaHome $JavaHome
    Assert-Check (!(Test-Path -LiteralPath $record)) 'Check does not call Maven or start an application'
    $env:SPRING_DATASOURCE_URL = 'unrelated-datasource-sentinel'
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    $env:JAVA_HOME = 'unrelated-jdk-sentinel'
    $env:START_WEB_TEST_EXITCODE = '7'
    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    & $launch -JavaHome $JavaHome -Port $port
    Assert-Check ($LASTEXITCODE -eq 7) 'Launcher propagates Maven failure exit code'
    $observed = Get-Content -LiteralPath $record -Raw | ConvertFrom-Json
    Assert-Check ($observed.profile -eq 'aiven' -and !$observed.datasource) 'Default Aiven clears unrelated datasource/profile'
    Assert-Check ($observed.seed -eq 'false' -and $observed.migrate -eq 'false' -and $observed.baseline -eq 'false' -and $observed.ddl -eq 'validate') 'Default startup cannot seed, migrate or baseline schema'
    Assert-Check ($observed.tunnel -eq 'false') 'Public tunnel is opt-in'
    Assert-Check ($env:SPRING_DATASOURCE_URL -eq 'unrelated-datasource-sentinel' -and $env:SPRING_PROFILES_ACTIVE -eq 'dev' -and $env:JAVA_HOME -eq 'unrelated-jdk-sentinel') 'Environment restored after failed Maven run'
    $env:START_WEB_TEST_EXITCODE = '0'
    & $launch -JavaHome $JavaHome -Port $port -Migrate
    $observed = Get-Content -LiteralPath $record -Raw | ConvertFrom-Json
    Assert-Check ($LASTEXITCODE -eq 0 -and $observed.migrate -eq 'true' -and $observed.baseline -eq 'false') 'Migration needs explicit opt-in and never enables baseline'
    & $launch -Database Local -Init
    $localConfig = Join-Path $fixture '.env.local-postgres'
    (Get-Content -LiteralPath $localConfig -Raw).Replace('replace_with_private_password','test-only-not-a-real-password') | Set-Content -LiteralPath $localConfig -Encoding UTF8
    & $launch -Database Local -Check -JavaHome $JavaHome
    & $launch -Database Local -JavaHome $JavaHome -Port $port
    $observed = Get-Content -LiteralPath $record -Raw | ConvertFrom-Json
    Assert-Check ($observed.profile -eq 'local-postgres') 'Local profile works with fresh configuration and no private embedded runtime'
    $text.Replace('AIVEN_PASSWORD=test-only-not-a-real-password','AIVEN_PASSWORD=') | Set-Content -LiteralPath $config -Encoding UTF8
    $refused = $false
    try { & $launch -Check -JavaHome $JavaHome } catch { $refused = $true }
    Assert-Check $refused 'Missing credentials fail before Maven or connection'
    $text.Replace('AIVEN_PORT=5432','AIVEN_PORT=invalid') | Set-Content -LiteralPath $config -Encoding UTF8
    $refused = $false
    try { & $launch -Check -JavaHome $JavaHome } catch { $refused = $true }
    Assert-Check $refused 'Invalid port fails during configuration check'
    Write-Host "All $passed launcher checks passed. Only fixture files and mocked Maven were used; no database connection."
} finally {
    foreach ($key in $saved.Keys) { [Environment]::SetEnvironmentVariable($key, $saved[$key], 'Process') }
    # This fixture contains only generated test configuration and mocked Maven.
    # Keep repeated launcher checks from leaving private test files in target/.
    $fixtureTarget = [IO.Path]::GetFullPath((Join-Path $root 'target'))
    $fixturePath = [IO.Path]::GetFullPath($fixture)
    if ([IO.Path]::GetDirectoryName($fixturePath) -ne $fixtureTarget -or
        ![IO.Path]::GetFileName($fixturePath).StartsWith('start-web-fixture-')) {
        throw 'Refusing to remove a launcher fixture outside target/.'
    }
    if (Test-Path -LiteralPath $fixturePath) {
        Remove-Item -LiteralPath $fixturePath -Recurse -Force
    }
}
