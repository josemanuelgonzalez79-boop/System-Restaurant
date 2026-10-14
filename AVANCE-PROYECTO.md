# Avance de Essential

## Essential Plus · bloque 2 de 5

La rama real de desarrollo es `plus`. El bloque 1 de inventario local ya fue probado y subido por
el usuario. El bloque 2 incorpora recetas por producto y por opción extra: cada receta indica los
insumos y cantidades para preparar una unidad. Se conservan las versiones y el último editor;
no se pueden desactivar ni cambiar de unidad insumos usados por una receta.

El avance de Plus es **2 de 5 bloques implementados (40 % por bloques)**. El bloque 3 hará el
consumo automático al enviar comandas y las reversas auditables. **Las ventas todavía no consumen
existencias**. La guía de esta entrega es `PRUEBA-PLUS-RECETAS.md`; el alcance completo está en
`PLANES-Y-ALCANCE.md`.

> **Decisión vigente (octubre de 2026):** Essential opera un solo restaurante por instalación y
> base de datos. La migración `V10` cierra la creación de sucursales y elimina la asignación
> manual de usuarios. Los apartados de bloques anteriores documentan su implementación original;
> `PLANES-Y-ALCANCE.md` describe el alcance actual.

## Objetivo

Construir un sistema de operación para restaurantes pequeños y medianos, accesible desde
computadoras, tablets y celulares conectados al Wi-Fi o red local del establecimiento. El flujo
principal será tomar pedidos, enviar comandas a preparación, cobrar, controlar caja y consultar el
resultado diario. La base técnica podrá reutilizarse en otros negocios, pero las pantallas y las
decisiones de la primera versión priorizan el trabajo real de un restaurante.

## Estado de la edición básica

- Implementados: 12 de 12 bloques funcionales.
- Avance medido por bloques: 100 % de la implementación planificada.
- Falta la aceptación en un restaurante: prueba LAN con tablets, reinicio real de Windows y
  simulacro de restauración de una copia en una base distinta.

## Bloques terminados

### Bloque 1: negocio y catálogo

1. Flyway crea el esquema inicial.
2. La configuración identifica el tipo y la identidad del negocio.
3. El administrador registra categorías.
4. El administrador registra productos o servicios.
5. Cada elemento tiene precio, estado, disponibilidad y ruta operativa.
6. Angular permite crear, editar, activar y desactivar los registros.

### Bloque 2: acceso y usuarios

1. La primera instalación crea un propietario inicial.
2. Las contraseñas se almacenan mediante un codificador seguro.
3. El acceso utiliza una sesión local y protección CSRF.
4. El propietario administra usuarios, roles, contraseñas y estado.
5. Los permisos restringen configuración, catálogo y usuarios.
6. Angular protege las rutas y restaura la sesión al recargar.

### Bloque 3: estructura operativa

1. Cada instalación crea una sucursal principal sin perder la configuración existente.
2. El administrador registra sucursales y controla su orden y estado.
3. Cada sucursal organiza áreas como comedor, terraza, cocina, barra, almacén o caja.
4. Cada área contiene puntos de servicio como mesas, cajas o estaciones de preparación.
5. Los usuarios se asignan a las sucursales en las que podrán operar.
6. Las jerarquías y asignaciones se validan tanto en la API como en Angular.

### Bloque 4: mesas y apertura de pedidos

1. Flyway crea la tabla de pedidos y protege las relaciones con sucursal, punto y usuarios.
2. Cada usuario operativo ve únicamente las sucursales a las que fue asignado.
3. El mapa muestra las mesas y puntos libres u ocupados por área del restaurante.
4. Se abren pedidos para consumo en el local o para llevar, con folio, responsable, comensales,
   referencia del cliente y notas.
5. Una restricción de PostgreSQL impide dos pedidos activos sobre la misma mesa, incluso cuando
   dos tablets intentan abrirla al mismo tiempo.
