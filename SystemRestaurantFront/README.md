# RestaurantSystem Frontend

Frontend de RestaurantSystem desarrollado con Angular y PrimeNG. Consume el backend Spring Boot
mediante la ruta relativa `/api`.

## Requisitos

- Node.js 22.12 o superior dentro de la rama 22, o Node.js 24.
- npm 10 o superior.
- Backend RestaurantSystem ejecutándose en `http://localhost:3210`.

## Instalar dependencias

```powershell
npm ci
```

## Ejecutar en desarrollo

```powershell
npm start
```

La aplicación abre en `http://localhost:4200`. El archivo `proxy.conf.json` redirige `/api` hacia `http://localhost:3210`.

## Compilar

```powershell
npm run build
```

El resultado queda en:

```text
dist/restaurant-system-front/browser
```

## Estructura recomendada

- `src/app/core`: servicios globales, modelos, interceptores y guardias.
- `src/app/features`: módulos funcionales como usuarios, mesas, pedidos, cocina e inventario.
- `src/environments/environment.ts`: configuración general del frontend.

El frontend nunca debe conectarse directamente a PostgreSQL. Toda operación con la base de datos debe pasar por el backend Spring Boot.

## Funciones disponibles

- Inicio con comprobación de la API y PostgreSQL.
- Configuración del restaurante.
- Administración de categorías.
- Administración de productos.
- Disponibilidad temporal y activación de productos.
- Destino de comanda para cocina, barra o productos sin comanda.

Rutas:

```text
/
/catalogo
/configuracion
```
