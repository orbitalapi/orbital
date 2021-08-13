package io.vyne.support

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import java.io.File
import java.time.Instant
import java.util.Properties
import java.util.UUID

class KafkaPublisher(bootstrapServers: String, private val topic: String) {
   private val kafkaProducer: Producer<String, ByteArray>
   init {
      val props = Properties()
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
      props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaProducer-${Instant.now().toEpochMilli()}")
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.getName())
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer::class.java.getName())
      props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000)
      kafkaProducer = KafkaProducer(props)
   }

   fun publish(inputFile: File) {
      kafkaProducer.send(ProducerRecord(topic, UUID.randomUUID().toString(), inputFile.readBytes()))
   }
}
