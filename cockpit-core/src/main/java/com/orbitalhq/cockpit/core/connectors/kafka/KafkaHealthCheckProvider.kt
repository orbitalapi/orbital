package com.orbitalhq.cockpit.core.connectors.kafka

import arrow.core.getOrElse
import arrow.core.getOrHandle
import com.orbitalhq.cockpit.core.connectors.ConnectionHealthProvider
import com.orbitalhq.connectors.config.kafka.KafkaConnection
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.KafkaInvoker
import com.orbitalhq.connectors.kafka.registry.test
import com.orbitalhq.connectors.registry.ConnectionStatus
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorType
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class KafkaHealthCheckProvider : ConnectionHealthProvider {
   override fun canProvideFor(config: ConnectorConfiguration): Boolean {
      return config.type == ConnectorType.MESSAGE_BROKER && config.driverName == KafkaConnection.DRIVER_NAME
   }

   override fun provide(config: ConnectorConfiguration): Mono<ConnectionStatus> {
      require(config is KafkaConnectionConfiguration) { "Expected to receive a ${KafkaConnectionConfig::class.simpleName}, but got ${config::class.simpleName}"}
      return Mono.create { sink ->
         val result = try {
            KafkaConnection.test(config)
               .map { ConnectionStatus.ok() }
               .getOrElse { ConnectionStatus.error(it) }
         } catch (e:Exception) {
            ConnectionStatus.error(e.message ?: e::class.simpleName!!)
         }
         sink.success(result)
      }
   }
}
