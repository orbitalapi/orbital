package io.vyne.cask.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@Configuration
@EnableKafka
@ConditionalOnProperty("cask.kafka.enabled", matchIfMissing = false)
class KafkaConfig {

   @Value("spring.kafka.consumer.bootstrap-servers")
   lateinit var bootstrapAddress: String

   @Value("spring.kafka.consumer.group-id")
   lateinit var groupId: String

   @Bean
   fun consumerFactory(): ConsumerFactory<String, ByteArray> {
      val props = mapOf<String, Any>(
         ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapAddress,
         ConsumerConfig.GROUP_ID_CONFIG to groupId,
         ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class,
         ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class
      )
      return DefaultKafkaConsumerFactory(props)
   }

   @Bean
   fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, ByteArray> {
      val factory = ConcurrentKafkaListenerContainerFactory<String, ByteArray>()
      factory.consumerFactory = consumerFactory()
      return factory
   }

}
