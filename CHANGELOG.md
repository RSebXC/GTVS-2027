# Registro de cambios

## v1.0.0 — 5 de noviembre de 2026

Primera publicación de GTVS a partir del prototipo académico local. Servicio Python con SQLite, Dijkstra, tres perfiles sobre red abstracta, reportes pendientes, vinculación sin duplicar hechos, auditoría y control de concurrencia. Cliente web y código Android nativo. Registros previos de 17 pruebas satisfactorias sobre datos sintéticos.

Se excluyen documentos personales, bases locales y credenciales. Licencia pendiente de elección. No hay validación de riesgo urbano ni compilación Android acreditada en esta versión.
# 2.0.0-demo.1 — 2026-11-05

- Web con mapa OpenStreetMap, búsqueda Photon y candidatos OSRM.
- API HTTPS, Neon Auth, Postgres aislado en `demo-v2`, reportes pendientes y moderación.
- Índice ilustrativo con recencia y proximidad; restricciones administrativas ficticias revisables.
- Android Kotlin con mapa Leaflet, búsqueda, rutas y envío de reportes a la API compartida.
- Logotipo proporcionado por el autor, paleta azul/verde/dorado/grafito y misión/visión propuestas.
- Web compilada, nueve pruebas unitarias y dieciséis de integración contra API desplegada aprobadas. APK debug compilada; instalación física pendiente.
- No se validó el riesgo con hechos reales, ni Google, ni entrega de correos. La revisión visual completa y la aprobación de alcance del asesor siguen pendientes.

# 2.0.0-demo.2 — 2026-11-05

- Corregida la lectura del JWT de Neon 0.8.2: se obtiene de la sesión del SDK, evitando el formato inconsistente de la respuesta en caché de `token()`.
- El botón de cuenta refleja la identidad aunque falle la consulta de permisos. Las consultas públicas de web y Android no envían token.
- Registro y reporte demostrativos sin confirmación de correo; controles de administrador separados.
- Web con claro/oscuro, tarjetas de categorías y panel inferior ajustable. Android con mapa completo, cabecera compacta, panel ajustable y conservación del formulario al marcar su ubicación.
- Once pruebas unitarias aprobadas; reproducción y regresión real del SDK documentadas. Google y revisión visual final pendientes de comprobar.

# 2.0.0-demo.3 — 2026-11-05

- Panel público de incidentes y Mis reportes con aislamiento por usuario; filtros y resumen de moderación.
- Lote reproducible de 36 escenarios ficticios, identificado por demo_batch, sin reportantes ni corroboraciones inventadas. Segunda carga: cero duplicados.
- Todos los candidatos distintos de OSRM permanecen visibles aunque no ganen perfiles. Recorrido de demostración comprobado con dos candidatos; no se garantizan múltiples rutas para cualquier par de puntos.
- Avisos de influencia por proximidad, iconos SVG propios y controles consistentes. Android incluye panel público e iconos vectoriales.
- Doce pruebas unitarias y veinte comprobaciones de integración aprobadas. La estimación de riesgo sigue sin calibración empírica.

