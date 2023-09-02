package com.orbitalhq.cockpit.core.connectors.aws

import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.connectors.aws.s3.S3AsyncConnection
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.connectors.registry.ConnectorConfigurationSummary
import mu.KotlinLogging
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger { }

@RestController
class AwsController(val registry: AwsConnectionRegistry) {

   @PostMapping("/api/connections/aws", params = ["test=true"])
   fun testConnection(@RequestBody connectionConfig: AwsConnectionConfiguration): Mono<Unit> {
//      ConnectorUtils.assertAllParametersPresent(
//         AwsConnection.parameters, connectionConfig.connectionParameters
//      )

      return S3AsyncConnection.test(connectionConfig)
         .onErrorMap { IllegalArgumentException("Invalid Aws connection settings") }
         .map {
            logger.info { "Verified aws connection ${connectionConfig.connectionName} by listing buckets" }
            Mono.empty<Unit>()
         }
   }

   @PostMapping("/api/connections/aws")
   fun createConnection(@RequestBody connectionConfig: AwsConnectionConfiguration): Mono<ConnectorConfigurationSummary> {
      testConnection(connectionConfig)
       TODO("Not currently supported - need to migrate to package based writing")
//      registry.register(connectionConfig)
//      val summary = ConnectorConfigurationSummary(connectionConfig)
//      return Mono.just(summary)
   }
}
