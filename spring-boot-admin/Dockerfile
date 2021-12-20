FROM eclipse-temurin:8u312-b07-jre-focal

COPY target/spring-boot-admin.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} spring-boot-admin.jar $OPTIONS
