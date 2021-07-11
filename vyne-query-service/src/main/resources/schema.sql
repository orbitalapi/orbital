CREATE SEQUENCE HIBERNATE_SEQUENCE START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS QUERY_SUMMARY
(
    id                   SERIAL PRIMARY KEY,
    query_id             VARCHAR(255),
    client_query_id      VARCHAR(255),
    taxi_ql              VARCHAR(5000),
    query_json           CLOB,
    start_time           TIMESTAMP,
    response_status      VARCHAR(255),
    end_time             TIMESTAMP,
    record_count         NUMBER,
    error_message        varchar(2000),
    anonymous_types_json CLOB
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_querySummary_clientQueryId ON QUERY_SUMMARY (client_query_id);
CREATE UNIQUE INDEX IF NOT EXISTS ix_querySummary_queryId ON QUERY_SUMMARY (query_id);

CREATE TABLE IF NOT EXISTS QUERY_RESULT_ROW
(
    row_id     SERIAL PRIMARY KEY,
    query_id   VARCHAR(255),
    json       CLOB,
    value_hash NUMBER
);

CREATE INDEX IF NOT EXISTS ix_queryResultRow_queryId ON QUERY_RESULT_ROW (query_id);
CREATE INDEX IF NOT EXISTS ix_queryResultRow_valueHash_queryId ON QUERY_RESULT_ROW (query_id, value_hash);

CREATE TABLE IF NOT EXISTS LINEAGE_RECORD
(
    data_source_id   VARCHAR(255) PRIMARY KEY,
    query_id         VARCHAR(255),
    data_source_type VARCHAR(255),
    data_source_json CLOB
);

CREATE INDEX IF NOT EXISTS ix_lineageRecord_queryId on LINEAGE_RECORD (query_id);

CREATE TABLE IF NOT EXISTS REMOTE_CALL_RESPONSE
(
    response_id    varchar(255) PRIMARY KEY,
    remote_call_id VARCHAR(255),
    query_id       VARCHAR(255),
    response       CLOB
);

CREATE INDEX IF NOT EXISTS ix_remoteCallResponse_queryId ON REMOTE_CALL_RESPONSE (query_id);
CREATE INDEX IF NOT EXISTS ix_remoteCallResponse_remoteCallId ON REMOTE_CALL_RESPONSE (remote_call_id);

