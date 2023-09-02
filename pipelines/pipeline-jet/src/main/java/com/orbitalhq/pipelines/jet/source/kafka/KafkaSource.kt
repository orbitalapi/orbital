package com.orbitalhq.pipelines.jet.source.kafka

import com.hazelcast.jet.kafka.KafkaSources
import com.hazelcast.jet.pipeline.StreamSource
import com.orbitalhq.connectors.kafka.MessageEncodingType
import com.orbitalhq.connectors.config.kafka.asKafkaProperties
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.toConsumerProps
import com.orbitalhq.pipelines.jet.BadRequestException
import com.orbitalhq.pipelines.jet.JetLogger
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.StringContentProvider
import com.orbitalhq.pipelines.jet.api.transport.kafka.KafkaTransportInputSpec
import com.orbitalhq.pipelines.jet.source.PipelineSourceBuilder
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Component

internal object KafkaSource // for logging

@Component
class KafkaSourceBuilder(private val connectionManager: KafkaConnectionRegistry) :
   PipelineSourceBuilder<KafkaTransportInputSpec> {
   companion object {
      val logger = JetLogger.getLogger(KafkaSource::class)
   }

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean = pipelineSpec.input is KafkaTransportInputSpec

   override fun getEmittedType(pipelineSpec: PipelineSpec<KafkaTransportInputSpec, *>, schema: Schema): QualifiedName {
      return pipelineSpec.input.targetType.typeName
   }

   override fun build(
      pipelineSpec: PipelineSpec<KafkaTransportInputSpec, *>,
      inputType: Type?
   ): StreamSource<MessageContentProvider> {
      val messageEncoding = MessageEncodingType.forType(inputType!!)
      if (!connectionManager.hasConnection(pipelineSpec.input.connectionName)) {
         throw BadRequestException("Pipeline ${pipelineSpec.id} defines an input from non-existant kafka connection ${pipelineSpec.input.connectionName}")
      }
      val connection = connectionManager.getConnection(pipelineSpec.input.connectionName)
      val props = connection.toConsumerProps().asKafkaProperties()
      return KafkaSources.kafka(
         props,
         { t: ConsumerRecord<String, Any> ->
            val result = when (messageEncoding) {
               MessageEncodingType.STRING -> {
                  // We're currently always using a ByteArrayDeserializer, so
                  // read as bytes then convert to string
                  val bytes = t.value() as ByteArray
                  StringContentProvider(String(bytes))
               }
               MessageEncodingType.BYTE_ARRAY -> TODO("We need to add support for Protobuf etc here")
            }
            logger.info("Received message from topic ${pipelineSpec.input.topic}: $result")
            result
         },
         pipelineSpec.input.topic
      )
   }
}
