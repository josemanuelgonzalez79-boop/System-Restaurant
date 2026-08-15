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
| `POST/PUT/PATCH`     | `/api/v1/branches`, `/areas`, `/service-points` | Propietario o administrador                  |
| `GET/PUT`            | `/api/v1/branches/{id}/assignments`             | Propietario o administrador                  |
| `GET/POST/PATCH`     | `/api/v1/orders`                                | Usuario autenticado y asignado a la sucursal |
| `POST`               | `/api/v1/orders/{id}/dispatch`                  | Usuario autenticado y asignado a la sucursal |
| `GET/PATCH`          | `/api/v1/preparation`                           | Usuario autenticado y asignado a la sucursal |
| WebSocket/STOMP      | `/ws`                                            | Usuario autenticado                          |

## Tiempo real

El endpoint `/ws` comparte la sesión HTTP de Essential. Los clientes se suscriben únicamente al
canal `/topic/branches/{branchId}` de una sucursal a la que el usuario esté asignado. Los avisos se
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
```

`V2` conserva los datos existentes, generaliza la configuración y agrega usuarios. `V4` incorpora
los pedidos y la protección contra dos pedidos activos en la misma mesa. `V5` agrega las partidas,
modificadores y totales; `V6` agrega comandas, snapshots y estados de preparación. El Bloque 7 no
necesita una migración porque incorpora comunicación en tiempo real. No edites una
migración que ya se ejecutó; cada cambio estructural debe ir en una migración nueva.
