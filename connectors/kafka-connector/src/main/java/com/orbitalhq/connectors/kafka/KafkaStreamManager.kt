package com.orbitalhq.connectors.kafka

import arrow.core.Either
import arrow.core.left
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Throwables
import com.google.common.cache.CacheBuilder
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.brokers
import com.orbitalhq.connectors.kafka.registry.toReceiverOptions
import com.orbitalhq.metrics.MetricTags
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.OperationResultDataSourceWrapper
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.MessageStreamExchange
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.MessageStreamErrorEvent
import com.orbitalhq.query.tracing.MessageStreamEventReceived
import com.orbitalhq.query.tracing.MessageStreamSubscription
import com.orbitalhq.query.tracing.PayloadEncoding
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.orElse
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.util.retry.Retry
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

data class KafkaConsumerRequest(
   val connectionName: String,
   val topicName: String,
   val offset: KafkaConnectorTaxi.Annotations.KafkaOperation.Offset,
   val service: Service,
   val operation: RemoteOperation,
   val streamSourceId: String? = null
) {
   val messageType = operation.returnType.name

   override fun toString(): String {
      return "KafkaConsumerRequest:  Operation: ${service.name} / ${operation.name} connection: $connectionName topic: $topicName offset: $offset consumerGroupId: ${
         streamSourceId.orElse(
            "-"
         )
      }"
   }
}

val kafkaDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

private data class PublicEventStream(
   val flux: Flux<Either<Pair<MessageStreamErrorEvent, StreamErrorMessage>, Pair<MessageStreamEventReceived, TypedInstance>>>
) {
   val flow = flux.asFlow()
}

/**
 * Manages shared Kafka subscriptions with proper backpressure handling and schema resilience.
 *
 * Requests with the same topic and consumer group ID share a single Kafka consumer via cached Flux instances,
 * this prevents issues where a query issued through the UI starts "stealing" messages from one another,
 * and from background stream jobs that are running.
 *
 * When downstream consumers are slow, backpressure propagates naturally through the reactive chain to the Kafka
 * consumer, which automatically reduces its polling rate rather than dropping messages.
 *
 * The shared Flux uses refCount() to automatically connect when the first subscriber arrives and disconnect when the last subscriber
 * leaves, ensuring proper resource cleanup without manual lifecycle management.
 *
 * Schema changes are handled transparently without restarting Kafka consumers - the message processing logic
 * fetches the current schema on each message, allowing the subscription to adapt to schema updates automatically.
 *
 * This approach replaces the previous two-sink architecture (long-lived public sinks + disposable internal Kafka
 * flows) which required complex buffering and dropping logic to handle backpressure. The new direct reactive
 * chain eliminates message loss while simplifying the codebase and providing true end-to-end backpressure from
 * consumer to Kafka polling.
 *
 * However, changes to connection details are NOT automatically detected, and will require the consumer to cancel
 * the subscription, and restart.
 */
