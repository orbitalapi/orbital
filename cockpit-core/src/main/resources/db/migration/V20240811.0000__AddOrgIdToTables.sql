-- V1__add_orgId_column_and_index_to_all_tables.sql

-- Adding orgId column and index to lineage_record table
ALTER TABLE lineage_record ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_lineage_record_orgId ON lineage_record(orgId);

-- Adding orgId column and index to operation_invocation_count table
ALTER TABLE operation_invocation_count ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_operation_invocation_count_orgId ON operation_invocation_count(orgId);

-- Adding orgId column and index to organisation table
ALTER TABLE organisation ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_organisation_orgId ON organisation(orgId);

-- Adding orgId column and index to organisation_member table
ALTER TABLE organisation_member ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_organisation_member_orgId ON organisation_member(orgId);

-- Adding orgId column and index to query_result_row table
ALTER TABLE query_result_row ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_query_result_row_orgId ON query_result_row(orgId);

-- Adding orgId column and index to query_sankey_row table
ALTER TABLE query_sankey_row ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_query_sankey_row_orgId ON query_sankey_row(orgId);

-- Adding orgId column and index to query_summary table
ALTER TABLE query_summary ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_query_summary_orgId ON query_summary(orgId);

-- Adding orgId column and index to remote_call_response table
ALTER TABLE remote_call_response ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_remote_call_response_orgId ON remote_call_response(orgId);

-- Adding orgId column and index to stream_status table
ALTER TABLE stream_status ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_stream_status_orgId ON stream_status(orgId);

-- Adding orgId column and index to user_roles table
ALTER TABLE user_roles ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_user_roles_orgId ON user_roles(orgId);

-- Adding orgId column and index to users table
ALTER TABLE users ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_users_orgId ON users(orgId);

-- Adding orgId column and index to workspace table
ALTER TABLE workspace ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_workspace_orgId ON workspace(orgId);

-- Adding orgId column and index to workspace_member table
ALTER TABLE workspace_member ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_workspace_member_orgId ON workspace_member(orgId);

-- Adding orgId column and index to workspace_schema_spec table
ALTER TABLE workspace_schema_spec ADD COLUMN orgId VARCHAR(255) DEFAULT current_user;
CREATE INDEX idx_workspace_schema_spec_orgId ON workspace_schema_spec(orgId);
