package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.connectors.config.mongodb.MongoConnection
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.nosql.mongodb.registry.InMemoryMongoConnectionRegistry
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class MongoConnectionHealthCheckTest: MongoDbTestcontainer() {
   @Test
   fun `can check mongodb connection status successfully`() {
      val connectionParams = mapOf(MongoConnection.Parameters.CONNECTION_STRING.templateParamName to connectionString)
      val mongo1ConnectionConfig = MongoConnectionConfiguration("mongo1", connectionParams)
      val mongoConnectionFactory =  MongoConnectionFactory(InMemoryMongoConnectionRegistry(listOf(mongo1ConnectionConfig)))

      StepVerifier
         .create(mongoConnectionFactory.ping(mongo1ConnectionConfig))
         .expectNextMatches {
            it.status == ConnectionStatus.Status.OK
         }.expectComplete().verify()
   }

   @Test
   fun `can detect mongodb connection errors when mongo password is wrong`() {
      val connectionParams = mapOf(MongoConnection.Parameters.CONNECTION_STRING.templateParamName to connectionStringWithInvalidPassword)
      val mongo1ConnectionConfig = MongoConnectionConfiguration("mongo1", connectionParams)
      val mongoConnectionFactory =  MongoConnectionFactory(InMemoryMongoConnectionRegistry(listOf(mongo1ConnectionConfig)))

      StepVerifier
         .create(mongoConnectionFactory.ping(mongo1ConnectionConfig))
         .expectNextMatches {
            it.status == ConnectionStatus.Status.ERROR
         }.expectComplete().verify()
   }

}
