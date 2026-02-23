ALTER TABLE query_summary ADD COLUMN persist_results BOOLEAN;
ALTER TABLE query_summary ADD COLUMN persist_remote_call_responses BOOLEAN;
ALTER TABLE query_summary ADD COLUMN persist_remote_call_metadata BOOLEAN;
ALTER TABLE query_summary ADD COLUMN persist_trace_events BOOLEAN;
ALTER TABLE query_summary ADD COLUMN persist_errors BOOLEAN;
