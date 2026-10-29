param(
    [Parameter(Mandatory=$true)][ValidatePattern('^CodexFlowTests_[0-9_]+$')][string]$DatabaseName,
    [ValidatePattern('^127\.0\.0\.1:[0-9]+$')][string]$Server='127.0.0.1:53735',
    [string]$Tests='BusinessRegressionTests,OrderAftercareTests,StaffWorkspaceTests,CheckoutEdgeSafetyTests,DemoPaymentTests,ChatBranchFlowTests,AdminCategoryManagementUseCaseTests,AdminUsersManagementUseCaseTests,AdminProductsManagementUseCaseTests,AdminOrdersManagementUseCaseTests'
)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$settings=@{}
$saved=@{}
# Read only database credentials: other .env values must not enable external services in tests.
foreach($line in Get-Content -LiteralPath (Join-Path $projectRoot '.env') -Encoding UTF8) {
    if($line -match '^\s*(SPRING_DATASOURCE_USERNAME|SPRING_DATASOURCE_PASSWORD)\s*=(.*)$') {
        $key=$Matches[1]; $value=$Matches[2].Trim()
        if(($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))){$value=$value.Substring(1,$value.Length-2)}
        $settings[$key]=$value
    }
}
$settings['SPRING_DATASOURCE_URL']="jdbc:sqlserver://$Server;databaseName=$DatabaseName;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true;loginTimeout=15;"
$settings['SPRING_DATASOURCE_DRIVER_CLASS_NAME']='com.microsoft.sqlserver.jdbc.SQLServerDriver'
$settings['SPRING_JPA_DATABASE_PLATFORM']='org.hibernate.dialect.SQLServerDialect'
$settings['SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT']='org.hibernate.dialect.SQLServerDialect'
$settings['SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA']='dbo'
$settings['SPRING_JPA_HIBERNATE_DDL_AUTO']='validate'
$settings['SPRING_PROFILES_INCLUDE']='sql-integration'
$settings['SPRING_FLYWAY_ENABLED']='false'
$settings['APP_DEMO_DATA_ENABLED']='false'
try {
    foreach($key in $settings.Keys){$saved[$key]=[Environment]::GetEnvironmentVariable($key,'Process');[Environment]::SetEnvironmentVariable($key,$settings[$key],'Process')}
    if(!$env:SPRING_DATASOURCE_USERNAME -or !$env:SPRING_DATASOURCE_PASSWORD){throw 'Missing SQL test credentials.'}
    Write-Host "SQL integration tests: $DatabaseName on $Server; Hibernate validates the Flyway-managed schema."
    & (Join-Path $PSScriptRoot 'maven.ps1') -MavenArguments @('-o','-B','-Duser.timezone=UTC',"-Dtest=$Tests",'-Dspring.test.context.cache.maxSize=4','-Dsurefire.reportNameSuffix=sql-local','test')
    $testExit=$LASTEXITCODE
} finally {
    foreach($key in $saved.Keys){[Environment]::SetEnvironmentVariable($key,$saved[$key],'Process')}
}
exit $testExit
