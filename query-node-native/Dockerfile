## Note: This file buils a "normal" docker image,
## note the Graal native image (used for lambdas, with rapid startup)
## See README on how to build a native image.

ARG REGISTRY
FROM ${REGISTRY}eclipse-temurin:17-jre-jammy

COPY target/query-node-native.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} query-node-native.jar $OPTIONS
