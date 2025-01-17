package com.orbitalhq.connectors.aws.s3

import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.query.ObjectStoreExchange
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.schemas.OperationInvocationException
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import software.amazon.awssdk.services.s3.model.S3Object
import java.time.Duration
import java.time.Instant

/**
 * Invoker for s3 that reads directly from files.
 * Does not support querying
 */
class S3ReadInvoker : BaseS3Invoker() {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions,
      awsConnection: AwsConnectionConfiguration,
      bucketName: String,
      formatRegistry: FormatRegistry,
      schema: Schema
   ): Flow<TypedInstance> {
      val filePattern = getFilenamePattern(parameters)
      val startTime = Instant.now()

      return S3Connection(awsConnection, bucketName)
         .fetchAsInputStream(filePattern)
         .onErrorMap { error ->
            logger.warn { "Failed to read bucket $bucketName on connection ${awsConnection.connectionName} - a ${error::class.simpleName} was thrown - ${error.message}" }
            val operationResult = buildOperationResult(
               service,
               operation,
               parameters.map { it.second },
               Duration.between(startTime, Instant.now()),
               awsConnection.connectionName,
               bucketName,
               s3Object = null,
               errorMessage = error.message
            )
            eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
            OperationInvocationException(
               "Failed to read from bucket $bucketName - ${error.message ?: "No message in instance of ${error::class.simpleName}"}",
               0,
               operationResult.remoteCall,
               parameters
            )
         }
         .flatMap { fileNameAndInputStream ->
            val (s3Object, deferredInputStream) = fileNameAndInputStream
            deferredInputStream
               .doOnSubscribe {
                  logger.info { "Fetching object ${s3Object.key()} (${FileUtils.byteCountToDisplaySize(s3Object.size())}) from bucket $bucketName on connection ${awsConnection.connectionName}" }
               }
               .map { inputStream -> s3Object to inputStream }
               .onErrorMap { error ->
                  logger.warn { "Failed to fetch object ${s3Object.key()} from bucket $bucketName on connection ${awsConnection.connectionName} - a ${error::class.simpleName} was thrown - ${error.message}" }
                  val operationResult = buildOperationResult(
                     service,
                     operation,
                     parameters.map { it.second },
                     Duration.between(startTime, Instant.now()),
                     awsConnection.connectionName,
                     bucketName,
                     s3Object = s3Object,
                     errorMessage = error.message
                  )
                  eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
                  OperationInvocationException(
                     "Failed to fetch objct ${s3Object.key()} from bucket $bucketName - ${error.message ?: "No message in instance of ${error::class.simpleName}"}",
                     0,
                     operationResult.remoteCall,
                     parameters
                  )
               }
         }

         .flatMap { (s3Object,inputStream) ->
            val operationResult = buildOperationResult(
               service,
               operation,
               parameters.map { it.second },
               Duration.between(startTime, Instant.now()),
               awsConnection.connectionName,
               bucketName,
               s3Object,
            )
            eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
            TypedInstance.forStream(
               operation.returnType,
               inputStream,
               schema,
               source = operationResult.asOperationReferenceDataSource(),
               formatRegistry = formatRegistry,
            )
         }
         .asFlow().flowOn(Dispatchers.IO)
   }


   private fun buildOperationResult(
      service: Service,
      operation: RemoteOperation,
      parameters: List<TypedInstance>,
      elapsed: Duration,
      address: String,
      bucketName: String,
      s3Object: S3Object? = null,
      errorMessage: String? = null
   ): OperationResult {

      val remoteCall = RemoteCall(
         service = service.name,
         address = address,
         operation = operation.name,
         responseTypeName = operation.returnType.name,
         requestBody = "Read from S3",
         durationMs = elapsed.toMillis(),
         timestamp = Instant.now(),
         responseMessageType = ResponseMessageType.FULL,
         isFailed = errorMessage != null,
         response = errorMessage, // We're only capturing errors
         exchange = ObjectStoreExchange(
            bucketName,
            s3Object?.key(),
            responseSize = s3Object?.size() ?: -1,
         )
      )
      return OperationResult.fromTypedInstances(
         parameters,
         remoteCall
      )
   }
}
