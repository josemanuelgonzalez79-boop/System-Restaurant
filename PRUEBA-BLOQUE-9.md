# Prueba del bloque 9: caja

Esta guía valida la apertura, operación y cierre de caja en una sucursal. El turno es único por
sucursal y se comparte entre las computadoras y tablets conectadas al sistema local.

## 1. Actualizar y compilar

1. Trabaja sobre tu rama `desarrollo` y conserva `SystemRestaurantBack/secret.yml`.
2. Inicia PostgreSQL.
3. Desde `SystemRestaurantBack`, ejecuta:

   ```powershell
   .\mvnw.cmd clean test
   .\mvnw.cmd spring-boot:run
   ```

4. Confirma en la consola que Flyway aplicó `V8__create_cash_register_sessions.sql`.
5. Desde `SystemRestaurantFront`, ejecuta:

   ```powershell
   npm ci
   npm run build
   npm test
   npm start
   ```

## 2. Permisos y apertura

1. Ingresa como propietario, administrador, gerente o cajero.
2. Confirma que aparece la opción **Caja** en la navegación.
3. Selecciona una sucursal y abre el turno con un fondo inicial, por ejemplo `$500.00`.
4. Abre Essential en otra tablet o pestaña y confirma que muestra el mismo turno abierto.
5. Intenta abrir otra caja para esa sucursal. La aplicación debe explicar que ya existe una.
6. Ingresa como operador. Puede capturar y cobrar pedidos, pero no debe ver ni administrar Caja.

## 3. Cobros vinculados

1. En una sucursal sin caja abierta, intenta registrar un cobro. Debe aparecer el mensaje para
   abrir caja y no debe guardarse el pago.
2. Abre la caja y registra cobros en efectivo, tarjeta y transferencia.
3. Regresa a Caja. Los importes deben aparecer separados por forma de pago.
4. Comprueba que el efectivo esperado sea:

   `fondo inicial + cobros en efectivo + entradas - salidas`.

5. Repite un cobro solo mediante el mismo identificador de operación. No debe duplicarse.

## 4. Entradas, salidas y auditoría

1. Registra una entrada con importe y concepto.
2. Registra una salida con importe y concepto.
3. Intenta una salida superior al efectivo esperado. Debe rechazarse con una explicación.
4. Anula un movimiento e indica el motivo.
5. Confirma que el movimiento no desaparece: debe conservar estado, usuario, fecha y motivo.
6. Si una entrada ya respaldó una salida, intenta anularla. Debe pedir anular primero la salida para
   evitar un efectivo esperado negativo.

## 5. Cierre y arqueo

1. Deja una cuenta parcialmente cobrada y todavía abierta.
2. Intenta cerrar caja. Debe bloquearse hasta completar o resolver esa cuenta.
3. Completa el saldo, cierra el pedido pagado y confirma que la mesa se libere; después vuelve a
   Caja.
4. Captura el efectivo contado. Por ejemplo, si el esperado es `$830.00` y cuentas `$825.00`, la
   diferencia debe ser `-$5.00`.
5. Cierra el turno y confirma responsable, hora, esperado, contado, diferencia y notas.
6. Revisa el historial y utiliza **Imprimir corte**.

## 6. Simultaneidad y tiempo real

1. Intenta abrir la caja de la misma sucursal casi al mismo tiempo desde dos tablets. Solo una
   apertura debe guardarse.
2. Registra un cobro o movimiento en una tablet y confirma que la vista de Caja de la otra se
   actualice mediante WebSocket.
3. Si el Wi-Fi se interrumpe, confirma que el indicador pase a reconexión y vuelva a conectado al
   recuperar la red.
4. Intenta desactivar una sucursal con caja abierta. Debe solicitar cerrar la caja primero.

## 7. Compatibilidad de datos

Los cobros creados antes de la migración V8 se conservan sin alteraciones. No se asignan
retroactivamente a un turno nuevo y, por lo tanto, no deben sumarse al corte recién abierto.

Al terminar, conserva capturas de cualquier mensaje inesperado y el texto completo de la consola
de Spring Boot. No publiques el cambio en Git hasta completar estas pruebas.
