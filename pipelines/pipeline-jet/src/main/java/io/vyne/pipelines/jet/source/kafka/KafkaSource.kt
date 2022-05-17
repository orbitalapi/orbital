package io.vyne.pipelines.jet.source.kafka

import com.hazelcast.jet.kafka.KafkaSources
import com.hazelcast.jet.pipeline.StreamSource
import io.vyne.connectors.kafka.MessageEncodingType
import io.vyne.connectors.kafka.asKafkaProperties
import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.pipelines.jet.BadRequestException
import io.vyne.pipelines.jet.JetLogger
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportInputSpec
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
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
