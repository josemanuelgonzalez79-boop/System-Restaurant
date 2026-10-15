# Essential Pro · Bloque 3: promociones y descuentos

## Instalación

Este paquete parte de la rama `pro` en `850702e` e incluye el proyecto completo. Conserva
`SystemRestaurantBack/secret.yml`, tu base y sus respaldos. La migración
`V18__create_promotions_and_order_discounts.sql` se ejecuta una sola vez con Flyway al arrancar;
no la corras manualmente. No hay limpieza de pedidos, usuarios ni clientes.

Con JDK 21:

```powershell
cd SystemRestaurantBack
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

En otra consola:

```powershell
cd SystemRestaurantFront
npm ci
npm run build
npm test -- --watch=false
npm start
```

## Prueba funcional

1. Entra como propietario, administrador o gerente. Abre **Promociones**, crea `MARTES10`
   con 10 %, fechas que incluyan hoy y compra mínima $100. Reabre la ficha, edita el nombre,
   pausa y vuelve a activar; verifica que la clave no pueda repetirse.
2. Abre caja. Crea un pedido, agrega productos por $200, envía las comandas. En **Cobro**, aplica
   `MARTES10` y confirma subtotal $200, descuento $20, total/saldo $180. Registra dos pagos que
   sumen $180, completa preparación y cierra el pedido. Imprime: deben figurar subtotal,
   descuento, total neto y pagos. El reporte diario debe contar $180 como valor completado.
3. Crea otra promoción fija de $15, pruébala en una cuenta elegible y verifica que no puedas
   aplicar una segunda oferta. Cambia después el valor configurado: la cuenta anterior debe
   conservar su importe y nombre originales.
4. Como gerente, aplica un descuento manual de 12,5 % con motivo. Anúlalo con motivo antes de
   cobrar. Comprueba que ambos movimientos siguen en el historial y que el total vuelve al
   subtotal. Un cajero puede aplicar una promoción, pero no un descuento manual.
5. Aplica un descuento, registra un pago parcial y prueba anularlo: debe rechazarse. Anula
   primero ese pago si el turno de caja sigue abierto. Después podrás anular el descuento.
6. Con dos tablets en la misma cuenta, aplica un descuento desde una y vuelve a intentarlo con
   la versión antigua desde otra. Solo una operación debe registrarse; actualiza la segunda
   tablet para ver el nuevo saldo. Repite el mismo `operationId` por red: no se duplica.
7. Mientras haya descuento activo, intenta cambiar productos, cancelar una partida de cocina o
   cancelar el pedido: se solicita anularlo antes. La cocina sí puede completar sus estados.
8. Prueba porcentaje de 90,01 %, importe fijo de $91 en subtotal de $100, clave duplicada,
   fechas invertidas y compra por debajo del mínimo: todos deben rechazarse con mensaje.

**Nota:** los importes por producto del Top 10 son brutos; el valor de pedidos completados y
las ventas cobradas reflejan los descuentos. Cada restaurante conserva su propia instalación
local y base PostgreSQL independiente.
