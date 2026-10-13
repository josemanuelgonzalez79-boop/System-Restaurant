# Bloque 12: instalación LAN y tablets (Windows)

Esta guía publica la edición local en un servidor Windows del restaurante. Los equipos de salón,
caja y cocina usan la misma URL HTTPS. No requiere internet para operar después de instalar las
dependencias y confiar en el certificado; PostgreSQL y Java no se exponen a la LAN.

## Antes de publicar

1. Reserva una dirección IP fija para el servidor en el router (por ejemplo, `192.168.1.50`).
   Conecta las tablets al mismo Wi-Fi/red, sin aislamiento entre clientes. Configura la red de
   Windows como **Privada**.
2. Instala PostgreSQL, JDK **21**, Node.js compatible con `package.json` y Caddy 2 para Windows.
   Descarga el ejecutable oficial de Caddy y ten a mano su ruta `caddy.exe`.
3. Conserva la base existente y `SystemRestaurantBack\secret.yml`. Si es una instalación nueva,
   crea ese archivo desde `secret.example.yml` con una contraseña PostgreSQL propia y crea la
   base `restaurant_system`. Mantén PostgreSQL escuchando solo en el servidor local.
4. Cierra Angular de desarrollo. Si ya hay una versión LAN ejecutándose, la compilación/publicación
   no la interrumpe; el reinicio posterior activa la nueva versión.
5. Como propietario, descarga un respaldo y realiza el simulacro de restauración a una base de
   prueba descrito en `PRUEBA-BLOQUE-11.md` antes de depender del equipo para operar.

No copies `secret.yml`, la clave privada de Caddy ni un ZIP de respaldo a una tablet.

## Publicar

En PowerShell, desde la carpeta del proyecto:

```powershell
.\deployment\Publicar-Essential.ps1 -LanIp 192.168.1.50 -CaddyExe 'C:\Herramientas\caddy.exe'
```

Opcionalmente usa `-Raiz 'D:\Essential'` y `-JavaExe 'C:\...\jdk-21\bin\java.exe'`.
El script verifica que la IP privada pertenezca al servidor, ejecuta las pruebas Maven, instala
dependencias npm mediante `npm ci`, compila Angular y valida el Caddyfile. Solo después apunta
`current.txt` a la nueva versión. Copia `secret.yml` solo si todavía no existe en la instalación;
una publicación posterior **no** lo reemplaza. La URL es `https://IP-DEL-SERVIDOR/`.

Para inspeccionar el inicio antes de instalar las tareas, abre dos ventanas PowerShell distintas:

```powershell
C:\Essential\bin\Iniciar-Essential.ps1 -Modo Backend
C:\Essential\bin\Iniciar-Essential.ps1 -Modo Proxy
```

Si usaste `-Raiz`, añade la misma opción a cada comando. Detén cada proceso con `Ctrl+C` antes
de instalar el inicio automático. Los mensajes del primer arranque de Caddy pueden indicar que
su CA no pudo instalarse automáticamente: el certificado se instalará manualmente más abajo.

Abre PowerShell **como administrador** y registra dos tareas al iniciar Windows y la regla de
firewall de entrada para TCP 443, perfil Privado, IP indicada, subred local y `caddy.exe`:

```powershell
.\deployment\Instalar-Arranque.ps1
Get-ScheduledTask Essential-Backend, Essential-Proxy | Select-Object TaskName, State
```

Si usaste `-Raiz`, añádela también aquí. Tras una actualización, vuelve a ejecutar el script de
arranque para reiniciar las tareas y cargar `current.txt`. El backend vuelve a intentar conectar
si PostgreSQL tarda en iniciar. Las tareas corren como `SYSTEM`: los archivos de `C:\Essential`
deben ser accesibles por esa cuenta. No compartas esa carpeta en la red.

## Certificados de los dispositivos

Caddy genera una CA local bajo `C:\Essential\data\caddy\pki\authorities\local\root.crt`.
Desde PowerShell administrador, tras iniciar el proxy, instala **solo el certificado público**
en la confianza del servidor:

```powershell
Import-Certificate -FilePath 'C:\Essential\data\caddy\pki\authorities\local\root.crt' -CertStoreLocation Cert:\LocalMachine\Root
.\deployment\Probar-LAN.ps1
```

Si usaste otra raíz, pasa `-Raiz` al script y cambia la ruta del certificado. Transfiere
`root.crt` a cada tablet por un medio controlado y verifica su SHA-256 en ambos extremos:

```powershell
Get-FileHash 'C:\Essential\data\caddy\pki\authorities\local\root.crt' -Algorithm SHA256
```

- **iPad/iPhone:** instala el perfil del certificado, luego actívalo en Ajustes → General →
  Información → Ajustes de confianza de certificados. Confía solo en la CA de este servidor.
- **Android:** importa el certificado como **certificado de CA** desde los ajustes de seguridad
  (los nombres varían por fabricante). Prueba con Chrome; otros navegadores o apps pueden usar
  almacenes de confianza distintos.
- **Windows tablet:** importa `root.crt` en Entidades de certificación raíz de confianza.

No copies `data\caddy\pki\authorities\local\root.key`: esa es la clave privada. Si cambia la
IP del servidor, vuelve a publicar con la nueva IP y reinicia las tareas; las tablets deberán
usar la nueva URL. Guarda la carpeta `data\caddy` en el servidor para conservar la CA.

## Prueba de aceptación con dos tablets

1. Abre `https://192.168.1.50/` en ambas tablets. Comprueba candado sin alerta, inicio de sesión
   y que la URL no use `:4200` ni `:3211`.
2. Verifica `https://192.168.1.50/api/health` y la página desde otro equipo de la misma LAN.
   En el servidor ejecuta `deployment\Probar-LAN.ps1` después de confiar en el certificado.
3. Asigna ambos usuarios a la sucursal desde **Estructura**. Abre **Preparación** en una tablet
   y captura/envía una comanda en la otra: debe aparecer sin esperar el sondeo de respaldo.
4. En DevTools de un navegador comprueba que la solicitud `wss://192.168.1.50/ws` recibe `101`
   y que el indicador muestra **Tiempo real**. La conexión `?token=...` de Angular de desarrollo
   no cuenta como evidencia.
5. Cobra una orden, cierra caja y revisa Reporte desde la tablet. Desconecta internet en el
   router dejando encendidos el servidor y la LAN: el flujo local debe continuar.
6. Reinicia Windows. Comprueba las tareas, inicio de sesión, sincronización, datos previos y la
  generación de un respaldo. Si falla, revisa Historial de tareas, los eventos de Windows y
  `C:\Essential\logs\Backend-AAAAmmdd.log` / `Proxy-AAAAmmdd.log`.
7. Confirma un simulacro **real** de restauración del ZIP en una base nueva según Bloque 11.
   Descargar el ZIP, por sí solo, no verifica la recuperación.

## Límites y operación

- Únicamente TCP 443 se abre en la red privada. No abras `5432`, `3211` ni el puerto de
  administración de Caddy en el router o firewall hacia internet.
- Cambiar el Wi-Fi/router, aislar clientes o apagar el servidor deja las tablets sin servicio.
  Considera UPS para servidor y router, y reservas DHCP estables.
- El certificado de CA local debe distribuirse y protegerse. Si se pierde `data\caddy`, se
  generará otra CA y las tablets tendrán que volver a confiar en ella.
- Los scripts y la guía están orientados a Windows. No se probó aquí el arranque en un servidor
  Windows físico ni el almacén de confianza de cada marca de tablet.
- Esta edición es una instalación por ubicación física. La WAN entre sucursales queda en la
  opción Multi-sucursal.
