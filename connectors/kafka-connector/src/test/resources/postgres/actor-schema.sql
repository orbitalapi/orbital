create table actor
(
   actor_id integer not null
      constraint actor_pkey
         primary key,
   first_name varchar(45) not null,
   last_name varchar(45) not null,
   last_update timestamp default now() not null
);