6. Los pedidos avanzan de abierto a en atención, completado o cancelado; los estados terminales
   liberan la mesa.
7. El control de versión detecta actualizaciones realizadas desde otro dispositivo.
8. No se puede desactivar ni mover una mesa, área o sucursal mientras tenga pedidos abiertos.

### Bloque 5: contenido y total del pedido

1. El mesero entra a la captura desde una mesa ocupada o un pedido para llevar.
2. El catálogo táctil permite filtrar por categoría y buscar por nombre o SKU.
3. Cada partida guarda cantidad, indicaciones, precio y ruta operativa históricos.
4. El catálogo administra grupos y opciones para tamaño, término, guarniciones o extras.
5. Los grupos definen selecciones mínimas y máximas, validadas por Angular y por la API.
6. El backend calcula subtotal de productos, extras y total mediante `BigDecimal`.
7. Las partidas pueden editarse o retirarse mientras el pedido esté activo.
8. El control de versión evita sobrescribir cambios realizados desde otra tablet.
9. El primer producto mueve el pedido a en atención y un pedido vacío no puede completarse.

### Bloque 6: comandas y preparación

1. El mesero envía únicamente las partidas nuevas con ruta de cocina o servicio.
2. Un mismo envío genera comandas independientes para `PRODUCTION` y `SERVICE`.
3. Nombre, cantidad, modificadores e indicaciones quedan congelados al enviar.
4. Una partida enviada no puede editarse ni retirarse desde la cuenta.
5. Los productos agregados después generan una comanda adicional sin duplicar los anteriores.
6. La pantalla de preparación filtra por sucursal y por ruta operativa.
7. Cada partida avanza por pendiente, en preparación, lista y entregada, o se cancela de forma
   controlada.
8. El tablero se actualiza automáticamente cada 15 segundos y permite actualización manual.
9. El historial conserva las comandas entregadas o canceladas.
10. El control de versión evita que dos pantallas sobrescriban el estado de una partida.
11. No puede cerrarse un pedido con partidas sin enviar o todavía activas en preparación.

### Bloque 7: actualización en tiempo real

1. Spring Boot expone un canal STOMP sobre WebSocket en `/ws`.
2. Los avisos se separan por sucursal mediante `/topic/branches/{branchId}`.
3. Solo un usuario activo y asignado puede suscribirse al canal de una sucursal.
4. Los clientes no pueden publicar eventos operativos en el broker.
5. Los eventos se emiten después de confirmar la transacción de PostgreSQL.
6. Pedidos, captura y preparación vuelven a consultar REST al recibir un aviso; WebSocket no
   reemplaza la fuente de verdad.
7. Una comanda nueva aparece en preparación sin esperar el antiguo sondeo de 15 segundos.
8. Los cambios de pedido, mesa, partidas y preparación se reflejan en las demás tablets.
9. Angular reconecta automáticamente después de una pérdida momentánea de Wi-Fi.
10. La barra superior y Preparación muestran el estado de la conexión en tiempo real.
11. Preparación conserva una consulta de respaldo cada 60 segundos.

### Bloque 8: cobro y formas de pago

1. La pantalla de cobro muestra total, importe cobrado y saldo pendiente calculados por el backend.
2. Una cuenta acepta uno o varios cobros en efectivo, tarjeta, transferencia u otra forma descrita.
3. El efectivo registra importe recibido y calcula el cambio; los otros métodos admiten referencia.
4. El importe aplicado nunca puede superar el saldo pendiente.
5. Cada intento usa un identificador de operación para no duplicar el cobro al reintentar.
6. El pedido se bloquea durante el cobro para impedir sobrepagos desde dos cajas o tablets.
7. Después del primer cobro, los productos no se modifican hasta anular todos los cobros activos.
8. Las anulaciones conservan movimiento, usuario, fecha y motivo para auditoría.
9. Solo propietario, administrador, gerente o cajero puede anular un cobro.
10. El historial sirve como comprobante interno y puede imprimirse desde el navegador.
11. El pedido se cierra y la mesa se libera solo cuando el saldo es cero, todas las partidas fueron
    enviadas y preparación ya no tiene trabajo activo.
