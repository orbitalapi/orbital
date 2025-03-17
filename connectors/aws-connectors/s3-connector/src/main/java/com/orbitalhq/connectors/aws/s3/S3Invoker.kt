package com.orbitalhq.connectors.aws.s3

import arrow.core.Either
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.formats.csv.CsvFormatSpec
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.FormatDetector
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import lang.taxi.services.OperationScope
import mu.KotlinLogging

class S3Invoker(
   private val connectionRegistry: AwsConnectionRegistry,
   private val schemaProvider: SchemaProvider,
   private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
   private val formatRegistry: FormatRegistry
) : OperationInvoker {
   private val formatDetector = FormatDetector.get(listOf(CsvFormatSpec))
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(S3ConnectorTaxi.Annotations.S3Service.NAME) &&
         operation.hasMetadata(S3ConnectorTaxi.Annotations.S3Operation.NAME)
   }

   private val mutatingInvoker = S3WriteInvoker()
   private val queryInvoker = S3QueryInvoker()
   private val readInvoker = S3ReadInvoker()

   companion object {
      private val logger = KotlinLogging.logger {}

      init {
         S3ConnectorTaxi.registerConnectionUsage()
      }
   }

   data class S3BucketConfig(
      val connection: AwsConnectionConfiguration,
      val bucketName: String
   )

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {

      val awsConnection = fetchConnection(service)
      val bucketName = fetchBucket(operation)

      return when {
         operation.operationType == OperationScope.READ_ONLY -> {
            readInvoker.invoke(
               service,
               operation,
               parameters,
               eventDispatcher,
               queryId,
               queryOptions,
               awsConnection,
               bucketName,
               formatRegistry = formatRegistry,
               schema = schemaProvider.schema
            )
         }
         operation.operationType == OperationScope.MUTATION -> {
            mutatingInvoker.invoke(
               service,
               operation,
               parameters,
               eventDispatcher,
               queryId,
               queryOptions,
               awsConnection,
               bucketName,
               formatRegistry = formatRegistry,
               schema = schemaProvider.schema
            )
         }
         else -> TODO("This type of S3 operation is not yet supported")
      }
   }

   private fun fetchConnection(service: Service): AwsConnectionConfiguration {
      val connectionName =
         service.firstMetadata(S3ConnectorTaxi.Annotations.S3Service.NAME).params["connectionName"] as String
      val awsConnectionConfiguration = connectionRegistry.getConnection(connectionName)
      logger.info { "AWS connection ${awsConnectionConfiguration.connectionName} with region ${awsConnectionConfiguration.region} found in configurations" }
      return awsConnectionConfiguration
   }

   private fun fetchBucket(operation: RemoteOperation): String {
      return operation.firstMetadata(S3ConnectorTaxi.Annotations.S3Operation.NAME).params[S3ConnectorTaxi.Annotations.S3Operation.bucketMetadataName] as String

   }
}
