param([ValidateRange(1024,65535)][int]$Port = 8080)
$ErrorActionPreference = 'Stop'
$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = New-Object Security.Principal.WindowsPrincipal($identity)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    $arguments = '-NoProfile -ExecutionPolicy Bypass -File "' + $PSCommandPath + '" -Port ' + $Port
    $elevated = Start-Process -FilePath 'powershell.exe' -ArgumentList $arguments -Verb RunAs -WindowStyle Hidden -PassThru -Wait
    if ($elevated.ExitCode -ne 0) { throw "Firewall update failed. See target/demo-phone-$Port.log for details." }
    Write-Host "Phone access enabled on Wi-Fi, TCP $Port, local subnet only."
    exit 0
}

try {
    $network = Get-NetAdapter -Name 'Wi-Fi' -ErrorAction Stop
    if ($network.Status -ne 'Up') { throw 'Connect the computer to Wi-Fi first.' }
    $listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop | Select-Object -First 1
    $javaExecutable = (Get-Process -Id $listener.OwningProcess).Path
    if ((Split-Path -Leaf $javaExecutable) -notin @('java.exe','javaw.exe')) {
        throw "Port $Port is not served by the Java application."
    }
    $ruleName = "RentalShopDemo-$Port"
    if (-not (Get-NetFirewallRule -Name $ruleName -ErrorAction SilentlyContinue)) {
        New-NetFirewallRule -Name $ruleName -DisplayName "Rental Shop Demo $Port" -Direction Inbound -Action Allow `
            -Protocol TCP -LocalPort $Port -RemoteAddress LocalSubnet -InterfaceAlias 'Wi-Fi' -Profile Any `
            -Program $javaExecutable | Out-Null
    } else {
        Set-NetFirewallRule -Name $ruleName -Enabled True -Direction Inbound -Action Allow -Profile Any -InterfaceAlias 'Wi-Fi'
        Get-NetFirewallRule -Name $ruleName | Get-NetFirewallApplicationFilter | Set-NetFirewallApplicationFilter -Program $javaExecutable
        Get-NetFirewallRule -Name $ruleName | Get-NetFirewallAddressFilter | Set-NetFirewallAddressFilter -RemoteAddress LocalSubnet
        Get-NetFirewallRule -Name $ruleName | Get-NetFirewallPortFilter | Set-NetFirewallPortFilter -Protocol TCP -LocalPort $Port
    }
    Write-Host "Phone access enabled on Wi-Fi, TCP $Port, local subnet only."
} catch {
    $logPath = Join-Path (Split-Path -Parent $PSScriptRoot) "target/demo-phone-$Port.log"
    $_ | Out-String | Set-Content -LiteralPath $logPath -Encoding UTF8
    throw
}
