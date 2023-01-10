FROM eclipse-temurin:11.0.16.1_1-jre-focal

RUN apt-get update && apt-get install zip -y

WORKDIR /opt/service/

COPY target/vyne.zip /opt/service/
RUN cd /opt/service && unzip vyne.zip
COPY target/classes/static /opt/service/webapp

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && vyne/bin/vyne  -Dspring.profiles.active=${PROFILE}  --spring.web.resources.static-locations=file:/opt/service/webapp $OPTIONS
