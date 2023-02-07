package io.vyne.cockpit.core.connectors.kafka

import arrow.core.getOrHandle
import io.vyne.cockpit.core.connectors.ConnectionTestedSuccessfully
import io.vyne.cockpit.core.connectors.jdbc.BadConnectionException
import io.vyne.connectors.ConnectorUtils
import io.vyne.connectors.kafka.KafkaConnection
import io.vyne.connectors.kafka.KafkaConnectionConfiguration
import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.connectors.registry.ConnectorConfigurationSummary
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class KafkaConnectorService(
   private val connectionRegistry: KafkaConnectionRegistry,
) {


   @PostMapping("/api/connections/message-broker", params = ["test=true"])
   fun testConnection(@RequestBody connectionConfig: KafkaConnectionConfiguration): ConnectionTestedSuccessfully {
      ConnectorUtils.assertAllParametersPresent(
         KafkaConnection.parameters, connectionConfig.connectionParameters
      )
      return KafkaConnection.test(connectionConfig)
         .map { ConnectionTestedSuccessfully }
         .getOrHandle { connectionFailureMessage ->
            throw BadConnectionException(connectionFailureMessage)
         }
   }

   @PostMapping("/api/connections/message-broker")
   fun createConnection(@RequestBody connectionConfig: KafkaConnectionConfiguration): ConnectorConfigurationSummary {
      testConnection(connectionConfig);
      connectionRegistry.register(connectionConfig)
      return ConnectorConfigurationSummary(connectionConfig)
   }
}
