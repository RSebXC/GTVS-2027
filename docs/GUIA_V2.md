# GTVS: guía de la demostración V2

Versión de trabajo: 2.0.0-demo.4. El prototipo utiliza cartografía real e incidentes exclusivamente ficticios. No ofrece garantías de seguridad ni navegación de emergencia.

Demo.4 añade desvíos por puntos intermedios y comprobación de cruces, colores distinguibles para todas las rutas, marcadores A/B de mayor contraste y logotipo con transparencia exterior. Pulsa **Ensayar un desvío ficticio** para cargar el escenario y calcularlo. [Qué se implementó y qué límites tiene](DESVIOS_Y_DISENO_DEMO_4.md).

Esta revisión incorpora 36 escenarios ficticios precargados, panel público y Mis reportes en la web, filtro de moderación, avisos de contexto de ruta e iconos vectoriales. Android incluye el panel público. [Guion de presentación y límites](DEMOSTRACION_INCIDENTES.md).

Esta revisión corrige la extracción del JWT del SDK y la actualización del botón de cuenta. En la web, usar Ctrl+F5 para descargar la nueva interfaz. En Android, volver a ejecutar `app` o instalar el APK nuevo. La sesión puede renovarse desde Cuenta. No se exige verificar el correo para reportar en la demostración. El control de administrador permanece separado.

La web permite cambiar entre claro y oscuro. En pantallas pequeñas, arrastrar la barra superior del panel para dejar más espacio al mapa o al formulario. Android utiliza un mapa completo con una cabecera compacta y un panel inferior ajustable; la tipografía de los controles se establece explícitamente en sans-serif.

## Abrir la web en esta computadora

Desde `Documento/GTVS-2027`, ejecutar `VER_WEB.cmd` y visitar **http://127.0.0.1:5173**. Mantener abierta esa consola. Se necesita Internet para Neon, Photon, OSRM y las teselas OpenStreetMap; no se necesita arrancar el antiguo prototipo Python. Si Vite ocupa otro puerto, utilizar la dirección que indique y revisar el origen autorizado de Auth.

El lanzador usa la instalación portátil de Node que está en `Documento/.tools`. En otra computadora: instalar Node LTS, ejecutar `npm ci`, configurar `.env.local` a partir de `.env.example`, indicar la URL pública de API y ejecutar `npm run dev`. Nunca copiar credenciales Postgres a la web ni a Android.

Publicación comprobada el 5 de noviembre de 2026 (HTTP 200): https://rsebxc.github.io/GTVS-2027/. GitHub Pages sirve la rama `gh-pages`.

## Abrir Android Studio y ejecutar en el teléfono

1. Usar **Open** y seleccionar `Documento/GTVS-2027/android`. No seleccionar `Documento/prototipo/android`, que pertenece al ensayo anterior.
2. En Settings → Build, Execution, Deployment → Build Tools → Gradle, seleccionar **Gradle JDK 17**. En esta computadora se preparó `Documento/.tools/java17/jdk-17.0.20.1+1`. Gradle 8.9 no funciona con el Java 25 del Android Studio instalado; véase la [matriz oficial](https://docs.gradle.org/current/userguide/compatibility.html).
3. Esperar la sincronización y disponer de Android SDK 35. Usar el Gradle Wrapper incluido.
4. Conectar el Xiaomi por USB, mantener la depuración USB habilitada y aceptar en el teléfono la autorización de esta computadora. Elegir el teléfono en el selector de dispositivos y ejecutar **Run app**.
5. El celular necesita Internet. La API está en Neon; no necesita compartir Wi-Fi con la PC. Las modificaciones Kotlin y del icono requieren volver a ejecutar la app. El mapa HTML se prepara con `node scripts/prepare-android.mjs` y se incluye en el APK.

Alternativa local: `COMPILAR_ANDROID.cmd`. La salida esperada es `android/app/build/outputs/apk/debug/app-debug.apk`. Una compilación exitosa no demuestra instalación ni funcionamiento en el teléfono; esa evidencia debe registrarse por separado.

## Recorrido de demostración

Consultar una ruta sin sesión, buscando o marcando origen y destino. Si OSRM devuelve un solo candidato, puede aparecer una alternativa con varios perfiles; no se fabrican tres recorridos. Crear una cuenta para registrar reportes ficticios. Desde una cuenta administradora, revisar, aprobar, rechazar o vincular evidencias a un incidente existente. Actualizar el contexto en web y Android para observar los incidentes aprobados en ambos clientes.

La invitación de administrador se concede únicamente a la cuenta cuyo correo coincide con la invitación privada y está verificado por el proveedor. No se publica el correo del propietario. La verificación por correo y Google requieren una comprobación manual final; las pruebas automáticas verificaron correo/contraseña, sesiones y JWT con cuentas ficticias, no la entrega de correos ni el consentimiento Google. El acceso móvil implementa correo/contraseña; Google dentro de WebView está excluido.

## Identidad y propósito

El logotipo original seleccionado por el autor se guarda en `web/public/logo-gtvs.jpg` y se copia al recurso Android `drawable-nodpi/logo_gtvs.jpg`. `node scripts/apply-logo.mjs` vuelve a aplicar esa imagen. No se afirma titularidad de los símbolos incorporados por el autor ni aval institucional.

La interfaz toma como referencia grafito `#202a2d`, azul `#133f5b`, verde `#14765d` y dorado `#bd9143`. Se conserva una superficie clara para leer mapas y formularios. La misión y visión se presentan como propósito propuesto de un proyecto académico en desarrollo.

## Pendientes reales

- Prueba visual completa en navegador y prueba de uso de todas las pantallas Android. La instalación de demo.3 sí se comprobó por ADB y consulta de versión.
- Inicio de sesión Google configurado y comprobado con el propietario; entrega de códigos y activación real del administrador.
- Polígono municipal: actualmente se limita una caja geográfica aproximada; no representa el límite administrativo exacto.
- Ubicación actual nativa, traducción y fotografías: no implementadas en Android. Web sí permite solicitar ubicación al pulsar el botón.
- Calibración del modelo y evaluación con evidencia real: no realizadas.
- Confirmación del alcance con el asesor y actualización posterior del capítulo 5.

Los servicios gratuitos pueden cambiar sus límites o dejar de responder. No se contrató un plan de pago ni se ofrece disponibilidad garantizada.
