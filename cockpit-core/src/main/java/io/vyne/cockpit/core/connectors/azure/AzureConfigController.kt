package io.vyne.cockpit.core.connectors.azure

import io.vyne.connectors.ConnectorUtils
import io.vyne.connectors.azure.blob.StoreCredentialsTester
import io.vyne.connectors.azure.blob.registry.AzureStorageConnection
import io.vyne.connectors.azure.blob.registry.AzureStorageConnectorConfiguration
import io.vyne.connectors.azure.blob.registry.AzureStoreConnectionFileRegistry
import io.vyne.connectors.registry.ConnectorConfigurationSummary
import mu.KotlinLogging
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger { }

@RestController
class AzureConfigController(private val registry: AzureStoreConnectionFileRegistry) {
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

   @PostMapping("/api/connections/azure_storage")
   fun createConnection(@RequestBody connectionConfig: AzureStorageConnectorConfiguration): Mono<ConnectorConfigurationSummary> {
      testConnection(connectionConfig)
      registry.register(connectionConfig)
      val summary = ConnectorConfigurationSummary(connectionConfig)
      return Mono.just(summary)
   }
}
//AzureStorageConnectorConfiguration
