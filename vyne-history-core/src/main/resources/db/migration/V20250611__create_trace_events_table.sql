-- V001__Create_trace_event_table.sql

CREATE TABLE TRACE_EVENT (
                            event_id VARCHAR(255) NOT NULL,
                            query_id VARCHAR(255) NOT NULL,
                            trace_id VARCHAR(255) NOT NULL,
                            span_id VARCHAR(255) NOT NULL,
                            parent_span_id VARCHAR(255),
                            tracing_event_kind VARCHAR(50) NOT NULL,
                            span_state VARCHAR(50) NOT NULL,
                            exchange_metadata JSONB,
                            timestamp timestamptz DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT pk_trace_event PRIMARY KEY (event_id)
);

-- Index for query_id (likely to be queried frequently)
CREATE INDEX idx_trace_event_query_id ON TRACE_EVENT (query_id);

-- Index for trace_id (essential for distributed tracing queries)
CREATE INDEX idx_trace_event_trace_id ON TRACE_EVENT (trace_id);

-- Composite index for trace_id + span_id (common query pattern)
CREATE INDEX idx_trace_event_trace_span ON TRACE_EVENT (trace_id, span_id);

-- Index for timestamp (useful for time-based queries and cleanup)
CREATE INDEX idx_trace_event_timestamp ON TRACE_EVENT (timestamp);

-- Optional: Index for parent_span_id (useful for building span hierarchies)
CREATE INDEX idx_trace_event_parent_span ON TRACE_EVENT (parent_span_id);

CREATE INDEX idx_trace_event_metadata_gin ON TRACE_EVENT USING GIN (exchange_metadata);
