package com.orbitalhq.connectors.kafka.registry

import com.orbitalhq.connectors.config.kafka.KafkaConnection
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import reactor.kafka.receiver.ReceiverOptions


fun KafkaConnectionConfiguration.toReceiverOptions(offset: String = "latest"): ReceiverOptions<Int, ByteArray> {
   val consumerProps = this.toConsumerProps(offset)
   return ReceiverOptions.create(consumerProps)
}

fun KafkaConnectionConfiguration.toAdminProps(): MutableMap<String, Any> {
   val adminProps: MutableMap<String, Any> = HashMap()
   adminProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = this.brokers
   adminProps[ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG] =3000
   adminProps[ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG] = 5000
   return adminProps
}

fun KafkaConnectionConfiguration.toConsumerProps(offset: String = "latest"): MutableMap<String, Any> {
   val brokers = this.brokers
   val groupId = this.groupId

   val consumerProps: MutableMap<String, Any> = HashMap()
   consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokers
   consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = groupId
   consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.qualifiedName!!
   consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.qualifiedName!!
   consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = offset

   return consumerProps
}

fun KafkaConnectionConfiguration.toProducerProps():MutableMap<String,Any> {
   val producerProps = mutableMapOf<String,Any>()
   producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = this.brokers
   producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.qualifiedName!!
   producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = ByteArraySerializer::class.qualifiedName!!
   return producerProps
}


// Using extension functions to avoid serialization issues with HOCON
val KafkaConnectionConfiguration.brokers: String
   get() {
      return this.connectionParameters[KafkaConnection.Parameters.BROKERS.templateParamName] as String
   }
val KafkaConnectionConfiguration.groupId: String
   get() {
      return this.connectionParameters[KafkaConnection.Parameters.GROUP_ID.templateParamName] as String
   }
