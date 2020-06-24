#!/bin/bash
# Start postgres database in the background
/usr/local/bin/docker-entrypoint.sh postgres&

# Start cask in the foreground as the main process
$BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} /opt/service/cask.jar $OPTIONS
