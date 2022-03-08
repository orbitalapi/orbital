package io.vyne.connectors.aws.s3

import io.vyne.connectors.aws.core.AwsConnectionConnectorConfiguration
import io.vyne.connectors.aws.core.region
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.models.TypedInstance
import io.vyne.models.csv.CsvAnnotationSpec
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schemaApi.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import mu.KotlinLogging
import org.apache.commons.csv.CSVRecord
import java.util.stream.Stream
import kotlin.streams.asSequence

private val logger = KotlinLogging.logger {  }
class S3Invoker(
   private val connectionRegistry: AwsConnectionRegistry,
   private val schemaProvider: SchemaProvider,
   private val dispatcher: CoroutineDispatcher = Dispatchers.IO): OperationInvoker {
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      val hasParamMatch = operation.parameters.isEmpty() ||
         (operation.parameters.size == 1 && operation.parameters[0].type.fullyQualifiedName == S3ConnectorTaxi.S3EntryKeyTypeFullyQualifiedName.fullyQualifiedName)
      return service.hasMetadata(S3ConnectorTaxi.Annotations.S3Service.NAME) &&
         operation.hasMetadata(S3ConnectorTaxi.Annotations.S3Operation.NAME) &&
         hasParamMatch &&
         streamReturnType(operation) != null
   }

   private fun streamReturnType(operation: RemoteOperation): Type? {
      val retType = schemaProvider.schema().type(operation.returnType.qualifiedName)
      return if (retType.name.name == "Stream") {
         retType.typeParameters[0]
      } else null
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String?): Flow<TypedInstance> {

      val awsConnection = fetchConnection(service)
      val bucketName = fetchBucket(operation)
      if (parameters.isNotEmpty()) {
         parameters.first()
      }

      val s3ObjectKey = tryExtractS3ObjectKey(parameters)
      val s3ConnectionConfiguration = AwsS3ConnectionConnectorConfiguration.fromAwsConnectionConfiguration(awsConnection, bucketName)
      logger.info { "AWS connection ${awsConnection.connectionName} with region ${awsConnection.region} found in configurations, using to access bucket $bucketName to fetch $s3ObjectKey" }
      val messageType = streamReturnType(operation)!!
      return fetchFromS3(s3ConnectionConfiguration, messageType, s3ObjectKey)
   }


   private fun fetchFromS3(s3connectionConfig: AwsS3ConnectionConnectorConfiguration, messageType: Type, s3ObjectKey: String?): Flow<TypedInstance> {
      val schema = schemaProvider.schema()
      return messageType
         .metadata.firstOrNull { metadata -> metadata.name == CsvAnnotationSpec.NAME }?.let {
            S3Connection(s3connectionConfig)
               .fetchAsCsv(s3ObjectKey).flatMap { messageValue ->
                  messageValue.records.map { csvRecord ->
                     TypedInstance.from(
                        messageType,
                        csvRecord,
                        schema
                     )
                  }.stream()
               }.asSequence().asFlow().flowOn(dispatcher)
         }
         ?:  S3Connection(s3connectionConfig)
           .fetch(s3ObjectKey).map { messageValue ->
            TypedInstance.from(
              type =  messageType,
              value =  messageValue,
              schema =  schema,
               formatSpecs = listOf(CsvFormatSpec)
            )
     }.asSequence().asFlow().flowOn(dispatcher)

   }

   private fun tryExtractS3ObjectKey(parameters: List<Pair<Parameter, TypedInstance>>): String? {
      return if (parameters.isNotEmpty()) {
         parameters[0].second.value?.toString()
      } else {
         null
      }
   }

   private fun fetchConnection(service: Service): AwsConnectionConnectorConfiguration {
      val connectionName = service.metadata(S3ConnectorTaxi.Annotations.S3Service.NAME).params["connectionName"] as String
      val awsConnectionConfiguration = connectionRegistry.getConnection(connectionName)
      logger.info { "AWS connection ${awsConnectionConfiguration.connectionName} with region ${awsConnectionConfiguration.region} found in configurations" }
      return awsConnectionConfiguration
   }

   private fun fetchBucket(operation: RemoteOperation): String {
      return operation.metadata(S3ConnectorTaxi.Annotations.S3Operation.NAME).params[S3ConnectorTaxi.Annotations.S3Operation.bucketMetadataName] as String

   }
}
