package io.vyne.connectors.kafka

import com.google.common.cache.CacheBuilder
import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.models.TypedInstance
import io.vyne.schemaApi.SchemaProvider
import io.vyne.schemas.QualifiedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.util.concurrent.atomic.AtomicInteger

data class KafkaConsumerRequest(
   val connectionName: String,
   val topicName: String,
   val offset: KafkaConnectorTaxi.Annotations.KafkaOperation.Offset,
   val messageType: QualifiedName
)

class KafkaStreamManager(
   private val connectionRegistry: KafkaConnectionRegistry,
   private val schemaProvider: SchemaProvider,
   private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

   private val logger = KotlinLogging.logger {}

   private val cache = CacheBuilder.newBuilder()
      .build<KafkaConsumerRequest, SharedFlow<TypedInstance>>()

   private val messageCounter = mutableMapOf<KafkaConsumerRequest, AtomicInteger>()

   /**
    * Returns the message counts received on topics where we're still subscribed.
    */
   fun getActiveConsumerMessageCounts(): Map<KafkaConsumerRequest, AtomicInteger> {
      return messageCounter.toMap()
   }

   fun getStream(request: KafkaConsumerRequest): Flow<TypedInstance> {
      return cache.get(request) {
         messageCounter[request] = AtomicInteger(0)
         buildSharedFlow(request)
      }
   }

   private fun evictConnection(consumerRequest: KafkaConsumerRequest) {
     cache.invalidate(consumerRequest)
      messageCounter.remove(consumerRequest)
      cache.cleanUp()
      logger.debug { "Evicted connection ${consumerRequest.connectionName} / ${consumerRequest.topicName}" }
   }

   private fun buildSharedFlow(request: KafkaConsumerRequest): SharedFlow<TypedInstance> {
      logger.info { "Creating new kafka subscription for request $request" }
      val receiverOptions = buildReceiverOptions(request)
      val messageType = schemaProvider.schema().type(request.messageType).let { type ->
         require(type.name.name == "Stream") { "Expected to receive a Stream type for consuming from Kafka. Instead found ${type.name.parameterizedName}" }
         type.typeParameters[0]
      }
      val schema = schemaProvider.schema()
      val flow = KafkaReceiver.create(receiverOptions)
         .receive()
         .doOnSubscribe {
            logger.info { "Subscriber detected for Kafka consumer on ${request.connectionName} / ${request.topicName}" }
         }
         .doOnCancel {
            logger.info { "Subscriber cancel detected for Kafka consumer on ${request.connectionName} / ${request.topicName}" }
            evictConnection(request)
         }
         .map { record ->

            messageCounter[request]?.incrementAndGet()
               ?: logger.warn { "Attempt to increment message counter for consumer on Kafka topic ${request.topicName} failed - the counter was not present" }

            logger.debug { "Received message on topic ${record.topic()} with offset ${record.offset()}" }
            TypedInstance.from(
               messageType,
               record.value()!!,
               schema
            )
         }
         .asFlow()
         // SharingStarted.WhileSubscribed() means that we unsubscribe when all subscribers have gone away.
         .shareIn(scope, SharingStarted.WhileSubscribed())
      return flow

   }

   private fun buildReceiverOptions(request: KafkaConsumerRequest): ReceiverOptions<Int, String> {
      val connectionConfiguration =
         connectionRegistry.getConnection(request.connectionName) as KafkaConnectionConfiguration

      val brokers = connectionConfiguration.brokers
      val groupId = connectionConfiguration.groupId

      val topic = request.topicName
      val offset = request.offset.toString().toLowerCase()

      val consumerProps: MutableMap<String, Any> = HashMap()
      consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokers
      consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = groupId
      consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
      consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
      consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = offset

      return ReceiverOptions
         .create<Int, String>(consumerProps)
         .subscription(listOf(topic))
   }
}
