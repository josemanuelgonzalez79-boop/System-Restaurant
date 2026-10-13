#requires -Version 5.1
<#
Restaura un respaldo de Essential a una base NUEVA. No modifica la base en uso.
Para promoverla a producción, detener primero el backend y ejecutar con -Promover.
#>
param(
    [Parameter(Mandatory = $true)][string]$Archivo,
    [string]$PgBin = '',
    [string]$Servidor = 'localhost',
    [ValidateRange(1, 65535)][int]$Puerto = 5432,
    [string]$Usuario = 'postgres',
    [switch]$Promover,
    [string]$DirectorioSeguridad = ''
)

$ErrorActionPreference = 'Stop'

function Herramienta([string]$Nombre) {
    if ($PgBin) {
        $ruta = Join-Path $PgBin ($Nombre + '.exe')
        if (-not (Test-Path -LiteralPath $ruta -PathType Leaf)) { throw "No se encontró $ruta" }
        return $ruta
    }
    $comando = Get-Command ($Nombre + '.exe') -ErrorAction SilentlyContinue
    if (-not $comando) { $comando = Get-Command $Nombre -ErrorAction SilentlyContinue }
    if (-not $comando) { throw "No se encontró $Nombre. Indica -PgBin con la carpeta bin de PostgreSQL." }
    return $comando.Source
}

function Ejecutar([string]$Programa, [string[]]$Argumentos) {
    & $Programa @Argumentos | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Falló $([IO.Path]::GetFileName($Programa)) (código $LASTEXITCODE)." }
}

if (-not (Test-Path -LiteralPath $Archivo -PathType Leaf)) { throw 'No existe el ZIP indicado.' }
$Archivo = (Resolve-Path -LiteralPath $Archivo).Path
$restaurar = Herramienta 'pg_restore'
$crear = Herramienta 'createdb'
$psql = Herramienta 'psql'
if ($Promover) {
    if (-not $DirectorioSeguridad -or -not (Test-Path -LiteralPath $DirectorioSeguridad -PathType Container)) {
        throw 'Para promover debes indicar -DirectorioSeguridad con una carpeta existente fuera del proyecto.'
    }
    $volcar = Herramienta 'pg_dump'
}

