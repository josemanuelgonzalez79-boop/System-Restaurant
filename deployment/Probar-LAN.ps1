#requires -Version 5.1
# Ejecutar en el servidor después de instalar la CA local en Windows.
param([string]$Raiz = 'C:\Essential')

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$ajustes = Get-Content -LiteralPath (Join-Path $Raiz 'runtime.json') -Raw | ConvertFrom-Json
$ip = [string]$ajustes.lanIp
$raizCA = Join-Path $Raiz 'data\caddy\pki\authorities\local\root.crt'
if (-not (Test-Path -LiteralPath $raizCA -PathType Leaf)) {
    throw "Todavía no se generó el certificado raíz: $raizCA. Comprueba la tarea Essential-Proxy."
}
if (-not (Test-NetConnection -ComputerName $ip -Port 443 -InformationLevel Quiet)) {
    throw "No se puede llegar a ${ip}:443 desde este equipo. Revisa la IP y la tarea Proxy."
}
$inicio = Invoke-WebRequest -Uri "https://$ip/" -UseBasicParsing -TimeoutSec 15
if ($inicio.StatusCode -ne 200 -or $inicio.Content -notmatch '<app-root') {
    throw 'La página de Angular no se sirvió correctamente.'
}
$salud = Invoke-RestMethod -Uri "https://$ip/api/health" -TimeoutSec 15
Write-Host "Página Angular: HTTP $($inicio.StatusCode)"
Write-Host "API: $($salud | ConvertTo-Json -Compress)"
Write-Host "Certificado raíz para tablets: $raizCA"
Write-Host "Ahora prueba inicio de sesión y /ws (101) desde otra tablet en https://$ip/."
