# Essential Back

API REST local y genérica para administrar negocios con Essential.

## Tecnologías

- Java 21
- Spring Boot 4.1
- Spring Web MVC, WebSocket/STOMP y Spring Security
- Spring Data JPA
- Jakarta Validation
- PostgreSQL
- Flyway

## Configuración local

La configuración compartida y sin secretos está en:

```text
src/main/resources/application.yml
```

Crea el archivo local de credenciales a partir del ejemplo:

```powershell
Copy-Item secret.example.yml secret.yml
```

Actualiza la URL, el usuario y la contraseña en `secret.yml`. El archivo está excluido por
`.gitignore` y no se incluye en los paquetes entregables.

La base debe existir previamente:

```sql
CREATE DATABASE restaurant_system;
```

Flyway crea y versiona las tablas. Hibernate usa `validate`; no debe utilizarse
`ddl-auto: update` para cambiar el esquema.

## Ejecutar

```powershell
.\mvnw.cmd clean spring-boot:run
```

La API inicia en `http://localhost:3211`.

## Acceso inicial

Con una base sin usuarios, Angular abre el asistente de configuración. Este llama a
`POST /api/v1/auth/setup` para registrar un único propietario. Después, el acceso normal usa
sesión HTTP y protección CSRF.

## Endpoints principales

| Método               | Ruta                                            | Acceso                                       |
| -------------------- | ----------------------------------------------- | -------------------------------------------- |
| `GET`                | `/api/health`                                   | Público                                      |
| `GET`                | `/api/v1/auth/csrf`                             | Público                                      |
| `GET`                | `/api/v1/auth/setup-status`                     | Público                                      |
| `POST`               | `/api/v1/auth/setup`                            | Solo antes del primer usuario                |
| `POST`               | `/api/v1/auth/login`                            | Público                                      |
| `POST`               | `/api/v1/auth/logout`                           | Usuario autenticado                          |
| `GET`                | `/api/v1/auth/me`                               | Usuario autenticado                          |
| `GET/POST/PUT/PATCH` | `/api/v1/users`                                 | Propietario                                  |
| `GET/PUT`            | `/api/v1/settings`                              | Propietario o administrador                  |
| `GET/POST/PUT/PATCH` | `/api/v1/categories`                            | Propietario o administrador                  |
| `GET/POST/PUT/PATCH` | `/api/v1/products`                              | Propietario o administrador                  |
| `GET`                | `/api/v1/branches`, `/areas`, `/service-points` | Usuario autenticado                          |
| `POST/PUT/PATCH`     | `/api/v1/areas`, `/api/v1/service-points`       | Propietario o administrador                  |
| `GET/POST/PATCH`     | `/api/v1/orders`                                | Usuario activo del restaurante               |
| `POST`               | `/api/v1/orders/{id}/dispatch`                  | Usuario activo del restaurante               |
| `GET/PATCH`          | `/api/v1/preparation`                           | Usuario activo del restaurante               |
| `GET/POST/PATCH`     | `/api/v1/orders/{id}/payments`                  | Usuario activo del restaurante               |
| `GET/POST/PATCH`     | `/api/v1/cash-registers`                        | Propietario, administrador, gerente o cajero |
| `GET/POST/PUT/PATCH` | `/api/v1/inventory/ingredients`                 | Propietario o administrador                  |
| `GET/POST`           | `/api/v1/inventory/ingredients/{id}/movements`  | Propietario o administrador                  |
| `GET/PUT`            | `/api/v1/recipes/products/{id}`                 | Propietario o administrador                  |
| `GET/PUT`            | `/api/v1/recipes/modifier-options/{id}`         | Propietario o administrador                  |
| WebSocket/STOMP      | `/ws`                                           | Usuario autenticado                          |

## Tiempo real

El endpoint `/ws` comparte la sesión HTTP de Essential. Los clientes se suscriben únicamente al
canal `/topic/branches/{branchId}` del restaurante local; solo los usuarios activos tienen acceso. Los avisos se
publican después de confirmar la transacción y hacen que Angular vuelva a consultar la API REST.

Para usar tablets mediante otra dirección de la LAN, agrega cada origen de Angular a
`app.cors.allowed-origins` dentro de `secret.yml`, por ejemplo
`http://192.168.1.50:4200`. No expongas `/ws`, la API ni PostgreSQL directamente a internet.

## Migraciones

