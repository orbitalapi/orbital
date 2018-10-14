FROM openjdk:8-jre-alpine

COPY target/vyne-query-service.jar /opt/service/

WORKDIR /opt/service/

CMD java $JVM_OPTS -jar -Dspring.profiles.active=${DEPLOYMENT_ENV} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 vyne-query-service.jar
