FROM openjdk:8-jre-alpine

VOLUME /work/

WORKDIR /work/

COPY target/appassembler/ /test-cli/

ENTRYPOINT ["/test-cli/bin/vynetest"]

CMD ["test"]
