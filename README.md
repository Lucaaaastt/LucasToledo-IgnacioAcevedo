# MarcelaPazKT

Aplicacion Android para organizar las reservas de espacios de la escuela Marcela Paz y evitar cruces de horario.

## Integrantes

- Lucas Toledo —
- Ignacio Acevedo —

## Funcionalidades

- Registro e inicio de sesion.
- Solicitud de espacios con validacion de fecha y horario.
- Consulta de reservas personales.
- Aprobacion y rechazo de solicitudes por el administrador.
- Notificaciones locales.
- Almacenamiento local con Room sobre SQLite.

## Flujo de uso

1. El solicitante inicia sesion.
2. Selecciona el espacio, la fecha, el horario y el motivo.
3. La solicitud queda pendiente y bloquea ese horario.
4. El administrador revisa y aprueba o rechaza la solicitud.
5. El solicitante consulta el resultado en Mis reservas. Si se rechaza, se libera el horario.

## Arquitectura de carpetas

```text
com.example.prueba2/
├── data/
│   ├── catalogo/     Espacios disponibles.
│   ├── local/        Base de datos, entidades y DAO.
│   └── repository/   Reglas y operaciones de reservas.
├── ui/
│   ├── login/        Inicio de sesion.
│   ├── registro/     Registro de usuarios.
│   ├── menu/         Menus de ambos roles.
│   ├── reservas/     Pantallas, ViewModels y validaciones.
│   └── theme/        Colores, tipografia y tema.
└── utils/            Notificaciones.
```

El flujo de reservas utiliza MVVM para separar la interfaz, el estado y el acceso a los datos.

## Pasos de ejecucion

1. Clonar o descargar este repositorio.
2. Abrir la carpeta del proyecto en Android Studio.
3. Sincronizar el proyecto con Gradle y esperar a que se descarguen las dependencias.
4. Crear un emulador movil con version 13 o mas
5. Ejecutar el modulo app con el boton Run.
6. Registrar una cuenta e iniciar sesion para utilizar la aplicacion.

Los datos se guardan localmente. Una instalacion nueva no incluye usuarios ni reservas.

## Planificacion

Tablero de Trello: [Lucas Toledo - Ignacio Acevedo](https://trello.com/b/vVxRqD7t/lucastoledo-ignacioacevedo)

## Alcance

Los usuarios y las reservas se almacenan en el dispositivo. No existe sincronizacion entre distintos dispositivos.

## Registro e inicio de sesión con el backend

1. Configura y ejecuta `ms-usuarios` siguiendo su README. Usa cuentas ficticias para la demo.
2. Ejecuta Android en modo debug en el emulador. La app usa `http://10.0.2.2:8081` para llegar al servicio local.
3. Registra un solicitante o un administrador e inicia sesión. Para registrar un administrador, ingresa el código configurado en `CODIGO_REGISTRO_ADMIN` en el servicio.

Para usar otro destino, configura `usuariosApiUrl` como propiedad de Gradle. En un teléfono conectado por USB puedes usar `adb reverse tcp:8081 tcp:8081` y compilar con `-PusuariosApiUrl=http://127.0.0.1:8081`. Para release, configura una URL HTTPS.

El registro requiere conexión. Si el servicio no está disponible, el login puede usar una cuenta ya guardada en ese dispositivo. Cuando el servidor rechaza las credenciales, Android muestra el error. Las reservas permanecen en Room/SQLite. Si una cuenta de Room anterior a la integración todavía no existe en el servidor, registra el mismo correo y contraseña e inicia sesión para vincularla sin perder sus reservas locales.
