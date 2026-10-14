# Planes y alcance de Essential

## Regla de instalación

Essential opera un solo restaurante por instalación y por base de datos PostgreSQL. Las tablets,
caja y cocina del mismo local se conectan al servidor por Wi-Fi/LAN. Un segundo restaurante
requiere otra instalación, otra base de datos, su configuración, usuarios, catálogo, reportes y
respaldos propios. No se enlazan las instalaciones ni se comparten ventas o sesiones.

El identificador interno de restaurante conserva la compatibilidad con los pedidos y turnos de
caja ya guardados. No se pueden crear más restaurantes dentro de la misma base. `V10` detiene la
actualización si detecta varias sucursales preexistentes, para evitar mezclar o borrar sus datos.

## Opción 1: Essential Restaurante Local

- Configuración, catálogo, usuarios y roles del restaurante.
- Áreas, mesas, pedidos de salón y para llevar.
- Captura de productos y modificadores; comandas para cocina y servicio.
- Sincronización entre tablets conectadas a la red local.
- Pagos, caja, reporte diario y respaldos.
- Despliegue local con HTTPS. La operación no depende de internet.

Los doce bloques de esta edición están implementados. Faltan el piloto con tablets, el reinicio
real del servidor y una restauración probada en una base separada antes de ofrecerla en producción.

## Opción 2: Essential Plus (en desarrollo)

Parte del código de Essential Restaurante Local y conserva **una instalación independiente por
restaurante**. Se desarrolla en la rama `plus` y se organiza en **cinco bloques**:

| Bloque | Función | Estado |
| --- | --- | --- |
| 1 | Insumos, existencias, compras, salidas, mermas y conteos con historial | Probado y subido a `plus` |
| 2 | Recetas por producto y opción extra, con cantidades por unidad | Probado y subido a `plus` |
| 3 | Consumo de recetas al enviar comandas y reversas auditables | Probado y subido a `plus` |
| 4 | Costos por receta, costo de extras y márgenes por producto | Implementado en esta entrega |
| 5 | Reportes de inventario, alertas y conciliación de conteos | Pendiente |

Las partidas enviadas a cocina o servicio consumen la receta de producto y las recetas adicionales
de las opciones elegidas. Una cancelación antes de comenzar revierte exactamente ese consumo;
después de comenzar se conserva el consumo físico. Una partida cancelada deja de cobrarse. Los
productos con ruta `NONE` no generan comandas y siguen fuera del consumo automático.

Los costos de Plus son de referencia por unidad de insumo, registrados con motivo e historial.
El margen es una estimación de materia prima sobre precio de venta; falta de receta o precio de
insumo deja el margen pendiente. V14 conserva el costo vigente en cada nuevo consumo y su reversa.
La compra de stock no recalcula el costo automáticamente.

## Opción 3: Essential Pro (alcance futuro)

Podría agregar reservaciones, clientes frecuentes, promociones e integraciones según las
necesidades del restaurante. También conservará el límite de un restaurante por instalación.

Ninguna de estas opciones presupone WAN, sincronización entre locales ni reportes consolidados.
Para dar servicio a otro establecimiento se instala una instancia distinta, con una base de datos
nueva y una red local propia.
