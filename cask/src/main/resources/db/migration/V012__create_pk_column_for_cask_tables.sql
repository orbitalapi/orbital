do
$$
    declare cask_table RECORD;
    declare primary_key_record RECORD;
    begin
        execute 'CREATE SEQUENCE IF NOT EXISTS cask_raw_id_generator';
        for cask_table in (
            select
                "tablename"
            from
                cask_config cc
            where
                    "exposestype" = false) loop
                select
                    kcu.table_schema,
                    kcu.table_name,
                    tco.constraint_name,
                    kcu.ordinal_position as position,
                    kcu.column_name as key_column
                into primary_key_record
                from
                    information_schema.table_constraints tco
                        join information_schema.key_column_usage kcu on
                                kcu.constraint_name = tco.constraint_name
                            and kcu.constraint_schema = tco.constraint_schema
                            and kcu.constraint_name = tco.constraint_name
                where
                        tco.constraint_type = 'PRIMARY KEY' and tco.table_name = cask_table."tablename";

                IF NOT FOUND then
                    RAISE NOTICE 'Adding cask_raw_id column into %', cask_table."tablename";
                    execute 'alter table ' || cask_table."tablename" || ' ADD cask_raw_id varchar(64) PRIMARY KEY DEFAULT NEXTVAL(''cask_raw_id_generator'')';
                END IF;
            end loop;
    end
$$;

