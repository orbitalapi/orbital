CACIB custom Pipeline Runner - Deserializing avro messages

Currently, the AVRO deserialization is <b>NOT</b> supported.

## Prerequisites
* Java 8+
* Maven 3.x

## Compile
`mvn clean compile`

Will also generate the classes from the Avro schema

## Running
Run the Spring Boot jar created in the `target/` folder. Or directly from IntelliJ.
Default port for the application is 9610.

## Parameters / Envvars
`SPRING_PROFILES_ACTIVE`: Set to `local` if your Eureka Server is local

`SERVICE_CASK_NAME`: CASK service name for Eureka Discovery (default 'CASK')

## Create Pipelines
You can post the following json to your server (e.g `localhost:9610/runner/pipelines`) in order to create a Pipeline (Kafka -> Cask)

```json
{
  "name" : "test-pipeline",
  "input" : {
    "type" : "imdb.Actor",
    "transport" : {
      "topic" : "pipeline-input",
      "targetType" : "imdb.Actor",
      "props" : {
        "group.id" : "vyne-pipeline-group",
        "bootstrap.servers" : "kafka:9092,",
        "heartbeat.interval.ms" : "3000",
        "session.timeout.ms" : "15000",
        "auto.offset.reset" : "earliest"
      },
      "type" : "kafka",
      "direction" : "INPUT"
    }
  },
  "output" : {
    "type" : "imdb.Actor",
    "transport" : {
      "topic" : "pipeline-output",
      "props" : {
        "value.serializer" : "org.apache.kafka.common.serialization.StringSerializer",
        "request.timeout.ms" : "120000",
        "bootstrap.servers" : "kafka:9092,",
        "key.serializer" : "org.apache.kafka.common.serialization.StringSerializer"
      },
      "targetType" : "imdb.Actor",
      "type" : "kafka",
      "direction" : "OUTPUT"
    }
  },
  "id" : "test-pipeline@196843982"
}
```

## Send messages
As for now, only plain json is supported. You can send messages to the topic defined above. The pipeline will listen to these messages and stream them to the cask.

