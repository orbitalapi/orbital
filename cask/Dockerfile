# Building the contianer locally
# cd cask, mvn clean install
# docker build -t vyneco/cask:latest-snapshot .

# 1. Entering running container
# docker exec -it vyne_cask_1 /bin/bash
# 2. Once inside, connect to local db
# psql --host=localhost --port=5432 --username=vynedb --password --dbname=vynedb
# provide password, same as username
# 3. Show tables
# vynedb-# \dt
#                     List of relations
# Schema |              Name               | Type  | Owner
#--------+---------------------------------+-------+--------
# public | rderwindowsummary_f1b588_c76cef | table | vynedb
# public | table_metadata                  | table | vynedb
# 4. Viewing table columns
# select column_name, data_type, character_maximum_length from INFORMATION_SCHEMA.COLUMNS where table_name = 'table_metadata';
# 5. Querying table data
# Select * from table_metadata;
# Note the ; at the end, without it query wont execute!

# base postgres image
FROM postgres:12.2-alpine

# install jdk 8 runtime
RUN apk update && apk add openjdk8-jre

# copy cask jar file and entrypoint scripts
COPY target/cask.jar /opt/service/
COPY docker-entrypoint.sh /opt/service/docker-entrypoint.sh
RUN chmod +x /opt/service/docker-entrypoint.sh
WORKDIR /opt/service/

# bake-in postgres image env variables
ENV POSTGRES_USER=vynedb
ENV POSTGRES_PASSWORD=vynedb
ENV POSTGRES_DB=vynedb
# default data folder /var/lib/postgresql/, reference https://hub.docker.com/_/postgres/?tab=description
ENV PGDATA=/cask/data/

# override postgres entry point with the cas entrypoint
ENTRYPOINT ["/opt/service/docker-entrypoint.sh"]
