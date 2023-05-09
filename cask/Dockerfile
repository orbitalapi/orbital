ARG REGISTRY
FROM ${REGISTRY}eclipse-temurin:17-jre-jammy

COPY target/cask.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} cask.jar $OPTIONS
