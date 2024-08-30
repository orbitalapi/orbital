package com.orbitalhq.cockpit.core.connectors.kafka

import arrow.core.getOrElse
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.connectors.ConnectorUtils
import com.orbitalhq.connectors.config.kafka.KafkaConnection
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.test
import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.connectors.registry.ConnectorConfigurationSummary
import com.orbitalhq.connectors.registry.MutableConnectionRegistry
import com.orbitalhq.security.VynePrivileges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class KafkaConnectorService(
   private val connectionRegistry: KafkaConnectionRegistry,
) {


   @PreAuthorize("hasAuthority('${VynePrivileges.TestConnections}')")
   @PostMapping("/api/packages/{packageUri}/connections/message-broker", params = ["test=true"])
   suspend fun testConnection(@RequestBody connectionConfig: KafkaConnectionConfiguration): ConnectionStatus {
      ConnectorUtils.assertAllParametersPresent(
         KafkaConnection.parameters, connectionConfig.connectionParameters
      )
      return KafkaConnection.test(connectionConfig)
         .map { ConnectionStatus.healthy() }
         .getOrElse { ConnectionStatus.error(it) }
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.EditConnections}')")
   @PostMapping("/api/packages/{packageUri}/connections/message-broker")
   suspend fun createConnection(@RequestBody connectionConfig: KafkaConnectionConfiguration,
                        @PathVariable("packageUri") packageUri: String): ConnectorConfigurationSummary {
      testConnection(connectionConfig)
      val packageIdentifier = PackageIdentifier.fromUriSafeId(packageUri)
      val connectionEditor = connectionRegistry as MutableConnectionRegistry<KafkaConnectionConfiguration>
      // Move the blockCall to the I/O dispatcher ...
      // Otherwsie we get:
      // block()/blockFirst()/blockLast() are blocking, which is not supported in thread reactor-http-epoll-11
      val result = withContext(Dispatchers.IO) {
         connectionRegistry.register(packageIdentifier,connectionConfig)
      }
      return ConnectorConfigurationSummary(packageIdentifier,connectionConfig, messages = result.messages)
   }
}
