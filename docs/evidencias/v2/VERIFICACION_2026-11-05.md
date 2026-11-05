# Comprobaciones de V2 — 5 de noviembre de 2026

- `npm run build`: TypeScript y Vite completados, 266 módulos transformados.
- `npm test`: 9 pruebas aprobadas, 0 fallos.
- `GTVS_TEST_BASE_URL=<API desplegada> npm run test:integration`: 16 comprobaciones aprobadas. Registro detallado: `integracion.json`. Se utilizaron cuentas e incidentes ficticios; los reportes e incidentes creados por la prueba se limpiaron al terminar.
- `gradlew.bat -p android assembleDebug --no-daemon --console=plain`, con Temurin 17: `BUILD SUCCESSFUL in 1m 11s`, 35 tareas ejecutadas. Advertencia: uso de `onBackPressed` obsoleto, sin error de compilación.
- `GET https://rsebxc.github.io/GTVS-2027/`: HTTP 200 tras configurar Pages desde la rama `gh-pages`.
- `adb devices`: lista vacía; no se instaló ni ejecutó la APK en un dispositivo físico.

El primer intento Android con Java 25 falló por incompatibilidad con Gradle 8.9; se resolvió usando Java 17 portátil verificado por SHA-256. La primera configuración de Pages devolvió HTTP 409; el segundo intento terminó correctamente.

No se ha completado revisión visual interactiva. No se verificaron Google, entrega de códigos por correo, ni activación de la cuenta administradora del propietario. Las pruebas técnicas no constituyen evaluación empírica del riesgo urbano, pruebas de carga ni auditoría de seguridad.
