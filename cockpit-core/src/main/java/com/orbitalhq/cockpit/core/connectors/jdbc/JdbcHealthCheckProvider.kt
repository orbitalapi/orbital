package com.orbitalhq.cockpit.core.connectors.jdbc

import arrow.core.getOrElse
import com.orbitalhq.cockpit.core.connectors.ConnectionHealthProvider
import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.jdbc.DatabaseMetadataService
import com.orbitalhq.connectors.jdbc.SimpleJdbcConnectionFactory
import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.connectors.jdbc.drivers.DatabaseDriverRegistry
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorType
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class JdbcHealthCheckProvider(private val driverRegistry: DatabaseDriverRegistry) : ConnectionHealthProvider {
   override fun canProvideFor(config: ConnectorConfiguration): Boolean = config.type == ConnectorType.JDBC

   override fun provide(config: ConnectorConfiguration): Mono<ConnectionStatus> {
      require(config is JdbcConnectionConfiguration) { "Expected to receive a ${JdbcConnectionConfiguration::class.simpleName}, but got ${config::class.simpleName}" }
      return testConnection(config, driverRegistry)
   }

   companion object {
      fun testConnection(connectionConfig: JdbcConnectionConfiguration, driverRegistry: DatabaseDriverRegistry): Mono<ConnectionStatus> {
         return Mono.create { sink ->
            val connectionProvider = SimpleJdbcConnectionFactory()
            val metadataService =
               DatabaseMetadataService(connectionProvider.jdbcTemplate(connectionConfig).jdbcTemplate, connectionConfig)
            val testQuery =  driverRegistry.getDatabaseDriver(connectionConfig.jdbcDriver)
               .jdbcDriverMetadata.testQuery
            val status = metadataService.testConnection(testQuery)
               .map { ConnectionStatus.healthy() }
               .getOrElse { errorMessage -> ConnectionStatus.error(errorMessage) }
            sink.success(status)
         }
      }
   }
}
