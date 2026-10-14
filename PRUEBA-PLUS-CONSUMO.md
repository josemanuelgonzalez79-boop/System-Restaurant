# Essential Plus · bloque 3 de 5: consumo por comanda

## Qué incorpora

- Al pulsar **Enviar comanda**, el servidor toma la receta del producto y suma las recetas de
  las opciones elegidas. Multiplica por la cantidad de la partida, comprueba el stock total y
  descuenta cada insumo en la misma transacción que crea las comandas.
- Cada salida guarda usuario, existencia resultante, folio de pedido y partida. No se repite al
  reenviar la misma solicitud. Si una sola receta o existencia no alcanza, no sale ninguna de las
  comandas del envío y no se modifica el inventario.
- Cancelar una partida **Pendiente** devuelve las cantidades originales, aunque la receta haya
  cambiado después. La devolución queda ligada al movimiento de consumo inicial.
- Cancelar **En preparación** o **Lista** conserva el consumo: el insumo pudo haberse usado. Si
  se recuperó físicamente, el administrador hace un ajuste verificado desde Inventario.
- Toda cancelación requiere motivo (5 a 180 caracteres), guarda al responsable y retira esa
  partida del total a cobrar. Si existen cobros activos, primero hay que anularlos.
- Las tablets abiertas en Inventario actualizan la existencia por WebSocket y conservan consulta
  de respaldo cada minuto.

## Actualización de tu instalación

1. Detén backend y frontend. Descarga un respaldo de la base antes de aplicar un nuevo bloque.
2. En el proyecto local, confirma la rama `plus` y tus cambios anteriores:

   ```powershell
   git switch plus
   git status
   git pull origin plus
   ```

3. Conserva `SystemRestaurantBack\secret.yml`. Copia **el contenido** de la carpeta del ZIP
   sobre el proyecto en `plus`. El ZIP no incluye `.git`, `secret.yml`, dependencias ni compilados.
4. Con JDK 21, ejecuta en `SystemRestaurantBack`:

   ```powershell
   .\mvnw.cmd clean test
   .\mvnw.cmd spring-boot:run
   ```

5. En `SystemRestaurantFront`:

   ```powershell
   npm ci
   npm run build
   npm test -- --watch=false
   npm start
   ```

Flyway ejecuta `V13__consume_recipes_on_dispatch.sql` **una sola vez**. No ejecutes V13
manualmente ni borres la base. Los pedidos y movimientos anteriores permanecen; las comandas
enviadas antes de V13 no reciben un consumo retroactivo.

## Secuencia de prueba

1. Crea tres insumos en Inventario y registra existencias: `Pan` (10 piezas), `Carne` (2.000 kg),
   `Queso` (1.000 kg). Configura la receta de `Hamburguesa` con 1 pan y 0.150 kg de carne por
   unidad, y la opción `Queso extra` con 0.020 kg de queso.
2. Confirma que Hamburguesa tenga ruta **Cocina**. Abre un pedido, agrega **2 hamburguesas** con
   Queso extra y pulsa **Enviar comanda**. Deben quedar 8 panes, 1.700 kg de carne y 0.960 kg
   de queso. En Inventario cada insumo debe mostrar un movimiento **Consumo por comanda** con
   el folio y la partida.
3. Sin iniciar la preparación, pulsa **Cancelar** en esa partida. Escribe el motivo y confirma.
   Las existencias deben volver a 10 panes, 2.000 kg de carne y 1.000 kg de queso. El historial
   registra **Devolución por cancelación** y la cuenta muestra la partida cancelada con $0.
4. En otro pedido, envía una hamburguesa y márcala **En preparación** antes de cancelarla. La
   cuenta dejará de cobrarla, pero el inventario **no** volverá a aumentar. Consulta motivo y
   usuario desde el historial de Preparación.
5. Con inventario insuficiente, intenta enviar dos hamburguesas: debe indicar qué insumo falta.
   No debe aparecer una nueva comanda ni un descuento parcial.
6. Prueba dos tablets con el mismo pedido; el segundo envío con versión antigua debe fallar sin
   duplicar consumos. Actualiza el pedido antes de volver a intentar.
7. Después de enviar una partida, edita su receta desde otra sesión. Si cancelas la partida aún
   pendiente, debe devolver **la cantidad que se descontó al enviarla**, no la receta nueva.
8. Registra un cobro parcial en un pedido con una partida activa. Si intentas cancelarla, debe
   pedirte anular primero los cobros. Al anularlos, la cancelación puede actualizar la cuenta.
9. Abre Inventario en otra tablet: el despacho y la cancelación deben actualizar las existencias
   sin recargar. Si no hay WebSocket, usa **Actualizar** y revisa la consulta de respaldo.

## Límites explícitos

- Una opción extra **sin receta** no agrega insumos. Configura la receta de los extras que deban
  consumir inventario.
- Un producto con ruta **Sin preparación** (`NONE`) no envía comanda y no genera consumo
  automático en este bloque. Si administra existencias, usa la ruta Cocina o Servicio.
- No se hace una devolución automática tras iniciar preparación; la recuperación física se
  registra manualmente con motivo.
- Los importes de cancelaciones anteriores a esta migración no se recalculan retroactivamente,
  para no alterar cuentas y reportes históricos.

## Verificación pendiente en tu equipo

En el entorno donde se preparó el ZIP solo hay un runtime Java 17 y Maven Central no respondió;
el proyecto requiere JDK 21 para compilar y ejecutar sus pruebas Maven. La suite Java nueva cubre consumo combinado,
existencia insuficiente, devolución del snapshot y exclusión de partidas canceladas de la cuenta.
Ejecuta `mvnw clean test` y prueba la migración con una copia de tu base antes de subir a Git.
