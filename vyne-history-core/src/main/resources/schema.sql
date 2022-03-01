CREATE TABLE IF NOT EXISTS QUERY_SUMMARY
(
    id                   SERIAL PRIMARY KEY,
    query_id             VARCHAR(255),
    client_query_id      VARCHAR(255),
    taxi_ql              VARCHAR(5000),
    query_json           CLOB(100000),
    start_time           TIMESTAMP,
    response_status      VARCHAR(255),
    end_time             TIMESTAMP,
    record_count         BIGINT,
    error_message        varchar(2000),
    anonymous_types_json CLOB,
    response_type        varchar(2000)
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_querySummary_clientQueryId ON QUERY_SUMMARY (client_query_id);
CREATE UNIQUE INDEX IF NOT EXISTS ix_querySummary_queryId ON QUERY_SUMMARY (query_id);

CREATE TABLE IF NOT EXISTS QUERY_RESULT_ROW
(
    row_id     SERIAL PRIMARY KEY,
    query_id   VARCHAR(255),
    json       CLOB,
    value_hash BIGINT
);

CREATE INDEX IF NOT EXISTS ix_queryResultRow_queryId ON QUERY_RESULT_ROW (query_id);
CREATE INDEX IF NOT EXISTS ix_queryResultRow_valueHash_queryId ON QUERY_RESULT_ROW (query_id, value_hash);

CREATE TABLE IF NOT EXISTS LINEAGE_RECORD
(
   record_id        VARCHAR(550) PRIMARY KEY,
   data_source_id   VARCHAR(255),
   query_id         VARCHAR(255),
   data_source_type VARCHAR(255),
   data_source_json CLOB
);

CREATE INDEX IF NOT EXISTS ix_lineageRecord_queryId on LINEAGE_RECORD (query_id);
CREATE UNIQUE INDEX IF NOT EXISTS ix_lineageRecord_dataSourceQueryId on LINEAGE_RECORD (data_source_id, query_id);

CREATE TABLE IF NOT EXISTS REMOTE_CALL_RESPONSE
(
   response_id    varchar(255) PRIMARY KEY,
   remote_call_id VARCHAR(255),
   query_id       VARCHAR(255),
   response       CLOB
);

CREATE INDEX IF NOT EXISTS ix_remoteCallResponse_queryId ON REMOTE_CALL_RESPONSE (query_id);
CREATE INDEX IF NOT EXISTS ix_remoteCallResponse_remoteCallId ON REMOTE_CALL_RESPONSE (remote_call_id);

CREATE TABLE IF NOT EXISTS QUERY_SANKEY_ROW
(
    id               SERIAL PRIMARY KEY,
    query_id         VARCHAR(255),
    client_query_id  VARCHAR(255) NULL,
    source_node_type VARCHAR(50),
    source_node      VARCHAR(1000),
    target_node_type VARCHAR(50),
    target_node      VARCHAR(1000),
    node_count       BIGINT
);

CREATE INDEX IF NOT EXISTS ix_querySankeyRow_queryId on QUERY_SANKEY_ROW(query_id);
CREATE INDEX IF NOT EXISTS ix_querySankeyRow_clientQueryId on QUERY_SANKEY_ROW(client_query_id);
