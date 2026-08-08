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
- `src/app/features`: acceso, usuarios, configuración, catálogo y estructura operativa.
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

Rutas:

```text
/login
/setup
/
/catalogo
/estructura
/configuracion
/usuarios
```
