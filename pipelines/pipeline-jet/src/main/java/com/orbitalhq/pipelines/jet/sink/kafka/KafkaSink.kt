package com.orbitalhq.pipelines.jet.sink.kafka

import com.hazelcast.jet.kafka.KafkaSinks
import com.hazelcast.jet.pipeline.Sink
import com.orbitalhq.connectors.config.kafka.asKafkaProperties
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.toProducerProps
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.kafka.KafkaTransportOutputSpec
import com.orbitalhq.pipelines.jet.connectionOrError
import com.orbitalhq.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
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
