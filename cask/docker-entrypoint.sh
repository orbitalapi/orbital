#!/bin/bash
# Start postgres database in the background
/usr/local/bin/docker-entrypoint.sh postgres&

# On a Shared/network volumes Postgres DB initialization takes time,
# hence the need to wait before starting cask app.
until pg_isready -q -d vynedb -U vynedb; do
   echo "Waiting for cask data store to initialize..."
   sleep 2
done
echo "Cask data store successfully initialized."

# Start cask in the foreground as the main process
$BEFORE_START_COMMAND && java $EMBEDDED_OPTS $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} /opt/service/cask.jar $OPTIONS
