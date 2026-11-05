CREATE SCHEMA IF NOT EXISTS gtvs;
CREATE TABLE IF NOT EXISTS gtvs.schema_migrations(version integer PRIMARY KEY, applied_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS gtvs.admins(user_id text PRIMARY KEY, created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS gtvs.incidents(
 id uuid PRIMARY KEY, type text NOT NULL CHECK(type IN ('robo_ataque','obstruccion','cierre','otro')),
 title text NOT NULL, latitude double precision NOT NULL CHECK(latitude BETWEEN -90 AND 90),
 longitude double precision NOT NULL CHECK(longitude BETWEEN -180 AND 180),
 occurred_at timestamptz NOT NULL, severity integer NOT NULL CHECK(severity BETWEEN 1 AND 5),
 confidence numeric NOT NULL DEFAULT 0.6 CHECK(confidence BETWEEN 0 AND 1),
 synthetic boolean NOT NULL DEFAULT true CHECK(synthetic),source_url text,
 created_at timestamptz NOT NULL DEFAULT now(), active boolean NOT NULL DEFAULT true);
CREATE TABLE IF NOT EXISTS gtvs.reports(
 id uuid PRIMARY KEY,user_id text NOT NULL,type text NOT NULL CHECK(type IN ('robo_ataque','obstruccion','cierre','otro')),
 description text NOT NULL CHECK(length(description) BETWEEN 10 AND 1500),
 latitude double precision NOT NULL,longitude double precision NOT NULL,occurred_at timestamptz,
 severity integer NOT NULL CHECK(severity BETWEEN 1 AND 5),source_url text,
 synthetic boolean NOT NULL DEFAULT true CHECK(synthetic),
 state text NOT NULL DEFAULT 'pending' CHECK(state IN ('pending','approved','rejected')),
 incident_id uuid REFERENCES gtvs.incidents(id),revision integer NOT NULL DEFAULT 1,
 idempotency_key text NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),UNIQUE(user_id,idempotency_key));
CREATE TABLE IF NOT EXISTS gtvs.reviews(
 id uuid PRIMARY KEY,report_id uuid NOT NULL REFERENCES gtvs.reports(id),reviewer_id text NOT NULL,
 action text NOT NULL CHECK(action IN ('approve','reject','pending')),reason text NOT NULL,
 incident_id uuid REFERENCES gtvs.incidents(id),created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS gtvs.restrictions(
 id uuid PRIMARY KEY,latitude double precision NOT NULL,longitude double precision NOT NULL,
 radius_m integer NOT NULL CHECK(radius_m BETWEEN 30 AND 2000),reason text NOT NULL,
 active boolean NOT NULL DEFAULT true,review_at timestamptz NOT NULL,created_by text NOT NULL,
 synthetic boolean NOT NULL DEFAULT true CHECK(synthetic),created_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX IF NOT EXISTS reports_state_time ON gtvs.reports(state,created_at DESC);
CREATE INDEX IF NOT EXISTS incident_active_time ON gtvs.incidents(occurred_at DESC) WHERE active;
CREATE TABLE IF NOT EXISTS gtvs.rate_limits(key text PRIMARY KEY,bucket bigint NOT NULL,count integer NOT NULL);
INSERT INTO gtvs.schema_migrations(version) VALUES(1) ON CONFLICT DO NOTHING;
