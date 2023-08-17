create table workspace
(
   id              serial primary key,
   name            varchar(255) not null,
   organisation_id numeric      not null,

   foreign key (organisation_id) references organisation (id),
   unique (name, organisation_id)
);

create table workspace_member
(
   id              serial primary key,
   user_id         varchar(800) not null,
   workspace_id    numeric      not null,
   workspace_roles text[]       not null
);

create unique index ix_workspace_member_user
   on workspace_member (
                        user_id, workspace_id
      );

