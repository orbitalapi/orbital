package com.orbitalhq.connectors.kafka

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.orbitalhq.VyneTypes
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.OperationResultDataSourceWrapper
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.QueryContextSchemaProvider
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.connectors.OperationCachingBehaviour
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.query.tracing.MessageStreamDisconnection
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import lang.taxi.services.OperationScope
import lang.taxi.types.PrimitiveType
import mu.KotlinLogging


private val logger = KotlinLogging.logger { }

class KafkaInvoker(
   private val streamManager: KafkaStreamManager,
   private val streamWriter: KafkaStreamPublisher
) : OperationInvoker {
   companion object {
      init {
         KafkaConnectorTaxi.registerMetadataUsage()
      }
   }

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(KafkaConnectorTaxi.Annotations.KafkaService.NAME) && operation.hasMetadata(
         KafkaConnectorTaxi.Annotations.KafkaOperation.NAME
      )
   }

   override fun getCachingBehaviour(service: Service, operation: RemoteOperation): OperationCachingBehaviour {
      return OperationCachingBehaviour.NO_CACHE
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {

      val connectionName =
         service.firstMetadata("${VyneTypes.NAMESPACE}.kafka.KafkaService").params["connectionName"] as String
      val kafkaOperation = operation.firstMetadata(KafkaConnectorTaxi.Annotations.KafkaOperation.NAME)
         .let { KafkaConnectorTaxi.Annotations.KafkaOperation.from(it) }

      return if (operation.operationType == OperationScope.MUTATION) {
         publishToTopic(connectionName, kafkaOperation, service, operation, eventDispatcher, queryId, parameters)
      } else {
         subscribeToTopic(connectionName, kafkaOperation, service, operation, eventDispatcher, queryId, queryOptions)
      }

   }

   private fun publishToTopic(
      connectionName: String,
      kafkaOperation: KafkaConnectorTaxi.Annotations.KafkaOperation,
      service: Service,
      operation: RemoteOperation,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      parameters: List<Pair<Parameter, TypedInstance>>
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      require(parameters.size == 1) { "Expected a single parameter (the message to publish), but found ${parameters.size}" }

      require(eventDispatcher is QueryContextSchemaProvider) { "EventDispatcher is not a QueryContext, Need a way to access the schema " }
      val schema = eventDispatcher.schema
      // TODO: Get the key from the parameters.
      val key = TypedNull.create(schema.type(PrimitiveType.STRING))

      val messagePayload = parameters.single().second
      return streamWriter.write(
         connectionName, kafkaOperation, service, operation, eventDispatcher, queryId, messagePayload, key, schema
      )
   }

   private fun subscribeToTopic(
      connectionName: String,
      kafkaOperation: KafkaConnectorTaxi.Annotations.KafkaOperation,
      service: Service,
      operation: RemoteOperation,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val span = eventDispatcher.createOperationTraceSpan(service, operation, kafkaOperation.topic)

      val (eventMetadata, rawStream) = streamManager.getStream(
         KafkaConsumerRequest(
            connectionName,
            kafkaOperation.topic,
            kafkaOperation.offset,
            service,
            operation,
            streamSourceId = queryOptions.streamConsumerId
         )
      )
      span.emitEvent(TracingEventKind.OK, SpanState.ACTIVE, null, eventMetadata, "Subscribe")
      val stream = rawStream.mapNotNull { errorOrInstance ->
         when (errorOrInstance) {
            is Either.Right -> {
               val (traceMessage, instance) = errorOrInstance.value
               val dataSource = instance.source
               val event = span.emitEvent(TracingEventKind.OK, SpanState.ACTIVE, instance.type, traceMessage, "Message received")

               require(dataSource is OperationResultDataSourceWrapper) { "Expected OperationResultDataSourceWrapper as the datasource, found ${dataSource::class.simpleName}" }
               val dataSourceWithTraceId = dataSource.copy(sourceEventId = event.idSet)
               eventDispatcher.reportRemoteOperationInvoked(dataSourceWithTraceId.operationResult, queryId)

               DataSourceUpdater.update(instance, dataSourceWithTraceId.operationResultReferenceSource).right()
            }

            is Either.Left -> {
               val (traceEvent, streamErrorMessage) = errorOrInstance.value
               span.emitEvent(TracingEventKind.OK, SpanState.ACTIVE, null, traceEvent, "Message error")
               streamErrorMessage.left()
            }
         }
      }.onCompletion {
         span.emitEvent(TracingEventKind.OK, SpanState.COMPLETE, null, MessageStreamDisconnection(disconnectionAction = MessageStreamDisconnection.DisconnectionAction.NOT_CAPTURED), "Disconnect")
      }
      return stream
   }
}
