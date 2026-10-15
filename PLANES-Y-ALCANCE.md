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

## Opción 2: Essential Plus (cinco bloques implementados)

Parte del código de Essential Restaurante Local y conserva **una instalación independiente por
restaurante**. Se desarrolla en la rama `plus` y se organiza en **cinco bloques**:

| Bloque | Función | Estado |
| --- | --- | --- |
| 1 | Insumos, existencias, compras, salidas, mermas y conteos con historial | Probado y subido a `plus` |
| 2 | Recetas por producto y opción extra, con cantidades por unidad | Probado y subido a `plus` |
| 3 | Consumo de recetas al enviar comandas y reversas auditables | Probado y subido a `plus` |
| 4 | Costos por receta, costo de extras y márgenes por producto | Probado y subido a `plus` |
| 5 | Reportes de inventario, alertas y conciliación de conteos | Implementado en esta entrega; pendiente de prueba en instalación |

Las partidas enviadas a cocina o servicio consumen la receta de producto y las recetas adicionales
de las opciones elegidas. Una cancelación antes de comenzar revierte exactamente ese consumo;
después de comenzar se conserva el consumo físico. Una partida cancelada deja de cobrarse. Los
productos con ruta `NONE` no generan comandas y siguen fuera del consumo automático.

Los costos de Plus son de referencia por unidad de insumo, registrados con motivo e historial.
El margen es una estimación de materia prima sobre precio de venta; falta de receta o precio de
insumo deja el margen pendiente. V14 conserva el costo vigente en cada nuevo consumo y su reversa.
La compra de stock no recalcula el costo automáticamente.

El bloque 5 agrega reporte de movimientos por periodo (hasta 31 días), alertas de stock y costos
faltantes, valoración de existencia actual y costo histórico conocido de comandas. Cuando faltan
costos se indica que el total es parcial. El conteo físico revisa todos los insumos activos en
una sola confirmación: registra la comparación esperada/contada, crea ajustes únicamente para las
diferencias y rechaza todo el conteo si algún insumo cambió antes de guardarlo. `V15` conserva el
historial anterior sin alterar existencias al migrar. La validación en una instalación con datos
reales sigue pendiente antes de ofrecer Plus a otros restaurantes.

## Opción 3: Essential Pro (rama `pro`)

Parte de **Plus completo, incluida V15**, y sigue instalado para un solo restaurante por base de
datos. El plan inicial comprende cuatro bloques, sujeto a ajustes después de probar cada uno:

| Bloque | Función | Estado |
| --- | --- | --- |
| 1 | Reservaciones, agenda de mesas y llegada con pedido | Probado y subido a `pro` |
| 2 | Fichas de clientes y preferencias, con control de acceso | Probado y subido a `pro` |
| 3 | Promociones y descuentos auditables durante el cobro | Probado y subido a `pro` |
| 4 | Propinas voluntarias en cobros, corte de caja y reporte diario | Implementado; pendiente de prueba local |

La reserva ocupa un intervalo específico de una mesa o salón y PostgreSQL impide traslapes.
Un cliente que llega puede sentarse y abrir un pedido vinculado. El pedido normal no puede
ocupar una mesa reservada dentro de los próximos 90 minutos; el mapa la señala sin revelar los
datos del cliente al personal de mesas. Cancelación y ausencia liberan el
horario y dejan historial. Los horarios se interpretan con la zona horaria del restaurante; no
hay reservas compartidas entre instalaciones ni dependencia de internet.

El bloque 2 agrega fichas locales con teléfono normalizado para prevenir duplicados, preferencias,
indicaciones alimentarias y archivado reversible. Solo propietario, administrador, gerente y
cajero consultan estas fichas. Las reservaciones pueden enlazarlas y los pedidos abiertos desde
la agenda conservan ese vínculo. En Pedidos, esos roles también pueden vincular directamente una
cuenta de mesa o para llevar. El historial muestra únicamente visitas vinculadas explícitamente;
no se infiere identidad a partir de nombres iguales ni se migran datos de clientes antiguos.

El bloque 3 incorpora promociones vigentes por fecha local, clave única, porcentaje o importe
fijo y compra mínima. Propietario, administrador y gerente las configuran; caja puede aplicar
una promoción a una cuenta antes del primer cobro. Los descuentos manuales exigen uno de esos
tres roles y un motivo. Solo hay un descuento activo por pedido; el importe se congela y las
anulaciones conservan usuario, motivo y fecha. La cuenta se cobra sobre el total neto, pero el
reporte de productos conserva su importe bruto para que no se asigne arbitrariamente un
descuento de cuenta a cada producto. No se descuentan pedidos ya pagados ni se combinan ofertas.

El bloque 4 registra propinas voluntarias en cada cobro y las atribuye al responsable
del pedido en ese momento. Consumo y propina se muestran y contabilizan por separado:
la propina no reduce el saldo ni incrementa la venta neta o el ticket promedio.
El efectivo esperado de caja sí incluye propinas en efectivo. Una anulación revierte
ambos importes y conserva el rastro de auditoría. El reporte diario y su CSV muestran
propinas por método y responsable, con la misma regla de fecha local usada por las
ventas. Un cobro no admite una propina mayor al importe aplicado. Esta primera versión
registra el dinero recibido; no gestiona reparto, retiro de propinas ni nómina.

Con este bloque queda implementado el alcance inicial de cuatro bloques de Pro; su
cierre comercial requiere pasar las pruebas locales del bloque y el recorrido integral
de las tres versiones en el restaurante piloto.

Ninguna de estas opciones presupone WAN, sincronización entre locales ni reportes consolidados.
Para dar servicio a otro establecimiento se instala una instancia distinta, con una base de datos
nueva y una red local propia.
