param([Parameter(ValueFromRemainingArguments = $true)][string[]]$MavenArguments)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$candidates = [Collections.Generic.List[string]]::new()
if ($env:JAVA_HOME) { $candidates.Add($env:JAVA_HOME) }
foreach ($directory in @((Join-Path $projectRoot '.tools'), 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Java')) {
    if (Test-Path -LiteralPath $directory) {
        Get-ChildItem -LiteralPath $directory -Directory -Filter 'jdk-21*' | ForEach-Object { $candidates.Add($_.FullName) }
    }
}
$selectedJdk = $null
foreach ($candidate in $candidates) {
    $javaExecutable = Join-Path $candidate 'bin/java.exe'
    if (Test-Path -LiteralPath $javaExecutable) {
        # java -version writes normal version information to stderr. Windows
        # PowerShell 5.1 treats redirected native stderr as NativeCommandError
        # under ErrorActionPreference=Stop, even when Java exits successfully.
        # Capture the two process streams directly instead of PowerShell 2>&1.
        $probeInfo = New-Object System.Diagnostics.ProcessStartInfo
        $probeInfo.FileName = $javaExecutable
        $probeInfo.Arguments = '-version'
        $probeInfo.UseShellExecute = $false
        $probeInfo.CreateNoWindow = $true
        $probeInfo.RedirectStandardOutput = $true
        $probeInfo.RedirectStandardError = $true
        $probe = $null
        try {
            $probe = [System.Diagnostics.Process]::Start($probeInfo)
            $probeOutput = $probe.StandardOutput.ReadToEndAsync()
            $probeError = $probe.StandardError.ReadToEndAsync()
            if (!$probe.WaitForExit(10000)) {
                $probe.Kill()
                $probe.WaitForExit()
                continue
            }
            $runtimeVersion = $probeOutput.GetAwaiter().GetResult() + $probeError.GetAwaiter().GetResult()
            if ($probe.ExitCode -eq 0 -and $runtimeVersion -match 'version "21\.') {
                $selectedJdk = $candidate
                break
            }
        } catch {
            # A broken/inaccessible JAVA_HOME must not prevent trying the
            # project's portable JDK or another installed JDK 21.
            continue
        } finally { if ($probe) { $probe.Dispose() } }
    }
}
if (!$selectedJdk) { throw 'Java 21 is required. Install Temurin JDK 21 or set JAVA_HOME to JDK 21.' }
Write-Host "Using JDK 21: $selectedJdk"
$originalJavaHome = $env:JAVA_HOME
$originalPath = $env:Path
$mavenExit = 1
try {
    $env:JAVA_HOME = $selectedJdk
    $env:Path = (Join-Path $selectedJdk 'bin') + ';' + $originalPath
    Push-Location -LiteralPath $projectRoot
    try {
        & (Join-Path $projectRoot 'mvnw.cmd') @MavenArguments
        $mavenExit = $LASTEXITCODE
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $originalJavaHome
    $env:Path = $originalPath
}
exit $mavenExit
