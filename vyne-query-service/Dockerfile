ARG REGISTRY
FROM ${REGISTRY}eclipse-temurin:18.0.2.1_1-jre-focal

RUN apt-get update && apt-get install zip -y

WORKDIR /opt/service/

COPY target/vyne.zip /opt/service/
COPY target/classes/static /opt/service/webapp

RUN cd /opt/service && unzip vyne.zip


WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && vyne/bin/vyne  -Dspring.profiles.active=${PROFILE}  --spring.web.resources.static-locations=file:/opt/service/webapp $OPTIONS
