package io.vyne.connectors.aws.lambda

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.connectors.aws.core.AwsConnectionConnectorConfiguration
import io.vyne.connectors.aws.core.accessKey
import io.vyne.connectors.aws.core.endPointOverride
import io.vyne.connectors.aws.core.region
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.connectors.aws.core.secretKey
import io.vyne.models.OperationResult
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.json.Jackson
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.RemoteCall
import io.vyne.query.ResponseMessageType
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.OperationInvocationException
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaAsyncClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import java.net.URI
import java.time.Instant
import java.util.UUID


private val logger = KotlinLogging.logger { }

class LambdaInvoker(private val connectionRegistry: AwsConnectionRegistry,
                    private val schemaProvider: SchemaProvider,
                    private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
                    private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : OperationInvoker {
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(LambdaConnectorTaxi.Annotations.LambdaInvocationService.NAME) &&
         operation.hasMetadata(LambdaConnectorTaxi.Annotations.LambdaOperation.NAME)
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String?): Flow<TypedInstance> {
      val awsConnection = fetchConnection(service)
      return invokeAwsLambda(awsConnection, parameters, service, operation)
   }

   private fun fetchConnection(service: Service): AwsConnectionConnectorConfiguration {
      val connectionName = service.metadata(LambdaConnectorTaxi.Annotations.LambdaInvocationService.NAME).params["connectionName"] as String
      val awsConnectionConfiguration = connectionRegistry.getConnection(connectionName)
      logger.info { "AWS connection ${awsConnectionConfiguration.connectionName} with region ${awsConnectionConfiguration.region} found in configurations" }
      return awsConnectionConfiguration
   }

   private fun fetchFunctionName(operation: RemoteOperation): String {
      return operation.metadata(LambdaConnectorTaxi.Annotations.LambdaOperation.NAME)
         .params[LambdaConnectorTaxi.Annotations.LambdaOperation.operationMetadataName] as String

   }

   private fun invokeAwsLambda(
      connection: AwsConnectionConnectorConfiguration,
      parameters: List<Pair<Parameter, TypedInstance>>,
      service: Service,
      operation: RemoteOperation): Flow<TypedInstance> {
      val functionName = fetchFunctionName(operation)
      val argument = if (parameters.size == 1) {
         parameters.first().second.toRawObject()
      } else null

      val client = createAsyncLambdaClient(connection)
      val payload = argument?.let { objectMapper.writeValueAsString(it) } ?: "{}"

      val invokeRequest: InvokeRequest = InvokeRequest.builder()
         .functionName(functionName)
         .payload(SdkBytes.fromUtf8String(payload))
         .build()

      val remoteCallId = UUID.randomUUID().toString()
      logger.info { "Invoking lambda function $functionName with arguments $payload" }
      return Mono.fromFuture(client.invoke(invokeRequest))
         .metrics()
         .elapsed()
         .publishOn(Schedulers.boundedElastic())
         .doOnTerminate {
         try {
            logger.info { "Closing Aws Lambda Client." }
            client.close()
         } catch (e: Exception) {
            logger.error(e) {  "Error in closing lambda client"  }
         }}
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
                  method = "Aws Lambda",
                  requestBody = payload,
                  resultCode = 0,
                  durationMs = duration,
                  response = responseBody,
                  timestamp = initiationTime,
                  responseMessageType = ResponseMessageType.FULL,
                  isFailed = failed
               )
            }

            val clientResponse = durationAndResponse.t2
            if (clientResponse.functionError() != null) {
               throw OperationInvocationException(
                  "Aws lambda invocation error ${clientResponse.functionError()} from function $functionName",
                  0,
                  remoteCall(clientResponse.functionError() ?: "", true),
                  parameters
               )
            }

            // UTF_8 won't be enough for all cases.
            val response = clientResponse.payload().asUtf8String()
            val remoteCall = remoteCall(responseBody = response)
            handleSuccessfulLambdaResponse(response, operation, parameters, remoteCall)
         }.asFlow().flowOn(dispatcher)

   }

   private fun createAsyncLambdaClient(connection: AwsConnectionConnectorConfiguration): LambdaAsyncClient {
      val clientBuilder = LambdaAsyncClient.builder()
         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(connection.accessKey,
            connection.secretKey)))
         .region(Region.of(connection.region))


      connection.endPointOverride?.let {
         clientBuilder.endpointOverride(URI.create(it))
      }

      return clientBuilder.build()
   }

   private fun handleSuccessfulLambdaResponse(
      result: String,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      remoteCall: RemoteCall
   ): Flux<TypedInstance> {
      logger.debug { "Result of ${operation.name} was $result" }
      val dataSource = OperationResult.from(parameters, remoteCall)

      val type = operation.returnType.collectionType ?: operation.returnType

      val typedInstance = TypedInstance.from(
         type,
         result,
         schemaProvider.schema(),
         source = dataSource,
         evaluateAccessors = true
      )
      return if (typedInstance is TypedCollection) {
         Flux.fromIterable(typedInstance.value)
      } else {
         Flux.fromIterable(listOf(typedInstance))
      }
   }
}
