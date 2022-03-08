package io.vyne.queryService.connectors.aws

import io.vyne.connectors.ConnectorUtils
import io.vyne.connectors.aws.core.AwsConnection
import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.aws.core.registry.AwsConfigFileConnectionRegistry
import io.vyne.connectors.aws.s3.AwsS3Connection
import io.vyne.connectors.registry.ConnectorConfigurationSummary
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class AwsController(val registry: AwsConfigFileConnectionRegistry) {

   @PostMapping("/api/connections/aws", params = ["test=true"])
   fun testConnection(@RequestBody connectionConfig: AwsConnectionConfiguration): Mono<Unit> {
      ConnectorUtils.assertAllParametersPresent(
         AwsConnection.parameters, connectionConfig.connectionParameters
      )
      return Mono.empty()
   }

   @PostMapping("/api/connections/aws")
   fun createConnection(@RequestBody connectionConfig: AwsConnectionConfiguration): Mono<ConnectorConfigurationSummary> {
      testConnection(connectionConfig)
      registry.register(connectionConfig)
      val summary =  ConnectorConfigurationSummary(connectionConfig)
      return Mono.just(summary)
   }
}
