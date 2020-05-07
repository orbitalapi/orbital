# CACIB custom Pipeline Runner
Special runner embedding custom logic to deserialize Avro messages.

## Prerequisites
* Java 8+
* Maven 3.x

## Compile
`mvn clean compile`

Note: This will also generate the classes from the Avro schema

## Running
Run the Spring Boot jar created in the `target/` folder. Or directly from IntelliJ.
Default port for the application is 9610.

## Parameters / Envvars
`SPRING_PROFILES_ACTIVE`: Set to `local` if your Eureka Server is local

`SERVICE_CASK_NAME`: Cask service name for Eureka Discovery (default 'CASK')

## Create Pipelines
Two types of Kafka inputs are accepted: `kafka` and `kafka-cacib`.
The first one expects a plain Json string in the message. The second one expects an Avro binary message compliant with the schema in `src/main/resources/matrix-recordv1.avsc`

<b>NOTE</b> Please note that at this point, the schema `matrix-recordv1.avsc` isn't exactly the one from CACIB. The field 
```{
    "name" : "SentTimeUtc",
    "type" : "long",
    "doc": "Producer Components will put this as System time in current milliseconds, so we know when it was sent last. This can be used for monitoring ass well to know when was the last message came from a particular component"
 }
 ``` 
is missing as there are serialiation issues with the `long` type which needs to be investigated.

#### Example
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

Just replace `"type" : "kafka"` with `"type" : "kafka-cacib"` to listen to avro messages 

## Generate binary Avro messages
A small script is available `AvroMessageWriter.kt` is available to generate binary messages. Messages can then be sent to kafka with the command: 

```
kafka-console-producer.sh --bootstrap-server kafka:9092 --topic pipeline-input < file.avro
```



