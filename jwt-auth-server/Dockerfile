FROM eclipse-temurin:8u342-b07-jre-focal

COPY target/jwt-auth-server.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} jwt-auth-server.jar $OPTIONS
