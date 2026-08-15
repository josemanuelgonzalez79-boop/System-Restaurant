# Prueba del Bloque 7: actualización en tiempo real

Este bloque no agrega tablas ni una migración de Flyway. Incorpora STOMP sobre WebSocket para
avisar cambios entre tablets, mientras REST y PostgreSQL continúan siendo la fuente de verdad.

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

6. Confirma que el backend inició en el puerto `3211`.
7. Desde `SystemRestaurantFront` ejecuta:

```powershell
npm ci
npm run build
npm test -- --watch=false
npm start
```

## 2. Corregir visualmente Catálogo

1. Abre **Catálogo** y baja hasta **Modificadores de productos**.
2. Comprueba los campos **Mínimo**, **Máximo** y **Orden** del formulario **Nuevo grupo**.
3. Ningún campo debe salir de su tarjeta ni invadir **Nueva opción**.
4. Reduce el ancho del navegador: primero deben conservarse dentro de la tarjeta y después
   apilarse en una columna en tamaño móvil.

## 3. Probar una comanda inmediata

1. Abre Essential en dos navegadores o dispositivos conectados a la misma red local.
2. Inicia sesión con usuarios activos asignados a la misma sucursal.
3. En el primer dispositivo abre **Preparación**.
4. En el segundo abre una mesa, captura un producto con ruta de cocina y envía la comanda.
5. La comanda debe aparecer en Preparación casi de inmediato, sin pulsar **Actualizar** y sin
   esperar 15 segundos.
6. La barra superior debe mostrar un punto verde y el texto **Tiempo real**.

## 4. Probar cambios entre cocina y mesero

1. Mantén el pedido abierto en el segundo dispositivo.
2. Desde Preparación cambia la partida a **En preparación** y después a **Lista**.
3. El detalle del pedido debe actualizarse automáticamente.
4. Marca la partida como **Entregada**.
5. Comprueba el historial de Preparación y completa el pedido.
6. En otra pantalla de **Pedidos**, la mesa debe volver a mostrarse libre sin recargar el navegador.

## 5. Probar reconexión

1. Mantén abierta Preparación y detén Spring Boot.
2. El indicador debe cambiar a **Reconectando…** o **Sin conexión**.
3. Inicia nuevamente Spring Boot sin recargar Angular.
4. En pocos segundos el indicador debe volver a **Tiempo real**.
5. Envía otra comanda desde el segundo dispositivo y confirma que vuelva a aparecer inmediatamente.
6. Preparación conserva además una consulta de respaldo cada 60 segundos.

## 6. Probar aislamiento por sucursal

1. Si tienes dos sucursales, usa un operador asignado únicamente a la primera.
2. Confirma que solo pueda seleccionar y recibir cambios de esa sucursal.
3. Usa un usuario asignado a la segunda y confirma que sus eventos no actualicen la primera.
4. El backend rechaza suscripciones a sucursales no asignadas y también impide que un navegador
   publique eventos operativos.

## 7. Probar desde tablets en la LAN

Si Angular se abre mediante una IP distinta de `localhost`, incluye su origen exacto en
`app.cors.allowed-origins` dentro de `SystemRestaurantBack/secret.yml`, por ejemplo:

```yaml
app:
  cors:
    allowed-origins:
      - http://localhost:4200
      - http://192.168.1.50:4200
```

Reinicia Spring Boot después del cambio. El servidor, las tablets y la pantalla de cocina deben
estar en la misma Wi-Fi/LAN. La edición local no requiere WAN ni debe exponerse directamente a
internet.

## Resultado esperado

- Los controles de modificadores permanecen dentro de su tarjeta.
- Cocina recibe comandas nuevas casi de inmediato.
- Meseros, mapa de mesas y preparación reflejan cambios de la sucursal sin recargar.
- La pérdida temporal del Wi-Fi no obliga a cerrar sesión ni recargar Angular.
- Cada usuario recibe únicamente los eventos de sucursales asignadas.
- Un aviso WebSocket nunca sustituye la validación ni los datos entregados por la API REST.
