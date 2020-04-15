package io.vyne.pipelines.runner.transport.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.pipelines.*
import io.vyne.pipelines.runner.transport.PipelineInputTransportBuilder
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.utils.log
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.IntegerDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant

// TODO : This needs to get much richer
data class KafkaTransportInputSpec(
   val topic: String,
   override val targetType: VersionedTypeReference,
   val consumerProps: Map<String, Any>
) : PipelineTransportSpec {
   override val direction: PipelineDirection
      get() = PipelineDirection.INPUT
   override val type: PipelineTransportType
      get() = KafkaTransport.TYPE


   // TODO : This all needs to be configurable
   val props: Map<String, Any> = consumerProps + mapOf(
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.qualifiedName!!,
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.qualifiedName!!
   )


}

@Component
class KafkaInputBuilder(val objectMapper: ObjectMapper) : PipelineInputTransportBuilder<KafkaTransportInputSpec> {
   override fun canBuild(spec: PipelineTransportSpec): Boolean {
      return spec.type == KafkaTransport.TYPE
         && spec.direction == PipelineDirection.INPUT
   }

   override fun build(spec: KafkaTransportInputSpec): PipelineInputTransport {
      return KafkaInput(spec, objectMapper)
   }

}

class KafkaInput(spec: KafkaTransportInputSpec, objectMapper: ObjectMapper) : PipelineInputTransport {
   private val receiverOptions = ReceiverOptions.create<String, String>(spec.props)
      .commitBatchSize(0) // Don't commit in batches ..  can explore this later
      .commitInterval(Duration.ZERO) // Don't delay commits .. can explore this later
      .subscription(listOf(spec.topic))
      .addAssignListener { partitions -> log().debug("onPartitionsAssigned: $partitions") }
      .addRevokeListener { partitions -> log().debug("onPartitionsRevoked: $partitions") }

   override val feed: Flux<PipelineInputMessage>

   init {
      feed = KafkaReceiver.create(receiverOptions)
         .receive()
         // EEK - this warning is concerning - suggests this ooesn't work
         .flatMap { kafkaMessage ->
            val recordId = kafkaMessage.key()
            val offset = kafkaMessage.offset()
            val partition = kafkaMessage.partition()
            val topic = kafkaMessage.topic()
            val headers = kafkaMessage.headers().map { it.key() to it.value().toString(Charset.defaultCharset()) }.toMap()

            val metadata = mapOf(
               "recordId" to recordId,
               "offset" to offset,
               "partition" to partition,
               "topic" to topic,
               "headers" to headers
            )


            val messageProvider = { schema: Schema, logger: PipelineLogger ->
               val targetType = schema.type(spec.targetType)
               logger.debug { "Deserializing record $partition/$offset" }
               val messageJson = kafkaMessage.value()
               val map = objectMapper.readValue<Map<String, Any>>(messageJson)
               logger.debug { "Converting Map to TypeInstance of ${targetType.fullyQualifiedName}" }
               TypedInstance.from(targetType, map, schema)
            }
            Mono.create<PipelineInputMessage> { sink ->
               sink.success(PipelineInputMessage(
                  Instant.now(), // TODO : Surely this is in the headers somewhere?
                  metadata,
                  messageProvider
               ))
            }.doOnSuccess {
               kafkaMessage.receiverOffset().acknowledge()
            }
         }
   }


}
