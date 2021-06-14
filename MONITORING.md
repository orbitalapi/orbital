# Monitoring Systems

Cask and VYNE may be run with the following command line option to point to an overriding configuration file

```
java -jar vyne-query-server.jar--spring.config.additional-location=file:/config/vyne.yml
```

##Sample configuration files for supported monitoring systems

###datadog

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
```yaml
management:
   metrics:
      export:
        jmx:
           enabled: true
           domain: vyne-metrics
```

###prometheus
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



