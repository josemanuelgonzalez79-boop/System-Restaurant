#requires -Version 5.1
<#
Compila el proyecto y crea una versión lista para servir en una IP privada de la LAN.
No cambia la base de datos ni sustituye la versión activa hasta validar Caddy.
#>
param(
    [Parameter(Mandatory = $true)][string]$LanIp,
    [string]$Raiz = 'C:\Essential',
    [string]$CaddyExe = '',
    [string]$JavaExe = ''
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Ejecutable([string]$Nombre, [string]$Ruta) {
    if ($Ruta) {
        if (-not (Test-Path -LiteralPath $Ruta -PathType Leaf)) { throw "No existe $Ruta" }
        return (Resolve-Path -LiteralPath $Ruta).Path
    }
    $hallado = Get-Command $Nombre -ErrorAction SilentlyContinue
    if (-not $hallado) { throw "No se encontró $Nombre. Instálalo o indica su ruta." }
    return $hallado.Source
}

$direccion = $null
if (-not [Net.IPAddress]::TryParse($LanIp, [ref]$direccion) -or
    $direccion.AddressFamily -ne [Net.Sockets.AddressFamily]::InterNetwork) {
    throw 'LanIp debe ser una dirección IPv4 privada del servidor.'
}
$octetos = $direccion.GetAddressBytes()
if (-not ($octetos[0] -eq 10 -or
          ($octetos[0] -eq 172 -and $octetos[1] -ge 16 -and $octetos[1] -le 31) -or
          ($octetos[0] -eq 192 -and $octetos[1] -eq 168))) {
    throw 'Usa una IPv4 privada (10.x, 172.16-31.x o 192.168.x) asignada a este servidor.'
}
if (-not (Get-NetIPAddress -AddressFamily IPv4 -IPAddress $LanIp -ErrorAction SilentlyContinue)) {
    throw "La dirección $LanIp no pertenece a este equipo. Fija su IP o reserva DHCP primero."
}

$proyecto = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $proyecto 'SystemRestaurantBack'
$frontend = Join-Path $proyecto 'SystemRestaurantFront'
$secretoOrigen = Join-Path $backend 'secret.yml'
$secretoDestino = Join-Path $Raiz 'backend\secret.yml'
if (-not (Test-Path -LiteralPath $secretoOrigen -PathType Leaf) -and
    -not (Test-Path -LiteralPath $secretoDestino -PathType Leaf)) {
    throw 'Falta secret.yml. Conserva tu archivo de configuración y colócalo en SystemRestaurantBack o en la carpeta backend de la instalación.'
}
$caddy = Ejecutable 'caddy.exe' $CaddyExe
$java = Ejecutable 'java.exe' $JavaExe
$npm = Ejecutable 'npm.cmd' ''
$maven = Join-Path $backend 'mvnw.cmd'
if (-not (Test-Path -LiteralPath $maven -PathType Leaf)) { throw "No existe $maven" }
$versionJava = (& $java --version | Out-String)
if ($LASTEXITCODE -ne 0 -or $versionJava -notmatch '(?m)^(?:openjdk|java) 21(?:\.|\+|\s|$)') {
    throw 'El servidor necesita Java 21. Indica -JavaExe con el java.exe de JDK 21.'
}

Push-Location $backend
try {
    & $maven clean package
    if ($LASTEXITCODE -ne 0) { throw 'Falló Maven; no se publicará esta versión.' }
} finally { Pop-Location }
Push-Location $frontend
try {
    & $npm ci
    if ($LASTEXITCODE -ne 0) { throw 'Falló npm ci; no se publicará esta versión.' }
    & $npm run build
    if ($LASTEXITCODE -ne 0) { throw 'Falló Angular; no se publicará esta versión.' }
} finally { Pop-Location }

$jars = @(Get-ChildItem -LiteralPath (Join-Path $backend 'target') -Filter '*.jar' -File |
    Where-Object { $_.Name -notmatch '(original|sources|javadoc)' })
if ($jars.Count -ne 1) { throw 'Se esperaba un único JAR ejecutable en target.' }
$webOrigen = Join-Path $frontend 'dist\restaurant-system-front\browser'
if (-not (Test-Path -LiteralPath (Join-Path $webOrigen 'index.html') -PathType Leaf)) {
    throw 'No se encontró index.html en la compilación de Angular.'
}

$Raiz = [IO.Path]::GetFullPath($Raiz)
$version = (Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 6)
$destino = Join-Path (Join-Path $Raiz 'releases') $version
$webDestino = Join-Path $destino 'www'
$binDestino = Join-Path $Raiz 'bin'
New-Item -ItemType Directory -Path $webDestino, $binDestino, (Join-Path $Raiz 'backend'), (Join-Path $Raiz 'data') -Force | Out-Null
Copy-Item -LiteralPath ($jars[0].FullName) -Destination (Join-Path $destino 'essential.jar')
Get-ChildItem -LiteralPath $webOrigen -Force | Copy-Item -Destination $webDestino -Recurse -Force
if (-not (Test-Path -LiteralPath $secretoDestino -PathType Leaf)) {
    Copy-Item -LiteralPath $secretoOrigen -Destination $secretoDestino
}
$caddyDestino = Join-Path $binDestino 'caddy.exe'
if ($caddy -ne $caddyDestino) { Copy-Item -LiteralPath $caddy -Destination $caddyDestino -Force }
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'Iniciar-Essential.ps1') -Destination (Join-Path $binDestino 'Iniciar-Essential.ps1') -Force

# handle conserva /api y /ws antes del fallback de rutas Angular.
$rutaWeb = $webDestino.Replace('\', '/')
$configuracion = @"
{
    auto_https disable_redirects
}

https://${LanIp} {
    tls internal
    encode zstd gzip
    @backend path /api /api/* /ws /ws/*
    handle @backend {
        reverse_proxy 127.0.0.1:3211
    }
    handle {
        root * "$rutaWeb"
        try_files {path} /index.html
        file_server
    }
}
"@
$archivoCaddy = Join-Path $destino 'Caddyfile'
[IO.File]::WriteAllText($archivoCaddy, $configuracion, (New-Object Text.UTF8Encoding($false)))
$env:XDG_DATA_HOME = Join-Path $Raiz 'data'
try {
    & $caddyDestino validate --config $archivoCaddy --adapter caddyfile
    if ($LASTEXITCODE -ne 0) { throw 'Caddy rechazó la configuración; la versión anterior sigue activa.' }
} finally {
    Remove-Item Env:XDG_DATA_HOME -ErrorAction SilentlyContinue
}
$ajustes = @{ javaExe = $java; lanIp = $LanIp } | ConvertTo-Json
[IO.File]::WriteAllText((Join-Path $Raiz 'runtime.json'), $ajustes, (New-Object Text.UTF8Encoding($false)))
[IO.File]::WriteAllText((Join-Path $Raiz 'current.txt'), $version, (New-Object Text.UTF8Encoding($false)))
Write-Host "Versión publicada: $version"
Write-Host "Enlace de las tablets: https://$LanIp/"
Write-Host "Inicia backend y proxy con $binDestino\Iniciar-Essential.ps1 y consulta PRUEBA-BLOQUE-12.md."
