package com.orbitalhq.connectors.nosql.mongodb

import com.google.common.cache.CacheBuilder
import com.mongodb.BasicDBObject
import com.mongodb.MongoClientSettings
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.nosql.mongodb.registry.MongoConnectionRegistry
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import reactor.core.publisher.Mono

class MongoConnectionFactory(private val connectionRegistry: MongoConnectionRegistry) {
   private val mongoClientCache = CacheBuilder.newBuilder()
      .build<String, MongoClient>()

   private val reactiveMongoTemplateCache = CacheBuilder.newBuilder()
      .build<String, ReactiveMongoTemplate>()

   fun config(connectionName: String): MongoConnectionConfiguration =
      connectionRegistry.getConnection(connectionName)

   fun reactiveMongoTemplate(connection: MongoConnectionConfiguration): ReactiveMongoTemplate {
      val mongoDbConnectionString = connection.connectionString
     val mongoClient =  mongoClientCache.get(connection.connectionName) {
         val mongoClientSettings = MongoClientSettings.builder()
           .applyConnectionString(mongoDbConnectionString)
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
