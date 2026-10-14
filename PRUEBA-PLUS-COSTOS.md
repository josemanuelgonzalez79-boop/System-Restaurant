# Essential Plus · bloque 4 de 5: costos y márgenes

## Novedades

- En **Costos** se registra el costo de referencia por pieza, kilogramo o litro, con motivo,
  usuario, fecha, versión e historial de los últimos 20 cambios por insumo.
- Se calcula costo de cada receta y opción extra, utilidad bruta y margen sobre precio de venta.
  El producto se presenta **sin opciones**; cada extra muestra su impacto adicional.
- Si falta una receta o el costo de algún insumo, el margen queda **pendiente**: no se supone costo
  cero. Un costo registrado de $0 sí es un dato explícito y se considera conocido.
- Cada nueva comanda guarda costo unitario y costo total por insumo en el movimiento de consumo.
  Al cancelar antes de preparar, la reversa usa ese importe original, aunque el costo vigente
  haya cambiado. Comandas anteriores a V14 conservan su historia sin costo retroactivo.
- Los costos no cambian el precio de venta, los importes a cobrar ni el descuento físico de stock.

Los costos son una **estimación de materia prima**. No incluyen trabajo, renta, comisiones,
impuestos ni merma no registrada. El costo de referencia se introduce manualmente; registrar una
entrada de inventario no lo recalcula automáticamente.

## Instalación

1. Haz un respaldo de PostgreSQL y detén backend y frontend. Trabaja sobre la rama `plus` que ya
   probaste. No borres tu base ni ejecutes SQL manualmente.
2. Copia el contenido de la carpeta `Essential-Plus` del ZIP sobre el proyecto local. Conserva
   `SystemRestaurantBack\secret.yml` de tu máquina; no viene en la entrega.
3. Con **JDK 21**, desde `SystemRestaurantBack` ejecuta:

   ```powershell
   .\mvnw.cmd clean test
   .\mvnw.cmd spring-boot:run
   ```

4. En `SystemRestaurantFront` ejecuta:

   ```powershell
   npm ci
   npm run build
   npm test -- --watch=false
   npm start
   ```

Flyway aplicará **V14** automáticamente después de V13. Solo propietario y administrador pueden
ver y modificar costos. El restaurante sigue siendo una sola instalación local.

## Prueba sugerida

1. En Inventario registra `Pan` (pieza), `Carne` (kg) y `Queso` (kg). En Catálogo, configura
   `Hamburguesa` a **$80** y el extra `Queso extra` a **$10**. En Recetas pon 1 pan y 0.150 kg
   de carne para la hamburguesa; 0.020 kg de queso para el extra.
2. Abre **Costos**: los márgenes deben aparecer pendientes. Captura estos costos por unidad y
   escribe un motivo: pan **$5/pieza**, carne **$100/kg**, queso **$200/kg**.
3. En el detalle de Hamburguesa deben aparecer: costo base **$20.00** ($5 + $15), utilidad
   bruta **$60.00**, margen **75.0 %**. Para Queso extra: costo **$4.00**, incremento de utilidad
   **$6.00** y margen individual **60.0 %**. El margen base excluye el extra.
4. Cambia carne a **$120/kg** con motivo. El costo base actual debe pasar a **$23.00** y el
   margen a **71.3 %**. Abre el historial: muestra el valor anterior, el nuevo, el usuario y el
   motivo. Abre la página en otra tablet para comprobar actualización.
5. Devuelve carne a $100/kg. Registra suficiente stock y envía **2 hamburguesas con queso extra**.
   Los movimientos automáticos deben guardar pan **−$10.00**, carne **−$30.00** y queso
   **−$8.00**, además de sus cantidades físicas; costo de insumos total **$48.00**.
6. Cambia el costo del queso a $250/kg. En el historial de movimientos la comanda anterior debe
   seguir mostrando queso a **$200/kg** y **−$8.00**. Cancela su partida mientras está Pendiente:
   la reversa debe ser **+$8.00** para queso, manteniendo el costo de $200/kg.
7. Envía una comanda con un insumo sin costo configurado: puede operar y descontar stock; su
   movimiento monetario muestra costo pendiente y la pantalla evita calcular el margen del
   producto. No se bloquea la cocina por falta de datos de precios.
8. Prueba dos tablets editando el mismo costo; la segunda debe recibir un conflicto de versión.
   Comprueba que un operador sin rol de propietario/administrador no acceda a `/costos`.

## Comprobaciones pendientes en este entorno

Angular compiló y su suite se ejecutó. Aquí solo está Java 17 y Maven Central no respondió, así
que **la compilación, pruebas Maven y migración V14 en PostgreSQL se deben comprobar con JDK 21**
en tu equipo antes de subir el bloque a Git. Las pruebas Java incluidas cubren cálculo base y
extras, costo faltante, historial de actualización y costo histórico en consumo/reversa.
