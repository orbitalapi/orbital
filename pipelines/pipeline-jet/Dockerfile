FROM adoptopenjdk/openjdk11:alpine-jre

COPY target/pipeline-jet.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} pipeline-jet.jar $OPTIONS
