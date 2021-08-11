---
description: >-
  In this section you going to learn how to build fully functional instrument
  service integrating with Vyne.
---

# Sample Instrument Service

## Getting started

![](https://img.shields.io/badge/dynamic/xml.svg?label=Latest&url=http%3A%2F%2Frepo.vyne.co%2Frelease%2Fio%2Fvyne%2Fplatform%2Fmaven-metadata.xml&query=%2F%2Frelease&colorB=green&prefix=v&style=for-the-badge&logo=kotlin&logoColor=white)

Once you've [set-up Vyne locally](setting-up-vyne-locally.md), we can start building services that can leverage Vyne's automated integration. Vyne is built with Spring Boot, and though it's not a requirement to use spring boot, it's the easiest way to get going.

All the steps below are based on our [instrument-service](https://gitlab.com/vyne/demos/-/tree/master/finance/instrument-service) demo project. It contains only what's needed to expose Vyne service so please use it as a template.

**Prerequisites:**

* Java 8
* Maven 3.x

## Configuring Maven pom.xml

Our project will need the following dependencies:

* io.vyne.vyne-spring - Provides set of annotations enabling spring-boot app communication with Vyne.
* lant.taxi.taxi-annotation-processor - Based on java annotations generates taxi types/schema that will be deployed to Vyne.
* spring-boot-starter-web - This will allow us to build REST endpoints.
* spring-cloud-starter-netflic-eureka-client - This will register service with eureka discovery service.

### **Dependencies**

```markup
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.2.6.RELEASE</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>instrument-service</artifactId>

    <properties>
        <maven.compiler.target>1.8</maven.compiler.target>
        <maven.compiler.source>1.8</maven.compiler.source>
        <kotlin.version>1.3.50</kotlin.version>
        <taxi.version>0.5.0</taxi.version>
        <vyne.version>0.3.1-SNAPSHOT</vyne.version>
        <spring.cloud.netflix.version>2.2.2.RELEASE</spring.cloud.netflix.version>
    </properties>

    <dependencies>
        <!-- Provides set of annotations enabling spring-boot app communication with Vyne. -->
        <dependency>
            <groupId>io.vyne</groupId>
            <artifactId>vyne-spring</artifactId>
            <version>${vyne.version}</version>
        </dependency>
        <!-- Based on java annotations generates taxi types/schema that will be deployed to Vyne. -->
        <dependency>
            <groupId>lang.taxi</groupId>
            <artifactId>taxi-annotation-processor</artifactId>
            <version>${taxi.version}</version>
        </dependency>
        <!-- This will allow us to build REST endpoints. -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- This will register service with eureka discovery service. -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
            <version>${spring.cloud.netflix.version}</version>
        </dependency>
    </dependencies>
```

### Repositories

Vyne and Taxi artefacts are not deployed to maven public repo, hence the need to custom repositories section:

```markup
    <repositories>
        <repository>
            <id>bintray-taxi-lang-releases</id>
            <url>https://dl.bintray.com/taxi-lang/releases</url>
        </repository>
        <repository>
            <id>vyne-releases</id>
            <url>http://repo.vyne.co/release</url>
        </repository>
        <repository>
            <id>vyne-snapshots</id>
            <url>http://repo.vyne.co/snapshot</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>
```

### Build

Our project uses kotlin, so we going to use kotlin-maven-plugin to build it and to generate taxi types.

```markup
<build>
        <plugins>
            <plugin>
                <artifactId>kotlin-maven-plugin</artifactId>
                <groupId>org.jetbrains.kotlin</groupId>
                <version>${kotlin.version}</version>
                <executions>
                    <execution>
                        <id>kapt</id>
                        <goals>
                            <goal>kapt</goal>
                        </goals>
                        <configuration>
                            <sourceDirs>
                                <sourceDir>src/main/java</sourceDir>
                            </sourceDirs>
                            <annotationProcessorPaths>
                                <annotationProcessorPath>
                                    <groupId>lang.taxi</groupId>
                                    <artifactId>taxi-annotation-processor</artifactId>
                                    <version>${taxi.version}</version>
                                </annotationProcessorPath>
                            </annotationProcessorPaths>
                        </configuration>
                    </execution>
                    <execution>
                        <id>compile</id>
                        <goals>
                            <goal>compile</goal>
                        </goals>
                        <configuration>
                            <sourceDirs>
                                <sourceDir>src/main/java</sourceDir>
                            </sourceDirs>
                        </configuration>
                    </execution>

                    <execution>
                        <id>test-compile</id>
                        <goals>
                            <goal>test-compile</goal>
                        </goals>
                        <configuration>
                            <sourceDirs>
                                <sourceDir>src/test/kotlin</sourceDir>
                                <sourceDir>src/test/java</sourceDir>
                                <sourceDir>target/generated-sources/kapt/test</sourceDir>
                            </sourceDirs>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

Note: taxi-annotation-processor is a processor generating taxi-types based on annotations in your code. 

Generated code can be found in target/generated-source/kaptKotlin/compile.

## Creating instrument service

### Spring boot main

First step is to create typical spring boot app with two extra Vyne and Eureka annotations. This will enable communication with Vyne schema server and Eureka service discovery.

```markup
@SpringBootApplication
@EnableEurekaClient
@VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.DISTRIBUTED)
open class InstrumentServiceApp {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Register Instrument service taxi types in Vyne.
            TypeAliasRegistry.register(TypeAliases::class.java)

            // start spring boot app
            SpringApplication.run(InstrumentServiceApp::class.java, *args)
        }
    }
}
```

### Spring boot config

Below you'll find example of how to Configure Eureka discovery service and Vyne schema. This is required for vyne to be able to discover your service and call it. It also need a name and the version.

```markup
spring:
  application:
    name: instrument-service

