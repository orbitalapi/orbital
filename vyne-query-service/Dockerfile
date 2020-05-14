FROM openjdk:8-jre-alpine

COPY target/vyne-query-service.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} vyne-query-service.jar $OPTIONS
