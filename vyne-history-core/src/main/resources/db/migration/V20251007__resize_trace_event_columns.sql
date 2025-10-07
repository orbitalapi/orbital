alter table public.trace_event
   alter column event_verb type text using event_verb::text,
   alter column event_resource type text using event_resource::text,
   alter column event_source type text using event_source::text;

