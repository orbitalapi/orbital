package com.orbitalhq.connectors.nosql.mongodb

import com.google.common.cache.CacheBuilder
import com.mongodb.MongoClientSettings
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import com.orbitalhq.config.UpdatableConfigRepository
import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.nosql.mongodb.registry.MongoConnectionRegistry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener
import mu.KotlinLogging
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import reactor.core.publisher.Mono

class MongoConnectionFactory(private val connectionRegistry: MongoConnectionRegistry, private val metricRegistry: MeterRegistry) {
   private val mongoClientCache = CacheBuilder.newBuilder()
      .build<String, MongoClient>()

   companion object {
      private val logger = KotlinLogging.logger {}
   }
   init {
       if (connectionRegistry is UpdatableConfigRepository<*>) {
          connectionRegistry.configUpdated.subscribe {
             logger.info { "Connection registry changed - invalidating cache of Mongo connections" }
             mongoClientCache.invalidateAll()
             reactiveMongoTemplateCache.invalidateAll()
          }
       }
   }

   private val reactiveMongoTemplateCache = CacheBuilder.newBuilder()
      .build<String, ReactiveMongoTemplate>()

   fun config(connectionName: String): MongoConnectionConfiguration =
      connectionRegistry.getConnection(connectionName)

   fun reactiveMongoTemplate(connection: MongoConnectionConfiguration): ReactiveMongoTemplate {
      val mongoDbConnectionString = connection.connectionString
      val mongoClient = mongoClientCache.get(connection.connectionName) {
         val mongoClientSettings = MongoClientSettings.builder()
            .applyConnectionString(mongoDbConnectionString)
            .addCommandListener(MongoMetricsCommandListener(metricRegistry))
            .build()

         MongoClients.create(mongoClientSettings)
      }

      val reactiveMongoTemplate = reactiveMongoTemplateCache.get(connection.connectionName) {
         ReactiveMongoTemplate(mongoClient, connection.database)
      }


      return reactiveMongoTemplate
   }

   fun ping(connection: MongoConnectionConfiguration): Mono<ConnectionStatus> {
      return reactiveMongoTemplate(connection)
         .executeCommand("{ buildInfo: 1 }")
         .map {
            val connectionStatus = ConnectionStatus.healthy()
            connectionStatus
         }.onErrorResume { ex ->
            val connectionStatus = ConnectionStatus.error(ex.message ?: "Error in pinging mongoDb")
            Mono.just(connectionStatus)
         }
   }
}
