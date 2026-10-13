# Validación de Essential

Base preparada con:

- Angular 21.2.x.
- PrimeNG 21.1.9.
- TypeScript 5.9.x.
- Node.js 22.12+ o 24.
- Proxy de desarrollo hacia Spring Boot en el puerto 3211.
- Acceso inicial, sesión y guardias de rutas.
- Usuarios y roles.
- Configuración del negocio.
- Categorías y productos o servicios.
- Sucursales, áreas, mesas y asignaciones.
- Apertura y estados de pedidos en mesa o para llevar.
- Captura, modificadores, total y bloqueo de partidas enviadas.
- Comandas separadas y tablero de preparación.
- Actualización por WebSocket, reconexión automática y consulta REST después de cada aviso.
- Cobro dividido, historial auditable y cierre de pedidos pagados.
- Apertura, movimientos, arqueo y cierre de caja.
- Reporte diario por sucursal y fecha con exportación CSV e impresión/PDF.
- Respaldos descargables para el propietario y ruta API del mismo origen para CSRF/tablets.
- Compilación estática para HTTPS/LAN; `/api` y `/ws` usan el mismo origen en tablets.
- Corrección responsive de los controles de modificadores del catálogo.

Validación que debe ejecutarse antes de publicar la rama:

- `npm run format:check`
- `npm run build`
- `npm test -- --watch=false`

La guía funcional del respaldo y restauración está en `../PRUEBA-BLOQUE-11.md`.
El despliegue del servidor y la prueba con tablets están en `../PRUEBA-BLOQUE-12.md`.
