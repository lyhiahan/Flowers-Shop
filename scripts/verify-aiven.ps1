param([string]$JavaHome = $env:JAVA_HOME, [switch]$Application)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (!$JavaHome -or !(Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) {
    $JavaHome = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools') -Directory -Filter 'jdk-21*' |
        Select-Object -First 1 -ExpandProperty FullName
}
if (!$JavaHome) { throw 'Set JAVA_HOME to JDK 21 or pass -JavaHome.' }
Push-Location -LiteralPath $projectRoot
try {
    & (Join-Path $PSScriptRoot 'maven.ps1') -MavenArguments @('-o', 'dependency:build-classpath', '-Dmdep.includeScope=runtime', '-Dmdep.outputFile=target/aiven-readonly-classpath.txt', '-q')
    if ($LASTEXITCODE -ne 0) { throw 'Prepare Maven dependencies before running the read-only check.' }
    $classpath = (Get-Content -LiteralPath 'target/aiven-readonly-classpath.txt' -Raw).Trim()
    if ($Application) {
        & (Join-Path $PSScriptRoot 'maven.ps1') -MavenArguments @('-o', '-DskipTests', 'compile', '-q')
        if ($LASTEXITCODE -ne 0) { throw 'Could not compile application for read-only smoke check.' }
        $classpath = (Join-Path $projectRoot 'target/classes') + [IO.Path]::PathSeparator + $classpath
        $source = 'java/AivenApplicationSmoke.java'
    } else { $source = 'java/AivenReadOnlyCheck.java' }
    & (Join-Path $JavaHome 'bin/java.exe') --class-path $classpath (Join-Path $PSScriptRoot $source)
    $checkResult = $LASTEXITCODE
} finally { Pop-Location }
exit $checkResult
