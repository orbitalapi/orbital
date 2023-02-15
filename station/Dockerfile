ARG REGISTRY
FROM ${REGISTRY}eclipse-temurin:17-jre-focal

RUN apt-get update && apt-get install zip -y

WORKDIR /opt/service/

COPY target/orbital.zip /opt/service/

# We depend on gitlab (or some other tool) to copy
# the UI artifacts here.
COPY target/classes/static /opt/service/webapp

RUN cd /opt/service && unzip orbital.zip


WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && orbital/bin/orbital  -Dspring.profiles.active=${PROFILE}  --spring.web.resources.static-locations=file:/opt/service/webapp $OPTIONS
