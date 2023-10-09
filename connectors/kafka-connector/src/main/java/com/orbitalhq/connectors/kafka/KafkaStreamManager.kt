package com.orbitalhq.connectors.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.brokers
import com.orbitalhq.connectors.kafka.registry.toReceiverOptions
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.OperationResultDataSourceWrapper
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.protobuf.ProtobufFormatSpec
import com.orbitalhq.query.MessageStreamExchange
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.time.Duration
import java.time.Instant
import java.util.*
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
      logger.info { "Evicted connection ${consumerRequest.connectionName} / ${consumerRequest.topicName}" }
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
         .doOnComplete {
            logger.info { "Flow Complete detected for Kafka consumer on ${request.connectionName} / ${request.topicName}" }
            evictConnection(request)
         }
         .doOnCancel {
            logger.info { "Subscriber cancel detected for Kafka consumer on ${request.connectionName} / ${request.topicName}" }
            evictConnection(request)
         }
         .map { record ->

            messageCounter[request]?.incrementAndGet()
               ?: logger.warn { "Attempt to increment message counter for consumer on Kafka topic ${request.topicName} failed - the counter was not present" }

            logger.trace { "Received message on topic ${record.topic()} with offset ${record.offset()}" }
            val messageValue = if (encoding == MessageEncodingType.BYTE_ARRAY) {
               record.value()!!
            } else {
               String(record.value())
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
         requestBody = objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(mapOf("topic" to request.topicName, "offset" to request.offset)),
         // What should we use for the duration?  Using zero, because I can't think of anything better
         durationMs = Duration.ZERO.toMillis(),
         timestamp = Instant.now(),
         responseMessageType = ResponseMessageType.EVENT,
         // Feels like capturing the results are a bad idea.  Can revisit if there's a use-case
         response = null,
         exchange = MessageStreamExchange(
            topic = request.topicName
         )
      )
      return OperationResultDataSourceWrapper(
         OperationResult.from(
            emptyList(),
            remoteCall
         )
      )
   }

   private fun buildReceiverOptions(request: KafkaConsumerRequest): Pair<KafkaConnectionConfiguration, ReceiverOptions<Int, ByteArray>> {
      val connectionConfiguration =
         connectionRegistry.getConnection(request.connectionName)

      val topic = request.topicName
      val offset = request.offset.toString().lowercase(Locale.getDefault())

      return connectionConfiguration to connectionConfiguration.toReceiverOptions(offset)
         .subscription(listOf(topic))
   }
}

