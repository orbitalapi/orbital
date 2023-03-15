FROM maven:3-jdk-11 as parent
ENV MAVEN_CONFIG="" \
    SNYK_INTEGRATION_NAME="DOCKER_SNYK" \
    SNYK_INTEGRATION_VERSION=maven-3-jdk-11 \
    SNYK_CFG_DISABLESUGGESTIONS=true
WORKDIR /app
RUN curl -fsSL https://get.docker.com -o get-docker.sh && sh ./get-docker.sh
ENTRYPOINT []
CMD ["snyk", "test"]

FROM ubuntu as snyk
RUN apt-get update && apt-get install -y curl
RUN curl -o ./snyk-linux https://static.snyk.io/cli/latest/snyk-linux && \
    curl -o ./snyk-linux.sha256 https://static.snyk.io/cli/latest/snyk-linux.sha256 && \
    sha256sum -c snyk-linux.sha256 && \
    mv snyk-linux /usr/local/bin/snyk && \
    chmod +x /usr/local/bin/snyk

FROM parent as linux
COPY --from=snyk /usr/local/bin/snyk /usr/local/bin/snyk
