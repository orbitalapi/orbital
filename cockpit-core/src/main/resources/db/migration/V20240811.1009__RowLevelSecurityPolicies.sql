-- Create a policy for SELECT operations
ALTER TABLE query_summary ENABLE ROW LEVEL SECURITY;
CREATE POLICY select_query_summary ON query_summary
   FOR SELECT
   USING (orgId = current_user);

-- Create a policy for INSERT operations
CREATE POLICY insert_query_summary ON query_summary
   FOR INSERT
   WITH CHECK (orgId = current_user);

-- Create a policy for UPDATE operations
CREATE POLICY update_query_summary ON query_summary
   FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);

-- Create a policy for DELETE operations
CREATE POLICY delete_query_summary ON query_summary
   FOR DELETE
   USING (orgId = current_user);

-- lineage_record
ALTER TABLE lineage_record ENABLE ROW LEVEL SECURITY;

CREATE POLICY select_lineage_record ON lineage_record FOR SELECT USING (orgId = current_user);
CREATE POLICY insert_lineage_record ON lineage_record FOR INSERT WITH CHECK (orgId = current_user);
CREATE POLICY update_lineage_record ON lineage_record FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);
CREATE POLICY delete_lineage_record ON lineage_record FOR DELETE USING (orgId = current_user);

-- operation_invocation_count
ALTER TABLE operation_invocation_count ENABLE ROW LEVEL SECURITY;

CREATE POLICY select_operation_invocation_count ON operation_invocation_count FOR SELECT USING (orgId = current_user);
CREATE POLICY insert_operation_invocation_count ON operation_invocation_count FOR INSERT WITH CHECK (orgId = current_user);
CREATE POLICY update_operation_invocation_count ON operation_invocation_count FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);
CREATE POLICY delete_operation_invocation_count ON operation_invocation_count FOR DELETE USING (orgId = current_user);

-- organisation
ALTER TABLE organisation ENABLE ROW LEVEL SECURITY;

CREATE POLICY select_organisation ON organisation FOR SELECT USING (orgId = current_user);
CREATE POLICY insert_organisation ON organisation FOR INSERT WITH CHECK (orgId = current_user);
CREATE POLICY update_organisation ON organisation FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);
CREATE POLICY delete_organisation ON organisation FOR DELETE USING (orgId = current_user);

-- organisation_member
ALTER TABLE organisation_member ENABLE ROW LEVEL SECURITY;

CREATE POLICY select_organisation_member ON organisation_member FOR SELECT USING (orgId = current_user);
CREATE POLICY insert_organisation_member ON organisation_member FOR INSERT WITH CHECK (orgId = current_user);
CREATE POLICY update_organisation_member ON organisation_member FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);
CREATE POLICY delete_organisation_member ON organisation_member FOR DELETE USING (orgId = current_user);

-- query_result_row
ALTER TABLE query_result_row ENABLE ROW LEVEL SECURITY;

CREATE POLICY select_query_result_row ON query_result_row FOR SELECT USING (orgId = current_user);
CREATE POLICY insert_query_result_row ON query_result_row FOR INSERT WITH CHECK (orgId = current_user);
CREATE POLICY update_query_result_row ON query_result_row FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);
CREATE POLICY delete_query_result_row ON query_result_row FOR DELETE USING (orgId = current_user);

-- query_sankey_row
ALTER TABLE query_sankey_row ENABLE ROW LEVEL SECURITY;

CREATE POLICY select_query_sankey_row ON query_sankey_row FOR SELECT USING (orgId = current_user);
CREATE POLICY insert_query_sankey_row ON query_sankey_row FOR INSERT WITH CHECK (orgId = current_user);
CREATE POLICY update_query_sankey_row ON query_sankey_row FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);
CREATE POLICY delete_query_sankey_row ON query_sankey_row FOR DELETE USING (orgId = current_user);

-- remote_call_response
ALTER TABLE remote_call_response ENABLE ROW LEVEL SECURITY;

CREATE POLICY select_remote_call_response ON remote_call_response FOR SELECT USING (orgId = current_user);
CREATE POLICY insert_remote_call_response ON remote_call_response FOR INSERT WITH CHECK (orgId = current_user);
CREATE POLICY update_remote_call_response ON remote_call_response FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);
CREATE POLICY delete_remote_call_response ON remote_call_response FOR DELETE USING (orgId = current_user);

-- stream_status
ALTER TABLE stream_status ENABLE ROW LEVEL SECURITY;

CREATE POLICY select_stream_status ON stream_status FOR SELECT USING (orgId = current_user);
CREATE POLICY insert_stream_status ON stream_status FOR INSERT WITH CHECK (orgId = current_user);
CREATE POLICY update_stream_status ON stream_status FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);
CREATE POLICY delete_stream_status ON stream_status FOR DELETE USING (orgId = current_user);

-- user_roles
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;

CREATE POLICY select_user_roles ON user_roles FOR SELECT USING (orgId = current_user);
CREATE POLICY insert_user_roles ON user_roles FOR INSERT WITH CHECK (orgId = current_user);
CREATE POLICY update_user_roles ON user_roles FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);
CREATE POLICY delete_user_roles ON user_roles FOR DELETE USING (orgId = current_user);

-- users
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

CREATE POLICY select_users ON users FOR SELECT USING (orgId = current_user);
CREATE POLICY insert_users ON users FOR INSERT WITH CHECK (orgId = current_user);
CREATE POLICY update_users ON users FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);
CREATE POLICY delete_users ON users FOR DELETE USING (orgId = current_user);

-- workspace
ALTER TABLE workspace ENABLE ROW LEVEL SECURITY;

CREATE POLICY select_workspace ON workspace FOR SELECT USING (orgId = current_user);
CREATE POLICY insert_workspace ON workspace FOR INSERT WITH CHECK (orgId = current_user);
CREATE POLICY update_workspace ON workspace FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);
CREATE POLICY delete_workspace ON workspace FOR DELETE USING (orgId = current_user);

-- workspace_member
ALTER TABLE workspace_member ENABLE ROW LEVEL SECURITY;

CREATE POLICY select_workspace_member ON workspace_member FOR SELECT USING (orgId = current_user);
CREATE POLICY insert_workspace_member ON workspace_member FOR INSERT WITH CHECK (orgId = current_user);
CREATE POLICY update_workspace_member ON workspace_member FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);
CREATE POLICY delete_workspace_member ON workspace_member FOR DELETE USING (orgId = current_user);

-- workspace_schema_spec
ALTER TABLE workspace_schema_spec ENABLE ROW LEVEL SECURITY;

CREATE POLICY select_workspace_schema_spec ON workspace_schema_spec FOR SELECT USING (orgId = current_user);
CREATE POLICY insert_workspace_schema_spec ON workspace_schema_spec FOR INSERT WITH CHECK (orgId = current_user);
CREATE POLICY update_workspace_schema_spec ON workspace_schema_spec FOR UPDATE
   USING (orgId = current_user)
   WITH CHECK (orgId = current_user);
CREATE POLICY delete_workspace_schema_spec ON workspace_schema_spec FOR DELETE USING (orgId = current_user);
