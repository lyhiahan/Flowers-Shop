param(
    [ValidateSet('Plan','Rehearse','Apply','Rollback','SelfTest')][string]$Mode = 'Plan',
    [string]$Snapshot
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $root
try {
    $jdk = @(Get-ChildItem -LiteralPath (Join-Path $root '.tools') -Directory -Filter 'jdk-21*' | Select-Object -First 1)
    $java = if ($jdk.Count) { Join-Path $jdk[0].FullName 'bin/java.exe' } elseif ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
    if (!(Test-Path -LiteralPath $java)) { throw 'Java 21 is required.' }
    $arguments = @('-Dstdout.encoding=UTF-8','-Dstderr.encoding=UTF-8')
    if ($Mode -ne 'SelfTest') {
        if (!(Test-Path -LiteralPath '.env.aiven')) { throw 'Configure Aiven locally first; do not put passwords into this script.' }
        & (Join-Path $PSScriptRoot 'maven.ps1') 'dependency:build-classpath' '-Dmdep.includeScope=runtime' '-Dmdep.outputFile=target/demo-review-runtime-classpath.txt' '-q'
        if ($LASTEXITCODE -ne 0) { throw 'Could not prepare the PostgreSQL driver.' }
        $arguments += @('--class-path',(Get-Content -LiteralPath 'target/demo-review-runtime-classpath.txt' -Raw).Trim())
    }
    $seedMode = '--' + $Mode.ToLowerInvariant().Replace('selftest','self-test')
    $arguments += @('scripts/java/DemoReviewSeed.java', $seedMode)
    if ($Mode -eq 'Rollback') {
        if ([string]::IsNullOrWhiteSpace($Snapshot)) { throw 'Supply -Snapshot with the snapshot file reported by Apply.' }
        $arguments += (Resolve-Path -LiteralPath $Snapshot -ErrorAction Stop).Path
    }
    & $java @arguments
    if ($LASTEXITCODE -ne 0) { throw 'Sample review operation failed; check the error. No partial database transaction was committed.' }
} finally { Pop-Location }
