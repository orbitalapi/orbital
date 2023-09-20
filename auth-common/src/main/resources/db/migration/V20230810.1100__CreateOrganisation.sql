create table organisation
(
    id     serial primary key,
    name   varchar(255) not null,
    idp_id varchar(255) not null
);

create table organisation_member
(
    id        serial primary key,
    user_id   varchar(800) not null,
    org_id    numeric      not null,
    org_roles text[]       not null
);

create unique index ix_org_member_user
    on organisation_member (
                            user_id, org_id
        );