```text
src/main/resources/db/migration/V1__create_restaurant_settings_and_catalog.sql
src/main/resources/db/migration/V2__generalize_business_and_add_users.sql
src/main/resources/db/migration/V3__create_operational_structure.sql
src/main/resources/db/migration/V4__create_restaurant_orders.sql
src/main/resources/db/migration/V5__create_order_items_and_modifiers.sql
src/main/resources/db/migration/V6__create_preparation_tickets.sql
src/main/resources/db/migration/V7__create_order_payments.sql
src/main/resources/db/migration/V8__create_cash_register_sessions.sql
src/main/resources/db/migration/V9__add_daily_report_indexes.sql
src/main/resources/db/migration/V10__one_local_restaurant.sql
src/main/resources/db/migration/V11__create_local_inventory.sql
src/main/resources/db/migration/V12__create_product_recipes.sql
src/main/resources/db/migration/V13__consume_recipes_on_dispatch.sql
src/main/resources/db/migration/V14__recipe_costs_and_margin_snapshots.sql
src/main/resources/db/migration/V15__inventory_counts.sql
src/main/resources/db/migration/V16__create_local_reservations.sql
```

`V2` conserva los datos existentes, generaliza la configuración y agrega usuarios. `V4` incorpora
los pedidos y la protección contra dos pedidos activos en la misma mesa. `V5` agrega las partidas,
modificadores y totales; `V6` agrega comandas, snapshots y estados de preparación. El Bloque 7 no
necesita una migración porque incorpora comunicación en tiempo real. `V7` agrega cobros, formas de
pago, referencias y auditoría de anulaciones. `V8` incorpora turnos de caja por sucursal,
movimientos manuales, arqueo de cierre y la relación entre cobros y caja. No edites una migración
que ya se ejecutó; cada cambio estructural debe ir en una migración nueva.

`V10` conserva el identificador interno y los datos históricos, limita la base a un restaurante
y evita desactivarlo. Si encuentra varias sucursales, se detiene antes de modificar los datos: cada
restaurante debe trasladarse a su propia instalación y base de datos.

`V11` agrega inventario local y movimientos auditables. `V12` relaciona recetas con productos y
opciones. `V13` enlaza los movimientos automáticos con partidas de comanda y guarda el motivo de
cancelación. El despacho descuenta los insumos en la misma transacción; si falta una receta o
existencia, la comanda no sale. Una cancelación aún pendiente devuelve el consumo original.

`V14` agrega costo de referencia auditado por insumo y costo congelado en nuevos movimientos
automáticos de comanda y reversa. La API `/api/v1/costs` calcula márgenes actuales de materia
prima para productos y extras; los costos no reescriben el historial anterior.

`V15` agrega el historial inmutable de conteos físicos por restaurante, sin alterar el stock al
migrar. La API `/api/v1/inventory/report?from=AAAA-MM-DD&to=AAAA-MM-DD` resume movimientos
del periodo y valora la existencia actual, señalando costos pendientes. La API
`/api/v1/inventory/counts` concilia todos los insumos activos con versiones, registra los ajustes
necesarios en el libro de movimientos y rechaza cambios simultáneos sin aplicar ajustes parciales.

`V16` agrega reservaciones y eventos auditables. Requiere la extensión PostgreSQL `btree_gist`
(se crea automáticamente si el usuario de la base tiene permiso). La exclusión GiST impide
reservaciones traslapadas por mesa; cancelar o registrar una ausencia libera el intervalo.
`/api/v1/reservations` administra la agenda en la zona horaria local y el pedido acepta un
`reservationId` opcional para sentar al cliente y vincular la cuenta en una sola transacción.

`V17` incorpora fichas locales de clientes y auditoría de sus cambios sin modificar pedidos ni
reservaciones existentes. El teléfono normalizado evita duplicados; las fichas se archivan sin
borrar el historial. `/api/v1/customers` solo está disponible para propietario, administrador,
gerente y cajero. Las reservas y pedidos admiten `customerId` opcional; el pedido originado en
una reserva hereda su cliente. Los operadores no pueden adjuntar identificadores de clientes o
reservaciones mediante la API de pedidos. Las notas alimentarias se muestran únicamente al
personal autorizado, y no se envían a cocina automáticamente.

`V18` agrega promociones y descuentos auditables de cuenta. `/api/v1/promotions` permite a
propietario, administrador y gerente crear o editar ofertas vigentes por fecha del restaurante;
caja puede consultar las disponibles. `/api/v1/orders/{orderId}/discounts` aplica una promoción
o un descuento manual autorizado antes del primer cobro. Solo se admite uno activo; importe,
nombre y condiciones quedan congelados en el historial, junto con anulaciones y motivos. Los
cobros usan el saldo neto y la modificación concurrente de la cuenta se rechaza por versión.
