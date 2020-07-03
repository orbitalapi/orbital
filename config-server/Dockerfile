FROM openjdk:8-jre-alpine

COPY target/config-server.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} config-server.jar $OPTIONS
