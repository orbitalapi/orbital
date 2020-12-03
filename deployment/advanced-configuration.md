# \(deprecated\) Advanced configuration

This page describes additional configuration that is supported by all the Vyne components including:

* [Query Server]()
* [Casks](../casks/)
* [Schema Server](../schema-server.md)
* [Pipeline Orchestrator](../pipelines/pipeline-orchestrator.md)

## Configuration

Vyne is a spring boot application. All the spring boot and Vyne specific configuration parameters can be overridden in the following way

| Mode | Flags |
| :--- | :--- |
| Docker | `-e logging.level.org.springframework=DEBUG  -e logging.level.io.vyne=DEBUG` |
| Java | `-Dlogging.level.org.springframework=DEBUG  -Dlogging.level.io.vyne=DEBUG` |

### Spring config server

By default Vyne does not require config server, all the configuration is passed via command-line. To enable config server simply add these command-line parameters:

| Mode | Flags |
| :--- | :--- |
| Docker | `-e spring.cloud.config.enabled=true  -e spring.cloud.config.uri=http://config-server:8888` |
| Java | `-Dspring.cloud.config.enabled=true  -Dspring.cloud.config.uri=http://config-server:8888` |

## Logging

By default Vyne logs everything to standard output \(console\)

### Logstash

Vyne supports exporting logs to ElasticSearch via Logstash. Here is the way to enable logstash server:

| Mode | Flags |
| :--- | :--- |
| Docker | `-e PROFILE='loogstash'  -e logstash.hostname=localhost:5044` |
| Java | `-Dspring.profiles.active='loogstash'  -Dlogstash.hostname=localhost:5044` |

