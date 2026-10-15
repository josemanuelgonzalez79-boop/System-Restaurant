# Essential Pro · Bloque 2: fichas de clientes

Base verificada: rama `pro`, commit `36e070b` (reservaciones y llegada a mesa). Este paquete
contiene el proyecto completo para sustituir tus archivos locales en **`pro`**. Conserva el
archivo `SystemRestaurantBack/secret.yml` y no reemplaces tu base PostgreSQL por una vacía.

## Preparación

1. Respalda la base y detén backend y frontend.
2. Copia el contenido del ZIP sobre el proyecto de la rama `pro`. Mantén tu `secret.yml`.
3. Con JDK 21: `cd SystemRestaurantBack; .\mvnw.cmd clean test; .\mvnw.cmd spring-boot:run`.
4. En otra terminal: `cd SystemRestaurantFront; npm ci; npm run build; npm test -- --watch=false; npm start`.
5. Flyway aplicará `V17__create_local_customers.sql` al arrancar. No ejecutes el SQL a mano.
   Comprueba que aparezcan V16 y V17 en `flyway_schema_history`.

## Recorrido de prueba

1. Entra como propietario, administrador, gerente o cajero y abre **Clientes**. Crea a Ana con
   teléfono `667 123 4567`, correo opcional, preferencia de mesa y una indicación alimentaria.
   Verifica que la ficha aparezca en el directorio y registre quién la creó.
2. Intenta crear otra ficha con `667-123-4567`: debe rechazarla por teléfono repetido. Busca por
   `Ana` y también por los dígitos del teléfono. Edita una preferencia y verifica el evento.
3. En dos tablets, abre la misma ficha. Guarda cambios en la primera y luego en la segunda: la
   segunda debe recibir conflicto de versión. Actualiza la ficha antes de reintentar.
4. En **Reservaciones**, busca la ficha de Ana, vincúlala a una nueva reserva y comprueba que
   nombre y teléfono se tomen del perfil. Puedes registrar otra reserva sin ficha.
5. Dentro del horario permitido, pulsa **Sentar y abrir pedido** en la reserva vinculada.
   Captura un producto, termina la preparación y paga con el flujo habitual. En **Clientes**,
   comprueba que el historial muestre la reserva y el pedido vinculado una sola vez.
6. Abre otro pedido, incluso **Para llevar**, eligiendo la ficha desde el diálogo de Pedidos.
   El historial debe agregar ese pedido. El nombre del cliente es visible como referencia del
   pedido; teléfono, correo y preferencias siguen restringidos al directorio autorizado.
7. Entra como **operador**: no debe aparecer la sección Clientes ni estar disponible su API.
   Un operador tampoco puede enviar `customerId` o `reservationId` al abrir pedidos.
8. Archiva la ficha. Su historial debe conservarse y no debe aparecer como opción para una
   cuenta nueva. Reactívala y verifica que vuelva a poder seleccionarse. Una reservación ya
   confirmada mantiene su nombre y teléfono originales aunque la ficha cambie después.

## Alcance y cuidado de los datos

- Los clientes existentes en reservas o pedidos anteriores a V17 no se vinculan automáticamente:
  nombres coincidentes no demuestran que sean la misma persona. Puedes vincular una reserva aún
  pendiente al editarla y seleccionar una ficha.
- El teléfono es opcional; cuando existe debe tener entre 7 y 15 dígitos. Se almacena el valor
  visible junto a una clave normalizada única. La búsqueda devuelve un máximo de 100 resultados.
- Las indicaciones alimentarias deben confirmarse con el cliente en cada visita y anotarse en el
  pedido cuando correspondan. No se copian automáticamente a una comanda de cocina.
- Archivar oculta la ficha de nuevas selecciones sin eliminar reservas ni pedidos anteriores.
  No se envían correos, mensajes ni campañas en este bloque.
- Una instalación sigue correspondiendo a un único restaurante, con sus propios clientes y BD.

Este ZIP no modifica GitHub. Tras probarlo, súbelo tú a `pro`.
