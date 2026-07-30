# Avance de RestaurantSystem

## Objetivo

Construir un sistema local y configurable para restaurantes, accesible desde computadoras,
tablets y celulares conectados a la red interna. La misma base de código se utilizará para todos
los clientes y cada instalación tendrá su propia configuración y base de datos.

## Bloque 1 completado

El primer flujo vertical conecta Angular, Spring Boot y PostgreSQL:

1. Flyway crea el esquema inicial.
2. La configuración identifica al restaurante.
3. El administrador registra categorías.
4. El administrador registra productos.
5. Cada producto tiene precio, estado, disponibilidad y destino de comanda.
6. Angular permite crear, editar, activar y desactivar los registros.

### Reglas incorporadas

- Los importes utilizan `NUMERIC(12,2)` y `BigDecimal`.
- Los nombres de categorías son únicos sin distinguir mayúsculas.
- Los SKU son opcionales, pero son únicos cuando se proporcionan.
- Un producto agotado continúa activo en el catálogo.
- Un producto inactivo queda fuera de la operación.
- Los registros utilizan control de versión para detectar modificaciones simultáneas.
- Los datos no se eliminan físicamente desde la interfaz.
- Hibernate valida el esquema y Flyway controla sus cambios.

## Orden de desarrollo

| Bloque | Alcance | Estado |
| --- | --- | --- |
| 1 | Configuración, categorías y productos | Terminado |
| 2 | Usuarios, contraseñas, roles y permisos | Siguiente |
| 3 | Áreas y mesas | Pendiente |
| 4 | Apertura de cuenta y captura de pedidos | Pendiente |
| 5 | Partidas, modificadores y observaciones | Pendiente |
| 6 | Pantalla de cocina y estados por partida | Pendiente |
| 7 | Notificaciones en tiempo real | Pendiente |
| 8 | Cobro y formas de pago | Pendiente |
| 9 | Apertura y cierre básico de caja | Pendiente |
| 10 | Reporte diario | Pendiente |
| 11 | Respaldos y restauración | Pendiente |
| 12 | Instalación en la red local | Pendiente |

## Decisiones vigentes

- Una instalación y base de datos por sucursal durante la primera versión.
- Angular y Spring Boot se desarrollan mediante funciones completas, no como dos proyectos
  aislados.
- REST realiza las operaciones y WebSocket se agregará para avisos en tiempo real.
- PostgreSQL siempre será la fuente de verdad.
- No se incluye CFDI, SAT, PLC, automatización industrial, nube pública ni WhatsApp estándar.
- Inventario, recetas, clientes frecuentes y reservaciones quedan fuera de la versión Esencial
  inicial.

## Próximo bloque

Agregar Spring Security y el primer flujo de autenticación:

- Usuario administrador inicial.
- Contraseñas cifradas.
- Roles `ADMIN`, `WAITER`, `KITCHEN` y `CASHIER`.
- Inicio y cierre de sesión mediante cookie.
- Protección CSRF.
- Guardias y pantalla de acceso en Angular.
- Restricción del catálogo y la configuración al rol administrador.
