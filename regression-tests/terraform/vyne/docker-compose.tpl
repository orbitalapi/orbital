version: "3.3"


services:
  vyne:
    image: vyneco/vyne:${vyne_version}
    ports:
      - 5701-5721
      - 9022:9022
    environment:
      PROFILE: eureka-schema,prometheus,logstash
      OPTIONS: --eureka.uri=http://${eureka-ip}:8761 --eureka.instance.preferIpAddress=true --eureka.instance.ipAddress=${local_ip} --logstash.hostname=${elk-ip} --management.metrics.export.elastic.enabled=true --management.endpoints.web.exposure.include=elastic,prometheus,metrics,info,health,logfile,loggers --management.metrics.export.elastic.host=http://${elk-ip}:9200 --management.metrics.export.elastic.user-name=elastic --management.metrics.export.elastic.password=changeme --management.metrics.export.elastic.index=vyne-metrics
