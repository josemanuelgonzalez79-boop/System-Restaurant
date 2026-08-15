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

Validación que debe ejecutarse antes de publicar la rama:

- `npm run format:check`
- `npm run build`
- `npm test -- --watch=false`

La guía funcional completa está en `../PRUEBA-BLOQUE-6.md`.
