# Prueba del Bloque 3

Esta entrega agrega la estructura operativa de Essential sin modificar los datos existentes de
configuración, catálogo o usuarios.

## 1. Backend

Conserva tu archivo local `SystemRestaurantBack/secret.yml` y ejecuta:

```powershell
cd SystemRestaurantBack
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Durante el primer arranque, Flyway debe aplicar:

```text
V3__create_operational_structure.sql
```

La migración crea una sucursal `PRINCIPAL` usando el nombre, dirección, teléfono y zona horaria
actuales del negocio. Los usuarios activos existentes quedan asignados inicialmente a esa
sucursal.

## 2. Frontend

En otra consola:

```powershell
cd SystemRestaurantFront
npm install
npm start
```

Abre `http://localhost:4200` e inicia sesión como propietario o administrador. En la barra superior
debe aparecer la opción **Estructura**.

## 3. Recorrido recomendado

1. Confirma que aparezca la sucursal principal.
2. Crea un área, por ejemplo `Salón` de tipo `Servicio o atención`.
3. Crea un punto, por ejemplo `Mesa 1` con clave `MESA-01`.
4. Edita los tres niveles y comprueba que el orden se actualice.
5. Desactiva y vuelve a activar un punto.
6. Asigna usuarios a la sucursal y conserva al menos un propietario.
7. Comprueba que no sea posible desactivar la última sucursal activa.

## 4. Si aparece un error

No edites ni vuelvas a ejecutar manualmente la migración. Copia desde la consola el primer bloque
que empiece con `Caused by` y compártelo sin incluir credenciales.
