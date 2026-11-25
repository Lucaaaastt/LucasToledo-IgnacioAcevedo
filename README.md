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
- Reportes de incidencias con foto opcional y respuesta del administrador.
- Persistencia compartida en Neon y compatibilidad con datos locales anteriores en Room.

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
│   ├── remote/       Clientes HTTP de usuarios, espacios, reservas y reportes.
│   └── repository/   Operaciones locales y compartidas.
├── ui/
│   ├── login/        Inicio de sesion.
│   ├── registro/     Registro de usuarios.
│   ├── menu/         Menus de ambos roles.
│   ├── reservas/     Pantallas, ViewModels y validaciones.
│   ├── reportes/     Incidencias, fotos y respuestas.
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

Antes de usar las funciones compartidas, ejecutar ambos servicios siguiendo el [README del backend](../backend/README.md). Una instalación nueva no incluye cuentas guardadas localmente.

## Planificacion

Tablero de Trello: [Lucas Toledo - Ignacio Acevedo](https://trello.com/b/vVxRqD7t/lucastoledo-ignacioacevedo)

## Alcance

Las cuentas conectadas al backend guardan sus nuevos registros, reservas y reportes en PostgreSQL/Neon. Room conserva las cuentas y los datos locales que ya existían, además de la copia del catálogo. Los datos anteriores no se suben automáticamente al servidor. Las nuevas operaciones de una cuenta conectada tienen una cola de envío en Room para cuando no hay conexión.

## Registro e inicio de sesión con el backend

1. Configura y ejecuta `ms-usuarios` siguiendo su README. Usa cuentas ficticias para la demo.
2. Ejecuta Android en modo debug en el emulador. La app usa `http://10.0.2.2:8081` para llegar al servicio local.
3. Registra un solicitante o un administrador e inicia sesión. Para registrar un administrador, ingresa el código configurado en `CODIGO_REGISTRO_ADMIN` en el servicio.

Para usar otro destino, configura `usuariosApiUrl` como propiedad de Gradle. En un teléfono conectado por USB puedes usar `adb reverse tcp:8081 tcp:8081` y compilar con `-PusuariosApiUrl=http://127.0.0.1:8081`. Para release, configura una URL HTTPS.

El registro requiere conexión. Si el servicio no está disponible, el login puede usar una cuenta ya guardada en ese dispositivo. Cuando el servidor rechaza las credenciales, Android muestra el error. Las cuentas conectadas usan reservas compartidas; las anteriores de Room se conservan. Si una cuenta de Room anterior a la integración todavía no existe en el servidor, registra el mismo correo y contraseña e inicia sesión para vincularla sin perder sus reservas locales.

## Catálogo de espacios

Ejecuta `ms-espacios-reservas` en el puerto 8080. Al abrir **Nueva reserva**, Android
consulta `GET /api/espacios` y guarda el catálogo en Room. Si falla la consulta,
usa la última copia guardada. Una instalación sin copia usa el catálogo incluido.

En debug, el emulador usa `http://10.0.2.2:8080`. Puedes cambiar el destino con
`-PespaciosApiUrl=URL`; para release usa HTTPS. En un teléfono por USB,
usa `adb reverse tcp:8080 tcp:8080` y `-PespaciosApiUrl=http://127.0.0.1:8080`.
## Reservas compartidas

Las cuentas vinculadas al servidor crean y consultan sus solicitudes en
`ms-espacios-reservas`. El administrador puede verlas y decidir desde otro teléfono.
Las solicitudes nuevas se guardan primero en Room. Si el servicio está caído quedan pendientes de envío; el servidor valida los cruces cuando vuelven a enviarse. Aprobar o rechazar reservas compartidas requiere conexión. Las reservas anteriores
de Room se conservan y siguen siendo locales. Las cuentas solo locales mantienen su
flujo anterior.

## Reportes de incidencias

La pestaña **Reportes** conserva las pantallas de la rama `feat/reportes`. El solicitante selecciona un espacio, describe el problema y puede adjuntar una foto de cámara o galería. El administrador consulta los reportes y escribe una respuesta.

Las cuentas vinculadas al backend guardan los nuevos reportes, la foto y la respuesta en Neon mediante `ms-espacios-reservas`. Las fotos se reducen y envían como JPEG, hasta 1 MB. Las cuentas y reportes anteriores de Room mantienen su funcionamiento local. Los reportes se consultan al entrar, al guardar y con **Actualizar reportes**. Las reservas compartidas se consultan cada cinco segundos mientras su pantalla está visible. Si falla la conexión se muestra el error para reintentar.

La integración conserva ambas variantes de Room versión 5 (catálogo o reportes) al abrir la versión 6, sin borrar usuarios ni cambiar contraseñas.

## Verificación

Las pruebas locales en `app/src/test` cubren formularios, navegación, DAO, repositorios y ViewModels con JUnit, coroutines-test y Robolectric. Se ejecutan en la JVM sin iniciar el backend ni usar credenciales de Neon:

```bash
bash ./gradlew :app:testDebugUnitTest
```

El informe se genera en `app/build/reports/tests/testDebugUnitTest/index.html`. Las pruebas instrumentadas de Room, catálogo y respaldo offline se conservan en `app/src/androidTest` y requieren un emulador o dispositivo.

```bash
bash ./gradlew :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest
```

Con un emulador conectado, `bash ./gradlew :app:connectedDebugAndroidTest` ejecuta las pruebas de Room y catálogo. La prueba remota de reportes se omite por defecto; solo se activa al pasar el argumento `neonPrueba` explícitamente y necesita ambos servicios de prueba en los puertos 18081 y 18080.

## Respaldo y envío offline

- **Consulta:** se guarda en Room la última respuesta válida de reservas y reportes de cada cuenta. Al fallar el backend, se conserva esa copia y se muestra el aviso de conexión. Las copias de solicitante y administrador se guardan por separado.
- **Creación:** las solicitudes y los reportes nuevos de cuentas vinculadas se guardan primero en una cola persistente. Las fotos del reporte también quedan incluidas en la cola.
- **Reconexión:** con una pantalla de la cuenta abierta, se intenta enviar la cola cada cinco segundos. Si se cierra la app, los pendientes permanecen en Room y se reanudan al abrirla. No se ejecuta un servicio permanente en segundo plano.
- **Horarios:** una solicitud pendiente de envío todavía no ocupa el espacio en el servidor. Si al sincronizar el horario está ocupado o la fecha ya pasó, aparece el error y se puede descartar la solicitud para crear otra.
- **Decisiones:** la aprobación, el rechazo y la respuesta del administrador se confirman en el backend; no se encolan offline.
- **Reintentos:** cada envío conserva un identificador UUID. El backend devuelve el registro ya creado si perdió la respuesta anterior, evitando duplicados. Es necesario ejecutar también la versión actualizada del backend.

La versión 7 de Room agrega la copia y la cola sin borrar los datos anteriores. En el backend, `V4__identificar_envios_offline.sql` identifica los reintentos en reservas y reportes; no cambia usuarios ni contraseñas.

## Compilación y firma de APK release

El commit `b8866d5` de Ignacio agrega `app/release/app-release.apk` e instrucciones de presentación. Se conserva aquí la documentación general junto con esas instrucciones.

### Estado del instalador publicado

La revisión estática confirmó una firma APK v2 válida con certificado `CN=MarcelaPazKt`. Paquete `com.example.prueba2`, versión `1.0` (`versionCode=1`), Android mínimo 13. El archivo pesa 46.54 MB.

**Ese binario tiene `USUARIOS_API_URL` y `ESPACIOS_API_URL` vacías. No está configurado para conectarse al backend. Modificar este README o Gradle no cambia un APK ya generado: debe recompilarse y volver a firmarse antes de presentarlo como instalador conectado. No se ha validado su ejecución en un teléfono.**

### Cómo generar el instalador conectado

1. Release utiliza por defecto `https://marcelapaz-usuarios.onrender.com` y `https://marcelapaz-reservas.onrender.com`. Comprobar ambos servicios antes de entregar. En reservas, `USUARIOS_API_URL` apunta al servicio de usuarios; cada uno mantiene su conexión correspondiente a Neon.
2. Solo si se necesitan otros destinos, en el equipo que compila definir en `~/.gradle/gradle.properties` (Windows: `%USERPROFILE%\.gradle\gradle.properties`):

```properties
usuariosApiUrl=https://URL_REAL_DEL_SERVICIO_USUARIOS
espaciosApiUrl=https://URL_REAL_DEL_SERVICIO_RESERVAS
```

Reemplazar los valores de ejemplo por las direcciones reales. La compilación release comprueba que ambas propiedades tengan una URL HTTPS; debug conserva su configuración de desarrollo. Los valores por defecto de release corresponden a los servicios creados para este proyecto en Render.

3. En Android Studio, ir a **Build → Generate Signed App Bundle or APK…**, elegir **APK** y la variante **release**.
4. Usar el mismo keystore de la entrega del compañero, su alias y sus contraseñas. Según su documentación, se llama **MarceltaKtKey** y se guarda fuera del repositorio en `E:\keystores\`. Esa ruta corresponde a su equipo; no es una dependencia incluida en Git.
5. Verificar la firma del archivo generado con `apksigner verify --verbose --print-certs RUTA_DEL_APK` y probarlo en el teléfono contra ambos servicios.
6. Sustituir el instalador anterior en `app/release/app-release.apk` por el archivo recién firmado y verificado.

Para una comprobación de compilación sin firma:

```bash
bash ./gradlew :app:assembleRelease
```

El resultado sin firma no es el instalador final de entrega. Conservar el keystore original permite actualizar el instalador firmado por el compañero; las claves y sus contraseñas no se versionan. Las credenciales de Neon se configuran únicamente en el backend y no se incluyen en el APK.


## Estados de guardado y reintentos

Los formularios bloquean el envío repetido mientras guardan. Reportes conserva un mensaje visible del último resultado y distingue registros locales, pendientes de envío y confirmados por el servidor. Los errores de la respuesta administrativa aparecen dentro del diálogo; el comentario se conserva para corregirlo o reintentarlo.

Las listas de reservas y reportes muestran `Actualizando…` durante la consulta y permiten reintentar la conexión y los envíos guardados. Si no se pudo consultar el servidor y no hay copia disponible, no se presenta la lista como si estuviera confirmada vacía. El reintento utiliza la misma cola y sus identificadores existentes.

Login y registro mantienen visibles los errores, y los tiempos de espera indican que el servidor tarda en responder. Los conflictos de horario requieren cambiar los datos; un error al decidir una solicitud ya resuelta indica actualizar la lista.
