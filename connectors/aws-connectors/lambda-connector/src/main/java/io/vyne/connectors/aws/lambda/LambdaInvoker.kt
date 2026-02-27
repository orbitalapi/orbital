package com.orbitalhq.connectors.aws.lambda

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.connectors.aws.configureWithExplicitValuesIfProvided
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.EmptyExchangeData
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.HttpRequest
import com.orbitalhq.query.tracing.HttpResponse
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.query.tracing.FunctionCallRequest
import com.orbitalhq.query.tracing.FunctionCallResponse
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.OperationInvocationException
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
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
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaAsyncClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import java.net.URI
import java.time.Instant
import java.util.UUID


private val logger = KotlinLogging.logger { }

class LambdaInvoker(
   private val connectionRegistry: AwsConnectionRegistry,
   private val schemaProvider: SchemaProvider,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : OperationInvoker {
   companion object {
      init {
          LambdaConnectorTaxi.registerConnectorUsage()
      }
   }

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(LambdaConnectorTaxi.Annotations.LambdaInvocationService.NAME) &&
         operation.hasMetadata(LambdaConnectorTaxi.Annotations.LambdaOperation.NAME)
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val awsConnection = fetchConnection(service)
      return invokeAwsLambda(awsConnection, parameters, service, operation, eventDispatcher, queryId)
   }

   private fun fetchConnection(service: Service): AwsConnectionConfiguration {
      val connectionName =
         service.firstMetadata(LambdaConnectorTaxi.Annotations.LambdaInvocationService.NAME).params["connectionName"] as String
      val awsConnectionConfiguration = connectionRegistry.getConnection(connectionName)
      logger.info { "AWS connection ${awsConnectionConfiguration.connectionName} with region ${awsConnectionConfiguration.region} found in configurations" }
      return awsConnectionConfiguration
   }

   private fun fetchFunctionName(operation: RemoteOperation): String {
      return operation.firstMetadata(LambdaConnectorTaxi.Annotations.LambdaOperation.NAME)
         .params[LambdaConnectorTaxi.Annotations.LambdaOperation.operationMetadataName] as String

   }

   private fun invokeAwsLambda(
      connection: AwsConnectionConfiguration,
      parameters: List<Pair<Parameter, TypedInstance>>,
      service: Service,
      operation: RemoteOperation,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val functionName = fetchFunctionName(operation)
      val argument = if (parameters.size == 1) {
         parameters.first().second.toRawObject()
      } else null

      val client = createAsyncLambdaClient(connection)
      val payload = argument?.let { objectMapper.writeValueAsString(it) } ?: "{}"
      val remoteCallId = UUID.randomUUID().toString()
      val traceContext = eventDispatcher.createOperationTraceSpan(service, operation, functionName, remoteCallId = remoteCallId)

      val invokeRequest: InvokeRequest = InvokeRequest.builder()
         .functionName(functionName)
         .payload(SdkBytes.fromUtf8String(payload))
         .build()
      logger.info { "Invoking lambda function $functionName with arguments $payload" }

      traceContext.emitEvent(
         kind = TracingEventKind.OK,
         spanState = SpanState.ACTIVE,
         payloadType = parameters.firstOrNull()?.first?.type,
         exchangeMetadata = FunctionCallRequest(functionName) { payload },
         verb = "Invoke",
         direction = TraceEventDirection.OUTBOUND,
      )

      return Mono.fromFuture(client.invoke(invokeRequest))
         .metrics()
         .elapsed()
         .publishOn(Schedulers.boundedElastic())
         .doOnTerminate {
            try {
               logger.info { "Closing Aws Lambda Client." }
               client.close()
            } catch (e: Exception) {
               logger.error(e) { "Error in closing lambda client" }
            }
         }
         .flatMapMany { durationAndResponse ->
            val duration = durationAndResponse.t1
            val initiationTime = Instant.now().minusMillis(duration)
            fun remoteCall(responseBody: String, failed: Boolean = false): RemoteCall {
               return RemoteCall(
                  remoteCallId = remoteCallId,
                  responseId = UUID.randomUUID().toString(),
                  service = service.name,
                  address = functionName,
                  operation = operation.name,
                  responseTypeName = operation.returnType.name,
                  requestBody = payload,
                  durationMs = duration,
                  response = responseBody,
                  timestamp = initiationTime,
                  responseMessageType = ResponseMessageType.FULL,
                  isFailed = failed,
                  exchange = EmptyExchangeData
               )
            }

            val clientResponse = durationAndResponse.t2
            if (clientResponse.functionError() != null) {
               val errorMessage = clientResponse.functionError() ?: ""
               traceContext.emitEvent(
                  TracingEventKind.ERROR,
                  SpanState.COMPLETE,
                  null,
                  FunctionCallResponse(0, errorMessage, clientResponse.statusCode(), { null }),
                  "Invoke error",
                  TraceEventDirection.INBOUND
               )
               val remoteCall = remoteCall(errorMessage, true)
               eventDispatcher.reportRemoteOperationInvoked(OperationResult.from(parameters, remoteCall), queryId)
               throw OperationInvocationException(
                  "Aws lambda invocation error ${clientResponse.functionError()} from function $functionName",
                  0,
                  remoteCall,
                  parameters
               )
            }

            // UTF_8 won't be enough for all cases.
            val response = clientResponse.payload().asUtf8String()
            val resultEvent = traceContext.emitEvent(
               TracingEventKind.OK,
               SpanState.COMPLETE,
               operation.returnType,
               FunctionCallResponse(response.length.toLong(),null,clientResponse.statusCode()) { response },
               "Invoke response",
               TraceEventDirection.INBOUND
            )
            val remoteCall = remoteCall(responseBody = response)
            val operationResult = OperationResult.from(parameters, remoteCall)
            eventDispatcher.reportRemoteOperationInvoked(OperationResult.from(parameters, remoteCall), queryId)
            handleSuccessfulLambdaResponse(response, operation, operationResult, resultEvent)
         }.asFlow().flowOn(dispatcher)
   }

   private fun createAsyncLambdaClient(connection: AwsConnectionConfiguration): LambdaAsyncClient {
      val clientBuilder = LambdaAsyncClient.builder()
         .configureWithExplicitValuesIfProvided(connection)

      connection.endPointOverride?.let {
         clientBuilder.endpointOverride(URI.create(it))
      }

      return clientBuilder.build()
   }

   private fun handleSuccessfulLambdaResponse(
      result: String,
      operation: RemoteOperation,
      operationResult: OperationResult,
      resultEvent: com.orbitalhq.query.tracing.TracingEvent
   ): Flux<Either<StreamErrorMessage, TypedInstance>> {
      logger.debug { "Result of ${operation.name} was $result" }


      val type = operation.returnType.collectionType ?: operation.returnType


      val typedInstanceOrError =
         try {
            Either.Right(TypedInstance.from(
               type,
               result,
               schemaProvider.schema,
               source = operationResult.asOperationReferenceDataSource(resultEvent.idSet),
               evaluateAccessors = true
            ))
         }
         catch (e: Exception) {
            Either.Left(StreamErrorMessage.fromException(e, type.paramaterizedName))
         }

      return when (typedInstanceOrError) {
         is Either.Left -> Flux.fromIterable(listOf(Either.Left(typedInstanceOrError.value)))
         is Either.Right -> if (typedInstanceOrError.value is TypedCollection) {
            Flux.fromIterable((typedInstanceOrError.value as TypedCollection).value).map { typedInstance -> Either.Right(typedInstance) }
         } else {
            Flux.fromIterable(listOf(Either.Right(typedInstanceOrError.value)))
         }
      }

   }
}
