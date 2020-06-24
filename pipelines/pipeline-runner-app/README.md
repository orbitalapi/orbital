# Pipeline Runner App

## Prerequisites
* Java 8+
* Maven 3.x
* Kafka Tools (https://kafka.apache.org/quickstart)

## Compile
`mvn clean compile`

## Running
Run the Spring Boot jar created in the `target/` folder. Or directly from IntelliJ.
Default port for the application is `9610`.

## Parameters / Envvars
`SPRING_PROFILES_ACTIVE`: Set to `local` if your Eureka Server is local

`SERVICE_CASK_NAME`: Cask service name for Eureka Discovery (default 'CASK')

##  Create Pipelines
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
        "session.timeout.ms" : "10000",
        "auto.offset.reset" : "earliest"
      },
      "type" : "kafka",
      "direction" : "INPUT"
    }
  },
  "output" : {
    "type" : "imdb.Actor",
    "transport" : {
      "props" : {
      },
      "targetType" : "imdb.Actor",
      "type" : "cask",
      "direction" : "OUTPUT"
    }
  },
  "id" : "test-pipeline@196843982"
}

```




