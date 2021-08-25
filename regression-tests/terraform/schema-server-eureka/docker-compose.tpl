version: "3.3"


services:

   # ===================================
   # Eureka Server
   # ===================================
   eureka:
      image: vyneco/eureka:${vyne_version}
      ports:
         - 8761:8761
      environment:
        OPTIONS: --eureka.instance.preferIpAddress=true

   schema-server:
      image: vyneco/schema-server:${vyne_version}
      depends_on:
         - eureka
      ports:
         - 9301:9301
         - 9305:9305
      volumes:
         - /tmp/taxonomy:/var/lib/vyne/schemas
         - ./wait-for.sh:/wait-for.sh
      environment:
         PROFILE: eureka-schema
         OPTIONS: --taxi.schema-local-storage=/var/lib/vyne/schemas  --eureka.uri=http://${local_ip}:8761 --eureka.instance.preferIpAddress=true --eureka.instance.ipAddress=${local_ip}

