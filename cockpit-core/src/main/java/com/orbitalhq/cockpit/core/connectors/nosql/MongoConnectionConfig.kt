package com.orbitalhq.cockpit.core.connectors.nosql

import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.nosql.mongodb.MongoConnectionFactory
import com.orbitalhq.connectors.nosql.mongodb.registry.MongoConnectionRegistry
import com.orbitalhq.connectors.nosql.mongodb.registry.SourceLoaderMongoConnectionRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class)
class MongoConnectionConfig {
   @Bean
   fun mongoConnectionRegistry(config: SourceLoaderConnectorsRegistry): MongoConnectionRegistry {
      return SourceLoaderMongoConnectionRegistry(config)
   }

   @Bean
   fun mongoConnectionFactory(mongoConnectionRegistry: MongoConnectionRegistry): MongoConnectionFactory {
      return MongoConnectionFactory(mongoConnectionRegistry)
   }
}
