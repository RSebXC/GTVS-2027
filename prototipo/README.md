# Prototipo académico web y Android

Versión 1.0.0. Implementación parcial para comprobar reglas del diseño. No es navegación real ni un sistema validado de riesgo urbano.

## Funciones implementadas

- API HTTP local y base SQLite inicializada con evidencia sintética.
- Dijkstra sobre una red abstracta bidireccional; perfiles rápida, equilibrada y menor riesgo estimado.
- Agregación por incidentes únicos, influencia temporal exponencial y explicación por tramo.
- Estado de información insuficiente y deduplicación de rutas coincidentes.
- Recepción de reportes pendientes con idempotencia y fecha desconocida explícita.
- Revisión local protegida por token, vinculación a hecho existente o creación de otro, auditoría y conflicto de revisión.
- Interfaz web adaptable al ancho de pantalla, formulario y revisión.
- Código de cliente Android Kotlin con Views nativas para rutas y reportes, consumiendo la misma API.

La compilación, instalación y prueba visual de Android **no están verificadas**. La web se sirve por HTTP y se comprobó a través de solicitudes automatizadas; su ejecución visual e interacción manual todavía deben comprobarse. El manifiesto web prepara una presentación móvil, pero no existe service worker ni modo sin conexión y no se declara una PWA instalable validada.

## Requisitos y arranque

Python 3.13 fue el entorno utilizado. El servicio solo usa biblioteca estándar: no requiere pip ni Node. Ejecutar desde `prototipo/`:

```powershell
python -m backend.servidor
```

Abrir `http://127.0.0.1:8765`. El servidor utiliza `datos/local.sqlite3` y conserva reportes entre reinicios. Al iniciar imprime un token de moderación generado para esa sesión; copiarlo a «Revisión local». No guardar el token en capturas o anexos. Puede suministrarse uno mediante `TESIS_DEMO_MOD_TOKEN` para una prueba controlada, sin incorporarlo a Git.

También puede ejecutarse `./Iniciar-Prototipo.ps1`. El servidor escucha únicamente en 127.0.0.1 por defecto. No exponerlo públicamente: `http.server` y la identidad colaboradora simulada no son una solución de producción.

## Demostración recomendada

1. Calcular A → I y revisar alternativas, costos y explicación. Pueden existir menos de tres rutas si coinciden perfiles.
2. Consultar E → H. La rápida puede usar el tramo sin información; su costo de riesgo se presenta nulo. Los perfiles con riesgo lo excluyen.
3. Enviar un reporte ficticio con fecha. Confirmar que permanece pendiente y no cambia el índice.
4. Revisarlo desde web con el token. Vincularlo a `inc-demo-1` si describe el mismo hecho del tramo BC, en vez de crear un incidente.
5. Recalcular y comprobar el conteo. La revisión puede cambiar aunque el valor no aumente.
6. Registrar y aceptar un hecho nuevo fechado en GH para observar una contribución adicional. No describir el cambio como reducción o aumento de criminalidad real.

La influencia temporal cambia con el reloj aun sin reportes nuevos. Para comparar valores exactos en pruebas se usa un instante común; las consultas manuales realizadas en momentos distintos pueden tener variaciones pequeñas por decaimiento.

## Pruebas

Desde `prototipo/`:

```powershell
python ejecutar_verificacion.py
```

El comando ejecuta unittest y actualiza `../docs/evidencias/`. No rellena resultados predeterminados. Las bases utilizadas por las pruebas son temporales. El conjunto incluye un oráculo exhaustivo independiente para el grafo pequeño, idempotencia concurrente, persistencia y API. No sustituye revisión visual, pruebas instrumentadas Android ni validación con datos reales.

## Android

Abrir `android/` en Android Studio. El código utiliza AGP 8.7.3, Kotlin 2.0.21, SDK 35, mínimo API 26 y destino JVM 17. La configuración toma como referencia el entorno descrito en el reporte de Mis Finanzas Privadas, pero no reutiliza sus datos bancarios ni requiere SMS o notificaciones.

El helper `Compilar-Android.ps1` genera un wrapper Gradle 8.9 y ejecuta `assembleDebug`. Ajustar los parámetros/rutas si el entorno cambia. Necesita acceso a repositorios de dependencias y permisos de escritura en cachés de Gradle y SDK. La solicitud de ejecución de este helper quedó interrumpida durante esta entrega; no se ha obtenido una APK confirmada.

```powershell
./Compilar-Android.ps1
```

Archivo esperado después de una compilación satisfactoria: `android/app/build/outputs/apk/debug/app-debug.apk`. Su presencia futura deberá comprobarse antes de afirmar que se generó.

Con el servidor en el mismo equipo:

- Emulador Android: configurar en Servicio `http://10.0.2.2:8765`.
- Teléfono USB: ejecutar el `adb` del SDK con `reverse tcp:8765 tcp:8765` y configurar `http://127.0.0.1:8765`. El dispositivo debe estar autorizado por su propietario. El script no cambia opciones de seguridad del teléfono.

La variante debug permite HTTP para la prueba local. La configuración principal no lo permite. No se solicitaron permisos de ubicación, SMS, cámara ni notificaciones. La app no tiene persistencia SQLite propia: la persistencia compartida es del backend. La pestaña de revisión corresponde a web en esta versión.

## Contratos implementados

| Método | Recurso | Operación |
|---|---|---|
| GET | /api/salud | Estado local y versión |
| GET | /api/escenario | Red, parámetros y condición sintética |
| POST | /api/rutas/calcular | origen/destino como ID A–I y perfil; retorna alternativas y factores |
| POST | /api/reportes | tipo, tramo, gravedad, fecha opcional y descripción |
| GET | /api/reportes | Lista restringida para moderación |
| POST | /api/reportes/{id}/revision | Decisión, motivo y revisión esperada; token requerido |
| GET | /api/incidentes | Hechos sintéticos aceptados |

La API es una reducción de los contratos conceptuales del capítulo 4. Todavía no soporta coordenadas reales, filtros geográficos, cuentas, fuentes externas ni corroboraciones ciudadanas. No se deben presentar los endpoints conceptuales restantes como implementados.

## Parámetros y límites

Semivida temporal de demostración: 7 días; gravedad: enteros 1–5; respaldo inicial sintético: 0.7–0.8; hecho creado por revisión: 0.6. Estos números se eligieron para ejercitar el código, no por investigación empírica. Proximidad es una asociación manual binaria al tramo, sin geocodificación ni distancia geográfica. La frecuencia se cuenta por incidentes, no por reportes.

Cada consulta utiliza una revisión leída en transacción. El valor se calcula en la solicitud; no existen lotes publicados y tareas programadas del diseño completo. Los perfiles son sumas ponderadas ilustrativas, no un frente de Pareto. La publicación de una revisión no implica aprendizaje automático.

No se implementan reputación, validación documental automática, OSM real, restricciones de giro reales, historial completo de redes, autenticación de usuarios ni medidas suficientes para publicación. Los límites de tamaño y frecuencia son controles locales básicos, no pruebas de resistencia. No ingresar información sensible.
