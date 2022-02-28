package io.vyne.queryService.connectors.kafka

import arrow.core.getOrHandle
import io.vyne.connectors.ConnectorUtils
import io.vyne.connectors.kafka.KafkaConnection
import io.vyne.connectors.kafka.KafkaConnectionConfiguration
import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.connectors.registry.ConnectorConfigurationSummary
import io.vyne.queryService.connectors.ConnectionTestedSuccessfully
import io.vyne.queryService.connectors.jdbc.BadConnectionException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

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