12. Los cambios de cobro notifican a las demás tablets mediante el canal en tiempo real.

### Bloque 9: apertura y cierre básico de caja

1. Cada sucursal mantiene como máximo un turno de caja abierto, compartido por las tablets del
   local.
2. Propietario, administrador, gerente o cajero abre la caja con fondo inicial y observaciones.
3. Todo cobro nuevo queda asociado al turno abierto; sin caja abierta no se registran pagos.
4. Efectivo, tarjeta, transferencia y otros métodos se totalizan por separado.
5. Entradas y salidas manuales guardan importe, concepto, responsable y hora.
6. Una salida no puede superar el efectivo esperado.
7. Los movimientos anulados permanecen en el historial con responsable, fecha y motivo.
8. El efectivo esperado combina fondo inicial, cobros en efectivo y movimientos manuales activos.
9. El cierre registra efectivo contado, esperado, diferencia, responsable y notas.
10. No se permite cerrar caja mientras una cuenta cobrada parcial o totalmente siga abierta.
11. Tampoco se puede desactivar una sucursal mientras su caja esté abierta.
12. Los cambios de caja y cobro se reflejan en las demás tablets mediante el canal en tiempo real.
13. La pantalla conserva los últimos 30 turnos y ofrece un corte imprimible desde el navegador.

### Bloque 10: reporte diario

1. Propietario, administrador y gerente consultan el resultado por sucursal y fecha.
2. El periodo se calcula con la zona horaria de la sucursal, aunque PostgreSQL conserve instantes
   en UTC.
3. Los cobros se separan en efectivo, tarjeta, transferencia y otros.
4. La venta neta distingue cobros recibidos y anulaciones realizadas durante el día.
5. La gráfica por hora utiliza la hora local del restaurante.
6. El resumen muestra pedidos abiertos, completados, cancelados, en mesa y para llevar, comensales
   y ticket promedio.
7. El top de productos usa cantidad, precio y modificadores históricos.
8. Caja presenta fondos iniciales, cobros en efectivo, entradas, salidas, flujo esperado y
   diferencia de cortes.
9. Los turnos relacionados conservan apertura, cierre, esperado, contado y diferencia.
10. La auditoría muestra pedidos cancelados y cobros anulados con usuario, fecha y motivo cuando
    existe.
11. El reporte puede exportarse a CSV o imprimirse/guardarse como PDF desde el navegador.
12. La vista se actualiza mediante WebSocket y permite una consulta manual de respaldo.
13. La migración V9 agrega índices de consulta sin alterar los datos históricos.

### Bloque 11: respaldo y restauración guiados

1. Solo el propietario puede descargar una copia completa desde Essential.
2. El servidor usa las herramientas oficiales de PostgreSQL en formato custom, valida el archivo
   y lo empaqueta con manifiesto y huella SHA-256.
3. La restauración comprueba integridad y crea una base de prueba sin alterar la original.
4. La promoción requiere intervención explícita, backend detenido, ausencia de conexiones y
   respaldo adicional; conserva la base anterior para permitir revisión o reversión.
5. Angular usa rutas API relativas al origen para sesiones/CSRF y acceso desde tablets.
6. Una guía de simulacro documenta copia diaria, almacenamiento separado y recuperación.

### Bloque 12: instalación en LAN y tablets

1. La compilación Maven y Angular publica una versión validada en el servidor Windows.
2. Caddy sirve la interfaz mediante HTTPS en una dirección privada y reenvía API y WebSocket
   al backend en loopback.
3. Java queda limitado a `127.0.0.1:3211`; la regla de firewall solo permite HTTPS desde la
   subred privada local.
4. Dos tareas de Windows arrancan backend y proxy al encender el servidor y reintentan si el
   proceso o PostgreSQL tarda en iniciar.
