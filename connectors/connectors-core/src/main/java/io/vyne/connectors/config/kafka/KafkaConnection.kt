package io.vyne.connectors.config.kafka

import io.vyne.connectors.*
import io.vyne.connectors.registry.ConnectorType
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

object KafkaConnection {
   val logger = KotlinLogging.logger {}

   enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
      BROKERS(ConnectionDriverParam("Broker address", SimpleDataType.STRING, templateParamName = "brokerAddress")),
      GROUP_ID(
         ConnectionDriverParam(
            "Group Id",
            SimpleDataType.STRING,
            defaultValue = "vyne",
            templateParamName = "groupId",
         )
      ),
   }

   const val DRIVER_NAME = "KAFKA"

   val parameters: List<ConnectionDriverParam> = Parameters.values().connectionParams()
   val driverOptions = ConnectionDriverOptions(
      "KAFKA", "Kafka", ConnectorType.MESSAGE_BROKER, parameters
   )
}

fun Map<String, Any>.asKafkaProperties(): Properties {
   val props = Properties()
   this.forEach { (key, value) ->
      props.setProperty(key, value.toString())
   }
   return props
}

