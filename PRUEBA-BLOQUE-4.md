# Prueba del Bloque 4: mesas y apertura de pedidos

Este bloque agrega la migración `V4__create_restaurant_orders.sql`. No ejecutes el archivo SQL
manualmente: Spring Boot y Flyway lo aplican al iniciar.

## 1. Actualizar y compilar

1. Detén Angular y Spring Boot.
2. Conserva tu archivo `SystemRestaurantBack/secret.yml`.
3. Reemplaza el proyecto con esta versión y vuelve a colocar `secret.yml`.
4. Desde `SystemRestaurantBack` ejecuta:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

5. Desde `SystemRestaurantFront` ejecuta:

```powershell
npm ci
npm test -- --watch=false
npm start
```

## 2. Preparar la sucursal

Desde una cuenta propietaria o administradora:

1. Abre **Estructura**.
2. Confirma que exista una sucursal activa.
3. Crea un área de tipo **Servicio o atención**, por ejemplo `Comedor`.
4. Crea al menos dos puntos de tipo **Mesa**.
5. En asignaciones, confirma que el propietario y el usuario que atenderá estén asignados a la
   sucursal.

## 3. Probar el mapa de mesas

1. Abre **Pedidos**.
2. Selecciona la sucursal y el área.
3. Confirma que las mesas aparezcan como **Libre**.
4. Pulsa **Abrir pedido** en una mesa.
5. Selecciona responsable, comensales y agrega una nota opcional.
6. Confirma que la mesa cambie a **Abierto** y muestre un folio como `PED-000001`.
7. Pulsa **Iniciar atención** y confirma el estado **En atención**.
8. Pulsa **Completar** y confirma que la mesa quede libre nuevamente.

## 4. Probar cancelación y pedido para llevar

1. Abre nuevamente una mesa y pulsa el botón rojo de cancelar.
2. Confirma la cancelación y verifica que la mesa se libere.
3. Pulsa **Pedido para llevar**.
4. Escribe una referencia de cliente y abre el pedido.
5. Confirma que aparezca en la sección **Pedidos para llevar** sin ocupar una mesa.

## 5. Probar dos dispositivos

1. Abre Essential en dos navegadores o tablets con usuarios asignados a la misma sucursal.
2. En ambos dispositivos intenta abrir la misma mesa casi al mismo tiempo.
3. Solo uno debe crear el pedido; el segundo debe recibir un aviso de que la mesa ya está ocupada.
4. Pulsa **Actualizar** en el segundo dispositivo para ver el estado correcto.

## Resultado esperado

- Cada usuario ve únicamente sus sucursales asignadas.
- Una mesa no admite dos pedidos activos.
- Los pedidos para llevar no ocupan mesas.
- Los cambios completados o cancelados liberan la mesa.
- Una mesa, área o sucursal con pedidos abiertos no se puede desactivar ni mover.
- Flyway registra la migración V4 sin borrar categorías, usuarios ni estructura existente.

El Bloque 5 agregará productos, cantidades, modificadores y precios dentro de estos pedidos.
