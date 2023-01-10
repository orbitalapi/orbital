FROM eclipse-temurin:11.0.15_10-jre-focal
RUN apt-get update
ARG JAR_FILE=tdarget/*.jar

COPY ${JAR_FILE} /opt/service/app.jar
COPY target/classes/static /opt/service/webapp

WORKDIR /opt/service/
CMD java -jar app.jar --spring.web.resources.static-locations=file:/opt/service/webapp
