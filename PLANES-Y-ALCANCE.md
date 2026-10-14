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
restaurante**. El primer módulo disponible registra insumos, unidad, mínimo, existencias, compras,
salidas manuales, mermas y conteos físicos con historial y control de cambios entre tablets.

Siguientes módulos previstos: recetas por producto, consumo de insumos asociado a pedidos,
costeo básico y alertas operativas. Este primer módulo de inventario **no descuenta ingredientes
automáticamente al vender**, porque todavía no hay recetas que indiquen las cantidades.

## Opción 3: Essential Pro (alcance futuro)

Podría agregar reservaciones, clientes frecuentes, promociones e integraciones según las
necesidades del restaurante. También conservará el límite de un restaurante por instalación.

Ninguna de estas opciones presupone WAN, sincronización entre locales ni reportes consolidados.
Para dar servicio a otro establecimiento se instala una instancia distinta, con una base de datos
nueva y una red local propia.
