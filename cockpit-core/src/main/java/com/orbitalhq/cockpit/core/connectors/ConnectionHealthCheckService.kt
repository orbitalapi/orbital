package com.orbitalhq.cockpit.core.connectors

import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.registry.ConnectionStatus
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.time.Duration

interface ConnectionHealthProvider {
   fun canProvideFor(config: ConnectorConfiguration): Boolean
   fun provide(config: ConnectorConfiguration): Mono<ConnectionStatus>
}

@Component
class ConnectionHealthCheckService(
   private val connectorsRegistry: SourceLoaderConnectorsRegistry,
   private val healthCheckProviders: List<ConnectionHealthProvider>
) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun getHealthCheckUpdates(pollDuration: Duration): Flux<Pair<ConnectorConfiguration, ConnectionStatus>> {
      return Flux.interval(pollDuration)
         .flatMap {
            val healthChecks = connectorsRegistry.load()
               .listAll()
               .map { connectorConfiguration ->
                  performHealthCheck(connectorConfiguration)
               }
            Flux.merge(healthChecks)

         }
   }

   private fun performHealthCheck(connectorConfiguration: ConnectorConfiguration): Mono<Pair<ConnectorConfiguration, ConnectionStatus>> {
      val healthCheck = healthCheckProviders.firstOrNull { it.canProvideFor(connectorConfiguration) }
         ?.let { it.provide(connectorConfiguration) }

      return if (healthCheck != null) {
         healthCheck.map { connectorConfiguration to it }
      } else {
         logger.warn { "No health check provider found for service ${connectorConfiguration.connectionName} (type = ${connectorConfiguration.type}, driver = ${connectorConfiguration.driverName})" }
         Mono.just(connectorConfiguration to ConnectionStatus.unknown())
      }
   }
}
