ARG REGISTRY
# We need to use a temruin 21 alpine image as earlier versions don't support arm64
FROM ${REGISTRY}eclipse-temurin:21-jre-alpine

ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} /opt/service/app.jar
COPY target/classes/static /opt/service/webapp

WORKDIR /opt/service/
CMD java -jar app.jar --spring.web.resources.static-locations=file:/opt/service/webapp
