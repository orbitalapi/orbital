package io.vyne.pipelines.runner.transport.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.TypedInstance
import io.vyne.pipelines.*
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.DOWN
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.UP
import io.vyne.pipelines.runner.transport.PipelineInputTransportBuilder
import io.vyne.schemas.Schema
import io.vyne.utils.log
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant

@Component
class KafkaInputBuilder(val objectMapper: ObjectMapper) : PipelineInputTransportBuilder<KafkaTransportInputSpec> {

   override fun canBuild(spec: PipelineTransportSpec) = spec.type == KafkaTransport.TYPE && spec.direction == PipelineDirection.INPUT

   override fun build(spec: KafkaTransportInputSpec) = KafkaInput(spec, objectMapper)

}

class KafkaInput(spec: KafkaTransportInputSpec, objectMapper: ObjectMapper): AbstractKafkaInput<String>(spec, objectMapper, StringDeserializer::class.qualifiedName!!) {

   override fun toStringMessage(message: String): String = message

}

abstract class  AbstractKafkaInput<V>(val spec: KafkaTransportInputSpec, objectMapper: ObjectMapper, deserializerClass: String) : PipelineInputTransport, AbstractPipelineTransportHealthMonitor() {

   override val feed: Flux<PipelineInputMessage>
   private val receiver: KafkaReceiver<String, V>;

   private lateinit var topicPartitions: Collection<TopicPartition>

   /**
    * Convert the incoming Kafka message to String for ingestion.
    * Example: convert an Avro binary message to Json string
    */
   abstract fun toStringMessage(message: V) : String


   init {
      reportStatus(UP)

      receiver = KafkaReceiver.create(getReceiverOptions(spec))
      feed = receiver
         .receive()
         .log("KAKFA")
         .doOnError { reportStatus(DOWN) }
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
               val message = kafkaMessage.value()

               logger.debug { "Converting Map to TypeInstance of ${targetType.fullyQualifiedName}" }

               val map = toStringMessage(message)
               TypedInstance.from(targetType, objectMapper.readTree(map), schema)
            }
            Mono.create<PipelineInputMessage> { sink ->
               sink.success(PipelineInputMessage(
                  Instant.now(), // TODO : Surely this is in the headers somewhere?
                  metadata,
                  messageProvider
               ))
            }.doOnSuccess {
               log().info("ACKNOWLEDGE MESSAGE")
               kafkaMessage.receiverOffset().acknowledge()
            }
         }
   }

   private val defaultProps = mapOf(
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.qualifiedName!!,
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to  deserializerClass
   )

   fun getReceiverOptions(spec: KafkaTransportInputSpec): ReceiverOptions<String, V> {
      return ReceiverOptions.create<String, V>(spec.props + defaultProps)
         .commitBatchSize(0) // Don't commit in batches ..  can explore this later
         .commitInterval(Duration.ZERO) // Don't delay commits .. can explore this later
         .subscription(listOf(spec.topic))
         .addAssignListener { partitions ->
            log().debug("onPartitionsAssigned: $partitions")
            topicPartitions = partitions.map { it.topicPartition() }
         }
         .addRevokeListener { partitions -> log().debug("onPartitionsRevoked: $partitions") }
   }

   override fun pause() { receiver.doOnConsumer { it.pause(topicPartitions) }.subscribe() }
   override fun resume() { receiver.doOnConsumer { it.resume(topicPartitions) }.subscribe() }

}
