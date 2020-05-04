FROM openjdk:8-jre-alpine

COPY target/file-schema-server.jar /opt/service/

WORKDIR /opt/service/

CMD java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} file-schema-server.jar $OPTIONS
