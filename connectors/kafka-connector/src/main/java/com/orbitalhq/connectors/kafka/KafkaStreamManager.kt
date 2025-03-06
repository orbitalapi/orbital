package com.orbitalhq.connectors.kafka

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.connectors.StreamErrorMessage
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.brokers
import com.orbitalhq.connectors.kafka.registry.toReceiverOptions
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.OperationResultDataSourceWrapper
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.MessageStreamExchange
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.util.retry.Retry
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
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
}

class KafkaStreamManager(
   private val connectionRegistry: KafkaConnectionRegistry,
   private val schemaProvider: SchemaProvider,
   private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val formatRegistry: FormatRegistry,
   private val meterRegistry: MeterRegistry,
   private val emitConsumerInfoMessages: Boolean,
   private val kafkaConsumerStatsFlowBuilder:KafkaConsumerStatsFlowBuilder
) {

   private val elasticScheduler: Scheduler = Schedulers.newBoundedElastic(20, Integer.MAX_VALUE, "orbital-kafka-stream")
   private val cache = CacheBuilder.newBuilder()
      .build<KafkaConsumerRequest, Flow<Either<StreamErrorMessage, TypedInstance>>>()

   private val messageCounter = ConcurrentHashMap<KafkaConsumerRequest, AtomicInteger>()

   /**
    * Returns the message counts received on topics where we're still subscribed.
    */
   fun getActiveConsumerMessageCounts(): Map<KafkaConsumerRequest, AtomicInteger> {
      return messageCounter.toMap()
   }

   fun getActiveRequests(): List<KafkaConsumerRequest> = cache.asMap().keys.toList()

   fun getStream(request: KafkaConsumerRequest): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val flow = cache.getIfPresent(request)?.let {
         logger.info { "Reusing existing kafka subscription for request $request" }
         return it
      }
         ?: cache.get(request) {
            getCounter(request) // Force creation
            val kafkaMessagesFlow = buildSharedFlow(request)
            // We create the stats flow regardless of if emitConsumerInfoMessages is set to true.
            // This is so we can emit stats to prometheus.
            // However, if emitConsumerInfoMessages = false,
            // we filter the messages out so they don't appear in the stream.
            val statsFlow = buildStatsFlow(request)
               .filter { emitConsumerInfoMessages }
            merge(kafkaMessagesFlow, statsFlow)
         }
      return flow
   }

   private fun buildStatsFlow(request: KafkaConsumerRequest): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val (connectionConfiguration, receiverOptions) = buildReceiverOptions(request)
      val consumerStatsFlow = kafkaConsumerStatsFlowBuilder
         .buildAndWrapAsErrorMessages(request, connectionConfiguration, receiverOptions)
         .map {
            it.left()
         }
      return consumerStatsFlow
   }

   private fun getCounter(request: KafkaConsumerRequest): AtomicInteger {
      return messageCounter.getOrPut(request) {
         logger.info { "Creating Kafka message counter for topic ${request.topicName}" }
         AtomicInteger(0)
      }
   }

   private fun evictConnection(consumerRequest: KafkaConsumerRequest) {
      cache.invalidate(consumerRequest)
      messageCounter.remove(consumerRequest)
      cache.cleanUp()
      logger.info { "Evicted connection ${consumerRequest.connectionName} / ${consumerRequest.topicName}" }
   }

   private fun buildSharedFlow(request: KafkaConsumerRequest): SharedFlow<Either<StreamErrorMessage, TypedInstance>> {
      logger.info { "Creating new kafka subscription for request $request" }
      val (connectionConfiguration, receiverOptions) = buildReceiverOptions(request)

      val messageType = schemaProvider.schema.type(request.messageType).let { type ->
         require(type.name.name == "Stream") { "Expected to receive a Stream type for consuming from Kafka. Instead found ${type.name.parameterizedName}" }
         type.typeParameters[0]
      }
      val encoding = MessageEncodingType.forType(messageType)
      val schema = schemaProvider.schema
      val dataSource = buildDataSource(request, connectionConfiguration)
      val flow = KafkaReceiver.create<Any, ByteArray>(
         // Commits are performed when either the interval or batch size is reached.
         receiverOptions
            .commitInterval(Duration.ofSeconds(2L))
            .commitBatchSize(20)
      )
         .receive()
         .publishOn(elasticScheduler) // Cannot block the receiver thread
         .doOnSubscribe {
            logger.info { "Subscriber detected for Kafka consumer on ${request.connectionName} / ${request.topicName}" }
         }.doOnError {
            logger.error(it) { "Error in kafka subscriber" }
         }
         .repeat()
         .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).transientErrors(true))
         .doOnComplete {
            logger.info { "Flow Complete detected for Kafka consumer on ${request.connectionName} / ${request.topicName}" }
            evictConnection(request)
         }
         .doOnCancel {
            logger.info { "Subscriber cancel detected for Kafka consumer on ${request.connectionName} / ${request.topicName}" }
            evictConnection(request)
         }
         .doOnEach { _ ->
            meterRegistry.counter("orbital.connections.kafka.${request.connectionName}.topic.${request.topicName}.messagesReceived")
               .increment()
         }
         .map { record ->
            getCounter(request).incrementAndGet()

            logger.trace { "Received message on topic ${record.topic()} with offset ${record.offset()}" }
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
                     schema,
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

               logger.info { "Failed to parse TypedInstance from kafka data for type => ${messageType.longDisplayName}  - error: ${errorMessage.message}" }
               Either.Left(errorMessage)
            } finally {
               // Only offsets explicitly acknowledged using ReceiverOffset#acknowledge() are committed.
               record.receiverOffset().acknowledge()
            }
            typedInstanceOrError
         }
         .asFlow()
         .catch {

            logger.error(
               it.cause ?: it
            ) { "Error in Kafka subscription for kafka connection ${request.connectionName}" }
            // see the error handling notes for SharedFlow:
            // https://github.com/Kotlin/kotlinx.coroutines/issues/2034
            val errorMessage = "Error in Kafka connection: ${request.connectionName}, details: ${it.cause?.message}"
            this.emit(ErrorType.errorMessage(errorMessage, schemaProvider.schema, dataSource).right())
         }

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

   private fun buildReceiverOptions(request: KafkaConsumerRequest): Pair<KafkaConnectionConfiguration, ReceiverOptions<Any, ByteArray>> {
      val connectionConfiguration =
         connectionRegistry.getConnection(request.connectionName)

      val topic = request.topicName
      val offset = request.offset.toString().lowercase(Locale.getDefault())

      return connectionConfiguration to connectionConfiguration.toReceiverOptions(offset, request)
         .subscription(listOf(topic))
   }

}

