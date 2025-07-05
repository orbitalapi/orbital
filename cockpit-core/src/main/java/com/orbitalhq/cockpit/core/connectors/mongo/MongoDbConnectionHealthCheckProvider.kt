package com.orbitalhq.cockpit.core.connectors.mongo

import com.orbitalhq.cockpit.core.connectors.ConnectionHealthProvider
import com.orbitalhq.cockpit.core.connectors.HealthCheckLogHelper
import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.connectors.config.mongodb.MongoConnection
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.nosql.mongodb.MongoConnectionFactory
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorCategory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono


@Component
class MongoDbConnectionHealthCheckProvider(
   private val logHelper: HealthCheckLogHelper,
   private val mongoConnectionFactory: MongoConnectionFactory
): ConnectionHealthProvider {
   override fun canProvideFor(config: ConnectorConfiguration): Boolean {
      return config.type == ConnectorCategory.NO_SQL && config.driverName == MongoConnection.DRIVER_NAME
   }

   override fun provide(config: ConnectorConfiguration): Mono<ConnectionStatus> {
     return mongoConnectionFactory
         .ping(config as MongoConnectionConfiguration)

   }
}
