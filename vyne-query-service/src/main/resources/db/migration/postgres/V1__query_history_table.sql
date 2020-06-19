CREATE TABLE IF NOT EXISTS query_history_records (
    id SERIAL PRIMARY KEY,
    query_id varchar(255),
    record jsonb NOT NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX query_history_records_query_id_index ON query_history_records  USING btree (query_id);
CREATE INDEX query_history_executed_at_index ON query_history_records  USING btree (executed_at);
