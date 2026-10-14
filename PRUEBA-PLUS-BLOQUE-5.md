# Essential Plus · Bloque 5: reporte, alertas y conteo físico

Base: rama `plus` publicada, commit `b779686` (bloque 4). Esta entrega es el **bloque 5 de 5** de
Plus. Cada instalación sigue correspondiendo a un solo restaurante con su propia base de datos.

## Preparación

1. Haz un respaldo de PostgreSQL y detén Spring Boot y Angular.
2. Conserva `SystemRestaurantBack/secret.yml` al reemplazar los archivos del proyecto.
3. No borres datos ni ejecutes SQL manualmente. Flyway aplicará **V15** al arrancar.
4. Con JDK 21, desde `SystemRestaurantBack`:

   ```powershell
   .\mvnw.cmd clean test
   .\mvnw.cmd spring-boot:run
   ```

5. Desde `SystemRestaurantFront`:

   ```powershell
   npm ci
   npm run build
   npm test -- --watch=false
   npm start
   ```

6. Inicia sesión como propietario o administrador. Abre **Reporte y conteo** desde la barra o el
   botón de **Inventario**.

## 1. Alertas y reporte

1. Crea tres insumos activos: uno sin existencias, otro con un mínimo mayor que su existencia y
   otro por encima del mínimo. Deja al menos uno sin costo de referencia.
2. Comprueba las tarjetas **Agotados**, **En o bajo el mínimo** y **Sin costo**. Activa el filtro
   de alertas y confirma que solo muestre los que requieren atención.
3. Desde **Inventario**, registra una compra, una merma y una salida manual. En el reporte elige
   el día de esos movimientos y pulsa **Consultar**; cada cantidad debe verse en su columna.
4. Crea una receta y envía una comanda; debe aparecer consumo. Si la cancelas antes de comenzar,
   debe aparecer la reversa. El costo de comanda corresponde al costo congelado en el momento del
   envío, aunque cambies después el costo de referencia.
5. Descarga el CSV. Contiene **todas las filas de insumos** del reporte, incluso las que estén
   fuera del filtro visual. Incluye totales de movimientos por insumo, no movimientos individuales.
6. La existencia y el valor son los **actuales al generar**, mientras que los movimientos son
   del periodo seleccionado. Si hay stock sin costo, el total de valor es explícitamente parcial.

## 2. Conteo físico con diferencias

1. Detén momentáneamente el despacho de comandas y los movimientos manuales. Pulsa **Iniciar
   conteo**. El sistema obtiene una nueva fotografía de todos los insumos activos.
2. Escribe la cantidad físicamente contada de **cada insumo**, incluidos ceros. Conserva uno sin
   diferencia y modifica otro. Escribe un motivo de al menos cinco caracteres.
3. Pulsa **Conciliar y guardar** una sola vez. El historial debe mostrar el número de conteo,
   responsable, fecha, esperado, contado, diferencia, costo registrado y número de movimiento.
4. Vuelve a **Inventario**: la existencia del insumo modificado debe coincidir con la contada.
   En su historial debe haber un movimiento de ajuste vinculado por motivo al número de conteo.
   El insumo sin diferencia no debe generar un movimiento innecesario.
5. Descarga el CSV del conteo. La valoración de diferencias sin costo se señala como parcial.

## 3. Concurrencia e integridad

1. Abre **Reporte y conteo** en una tablet e inicia un conteo. Abre **Inventario** en otro
   navegador y registra una compra o envía una comanda que consuma uno de esos insumos.
2. Completa el conteo inicial y pulsa **Conciliar y guardar**. Debe mostrar un conflicto; ninguna
   de las cantidades de ese conteo debe aplicarse. Pulsa **Descartar borrador** e inicia uno nuevo.
3. Confirma que el conteo nuevo guarde correctamente. Repetir por error la misma solicitud con
   el mismo `operationId` y exactamente los mismos datos devuelve el conteo original sin duplicar
   ajustes. Un `operationId` reutilizado con otros datos devuelve conflicto.
4. Comprueba que un usuario operador no pueda abrir `/inventario/reporte` ni utilizar la API
   `/api/v1/inventory/**`; propietario y administrador sí pueden.

## Resultado y límites

- `V15` solo agrega tablas de conteos: no modifica existencias anteriores ni reescribe movimientos.
- Los ajustes de conteo actualizan existencias y el libro en una única transacción.
- La fotografía del conteo vive en la pantalla hasta confirmar; si recargas la página, debes
  iniciar uno nuevo. Los conteos **confirmados** permanecen en base de datos.
- El reporte consulta como máximo 31 días por petición. El historial de pantalla muestra los
  últimos 20 conteos; los documentos anteriores siguen almacenados y disponibles por ID en la API.
- El costo de referencia no incluye mano de obra, renta, impuestos, ni cambia automáticamente
  al registrar compras. Los importes conocidos pueden ser parciales si faltan costos.

Antes de ofrecer Plus comercialmente, comprueba el flujo completo con tablets, el cierre de caja,
una restauración de base en entorno separado y la operación sin internet en la red local.
