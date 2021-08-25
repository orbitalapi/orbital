version: "3.3"


services:
  vyne:
    image: vyneco/vyne:${vyne_version}
    network_mode: "host"
    ports:
      - 5701-5721
      - 9022:9022
    environment:
      AWS_REGION: eu-west-2
      PROFILE: eureka-schema,prometheus,logstash
      OPTIONS: --vyne.projection.distributionMode=HAZELCAST --vyne.hazelcast.enabled=true  --vyne.hazelcast.useMetadataForHostAndPort=true --vyne.hazelcast.networkInterface=10.0.*.* --vyne.hazelcast.discovery=eureka --vyne.hazelcast.eurekaUri=http://${eureka-ip}:8761/eureka/ --eureka.uri=http://${eureka-ip}:8761 --eureka.instance.preferIpAddress=true --eureka.instance.ipAddress=${local_ip} --logstash.hostname=${elk-ip} --management.metrics.export.elastic.enabled=true --management.endpoints.web.exposure.include=elastic,prometheus,metrics,info,health,logfile,loggers --management.metrics.export.elastic.host=http://${elk-ip}:9200 --management.metrics.export.elastic.user-name=elastic --management.metrics.export.elastic.password=changeme --management.metrics.export.elastic.index=vyne-metrics
