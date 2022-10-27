package io.vyne.pipelines.jet.sink.kafka

import com.hazelcast.jet.kafka.KafkaSinks
import com.hazelcast.jet.pipeline.Sink
import io.vyne.connectors.kafka.asKafkaProperties
import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportOutputSpec
import io.vyne.pipelines.jet.connectionOrError
import io.vyne.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
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

   override fun canSupport(pipelineTransportSpec: PipelineTransportSpec): Boolean =
      pipelineTransportSpec is KafkaTransportOutputSpec

   override fun getRequiredType(
      pipelineTransportSpec: KafkaTransportOutputSpec,
      schema: Schema
   ): QualifiedName {
      return pipelineTransportSpec.targetType.typeName
   }

   override fun build(
      pipelineId: String,
      pipelineName: String,
      pipelineTransportSpec: KafkaTransportOutputSpec
   ): Sink<MessageContentProvider> {
      val connection = connectionRegistry.connectionOrError(pipelineId, pipelineTransportSpec.connectionName)

      return KafkaSinks
         // Use ByteArray for encoding so that we can support String as well as Binary formats
         // like proto / avro, etc
         .kafka<MessageContentProvider, String, ByteArray>(
            connection.toProducerProps().asKafkaProperties(),
            pipelineTransportSpec.topic,
            { message: MessageContentProvider -> null }, // What should we do for the key?
            { message: MessageContentProvider ->
               val stringContent = message.asString()
               logger.info("Writing content to kafka topic ${pipelineTransportSpec.topic}: $stringContent")
               stringContent.encodeToByteArray()
            }
         )
   }
}
