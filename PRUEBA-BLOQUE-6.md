# Prueba del Bloque 6: comandas y preparación

Este bloque agrega la migración `V6__create_preparation_tickets.sql`. No ejecutes el SQL
manualmente: Spring Boot y Flyway lo aplican al iniciar sin borrar los datos anteriores.

## 1. Actualizar y compilar

1. Confirma que estás trabajando en la rama `desarrollo`.
2. Detén Angular y Spring Boot.
3. Conserva `SystemRestaurantBack/secret.yml`; el ZIP no contiene credenciales.
4. Reemplaza los archivos del proyecto y vuelve a colocar `secret.yml`.
5. Desde `SystemRestaurantBack` ejecuta:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

6. Confirma en la consola que Flyway aplicó V6 y que el backend inició en el puerto `3211`.
7. Desde `SystemRestaurantFront` ejecuta:

```powershell
npm ci
npm run build
npm test -- --watch=false
npm start
```

## 2. Preparar productos con rutas diferentes

1. Inicia sesión con propietario o administrador y abre **Catálogo**.
2. Confirma que un producto, por ejemplo `Hamburguesa`, tenga ruta **Producción o preparación**.
3. Confirma que otro producto, por ejemplo `Refresco`, tenga ruta **Barra o servicio**.
4. Deja un tercer producto con ruta **Sin preparación**, por ejemplo un cargo de servicio.
5. Verifica que los productos estén activos y disponibles.

## 3. Enviar una comanda desde una tablet

1. Abre **Pedidos**, selecciona una mesa libre y crea el pedido.
2. Captura una hamburguesa con modificadores y una nota como `Sin cebolla`.
3. Captura un refresco con una nota como `Sin hielo`.
4. Captura el producto sin preparación.
5. Comprueba que el botón **Enviar comanda** muestre `2`: el producto sin ruta no se envía.
6. Pulsa el botón, lee la confirmación y acepta.
7. Comprueba el mensaje de éxito.
8. Las dos partidas enviadas deben mostrar **Enviada** y ya no deben mostrar editar ni retirar.
9. El producto sin preparación debe continuar editable mientras el pedido esté activo.

## 4. Operar cocina y servicio

1. En otra computadora o tablet inicia sesión con un usuario asignado a la misma sucursal.
2. Abre **Preparación**.
3. Filtra por **Cocina**: debe aparecer la hamburguesa, sus modificadores y la nota.
4. Filtra por **Barra / servicio**: debe aparecer el refresco y su nota.
5. Pulsa **Comenzar** en una partida: debe pasar a **En preparación**.
6. Pulsa **Marcar lista**: debe pasar a **Listo para entregar**.
7. Pulsa **Entregada**: debe salir del tablero activo.
8. Abre **Ver historial** y confirma que la partida aparezca como entregada.
9. Repite con la otra ruta.

## 5. Probar una comanda adicional

1. Regresa al pedido original.
2. Agrega un producto nuevo con ruta de cocina.
3. El botón debe indicar `1`; los productos del primer envío no deben contarse otra vez.
4. Envía la nueva partida.
5. En Preparación debe aparecer como `envío 2` para esa ruta y no debe duplicar el envío anterior.

## 6. Probar cancelación y protección

1. Envía otra partida nueva.
2. En Preparación pulsa **Cancelar**, confirma y revisa el historial.
3. Abre el mismo pedido en dos navegadores.
4. Avanza una partida en el primer navegador.
5. Sin actualizar el segundo, intenta avanzar la misma partida.
6. El segundo debe avisar que la partida cambió en otra pantalla.
7. Intenta completar un pedido mientras una partida esté pendiente, en preparación o lista.
8. Essential debe impedirlo hasta que todas estén entregadas o canceladas.
9. Intenta cancelar todo el pedido con una partida activa: también debe pedir que primero se
   cancelen las partidas desde Preparación.

## 7. Probar tablet y actualización automática

1. Abre Preparación en una tablet horizontal y comprueba que los botones sean cómodos de tocar.
2. Reduce el navegador a tamaño de teléfono: las columnas deben apilarse sin sacar controles del
   contenedor.
3. Mantén Preparación abierta y envía una comanda desde otro dispositivo.
4. Sin pulsar **Actualizar**, debe aparecer en un máximo aproximado de 15 segundos.
5. El Bloque 7 reducirá esta espera mediante WebSocket; el sondeo actual es intencional.

## Resultado esperado

- Cocina y servicio reciben comandas independientes.
- Nunca se duplica una partida ya enviada.
- El contenido enviado queda congelado aunque después cambie el catálogo.
- Una adición posterior crea otra comanda.
- Los estados se controlan por partida y no por toda la comanda.
- Los tiempos de envío, inicio, listo, entrega o cancelación se conservan.
- Un usuario solo consulta sucursales a las que está asignado.
- Flyway registra V6 sin perder catálogo, usuarios, estructura, pedidos ni partidas existentes.
