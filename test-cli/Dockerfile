FROM eclipse-temurin:8u342-b07-jre-focal

VOLUME /work/

WORKDIR /work/

COPY target/appassembler/ /test-cli/

ENTRYPOINT ["/test-cli/bin/vynetest"]

CMD ["test"]
