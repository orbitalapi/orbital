ARG REGISTRY
FROM ${REGISTRY}eclipse-temurin:18.0.2.1_1-jre-focal

COPY target/pipeline-jet.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} pipeline-jet.jar $OPTIONS
