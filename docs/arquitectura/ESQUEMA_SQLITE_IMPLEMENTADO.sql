-- Esquema extraído de SQLite, prototipo sintético v0.1. Sin base geoespacial real.
PRAGMA foreign_keys=ON;

CREATE TABLE incidentes(id TEXT PRIMARY KEY,tramo TEXT NOT NULL,
              gravedad INTEGER NOT NULL CHECK(gravedad BETWEEN 1 AND 5),ocurrido_en TEXT NOT NULL,
              respaldo REAL NOT NULL CHECK(respaldo BETWEEN 0 AND 1),estado TEXT NOT NULL);

CREATE TABLE meta(clave TEXT PRIMARY KEY,valor TEXT NOT NULL);

CREATE TABLE reportes(id TEXT PRIMARY KEY,actor TEXT NOT NULL,
              tipo TEXT NOT NULL,tramo TEXT NOT NULL,gravedad INTEGER NOT NULL,
              ocurrido_en TEXT,descripcion TEXT NOT NULL,recibido_en TEXT NOT NULL,
              estado TEXT NOT NULL,incidente_id TEXT REFERENCES incidentes(id),
              clave TEXT,huella TEXT NOT NULL,revision INTEGER NOT NULL DEFAULT 1,
              UNIQUE(actor,clave));

CREATE TABLE revisiones(id TEXT PRIMARY KEY,reporte_id TEXT NOT NULL
              REFERENCES reportes(id),decision TEXT NOT NULL,motivo TEXT NOT NULL,
              realizada_en TEXT NOT NULL,revision_anterior INTEGER NOT NULL,
              incidente_id TEXT REFERENCES incidentes(id));
