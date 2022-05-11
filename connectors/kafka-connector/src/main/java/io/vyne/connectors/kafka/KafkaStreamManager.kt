package io.vyne.connectors.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.models.DataSource
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.models.json.Jackson
import io.vyne.protobuf.ProtobufFormatSpec
import io.vyne.query.RemoteCall
import io.vyne.query.ResponseMessageType
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.generators.protobuf.ProtobufMessageAnnotation
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

data class KafkaConsumerRequest(
   val connectionName: String,
   val topicName: String,
   val offset: KafkaConnectorTaxi.Annotations.KafkaOperation.Offset,
   val service: Service,
   val operation: RemoteOperation
) {
   val messageType = operation.returnType.name
}

class KafkaStreamManager(
    private val connectionRegistry: KafkaConnectionRegistry,
    private val schemaProvider: SchemaProvider,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper
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

   fun getActiveRequests(): List<KafkaConsumerRequest> = cache.asMap().keys.toList()

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
      val (connectionConfiguration, receiverOptions) = buildReceiverOptions(request)
      val messageType = schemaProvider.schema.type(request.messageType).let { type ->
         require(type.name.name == "Stream") { "Expected to receive a Stream type for consuming from Kafka. Instead found ${type.name.parameterizedName}" }
         type.typeParameters[0]
      }
      // TODO : We need to introduce a vyne annotation - readAsByteArray or something similar
      val encoding = MessageEncodingType.forType(messageType)
      val schema = schemaProvider.schema
      val dataSource = buildDataSource(request, connectionConfiguration)
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
            val messageValue = if (encoding == MessageEncodingType.BYTE_ARRAY) {
               record.value()!!
            } else {
               record.value()!!.toString()
            }

            TypedInstance.from(
               messageType,
               messageValue,
               schema,
               // TODO : How do I provide this more globally / consistently?
               // Difficult to inject given from the base type given how
               // jars are segregated
               formatSpecs = listOf(
                  ProtobufFormatSpec
               ),
               source = dataSource
            )
         }
         .asFlow()
         // SharingStarted.WhileSubscribed() means that we unsubscribe when all subscribers have gone away.
         .shareIn(scope, SharingStarted.WhileSubscribed())
      return flow
   }

   private fun buildDataSource(
      request: KafkaConsumerRequest,
      connectionConfiguration: KafkaConnectionConfiguration
   ): DataSource {

      val remoteCall = RemoteCall(
         service = request.service.name,
         address = connectionConfiguration.brokers,
         operation = request.operation.name,
         responseTypeName = request.operation.returnType.name,
         method = "READ",
         requestBody = objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(mapOf("topic" to request.topicName, "offset" to request.offset)),
         resultCode = 200, // Using HTTP status codes here, becuase I'm not sure what else to use
         // What should we use for the duration?  Using zero, because I can't think of anything better
         durationMs = Duration.ZERO.toMillis(),
         timestamp = Instant.now(),
         responseMessageType = ResponseMessageType.EVENT,
         // Feels like capturing the results are a bad idea.  Can revisit if there's a use-case
         response = "Not captured"
      )
      return OperationResult.from(
         emptyList(),
         remoteCall
      )
   }

   private fun buildReceiverOptions(request: KafkaConsumerRequest): Pair<KafkaConnectionConfiguration, ReceiverOptions<Int, ByteArray>> {
      val connectionConfiguration =
         connectionRegistry.getConnection(request.connectionName) as KafkaConnectionConfiguration

      val topic = request.topicName
      val offset = request.offset.toString().toLowerCase()

      return connectionConfiguration to connectionConfiguration.toReceiverOptions(offset)
         .subscription(listOf(topic))
   }
}

