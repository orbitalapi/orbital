# Sample docker-compose

## Base docker-compose

docker-compose-base.yml

```text
version: "3.3"

services:

   vyne:
      image: vyneco/vyne:${VYNE_VERSION}
      ports:
         - 5701-5721
         - 9022:9022

   file-schema-server:
      image: vyneco/file-schema-server:${VYNE_VERSION}
      depends_on:
         - vyne
      ports:
         - 5701-5721

   cask:
      image: vyneco/cask:${VYNE_VERSION}
      depends_on:
         - vyne
      ports:
         - 5701-5721
         - 8800:8800

   pipelines-orchestrator:
      image: vyneco/pipelines-orchestrator:${VYNE_VERSION}
      depends_on:
         - vyne
      ports:
         - 5701-5721
         - 9600:9600
```

## Local environment with eureka

docker-compose-local.yml 

```text
version: "3.4"

x-default_env: &default_env
   environment:
      PROFILE:
      OPTIONS: --eureka.uri=http://eureka:8761
      JVM_OPTS: -Xmx1024m

services:

   eureka:
      image: vyneco/eureka:${VYNE_VERSION}
      ports:
         - 8761:8761

   vyne:
      <<: *default_env

   file-schema-server:
      <<: *default_env

   cask:
      <<: *default_env

   pipelines-orchestrator:
      <<: *default_env
```

## How to run

Before you start create .env folder with VYNE\_VERSION=x.x.x

`docker-compose -f docker-compose-base.yml -f docker-compose-local.yml up`





