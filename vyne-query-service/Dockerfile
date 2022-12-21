FROM eclipse-temurin:11.0.16.1_1-jre-focal as builder

RUN apt-get update && apt-get install zip -y

WORKDIR /opt/service/

COPY target/vyne.zip /opt/service/
RUN cd /opt/service && unzip vyne.zip

WORKDIR /opt/service/

FROM public.ecr.aws/lambda/java:11
COPY --from=builder /opt/service/ /opt/service/
CMD $BEFORE_START_COMMAND && vyne/bin/vyne -Dspring.profiles.active=${PROFILE} $OPTIONS
