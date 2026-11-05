CREATE TABLE IF NOT EXISTS gtvs.admin_invitations(email_hash text PRIMARY KEY CHECK(length(email_hash)=64),created_at timestamptz NOT NULL DEFAULT now());
INSERT INTO gtvs.schema_migrations(version) VALUES(2) ON CONFLICT DO NOTHING;
