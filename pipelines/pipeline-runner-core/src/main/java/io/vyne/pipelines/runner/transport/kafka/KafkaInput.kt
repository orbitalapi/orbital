package io.vyne.pipelines.runner.transport.kafka

import com.google.common.io.ByteStreams
import io.vyne.pipelines.*
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.DOWN
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.UP
import io.vyne.pipelines.runner.transport.PipelineInputTransportBuilder
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.utils.log
import io.vyne.utils.orElse
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import java.io.OutputStream
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant

@Component
class KafkaInputBuilder : PipelineInputTransportBuilder<KafkaTransportInputSpec> {

   override fun canBuild(spec: PipelineTransportSpec) = spec.type == KafkaTransport.TYPE && spec.direction == PipelineDirection.INPUT

   override fun build(spec: KafkaTransportInputSpec, logger: PipelineLogger, transportFactory: PipelineTransportFactory) = KafkaInput(spec, transportFactory)

}

class KafkaInput(spec: KafkaTransportInputSpec, transportFactory: PipelineTransportFactory) : AbstractKafkaInput<String>(spec, StringDeserializer::class.qualifiedName!!, transportFactory) {

   override val description: String = spec.description
   override fun toMessageContent(message: ReceiverRecord<String, String>, metadata: Map<String, Any>): MessageContentProvider {

      return object : MessageContentProvider {

         override fun asString(logger: PipelineLogger): String {
            logger.debug { "Deserializing record partition=${metadata["partition"]}/ offset=${metadata["offset"]}" }
            return message.value()
         }
         override fun writeToStream(logger: PipelineLogger, outputStream: OutputStream) {
            // Step 1. Get the message
            logger.debug { "Deserializing record partition=${metadata["partition"]}/ offset=${metadata["offset"]}" }
            val messageValue = message.value()
            ByteStreams.copy(messageValue.byteInputStream(), outputStream)
         }
      }
   }
}

object KafkaMetadata {
   const val RECORD_ID = "recordId"
   const val OFFSET = "offset"
   const val PARTITION = "partition"
   const val TOPIC = "topic"
   const val HEADERS = "headers"
}
abstract class AbstractKafkaInput<V>(private val spec: KafkaTransportInputSpec, deserializerClass: String, private val transportFactory: PipelineTransportFactory) : PipelineInputTransport {

   final override val feed: Flux<PipelineInputMessage>

   // Kafka specifics
   private val receiver: KafkaReceiver<String, V>
   private var topicPartitions: Collection<TopicPartition>? = null

   private val defaultProps = mapOf(
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.qualifiedName!!,
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to deserializerClass
   )

   final override val healthMonitor = EmitterPipelineTransportHealthMonitor()

   /**
    * Convert the incoming Kafka message to InputStream for ingestion.
    * Example: convert an Avro binary message to Json string
    */
   protected abstract fun toMessageContent(message: ReceiverRecord<String,V>, metadata:Map<String,Any>):MessageContentProvider

   protected open fun getOverrideOutput(message: ReceiverRecord<String, V>, metadata: Map<String, Any>):PipelineOutputTransport? {
      return null
   }

   init {
      // ENHANCE: there might be a way to hook on some events from the flux below to know when we are actually connected to kafka
      healthMonitor.reportStatus(UP)

      val options = getReceiverOptions(spec)
      receiver = KafkaReceiver.create(options)
      feed = receiver
         .receive()
         .doOnError { healthMonitor.reportStatus(DOWN) }
         .flatMap { kafkaMessage ->
            val recordId = kafkaMessage.key()
            val offset = kafkaMessage.offset()
            val partition = kafkaMessage.partition()
            val topic = kafkaMessage.topic()
            val headers = kafkaMessage.headers().map { it.key() to it.value().toString(Charset.defaultCharset()) }.toMap()

            val metadata = mapOf(
               KafkaMetadata.RECORD_ID to recordId,
               KafkaMetadata.OFFSET to offset,
               KafkaMetadata.PARTITION to partition,
               KafkaMetadata.TOPIC to topic,
               KafkaMetadata.HEADERS to headers
            )

            val messageContent = toMessageContent(kafkaMessage, metadata)
            val overrideOutput = getOverrideOutput(kafkaMessage, metadata)
            Mono.create<PipelineInputMessage> { sink ->
               sink.success(PipelineInputMessage(
                  Instant.now(), // TODO : Surely this is in the headers somewhere?
                  metadata,
                  messageContent,
                  overrideOutput
               ))
            }.doOnSuccess {
               kafkaMessage.receiverOffset().acknowledge()
            }
         }
   }

   protected fun getReceiverOptions(spec: KafkaTransportInputSpec): ReceiverOptions<String, V> {
      return ReceiverOptions.create<String, V>(spec.props + defaultProps)
         .commitBatchSize(0) // Don't commit in batches ..  can explore this later
         .commitInterval(Duration.ZERO) // Don't delay commits .. can explore this later
         .subscription(listOf(spec.topic))
         .addAssignListener { partitions ->
            log().debug("Partitions assigned to KafkaInput: $partitions")
            topicPartitions = partitions.map { it.topicPartition() }


         }
         .addRevokeListener { partitions -> log().debug("Partitions revoked to KafkaInput: $partitions")
            // ENHANCE: there might be a way to hook on some events from the flux below to know when we are actually connected to kafka
            healthMonitor.reportStatus(DOWN)}
   }

   override fun pause() {
      receiver.doOnConsumer { if (topicPartitions != null) it.pause(topicPartitions) }.subscribe()
   }

   override fun resume() {
      receiver.doOnConsumer { if (topicPartitions != null) it.resume(topicPartitions) }.subscribe()
   }

   fun isPaused(): Boolean {
      val paused = receiver.doOnConsumer { it.paused() }.blockOptional()
      return paused.isPresent.orElse(false)
   }
}
