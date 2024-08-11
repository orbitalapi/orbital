package com.orbitalhq.cockpit.core.connectors.azure

import com.orbitalhq.connectors.ConnectorUtils
import com.orbitalhq.connectors.azure.blob.StoreCredentialsTester
import com.orbitalhq.connectors.azure.blob.registry.AzureStorageConnection
import com.orbitalhq.connectors.azure.blob.registry.AzureStorageConnectorConfiguration
import com.orbitalhq.connectors.azure.blob.registry.AzureStoreConnectionFileRegistry
import com.orbitalhq.connectors.registry.ConnectorConfigurationSummary
import com.orbitalhq.security.VynePrivileges
import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger { }

@RestController
class AzureConfigController(private val registry: AzureStoreConnectionFileRegistry) {
   @PreAuthorize("hasAuthority('${VynePrivileges.TestConnections}')")
   @PostMapping("/api/connections/azure_storage", params = ["test=true"])
   fun testConnection(@RequestBody connectionConfig: AzureStorageConnectorConfiguration): Mono<Unit> {
      ConnectorUtils.assertAllParametersPresent(
         AzureStorageConnection.parameters, connectionConfig.connectionParameters
      )

      return StoreCredentialsTester.testConnection(connectionConfig).map {
         logger.info { "successfully test connection for ${connectionConfig.connectionName}" }
         Mono.empty<Unit>()
      }
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.EditConnections}')")
   @PostMapping("/api/connections/azure_storage")
   fun createConnection(@RequestBody connectionConfig: AzureStorageConnectorConfiguration): Mono<ConnectorConfigurationSummary> {
      TODO("Needs migrating - don't think this is used")
//      testConnection(connectionConfig)
//      registry.register(connectionConfig)
//      val summary = ConnectorConfigurationSummary(connectionConfig)
//      return Mono.just(summary)
   }
}
//AzureStorageConnectorConfiguration
