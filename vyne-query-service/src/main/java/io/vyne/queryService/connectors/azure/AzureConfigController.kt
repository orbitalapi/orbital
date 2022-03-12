package io.vyne.queryService.connectors.azure

import io.vyne.connectors.ConnectorUtils
import io.vyne.connectors.aws.core.AwsConnection
import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.azure.blob.registry.AzureStorageConnection
import io.vyne.connectors.azure.blob.registry.AzureStorageConnectorConfiguration
import io.vyne.connectors.azure.blob.registry.AzureStoreConnectionFileRegistry
import io.vyne.connectors.registry.ConnectorConfigurationSummary
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class AzureConfigController(private val registry: AzureStoreConnectionFileRegistry) {
   @PostMapping("/api/connections/azure_storage", params = ["test=true"])
   fun testConnection(@RequestBody connectionConfig: AzureStorageConnectorConfiguration): Mono<Unit> {
      ConnectorUtils.assertAllParametersPresent(
         AzureStorageConnection.parameters, connectionConfig.connectionParameters
      )
      return Mono.empty()
   }

   @PostMapping("/api/connections/azure_storage")
   fun createConnection(@RequestBody connectionConfig: AzureStorageConnectorConfiguration): Mono<ConnectorConfigurationSummary> {
      testConnection(connectionConfig)
      registry.register(connectionConfig)
      val summary =  ConnectorConfigurationSummary(connectionConfig)
      return Mono.just(summary)
   }
}
//AzureStorageConnectorConfiguration
