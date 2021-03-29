CREATE TABLE ingestion_error(
    id               SERIAL PRIMARY KEY NOT NULL,
    error            TEXT NOT NULL,
    cask_message_id    varchar(40) NOT NULL,
    type_fqn         TEXT NOT NULL,
    inserted_at       timestamp NOT NULL
)
