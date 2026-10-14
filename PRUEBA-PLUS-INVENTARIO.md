# Essential Plus · primer avance: inventario

## Cómo separar las ediciones en Git

Usa **ramas**, no carpetas copiadas para continuar el desarrollo. El historial de cambios queda
compartido y podrás incorporar una corrección de la edición base en Plus cuando haga falta.

1. Comprueba que la edición base de restaurante único **y la corrección de
   `BranchServiceTest.java`** quedaron probadas y confirmadas en `desarrollo`.
2. Desde tu proyecto local, crea la rama una sola vez:

   ```powershell
   git switch desarrollo
   git status
   git pull origin desarrollo
   git switch -c edicion/plus
   ```

3. Copia el contenido de este ZIP sobre el proyecto, conserva
   `SystemRestaurantBack\secret.yml` y elimina los tres archivos de asignación obsoletos si
   permanecieron por una copia anterior:

   ```powershell
   Remove-Item 'SystemRestaurantBack\src\main\java\com\rest\restaurantsystem\structure\BranchAssignmentRequest.java' -ErrorAction SilentlyContinue
   Remove-Item 'SystemRestaurantBack\src\main\java\com\rest\restaurantsystem\structure\BranchUserAssignment.java' -ErrorAction SilentlyContinue
   Remove-Item 'SystemRestaurantBack\src\main\java\com\rest\restaurantsystem\structure\BranchUserAssignmentRepository.java' -ErrorAction SilentlyContinue
   ```

Si `git status` muestra cambios sin confirmar antes de `git pull` o `git switch`, termina o
guarda esos cambios primero. **No crees `edicion/plus` desde el commit viejo que todavía no
incluya el restaurante único.** La edición base se conserva en `desarrollo`; Plus avanza en
`edicion/plus`. Cuando llegue Pro, podrá partir de una versión validada de Plus.

## Instalación y datos

Haz un respaldo antes de actualizar una instalación existente. Utiliza una base de prueba para
la primera ejecución del módulo. Flyway aplicará `V11__create_local_inventory.sql` una sola vez;
crea dos tablas nuevas y no modifica ventas, pedidos, caja ni usuarios. La base debe tener ya
aplicada `V10` o tener como máximo un restaurante para que `V10` pueda aplicarse antes de `V11`.

El ZIP contiene el proyecto completo. No incluye `.git`, `secret.yml`, `node_modules`, `dist` ni
`target`. Copiar un ZIP encima de una carpeta existente **no elimina** archivos obsoletos.

## Qué probar

1. Inicia backend y frontend. Entra como propietario o administrador; debe aparecer
   **Inventario** en la navegación. Un operador no puede abrir la ruta ni llamar a la API.
2. Crea `Harina` en kg con mínimo `1`. Comienza en `0` y aparece en nivel mínimo.
3. Selecciona el insumo y registra una **entrada** de `2.500` kg con motivo. Queda en `2.500`.
4. Registra una **merma** de `0.250` kg. Debe quedar `2.250`, con motivo y usuario en el historial.
5. Intenta retirar `3` kg: debe avisar que no hay existencias y conservar `2.250`.
6. Registra un **conteo físico** de `1.250` kg: el historial guarda una diferencia de `-1.000`.
7. Prueba dos tablets con el mismo insumo: al guardar desde la primera, un movimiento basado
   en la existencia anterior debe devolver conflicto en la segunda. Usa **Actualizar** y repite.
8. Crea `Tortillas` en piezas: intenta registrar `0.5`; debe rechazarse. Registra `10` piezas.
9. Un insumo con existencias no puede desactivarse ni cambiar de unidad. Al dejarlo en cero,
   puede desactivarse. Si ya tiene movimientos, su unidad histórica sigue protegida.

El historial muestra los **últimos 50 movimientos por insumo**. El módulo aún no descuenta
ingredientes al vender productos: eso requiere el siguiente módulo de recetas y su regla de
consumo. Las mermas, salidas y conteos se registran manualmente.

## Verificaciones

Con JDK 21, desde `SystemRestaurantBack`:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Desde `SystemRestaurantFront`:

```powershell
npm ci
npm run build
npm test -- --watch=false
npm start
```

Esta entrega se compiló y probó en Angular. Los tests Java del inventario están incluidos;
el entorno donde se preparó el ZIP no cuenta con JDK 21 ni con las dependencias Maven necesarias
para ejecutarlos. Ejecuta `clean test` en tu equipo antes de publicar la rama.

## Instalaciones independientes

`edicion/plus` es una rama del **código**. Cada restaurante que compre Plus tendrá su propia
instalación, base PostgreSQL, propietarios, catálogo, insumos, sesiones y respaldos. Crear una
instalación para otro restaurante no significa duplicar la base con ventas y usuarios del primero.
