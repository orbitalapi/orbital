package com.orbitalhq.cockpit.core.connectors.aws

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.connectors.aws.s3.S3AsyncConnection
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.registry.ConnectorConfigurationSummary
import com.orbitalhq.connectors.registry.MutableConnectionRegistry
import mu.KotlinLogging
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger { }

@RestController
class AwsController(val registry: AwsConnectionRegistry) {

   @PostMapping("/api/packages/{packageUri}/connections/aws", params = ["test=true"])
   fun testConnection(@RequestBody connectionConfig: AwsConnectionConfiguration): Mono<Unit> {
      return S3AsyncConnection.test(connectionConfig)
         .onErrorMap { IllegalArgumentException("Invalid Aws connection settings") }
         .map {
            logger.info { "Verified aws connection ${connectionConfig.connectionName} by listing buckets" }
            Mono.empty<Unit>()
         }
   }

   @PostMapping("/api/packages/{packageUri}/connections/aws")
   fun createConnection(
      @RequestBody connectionConfig: AwsConnectionConfiguration,
      @PathVariable("packageUri") packageUri: String
   ): Mono<ConnectorConfigurationSummary> {
      return testConnection(connectionConfig)
         .map {
            val packageIdentifier = PackageIdentifier.fromUriSafeId(packageUri)
            val connectionEditor = registry as MutableConnectionRegistry<AwsConnectionConfiguration>
            connectionEditor.register(packageIdentifier, connectionConfig)
            ConnectorConfigurationSummary(packageIdentifier, connectionConfig)
         }
   }
}
