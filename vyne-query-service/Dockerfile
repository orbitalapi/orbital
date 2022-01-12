FROM eclipse-temurin:11.0.13_8-jre-focal

COPY target/vyne-query-service.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} vyne-query-service.jar $OPTIONS
