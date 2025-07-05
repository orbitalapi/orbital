package com.orbitalhq.cockpit.core.connectors.kafka

import arrow.core.getOrElse
import com.orbitalhq.cockpit.core.connectors.ConnectionHealthProvider
import com.orbitalhq.cockpit.core.connectors.HealthCheckLogHelper
import com.orbitalhq.connectors.config.kafka.KafkaConnection
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.test
import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorCategory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class KafkaHealthCheckProvider(
   private val logHelper: HealthCheckLogHelper
) : ConnectionHealthProvider {
   // To reduce the noise of logging (but not remove it completely), we hold a hashmap of
   // endpoints that are healthy, and the last time that we logged about it.
   //

   override fun canProvideFor(config: ConnectorConfiguration): Boolean {
      return config.type == ConnectorCategory.MESSAGE_BROKER && config.driverName == KafkaConnection.DRIVER_NAME
   }

   override fun provide(config: ConnectorConfiguration): Mono<ConnectionStatus> {
      require(config is KafkaConnectionConfiguration) { "Expected to receive a ${KafkaConnectionConfig::class.simpleName}, but got ${config::class.simpleName}"}
      return Mono.create { sink ->
         val result = try {
            KafkaConnection.test(config)
               .map { ConnectionStatus.healthy() }
               .getOrElse { ConnectionStatus.error(it) }
         } catch (e:Exception) {
            ConnectionStatus.error(e.message ?: e::class.simpleName!!)
         }
         logHelper.logHealthStatus(config, result)
         sink.success(result)
      }
   }
}
