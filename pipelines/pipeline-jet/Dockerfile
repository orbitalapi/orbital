ARG REGISTRY
# We need to use a temruin 21 alpine image as earlier versions don't support arm64
FROM ${REGISTRY}eclipse-temurin:21-jre-alpine

COPY target/pipeline-jet.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} pipeline-jet.jar $OPTIONS
