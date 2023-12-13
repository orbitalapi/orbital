ARG REGISTRY
# Generating SOAP clients from WSDLs at runtime requires the JDK, not JRE
# As Apache CXF generates and compiles code.
# We need to use a temruin 21 alpine image as earlier versions don't support arm64
FROM ${REGISTRY}eclipse-temurin:21-jdk-alpine

RUN apk --no-cache add zip

WORKDIR /opt/service/

COPY target/orbital.zip /opt/service/

# We depend on gitlab (or some other tool) to copy
# the UI artifacts here.
COPY target/classes/static /opt/service/webapp

RUN cd /opt/service && unzip orbital.zip


WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && orbital/bin/orbital  -Dspring.profiles.active=${PROFILE}  --spring.web.resources.static-locations=file:/opt/service/webapp $OPTIONS
