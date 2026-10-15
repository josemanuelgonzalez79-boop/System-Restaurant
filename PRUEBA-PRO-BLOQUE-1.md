# Essential Pro · Bloque 1: reservaciones y llegada a mesa

Este paquete parte del **Plus completo (V15)**. Verifiqué que la rama remota `plus` ya contiene
el bloque 5 en el commit `a66bb5f` ("reportes"). **Crea `pro` desde el último commit de `plus`**:

```powershell
git switch plus
git pull origin plus
git switch -c pro
git push -u origin pro
```

Si ya creaste `pro` desde un commit anterior, incorpora primero los cambios de `plus` en `pro`
antes de copiar este paquete. Conserva tu `secret.yml`.

## Instalación

1. Respalda PostgreSQL. Detén ambos servidores y conserva
   `SystemRestaurantBack/secret.yml`.
2. Copia este proyecto completo a tu rama local `pro`. No ejecutes migraciones SQL manualmente:
   Flyway aplicará **V16 después de V15** al iniciar.
3. Con JDK 21, en `SystemRestaurantBack`:

   ```powershell
   .\mvnw.cmd clean test
   .\mvnw.cmd spring-boot:run
   ```

4. En `SystemRestaurantFront`:

   ```powershell
   npm ci
   npm run build
   npm test -- --watch=false
   npm start
   ```

5. Si Flyway indica que no puede crear la extensión `btree_gist`, concede al usuario propietario
   de la base permisos para esa extensión en tu PostgreSQL local. No edites ni marques V16 como
   aplicada sin resolver el error.

## Prueba funcional

1. En **Estructura** prepara dos puntos activos de tipo **Mesa** dentro de un área activa.
2. Abre **Reservaciones** como propietario, administrador, gerente o cajero. Elige mañana a
   las 19:00, una duración de 90 minutos, mesa 1, nombre y dos comensales. Guárdala.
3. Intenta guardar otra en mesa 1 desde las 20:00 hasta las 21:00. Debe rechazarla por traslape.
   A las 20:30 debe poder reservarse la mesa 1 (los extremos contiguos no se traslapan).
   Una reserva para mesa 2 a las 20:00 también debe permitirse.
4. Edita la primera, cambia hora o mesa y confirma que el historial registre la modificación.
   Con dos tablets abiertas, intenta cambiar la misma reserva desde ambas: la segunda debe recibir
   conflicto de versión, actualizar la agenda y volver a intentarlo.
5. Cancela una reserva indicando motivo (mínimo cinco caracteres). El horario queda disponible;
   el historial conserva el responsable, la fecha y el motivo.
6. Para probar **Sentar y abrir pedido**, crea una reserva a partir de 30 minutos desde ahora.
   Pulsa la acción cuando entre en la ventana permitida: desde 30 minutos antes hasta antes del
   fin del intervalo. Se debe abrir `PED-...` en esa mesa, con nombre y número de comensales.
   La reserva pasa a **Sentada** y muestra el vínculo al pedido.
7. Con otra reserva pendiente para pronto, intenta abrir un pedido normal desde **Pedidos** en
   esa mesa. Si su intervalo inicia dentro de los próximos 90 minutos, el mapa debe mostrarla
   **Reservada** e impedir **Abrir pedido**. Desde **Reservaciones** sí podrás sentar al cliente.
8. Cuando hayan pasado 15 minutos de la hora de inicio de una reserva pendiente, pulsa **No
   asistió**: su intervalo queda libre y se conserva el evento auditado.
9. Con una reserva futura pendiente, intenta desactivar o mover su mesa/área desde Estructura;
   debe impedirlo hasta cancelar la reservación.

## Límites deliberados del primer bloque

- Reservaciones solo para **mesa o salón**, en un único restaurante local.
- Horarios en intervalos de 15 minutos, duración de 30 a 240 minutos y fecha dentro de 90 días.
- El teléfono es opcional. No se envían SMS, WhatsApp o correos automáticamente.
- El bloque no define aforo por mesa ni horarios comerciales: recepción debe elegir una mesa y
  hora acordes a la operación del restaurante.
- El histórico y el pedido del cliente se vinculan al sentarlo; el bloque 2 abordará fichas de
  clientes. No crea una base de datos central para varias instalaciones.
- La reserva sentada conserva su intervalo hasta la hora fin programada; el pedido sigue sus
  propias reglas de pago, preparación y liberación de mesa.

## Validación antes de publicarlo

Confirma que `V15` y `V16` aparezcan en `flyway_schema_history`, que no haya nuevas reservas
traslapadas y que `mvnw clean test` termine bien en tu máquina. Este paquete no fue publicado en
GitHub por el asistente.
