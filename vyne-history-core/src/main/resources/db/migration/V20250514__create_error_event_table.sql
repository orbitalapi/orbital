CREATE TABLE QUERY_ERROR_EVENT (
                                  row_id BIGSERIAL PRIMARY KEY,
                                  query_id VARCHAR(255) NOT NULL,
                                  timestamp TIMESTAMPTZ NOT NULL,
                                  message TEXT NOT NULL,
                                  type_name TEXT NOT NULL,
                                  payload TEXT NOT NULL,
                                  task_stack TEXT
);

CREATE INDEX IF NOT EXISTS ix_queryErrorEvent_queryId ON QUERY_ERROR_EVENT (query_id);

