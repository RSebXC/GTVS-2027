# Diccionario del esquema implementado

Fuente autoritativa: `db/001_gtvs.sql` y `db/002_admin_invitation.sql`. Si cambia el código, actualizar este documento junto con la migración. Los nombres siguientes corresponden al prototipo, no a entidades futuras pendientes.

| Tabla | Clave y relaciones | Atributos y propósito |
|---|---|---|
| `incidents` | `id uuid` PK | `type text` catálogo limitado; `title text`; `latitude/longitude double precision`; `occurred_at timestamptz` obligatorio; `severity integer` 1–5; `confidence numeric` 0–1; `source_url text` opcional; `synthetic boolean` debe ser verdadero; `active boolean`; `created_at timestamptz`. Hecho consolidado ficticio. |
| `reports` | `id uuid` PK; `incident_id uuid` FK opcional a incidents; UNIQUE `(user_id,idempotency_key)` | `user_id text` identidad Auth; tipo, descripción (10–1500 caracteres), coordenadas, ocurrencia opcional, gravedad y referencia opcional; `state text` pending/approved/rejected; `revision integer`; condición ficticia y fecha de registro. Evidencia enviada por un colaborador. |
| `reviews` | `id uuid` PK; `report_id uuid` FK obligatoria; `incident_id uuid` FK opcional | `reviewer_id text`, `action text` approve/reject/pending, `reason text`, `created_at timestamptz`. Historial de decisiones de moderación. |
| `restrictions` | `id uuid` PK | Coordenadas; `radius_m integer` 30–2000; `reason text`; `active boolean`; `review_at timestamptz`; `created_by text`; condición ficticia y fecha. Influencia administrativa hasta retiro explícito. |
| `admins` | `user_id text` PK | Fecha de alta. Autoriza a una identidad gestionada por Auth, sin almacenar contraseña. |
| `admin_invitations` | `email_hash text` PK, 64 caracteres | Huella SHA-256 de correo normalizado y fecha; permite activar al primer administrador tras comprobar verificación del correo. No se expone por API pública. |
| `rate_limits` | `key text` PK | Huella por ámbito e identidad/dirección; `bucket bigint` minuto y `count integer`. Control básico de cuota. Sin política automática de limpieza todavía. |
| `schema_migrations` | `version integer` PK | `applied_at timestamptz`. Registro de migraciones aplicadas. |

Un incidente puede relacionarse con cero o muchos reportes; cada reporte con cero o un incidente. Un reporte tiene cero o muchas revisiones. El proveedor mantiene `neon_auth.user`; los identificadores de usuario propios son referencias lógicas sin FK entre esquemas. Esa separación evita modificar tablas gestionadas por Auth, pero requiere tratar usuarios eliminados y retención en una etapa posterior.

Se separan aportación y hecho para no duplicar frecuencia por múltiples evidencias. Los tipos se restringen mediante CHECK, sin tabla de catálogo adicional en esta versión. Una referencia URL es un atributo por evidencia; un modelo normalizado de fuentes múltiples queda pendiente. No existen tablas propias de segmento vial, ruta ni riesgo por segmento: las geometrías provienen del proveedor y el índice se calcula por candidato en memoria.

Los campos de coordenadas no utilizan PostGIS; la validación de ámbito aproximado se realiza en la API. Las restricciones SQL completas están en las migraciones. La clave de idempotencia evita repetir una inserción por reenvío, pero no detecta hechos duplicados entre personas diferentes.