class KafkaStreamManager(
   private val connectionRegistry: KafkaConnectionRegistry,
   private val schemaProvider: SchemaStore,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val formatRegistry: FormatRegistry,
   private val meterRegistry: MeterRegistry,
   private val kafkaConsumerStatsFlowBuilder: KafkaConsumerStatsFlowBuilder,
) {

   private val elasticScheduler: Scheduler = Schedulers.newBoundedElastic(20, Integer.MAX_VALUE, "orbital-kafka-stream")

   // These are the flows returned to callers.
   private val publicFlowCache = CacheBuilder.newBuilder()
      .build<KafkaConsumerRequest, PublicEventStream>()

   private val messageCounter = ConcurrentHashMap<KafkaConsumerRequest, AtomicInteger>()
   private val droppedMessageCounter = ConcurrentHashMap<KafkaConsumerRequest, AtomicInteger>()

   /**
    * Returns the message counts received on topics where we're still subscribed.
    */
   fun getActiveConsumerMessageCounts(): Map<KafkaConsumerRequest, AtomicInteger> {
      return messageCounter.toMap()
   }

   fun getActiveRequests(): List<KafkaConsumerRequest> = publicFlowCache.asMap().keys.toList()

   /**
    * Returns a connected, sharable stream of messages from Kafka.
    *
    * This stream survives schema changes (so messages are always parsed against the schema at the time of message arrival).
    * The stream stays active as long as there is at least 1 subscriber.
    *
    */
   fun getStream(request: KafkaConsumerRequest): Pair<MessageStreamSubscription, Flow<Either<Pair<MessageStreamErrorEvent, StreamErrorMessage>, Pair<MessageStreamEventReceived, TypedInstance>>>> {
      val flowFromCache =
         publicFlowCache.getIfPresent(request)?.let {
            logger.info { "Reusing existing kafka subscription for request $request" }
            it.flow
         };
      if (flowFromCache != null) {
         return MessageStreamSubscription(
            request.topicName,
            request.connectionName,
            MessageStreamSubscription.SubscriptionAction.JOINED_EXISTING_SUBSCRIPTION
         ) to flowFromCache
      }


      val newFlow = publicFlowCache.get(request) {
         getCounter(request) // Force creation

         val flux = buildKafkaConsumer(request)
            .doOnCancel {
               // Unsubscribe when all consumers have gone away
               logger.info { "Cancelling Kafka subscription as all consumers have gone away: $request" }
               evictConnection(request)
            }

         val publicSharedFlow = PublicEventStream(flux)
         publicSharedFlow
      }
      val subscriptionEvent = MessageStreamSubscription(
         request.topicName,
         request.connectionName,
         MessageStreamSubscription.SubscriptionAction.CREATED_NEW_SUBSCRIPTION
      )
      return subscriptionEvent to newFlow.flow
   }

   private fun startTopicMonitoring(request: KafkaConsumerRequest) {
      val (connectionConfiguration, receiverOptions) = buildReceiverOptions(request)
      kafkaConsumerStatsFlowBuilder.startMonitoring(request, connectionConfiguration, receiverOptions)
   }

   private fun getCounter(request: KafkaConsumerRequest): AtomicInteger {
      return messageCounter.getOrPut(request) {
         logger.info { "Creating Kafka message counter for topic ${request.topicName}" }
         AtomicInteger(0)
      }
   }

   private fun evictConnection(consumerRequest: KafkaConsumerRequest) {
      kafkaConsumerStatsFlowBuilder.stopMonitoring(consumerRequest)
      publicFlowCache.invalidate(consumerRequest)
      messageCounter.remove(consumerRequest)
      publicFlowCache.cleanUp()
      logger.info { "Evicted connection ${consumerRequest.connectionName} / ${consumerRequest.topicName}" }
   }


   fun getDroppedMessageCounts(): Map<KafkaConsumerRequest, Int> {
      return this.droppedMessageCounter.mapValues { (key, value) -> value.get() }
   }

   /**
    * Builds the internal, kafka-facing connection.
    * This flow should emit into a seperate, shared public-facing flow, rather than be returned
    * directly to consumers.
    */
   private fun buildKafkaConsumer(request: KafkaConsumerRequest): Flux<Either<Pair<MessageStreamErrorEvent, StreamErrorMessage>, Pair<MessageStreamEventReceived, TypedInstance>>> {
      logger.info { "Creating new kafka subscription for request $request" }

      /**
       * Returns the encoding and message type, as defined in the schema
       * at the time of calling.
       * Don't cache / store these values, as the definition of a type can change over the
       * lifetime of a subscription
       */
      fun getMessageTypeAndEncoding():Pair<MessageEncodingType, Type> {
         // This lookup needs to happen inside the .map { .. }
         // so that we do it on each message (it should be cheap), as
         // the schema evolves over the course of a subscription
         val messageType = schemaProvider.schema().type(request.messageType).let { type ->
            require(type.name.name == "Stream") { "Expected to receive a Stream type for consuming from Kafka. Instead found ${type.name.parameterizedName}" }
            type.typeParameters[0]
         }
         val encoding = MessageEncodingType.forType(messageType)
         return encoding to messageType
      }

      val (connectionConfiguration, receiverOptions) = buildReceiverOptions(request)
      // The groupId gets updated by the consumer after we connect, so capture now.
      val originalGroupId = receiverOptions.groupId() ?: "Unknown"


      val dataSource = buildDataSource(request, connectionConfiguration)
      val kafkaFlow = KafkaReceiver.create<Any, ByteArray>(
         // Commits are performed when either the interval or batch size is reached.
         receiverOptions
            .commitInterval(Duration.ofSeconds(2L))
            .commitBatchSize(20)
      )
         .receive()
         .publishOn(elasticScheduler) // Cannot block the receiver thread
         .publish()
         .refCount(1) // No grace period - disconnect immediately.
         .doOnSubscribe {
            logger.info { "Subscriber detected for Kafka consumer on ${request.connectionName} / ${request.topicName}" }
         }
         .repeat()
         .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).transientErrors(true))
         .doOnComplete {
            logger.info { "Flow Complete detected for Kafka consumer on ${request.connectionName} / ${request.topicName}" }
//            evictConnection(request)
         }
         .doOnCancel {
            // Note: Don't
            logger.info { "Subscriber cancel detected for Kafka consumer on ${request.connectionName} / ${request.topicName}" }
//            evictConnection(request)
         }
         .doOnEach { _ ->
            meterRegistry.counter(
               "orbital.connections.kafka.messagesReceived",
               listOf(
                  MetricTags.KafkaGroupId.of(originalGroupId),
                  MetricTags.Topic.of(request.topicName),
                  MetricTags.ConnectionName.of(request.connectionName)
               )
            )
               .increment()
         }
         .map { record ->
            getCounter(request).incrementAndGet()

           val (encoding, messageType) = getMessageTypeAndEncoding()

            logger.debug { "Received message on topic ${record.topic()} with offset ${record.offset()}" }
            val messageValue = when {
               record.value() == null -> null
               encoding == MessageEncodingType.BYTE_ARRAY -> record.value()
               else -> String(record.value())
            }
            val (messageEncoding, messageValueAsString) = when (messageValue) {
               null -> PayloadEncoding.STRING to { null }
               is ByteArray -> PayloadEncoding.BASE64_BYTEARRAY to { Base64.getEncoder().encodeToString(messageValue) }
               else -> PayloadEncoding.STRING to { messageValue as String }
            }

            fun incrementErrorCounter() {
               meterRegistry.counter(
                  "orbital.connections.kafka.messageErrors",
                  listOf(
                     MetricTags.KafkaGroupId.of(originalGroupId),
                     MetricTags.ConnectionName.of(request.connectionName),
                     MetricTags.Topic.of(request.topicName)
                  )
               )
                  .increment()
            }


            val typedInstanceOrError = try {
               if ((messageValue as Any?) == null) {
                  val errorMessage = StreamErrorMessage(
                     timestamp = Instant.now(),
                     exception = InvalidPayloadException,
                     message = "A message without a payload was received on topic ${request.topicName}",
                     typeName = messageType.paramaterizedName,
                     payload = ""
                  )
                  Either.Left(
                     MessageStreamErrorEvent(
                        0,
                        errorMessage.message,
                        messageEncoding,
                        messageValueAsString
                     ) to errorMessage
                  )
               } else {
                  // Note: Don't store a reference globally, as the schema may change over the course
                  // of a subscription.
                  // Calls to schemaProvider.schema() should be cheap.
                  val currentSchema = schemaProvider.schema()
                  val messageReceivedEvent = MessageStreamEventReceived(
                     record.serializedValueSize().toLong(),
                     messageEncoding,
                     messageValueAsString
                  )
                  val typedInstance = TypedInstance.from(
                     messageType,
                     messageValue,
                     currentSchema,
                     formatSpecs = formatRegistry.formats,
                     source = dataSource,
                     valueSuppliers = listOf(KafkaValueSupplier(record))
                  )
                  Either.Right(
                     messageReceivedEvent to typedInstance
                  )
               }

            } catch (e: Exception) {
               val errorMessage = StreamErrorMessage(
                  timestamp = Instant.now(),
                  exception = e,
                  message = e.message ?: e::class.simpleName!!,
                  typeName = messageType.paramaterizedName,
                  payload = messageValue ?: ""
               )

               incrementErrorCounter()
               logger.info { "Failed to parse TypedInstance from kafka data for type => ${messageType.longDisplayName}  - error: ${errorMessage.message}" }
               Either.Left(
                  MessageStreamErrorEvent(
                     record.serializedValueSize().toLong(),
                     errorMessage.message,
                     messageEncoding,
                     messageValueAsString
                  ) to errorMessage
               )
            } finally {
               // Only offsets explicitly acknowledged using ReceiverOffset#acknowledge() are committed.
               record.receiverOffset().acknowledge()
            }
            typedInstanceOrError
         }
         .onErrorResume { error ->
            // Note: These are errors thrown when making a connection, NOT
            // parsing errors within the message itself.

            val rootCause = Throwables.getRootCause(error)
            logger.error(rootCause) { "Error in Kafka subscription for kafka connection ${request.connectionName}" }
            // see the error handling notes for SharedFlow:
            // https://github.com/Kotlin/kotlinx.coroutines/issues/2034
            val detailMessage =
               "${rootCause::class.simpleName} - ${rootCause.message ?: "No further details available"}"
            val errorMessageText = "Error in Kafka connection: ${request.connectionName}, details: $detailMessage"
            val errorEvent = MessageStreamErrorEvent(0, errorMessageText, PayloadEncoding.STRING, { null })
            val streamErrorMessage = StreamErrorMessage(
               timestamp = Instant.now(),
               exception = rootCause,
               message = rootCause.message ?: rootCause::class.simpleName!!,
               typeName = request.messageType.parameterizedName,
               payload = errorMessageText
            )
            val errorResponse = (errorEvent to streamErrorMessage).left()
            Flux.just(errorResponse)
         }

      startTopicMonitoring(request)
      return kafkaFlow
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

   private fun buildReceiverOptions(request: KafkaConsumerRequest): Pair<KafkaConnectionConfiguration, ReceiverOptions<Any, ByteArray>> {
      val connectionConfiguration =
         connectionRegistry.getConnection(request.connectionName)

      val topic = request.topicName
      val offset = request.offset.toString().lowercase(Locale.getDefault())

      return connectionConfiguration to connectionConfiguration.toReceiverOptions(offset, request)
         .subscription(listOf(topic))
   }

}


object InvalidPayloadException : RuntimeException("An invalid payload was received")
