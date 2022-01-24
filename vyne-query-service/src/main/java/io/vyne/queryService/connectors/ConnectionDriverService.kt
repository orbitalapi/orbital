package io.vyne.queryService.connectors

import io.vyne.connectors.ConnectionDriverOptions
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.kafka.KafkaConnection
import io.vyne.connectors.registry.ConnectionRegistry
import io.vyne.connectors.registry.ConnectorConfigurationSummary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ConnectionDriverService(
   val connectorRegistries: List<ConnectionRegistry<*>>
) {

   @GetMapping("/api/connections/drivers")
   fun listAvailableDrivers(): List<ConnectionDriverOptions> {
      return JdbcDriver.driverOptions + KafkaConnection.driverOptions
   }

   @GetMapping("/api/connections")
   fun listConnections(): List<ConnectorConfigurationSummary> {
      return this.connectorRegistries
         .flatMap { it.listAll() }
         .map { ConnectorConfigurationSummary(it) }
   }

}
