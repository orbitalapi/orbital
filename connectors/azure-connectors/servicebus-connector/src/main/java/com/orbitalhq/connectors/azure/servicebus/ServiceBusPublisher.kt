package com.orbitalhq.connectors.azure.servicebus

import arrow.core.Either
import com.azure.core.util.BinaryData
import com.azure.messaging.servicebus.ServiceBusMessage
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.connectors.BatchWriteCacheProvider
import com.orbitalhq.metrics.MetricTags
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.SqlExchange
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.Instant


private val logger = KotlinLogging.logger { }

class ServiceBusPublisher(
   private val serviceBusConnectionFactory: ServiceBusConnectionFactory,
   private val formatRegistry: FormatRegistry,
   private val meterRegistry: MeterRegistry,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper
) {
   private val batchWriteCacheProvider = BatchWriteCacheProvider<TypedInstance, OperationResultReference>()


   fun publishToTopic(
      connectionName: String,
      serviceBusTopicOperation: ServiceBusTaxi.Annotations.ServiceBusTopicPublicationOperation,
      payload: TypedInstance,
      schema: Schema
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val message = buildServiceBusMessage(payload, schema)
      return serviceBusConnectionFactory
         .topicSender(connectionName, serviceBusTopicOperation.topic)
         .sendMessage(message)
         .subscribeOn(Schedulers.boundedElastic())
         .doOnEach { _ ->
            meterRegistry.counter(
               "orbital.connections.servicebus.topic.messagesPublished",
               listOf(
                  MetricTags.ConnectionName.of(connectionName),
                  MetricTags.Topic.of(serviceBusTopicOperation.topic),
               )
            ).increment()
         }.then(Mono.fromCallable { Either.Right(payload) }).asFlow().flowOn(Dispatchers.IO)
   }

   fun publishToQueue(
      connectionName: String,
      serviceBusQueueOperation: ServiceBusTaxi.Annotations.ServiceBusQueueOperation,
      payload: TypedInstance,
      schema: Schema
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val message = buildServiceBusMessage(payload, schema)
      return serviceBusConnectionFactory
         .queueSender(connectionName, serviceBusQueueOperation.queue)
         .sendMessage(message)
         .subscribeOn(Schedulers.boundedElastic())
         .doOnEach { _ ->
            meterRegistry.counter(
               "orbital.connections.servicebus.messagesPublished",
               listOf(
                  MetricTags.ConnectionName.of(connectionName),
                  MetricTags.Queue.of(serviceBusQueueOperation.queue),
               )
            )
               .increment()
         }.then(Mono.fromCallable { Either.Right(payload) }).asFlow()
   }

   suspend fun batchPublishToTopic(
      service: Service,
      operation: RemoteOperation,
      queryId: String,
      connectionName: String,
      serviceBusTopicOperation: ServiceBusTaxi.Annotations.ServiceBusTopicPublicationOperation,
      payload: TypedInstance,
      schema: Schema
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {

      val asyncSender = serviceBusConnectionFactory
         .topicSender(connectionName, serviceBusTopicOperation.topic)

      return this.doBatch(
         service,
         operation,
         queryId,
         connectionName,
         serviceBusTopicOperation.cacheSpec!!,
         payload,
         schema,
         asyncSender,
         listOf(MetricTags.Topic.of(serviceBusTopicOperation.topic))
      )
   }

   suspend fun batchPublishToQueue(
      service: Service,
      operation: RemoteOperation,
      queryId: String,
      connectionName: String,
      serviceBusTopicOperation: ServiceBusTaxi.Annotations.ServiceBusQueueOperation,
      payload: TypedInstance,
      schema: Schema
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {

      val asyncSender = serviceBusConnectionFactory
         .queueSender(connectionName, serviceBusTopicOperation.queue)

      return this.doBatch(
         service,
         operation,
         queryId,
         connectionName,
         serviceBusTopicOperation.cacheSpec!!,
         payload,
         schema,
         asyncSender,
         listOf(MetricTags.Queue.of(serviceBusTopicOperation.queue))
      )
   }

   private suspend fun doBatch(
      service: Service,
      operation: RemoteOperation,
      queryId: String,
      connectionName: String,
      serviceBusCacheSpec: ServiceBusTaxi.Annotations.ServiceBusCacheSpec,
      payload: TypedInstance,
      schema: Schema,
      asyncSender: ServiceBusSenderAsyncClient,
      metricsTags: List<Tag>,
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val batchWriteCache = batchWriteCacheProvider.forQueryId(
         queryId,
         serviceBusCacheSpec.batchSize,
         serviceBusCacheSpec.batchDuration,
         currentCoroutineContext().job
      ) { batchItems ->
         val serviceBusMessages = batchItems.map { message -> buildServiceBusMessage(message, schema) }
         logger.info { "publishing batch size of ${batchItems.size}" }
         asyncSender
            .sendMessages(serviceBusMessages)
            .subscribeOn(Schedulers.boundedElastic())
            .doOnEach { _ ->
               meterRegistry.counter(
                  "orbital.connections.servicebus.messagesPublished",
                  metricsTags + MetricTags.ConnectionName.of(connectionName)
               )
                  .increment(serviceBusMessages.size.toDouble())
            }
            .doOnSubscribe {
               logger.info { "batch request is subscribed." }
            }
            .elapsed()
            .then(Mono.fromCallable {
               logger.info { "published batch size of ${batchItems.size}" }
               val operationResult = buildOperationResult(
                  service,
                  operation,
                  emptyList(),
                  "Upsert ${batchItems.size} items to collection $batchItems",
                  connectionName,
                  java.time.Duration.ofMillis(1),
                  recordCount = batchItems.size,
               )
               operationResult.asOperationReferenceDataSource()
            })
      }

      return batchWriteCache.emit(payload)
         .map { operationResult ->
            Either.Right(DataSourceUpdater.update(payload, operationResult))
         }
         .asFlow()
         .flowOn(Dispatchers.IO)

   }

   protected fun buildOperationResult(
      service: Service,
      operation: RemoteOperation,
      parameters: List<TypedInstance>,
      criteria: String,
      mongoHosts: String,
      elapsed: Duration,
      recordCount: Int,
      verb: String = "Query"
   ): OperationResult {

      val remoteCall = buildRemoteCall(service, mongoHosts, operation, criteria, elapsed, recordCount, verb)
      return OperationResult.fromTypedInstances(
         parameters,
         remoteCall
      )
   }

   protected fun buildRemoteCall(
      service: Service,
      mongoHosts: String,
      operation: RemoteOperation,
      criteria: String,
      elapsed: Duration,
      recordCount: Int,
      verb: String
   ) = RemoteCall(
      service = service.name,
      address = mongoHosts,
      operation = operation.name,
      responseTypeName = operation.returnType.name,
      requestBody = criteria,
      durationMs = elapsed.toMillis(),
      timestamp = Instant.now(),
      // If we implement streaming database queries, this will change
      responseMessageType = ResponseMessageType.FULL,
      // Feels like capturing the results are a bad idea.  Can revisit if there's a use-case
      response = null,
      exchange = SqlExchange(
         sql = criteria,
         recordCount = recordCount,
         verb = verb,
         parameters = null
      ),

      )

   private fun buildServiceBusMessage(
      payload: TypedInstance,
      schema: Schema
   ): ServiceBusMessage {
      val (metadata, format) = formatRegistry.forType(payload.type)
      val messageContent = if (format != null) {
         format.serializer.writeAsBytes(payload, metadata!!, schema, -1)
      } else {
         val rawObject = payload.toRawObject()
         objectMapper.writeValueAsBytes(rawObject)
      }
      return ServiceBusMessage(BinaryData.fromBytes(messageContent))
   }


}
