# Prueba de los ajustes de experiencia de usuario

Estos cambios no agregan migraciones de base de datos. Conserva tu archivo
`SystemRestaurantBack/secret.yml` al reemplazar el proyecto.

## 1. Compilación

Backend, desde `SystemRestaurantBack`:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Frontend, desde `SystemRestaurantFront`:

```powershell
npm ci
npm test -- --watch=false
npm start
```

## 2. Mensajes de contraseña

En **Usuarios**, intenta crear un usuario con cada caso:

| Contraseña de prueba | Resultado esperado                         |
| -------------------- | ------------------------------------------ |
| Vacía                | Indica que es obligatoria                  |
| `corta1`             | Indica que necesita al menos 10 caracteres |
| `sololetras`         | Indica que falta al menos un número        |
| `1234567890`         | Indica que falta al menos una letra        |
| `Restaurante2026`    | Permite crear el usuario                   |

Repite la prueba en **Restablecer contraseña**. Al presionar el botón con un formulario inválido
debe aparecer una advertencia y el mensaje específico debajo del campo.

## 3. Contención de formularios

1. Abre **Estructura** con el tema claro y el oscuro.
2. Revisa los tres formularios: sucursal, área y punto.
3. Confirma que `Orden`, `Sucursal`, `Tipo` y los demás inputs permanecen dentro de cada tarjeta.
4. Reduce el ancho de la ventana y confirma que las tarjetas pasan a dos columnas y después a una.

## 4. Regresión rápida

- Crear un usuario con contraseña válida.
- Editar su nombre y rol.
- Restablecer su contraseña.
- Crear o editar una sucursal, un área y una mesa.
- Cerrar sesión y entrar con la nueva contraseña.

Cuando estas pruebas funcionen, el siguiente bloque es el mapa de mesas y la apertura de pedidos
desde tablet, barra, mostrador o para llevar.
