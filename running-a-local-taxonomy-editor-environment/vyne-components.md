# Vyne Components

## Schema Stores

The Schema Store is the central location for services to register with Vyne, and upload their schema.

Schema's are described via [Taxi](https://docs.taxilang.org) - though support for other formats is coming soon.

### HTTP

```kotlin
@EnableVyne(remoteSchemaStore = RemoteSchemaStoreType.HTTP)
```

### Hazelcast

```kotlin
@EnableVyne(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
```

Forms a hazelcast cluster of the Vyne nodes, which allow for resilient sharing and updating of schema's.  

## Service Discovery

Vyne requires **some kind** of service discovery to be present, in order for it to look up the services it needs to interact with.  Currently, Eureka is the preferred discovery client.  However, any working implementation of Spring's `DiscoveryClient` should work \(ie., Consul or Zookeeper\), but these haven't been tested yet.

Better support for these - and other - service discovery mechanisms is planned.