server:
  port: 9402

eureka:
  uri: http://127.0.0.1:9022
  client:
    registryFetchIntervalSeconds: 1
    initialInstanceInfoReplicationIntervalSeconds: 5
    serviceUrl:
      defaultZone: ${eureka.uri}/eureka/
  instance:
    leaseRenewalIntervalInSeconds: 2
    leaseExpirationDurationInSeconds: 5
    # this is enabling vyne running in docker to call service running in your IDE
    preferIpAddress: true

vyne:
  schema:
    name: ${spring.application.name} # Name of the app exposed to Vyne
    version: 0.1.0 # Version of the app exposed to Vyne 
```

### Defining Taxi Types

Our service needs to define model \(Taxi types\) discover-able through Vyne. This can be done simply by annotating your pojo objects with Taxi Annotations.

```markup
@DataType("demo.Instrument")
data class Instrument(
        val isin: Isin,
        val name: LegalEntityName,
        val symbol: Symbol,
        val countryCode: CountryCode
)

@DataType("demo.instrument.Isin")
typealias Isin = String

@DataType("demo.instrument.LegalEntityName")
typealias LegalEntityName = String

@DataType("demo.instrument.Symbol")
typealias Symbol = String

@DataType("demo.instrument.CountryCode")
typealias CountryCode = String
```

Now, to generate taxi types you have to execute maven command:

`mvn compile`

After that your IDE should pick TypeAliases dependency.

### Exposing REST endpoints

Now when we got the types we can create REST endpoints. In our example we going to expose two endpoints:

* listing all instruments,
* looking up for instrument by ISIN.

```markup
@RestController
@Service
class InstrumentService {
    private val instruments = listOf(
            Instrument("US0378331005", "Apple", "AAPL", "USA"),
            Instrument("US02079K1079", "Alphabet C (ex Google)", "GOOG", "USA"),
            Instrument("GB00B03MLX29", "Royal Dutch Shell A", "RDSA", "UK")
    )

    @Operation
    @GetMapping("/instrument/{isin}")
    fun getInstrument(@PathVariable("isin") isin: Isin): Instrument {
        return instruments.first { it.isin == isin }
    }

    @Operation
    @GetMapping("/instruments")
    fun getAllInstruments(): List<Instrument> {
        return instruments
    }
}
```

## Starting instrument service

For this step to work we need a running Vyne instance. If you don't have it please follow instructions of how [set-up Vyne locally](setting-up-vyne-locally.md).

For the app to connect to Vyne we need to specify hazelcast.config location \(refer to [set-up Vyne locally](setting-up-vyne-locally.md)\)

This is to allow your app to join the Hazelcast cluster created by Vyne.

{% tabs %}
{% tab title="Command line" %}
Open command line prompt and navigate to your demo app, e.g.

cd ...demo/finance/instrument-service

and type in the following maven command:

```markup
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dhazelcast.config=../../vyne-docker-stack/hazelcast.yml"
```
{% endtab %}

{% tab title="Intellij" %}
Before starting the app append this system property 

```markup
-Dhazelcast.config=...path\to\demos\vyne-docker-stack\hazelcast.yml
```

![](../.gitbook/assets/image%20%287%29.png)
{% endtab %}
{% endtabs %}

If everything went smoothly you should see in the console log no exceptions and the following statement:

```markup
Members {size:2, ver:14} [
        Member [192.168.8.112]:5701 - d9fe0048-8df2-47b1-8c6b-b26c66eab0ae
        Member [192.168.8.112]:5702 - 20836466-429d-46b9-a49d-7af1c7dbc6fd this
]
```

This means your app successfully joined Vyne cluster.

## Testing

### Vyne schema-explorer

Vyne schema-explorer shows all the services discover-able through Vyne. 

When you navigate to [http://localhost:9022/schema-explorer](http://localhost:9022/schema-explorer) you should see:

* instrument-service,
* version 0.1.0 \(as defined in application.yml\), 
* all our taxi types,
* two REST endpoints.

![](../.gitbook/assets/image%20%2819%29.png)

### Querying

Vyne offers couple of ways of querying, for a full list refer to [Query Api](../running-a-local-taxonomy-editor-environment/query-api.md) section.

#### Querying all instruments

Vyne UI offers an easy way for testing your services. To query for all the instruments:

* navigate to [http://localhost:9022/query-wizard](http://localhost:9022/query-wizard),
* select type to discover 'demo.Instrument',
* select 'Find as array' checkbox'
* click Submit Query.

![](../.gitbook/assets/image%20%2817%29.png)

In the result you can see all the instruments defined by your service, the endpoint /instruments and how long it took to call that endpoint.

#### Looking up instrument by ISIN

Same as before navigate to [http://localhost:9022/query-wizard](http://localhost:9022/query-wizard) and 

* click 'Add new fact' button
* paste Isin value 'US0378331005' \(Apple\)
* select type to discover 'demo.Instrument'
* click 'Submit query' button

![](../.gitbook/assets/image%20%2830%29.png)

As you can see this time different endpoint was used to discover that data /instrument/{isin}, and only single instrument representing 'Apple' stock was returned.

## Summary

Congratulations!

In this section you have learned to build:

* sample instrument service,
* how to connect to vyne,
* how to query your service.



