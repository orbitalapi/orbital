create table workspace_schema_spec (
    id serial primary key ,
    kind varchar(255) not null,
    spec text not null
);
