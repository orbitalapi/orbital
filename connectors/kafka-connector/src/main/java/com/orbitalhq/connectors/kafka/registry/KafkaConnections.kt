package com.orbitalhq.connectors.kafka.registry

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.orbitalhq.connectors.ConnectionSucceeded
import com.orbitalhq.connectors.config.kafka.KafkaConnection
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.registry.ConnectionConfigMap
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.DescribeClusterOptions

data class KafkaConnections(
   val kafka: MutableMap<String, KafkaConnectionConfiguration> = mutableMapOf()
) : ConnectionConfigMap {
   companion object {
      val CONFIG_PREFIX = KafkaConnections::kafka.name  // must match the name of the param in the constructor
   }
}

fun KafkaConnection.test(connection: KafkaConnectionConfiguration): Either<String, ConnectionSucceeded> {
   logger.debug { "testing kafka connection configuration => $connection" }
   return try {
      AdminClient.create(connection.toAdminProps()).use { adminClient ->
         val nodes = adminClient.describeCluster(DescribeClusterOptions().timeoutMs(100)).nodes().get()
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
