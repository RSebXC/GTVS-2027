# Escenarios de presentación y panel de reportes

## Carga ficticia

El lote `presentacion-2026-11-v1` contiene **36 incidentes ficticios**: doce grupos de tres hechos distintos. Incluye robo/ataque, obstrucción, cierre y otro contexto, con gravedad y antigüedad variables. Las coordenadas permiten probar proximidad sobre cartografía real; no corresponden a delitos conocidos ni califican barrios o habitantes.

Los registros se insertaron directamente como escenarios de software. No proceden de usuarios, noticias, fuentes oficiales ni una corroboración comunitaria. La interfaz los identifica como **ESCENARIO PRECARGADO · FICTICIO**. La base registra el lote en `incidents.demo_batch`, sin reemplazar los incidentes ingresados manualmente.

Desde la raíz del repositorio, con el entorno privado de la rama demo-v2:

```text
npm run db:migrate
node --import tsx scripts/seed-demo.ts
```

El script rechaza una vinculación distinta de `demo-v2`. Utiliza identificadores deterministas y `ON CONFLICT DO NOTHING`: la segunda ejecución comprobada insertó cero filas y el lote continuó con 36. No modifica fechas al repetirse. La antigüedad sigue aumentando para mostrar el decaimiento temporal; para otra entrega debe diseñarse otro lote explícito, sin presentar fechas nuevas como hechos observados.

## Paneles

**Reportes → Incidentes públicos:** muestra incidentes activos ficticios, filtros de tipo y temporalidad, contadores de la lista cargada y ubicación en el mapa. El máximo actual es 300 registros; los contadores no representan estadísticas de criminalidad ni el total histórico de la base.

**Reportes → Mis reportes:** requiere sesión. Muestra solo los 100 aportes más recientes del usuario autenticado y sus estados. Un aporte pendiente no aparece como incidente público ni modifica el índice. Al cerrar sesión o cambiar de cuenta se limpia el listado privado en memoria.

**Gestión:** conserva la moderación de reportes enviados por cuentas. Se añadieron resumen y filtro de estados. Los escenarios precargados no generan falsos reportantes ni revisiones administrativas inventadas, por lo que se consultan en el panel público, no como una cola de denuncias de usuarios.

**Android:** incluye panel público con filtro por tipo y acceso al mapa. Mis reportes y la moderación se consultan desde la web en esta iteración.

## Cantidad de rutas

OSRM genera los candidatos. GTVS compara sus métricas, estima influencia de incidentes ficticios y asigna los perfiles rápida, equilibrada y menor riesgo estimado. Los perfiles pueden coincidir en un recorrido. Esta revisión conserva visibles también los candidatos adicionales que no ganan un perfil, en lugar de ocultarlos.

Esto no crea desvíos nuevos: si OSRM devuelve un candidato, se presenta uno. Tampoco se garantiza encontrar el mínimo global de riesgo ni evitar una restricción administrativa. La búsqueda sobre un grafo ponderado propio sigue pendiente.

El botón **Probar recorrido de demostración** selecciona `[-90.5218,14.6349]` y `[-90.5352,14.5905]`. En la comprobación registrada el 5 de noviembre de 2026, OSRM devolvió dos candidatos y se mostraron ambos. Los tres perfiles eligieron el primero. Otros dos recorridos consultados devolvieron un candidato cada uno. Son respuestas puntuales de un servicio externo, no garantías de disponibilidad futura ni resultados de validación de seguridad. Véase `docs/evidencias/v2/recorridos_demo3.json`.

## Contexto de la ruta

El aviso bajo las alternativas enumera la cantidad y los tipos de incidentes ficticios que influyen por proximidad. «Próximo» significa a menos del radio ilustrativo de 350 m de la geometría; no demuestra que el hecho ocurrió en la vía, ni representa exposición real. Sin incidentes próximos se mantiene cobertura desconocida.

Cada incidente distinto puede contribuir al índice. Tres hechos cercanos suman contribuciones; no existe un umbral validado que convierta automáticamente una ubicación en «peligrosa». Varias evidencias vinculadas al mismo incidente no deben multiplicarlo.

## Guion de presentación

1. Abrir Reportes y mostrar que los escenarios están etiquetados como ficticios.
2. Filtrar por tipo y por fecha; ubicar un registro en el mapa.
3. Ir a Rutas y pulsar Probar recorrido de demostración. Comparar candidatos reales y revisar el aviso de contexto.
4. Ingresar un reporte desde una cuenta, mostrar su estado pendiente en Mis reportes y revisarlo desde Gestión con el administrador.
5. Actualizar el mapa en Android para mostrar la base compartida. No presentar el índice ilustrativo como una medición validada de seguridad urbana.
