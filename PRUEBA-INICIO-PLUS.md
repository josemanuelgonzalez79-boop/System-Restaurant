# Inicio operativo — Essential Plus

Este paquete parte de `plus` en el commit `a66bb5f1160db51191b458ccf66c2580a1b0e629`. Incluye el nuevo Inicio de Base adaptado a Plus y conserva los cinco bloques de inventario, recetas, consumo, costos y reportes. La pantalla y la pestaña del navegador indican **Essential Plus**. No hay cambios en backend ni migraciones.

## Qué encontrarás

- Pedidos activos, mesas ocupadas, partidas pendientes y listas para entregar, pedidos recientes y estado de caja según el rol.
- Para propietario y administrador: alertas de insumos agotados o en/bajo el mínimo, costos faltantes, valor conocido de existencias y hasta cinco insumos que requieren atención.
- Cuando hay insumos con stock y sin costo, la valoración se indica como **parcial**. El valor se calcula en el servidor y no representa ventas ni utilidad.
- Accesos a Inventario, Recetas, Costos y Conteo para usuarios autorizados. Los operadores no consultan la API de inventario ni ven esos accesos.
- Actualización por eventos WebSocket del restaurante, botón manual y consulta periódica cada 60 segundos. Si el reporte de inventario falla, los pedidos siguen visibles y el error aparece solo en su panel.

## Instalación y prueba

1. Cambia a la rama local `plus`. Conserva tu `SystemRestaurantBack/secret.yml` y tu base PostgreSQL. Copia el contenido del ZIP sobre el proyecto, sin borrar archivos de configuración propios.
2. En `SystemRestaurantFront`, ejecuta `npm ci`, `npm run build`, `npm test -- --watch=false` y `npm start`. Inicia el backend como acostumbras; no hay migración nueva.
3. Inicia sesión como propietario o administrador. Comprueba **Essential Plus** y los datos actuales del restaurante. Registra un insumo con mínimo mayor que su existencia: debe aparecer en “Revisa primero”. Déjalo en cero para comprobar “Agotado”.
4. Deja un insumo activo con existencia positiva y sin costo. Revisa que “Sin costo de referencia” aumente y que el valor conocido diga **Parcial**. Captura su costo en Costos y actualiza Inicio.
5. Abre un pedido y envía una comanda desde otra tablet o pestaña. Verifica que cambien las tarjetas de operación y, al consumir receta, las alertas de existencias.
6. Inicia sesión como operador: debe ver pedidos y preparación, sin el panel de inventario ni enlaces a Inventario, Recetas, Costos o Conteo. Un cajero conserva el acceso a Caja, sin inventario.
7. Revisa modo oscuro y una pantalla estrecha. Las tarjetas deben ajustarse sin salirse de sus contenedores.

Los reportes, movimientos e historial completos siguen en sus pantallas respectivas. Cada instalación corresponde a un solo restaurante y una base de datos independiente.
