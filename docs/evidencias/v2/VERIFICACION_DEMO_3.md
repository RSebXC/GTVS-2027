# Verificación demo.3 — 5 de noviembre de 2026

Se aplicó la migración 003 a demo-v2. El script seed-demo insertó 36 escenarios ficticios; al repetirse insertó cero y mantuvo 36. Utiliza identificadores deterministas y no actualiza registros existentes. Los incidentes y reportes manuales se preservaron mediante inserciones limitadas al lote.

`npm test`: 12 aprobadas, 0 fallos. Incluye la regresión que conserva un candidato aunque no gane ningún perfil. `npm run build`: TypeScript y Vite completados, 271 módulos transformados.

`npm run test:integration` contra la API desplegada: **20 comprobaciones aprobadas**. Entre ellas: sesión obligatoria para Mis reportes, inclusión del aporte propio, aislamiento frente a otra cuenta, 36 registros precargados identificados y condición ficticia en toda la lista pública. Los registros y permisos temporales de esta prueba se limpiaron por sus identificadores, sin retirar la carga de presentación ni al administrador real. Las cuentas ficticias de Auth permanecen sin datos personales reales.

`scripts/check-demo-routes.mjs` registró respuestas reales de OSRM: ejemplo centro-sur, dos candidatos y dos visibles; oeste-este y oeste-sur, un candidato cada uno. Las cantidades y métricas puntuales están en recorridos_demo3.json. Los incidentes usados en sus índices son ficticios. No son resultados de validación de seguridad urbana.

La compilación final de Android terminó con `BUILD SUCCESSFUL in 18s`. Se instaló la actualización mediante `adb install -r`: resultado `Success`. La consulta del paquete en el dispositivo confirmó `versionCode=4` y `versionName=2.0.0-demo.3`. Se conservó la instalación existente. Esto verifica instalación, no una prueba interactiva de todas las pantallas; la revisión visual y de uso final permanece pendiente.

La web y la API fueron publicadas. `scripts/verify-public.mjs` confirmó HTTP 200 y JavaScript público idéntico al build local.

No se añadieron navegación giro a giro, tráfico en tiempo real, mapa 3D, patrullaje, puntuaciones de confianza de usuarios ni datos criminales reales. La cartografía se mantiene en OpenStreetMap; el ajuste de saturación solo afecta a la presentación de las teselas.
