package io.vyne.pipelines.runner.transport.kafka

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.*
import io.vyne.pipelines.runner.transport.PipelineOutputTransportBuilder
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import java.io.InputStream

@Component
class KafkaOutputBuilder : PipelineOutputTransportBuilder<KafkaTransportOutputSpec> {
   override fun canBuild(spec: PipelineTransportSpec) = spec.type == KafkaTransport.TYPE && spec.direction == PipelineDirection.OUTPUT

   override fun build(spec: KafkaTransportOutputSpec, logger: PipelineLogger) = KafkaOutput(spec)
}

class KafkaOutput(private val spec: KafkaTransportOutputSpec) : PipelineOutputTransport {
   override val type: VersionedTypeReference = spec.targetType

   override val healthMonitor = EmitterPipelineTransportHealthMonitor()

   private val defaultProps = mapOf(
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.qualifiedName!!,
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.qualifiedName!!
   )
   private val senderOptions = SenderOptions.create<String, InputStream>(spec.props + defaultProps)
   private val sender = KafkaSender.create(senderOptions)
   override fun write(message: InputStream, logger: PipelineLogger) {

      var producer = Mono.create<ProducerRecord<String, InputStream>> { sink ->
         logger.info { "Sending message to Kafka topic ${spec.topic}" }
         sink.success(ProducerRecord<String, InputStream>(spec.topic, message))
      }


      sender.createOutbound().send(producer)
         .then()
         // TODO  :This error is lacking enough context to be useful.  Need a ref to the pipeline instance / message
         .doOnError { error -> logger.error(error) { "Pipeline failed to send to kafka topic ${spec.topic}" } }
         .doOnSuccess { logger.info { "Pipeline published message to topic ${spec.topic}" } }
         .subscribe()
   }
}
