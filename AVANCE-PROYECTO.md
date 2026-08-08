# Avance de Essential

## Objetivo

Construir un sistema de operación para restaurantes pequeños y medianos, accesible desde
computadoras, tablets y celulares conectados al Wi-Fi o red local del establecimiento. El flujo
principal será tomar pedidos, enviar comandas a preparación, cobrar, controlar caja y consultar el
resultado diario. La base técnica podrá reutilizarse en otros negocios, pero las pantallas y las
decisiones de la primera versión priorizan el trabajo real de un restaurante.

## Estado de la edición básica

- Terminados: 4 de 12 bloques funcionales.
- Avance medido por bloques: 33 %.
- Avance práctico estimado: entre 35 % y 40 %, porque ya existe la base de datos, seguridad,
  catálogo, usuarios, estructura y apertura de pedidos desde tablets.
- Restan 8 bloques. Los más importantes todavía son productos y modificadores dentro del pedido,
  comandas de cocina, actualización en tiempo real, cobro, caja, reporte, respaldos e instalación.

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
| 4      | Apertura de pedido o comanda desde mesa, barra o mostrador         | Terminado |
| 5      | Productos, cantidades, variantes, modificadores y notas del pedido | Siguiente |
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

## Próximo bloque: contenido del pedido

El siguiente desarrollo completará la captura que realiza el mesero después de abrir una mesa:

- Entrar a un pedido abierto desde su mesa o tarjeta.
- Buscar productos por categoría desde una interfaz cómoda para tablet.
- Agregar cantidades, precio capturado y observaciones por partida.
- Preparar el modelo de variantes y modificadores, por ejemplo término, tamaño o extras.
- Calcular subtotal y total del pedido con `BigDecimal` y `NUMERIC`.
- Evitar que se agreguen productos inactivos o no disponibles.

El Bloque 6 enviará las partidas a cocina o barra y administrará su preparación. Consulta
`PLANES-Y-ALCANCE.md` para la separación propuesta de las tres ediciones.
