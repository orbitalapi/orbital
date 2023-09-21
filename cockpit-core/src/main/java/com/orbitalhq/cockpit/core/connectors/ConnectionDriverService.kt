package com.orbitalhq.cockpit.core.connectors

import com.orbitalhq.connectors.ConnectionDriverOptions
import com.orbitalhq.connectors.config.aws.AwsConnection
import com.orbitalhq.connectors.azure.blob.registry.AzureStorageConnection
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.jdbc.JdbcDriverOptions
import com.orbitalhq.connectors.config.kafka.KafkaConnection
import com.orbitalhq.connectors.registry.ConnectionRegistry
import com.orbitalhq.connectors.registry.ConnectorConfigurationSummary
import com.orbitalhq.security.VynePrivileges
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux

@RestController
class ConnectionDriverService(
   private val new: SourceLoaderConnectorsRegistry,
   val connectorRegistries: List<ConnectionRegistry<*>>
) {

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections/drivers")
   fun listAvailableDrivers(): Flux<ConnectionDriverOptions> {
      return Flux.fromIterable(JdbcDriverOptions.driverOptions + KafkaConnection.driverOptions + AwsConnection.driverOptions + AzureStorageConnection.driverOptions)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections")
   fun listConnections(): Flux<ConnectorConfigurationSummary> {
      return this.new.load()
         .listAll()
         .map {
            ConnectorConfigurationSummary(
               it.connectionName,
               it.type,
               it.driverName,
               it.getUiDisplayProperties()
            )
         }
         .toFlux()
   }

}

object ConnectionTestedSuccessfully {
   const val message: String = "ok"
}
