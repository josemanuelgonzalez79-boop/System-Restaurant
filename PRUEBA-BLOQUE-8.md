# Prueba del Bloque 8: cobro y formas de pago

Este bloque agrega cobros completos o divididos, cambio en efectivo, anulaciones auditables y el
cierre pagado del pedido. También corrige el indicador de Preparación cuando el usuario no tiene una
sucursal asignada.

## 1. Actualizar y compilar

1. Confirma que estás trabajando en la rama `desarrollo`.
2. Detén Angular y Spring Boot.
3. Conserva `SystemRestaurantBack/secret.yml`; el ZIP no contiene credenciales.
4. Reemplaza los archivos y vuelve a colocar `secret.yml`.
5. Desde `SystemRestaurantBack` ejecuta:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

6. Flyway debe aplicar `V7__create_order_payments.sql` y el backend debe iniciar en el puerto 3211.
7. Desde `SystemRestaurantFront` ejecuta:

```powershell
npm ci
npm run build
npm test -- --watch=false
npm start
```

## 2. Verificar la asignación y el canal en tiempo real

1. Entra con el propietario y abre **Estructura**.
2. En la sucursal principal, guarda la asignación del usuario con el que operarás.
3. Abre **Preparación**. El indicador debe terminar en **Actualización inmediata**.
4. En DevTools, la conexión propia de Essential aparece como `/ws` con estado `101`.
5. Una conexión con nombre `?token=...` es la recarga en caliente de Angular durante desarrollo;
   no es el WebSocket operativo de Essential.
6. Si el usuario no tiene asignaciones, Preparación debe decir **Sin sucursal asignada**, sin
   quedarse mostrando **Reconectando…**.

## 3. Cobro completo en efectivo

1. Abre una mesa, agrega productos, envía las partidas y entrégalas desde Preparación.
2. En el pedido pulsa **Cobrar cuenta**.
3. Confirma que total, cobrado y saldo coincidan con la captura.
4. Selecciona efectivo, escribe un importe recibido mayor al saldo y registra el cobro.
5. Confirma que Essential muestre el cambio correcto y que el historial conserve el movimiento.
6. Pulsa **Cerrar pedido** y confirma que la mesa quede libre.

## 4. Cuenta dividida

1. Abre otro pedido con un total conocido.
2. Registra una parte con tarjeta y una referencia de autorización.
3. Registra el saldo con transferencia u otra forma de pago.
4. Comprueba que cada movimiento aparezca por separado y que el saldo termine exactamente en cero.
5. Intenta aplicar un importe superior al saldo; Angular debe advertirlo y el backend debe rechazarlo.

## 5. Reglas operativas y anulación

1. Después del primer cobro, intenta agregar, editar o retirar productos del pedido.
2. Essential debe pedir que se anulen primero los cobros activos.
3. Con propietario, administrador, gerente o cajero, anula un cobro e indica el motivo.
4. Comprueba que el movimiento no desaparezca: debe mostrar usuario, fecha y motivo de anulación.
5. Un operador puede cobrar, pero no debe ver la acción para anular.
6. Intenta cancelar un pedido con un cobro activo; debe solicitar anularlo primero.

## 6. Preparación y cierre

1. Paga totalmente un pedido que todavía tenga una partida activa en cocina.
2. El saldo debe quedar en cero, pero **Cerrar pedido** debe continuar bloqueado.
3. Entrega todas las partidas desde Preparación.
4. Regresa a Cobro, confirma que el cierre quede habilitado y libera la mesa.

## 7. Dos cajas o tablets

1. Abre la misma cuenta en dos navegadores con usuarios asignados a la sucursal.
2. En ambos intenta registrar al mismo tiempo el saldo completo.
3. Solo un cobro debe aplicarse. La otra pantalla debe informar que el pedido cambió y pedir una
   actualización.
4. Actualiza ambas pantallas y confirma que no exista sobrepago ni un movimiento duplicado.

## Resultado esperado

- Un usuario sin sucursal ya no se confunde con una reconexión fallida.
- Los cobros pueden dividirse y nunca superan el saldo del pedido.
- El efectivo calcula cambio y los métodos electrónicos conservan su referencia.
- Las anulaciones permanecen auditables.
- Dos dispositivos no pueden cobrar o modificar la misma cuenta de forma incompatible.
- La mesa se libera solo después de pagar y concluir la operación de preparación.
