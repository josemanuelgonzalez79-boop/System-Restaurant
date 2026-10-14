# Essential Plus · bloque 2: recetas de productos y extras

## Rama y alcance

Trabaja en la rama existente **`plus`**. Esta entrega parte del commit `abb5416` con el bloque 1
de inventario y contiene el proyecto completo. Conserva un solo restaurante por instalación y
una base de datos independiente para cada restaurante.

Plus tendrá **cinco bloques**: 1) inventario y movimientos, 2) recetas por producto y opción,
3) consumo al enviar y reversas auditables, 4) costos y márgenes, 5) reportes y conciliación de
inventario. Esta entrega implementa el segundo bloque.

## Actualizar tu copia

1. Detén el backend y Angular. Guarda un respaldo de la base antes de probar la migración.
2. Comprueba que estás en `plus` y que tus cambios anteriores están confirmados:

   ```powershell
   git switch plus
   git status
   git pull origin plus
   ```

3. Conserva `SystemRestaurantBack\secret.yml`. Copia **el contenido** de la carpeta del ZIP
   sobre tu proyecto en `plus`; no copies la carpeta contenedora como proyecto separado.
4. Si aún existen estos archivos heredados de asignaciones entre sucursales, elimínalos. Un ZIP
   copiado sobre una carpeta existente no puede borrar archivos antiguos:

   ```powershell
   Remove-Item 'SystemRestaurantBack\src\main\java\com\rest\restaurantsystem\structure\BranchAssignmentRequest.java' -ErrorAction SilentlyContinue
   Remove-Item 'SystemRestaurantBack\src\main\java\com\rest\restaurantsystem\structure\BranchUserAssignment.java' -ErrorAction SilentlyContinue
   Remove-Item 'SystemRestaurantBack\src\main\java\com\rest\restaurantsystem\structure\BranchUserAssignmentRepository.java' -ErrorAction SilentlyContinue
   ```

5. Desde `SystemRestaurantBack`, con JDK 21:

   ```powershell
   .\mvnw.cmd clean test
   .\mvnw.cmd spring-boot:run
   ```

6. Desde `SystemRestaurantFront`:

   ```powershell
   npm ci
   npm run build
   npm test -- --watch=false
   npm start
   ```

Flyway ejecuta `V12__create_product_recipes.sql` al iniciar el backend. No ejecutes la migración
manualmente, no borres la base y no copies `secret.yml` desde ningún ZIP. `V12` agrega las tablas
de recetas y no modifica las existencias, los movimientos ni las ventas existentes.

## Prueba funcional

1. Entra como propietario o administrador. Confirma que aparece **Recetas** junto a Inventario.
   Un operador no debe poder abrir `/recetas` ni acceder a `/api/v1/recipes/**`.
2. En Inventario crea `Pan` en piezas y `Carne` en kg. Registra existencias, por ejemplo 10 piezas
   y 2.000 kg. En Catálogo crea `Hamburguesa` y un grupo con una opción `Queso extra`.
3. En Recetas elige Hamburguesa; agrega `Pan = 1 pieza` y `Carne = 0.150 kg` por unidad. Guarda,
   cambia de producto y vuelve: cantidades, editor y fecha deben permanecer.
4. Elige la opción `Queso extra` y asígnale el insumo queso, por ejemplo `0.020 kg`. Cada opción
   conserva su propia receta separada de la del producto principal.
5. Intenta repetir el mismo insumo o registrar `0.5` piezas: la pantalla debe explicarlo; la API
   también rechaza duplicados y fracciones de piezas. Las cantidades deben ser positivas y de
   hasta tres decimales.
6. Desde Inventario intenta desactivar `Pan` o cambiar su unidad mientras siga en la receta:
   debe rechazarse. Retíralo de la receta y guarda antes de repetir el cambio. Un insumo que ya
   tenga movimientos conserva además la protección histórica de unidad del bloque 1.
7. Abre la misma receta en dos navegadores. Modifícala y guárdala en el primero; guarda una
   versión antigua desde el segundo: debe recibir conflicto. Actualiza la página y vuelve a
   revisar las cantidades antes de editar.
8. Registra un pedido con Hamburguesa y envíalo a preparación. Confirma que el inventario **no
   disminuye** todavía: el consumo automático corresponde al bloque 3.

## Validación de esta entrega

La compilación de producción Angular y sus pruebas se ejecutaron en el entorno de preparación.
Los archivos Java se comprobaron sintácticamente. En este entorno solo está disponible Java 17,
por lo que la ejecución de `mvnw clean test` con el JDK 21 del proyecto queda pendiente en tu
máquina. Hay pruebas Java nuevas de duplicados, piezas enteras y versiones de recetas.
