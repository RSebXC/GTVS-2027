ALTER TABLE gtvs.incidents ADD COLUMN IF NOT EXISTS demo_batch text;
CREATE INDEX IF NOT EXISTS incident_demo_batch ON gtvs.incidents(demo_batch) WHERE demo_batch IS NOT NULL;
INSERT INTO gtvs.schema_migrations(version) VALUES(3) ON CONFLICT DO NOTHING;
