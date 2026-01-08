# Schema Publisher Spring Boot Starter

A Spring Boot Starter that automatically publishes Taxi schemas from Spring Boot applications to an Orbital Schema Server.

## Usage

Add the dependency to your Spring Boot application:

```xml
<dependency>
    <groupId>com.orbitalhq</groupId>
    <artifactId>schema-publisher-spring-boot-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

The starter will automatically:
- Discover Taxi schemas in your application
- Publish them to the configured schema server on startup

## Configuration

Configure the publisher in your `application.yml` or `application.properties`:

```yaml
orbital:
  schema:
    publisher:
      enabled: true                          # Enable/disable publishing (default: true)
      publisher-id: my-service               # Publisher ID (default: ${spring.application.name})
      url: http://localhost:9022             # Schema server URL (default: http://localhost:9022)
      connection-timeout: 5000               # Connection timeout in ms (default: 5000)
```

Or in `application.properties`:

```properties
orbital.schema.publisher.enabled=true
orbital.schema.publisher.publisher-id=my-service
orbital.schema.publisher.url=http://localhost:9022
orbital.schema.publisher.connection-timeout=5000
```

## Disabling the Publisher

To disable schema publishing, set:

```yaml
orbital:
  schema:
    publisher:
      enabled: false
```

## Implementation Status

This starter is currently scaffolded with placeholder implementations. The following components need to be implemented:

1. **HTTP Transport**: Create actual HTTP-based SchemaPublisherTransport
2. **Schema Discovery**: Implement mechanism to discover Taxi schemas from:
   - Annotated classes (@DataType, @Service, etc.)
   - Taxi files in classpath (META-INF/taxi/*.taxi)
   - Spring components converted via java2taxi
3. **Schema Loading**: Load and parse discovered schemas
4. **Publication**: Publish schemas using the SchemaPublisherService

## Architecture

The starter follows Spring Boot autoconfiguration conventions:

- `SchemaPublisherProperties`: Configuration properties
- `SchemaPublisherAutoConfiguration`: Main autoconfiguration class
- `SchemaLoader`: Component for discovering and loading schemas
- Auto-registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
