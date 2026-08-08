# Avance de Essential

## Objetivo

Construir un sistema de operación para restaurantes pequeños y medianos, accesible desde
computadoras, tablets y celulares conectados al Wi-Fi o red local del establecimiento. El flujo
principal será tomar pedidos, enviar comandas a preparación, cobrar, controlar caja y consultar el
resultado diario. La base técnica podrá reutilizarse en otros negocios, pero las pantallas y las
decisiones de la primera versión priorizan el trabajo real de un restaurante.

## Estado de la edición básica

- Terminados: 3 de 12 bloques funcionales.
- Avance medido por bloques: 25 %.
- Avance práctico estimado: entre 25 % y 30 %, porque ya existe la base de datos, seguridad,
  catálogo, usuarios y estructura.
- Restan 9 bloques. Los más importantes todavía son pedidos desde tablet, comandas de cocina,
  actualización en tiempo real, cobro, caja, reporte, respaldos e instalación en red local.

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

### Roles disponibles

| Rol        | Uso previsto                                                     |
| ---------- | ---------------------------------------------------------------- |
| `OWNER`    | Propietario de la instalación; controla usuarios y configuración |
| `ADMIN`    | Administración de configuración y catálogo                       |
| `MANAGER`  | Supervisión operativa                                            |
| `CASHIER`  | Caja y cobros futuros                                            |
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
| 4      | Apertura de pedido o comanda desde mesa, barra o mostrador         | Siguiente |
| 5      | Productos, cantidades, variantes, modificadores y notas del pedido | Pendiente |
| 6      | Pantalla de cocina/barra y estados por partida                     | Pendiente |
| 7      | Notificaciones en tiempo real                                      | Pendiente |
| 8      | Cobro y formas de pago                                             | Pendiente |
| 9      | Apertura y cierre básico de caja                                   | Pendiente |
| 10     | Reporte diario                                                     | Pendiente |
| 11     | Respaldos y restauración                                           | Pendiente |
| 12     | Instalación en la red local                                        | Pendiente |

## Decisiones vigentes

- La edición básica funciona dentro de un restaurante mediante un servidor local y tablets o
  computadoras conectadas a su Wi-Fi/LAN; no requiere internet ni una WAN para operar.
- La entidad sucursal se conserva desde ahora. En la edición básica se recomienda una instalación
  por ubicación física; comunicar ubicaciones remotas pertenecerá a la edición multi-sucursal.
- Una edición multi-sucursal necesitará conectividad WAN segura mediante VPN o una arquitectura
  central en la nube. Nunca se expondrá directamente el servidor local a internet.
- Una instalación y base de datos por restaurante durante la primera edición; dentro de ella se
  comparten catálogo y usuarios.
- Angular y Spring Boot se desarrollan mediante funciones completas.
- REST realiza las operaciones y WebSocket se agregará para avisos en tiempo real.
- PostgreSQL siempre será la fuente de verdad.
- El modelo interno conserva conceptos reutilizables, pero la experiencia de usuario prioriza
  restaurantes: mesas, pedidos, comandas, cocina, cobro y caja.
- La edición básica no incluye CFDI/SAT, nube pública, inventario por receta, clientes frecuentes,
  reservaciones ni integraciones de reparto; se consideran para ediciones posteriores.

## Próximo bloque: pedidos desde el restaurante

El siguiente desarrollo debe permitir iniciar el trabajo que hará un mesero desde una tablet:

- Mostrar las mesas y puntos de servicio disponibles por área.
- Abrir un pedido para una mesa, barra, mostrador o pedido para llevar.
- Asignar folio, sucursal, punto, mesero, fecha y número de comensales.
- Consultar pedidos abiertos y recuperar uno para continuar capturándolo.
- Manejar los estados iniciales `OPEN`, `IN_PROGRESS`, `COMPLETED` y `CANCELLED`.

El Bloque 5 agregará productos, cantidades, modificadores y notas; el Bloque 6 enviará las partidas
a cocina o barra. Consulta `PLANES-Y-ALCANCE.md` para la separación propuesta de las tres ediciones.
