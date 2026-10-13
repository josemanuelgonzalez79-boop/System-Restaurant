# Bloque 11 · respaldos y restauración guiados

## Qué cambió

- Solo el propietario ve **Respaldos** y puede solicitar `POST /api/v1/backups`.
- El backend ejecuta `pg_dump` en formato custom, valida el índice con `pg_restore --list`,
  calcula SHA-256 y entrega un ZIP con `database.dump` y `manifest.json`. No guarda copias
  permanentes en el servidor; elimina los archivos temporales al terminar la descarga.
- La restauración es deliberadamente **offline y guiada**: el script PowerShell verifica el ZIP,
  restaura en una base nueva y comprueba Flyway. Solo con `-Promover`, confirmación exacta,
  backend detenido, ninguna conexión abierta y una copia adicional de seguridad renombra las bases.
  Nunca elimina la base anterior.
- No hay migración nueva. El cambio de Angular a `/api` usa el proxy local y evita el `403` CSRF
  en la creación inicial de propietario; también permite que las tablets no llamen a su propio
  `localhost`.

## Preparación

1. Conserva `SystemRestaurantBack/secret.yml`; nunca lo copies del ZIP ni lo subas a Git.
2. En el **equipo que ejecuta Spring Boot**, instala las utilidades PostgreSQL (`pg_dump` y
   `pg_restore`) de versión compatible o más nueva que el servidor. En Windows, Essential busca
   automáticamente la versión más reciente en `C:\Program Files\PostgreSQL\<versión>\bin`.
   También acepta la carpeta `bin` en el `PATH` o las variables `APP_BACKUP_PG_DUMP` y
   `APP_BACKUP_PG_RESTORE` con rutas absolutas, definidas antes de arrancar Spring.
3. Levanta backend con JDK 21 y Angular con `npm start` (usa `proxy.conf.json`).
4. Inicia sesión como propietario y confirma que `Usuarios` y `Respaldos` están visibles.

## Prueba de respaldo

1. Registra un producto y un pedido de prueba para que la copia contenga información reconocible.
2. En **Respaldos**, pulsa **Generar y descargar ZIP**. Guarda el ZIP en un disco distinto al del
   servidor. La petición puede tardar si la base es grande; no cierres la pestaña.
3. Abre el ZIP y comprueba que contiene exactamente `manifest.json` y `database.dump`.
4. Repite con un usuario `ADMIN`: no verá la pantalla y un `POST` directo debe recibir `403`.
5. Si faltan `pg_dump`/`pg_restore`, el servidor debe responder con un error que nombre la
   herramienta y no generar un ZIP vacío. La pantalla muestra el detalle del backend incluso
   cuando Angular espera una descarga binaria. Una segunda descarga simultánea recibe `409`.

### Si aparece 503 al generar el ZIP

La petición sí llegó al backend, pero falló una herramienta de PostgreSQL o la escritura temporal.
Lee el mensaje rojo dentro de **Respaldos** después de aplicar esta corrección. En PowerShell del
equipo servidor comprueba:

```powershell
& "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" --version
& "C:\Program Files\PostgreSQL\18\bin\pg_restore.exe" --version
```

Si están en otra versión/carpeta, usa esa ruta. Para configurarlas de forma explícita en la
consola desde la que arrancas Spring Boot:

```powershell
$env:APP_BACKUP_PG_DUMP = "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe"
$env:APP_BACKUP_PG_RESTORE = "C:\Program Files\PostgreSQL\18\bin\pg_restore.exe"
.\mvnw.cmd spring-boot:run
```

No escribas la contraseña en la pantalla de Respaldos; el backend usa su configuración existente
de base de datos. Si el mensaje habla de autenticación, revisa `DB_USERNAME` y `DB_PASSWORD` del
backend. Si habla de versión, usa herramientas PostgreSQL compatibles con el servidor.

## Prueba de restauración SIN alterar la base actual

En PowerShell, desde `SystemRestaurantBack` del servidor (ajusta rutas y versión):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Restaurar-Essential.ps1 `
  -Archivo "D:\Respaldos\Essential-AAAAMMDD-HHMMSS.zip" `
  -PgBin "C:\Program Files\PostgreSQL\18\bin"
```

El script pedirá la contraseña de PostgreSQL sin imprimirla. Debe informar una base nueva como
`restaurant_system_restore_20261013194500` y verificar migraciones. La base de trabajo
`restaurant_system` queda intacta. Conéctate a la base de prueba y confirma el producto/pedido.
Una copia alterada o truncada debe fallar por huella SHA-256 antes de crear la base.

## Promoción real (solo ante recuperación necesaria)

1. Hazlo fuera del horario de atención. Detén Spring Boot, cierra pgAdmin u otras conexiones y
   asegúrate de tener espacio suficiente para dos bases y una copia adicional.
2. Crea previamente `D:\Respaldos\Seguridad` fuera de la carpeta del proyecto.
3. Repite el comando anterior añadiendo `-Promover -DirectorioSeguridad "D:\Respaldos\Seguridad"`.
   La primera parte volverá a restaurar una base de prueba y comprobarla.
4. Escribe exactamente `RESTAURAR restaurant_system` solo cuando estés listo. El script
   generará un respaldo de seguridad de la base actual y su SHA-256, renombrará la base actual
   como `restaurant_system_before_<fecha>` y pondrá la copia validada en su lugar. Si detecta
   conexiones abiertas, se detiene sin renombrar.
5. Levanta Spring Boot. Comprueba inicio de sesión, catálogo, pedidos, caja y reporte. Conserva
   la base anterior y el ZIP hasta terminar la revisión. Si algo falla, detén Spring Boot y
   planifica el regreso a la base anterior; **no borres ninguna base en automático**.

## Limitaciones y siguiente bloque

- El ZIP contiene datos sensibles. Restringe su acceso y preferiblemente cifra el medio de
  almacenamiento. Este bloque no configura tareas automáticas ni nube; la disciplina diaria
  de copias sigue a cargo del responsable del restaurante.
- La restauración requiere acceso administrativo al servidor PostgreSQL y privilegios para crear
  y renombrar bases. No puede ejecutarse desde una tablet.
- Bloque 12: instalación reproducible del servidor local, publicación por HTTPS/LAN, acceso desde
  tablets y verificación de arranque/recuperación.
