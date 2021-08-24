package io.vyne.pipelines.jet.sink.kafka

import com.hazelcast.jet.kafka.KafkaSinks
import com.hazelcast.jet.pipeline.Sink
import io.vyne.pipelines.MessageContentProvider
import io.vyne.pipelines.PipelineSpec
import io.vyne.pipelines.jet.JetLogger
import io.vyne.pipelines.jet.sink.PipelineSinkBuilder
import io.vyne.pipelines.jet.source.kafka.KafkaPipelineConfig
import io.vyne.pipelines.jet.source.kafka.KafkaUtils
import io.vyne.pipelines.runner.transport.kafka.KafkaTransportOutputSpec
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import mu.KotlinLogging

object KafkaSink // for Logging
class KafkaSinkBuilder(private val kafkaConfig: KafkaPipelineConfig = KafkaPipelineConfig()) :
   PipelineSinkBuilder<KafkaTransportOutputSpec> {
   companion object {
      val logger = KotlinLogging.logger {  }
   }
   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean = pipelineSpec.output is KafkaTransportOutputSpec

   override fun getRequiredType(
      pipelineSpec: PipelineSpec<*, KafkaTransportOutputSpec>,
      schema: Schema
   ): QualifiedName {
      return pipelineSpec.output.targetType.typeName
   }

   override fun build(pipelineSpec: PipelineSpec<*, KafkaTransportOutputSpec>): Sink<MessageContentProvider> {
      val props = KafkaUtils.buildProps(pipelineSpec.output, kafkaConfig)
      return KafkaSinks
         .kafka<MessageContentProvider, String, String>(
         props,
         pipelineSpec.output.topic,
         { message: MessageContentProvider -> null }, // What should we do for the key?
         { message: MessageContentProvider ->
            val stringContent = message.asString(JetLogger.getVynePipelineLogger(KafkaSink::class))
            logger.info("Writing content to kafka topic ${pipelineSpec.output.topic}: $stringContent")
            stringContent
         }
      )
   }

}
