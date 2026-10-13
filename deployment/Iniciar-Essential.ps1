#requires -Version 5.1
<#
Ejecutar una instancia Backend y otra Proxy (en dos terminales o tareas programadas).
La tarea permanece viva y reintenta tras un fallo o un PostgreSQL que arranca tarde.
#>
param(
    [Parameter(Mandatory = $true)][ValidateSet('Backend', 'Proxy')][string]$Modo,
    [string]$Raiz = 'C:\Essential'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$Raiz = [IO.Path]::GetFullPath($Raiz)
$actual = Join-Path $Raiz 'current.txt'
$configuracion = Join-Path $Raiz 'runtime.json'
if (-not (Test-Path -LiteralPath $actual -PathType Leaf) -or
    -not (Test-Path -LiteralPath $configuracion -PathType Leaf)) {
    throw 'Primero ejecuta Publicar-Essential.ps1.'
}
$version = (Get-Content -LiteralPath $actual -Raw).Trim()
if ($version -cnotmatch '^\d{14}-[0-9a-f]{6}$') { throw 'El identificador de versión no es válido.' }
$release = Join-Path (Join-Path $Raiz 'releases') $version
$ajustes = Get-Content -LiteralPath $configuracion -Raw | ConvertFrom-Json
$env:XDG_DATA_HOME = Join-Path $Raiz 'data'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SERVER_PORT = '3211'

if ($Modo -eq 'Backend') {
    $programa = [string]$ajustes.javaExe
    $argumentos = @('-jar', (Join-Path $release 'essential.jar'))
    $directorio = Join-Path $Raiz 'backend'
} else {
    $programa = Join-Path $Raiz 'bin\caddy.exe'
    $argumentos = @('run', '--config', (Join-Path $release 'Caddyfile'), '--adapter', 'caddyfile')
    $directorio = $release
}
if (-not (Test-Path -LiteralPath $programa -PathType Leaf)) { throw "No existe $programa" }
$logs = Join-Path $Raiz 'logs'
New-Item -ItemType Directory -Path $logs -Force | Out-Null
$ErrorActionPreference = 'Continue' # Los procesos nativos pueden escribir avisos en stderr.
Push-Location $directorio
try {
    while ($true) {
        $registro = Join-Path $logs ("${Modo}-$(Get-Date -Format yyyyMMdd).log")
        "$(Get-Date -Format o) Iniciando $Modo ($version)." | Out-File -FilePath $registro -Append
        & $programa @argumentos *>> $registro
        "$(Get-Date -Format o) $Modo se detuvo (código $LASTEXITCODE); reintentando en 15 segundos." |
            Out-File -FilePath $registro -Append
        Start-Sleep -Seconds 15
    }
} finally { Pop-Location }
