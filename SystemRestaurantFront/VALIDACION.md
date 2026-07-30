# Validación del bloque 1

Plantilla preparada para RestaurantSystem con:

- Angular 21.2.x.
- PrimeNG 21.1.9.
- TypeScript 5.9.x.
- Node.js 22.12+ o 24.
- Proxy de desarrollo hacia Spring Boot en el puerto 3210.
- Endpoint de comprobación `GET /api/health`.
- Configuración del restaurante en `/configuracion`.
- Categorías y productos en `/catalogo`.

Validación realizada:

- `npm run build`
- `npm test -- --watch=false`
- 4 pruebas superadas.
