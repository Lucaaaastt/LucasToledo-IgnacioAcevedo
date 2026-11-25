# Compilación y Firma de APK Release

## Resumen
Este documento describe el proceso realizado para compilar, firmar y dejar disponible el paquete instalable (`APK` en variante `release`) del proyecto Android **LucasToledo-IgnacioAcevedo**, de modo que pueda revisarse o utilizarse en otro equipo durante la presentación.

## Entorno y Servicios
- Proyecto Android sincronizado con la rama base de desarrollo (`origin/dev`).
- Servicios backend locales en Spring Boot conectados a PostgreSQL:
  - `ms-usuarios` en puerto `8081`
  - `ms-espacios-reservas` en puerto `8080`

## Seguridad de Firma (Keystore)
- La variante `release` se firma mediante el keystore **MarceltaKtKey**.
- Por seguridad, el archivo de firma y sus credenciales se mantienen fuera del repositorio (en `E:\keystores\`) y no se versionan en Git.
- Para generar nuevas compilaciones firmadas o actualizaciones desde otro equipo, se requiere disponer del archivo keystore correspondiente, su alias y sus contraseñas.

## Generación y Ubicación del APK
1. En Android Studio, ir a **Build** → **Generate Signed App Bundle or APK...**.
2. Elegir **APK**, seleccionar el keystore externo y marcar la variante **release**.
3. Al completar la compilación (`BUILD SUCCESSFUL`), el archivo instalable queda generado en `app/release/app-release.apk` (ruta local de compilación: `E:\LucasToledo-IgnacioAcevedo\app\release\app-release.apk`).
