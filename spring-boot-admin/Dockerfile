FROM openjdk:8-jre-alpine

COPY target/spring-boot-admin.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} spring-boot-admin.jar $OPTIONS
