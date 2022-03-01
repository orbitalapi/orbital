package io.vyne.queryService.connectors

import io.vyne.connectors.ConnectionDriverOptions
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.kafka.KafkaConnection
import io.vyne.connectors.registry.ConnectionRegistry
import io.vyne.connectors.registry.ConnectorConfigurationSummary
import io.vyne.security.VynePrivileges
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux

@RestController
class ConnectionDriverService(
   val connectorRegistries: List<ConnectionRegistry<*>>
) {

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections/drivers")
   fun listAvailableDrivers(): Flux<ConnectionDriverOptions> {
      return Flux.fromIterable(JdbcDriver.driverOptions + KafkaConnection.driverOptions)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections")
   fun listConnections(): Flux<ConnectorConfigurationSummary> {
      return this.connectorRegistries
         .flatMap { it.listAll() }
         .map { ConnectorConfigurationSummary(it) }
         .toFlux()
   }

}

object ConnectionTestedSuccessfully {
   const val message: String = "ok"
}
