# Prueba de instalación para un solo restaurante

## Corrección al copiar sobre `desarrollo`

La primera versión del ZIP omitía `BranchServiceTest.java`. Al copiar un ZIP sobre un proyecto
existente, Windows conserva los archivos que no aparecen en el ZIP: por eso el test anterior
seguía invocando `create`, `changeActive` y `countByActiveTrue`, retirados en esta edición.
Este paquete ya incluye el test actualizado y lo sobrescribe. Después de copiarlo, desde la raíz
del proyecto en PowerShell elimina estos tres archivos de la lógica antigua de asignaciones:

```powershell
Remove-Item 'SystemRestaurantBack\src\main\java\com\rest\restaurantsystem\structure\BranchAssignmentRequest.java' -ErrorAction SilentlyContinue
Remove-Item 'SystemRestaurantBack\src\main\java\com\rest\restaurantsystem\structure\BranchUserAssignment.java' -ErrorAction SilentlyContinue
Remove-Item 'SystemRestaurantBack\src\main\java\com\rest\restaurantsystem\structure\BranchUserAssignmentRepository.java' -ErrorAction SilentlyContinue
```

`git status` debe mostrar esos tres archivos como eliminados y el test como modificado. No
elimines `Branch.java`, `BranchRepository.java` ni la tabla `branches`: el identificador interno
sigue vinculando pedidos, caja e informes del restaurante local.

Esta entrega parte del commit `1692893` de `desarrollo`. No cambia los datos de catálogo, usuarios,
pedidos, pagos ni caja. El identificador interno del restaurante sigue existiendo para que los
registros anteriores conserven sus relaciones.
La tabla histórica de asignaciones permanece en PostgreSQL para evitar una eliminación de datos,
pero la aplicación ya no la consulta ni permite editarla.

## Antes de actualizar

1. Descarga un respaldo completo desde **Respaldos** y conserva otra copia fuera del servidor.
2. Detén el backend y el frontend. Conserva `SystemRestaurantBack\secret.yml`.
3. Confirma cuántas sucursales existen en la base actual. Si registraste varias, **no instales
   todavía**: la migración `V10` detiene el arranque sin borrar ni combinar datos. Cada restaurante
   necesita su propia base; la separación debe planificarse y probarse sobre copias.
4. Sustituye el proyecto, vuelve a colocar `secret.yml` e inicia el backend con JDK 21. Flyway
   aplicará `V10` automáticamente. No ejecutes el SQL manualmente.

## Qué comprobar

- **Estructura** muestra solo áreas y mesas. Ya no ofrece crear, editar o desactivar sucursales,
  ni asignar usuarios por sucursal.
- Crea un usuario activo. Al iniciar sesión puede entrar a Pedidos y Preparación según su rol,
  sin una asignación adicional. Un usuario desactivado no conserva acceso operativo.
- Pedidos, Preparación, Caja y Reporte ya no muestran un selector de sucursal.
- En Configuración cambia el nombre y la zona horaria; verifica que el nombre aparezca en Caja
  y que el reporte use la zona horaria nueva.
- Abre caja, crea un pedido, envía una comanda, cobra y consulta el reporte. Los datos históricos
  de la misma instalación deben seguir disponibles.
- Las rutas `POST /api/v1/branches`, `PUT /api/v1/branches/{id}`,
  `PATCH /api/v1/branches/{id}/active` y `/api/v1/branches/{id}/assignments` ya no están
  disponibles. La base de datos impide insertar una segunda fila en `branches` o desactivar la
  única existente.

## Otro restaurante

Instala otra copia del backend y frontend con **una base PostgreSQL nueva y vacía**. Prepara su
propio `secret.yml`, usuario propietario, catálogo, áreas, mesas, certificados LAN y rutina de
respaldos. No restaures en ella el respaldo del primer restaurante: eso copiaría sus ventas y
usuarios. Si ambas instalaciones viven en el mismo equipo, necesitan nombres de base, puertos,
servicios y dominios/hosts distintos.

## Comandos de validación

En `SystemRestaurantBack` (JDK 21):

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

En `SystemRestaurantFront`:

```powershell
npm ci
npm run build
npm test -- --watch=false
npm start
```

El frontend fue compilado y sus pruebas se ejecutaron en la preparación de esta entrega. La
suite Java requiere JDK 21 y acceso a las dependencias de Maven Central en tu equipo.
