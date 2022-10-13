CREATE TABLE IF NOT EXISTS STORED_SCHEMA
(
   id           VARCHAR(255) PRIMARY KEY,
   uri_slug     VARCHAR(255),
   taxi         VARCHAR(500000),
   content_sha  VARCHAR(255),
   created_date timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_schema_content_sha ON STORED_SCHEMA (content_sha);
CREATE UNIQUE INDEX IF NOT EXISTS ix_schema_uri_slug ON STORED_SCHEMA (uri_slug);
