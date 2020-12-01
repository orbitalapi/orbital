# Vyne Query Server

## Overview

Vyne's service becomes your central integration point when automating integration.

### Quick start - Docker

```text
docker run -e PROFILE='embedded-discovery' -p 9022:9022 vyneco/vyne 
```

Then navigate to [http://localhost:9022](http://localhost:9022) and you should see the Vyne query server ready and waiting:

![](.gitbook/assets/image%20%2811%29.png)

We've also started Vyne with an embedded Eureka instance running.  You can see Eureka's dashboard at [http://localhost:9022/eureka-ui](http://localhost:9022/eureka-ui)

###  Quick start - Java download 

![](https://img.shields.io/badge/dynamic/xml.svg?label=latest&url=https%3A%2F%2Fnexus.vyne.io%2Frepository%2Fmaven-releases%2Fio%2Fvyne%2Fvyne-query-service%2Fmaven-metadata.xml&query=%2F%2Frelease&colorB=green&prefix=v)

Head over to our [maven repository](https://repo.vyne.co) and download the latest Vyne query server, then start by running:

```bash
java -jar -Dspring.profiles.active=embedded-discovery vyne-query-service.jar
```

## Service Discovery

Vyne uses service discovery to find services when performing integrations.  Currently, only Eureka is supported.  

#### Embedded Eureka instance

Vyne's query service can also run as a Eureka instance.  This is useful for getting up & going quickly.  To enable it, use the following config

| Mode | Flag |
| :--- | :--- |
| Docker | `-e PROFILE='embedded-discovery'` |
| Java | `-Dspring.profiles.active=embedded-discovery` |

Eureka's dashboard is available at the `/eurkea-ui` endpoint.

#### External Eureka instance

| Mode | Flag |
| :--- | :--- |
| Docker | `-e eureka.uri=http://192.168.0.4/eureka` |
| Java | `-Deureka.uri=http://localhost:8761` |



