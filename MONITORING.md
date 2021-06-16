# Monitoring Systems

Cask and VYNE may be run with the following command line option to point to an overriding configuration file

```
java -jar vyne-query-server.jar--spring.config.additional-location=file:/config/vyne.yml
```

##Metrics

Counters

- schema.import.sources.success
- schema.import.sources.errors
- cask.import.success
- cask.import.rejected


Gauge 

- schema.compiled.count
- cask.count
- cask.row.counts (multi gauge tagged by cask name)

##Supported Monitoring Systems

###datadog

Config
```yaml
management:
   metrics:
      export:
        datadog:
           enabled: true
           api-key:
           uri: https://api.datadoghq.com
```

###dynatrace

Config
```yaml
management:
   metrics:
      export:
        dynatrace:
           enabled: true
           technology-type: java
           api-token:
           group: vyne
```

###elastic

Config
```yaml
management:
   metrics:
      export:
        elastic:
           enabled: true
           host: http://127.0.0.1:9200
           user-name: vyne
           password: vyne
           index: vyne-metrics
```

###graphite

Config
```yaml
management:
   metrics:
      export:
        graphite:
           enabled: true
           port: 2004
           host: localhost
```

###influx

Config
```yaml
management:
   metrics:
      export:
        influx:
           enabled: true
           db: vyne
           uri: http://127.0.0.1:8086
           user-name: vyne
           password: vyne
```

###jmx

Config
```yaml
management:
   metrics:
      export:
        jmx:
           enabled: true
           domain: vyne-metrics
```

###prometheus

Vyne Query service

http://localhost:9022/api/actuator/prometheus

Cask

http://localhost:8800/api/actuator/prometheus

Config
```yaml
management:
   metrics:
      export:
        prometheus:
           enabled: true

   endpoints:
      web:
         exposure:
            include: prometheus

   endpoint:
      prometheus:
         enabled: true
```



