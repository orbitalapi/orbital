FROM openjdk:8-jre-alpine

COPY target/pipeline-runner-app.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} pipeline-runner-app.jar $OPTIONS
