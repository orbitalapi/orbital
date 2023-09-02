package com.orbitalhq.cockpit.core.connectors.kafka

import arrow.core.getOrHandle
import com.orbitalhq.cockpit.core.connectors.ConnectionTestedSuccessfully
import com.orbitalhq.cockpit.core.connectors.jdbc.BadConnectionException
import com.orbitalhq.connectors.ConnectorUtils
import com.orbitalhq.connectors.config.kafka.KafkaConnection
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.test
import com.orbitalhq.connectors.registry.ConnectorConfigurationSummary
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
       TODO("Not currently supported - need to migrate to package based writing")
//      connectionRegistry.register(connectionConfig)
//      return ConnectorConfigurationSummary(connectionConfig)
   }
}
