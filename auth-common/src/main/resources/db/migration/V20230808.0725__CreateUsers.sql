create table USERS
(
    -- The ID, as provided by the IDP
   id          VARCHAR(800) NOT NULL PRIMARY KEY,
   issuer      VARCHAR(500) not null,
   username    varchar(255) not null,
   email       varchar(255) not null,
   profile_url varchar(500),
   name        varchar(255)
);

CREATE UNIQUE INDEX ix_userId_issuer on USERS (id, issuer);

CREATE TABLE USER_ROLES
(
   user_id   VARCHAR(800) NOT NULL,
   user_role VARCHAR(255) NOT NULL,
   FOREIGN KEY (user_id) REFERENCES USERS (id),
   UNIQUE (user_id, user_role) -- This ensures that the combination of a user and a role is unique
);
