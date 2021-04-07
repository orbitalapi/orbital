CREATE TABLE IF NOT EXISTS QUERY_SUMMARY
(
    id              SERIAL PRIMARY KEY,
    query_id        VARCHAR(255),
    client_query_id VARCHAR(255),
    taxi_ql         VARCHAR(5000),
    query_json      VARCHAR(10000),
    start_time      TIMESTAMP,
    response_status VARCHAR(255),
    end_time        TIMESTAMP,
    record_size     NUMBER,
    error_message   varchar(2000)
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_querySummary_clientQueryId ON QUERY_SUMMARY (client_query_id);
CREATE UNIQUE INDEX IF NOT EXISTS ix_querySummary_queryId ON QUERY_SUMMARY (query_id);

CREATE TABLE IF NOT EXISTS QUERY_RESULT_ROW
(
    row_id   SERIAL PRIMARY KEY,
    query_id VARCHAR(255),
    json     VARCHAR(MAX)
);

CREATE INDEX IF NOT EXISTS ix_queryResultRow_queryId ON QUERY_RESULT_ROW (query_id);
