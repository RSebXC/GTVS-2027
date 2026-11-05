# API implementada V2

Base: `https://br-morning-resonance-belnuka7-gtvsapi.compute.c-9.us-east-2.aws.neon.tech/api`.

JSON con coordenadas `[longitud, latitud]`. Fechas de ocurrencia en ISO 8601 con zona. Toda evidencia es ficticia. La consulta de configuración describe ámbito y parámetros actuales. Los detalles ejecutables se encuentran en `src/server.ts`; esta tabla no constituye una API futura ya desarrollada.

| Método y recurso | Acceso | Entrada y salida principal |
|---|---|---|
| GET `/health` | Público | Estado y comprobación SQL |
| GET `/config` | Público | Tipos, ámbito, Auth y parámetros ilustrativos |
| GET `/me` | Sesión | Identificador propio y permiso administrador |
| GET `/search?q=...` | Público | Consulta explícita; lugares y coordenadas Photon |
| GET `/incidents` | Público | Hasta 300 incidentes aprobados activos; sin autor |
| GET `/restrictions` | Público | Hasta 100 restricciones activas ficticias |
| POST `/routes` | Público | `origin`, `destination`; geometrías, perfiles, métricas e índice |
| POST `/reports` | Sesión | Tipo, coordenada, descripción, gravedad, ocurrencia opcional, referencia y `synthetic:true`; reporte pendiente |
| GET `/admin/reports` | Administrador | Hasta 100 reportes con estado y revisión |
| POST `/admin/reports/{id}/review` | Administrador | Decisión, motivo y revisión esperada; creación o vinculación de incidente |
| POST `/admin/restrictions` | Administrador | Coordenada, radio, motivo, fecha de revisión y condición ficticia |
| PATCH `/admin/restrictions/{id}` | Administrador | `active:false`; retiro de restricción |

Sesión: `Authorization: Bearer <JWT>` emitido por Neon Auth. Reportes: cabecera `Idempotency-Key` de 8 a 100 caracteres; repetir la clave con el mismo contenido conserva un reporte. Cambiar el contenido genera conflicto. La identidad procede del JWT, nunca del cuerpo enviado.

Errores principales: 401 sin sesión o token inválido; 403 sin rol; 404 recurso ausente; 409 reenvío incompatible o revisión obsoleta; 413 cuerpo demasiado grande; 422 datos no admitidos y límites básicos actuales; 503 indisponibilidad externa o de base de datos. No existe aún un contrato de errores exhaustivo ni paginación completa. El límite de solicitudes debería distinguirse con 429 en una futura revisión.

No se publican endpoints para extraer identidades, contraseñas ni cadenas de conexión. No se implementaron carga de fotografías, scraping, corroboración pública ni almacenamiento de rutas personales.
