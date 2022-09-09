FROM eclipse-temurin:11.0.13_8-jre-focal
RUN apt-get update
ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} /opt/service/app.jar

WORKDIR /opt/service/
CMD java -jar app.jar
