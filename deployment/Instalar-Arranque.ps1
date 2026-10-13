#requires -Version 5.1
# Ejecutar como administrador una vez, después de probar el inicio manual.
param([string]$Raiz = 'C:\Essential')

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$Raiz = [IO.Path]::GetFullPath($Raiz)
$ajustes = Get-Content -LiteralPath (Join-Path $Raiz 'runtime.json') -Raw | ConvertFrom-Json
$script = (Resolve-Path -LiteralPath (Join-Path $Raiz 'bin\Iniciar-Essential.ps1')).Path
$administrador = [Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()
if (-not $administrador.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw 'Ejecuta PowerShell como administrador para instalar las tareas y la regla de firewall.'
}
$disparador = New-ScheduledTaskTrigger -AtStartup
$principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
$opciones = New-ScheduledTaskSettingsSet -RestartInterval (New-TimeSpan -Minutes 1) -RestartCount 3 -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Seconds 0)
foreach ($modo in @('Backend', 'Proxy')) {
    $existente = Get-ScheduledTask -TaskName "Essential-$modo" -ErrorAction SilentlyContinue
    if ($existente -and $existente.State -eq 'Running') {
        Stop-ScheduledTask -TaskName "Essential-$modo"
        for ($intento = 0; $intento -lt 20; $intento++) {
            if ((Get-ScheduledTask -TaskName "Essential-$modo").State -ne 'Running') { break }
            Start-Sleep -Seconds 1
        }
        if ((Get-ScheduledTask -TaskName "Essential-$modo").State -eq 'Running') {
            throw "No se pudo detener Essential-$modo. La versión anterior sigue activa."
        }
    }
}
foreach ($modo in @('Backend', 'Proxy')) {
    $argumentos = '-NoProfile -ExecutionPolicy Bypass -File "{0}" -Modo {1} -Raiz "{2}"' -f $script, $modo, $Raiz
    $accion = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument $argumentos
    $tarea = New-ScheduledTask -Action $accion -Trigger $disparador -Principal $principal -Settings $opciones
    Register-ScheduledTask -TaskName "Essential-$modo" -InputObject $tarea -Force | Out-Null
}
$nombreRegla = 'Essential LAN HTTPS'
if (Get-NetFirewallRule -DisplayName $nombreRegla -ErrorAction SilentlyContinue) {
    Remove-NetFirewallRule -DisplayName $nombreRegla
}
New-NetFirewallRule -DisplayName $nombreRegla -Direction Inbound -Action Allow -Enabled True `
    -Profile Private -Protocol TCP -LocalPort 443 -LocalAddress ([string]$ajustes.lanIp) `
    -RemoteAddress LocalSubnet -Program (Join-Path $Raiz 'bin\caddy.exe') | Out-Null
Start-ScheduledTask -TaskName 'Essential-Backend'
Start-ScheduledTask -TaskName 'Essential-Proxy'
Write-Host 'Tareas instaladas y arrancadas. Solo TCP 443 en la red privada local.'
Write-Host "Abre https://$($ajustes.lanIp)/ después de confiar en el certificado raíz de este servidor."