5. La guía cubre certificado raíz local, acceso y prueba de tiempo real desde tablets, caída
   de internet, reinicio del servidor y simulacro de recuperación.

### Roles disponibles

| Rol        | Uso previsto                                                     |
| ---------- | ---------------------------------------------------------------- |
| `OWNER`    | Propietario de la instalación; controla usuarios y configuración |
| `ADMIN`    | Administración de configuración y catálogo                       |
| `MANAGER`  | Supervisión operativa                                            |
| `CASHIER`  | Apertura, movimientos y cierre de caja                           |
| `OPERATOR` | Operación diaria                                                 |

## Reglas incorporadas

- Los importes utilizan `NUMERIC(12,2)` y `BigDecimal`.
- Los nombres de categorías son únicos sin distinguir mayúsculas.
- Los SKU son opcionales, pero son únicos cuando se proporcionan.
- Un elemento agotado continúa activo en el catálogo.
- Un elemento inactivo queda fuera de la operación.
- Los registros utilizan control de versión para detectar modificaciones simultáneas.
- Los datos no se eliminan físicamente desde la interfaz.
- Hibernate valida el esquema y Flyway controla sus cambios.
- La interfaz no guarda contraseñas ni credenciales de PostgreSQL.
- Las contraseñas contienen de 10 a 72 caracteres, al menos una letra y al menos un número.
- Angular explica los errores de validación y Spring Boot aplica las mismas reglas.

## Orden de desarrollo

| Bloque | Alcance                                                            | Estado    |
| ------ | ------------------------------------------------------------------ | --------- |
| 1      | Configuración, categorías y productos o servicios                  | Terminado |
| 2      | Usuarios, contraseñas, roles y permisos                            | Terminado |
| 3      | Sucursales, áreas operativas y puntos de atención                  | Terminado |
| 4      | Apertura de pedido o comanda desde mesa, barra o mostrador         | Terminado |
| 5      | Productos, cantidades, variantes, modificadores y notas del pedido | Terminado |
| 6      | Pantalla de cocina/barra y estados por partida                     | Terminado |
| 7      | Notificaciones en tiempo real                                      | Terminado |
| 8      | Cobro y formas de pago                                             | Terminado |
| 9      | Apertura y cierre básico de caja                                   | Terminado |
| 10     | Reporte diario                                                     | Terminado |
| 11     | Respaldos y restauración                                           | Implementado |
| 12     | Instalación en la red local                                        | Implementado |

## Decisiones vigentes

- La edición básica funciona dentro de un restaurante mediante un servidor local y tablets o
  computadoras conectadas a su Wi-Fi/LAN; no requiere internet ni una WAN para operar.
- Una instalación y base de datos por restaurante en todas las ediciones. Otro local utiliza
  su propia copia; no se conectan ni comparten catálogo, usuarios, pedidos o reportes.
- Se conserva un identificador interno único para respetar pedidos y caja existentes. No se
  pueden registrar otras sucursales ni desactivar el restaurante local.
- Angular y Spring Boot se desarrollan mediante funciones completas.
- REST realiza las operaciones y WebSocket transporta avisos en tiempo real.
- PostgreSQL siempre será la fuente de verdad.
- El modelo interno conserva conceptos reutilizables, pero la experiencia de usuario prioriza
  restaurantes: mesas, pedidos, comandas, cocina, cobro y caja.
- La edición básica no incluye CFDI/SAT, nube pública, inventario por receta, clientes frecuentes,
  reservaciones ni integraciones de reparto; se consideran para ediciones posteriores.

## Siguiente etapa: piloto de la edición local

Ejecutar `PRUEBA-BLOQUE-12.md` en un servidor Windows con dos tablets, verificar el canal WSS,
reiniciar el equipo y ensayar la restauración del respaldo en una base de prueba. Registrar los
problemas observados antes de ofrecer la edición básica a un restaurante.

Consulta `PLANES-Y-ALCANCE.md` para la separación propuesta de las tres ediciones.
