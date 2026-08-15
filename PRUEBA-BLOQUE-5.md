# Prueba del Bloque 5: captura y total del pedido

Este bloque agrega la migración `V5__create_order_items_and_modifiers.sql`. No ejecutes el SQL
manualmente: Spring Boot y Flyway lo aplican al iniciar sin borrar los datos anteriores.

## 1. Actualizar y compilar

1. Detén Angular y Spring Boot.
2. Conserva `SystemRestaurantBack/secret.yml`.
3. Reemplaza el proyecto con esta versión y vuelve a colocar `secret.yml`.
4. Desde `SystemRestaurantBack` ejecuta:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

5. Desde `SystemRestaurantFront` ejecuta:

```powershell
npm ci
npm test -- --watch=false
npm start
```

## 2. Configurar modificadores

Desde una cuenta propietaria o administradora:

1. Abre **Catálogo** y confirma que existan productos activos y disponibles.
2. Baja a **Modificadores de productos**.
3. Crea para una hamburguesa el grupo `Término`, mínimo `1` y máximo `1`.
4. Agrega las opciones `Medio`, `Tres cuartos` y `Bien cocido`, todas con cambio de precio `$0`.
5. Crea el grupo `Extras`, mínimo `0` y máximo `3`.
6. Agrega `Queso extra` con `$20` y `Tocino` con `$30`.
7. Comprueba que los grupos y opciones permitan editarse, activarse y desactivarse.

## 3. Capturar un pedido

1. Abre **Pedidos** y pulsa **Abrir pedido** en una mesa libre.
2. Confirma que Essential entre automáticamente a la captura del pedido.
3. Filtra por categoría o busca por nombre/SKU.
4. Toca la hamburguesa.
5. Intenta guardarla sin término: debe explicar que falta la selección obligatoria.
6. Selecciona un término, agrega dos extras, cantidad `2` y una nota como `Sin cebolla`.
7. Pulsa **Agregar al pedido**.
8. Confirma que aparezcan cantidad, modificadores, nota y total de la partida.
9. Agrega otro producto sin modificadores.

## 4. Probar edición, retiro y precio histórico

1. Edita una partida, cambia su cantidad y guarda.
2. Retira otra partida y confirma el aviso.
3. Verifica que productos, extras y total se recalculen correctamente.
4. Sin cerrar el pedido, entra a **Catálogo** desde otra sesión administradora y cambia el precio
   del producto.
5. Vuelve al pedido y actualiza: la partida ya capturada debe conservar su precio anterior.
6. Agrega nuevamente el mismo producto: la nueva partida debe usar el precio nuevo.

## 5. Probar dos tablets

1. Abre el mismo pedido en dos navegadores o tablets.
2. En el primer dispositivo agrega un producto.
3. Sin actualizar el segundo, intenta agregar otro.
4. El segundo debe recibir el aviso de que el pedido cambió en otro dispositivo.
5. Pulsa **Actualizar** y repite la operación.

## Resultado esperado

- Un operador autenticado puede consultar el catálogo para tomar pedidos, pero no administrarlo.
- No se agregan productos inactivos o agotados.
- El backend valida mínimos y máximos de modificadores y calcula todos los importes.
- Precio, nombre, ruta y modificadores quedan congelados en cada partida.
- El primer producto mueve automáticamente el pedido a **En atención**.
- No se puede completar un pedido vacío.
- Flyway registra V5 sin perder mesas, pedidos, usuarios ni catálogo existentes.

El Bloque 6 utilizará estas partidas para generar comandas y la pantalla de cocina o barra.
