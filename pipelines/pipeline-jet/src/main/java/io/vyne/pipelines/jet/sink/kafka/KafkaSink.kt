package io.vyne.pipelines.jet.sink.kafka

import com.hazelcast.jet.kafka.KafkaSinks
import com.hazelcast.jet.pipeline.Sink
import io.vyne.connectors.kafka.asKafkaProperties
import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.pipelines.jet.BadRequestException
import io.vyne.pipelines.jet.JetLogger
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportOutputSpec
import io.vyne.pipelines.jet.connectionOrError
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import mu.KotlinLogging
import org.springframework.stereotype.Component

object KafkaSink // for Logging
@Component
class KafkaSinkBuilder(private val connectionRegistry: KafkaConnectionRegistry) :
   SingleMessagePipelineSinkBuilder<KafkaTransportOutputSpec> {
   companion object {
      val logger = KotlinLogging.logger { }
   }

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean = pipelineSpec.output is KafkaTransportOutputSpec

   override fun getRequiredType(
      pipelineSpec: PipelineSpec<*, KafkaTransportOutputSpec>,
      schema: Schema
   ): QualifiedName {
      return pipelineSpec.output.targetType.typeName
   }

   override fun build(pipelineSpec: PipelineSpec<*, KafkaTransportOutputSpec>): Sink<MessageContentProvider> {
      val connection = connectionRegistry.connectionOrError(pipelineSpec.id, pipelineSpec.output.connectionName)

      return KafkaSinks
         // Use ByteArray for encoding so that we can support String as well as Binary formats
         // like proto / avro, etc
         .kafka<MessageContentProvider, String, ByteArray>(
            connection.toProducerProps().asKafkaProperties(),
            pipelineSpec.output.topic,
            { message: MessageContentProvider -> null }, // What should we do for the key?
            { message: MessageContentProvider ->
               val stringContent = message.asString(JetLogger.getVynePipelineLogger(KafkaSink::class))
               logger.info("Writing content to kafka topic ${pipelineSpec.output.topic}: $stringContent")
               stringContent.encodeToByteArray()
            }
         )
   }

}
