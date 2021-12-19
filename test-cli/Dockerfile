FROM eclipse-temurin:8u312-b07-jre-focal

VOLUME /work/

WORKDIR /work/

COPY target/appassembler/ /test-cli/

ENTRYPOINT ["/test-cli/bin/vynetest"]

CMD ["test"]
