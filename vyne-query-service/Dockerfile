FROM eclipse-temurin:11.0.13_8-jre-focal

RUN addgroup --system worker --gid 1000 && \
    adduser --system worker --ingroup worker --uid 1001

RUN mkdir -p /opt/service
RUN chown worker:worker /opt/service/
RUN apt-get update && apt-get install zip -y

USER worker
WORKDIR /opt/service/

COPY --chown=worker target/vyne.zip /opt/service/
RUN cd /opt/service && unzip vyne.zip

WORKDIR /opt/service/

CMD $BEFORE_START_COMMAND && vyne/bin/vyne  -Dspring.profiles.active=${PROFILE} $OPTIONS
