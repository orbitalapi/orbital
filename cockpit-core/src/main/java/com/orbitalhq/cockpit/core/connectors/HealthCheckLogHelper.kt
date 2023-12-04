package com.orbitalhq.cockpit.core.connectors

import com.orbitalhq.cockpit.core.connectors.kafka.KafkaHealthCheckProvider
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.registry.ConnectionStatus
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Health checks can be really noisy when logging, so we tend to reduce the default
 * log levels in application.yml. However, that means we lose important information.
 *
 * This class logs occasionally when something is unhealthy, and logs changes to connection health
 * status.
 */
@Component
class HealthCheckLogHelper(
   private val logBufferDuration: Duration = Duration.ofMinutes(2)
) {
   private val unhealthyEndpoints = ConcurrentHashMap<ConnectorConfiguration, Instant>()
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun logHealthStatus(config: KafkaConnectionConfiguration, result: ConnectionStatus) {
      val mostRecentUnhealthyLogTime = unhealthyEndpoints[config]
      when {
         result.status == ConnectionStatus.Status.ERROR -> logIfPastThreshold(config, result, mostRecentUnhealthyLogTime)
         result.status == ConnectionStatus.Status.OK && mostRecentUnhealthyLogTime != null -> logConnectionReturnedToHealthy(config)
      }
   }

   private fun logConnectionReturnedToHealthy(config: KafkaConnectionConfiguration) {
      logger.info { "Connection ${config.connectionName} is healthy again" }
      unhealthyEndpoints.remove(config)
   }


   private fun logIfPastThreshold(
      config: KafkaConnectionConfiguration,
      result: ConnectionStatus,
      mostRecentUnhealthyLogTime: Instant?
   ) {
      val lastLogged = mostRecentUnhealthyLogTime ?: Instant.ofEpochSecond(0)
      if (Duration.between(lastLogged, Instant.now()) >= logBufferDuration) {
         logger.warn { "Connection ${config.connectionName} is unhealthy: $result" }
         unhealthyEndpoints[config] = Instant.now()
      }
   }
}
