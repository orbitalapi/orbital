FROM openjdk:8-jre-alpine

COPY target/pipelines-orchestrator.jar /opt/service/

WORKDIR /opt/service/

CMD java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} pipelines-orchestrator.jar $OPTIONS
