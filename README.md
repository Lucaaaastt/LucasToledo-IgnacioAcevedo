# MarcelaPazKT

Proyecto semestral para la escuela Marcela Paz, orientado a la solicitud y gestion de reservas de espacios.

## Organizacion del codigo

El codigo principal se encuentra en `app/src/main/java/com/example/prueba2` y se organiza en dos capas:

### Datos (`data`)

Contiene el almacenamiento local y la logica de acceso a los datos.

- `catalogo`: tipos de espacios y listado de salas, laboratorios y otros espacios disponibles en la escuela.
- `local`: base de datos Room sobre SQLite, entidades, DAO y conversiones de fechas y horas.
- `repository`: consulta y guardado de reservas, verificacion del rol de administrador y validacion de disponibilidad por fecha y horario.

### Interfaz (`ui`)

Contiene las pantallas y los componentes con los que interactua el usuario.

- `login`: inicio de sesion.
- `registro`: registro de solicitantes y administradores.
- `menu`: menus de navegacion segun el rol.
- `reservas`: creacion y consulta de reservas, lista de solicitudes pendientes del administrador, ViewModels, estados y validaciones del formulario.
- `theme`: colores, tipografia y tema visual de la aplicacion.

En reservas se utiliza MVVM: las pantallas presentan la informacion, los ViewModels gestionan el estado y el repositorio coordina el acceso a la base de datos. Las tarjetas y la lista se reutilizan entre solicitantes y administradores.
