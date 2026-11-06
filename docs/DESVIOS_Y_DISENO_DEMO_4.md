# Desvíos y revisión visual de demo.4

## Cambio funcional

La API solicita hasta tres alternativas directas a OSRM. Cuando un candidato atraviesa una restricción circular activa, ensaya hasta cuatro planes de desvío, cada uno con dos puntos intermedios. Los sectores próximos se consideran juntos para evitar que rodear uno lleve directamente hacia otro. Las coordenadas intermedias generan solicitudes de rutas sobre la red vial; no se conectan con líneas rectas para presentarlas como caminos reales.

Cada geometría recibida se comprueba contra **todos** los círculos activos. Solo se muestra «sin cruces de sectores de prueba» cuando la distancia geométrica a cada centro supera su radio. Si ningún candidato cumple, todos conservan la advertencia de cruce. Si el origen o destino se encuentra dentro del sector considerado, no se afirma que este pueda evitarse. OSRM ajusta los puntos a la red vial, por lo que la comprobación se hace sobre la geometría devuelta.

La heurística utiliza una envolvente orientada de círculos cercanos, márgenes de ensayo y un máximo de cuatro consultas adicionales. No es una optimización global ni una exclusión de polígonos incorporada al grafo de OSRM. Puede producir rodeos largos, fallar por disponibilidad o no encontrar un candidato adecuado. La interfaz conserva la referencia rápida y muestra tiempo y distancia, además de los cruces. La preferencia por no cruzar restricciones está separada del índice ilustrativo de incidentes.

La [API oficial de OSRM](https://project-osrm.org/docs/v5.24.0/api/) admite puntos intermedios y solicitudes de alternativas; no garantiza devolver alternativas en todos los casos. Los tiempos de espera y el máximo de consultas son decisiones de operación del prototipo, no parámetros de riesgo validados.

## Sectores ficticios

`scripts/seed-sectors.ts` agrega tres círculos de 230 m, identificados explícitamente como simulaciones, con referencias nominales a El Gallito, Bethania y Landívar. Los puntos de referencia se obtuvieron de Photon/OpenStreetMap el 5 de noviembre de 2026; los radios y las condiciones de restricción son inventados para probar el software. No representan límites oficiales, criminalidad real ni una clasificación de barrios. No se trasladaron noticias a estos escenarios.

El script solo permite la rama demo-v2, utiliza identificadores constantes y no sobrescribe restricciones existentes. La moderación puede retirarlos como cualquier restricción de prueba. Su autor técnico `fixture:demo-v4` identifica una carga de software, no una persona ni una revisión administrativa real. Los 36 incidentes anteriores permanecen diferenciados de estos círculos.

## Comprobaciones del desvío

El primer ensayo con un punto intermedio no encontró un recorrido libre de todos los círculos. Un segundo enfoque rodeaba un círculo pero atravesaba otro. La agrupación inicial demasiado amplia produjo un rodeo de unos 25 km. Se ajustó la geometría de los planes y se registró la respuesta de la API demo.4, no la de una versión previa en despliegue.

En el escenario principal se obtuvieron cuatro candidatos reales: tres cruzaban algún círculo y uno no cruzaba ninguno. La referencia rápida midió 6 235.6 m y 542.6 s; el desvío sin cruces midió 8 918.6 m y 976.1 s en esa consulta. Estas medidas pertenecen a respuestas puntuales del proveedor. No prueban que alguna ruta sea segura, ni validan la estimación del riesgo. Registro: `evidencias/v2/desvios_demo4.json`.

## Revisión de interfaz

Las rutas permanecen dibujadas simultáneamente, con colores propios, bordes blancos, numeración y opacidad completa. La selección solo resalta un recorrido; no oculta los demás. Los cruces de sectores se señalan con trazos discontinuos y avisos explícitos. El recorrido que gana el perfil de menor riesgo mantiene el verde cuando no cruza restricciones.

Los marcadores A y B tienen color azul y magenta, letra grande y doble borde independiente del tema. Los incidentes distinguen robo/ataque en coral, obstrucción en naranja, cierre en violeta y otro contexto en azul. Los círculos se rotulan como sectores ficticios. No se usan el amarillo como color único ni el gris para desvanecer alternativas.

Android conserva los formularios Kotlin y el mapa Leaflet. Se rediseñaron los buscadores de origen/destino, las tarjetas de ruta y las categorías de reporte; el campo técnico de fecha ISO se sustituyó por selectores nativos. La fecha enviada sigue siendo ISO con zona, pero no se exige escribir ese formato.

El logotipo original se conserva. `logo-emblema.png` es una versión editada con herramienta generativa para retirar el marco exterior y conservar una silueta con transparencia; puede contener variaciones del dibujo. Se usa como recurso separado, sin sobrescribir la imagen del autor.

Las observaciones de una persona fueron comunicadas por el estudiante de manera informal. No se presentan como un estudio de usabilidad, muestra de investigación ni evaluación experimental.
