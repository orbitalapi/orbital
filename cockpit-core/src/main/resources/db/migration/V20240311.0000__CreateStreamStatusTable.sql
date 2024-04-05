create table stream_status(
   stream_name varchar(1024) primary key ,
   state varchar(50) not null ,
   timestamp timestamp with time zone not null ,
   username varchar(150)
)
