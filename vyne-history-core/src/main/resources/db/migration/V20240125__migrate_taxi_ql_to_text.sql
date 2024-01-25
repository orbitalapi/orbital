do
$$
begin
   update query_summary
   set taxi_ql  = lt.query_text
      from (select *, convert_from(lo_get(m.oid), 'UTF8') as query_text from query_summary qs
      inner join pg_largeobject_metadata m on qs.taxi_ql::oid = m."oid"
      ) lt
   where query_summary.id  = lt.id;
   exception when OTHERS THEN
end;
$$
language plpgsql;
