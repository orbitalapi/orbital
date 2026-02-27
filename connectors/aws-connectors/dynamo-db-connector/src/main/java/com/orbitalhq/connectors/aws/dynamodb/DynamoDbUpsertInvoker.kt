package com.orbitalhq.connectors.aws.dynamodb

import arrow.core.Either
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.DatabaseRequest
import com.orbitalhq.query.tracing.DatabaseResponse
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TracingEventKind
import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID

class DynamoDbUpsertInvoker(
   private val connectionRegistry: AwsConnectionRegistry,
   private val schemaProvider: SchemaProvider,
   private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseDynamoInvoker(connectionRegistry) {
   private val objectMapper = ObjectMapper()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val queryBuilder = DynamoDbRequestBuilder()

   fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val recordToWrite = parameters[0].second
      val request = queryBuilder.buildPut(schemaProvider.schema, recordToWrite)

      val (client, awsConfig) = buildClient(service, operation)
      val remoteCallId = UUID.randomUUID().toString()
      val traceContext = eventDispatcher.createOperationTraceSpan(service, operation, "", remoteCallId = remoteCallId)

      return Mono.fromFuture(executeRequest(request, client))
         .doOnSubscribe {
            traceContext.emitEvent(
               kind = TracingEventKind.OK,
               spanState = SpanState.ACTIVE,
               payloadType = operation.parameters.firstOrNull()?.type,
               exchangeMetadata = DatabaseRequest(awsConfig.connectionName, "Put", "") { request.toString() },
               verb = "Upsert",
               direction = TraceEventDirection.OUTBOUND
            )
         }
         .doOnError { e ->
            val message = "Call to Dynamo failed: ${e.message ?: e.toString()}"
            traceContext.emitEvent(
               TracingEventKind.ERROR,
               SpanState.COMPLETE,
               null,
               DatabaseResponse(-1) { message },
               "Upsert error",
               direction = TraceEventDirection.INBOUND
            )
         }
         .publishOn(Schedulers.boundedElastic())
         .elapsed()
         .flatMapMany<Either<StreamErrorMessage, TypedInstance>> { responsePair ->
            val duration = responsePair.t1
            logger.info { "DynamoDb call completed in ${duration}ms for request $request" }
            val response = responsePair.t2
            val count = 1
            val resultEvent = traceContext.emitEvent(
               TracingEventKind.OK,
               SpanState.COMPLETE,
               operation.returnType,
               DatabaseResponse(count.toLong()) {
                  // Not sure what else to return here, but the PutItemResponse is pretty sparse in what we get back
                  response.toString()
               },
               "Upsert response",
               direction = TraceEventDirection.INBOUND
            )
            val remoteCall = buildRemoteCall(service, awsConfig, operation, request, duration, count, parameterPairs = parameters)
            val operationResult = OperationResult.fromTypedInstances(parameters.map { it.second }, remoteCall)
            eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
            val writeResultValue: Either<StreamErrorMessage, TypedInstance> = Either.Right(
               DataSourceUpdater.update(
                  recordToWrite,
                  operationResult.asOperationReferenceDataSource(resultEvent.idSet)
               )
            )
            Flux.fromIterable(listOf(writeResultValue))
         }.asFlow().flowOn(dispatcher)
   }
}
