FROM openjdk:8-jre-alpine

COPY target/pipeline-runner-cacib-app.jar /opt/service/

WORKDIR /opt/service/

CMD java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} pipeline-runner-cacib-app.jar $OPTIONS
