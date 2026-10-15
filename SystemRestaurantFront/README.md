# Essential Frontend

Interfaz Angular y PrimeNG para Essential. Consume el backend Spring Boot mediante `/api`.

## Requisitos

- Node.js 22.12 o superior dentro de la rama 22, o Node.js 24.
- npm 10 o superior.
- Backend Essential ejecutándose en `http://localhost:3211`.

## Instalar y ejecutar

```powershell
npm ci
npm start
```

La aplicación abre en `http://localhost:4200`. `proxy.conf.json` redirige `/api` y `/ws` hacia el
backend local.

## Compilar

```powershell
npm run build
```

El resultado queda en:

```text
dist/restaurant-system-front/browser
```

## Estructura

- `src/app/core`: servicios globales, modelos, interceptores y guardias.
- `src/app/features`: acceso, usuarios, configuración, catálogo, estructura y pedidos.
- `src/environments/environment.ts`: configuración general.
- `public/essential-mark.png`: marca principal de Essential.

El frontend nunca se conecta directamente a PostgreSQL. Toda operación pasa por Spring Boot.

## Funciones disponibles

- Configuración guiada del primer propietario.
- Inicio, restauración y cierre de sesión.
- Administración de usuarios, roles y contraseñas.
- Configuración genérica del negocio.
- Administración de categorías y productos o servicios.
- Disponibilidad, activación y ruta operativa.
- Administración de áreas y puntos de atención de un único restaurante local.
- Usuarios activos con acceso según su rol, sin asignación de sucursal.
- Mapa de mesas libres y ocupadas por área.
- Apertura de pedidos en el local o para llevar.
- Folio, responsable, comensales, notas y estados del pedido.
- Captura táctil con variantes, notas y total.
- Envío de comandas nuevas a cocina o servicio.
- Tablero de preparación por ruta y estado de cada partida.
- Avisos STOMP/WebSocket del restaurante local y actualización inmediata desde REST.
- Reconexión automática e indicador visible del canal en tiempo real.
- Cobro completo o dividido, cambio en efectivo e historial de movimientos.
- Anulación auditable, cierre pagado de la cuenta y comprobante interno imprimible.
- Apertura de caja del restaurante, movimientos manuales y totales por forma de pago.
- Arqueo con efectivo esperado, conteo real, diferencia e historial imprimible.
- Essential Plus: insumos, existencias, compras, salidas, mermas y conteos físicos con historial.
- Essential Plus: recetas por producto y opción extra con cantidades de insumos por unidad.
- Essential Plus: consumo por comanda, historial por partida, devolución antes de preparar y
  cancelaciones auditadas sin cargo en la cuenta.
- Essential Plus: costos de referencia auditables, costo por receta y extra, utilidad bruta y
  margen sobre venta; costo histórico por comanda.
- Essential Plus: reporte y alertas de inventario, exportación CSV y conciliación auditable de
  conteos físicos sin cambios parciales.
- Essential Pro: agenda de reservaciones por mesa, duración, cancelación, ausencia e historial;
  llegada enlazada al pedido, sin traslapes y con actualización entre tablets.
- Essential Pro: fichas de clientes con contacto, preferencias, indicaciones alimentarias,
  historial de visitas vinculadas, archivado reversible y selección en reservaciones o pedidos.

Rutas:

```text
/login
/setup
/
/pedidos
/pedidos/:id/cobro
/preparacion
/caja
/catalogo
/estructura
/inventario
/inventario/reporte
/recetas
/costos
/reservaciones
/clientes
/configuracion
/usuarios
```
