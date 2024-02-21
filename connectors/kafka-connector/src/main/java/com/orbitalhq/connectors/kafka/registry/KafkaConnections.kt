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
import java.time.Duration

data class KafkaConnections(
   val kafka: MutableMap<String, KafkaConnectionConfiguration> = mutableMapOf()
) : ConnectionConfigMap {
   companion object {
      val CONFIG_PREFIX = KafkaConnections::kafka.name  // must match the name of the param in the constructor
   }
}

fun KafkaConnection.test(connection: KafkaConnectionConfiguration, timeout: Duration = Duration.ofSeconds(5)): Either<String, ConnectionSucceeded> {
   logger.debug { "testing kafka connection configuration => $connection" }
   return try {
      AdminClient.create(connection.toAdminProps()).use { adminClient ->
         val nodes = adminClient.describeCluster(DescribeClusterOptions().timeoutMs(timeout.toMillis().toInt())).nodes().get()
         if (nodes.isNullOrEmpty()) {
            logger.info { "Could not retrieve nodes from kafka client - test failed" }
            return "Unable to connect to Kafka cluster".left()
         }
         try {
            logger.debug { "Attempting to fetch topics for connection ${connection.connectionName}" }
            val topics = adminClient.listTopics().listings().get()
            logger.debug { "Successfully fetched ${topics.size} topics" }
            return ConnectionSucceeded.right()
         } catch (e: Exception) {
            TODO()
         }
      }

   } catch (e: Exception) {
      val message = listOfNotNull(e.message, e.cause?.message).joinToString(" : ")
      message.left()
   }
}
