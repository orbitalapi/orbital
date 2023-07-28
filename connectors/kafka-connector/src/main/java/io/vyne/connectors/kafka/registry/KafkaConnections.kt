package io.vyne.connectors.kafka.registry

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.vyne.connectors.ConnectionSucceeded
import io.vyne.connectors.config.kafka.KafkaConnection
import io.vyne.connectors.config.kafka.KafkaConnectionConfiguration
import io.vyne.connectors.registry.ConnectionConfigMap
import org.apache.kafka.clients.admin.AdminClient

data class KafkaConnections(
   val kafka: MutableMap<String, KafkaConnectionConfiguration> = mutableMapOf()
) : ConnectionConfigMap {
   companion object {
      val CONFIG_PREFIX = KafkaConnections::kafka.name  // must match the name of the param in the constructor
   }
}

fun KafkaConnection.test(connection: KafkaConnectionConfiguration): Either<String, ConnectionSucceeded> {
   KafkaConnection.logger.info { "testing kafka connection configuration => $connection" }
   return try {
      AdminClient.create(connection.toAdminProps()).use { adminClient ->
         val nodes = adminClient.describeCluster().nodes().get()
         return if (!nodes.isNullOrEmpty()) {
            ConnectionSucceeded.right()
         } else {
            "Invalid Kafka Cluster".left()
         }
      }

   } catch (e: Exception) {
      val message = listOfNotNull(e.message, e.cause?.message).joinToString(" : ")
      message.left()
   }
}
