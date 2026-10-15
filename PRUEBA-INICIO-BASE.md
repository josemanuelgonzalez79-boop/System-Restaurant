# Inicio operativo — Essential Base

Este paquete parte de `desarrollo` en el commit `67b2a24a06aed48285902a4c6a1aca2e9bbad33f` y cambia la pantalla Inicio. No contiene migraciones ni cambios en el backend. La edición **Essential Base** queda visible en el encabezado del tablero y en el título de la página.

## Qué muestra

- Pedidos activos y mesas ocupadas del restaurante local.
- Partidas de cocina o barra por iniciar, en preparación y listas para entregar.
- Los cinco pedidos activos más recientes, con acceso directo a cada uno.
- Estado de la caja, solo para usuarios con permiso de caja.
- Accesos a mesas, preparación, reporte, catálogo y respaldos según el rol.
- Última actualización, botón manual, avisos en tiempo real y consulta de respaldo cada 60 segundos.

Las cifras se obtienen de las APIs actuales; no se guardan ni calculan totales de venta nuevos. Una falla de red muestra el error y, si ya hubo una carga correcta, conserva el último estado con su hora de actualización.

## Instalar y probar

1. Confirma que estás en `desarrollo` y conserva tu `SystemRestaurantBack/secret.yml` local. Copia los archivos del paquete sobre tu proyecto sin borrar tu base de datos ni los archivos locales de configuración.
2. Inicia el backend como de costumbre. No hay nueva migración Flyway.
3. En `SystemRestaurantFront`, ejecuta `npm ci`, `npm run build`, `npm test -- --watch=false` y `npm start`.
4. Entra con un propietario o administrador. Comprueba la etiqueta **Essential Base**, el nombre del restaurante y el estado de caja.
5. Abre una mesa, agrega productos y envía una comanda desde otra tablet o pestaña. En Inicio deben cambiar los pedidos y la preparación sin recargar. Cambia una partida a lista y comprueba la tarjeta correspondiente.
6. Prueba con un operador: no debe aparecer Caja, Reporte, Catálogo ni Respaldos. El resto de la operación debe seguir visible.
7. Prueba en una tablet o ventana estrecha. Las tarjetas deben formar dos columnas y los paneles pasar a una columna, sin desbordamientos.
8. Si el usuario no tiene el restaurante asignado, Inicio debe explicar el motivo sin mostrar cifras falsas ni abrir el canal WebSocket.

Si no hay conexión WebSocket, el botón **Actualizar** y la recarga cada 60 segundos continúan consultando REST. El tablero no sustituye las pantallas de Pedidos, Preparación, Caja ni Reporte.
