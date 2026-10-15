# Essential Pro · Bloque 4: propinas

Base: rama `pro`, commit `3539debc6ca3febbe0934113441d895f35608956`.

## Antes de copiar

- Trabaja en `pro` y conserva `SystemRestaurantBack/secret.yml` y tu base de datos.
- Copia el contenido del ZIP sobre tu proyecto; no ejecutes SQL a mano. Flyway aplicará `V19__add_payment_tips.sql` una vez al arrancar.
- Requiere JDK 21, PostgreSQL y Node según el proyecto.

```powershell
cd SystemRestaurantBack
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

En otra terminal:

```powershell
cd SystemRestaurantFront
npm ci
npm run build
npm test -- --watch=false
npm start
```

## Recorrido funcional

1. Abre caja, crea un pedido con productos por $100 y envía sus comandas.
2. En **Cobro**, registra $100 de consumo y $15 de propina en efectivo; recibe $150. Debe indicar cambio $35, saldo del pedido $0, consumo $100 y propina $15. La propina corresponde al responsable del pedido.
3. En **Caja**, confirma que ventas en efectivo suman $100 y propinas en efectivo $15. Si el fondo inicial fue $500 y no hay movimientos manuales, el efectivo esperado será $615. Cierra el pedido y arquea la caja.
4. En otro pedido, cobra una parte de $40 con tarjeta y $5 de propina. El importe efectivamente cargado deberá ser $45; el saldo del pedido baja sólo $40. Prueba también transferencia y cobros sin propina.
5. Anula un cobro con propina antes de cerrar caja. Confirma que se revierte tanto el consumo como la propina, que vuelve el saldo del pedido y que se conserva usuario, fecha y motivo de la anulación.
6. En **Reporte diario**, confirma venta neta sin propinas, propinas por medio y responsable, flujo de efectivo con propinas de efectivo y el mismo desglose al descargar CSV e imprimir.
7. Intenta una propina mayor que el consumo del cobro, y una tarjeta cuyo importe recibido no incluya la propina. Ambos deben ser rechazados con un mensaje claro.
8. Comprueba que cobros anteriores a `V19` aparecen con propina $0 y que el arranque no duplicó datos. Desde dos tablets, un cobro repetido con la misma operación no debe duplicar el importe.

El reporte agrupa entradas por fecha de cobro y anulaciones por fecha de anulación según la zona horaria del restaurante. Un día puede mostrar una propina neta negativa si se anuló un cobro de otra fecha. El nombre del responsable se congela al registrar el cobro.

## Alcance

Con este bloque termina la construcción planeada de Pro (4 de 4). La aceptación definitiva sigue sujeta a ejecutar las pruebas anteriores y el recorrido integral en el restaurante piloto. No incluye distribución de propinas entre empleados, pago de nómina ni conexión bancaria.
