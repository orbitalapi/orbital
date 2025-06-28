package com.orbitalhq.connectors.aws.sqs

import arrow.core.Either
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.QueryContextSchemaProvider
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.MessageStreamSubscription
import com.orbitalhq.query.tracing.MessageStreamEventReceived
import com.orbitalhq.query.tracing.PayloadEncoding
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.query.tracing.OperationTraceSpan
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.services.OperationScope
import mu.KotlinLogging

class SqsInvoker(
   private val schemaProvider: SchemaProvider,
   private val sqsStreamManager: SqsStreamManager,
   private val connectionBuilder: SqsConnectionBuilder,

   ) :
   OperationInvoker {
   companion object {
      private val logger = KotlinLogging.logger {}

      init {
         SqsConnectorTaxi.registerConnectionUsage()
      }
   }

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      val hasCorrectAnnotations = service.hasMetadata(SqsConnectorTaxi.Annotations.SqsService.NAME) &&
         operation.hasMetadata(SqsConnectorTaxi.Annotations.SqsOperation.NAME)

      if (!hasCorrectAnnotations) return false

      return isSupportedReadOperation(operation) || isSupportedWriteOperation(operation)
   }

   private fun isSupportedWriteOperation(operation: RemoteOperation): Boolean {
      if (operation.operationType != OperationScope.MUTATION) return false

      val hasSingleParameter = operation.parameters.size == 1

      fun logAndFalse(message: String): Boolean {
         logger.warn { "Operation ${operation.qualifiedName.fullyQualifiedName} looks misconfigured - cannot build a consumer for this method for reason: ${message}" }
         return false
      }

      return when {
         !hasSingleParameter -> logAndFalse("Method should accept a single parameter")
         else -> true
      }
   }

   private fun isSupportedReadOperation(operation: RemoteOperation): Boolean {
      if (operation.operationType != OperationScope.READ_ONLY) return false

      val hasEmptyParams = operation.parameters.isEmpty()
      val isStreamOperation = isStreamDeclaration(operation)

      fun logAndFalse(message: String): Boolean {
         logger.warn { "Operation ${operation.qualifiedName.fullyQualifiedName} looks misconfigured - cannot build a consumer for this method for reason: ${message}" }
         return false
      }

      return when {
         !hasEmptyParams -> logAndFalse("Method should not accept parameters")
         !isStreamOperation -> logAndFalse("Method should return Stream<YourMessageType>")
         else -> true
      }
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val connectionName = service.firstMetadata(SqsConnectorTaxi.Annotations.SqsService.NAME)
         .params[SqsConnectorTaxi.Annotations.SqsService.ConnectionNameParam] as String
      val sqsOperation = operation.firstMetadata(SqsConnectorTaxi.Annotations.SqsOperation.NAME)
         .let { SqsConnectorTaxi.Annotations.SqsOperation.from(it) }

      val traceContext = eventDispatcher.createOperationTraceSpan(service, operation, sqsOperation.queue)

      return if (operation.operationType == OperationScope.MUTATION) {
         publishToTopic(connectionName, sqsOperation, parameters, eventDispatcher, traceContext)
      } else {
         subscribeToTopic(connectionName, sqsOperation, operation, traceContext)
      }


   }

   private fun publishToTopic(
      connectionName: String,
      sqsOperation: SqsConnectorTaxi.Annotations.SqsOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      traceContext: OperationTraceSpan
   ): Flow<Either<StreamErrorMessage, TypedInstance>>  {
      val publisher = connectionBuilder.buildPublisher(connectionName, sqsOperation.queue)
      require(parameters.size == 1) { "When publishing to SQS, exactly one parameter (the message to publish) is required" }
      val messageBody = parameters.single().second

      require(eventDispatcher is QueryContextSchemaProvider) {
         "Provided eventDispatcher must implement QueryContextSchemaProvider. Got ${eventDispatcher::class.simpleName}"
      }

      val messageBodyStr = messageBody.toRawObject().toString()
      traceContext.emitEvent(
         kind = TracingEventKind.OK,
         spanState = SpanState.ACTIVE,
         payloadType = messageBody.type,
         exchangeMetadata = MessageStreamEventReceived(
            messageBodyStr.length.toLong(),
            PayloadEncoding.STRING
         ) { messageBodyStr },
         verb = "Publish",
         direction = TraceEventDirection.OUTBOUND
      )

      return publisher.sendMessage(messageBody, eventDispatcher.schema)
         .map { result ->
            traceContext.emitEvent(
               TracingEventKind.OK,
               SpanState.COMPLETE,
               messageBody.type,
               MessageStreamEventReceived(
                  messageBodyStr.length.toLong(),
                  PayloadEncoding.STRING
               ) { "Message published successfully" },
               "Publish response",
               TraceEventDirection.INBOUND
            )
            result
         }
         .asFlow()
   }

   private fun subscribeToTopic(
      connectionName: String,
      sqsOperation: SqsConnectorTaxi.Annotations.SqsOperation,
      operation: RemoteOperation,
      traceContext: OperationTraceSpan
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      traceContext.emitEvent(
         kind = TracingEventKind.OK,
         spanState = SpanState.ACTIVE,
         payloadType = operation.returnType,
         exchangeMetadata = MessageStreamSubscription(
            sqsOperation.queue,
            connectionName,
            MessageStreamSubscription.SubscriptionAction.JOINED_EXISTING_SUBSCRIPTION
         ),
         verb = "Subscribe",
         TraceEventDirection.OUTBOUND
      )

      return sqsStreamManager.getStream(
         SqsConsumerRequest(
            connectionName,
            sqsOperation.queue,
            operation.returnType.qualifiedName
         )
      )
   }

   private fun isStreamDeclaration(operation: RemoteOperation): Boolean {
      val retType = schemaProvider.schema.type(operation.returnType.qualifiedName)
      return retType.isStream
   }
}
