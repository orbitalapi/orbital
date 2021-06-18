package io.vyne.cask.observers.kafka

import io.vyne.cask.observers.ObservedChange
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate



object KafkaTemplateFactory {
   fun kafkaTemplateForBootstrapServers(bootstrapServers: String): KafkaTemplate<String, ObservedChange> {
      val senderProps = mapOf(
         ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
         ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to org.apache.kafka.common.serialization.StringSerializer::class.java,
         /*
          Starting with version 2.1, type information can be conveyed in record Headers,
          default value of  JsonSerializer.ADD_TYPE_INFO_HEADERS is true so setting to false here.
          */
         org.springframework.kafka.support.serializer.JsonSerializer.ADD_TYPE_INFO_HEADERS to false,
         ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to org.springframework.kafka.support.serializer.JsonSerializer::class.java,
      )
      return KafkaTemplate(DefaultKafkaProducerFactory(senderProps))
   }
}
