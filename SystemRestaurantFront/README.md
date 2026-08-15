# Essential Frontend

Interfaz Angular y PrimeNG para Essential. Consume el backend Spring Boot mediante `/api`.

## Requisitos

- Node.js 22.12 o superior dentro de la rama 22, o Node.js 24.
- npm 10 o superior.
- Backend Essential ejecutándose en `http://localhost:3211`.

## Instalar y ejecutar

```powershell
npm ci
npm start
```

La aplicación abre en `http://localhost:4200`. `proxy.conf.json` redirige `/api` hacia el
backend local.

## Compilar

```powershell
npm run build
```

El resultado queda en:

```text
dist/restaurant-system-front/browser
```

## Estructura

- `src/app/core`: servicios globales, modelos, interceptores y guardias.
- `src/app/features`: acceso, usuarios, configuración, catálogo, estructura y pedidos.
- `src/environments/environment.ts`: configuración general.
- `public/essential-mark.png`: marca principal de Essential.

El frontend nunca se conecta directamente a PostgreSQL. Toda operación pasa por Spring Boot.

## Funciones disponibles

- Configuración guiada del primer propietario.
- Inicio, restauración y cierre de sesión.
- Administración de usuarios, roles y contraseñas.
- Configuración genérica del negocio.
- Administración de categorías y productos o servicios.
- Disponibilidad, activación y ruta operativa.
- Administración de sucursales, áreas y puntos de atención.
- Asignación de usuarios por sucursal.
- Mapa de mesas libres y ocupadas por área.
- Apertura de pedidos en el local o para llevar.
- Folio, responsable, comensales, notas y estados del pedido.
- Captura táctil con variantes, notas y total.
- Envío de comandas nuevas a cocina o servicio.
- Tablero de preparación por sucursal, ruta y estado de cada partida.

Rutas:

```text
/login
/setup
/
/pedidos
/preparacion
/catalogo
/estructura
/configuracion
/usuarios
```
