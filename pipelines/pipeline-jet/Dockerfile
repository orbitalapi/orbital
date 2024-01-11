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
# We need to use a temruin 21 alpine image as earlier versions don't support arm64
FROM ${REGISTRY}eclipse-temurin:21-jre-${BASE_IMAGE_TAG:-alpine}

COPY target/pipeline-jet.jar /opt/service/

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && java $JVM_OPTS -jar -Dspring.profiles.active=${PROFILE} pipeline-jet.jar $OPTIONS
