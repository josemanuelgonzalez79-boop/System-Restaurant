# Prueba del bloque 10: reporte diario

Esta guía valida el resumen operativo diario por sucursal. El reporte usa el calendario y la zona
horaria configurados en la sucursal; PostgreSQL conserva las fechas internamente en UTC.

## 1. Actualizar y compilar

1. Trabaja sobre la rama `desarrollo` y conserva `SystemRestaurantBack/secret.yml`.
2. Inicia PostgreSQL.
3. Desde `SystemRestaurantBack`, ejecuta:

   ```powershell
   .\mvnw.cmd clean test
   .\mvnw.cmd spring-boot:run
   ```

4. Confirma que Flyway aplicó `V9__add_daily_report_indexes.sql`. Esta migración no modifica ni
   elimina ventas: solamente agrega índices para consultar el reporte con rapidez.
5. Desde `SystemRestaurantFront`, ejecuta:

   ```powershell
   npm ci
   npm run build
   npm test -- --watch=false
   npm start
   ```

## 2. Permisos y filtros

1. Ingresa como propietario, administrador o gerente.
2. Confirma que aparece la opción **Reporte** y abre **Reporte diario**.
3. Cambia de sucursal y de fecha. El encabezado debe mostrar la selección correcta.
4. Intenta elegir una fecha futura. El selector no debe permitirla y la API también debe
   rechazarla.
5. Ingresa como cajero u operador. No deben ver la opción ni poder abrir directamente
   `/reporte-diario`.
6. Comprueba que un usuario no pueda consultar una sucursal a la que no está asignado.

## 3. Preparar un día de prueba

En una sucursal con caja abierta:

1. Completa al menos dos pedidos; uno en mesa y otro para llevar.
2. Registra cobros en efectivo, tarjeta y transferencia.
3. Registra una entrada y una salida manual de caja.
4. Anula un cobro indicando el motivo.
5. Cancela otro pedido que no tenga cobros ni partidas activas en preparación.
6. Cierra los pedidos pagados y después realiza el cierre de caja con un conteo diferente al
   esperado, por ejemplo una diferencia de `-$5.00`.

## 4. Validar los totales

En el reporte de la fecha utilizada, confirma:

1. **Cobros recibidos** suma todos los pagos registrados durante el día.
2. **Anulaciones del día** suma los cobros anulados durante el día, aunque el cobro original sea de
   otra fecha.
3. **Venta neta** es `cobros recibidos - anulaciones del día`.
4. Efectivo, tarjeta, transferencia y otros coinciden con los movimientos registrados.
5. Los pedidos abiertos, completados, cancelados, en mesa y para llevar coinciden con la operación.
6. El ticket promedio usa el valor histórico de los pedidos completados.
7. La gráfica coloca cada cobro o anulación en la hora local correcta de la sucursal.
8. El top de productos respeta cantidades, precios y modificadores históricos.
9. El flujo esperado de caja es:

   `fondos de turnos abiertos ese día + efectivo cobrado + entradas netas - salidas netas`.

10. La diferencia total de caja coincide con los cortes cerrados durante el día.

## 5. Auditoría y turnos

1. Confirma que el pedido cancelado conserva folio, modalidad, responsable, hora e importe.
2. Confirma que el cobro anulado conserva folio, pedido, forma de pago, usuario, hora y motivo.
3. Revisa los turnos relacionados con el día: apertura, cierre, esperado, contado y diferencia.
4. Si el turno atraviesa la medianoche, debe aparecer en ambos días relacionados, pero sus cobros se
   contabilizan en la fecha y hora en que ocurrieron.

## 6. Exportación y actualización

1. Pulsa **Exportar CSV**, abre el archivo y comprueba que contiene resumen, formas de pago, horas,
   productos, caja, cancelaciones y anulaciones.
2. Pulsa **Imprimir / PDF** y comprueba la vista previa; desde ahí puede guardarse como PDF.
3. Mantén el reporte de hoy abierto y registra un cobro desde otra tablet. Debe actualizarse al
   recibir el aviso WebSocket; **Actualizar** permanece disponible como respaldo manual.
4. Desconecta temporalmente el Wi-Fi y confirma que, después de reconectar, el botón **Actualizar**
   recupera la información desde REST.

## Definiciones importantes

- La venta neta es un resumen operativo basado en cobros, no sustituye contabilidad, CFDI ni una
  declaración fiscal.
- El valor de pedidos completados se calcula con los nombres, precios y modificadores históricos
  guardados al capturar la cuenta.
- Una anulación se descuenta el día en que se realiza. Esto permite auditar correctamente una
  devolución o corrección hecha después del cobro original.

No publiques el cambio en Git hasta completar estas pruebas y conservar una copia de
`secret.yml`.
