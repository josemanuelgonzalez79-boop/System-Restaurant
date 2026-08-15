# Planes y alcance de Essential

Esta separación mantiene una primera edición alcanzable para restaurantes pequeños sin cerrar la
puerta a clientes con varias ubicaciones. Los nombres son provisionales.

## Opción 1: Essential Restaurante Local

Dirigida a un restaurante pequeño o mediano que trabaja en una sola ubicación física.

### Arquitectura

- Un equipo del restaurante ejecuta Spring Boot y PostgreSQL como servidor local.
- Tablets, cajas y pantallas de cocina entran a Essential mediante el Wi-Fi o LAN del local.
- La operación diaria continúa aunque falle internet.
- La sucursal y las áreas se conservan en el modelo, pero una ubicación remota utiliza otra
  instalación hasta contratar la opción multi-sucursal.

### Alcance para considerar terminada la edición

1. Configuración, catálogo, usuarios, permisos, sucursal, áreas y mesas.
2. Mapa de mesas y apertura de pedidos desde tablet, barra, mostrador o para llevar.
3. Captura de productos, cantidades, modificadores, observaciones y comensales.
4. Comandas en cocina o barra con estados y actualización en tiempo real.
5. Cobro con una o varias formas de pago y cierre del pedido.
6. Apertura/cierre de caja y resumen diario.
7. Respaldo y restauración guiados.
8. Instalación documentada en la red local y acceso sencillo desde tablets.

No necesita WAN ni nube. Tampoco debe exponer PostgreSQL o Spring Boot directamente a internet.

## Opción 2: Essential Multi-sucursal

Dirigida a un propietario que necesita consultar y coordinar restaurantes ubicados en redes
diferentes.

- Conectividad WAN protegida por VPN o despliegue central con HTTPS en la nube.
- Catálogo, usuarios y permisos comunes para varias sucursales.
- Supervisión y reportes consolidados desde otra ubicación.
- Auditoría de cambios, copias de seguridad centralizadas y control de acceso remoto.
- Estrategia de continuidad cuando una sucursal pierde su enlace a internet.

Antes de construir esta opción deberá elegirse entre servidor central o sincronización entre
instalaciones. Esa decisión afecta seguridad, costos, operación sin internet y mantenimiento.

## Opción 3: Essential Restaurante Pro

Dirigida a restaurantes que requieren administración adicional e integraciones.

- Inventario, insumos, recetas, mermas y costo de platillos.
- Clientes frecuentes, reservaciones, promociones y fidelización.
- Analítica avanzada, metas y comparación entre sucursales.
- Integraciones de reparto, facturación electrónica y contabilidad cuando se definan los países.
- Mayor continuidad operativa, monitoreo y soporte administrado.

## Situación actual

| Medida                                | Estado                           |
| ------------------------------------- | -------------------------------- |
| Bloques terminados                    | 8 de 12                          |
| Avance por bloques                    | 67 %                             |
| Avance práctico estimado              | Alrededor de 75 %                |
| Bloques pendientes de la opción local | 4                                |
| Siguiente bloque                      | Apertura y cierre básico de caja |

La captura del pedido y la comanda de cocina desde tablets ya forman parte del sistema. La prioridad
inmediata no es la WAN: la actualización instantánea local y el cobro ya forman un circuito completo.
Ahora se incorporarán caja, reporte, respaldo e instalación. La conexión entre sucursales se diseñará
después sobre esa operación estable.
