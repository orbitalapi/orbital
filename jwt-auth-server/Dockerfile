FROM openjdk:8-jre-alpine

COPY target/jwt-auth-server.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} jwt-auth-server.jar $OPTIONS
