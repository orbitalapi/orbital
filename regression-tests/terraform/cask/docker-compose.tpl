version: "3.3"

services:
  cask:
    image: vyneco/casknodb:${vyne_version}
    environment:
      PROFILE: eureka-schema,logstash
      OPTIONS: --eureka.uri=http://${eureka-ip}:8761 --eureka.instance.preferIpAddress=true --eureka.instance.ipAddress=${local_ip} --spring.datasource.url=jdbc:postgresql://${db-ip}/vynedb --spring.datasource.password=${db-password} --logstash.hostname=${elk-ip}
    ports:
      - 8800:8800





