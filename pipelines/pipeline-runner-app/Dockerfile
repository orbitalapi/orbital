FROM eclipse-temurin:8u312-b07-jre-focal

COPY target/pipeline-runner-app.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} pipeline-runner-app.jar $OPTIONS
