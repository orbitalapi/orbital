package com.orbitalhq.connectors.aws.s3

import com.orbitalhq.connectors.aws.s3.S3ConnectorTaxi.FilenamePatternFqn
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.query.EmptyExchangeData
import com.orbitalhq.query.ObjectStoreExchange
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import java.time.Duration
import java.time.Instant

/**
 * Invoker for s3 that reads directly from files.
 * Does not support querying
 */
class S3ReadInvoker : BaseS3Invoker() {
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
         .flatMap { inputStream ->
            val operationResult = buildOperationResult(
               service,
               operation,
               parameters.map { it.second },
               Duration.between(startTime, Instant.now()),
               awsConnection.connectionName,
               bucketName,
               filePattern
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
         .asFlow()
   }


   private fun buildOperationResult(
      service: Service,
      operation: RemoteOperation,
      parameters: List<TypedInstance>,
      elapsed: Duration,
      address: String,
      bucketName: String,
      filePattern: String
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
         response = null,
         exchange = ObjectStoreExchange(
            bucketName,
            filePattern
         )
      )
      return OperationResult.fromTypedInstances(
         parameters,
         remoteCall
      )
   }
}
