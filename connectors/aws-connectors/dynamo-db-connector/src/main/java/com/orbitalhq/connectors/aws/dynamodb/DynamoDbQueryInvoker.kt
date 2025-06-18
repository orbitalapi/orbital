package com.orbitalhq.connectors.aws.dynamodb

import arrow.core.Either
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.DatabaseRequest
import com.orbitalhq.query.tracing.DatabaseResponse
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TracingEventKind
import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.query.tracing.OperationTraceSpan
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.function.Tuple2
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException
import software.amazon.awssdk.services.dynamodb.model.DynamoDbResponse
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.QueryResponse
import software.amazon.awssdk.services.dynamodb.model.ScanResponse
import java.math.BigDecimal

class DynamoDbQueryInvoker(
   private val connectionRegistry: AwsConnectionRegistry,
   private val schemaProvider: SchemaProvider,
   private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseDynamoInvoker(connectionRegistry) {
   private val objectMapper = ObjectMapper()
   private val queryBuilder = DynamoDbRequestBuilder()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val schema = schemaProvider.schema
      val (taxiQuery, constructedQueryDataSource) = parameters[0].second.let { it.value as String to it.source as ConstructedQueryDataSource }
      val query = queryBuilder.buildQuery(schema, taxiQuery)
      val (client, awsConfig) = buildClient(service, operation)
      val traceContext = eventDispatcher.createOperationTraceSpan(service, operation, "")

      return Mono.fromFuture(executeRequest(query, client))
         .doOnError { e ->
            val errorCode = when (e) {
               is DynamoDbException -> e.statusCode()
               else -> 400
            }
            val message = "Call to Dynamo failed: ${e.message ?: e.toString()}"
            traceContext.emitEvent(
               TracingEventKind.ERROR,
               SpanState.COMPLETE,
               null,
               DatabaseResponse(-1) { message },
               "Query error"
            )
            val remoteCall = buildRemoteCall(service, awsConfig, operation, query, -1, -1, errorCode, message)
            val operationResult = OperationResult.fromTypedInstances(constructedQueryDataSource.inputs, remoteCall)
            eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
            logger.error(e) { message }
         }
         .publishOn(Schedulers.boundedElastic())
         .doOnTerminate {
            try {
               logger.info { "Closing Aws Lambda Client." }
               client.close()
            } catch (e: Exception) {
               logger.error(e) { "Error in closing lambda client" }
            }
         }
         .doOnSubscribe {
            logger.info { "Sending request to Dynamo $query" }
            traceContext.emitEvent(
               kind = TracingEventKind.OK,
               spanState = SpanState.ACTIVE,
               payloadType = null,
               exchangeMetadata = DatabaseRequest(awsConfig.connectionName, "Query", "") { query.toString() },
               verb = "Query"
            )
         }
         .elapsed()
         .flatMapMany { responsePair ->
            val duration = responsePair.t1
            logger.info { "DynamoDb call completed in ${duration}ms for request $query" }
            val response = responsePair.t2
            val count = response.count()
            val remoteCall = buildRemoteCall(service, awsConfig, operation, query, duration, count)
            val operationResult = OperationResult.fromTypedInstances(constructedQueryDataSource.inputs, remoteCall)
            eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
            val items = safeParse(responsePair, operation, eventDispatcher, queryId, schema, operationResult, traceContext)
            Flux.fromIterable(items)
         }.asFlow().flowOn(dispatcher)


   }

   fun safeParse(
      responsePair: Tuple2<Long, DynamoDbResponse>,
      operation: RemoteOperation,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      schema: Schema,
      operationResult: OperationResult,
      traceContext: OperationTraceSpan
   ): List<Either<StreamErrorMessage, TypedInstance>> {
      val duration = responsePair.t1
     // logger.info { "DynamoDb call completed in ${duration}ms for request $query" }
      val response = responsePair.t2
      eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
      return when (response) {
         is GetItemResponse -> {
            val resultEvent = traceContext.emitEvent(
               TracingEventKind.OK,
               SpanState.COMPLETE,
               operation.returnType,
               DatabaseResponse(if (response.hasItem()) 1L else 0L) {
                  // Outputs something like:
                  // GetItemResponse(Item={score=AttributeValue(N=3), movieId=AttributeValue(N=1)})
                  // Which is probably fine
                  response.toString()
               },
               "GetItem response"
            )
            listOf(
               readItem(
                  response,
                  operation.returnType,
                  schema,
                  operationResult.asOperationReferenceDataSource(resultEvent.idSet)
               )
            )
         }

         is QueryResponse -> {
            val resultEvent = traceContext.emitEvent(
               TracingEventKind.OK,
               SpanState.COMPLETE,
               operation.returnType,
               DatabaseResponse(response.count().toLong()) {
                  response.toString()
               },
               "Query response"
            )
            readItems(
               response.items(),
               operation.returnType,
               schema,
               operationResult.asOperationReferenceDataSource(resultEvent.idSet)
            )
         }

         is ScanResponse -> {
            val resultEvent = traceContext.emitEvent(
               TracingEventKind.OK,
               SpanState.COMPLETE,
               operation.returnType,
               DatabaseResponse(response.count().toLong()) {
                  response.toString()
               },
               "Scan response"
            )
            readItems(
               response.items(),
               operation.returnType,
               schema,
               operationResult.asOperationReferenceDataSource(resultEvent.idSet)
            )
         }

         else -> error("Not implemented - Response type of ${response::class.simpleName}")
      }

   }

   private fun readItems(
      items: List<Map<String, AttributeValue>>,
      returnType: Type,
      schema: Schema,
      dataSource: DataSource
   ): List<Either<StreamErrorMessage,  TypedInstance>> {
      return items.map {
         try {
          Either.Right(convertToTypedInstance(it, returnType, schema, dataSource))
         } catch (e: Exception) {
            Either.Left(StreamErrorMessage.fromException(e, returnType.paramaterizedName))
         }
      }
   }


   private fun DynamoDbResponse.count(): Int {
      return when (this) {
         is GetItemResponse -> if (this.hasItem()) 1 else 0
         is QueryResponse -> this.count()
         is ScanResponse -> this.count()
         else -> error("Not implemented - record count for response type ${this::class.simpleName}")
      }
   }

   private fun readItem(
      response: GetItemResponse,
      returnType: Type,
      schema: Schema,
      dataSource: DataSource
   ): Either<StreamErrorMessage, TypedInstance> {
      if (!response.hasItem()) {
         return Either.Right(TypedNull.create(returnType, dataSource))
      }
      return try {
         Either.Right(convertToTypedInstance(response.item(), returnType, schema, dataSource))
      } catch (e: Exception) {
         Either.Left(StreamErrorMessage.fromException(e, returnType.paramaterizedName))
      }
   }

   private fun
           convertToTypedInstance(
      item: Map<String, AttributeValue>,
      returnType: Type,
      schema: Schema,
      dataSource: DataSource
   ): TypedInstance {
      val itemValues: Map<String, Any?> = item.map { (key, value) ->
         key to unwrapAttributeValue(value)
      }.toMap()
      val memberType = returnType.collectionType ?: returnType
      return TypedInstance.from(memberType, itemValues, schema, source = dataSource)
   }

   private fun unwrapAttributeValue(value: AttributeValue):Any? = when (value.type()) {
      AttributeValue.Type.N -> BigDecimal(value.n())
      AttributeValue.Type.BOOL -> value.bool()
      AttributeValue.Type.S -> value.s()
      AttributeValue.Type.SS -> value.ss()
      AttributeValue.Type.NS -> value.ns()
      AttributeValue.Type.L -> value.l().map { unwrapAttributeValue(it) }
      AttributeValue.Type.M -> value.m().entries.map { (key,value) ->
         key to unwrapAttributeValue(value)
      }.toMap()
      AttributeValue.Type.NUL -> null
      else -> error("Parsing not implemented for type ${value.type().name}")
   }

}
