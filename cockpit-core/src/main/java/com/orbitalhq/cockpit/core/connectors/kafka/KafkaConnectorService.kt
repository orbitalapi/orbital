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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class KafkaConnectorService(
   private val connectionRegistry: KafkaConnectionRegistry,
) {


   @PostMapping("/api/packages/{packageUri}/connections/message-broker", params = ["test=true"])
   fun testConnection(@RequestBody connectionConfig: KafkaConnectionConfiguration): ConnectionStatus {
      ConnectorUtils.assertAllParametersPresent(
         KafkaConnection.parameters, connectionConfig.connectionParameters
      )
      return KafkaConnection.test(connectionConfig)
         .map { ConnectionStatus.healthy() }
         .getOrElse { ConnectionStatus.error(it) }
   }

   @PostMapping("/api/packages/{packageUri}/connections/message-broker")
   fun createConnection(@RequestBody connectionConfig: KafkaConnectionConfiguration,
                        @PathVariable("packageUri") packageUri: String): ConnectorConfigurationSummary {
      testConnection(connectionConfig)
      val packageIdentifier = PackageIdentifier.fromUriSafeId(packageUri)
      val connectionEditor = connectionRegistry as MutableConnectionRegistry<KafkaConnectionConfiguration>
      val result = connectionRegistry.register(packageIdentifier,connectionConfig)
      return ConnectorConfigurationSummary(packageIdentifier,connectionConfig, messages = result.messages)
   }
}
