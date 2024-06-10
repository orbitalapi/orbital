package com.orbitalhq.connectors.kafka.registry

import com.orbitalhq.connectors.config.kafka.KafkaConnection
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.KafkaConsumerRequest
import com.orbitalhq.connectors.valueOrThrowNiceMessage
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.SenderOptions
import java.util.UUID

private val logger = KotlinLogging.logger {  }

fun KafkaConnectionConfiguration.toReceiverOptions(offset: String = "latest", request: KafkaConsumerRequest): ReceiverOptions<Int, ByteArray> {
   val consumerProps = this.toConsumerProps(offset, request)
   return ReceiverOptions.create(consumerProps)
}

fun KafkaConnectionConfiguration.toSenderOptions(): SenderOptions<Any, Any> {
   val producerProps = this.toProducerProps()
   return SenderOptions.create<Any, Any>(producerProps)
}

fun KafkaConnectionConfiguration.toAdminProps(): MutableMap<String, Any> {
   val adminProps: MutableMap<String, Any> = this.connectionParameters.toMutableMap()
   adminProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = this.brokers
   adminProps[ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG] = 3000
   adminProps[ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG] = 5000
   return adminProps
}

fun KafkaConnectionConfiguration.toConsumerProps(offset: String = "latest", request: KafkaConsumerRequest? = null): MutableMap<String, Any> {
   val brokers = this.brokers
   val groupId = request?.streamSourceId ?: this.groupId

   val consumerProps: MutableMap<String, Any> = this.connectionParameters.toMutableMap()
   consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokers
   consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = groupId
   consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.qualifiedName!!
   consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.qualifiedName!!
   consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = offset

   return consumerProps
}

fun KafkaConnectionConfiguration.toProducerProps(): MutableMap<String, Any> {
   val producerProps: MutableMap<String, Any> = this.connectionParameters.toMutableMap()
   producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = this.brokers
   producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.qualifiedName!!
   producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = ByteArraySerializer::class.qualifiedName!!
   return producerProps
}


// Using extension functions to avoid serialization issues with HOCON
val KafkaConnectionConfiguration.brokers: String
   get() {
      return this.connectionParameters.valueOrThrowNiceMessage(KafkaConnection.Parameters.BROKERS.templateParamName)
   }
val KafkaConnectionConfiguration.groupId: String
   get() {
      val providedGroupId = this.connectionParameters[KafkaConnection.Parameters.GROUP_ID.templateParamName]
      return if (providedGroupId == null) {
         val randomGroupId = UUID.randomUUID().toString()
         logger.warn { "No Group Id found in kafka configuration ${this.connectionName}, using a random group Id $randomGroupId instead!" }
         randomGroupId
      } else {
          providedGroupId
      }
   }
