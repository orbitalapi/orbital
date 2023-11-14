package com.orbitalhq.cockpit.core.connectors.jdbc

import arrow.core.getOrElse
import arrow.core.getOrHandle
import com.orbitalhq.cockpit.core.connectors.ConnectionHealthProvider
import com.orbitalhq.cockpit.core.connectors.ConnectionTestedSuccessfully
import com.orbitalhq.cockpit.core.connectors.kafka.KafkaConnectionConfig
import com.orbitalhq.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.jdbc.DatabaseMetadataService
import com.orbitalhq.connectors.jdbc.SimpleJdbcConnectionFactory
import com.orbitalhq.connectors.registry.ConnectionStatus
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorType
import com.orbitalhq.utils.orElse
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class JdbcHealthCheckProvider : ConnectionHealthProvider {
   override fun canProvideFor(config: ConnectorConfiguration): Boolean = config.type == ConnectorType.JDBC

   override fun provide(config: ConnectorConfiguration): Mono<ConnectionStatus> {
      require(config is JdbcConnectionConfiguration) { "Expected to receive a ${JdbcConnectionConfiguration::class.simpleName}, but got ${config::class.simpleName}" }
      return testConnection(config)
   }

   companion object {
      fun testConnection(connectionConfig: JdbcConnectionConfiguration): Mono<ConnectionStatus> {
         return Mono.create { sink ->
            val connectionProvider = SimpleJdbcConnectionFactory()
            val metadataService =
               DatabaseMetadataService(connectionProvider.jdbcTemplate(connectionConfig).jdbcTemplate)
            val status = metadataService.testConnection(connectionConfig.jdbcDriver.metadata.testQuery)
               .map { ConnectionStatus.ok() }
               .getOrElse { errorMessage -> ConnectionStatus.error(errorMessage) }
            sink.success(status)
         }
      }
   }
}
