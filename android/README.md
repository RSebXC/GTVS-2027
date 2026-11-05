# Android GTVS V2

Abrir esta carpeta en Android Studio. Gradle JDK **17**, SDK 35, mínimo Android 8.0 (API 26). Seleccionar el teléfono autorizado y ejecutar la configuración `app`.

La API HTTPS y la autenticación web están configuradas en `app/build.gradle.kts`. Son direcciones públicas, no credenciales. No incluir `DATABASE_URL` ni contraseñas. El acceso a cuentas depende de la página publicada; Google dentro del WebView está desactivado.

Los formularios y la navegación son Kotlin nativos. El mapa usa Leaflet dentro de un WebView con atribución OSM. Tras cambiar el mapa: desde la raíz del repositorio ejecutar `node scripts/prepare-android.mjs` y volver a compilar. Para sustituir el logotipo, guardar la imagen en `web/public/logo-gtvs.jpg` y ejecutar `node scripts/apply-logo.mjs`.

En esta computadora se puede compilar con `COMPILAR_ANDROID.cmd`. APK: `app/build/outputs/apk/debug/app-debug.apk`. Es una compilación debug para demostración, no una versión de Play Store.

[Guía y límites comprobados](../docs/GUIA_V2.md).
