package com.orbitalhq.cockpit.core.connectors.aws

import arrow.core.getOrElse
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.connectors.ConnectorUtils
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.connectors.aws.s3.S3AsyncConnection
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.connectors.config.kafka.KafkaConnection
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.test
import com.orbitalhq.connectors.registry.ConnectorConfigurationSummary
import com.orbitalhq.connectors.registry.MutableConnectionRegistry
import com.orbitalhq.security.VynePrivileges
import io.vyne.connectors.aws.core.registry.test
import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration

private val logger = KotlinLogging.logger { }

@RestController
class AwsController(val registry: AwsConnectionRegistry) {

   @PreAuthorize("hasAuthority('${VynePrivileges.TestConnections}')")
   @PostMapping("/api/packages/{packageUri}/connections/aws", params = ["test=true"])
   fun testConnection(@RequestBody connectionConfig: AwsConnectionConfiguration): Mono<ConnectionStatus> {
      return Mono.defer {
         val result = connectionConfig.test()
            .map { ConnectionStatus.healthy() }
            .getOrElse { ConnectionStatus.error(it) }
         Mono.just(result)
      }.subscribeOn(Schedulers.boundedElastic())
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.EditConnections}')")
   @PostMapping("/api/packages/{packageUri}/connections/aws")
   fun createConnection(
      @RequestBody connectionConfig: AwsConnectionConfiguration,
      @PathVariable("packageUri") packageUri: String
   ): Mono<ConnectorConfigurationSummary> {
      return testConnection(connectionConfig)
         .map {
            val packageIdentifier = PackageIdentifier.fromUriSafeId(packageUri)
            val connectionEditor = registry as MutableConnectionRegistry<AwsConnectionConfiguration>
            val result = connectionEditor.register(packageIdentifier, connectionConfig)
            ConnectorConfigurationSummary(packageIdentifier, connectionConfig, messages = result.messages)
         }
   }
}
