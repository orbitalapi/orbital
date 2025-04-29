package com.orbitalhq.connectors.kafka

import arrow.core.Either
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Throwables
import com.google.common.cache.CacheBuilder
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.brokers
import com.orbitalhq.connectors.kafka.registry.toReceiverOptions
import com.orbitalhq.errors.ErrorType
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
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.utils.orElse
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
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
   val sink: Sinks.Many<Either<StreamErrorMessage, TypedInstance>>,
   val flux: Flux<Either<StreamErrorMessage, TypedInstance>>
) {
   val flow = flux.asFlow()
}

class KafkaStreamManager(
   private val connectionRegistry: KafkaConnectionRegistry,
   private val schemaProvider: SchemaStore,
   private val scope: CoroutineScope = CoroutineScope(kafkaDispatcher),
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val formatRegistry: FormatRegistry,
   private val meterRegistry: MeterRegistry,
   private val emitConsumerInfoMessages: Boolean,
   private val kafkaConsumerStatsFlowBuilder: KafkaConsumerStatsFlowBuilder,
) {

   private val elasticScheduler: Scheduler = Schedulers.newBoundedElastic(20, Integer.MAX_VALUE, "orbital-kafka-stream")

   // These are the flows returned to callers.
   // They're the long-lived 'outer' flows, that callers subscribe to, which survive
   // schema changes.
   // Internal kafka-facing flows emit into these flows.
   private val publicFlowCache = CacheBuilder.newBuilder()
      .build<KafkaConsumerRequest, PublicEventStream>()

   // These are the internal kafka flows.
   // These connect to Kafka and emit messags into the public flows.
   // These are scoped to live as long as a schema, and are destroyed and recreated
   // when the schema changes
   private val kafkaFlowCache = ConcurrentHashMap<KafkaConsumerRequest, Disposable>()

   private val messageCounter = ConcurrentHashMap<KafkaConsumerRequest, AtomicInteger>()

   init {
      Flux.from(schemaProvider.schemaChanged)
         // Debounce a little
         .bufferTimeout(100, Duration.ofSeconds(5))
         .subscribe {
            val activeRequests = publicFlowCache.asMap()
            if (activeRequests.isNotEmpty()) {
               logger.info { "Schema changed - rebuilding existing Kafka subscriptions (${activeRequests.size} active subscriptions)" }
            }
            activeRequests.entries.forEach { (request, publicFlow) ->
               terminateKafkaFlow(request)
               launchKafkaFlowInto(publicFlow.sink, request)
            }
         }

   }

   /**
    * Returns the message counts received on topics where we're still subscribed.
    */
   fun getActiveConsumerMessageCounts(): Map<KafkaConsumerRequest, AtomicInteger> {
      return messageCounter.toMap()
   }

   fun getActiveRequests(): List<KafkaConsumerRequest> = publicFlowCache.asMap().keys.toList()

   fun getStream(request: KafkaConsumerRequest): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val flow =
         publicFlowCache.getIfPresent(request)?.let {
            logger.info { "Reusing existing kafka subscription for request $request" }
            return it.flow
         }
            ?: publicFlowCache.get(request) {
               getCounter(request) // Force creation

               val sink = Sinks.many().multicast().onBackpressureBuffer<Either<StreamErrorMessage, TypedInstance>>()
               val flux = sink.asFlux()
                  .doOnCancel {
                     logger.info { "Cancelling Kafka subscription as all consumers have gone away: $request" }
                     evictConnection(request)
                  }

               launchKafkaFlowInto(sink, request)
               // Unsubscribe when all consumers have gone away
               val publicSharedFlow = PublicEventStream(sink, flux)
               publicSharedFlow
            }
      return flow.flow
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
      terminateKafkaFlow(consumerRequest)
      publicFlowCache.invalidate(consumerRequest)
      messageCounter.remove(consumerRequest)
      publicFlowCache.cleanUp()
      logger.info { "Evicted connection ${consumerRequest.connectionName} / ${consumerRequest.topicName}" }
   }


   private fun terminateKafkaFlow(request: KafkaConsumerRequest) {
      val existingConsumer = kafkaFlowCache.remove(request)
      if (existingConsumer != null) {
         logger.info { "Terminating internal Kafka consumer for connection ${request.connectionName} topic ${request.topicName}" }
         existingConsumer.dispose()
      }
   }

   private fun launchKafkaFlowInto(
      sink: Sinks.Many<Either<StreamErrorMessage, TypedInstance>>,
      request: KafkaConsumerRequest
   ) {
      // Kill the existing inner flow (and associated Kafka subscription, if present)
      terminateKafkaFlow(request)

      // Create a subscription to Kafka, and emit on the provided flow.
      val kafkaFlow = buildKafkaConsumer(request)
      val subscription = kafkaFlow
         .subscribeOn(Schedulers.boundedElastic())
         .subscribe {
            val emitResult = sink.tryEmitNext(it)
            if (emitResult.isFailure) {
               logger.warn { "Failed to emit Kafka message from ${request.topicName} to consumers: ${emitResult.name} - Message is dropped" }
            }
         }
      // Atomically replace the old consumer (if present) with the new one.
      // This is thread-safe.
      kafkaFlowCache.compute(request) { _, previousConsumerJob ->
         previousConsumerJob?.dispose()
         subscription
      }

   }

   /**
    * Builds the internal, kafka-facing connection.
    * This flow should emit into a seperate, shared public-facing flow, rather than be returned
    * directly to consumers.
    */
   private fun buildKafkaConsumer(request: KafkaConsumerRequest): Flux<Either<StreamErrorMessage, TypedInstance>> {
      logger.info { "Creating new kafka subscription for request $request" }
      val (connectionConfiguration, receiverOptions) = buildReceiverOptions(request)
      // The groupId gets updated by the consumer after we connect, so capture now.
      val originalGroupId = receiverOptions.groupId() ?: "Unknown"

      val messageType = schemaProvider.schema().type(request.messageType).let { type ->
         require(type.name.name == "Stream") { "Expected to receive a Stream type for consuming from Kafka. Instead found ${type.name.parameterizedName}" }
         type.typeParameters[0]
      }
      val encoding = MessageEncodingType.forType(messageType)
      val dataSource = buildDataSource(request, connectionConfiguration)
      val kafkaFlow = KafkaReceiver.create<Any, ByteArray>(
         // Commits are performed when either the interval or batch size is reached.
         receiverOptions
            .commitInterval(Duration.ofSeconds(2L))
            .commitBatchSize(20)
      )
         .receive()
         .publishOn(elasticScheduler) // Cannot block the receiver thread
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

            logger.debug { "Received message on topic ${record.topic()} with offset ${record.offset()}" }
            val messageValue = if (encoding == MessageEncodingType.BYTE_ARRAY) {
               record.value()!!
            } else {
               String(record.value())
            }

            val typedInstanceOrError = try {
               Either.Right(
                  TypedInstance.from(
                     messageType,
                     messageValue,
                     // Note: Don't store a reference here, as the schema may change over the course
                     // of a subscription.
                     // Calls to schemaProvider.schema() should be cheap.
                     schemaProvider.schema(),
                     formatSpecs = formatRegistry.formats,
                     source = dataSource,
                     valueSuppliers = listOf(KafkaValueSupplier(record))
                  )
               )
            } catch (e: Exception) {
               val errorMessage = StreamErrorMessage(
                  timestamp = Instant.now(),
                  exception = e,
                  message = e.message ?: e::class.simpleName!!,
                  typeName = messageType.paramaterizedName,
                  payload = messageValue
               )

               meterRegistry.counter(
                  "orbital.connections.kafka.messageErrors",
                  listOf(
                     MetricTags.KafkaGroupId.of(originalGroupId),
                     MetricTags.ConnectionName.of(request.connectionName),
                     MetricTags.Topic.of(request.topicName)
                  )
               )
                  .increment()
               logger.info { "Failed to parse TypedInstance from kafka data for type => ${messageType.longDisplayName}  - error: ${errorMessage.message}" }
               Either.Left(errorMessage)
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
            val errorMessage = "Error in Kafka connection: ${request.connectionName}, details: ${rootCause.message}"
            Flux.just(ErrorType.errorMessage(errorMessage, schemaProvider.schema(), dataSource).right())
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

