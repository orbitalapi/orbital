FROM openjdk:8-jre-alpine

COPY target/cask.jar /opt/service/

WORKDIR /opt/service/

CMD java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} cask.jar $OPTIONS
