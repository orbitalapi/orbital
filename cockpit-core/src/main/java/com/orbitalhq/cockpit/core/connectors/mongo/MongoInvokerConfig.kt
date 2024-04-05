package com.orbitalhq.cockpit.core.connectors.mongo

import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.nosql.mongodb.MongoConnectionFactory
import com.orbitalhq.connectors.nosql.mongodb.MongoConnectionUIDisplayProvider
import com.orbitalhq.connectors.nosql.mongodb.MongoDbInvoker
import com.orbitalhq.connectors.registry.ConnectionUIDisplayProvider
import com.orbitalhq.schema.api.SchemaProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class)
class MongoInvokerConfig {
   @Bean
   fun mongoConnectionUIDisplayProvider(): ConnectionUIDisplayProvider = MongoConnectionUIDisplayProvider()

   @Bean
   fun mongoDbInvoker(mongoConnectionFactory: MongoConnectionFactory, schemaProvider: SchemaProvider) = MongoDbInvoker(mongoConnectionFactory, schemaProvider)
}
