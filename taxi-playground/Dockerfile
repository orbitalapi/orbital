ARG REGISTRY
FROM ${REGISTRY}eclipse-temurin:17-jre-jammy

RUN apt-get update
ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} /opt/service/app.jar
COPY target/classes/static /opt/service/webapp

WORKDIR /opt/service/
CMD java -jar app.jar --spring.web.resources.static-locations=file:/opt/service/webapp
