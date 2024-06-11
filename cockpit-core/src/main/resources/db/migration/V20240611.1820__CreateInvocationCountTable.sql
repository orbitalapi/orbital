create table operation_invocation_count
(
   id            bigserial primary key,
   window_start   timestamp WITH TIME ZONE not null,
   window_end     timestamp WITH TIME ZONE not null,
   operation_name varchar(1024),
   operation_kind varchar(200),
   count         numeric(12, 0)
);

CREATE INDEX idx_operation_invocation_count_windowStart ON operation_invocation_count(window_start);
CREATE INDEX idx_operation_invocation_count_windowEnd ON operation_invocation_count(window_end);
CREATE INDEX idx_operation_invocation_count_windowRange ON operation_invocation_count(window_start, window_end);

-- CREATE SEQUENCE operation_invocation_count_seq
--    INCREMENT BY 1
--    NO MAXVALUE
--    NO MINVALUE
--    CACHE 1;

