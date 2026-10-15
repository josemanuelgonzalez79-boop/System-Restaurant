# Inicio operativo — Essential Pro

Este paquete parte de `pro` en el commit `a5546f29909556977f58edce0d91c5d5c1be5130`. Reemplaza el Inicio genérico por el tablero operativo de Base, el panel de inventario de Plus y una sección propia de Pro. La pantalla y la pestaña del navegador indican **Essential Pro**. No hay migraciones ni cambios en el backend.

## Qué muestra

- Pedidos activos, mesas ocupadas, preparación, pedidos recientes y caja, según el rol.
- Propietario, administrador, gerente y cajero: reservaciones **de hoy** con pendientes por recibir, clientes ya sentados y las cuatro llegadas pendientes más próximas; acceso a Reservaciones y Clientes. Los horarios usan la zona horaria configurada del restaurante. No se muestra teléfono ni ficha del cliente en Inicio.
- Propietario, administrador y gerente: promociones vigentes según el servidor y propinas netas del reporte diario. Las propinas se muestran separadas de las ventas.
- Propietario y administrador: faltantes, costos sin capturar y valor conocido del inventario, indicando si la valoración es parcial.
- Eventos del restaurante por WebSocket, actualización manual y consulta periódica cada 60 segundos. Una falla de agenda, reporte o inventario no borra el tablero de pedidos y se identifica junto al dato afectado.

## Instalar y comprobar

1. Usa la rama local `pro`. Conserva tu `SystemRestaurantBack/secret.yml`, la base de datos y cualquier otra configuración local. Copia el contenido del ZIP sobre el proyecto.
2. Inicia el backend normalmente. En `SystemRestaurantFront`, ejecuta `npm ci`, `npm run build`, `npm test -- --watch=false` y `npm start`.
3. Como propietario o administrador, confirma el distintivo **Essential Pro** y los tres conjuntos de información: operación, recepción e inventario. Crea una reservación para hoy y comprueba el nombre, la mesa y la hora en Inicio; siéntala y confirma el cambio entre pendientes y sentadas.
4. Activa una promoción vigente: el contador debe aumentar. Registra un cobro con propina y revisa que aparezca en **Propinas recibidas hoy**, sin sumarla a pedidos ni a ventas. El reporte completo sigue en Reporte diario.
5. Como cajero, comprueba Reservaciones y Clientes, sin Inventario ni el importe de propinas o enlace a Promociones. Como operador, comprueba solo la operación diaria, sin los paneles restringidos.
6. En otra tablet o pestaña, cambia una reservación, prepara una comanda o registra un movimiento de inventario. Inicio debe actualizar el apartado correspondiente. Comprueba el botón **Actualizar** y el diseño en modo oscuro y pantalla estrecha.

Cada instalación corresponde a un solo restaurante y una base de datos independiente. Las cifras usan APIs existentes y no alteran ventas, reservas ni existencias.
