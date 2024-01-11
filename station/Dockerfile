# Re: BASE_IMAGE_TAG:
# For production, we prefer 21-jdk-alpine
# which is a thinner image, and very few security
# advisories.
# However, it causes issues on M2 macs, with runtime
# errors at startup:
#
# A fatal error has been detected by the Java Runtime Environment:
#
#  SIGSEGV (0xb) at pc=0x00000000000226a0, pid=1, tid=66
#
# JRE version: OpenJDK Runtime Environment Temurin-21.0.1+12 (21.0.1+12) (build 21.0.1+12-LTS)
# Java VM: OpenJDK 64-Bit Server VM Temurin-21.0.1+12 (21.0.1+12-LTS, mixed mode, sharing, tiered, compressed oops, compressed class ptrs, serial gc, linux-aarch64)
#
# Others have reported similar issues, and looks like a bug in either Docker or somewhere in the Apple stack:
# https://github.com/docker/for-mac/issues/7006
#
# Swapping back to jammy worked for M2's
# Therefore, we produce two images:
# -alpine (for prod) and -jammy (for mac's)
ARG BASE_IMAGE_TAG

ARG REGISTRY
# Generating SOAP clients from WSDLs at runtime requires the JDK, not JRE
# As Apache CXF generates and compiles code.
# We need to use a temruin 21 alpine image as earlier versions don't support arm64
FROM ${REGISTRY}eclipse-temurin:21-jdk-${BASE_IMAGE_TAG:-alpine}

RUN apt-get update && apt-get install zip -y

WORKDIR /opt/service/

COPY target/orbital.zip /opt/service/

# We depend on gitlab (or some other tool) to copy
# the UI artifacts here.
COPY target/classes/static /opt/service/webapp

RUN cd /opt/service && unzip orbital.zip


WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && orbital/bin/orbital  -Dspring.profiles.active=${PROFILE}  --spring.web.resources.static-locations=file:/opt/service/webapp,classpath:/public $OPTIONS
