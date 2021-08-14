package io.vyne.pipelines.runner.transport.kafka

import io.vyne.pipelines.EmitterPipelineTransportHealthMonitor
import io.vyne.pipelines.MessageContentProvider
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineLogger
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.runner.transport.PipelineOutputTransportBuilder
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions

@Component
class KafkaOutputBuilder : PipelineOutputTransportBuilder<KafkaTransportOutputSpec> {
   override fun canBuild(spec: PipelineTransportSpec) =
      spec.type == KafkaTransport.TYPE && spec.direction == PipelineDirection.OUTPUT

   override fun build(
      spec: KafkaTransportOutputSpec,
      logger: PipelineLogger,
      transportFactory: PipelineTransportFactory,
      pipeline: Pipeline

   ) = KafkaOutput(spec)
}

class KafkaOutput(private val spec: KafkaTransportOutputSpec) : PipelineOutputTransport {
   override val description: String = spec.description
   override fun type(schema: Schema): Type {
      return schema.type(spec.targetType)
   }

   override val healthMonitor = EmitterPipelineTransportHealthMonitor()

   private val defaultProps = mapOf(
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.qualifiedName!!,
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.qualifiedName!!
   )
   private val senderOptions = SenderOptions.create<String, String>(spec.props + defaultProps)
   private val sender = KafkaSender.create(senderOptions)
   override fun write(message: MessageContentProvider, logger: PipelineLogger, schema: Schema) {

      var producer = Mono.create<ProducerRecord<String, String>> { sink ->
         logger.info { "Sending message to Kafka topic ${spec.topic}" }

         sink.success(ProducerRecord<String, String>(spec.topic, message.asString(logger)))
      }


      sender.createOutbound().send(producer)
         .then()
         // TODO  :This error is lacking enough context to be useful.  Need a ref to the pipeline instance / message
         .doOnError { error -> logger.error(error) { "Pipeline failed to send to kafka topic ${spec.topic}" } }
         .doOnSuccess { logger.info { "Pipeline published message to topic ${spec.topic}" } }
         .subscribe()
   }
}
