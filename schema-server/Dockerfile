FROM eclipse-temurin:11.0.16.1_1-jre-focal

COPY target/schema-server.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} schema-server.jar $OPTIONS
