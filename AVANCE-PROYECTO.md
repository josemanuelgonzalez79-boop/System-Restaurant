# Avance de Essential

## Objetivo

Construir una plataforma local y configurable para restaurantes y otros negocios, accesible
desde computadoras, tablets y celulares conectados a la red interna. La misma base de código se
utiliza para todos los clientes; cada instalación conserva su configuración y su base de datos.

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
3. Cada sucursal organiza áreas genéricas de servicio, producción, almacén, caja u oficina.
4. Cada área contiene puntos de atención como mesas, cajas, estaciones, consultorios o ventanillas.
5. Los usuarios se asignan a las sucursales en las que podrán operar.
6. Las jerarquías y asignaciones se validan tanto en la API como en Angular.

### Roles disponibles

| Rol | Uso previsto |
| --- | --- |
| `OWNER` | Propietario de la instalación; controla usuarios y configuración |
| `ADMIN` | Administración de configuración y catálogo |
| `MANAGER` | Supervisión operativa |
| `CASHIER` | Caja y cobros futuros |
| `OPERATOR` | Operación diaria |

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

## Orden de desarrollo

| Bloque | Alcance | Estado |
| --- | --- | --- |
| 1 | Configuración, categorías y productos o servicios | Terminado |
| 2 | Usuarios, contraseñas, roles y permisos | Terminado |
| 3 | Sucursales, áreas operativas y puntos de atención | Terminado |
| 4 | Apertura de operación y captura de movimientos | Siguiente |
| 5 | Partidas, variantes, modificadores y observaciones | Pendiente |
| 6 | Tablero operativo y estados por partida | Pendiente |
| 7 | Notificaciones en tiempo real | Pendiente |
| 8 | Cobro y formas de pago | Pendiente |
| 9 | Apertura y cierre básico de caja | Pendiente |
| 10 | Reporte diario | Pendiente |
| 11 | Respaldos y restauración | Pendiente |
| 12 | Instalación en la red local | Pendiente |

## Decisiones vigentes

- Una instalación y base de datos por negocio durante la primera versión; sus sucursales comparten catálogo y usuarios.
- Angular y Spring Boot se desarrollan mediante funciones completas.
- REST realiza las operaciones y WebSocket se agregará para avisos en tiempo real.
- PostgreSQL siempre será la fuente de verdad.
- El núcleo evita términos exclusivos de restaurantes.
- No se incluye CFDI, SAT, PLC, automatización industrial, nube pública ni WhatsApp estándar.
- Inventario, recetas, clientes frecuentes y reservaciones se agregarán como módulos posteriores.

## Próximo bloque

Abrir y registrar la operación diaria sin atarla a un solo giro:

- Apertura de una jornada o turno por sucursal.
- Selección del punto donde se captura el movimiento.
- Encabezado del movimiento con responsable, fecha y estado.
- Folio consecutivo local.
- Estados iniciales: abierto, en proceso, completado y cancelado.
