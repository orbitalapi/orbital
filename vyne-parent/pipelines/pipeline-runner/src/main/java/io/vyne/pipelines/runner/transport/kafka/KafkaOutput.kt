package io.vyne.pipelines.runner.transport.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.pipelines.*
import io.vyne.pipelines.runner.transport.PipelineTransportSpecId
import io.vyne.pipelines.runner.transport.PipelineOutputTransportBuilder
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.stereotype.Component
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord

@Component
class KafkaOutputBuilder(val objectMapper: ObjectMapper) : PipelineOutputTransportBuilder<KafkaTransportOutputSpec> {
   override fun canBuild(spec: PipelineTransportSpec): Boolean {
      return spec.type == KafkaTransport.TYPE
         && spec.direction == PipelineDirection.OUTPUT
   }

   override fun build(spec: KafkaTransportOutputSpec): PipelineOutputTransport {
      return KafkaOutput(spec, objectMapper)
   }

}

class KafkaOutput(private val spec: KafkaTransportOutputSpec, private val objectMapper: ObjectMapper) : PipelineOutputTransport {
   override val type: VersionedTypeReference = spec.targetType
   private val defaultProps = mapOf(
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.qualifiedName!!,
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.qualifiedName!!
   )
   private val senderOptions = SenderOptions.create<String, String>(spec.props + defaultProps)
   private val sender = KafkaSender.create(senderOptions)
   override fun write(typedInstance: TypedInstance, logger: PipelineLogger) {
      sender.send<String> { sink ->
         val json = objectMapper.writeValueAsString(typedInstance.toRawObject())
         logger.debug { "Generated json: $json" }
         logger.info { "Sending instance ${typedInstance.type.fullyQualifiedName} to Kafka topic ${spec.topic}" }
         sink.onNext(SenderRecord.create(
            ProducerRecord(
               spec.topic,
               json
            ),
            ""
         ))
      }
   }

}
