# Arquitectura implementada de GTVS V2

Esta descripción corresponde al código de `develop/v2`. No debe confundirse con el diseño completo de la tesis ni con una evaluación del modelo sobre incidentes reales.

## Distribución

La web estática se compila con Vite y se publica en GitHub Pages. El cliente Android contiene formularios Kotlin y un WebView para Leaflet; utiliza la misma API HTTPS. Neon Auth administra cuentas y emite JWT. Una Neon Function con Hono verifica la identidad y los permisos antes de acceder al esquema `gtvs` de Postgres. La aplicación no expone conexiones SQL al navegador ni a la APK.

La búsqueda de lugares se consulta a Photon y las geometrías de rutas a OSRM desde la API. Los clientes solicitan teselas a OpenStreetMap con atribución visible. Los proveedores conocen las consultas que reciben. No se almacenan trayectos consultados en la base propia.

## Reportes e incidentes

Un reporte es una aportación pendiente; un incidente representa un hecho consolidado. Solo la moderación puede crear o vincular un incidente a partir de un reporte. Varios reportes pueden respaldar el mismo incidente. La interfaz ofrece vinculación manual, no detección automática validada de duplicados. Una revisión conserva actor, motivo, resultado y fecha. El control de revisión impide sobrescribir una decisión concurrente con una versión desactualizada.

`source_url` conserva una referencia opcional por reporte e incidente; no existe todavía un catálogo normalizado de fuentes ni adquisición de noticias. Tampoco se almacena una red vial propia: OSRM devuelve candidatos y sus geometrías.

## Estimación ilustrativa

Para cada incidente único, el código combina gravedad, confiabilidad fija demostrativa, decaimiento exponencial y proximidad aproximada a la geometría candidata. La semivida de siete días y el radio de 350 metros son parámetros de prueba, sin calibración empírica. Tres hechos distintos aumentan la contribución acumulada; tres noticias vinculadas al mismo incidente no lo multiplican.

Los perfiles rápida, equilibrada y menor riesgo estimado seleccionan entre los candidatos recibidos; no ejecutan una búsqueda global sobre un grafo ponderado propio. Una restricción administrativa ficticia conserva influencia hasta su retiro y requiere fecha de revisión. No convierte una colonia o zona completa en una categoría permanente.

## Seguridad y límites

Los permisos se verifican en el servidor. Un cliente no puede declararse administrador. La invitación privada utiliza una huella del correo y exige verificación del proveedor. Los JWT se validan con claves públicas remotas. Los reportes requieren sesión e idempotencia; las escrituras de moderación utilizan transacciones. Existen límites básicos de cuerpo y solicitudes; no equivalen a una auditoría de seguridad ni a protección completa contra abuso.

La base solo admite incidentes marcados como ficticios. Los endpoints públicos excluyen identidad de reportantes. No se ha implementado borrado de cuenta desde la interfaz, gestión completa de retención, recuperación funcional comprobada, fotos ni notificaciones. El cliente Android conserva el token en memoria; renovar sesión requiere acceso a la web de autenticación.

La caja geográfica provisional excede algunos límites municipales. La distancia incidente-ruta es una aproximación plana local; no considera conectividad, barreras físicas ni exposición real. La cobertura desconocida se comunica aunque el índice numérico sea cero.

## Proveedores y atribuciones

- Cartografía: [OpenStreetMap y licencia de datos](https://www.openstreetmap.org/copyright). [Política de teselas](https://operations.osmfoundation.org/policies/tiles/): sin descarga masiva ni modo sin conexión.
- Búsqueda: [Photon](https://github.com/komoot/photon). El prototipo usa búsqueda explícita, caché y límites básicos; no ofrece autocompletado continuo.
- Rutas: [servidor OSRM de FOSSGIS](https://routing.openstreetmap.de/about.html). Servicio público sin garantía de disponibilidad; revisar sus condiciones antes de ampliar uso.
- Leaflet: licencia BSD de dos cláusulas, incluida en los recursos Android como `LEAFLET-LICENSE.txt`.
- Infraestructura: Neon y GitHub Pages, sujetos a sus cuotas. No se habilitaron planes de pago.

El logotipo fue proporcionado por el autor. La licencia del código propio sigue pendiente; las licencias de dependencias y datos conservan sus condiciones.
