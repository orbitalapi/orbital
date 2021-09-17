package io.vyne.pipelines.jet.source.kafka

import com.google.common.annotations.VisibleForTesting
import com.hazelcast.jet.kafka.KafkaSources
import com.hazelcast.jet.pipeline.StreamSource
import io.vyne.pipelines.jet.JetLogger
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportInputSpec
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.Properties

/**
 * Allows defining global properties that are applied to all
 * pipelines.
 *
 * These override the defaults in DEFAULT_PROPS
 */
@ConfigurationProperties("vyne.pipelines.kafka")
data class KafkaPipelineConfig(
   val props: Map<String, Any> = emptyMap()
) {
   companion object {
      val DEFAULT_PROPS = mapOf<String, Any>(
         ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
         ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "true",
         ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG to 5000,
         ConsumerConfig.GROUP_ID_CONFIG to "vyne-pipelines",
         ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.canonicalName,
         ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.canonicalName,
         ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
         ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
      )
   }

   fun mergeWithDefaults(): Map<String, Any> {
      // When adding maps, the latest values win
      return DEFAULT_PROPS + props
   }
}

internal object KafkaSource // for logging
class KafkaSourceBuilder(private val kafkaConfig: KafkaPipelineConfig = KafkaPipelineConfig()) :
   PipelineSourceBuilder<KafkaTransportInputSpec> {
   companion object {
      val logger = JetLogger.getLogger(KafkaSource::class)
   }

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean = pipelineSpec.input is KafkaTransportInputSpec

   override fun getEmittedType(pipelineSpec: PipelineSpec<KafkaTransportInputSpec, *>, schema: Schema): QualifiedName {
      return pipelineSpec.input.targetType.typeName
   }

   override fun build(pipelineSpec: PipelineSpec<KafkaTransportInputSpec, *>): StreamSource<MessageContentProvider> {

      val props = KafkaUtils.buildProps(pipelineSpec.input, kafkaConfig)
      return KafkaSources.kafka(
         props,
         { t: ConsumerRecord<String, String> ->
            val result = StringContentProvider(t.value())
            logger.info("Received message from topic ${pipelineSpec.input.topic}: $result")
            result
         },
         pipelineSpec.input.topic
      )
   }


}


object KafkaUtils {
   fun buildProps(transportSpec: PipelineTransportSpec, config: KafkaPipelineConfig): Properties {
      val props = Properties()
      mergeConfig(transportSpec, config)
         .forEach { (key, value) -> props.setProperty(key, value.toString()) }
      return props
   }

   @VisibleForTesting
   internal fun mergeConfig(kafkaTransportSpec: PipelineTransportSpec, config: KafkaPipelineConfig): Map<String, Any> {
      // When adding maps, the latest values win
      return config.mergeWithDefaults() + kafkaTransportSpec.props
   }
}
