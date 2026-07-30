# RestaurantSystem Back

API REST local para el sistema de restaurante.

## Tecnologías

- Java 21
- Spring Boot 4.1
- Spring Web MVC
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

Después actualiza la URL, el usuario y la contraseña en `secret.yml`. Este archivo está excluido
por `.gitignore`.

La base debe existir previamente:

```sql
CREATE DATABASE restaurant_system;
```

Flyway crea y versiona las tablas. Hibernate se encuentra en modo `validate`; no debe utilizarse
`ddl-auto: update` para evolucionar el esquema.

## Ejecutar

```powershell
.\mvnw.cmd clean spring-boot:run
```

La API inicia en `http://localhost:3210`.

## Endpoints del bloque 1

| Método | Ruta | Uso |
| --- | --- | --- |
| `GET` | `/api/health` | Comprobar API y PostgreSQL |
| `GET` | `/api/v1/settings` | Consultar configuración |
| `PUT` | `/api/v1/settings` | Actualizar configuración |
| `GET` | `/api/v1/categories` | Listar categorías |
| `POST` | `/api/v1/categories` | Crear categoría |
| `PUT` | `/api/v1/categories/{id}` | Editar categoría |
| `PATCH` | `/api/v1/categories/{id}/active` | Activar o desactivar |
| `GET` | `/api/v1/products` | Listar productos |
| `POST` | `/api/v1/products` | Crear producto |
| `PUT` | `/api/v1/products/{id}` | Editar producto |
| `PATCH` | `/api/v1/products/{id}/active` | Activar o desactivar |
| `PATCH` | `/api/v1/products/{id}/availability` | Disponible o agotado |

## Migraciones

La migración inicial está en:

```text
src/main/resources/db/migration/V1__create_restaurant_settings_and_catalog.sql
```

Cada cambio futuro de estructura debe agregarse en una migración nueva. No se deben editar
migraciones que ya fueron ejecutadas en una instalación.
