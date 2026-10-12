param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$nodeExecutable = (Get-Command node -ErrorAction SilentlyContinue).Source
if (!$nodeExecutable) {
    $runtime = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools') -Directory -Filter 'node-v22*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($runtime) { $nodeExecutable = Join-Path $runtime.FullName 'node.exe' }
}
if (!$nodeExecutable) { throw 'Install Node.js 22 or place its portable runtime in .tools.' }
$testFiles = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/test/js') -File -Filter '*.test.cjs' | Select-Object -ExpandProperty FullName
& $nodeExecutable --test @testFiles
exit $LASTEXITCODE