$temporal = Join-Path ([IO.Path]::GetTempPath()) ('Essential-restore-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temporal | Out-Null
$contrasenaAnterior = [Environment]::GetEnvironmentVariable('PGPASSWORD', 'Process')
$seCreoBase = $false
$sePromovio = $false
$basePrueba = ''
try {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($Archivo)
    try {
        $entradas = @($zip.Entries | Where-Object { $_.FullName -eq 'manifest.json' -or $_.FullName -eq 'database.dump' })
        if ($zip.Entries.Count -ne 2 -or $entradas.Count -ne 2) {
            throw 'El ZIP debe contener exactamente manifest.json y database.dump.'
        }
        foreach ($entrada in $entradas) {
            $destino = Join-Path $temporal $entrada.FullName
            $origen = $entrada.Open()
            $salida = [IO.File]::Create($destino)
            try { $origen.CopyTo($salida) } finally { $salida.Dispose(); $origen.Dispose() }
        }
    } finally { $zip.Dispose() }
    $manifestPath = Join-Path $temporal 'manifest.json'
    $dumpPath = Join-Path $temporal 'database.dump'
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf) -or
        -not (Test-Path -LiteralPath $dumpPath -PathType Leaf)) {
        throw 'El ZIP no contiene manifest.json y database.dump.'
    }
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if ($manifest.format -ne 1 -or $manifest.application -ne 'Essential' -or
        $manifest.dumpFile -ne 'database.dump' -or
        $manifest.database -cnotmatch '^[a-zA-Z_][a-zA-Z0-9_]{0,48}$' -or
        $manifest.sha256 -cnotmatch '^[a-f0-9]{64}$') {
        throw 'El manifiesto no corresponde a un respaldo compatible de Essential.'
    }
    $tamano = (Get-Item -LiteralPath $dumpPath).Length
    $huella = (Get-FileHash -LiteralPath $dumpPath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($tamano -ne $manifest.bytes -or $huella -cne $manifest.sha256) {
        throw 'El respaldo está incompleto o fue modificado: no coincide su SHA-256.'
    }
    $baseActual = [string]$manifest.database
    $basePrueba = $baseActual + '_restore_' + (Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmss')
    if ($basePrueba.Length -gt 63) { throw 'El nombre de la base es demasiado largo para preparar una restauración.' }

    $segura = Read-Host "Contraseña PostgreSQL de $Usuario" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
    try {
        [Environment]::SetEnvironmentVariable('PGPASSWORD',
            [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr), 'Process')
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
    $conexion = @('-h', $Servidor, '-p', "$Puerto", '-U', $Usuario, '--no-password')
    Ejecutar $restaurar (@('--list', $dumpPath))
    $existente = & $psql @conexion -d postgres -At -v ON_ERROR_STOP=1 -c "SELECT 1 FROM pg_database WHERE datname = '$basePrueba'"
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible consultar PostgreSQL.' }
    if ($existente -eq '1') { throw "Ya existe la base de prueba $basePrueba. Reintenta en otro momento." }
    Ejecutar $crear ($conexion + @('--maintenance-db=postgres', $basePrueba))
    $seCreoBase = $true
    Ejecutar $restaurar ($conexion + @('--exit-on-error', '--no-owner', '--no-acl', '-d', $basePrueba, $dumpPath))
    $versiones = & $psql @conexion -d $basePrueba -At -v ON_ERROR_STOP=1 -c 'SELECT count(*) FROM flyway_schema_history WHERE success = true'
    if ($LASTEXITCODE -ne 0 -or [int]$versiones -lt 1) { throw 'La base restaurada no contiene el historial válido de Flyway.' }
    Write-Host "Restauración de prueba correcta: $basePrueba ($versiones migraciones)."

    if ($Promover) {
        $frase = "RESTAURAR $baseActual"
        $confirmacion = Read-Host "Detén Spring Boot. Para promover escribe exactamente: $frase"
        if ($confirmacion -cne $frase) {
            Write-Host "No se promovió. La base de prueba $basePrueba se conserva para revisión."
            return
        }
        $sesiones = & $psql @conexion -d postgres -At -v ON_ERROR_STOP=1 -c "SELECT count(*) FROM pg_stat_activity WHERE datname IN ('$baseActual', '$basePrueba')"
        if ($LASTEXITCODE -ne 0 -or [int]$sesiones -ne 0) {
            throw 'Hay conexiones activas a la base. Detén el backend y cierra pgAdmin antes de promover.'
        }
        $sufijo = (Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmss')
        $anterior = $baseActual + '_before_' + $sufijo
        if ($anterior.Length -gt 63) { throw 'El nombre de la base es demasiado largo para conservar la anterior.' }
        $seguridad = Join-Path (Resolve-Path -LiteralPath $DirectorioSeguridad).Path ($anterior + '.dump')
        Ejecutar $volcar ($conexion + @('--format=custom', '--no-owner', '--no-acl', '--file', $seguridad, $baseActual))
        Ejecutar $restaurar (@('--list', $seguridad))
        (Get-FileHash -LiteralPath $seguridad -Algorithm SHA256).Hash.ToLowerInvariant() |
            Set-Content -LiteralPath ($seguridad + '.sha256') -Encoding Ascii
        Ejecutar $psql ($conexion + @('-d', 'postgres', '-v', 'ON_ERROR_STOP=1', '-c', "ALTER DATABASE $baseActual RENAME TO $anterior"))
        try {
            Ejecutar $psql ($conexion + @('-d', 'postgres', '-v', 'ON_ERROR_STOP=1', '-c', "ALTER DATABASE $basePrueba RENAME TO $baseActual"))
        } catch {
            Ejecutar $psql ($conexion + @('-d', 'postgres', '-v', 'ON_ERROR_STOP=1', '-c', "ALTER DATABASE $anterior RENAME TO $baseActual"))
            throw
        }
        $sePromovio = $true
        Write-Host "Promovida: $baseActual. Anterior conservada como $anterior."
        Write-Host "Respaldo de seguridad: $seguridad. Arranca Spring Boot y comprueba login, pedidos y reporte."
    } else {
        Write-Host "La base original $baseActual NO se modificó. Para promover, vuelve a ejecutar con -Promover y -DirectorioSeguridad."
    }
} catch {
    if ($seCreoBase -and -not $sePromovio) {
        Write-Warning "La base de prueba $basePrueba se conserva para inspección. No se eliminó ninguna base."
    }
    throw
} finally {
    [Environment]::SetEnvironmentVariable('PGPASSWORD', $contrasenaAnterior, 'Process')
    Remove-Item -LiteralPath $temporal -Recurse -Force
}
