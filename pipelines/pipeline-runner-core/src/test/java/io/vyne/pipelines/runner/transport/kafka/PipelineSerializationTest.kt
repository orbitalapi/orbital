package io.vyne.pipelines.runner.transport.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.VersionedTypeReference
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineChannel
import io.vyne.query.VyneJacksonModule
import io.vyne.schemas.fqn
import io.vyne.utils.log
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.Test
import java.util.*

class PipelineSerializationTest() {

   fun producerProps(): Map<String, Any> {
      val props: MutableMap<String, Any> = HashMap<String, Any>()
      props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "127.0.0.1:9092"
      props[ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG] = 3000
      props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
      props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
      return props
   }

   protected fun consumerProps(groupId: String): Map<String, Any> {
      val props: MutableMap<String, Any> = HashMap<String, Any>()
      props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] =  "127.0.0.1:9092"
      props[ConsumerConfig.GROUP_ID_CONFIG] = groupId
      props[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = 3000
      props[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = 3000
      props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.canonicalName
      props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.canonicalName
      props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
      return props
   }

   @Test
   @Ignore
   fun canSerializePipeline() {
      val inputTopicName = "pipeline-input"
      val outputTopicName = "pipeline-output"
      val pipeline = Pipeline(
         input = PipelineChannel(
            VersionedTypeReference("PersonLoggedOnEvent".fqn()),
            KafkaTransportInputSpec(
               topic = inputTopicName,
               targetType = VersionedTypeReference("PersonLoggedOnEvent".fqn()),
               props = consumerProps("vyne-pipeline-group")
            )
         ),
         output = PipelineChannel(
            VersionedTypeReference("UserEvent".fqn()),
            KafkaTransportOutputSpec(
               topic = outputTopicName,
               targetType = VersionedTypeReference("UserEvent".fqn()),
               props = producerProps()
            )
         ),
         name = "test-pipeline"
      )

      val json = jacksonObjectMapper()
         .registerModule(VyneJacksonModule())
         .writerWithDefaultPrettyPrinter().writeValueAsString(pipeline)
      log().info("PipelineJson: \n$json")
   }
}
