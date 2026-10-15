$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'cloudinary-env.ps1')

function Assert-Check {
    param([bool]$Condition, [string]$Description)
    if (!$Condition) { throw "Failed: $Description" }
}

$names = @('CLOUDINARY_CLOUD_NAME', 'CLOUDINARY_API_KEY', 'CLOUDINARY_API_SECRET',
    'SPRING_PROFILES_ACTIVE', 'SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME',
    'SPRING_DATASOURCE_PASSWORD', 'MAIL_PROVIDER', 'MAIL_FROM_EMAIL', 'MAIL_FROM_NAME', 'BREVO_API_KEY')
$original = @{}
foreach ($name in $names) { $original[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
$testDirectory = Join-Path ([IO.Path]::GetTempPath()) ('cloudinary-env-test-' + [Guid]::NewGuid().ToString('N'))
[void][IO.Directory]::CreateDirectory($testDirectory)
$fixture = Join-Path $testDirectory '.env'
$passed = 0
try {
    $settings = Get-LocalCloudinarySettings -Path (Join-Path $testDirectory 'absent.env')
    Assert-Check ($settings.Count -eq 0) 'Missing .env is optional'
    $passed++

    [IO.File]::WriteAllText($fixture, @'
# Only dummy credentials are used by these tests.
CLOUDINARY_CLOUD_NAME = "dummy-cloud"
CLOUDINARY_API_KEY='123456'
CLOUDINARY_API_SECRET="dummy=secret#literal"
SPRING_DATASOURCE_URL=not-a-database-url
PATH=not-a-system-path
CLOUDINARY_URL=not-used
'@)
    $settings = Get-LocalCloudinarySettings -Path $fixture
    Assert-Check ($settings.Count -eq 3 -and $settings['CLOUDINARY_CLOUD_NAME'] -ceq 'dummy-cloud' -and
        $settings['CLOUDINARY_API_KEY'] -ceq '123456' -and
        $settings['CLOUDINARY_API_SECRET'] -ceq 'dummy=secret#literal') 'Literal parsing and allowlist'
    $passed++

    foreach ($name in @('CLOUDINARY_CLOUD_NAME', 'CLOUDINARY_API_KEY', 'CLOUDINARY_API_SECRET')) {
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
    }
    $databaseBefore = [Environment]::GetEnvironmentVariable('SPRING_DATASOURCE_URL', 'Process')
    $pathBefore = $env:PATH
    $output = @(Import-LocalCloudinarySettings -Path $fixture)
    Assert-Check ($output.Count -eq 0 -and $env:CLOUDINARY_CLOUD_NAME -ceq 'dummy-cloud' -and
        $env:CLOUDINARY_API_KEY -ceq '123456' -and $env:CLOUDINARY_API_SECRET -ceq 'dummy=secret#literal') 'Import without credential output'
    Assert-Check ($env:SPRING_DATASOURCE_URL -ceq $databaseBefore -and $env:PATH -ceq $pathBefore) 'Database and system environment unchanged'
    $passed++

    $env:CLOUDINARY_API_SECRET = 'explicit-dummy-secret'
    Import-LocalCloudinarySettings -Path $fixture
    Assert-Check ($env:CLOUDINARY_API_SECRET -ceq 'explicit-dummy-secret') 'Explicit environment takes precedence'
    $passed++

    [IO.File]::WriteAllText($fixture, 'CLOUDINARY_API_SECRET=$(throw ''must never execute'')')
    $settings = Get-LocalCloudinarySettings -Path $fixture
    Assert-Check ($settings['CLOUDINARY_API_SECRET'] -ceq '$(throw ''must never execute'')') 'Never evaluate shell expressions'
    $passed++

    foreach ($badContent in @('CLOUDINARY_API_SECRET="fixture-secret-do-not-echo',
            "CLOUDINARY_API_KEY=first`nCLOUDINARY_API_KEY=second", 'CLOUDINARY_API_SECRET')) {
        [IO.File]::WriteAllText($fixture, $badContent)
        $rejected = $false
        try { [void](Get-LocalCloudinarySettings -Path $fixture) }
        catch {
            $rejected = $_.Exception.Message -match '\.env line [0-9]+' -and
                $_.Exception.Message -notmatch 'fixture-secret-do-not-echo|first|second'
        }
        Assert-Check $rejected 'Malformed settings rejected without exposing values'
        $passed++
    }

    # Exercise the real SQL runner with a dummy Maven script; no Java, SQL or network.
    $fakeScripts = Join-Path $testDirectory 'scripts'
    [void][IO.Directory]::CreateDirectory($fakeScripts)
    foreach ($script in @('sqlserver.ps1', 'cloudinary-env.ps1')) {
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot $script) -Destination (Join-Path $fakeScripts $script)
    }
    [IO.File]::WriteAllText((Join-Path $fakeScripts 'maven.ps1'), @'
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$MavenArguments)
if ($env:CLOUDINARY_CLOUD_NAME -cne 'runner-cloud' -or
        $env:CLOUDINARY_API_KEY -cne '987654' -or
        $env:CLOUDINARY_API_SECRET -cne 'explicit-dummy-secret' -or
        $env:SPRING_DATASOURCE_URL -notlike '*instanceName=SQLEXPRESS;databaseName=RentalShop;*') {
    throw 'Dummy runner did not receive the intended environment.'
}
exit 0
'@)
    [IO.File]::WriteAllText($fixture, @'
CLOUDINARY_CLOUD_NAME=runner-cloud
CLOUDINARY_API_KEY=987654
CLOUDINARY_API_SECRET=file-dummy-secret
SPRING_DATASOURCE_URL=must-not-override-the-selected-instance
'@)
    $env:CLOUDINARY_CLOUD_NAME = ''
    $env:CLOUDINARY_API_KEY = ''
    $env:SPRING_DATASOURCE_PASSWORD = 'dummy-database-password'
    $env:BREVO_API_KEY = 'dummy-mail-key'
    $beforeRunner = @{}
    foreach ($name in $names) { $beforeRunner[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
    & (Join-Path $fakeScripts 'sqlserver.ps1')
    Assert-Check ($LASTEXITCODE -eq 0) 'SQL runner forwards the configured environment'
    foreach ($name in $names) {
        Assert-Check ([Environment]::GetEnvironmentVariable($name, 'Process') -ceq $beforeRunner[$name]) "SQL runner restores $name after exit"
    }
    $passed++

    [IO.File]::WriteAllText((Join-Path $fakeScripts 'maven.ps1'), "throw 'Expected dummy runner failure.'")
    $failed = $false
    try { & (Join-Path $fakeScripts 'sqlserver.ps1') }
    catch { $failed = $_.Exception.Message -eq 'Expected dummy runner failure.' }
    Assert-Check $failed 'Dummy runner failure was propagated'
    foreach ($name in $names) {
        Assert-Check ([Environment]::GetEnvironmentVariable($name, 'Process') -ceq $beforeRunner[$name]) "SQL runner restores $name even on failure"
    }
    $passed++
    Write-Host "Cloudinary local environment: $passed checks passed (dummy credentials only)."
} finally {
    foreach ($name in $names) {
        if ($null -eq $original[$name]) {
            Remove-Item -LiteralPath ('Env:\' + $name) -ErrorAction SilentlyContinue
        } else {
            [Environment]::SetEnvironmentVariable($name, $original[$name], 'Process')
        }
    }
    # Delete only the unique directory created by this test, after boundary checks.
    $resolvedTestDirectory = [IO.Path]::GetFullPath($testDirectory)
    $resolvedTempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\') + '\'
    if ($resolvedTestDirectory.StartsWith($resolvedTempRoot, [StringComparison]::OrdinalIgnoreCase) -and
            [IO.Path]::GetFileName($resolvedTestDirectory) -match '^cloudinary-env-test-[0-9a-f]{32}$') {
        Remove-Item -LiteralPath $resolvedTestDirectory -Recurse -Force
    }
}
