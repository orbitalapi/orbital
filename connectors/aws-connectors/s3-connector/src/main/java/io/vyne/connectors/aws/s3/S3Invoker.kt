package io.vyne.connectors.aws.s3

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import io.vyne.connectors.TaxiQlToSqlConverter
import io.vyne.connectors.aws.core.AwsConnectionConnectorConfiguration
import io.vyne.connectors.aws.core.region
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.connectors.calcite.VyneCalciteDataSource
import io.vyne.connectors.convertToTypedInstances
import io.vyne.connectors.resultType
import io.vyne.models.DataSource
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.models.csv.CsvAnnotationSpec
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.csv.CsvFormatSpecAnnotation
import io.vyne.models.format.FormatDetector
import io.vyne.models.json.Jackson
import io.vyne.query.ConstructedQueryDataSource
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.RemoteCall
import io.vyne.query.ResponseMessageType
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.schemas.toVyneQualifiedName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import lang.taxi.Compiler
import mu.KotlinLogging
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

private val logger = KotlinLogging.logger {  }
class S3Invoker(
    private val connectionRegistry: AwsConnectionRegistry,
    private val schemaProvider: SchemaProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper): OperationInvoker {
   private val formatDetector = FormatDetector.get(listOf(CsvFormatSpec))
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(S3ConnectorTaxi.Annotations.S3Service.NAME) &&
         operation.hasMetadata(S3ConnectorTaxi.Annotations.S3Operation.NAME)
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String?): Flow<TypedInstance> {
      val schema = schemaProvider.schema
      val awsConnection = fetchConnection(service)
      val bucketName = fetchBucket(operation)

      val s3ConnectionConfiguration = AwsS3ConnectionConnectorConfiguration.fromAwsConnectionConfiguration(awsConnection, bucketName)
      logger.info { "AWS connection ${awsConnection.connectionName} with region ${awsConnection.region} found in configurations, using to access bucket $bucketName" }
      val taxiSchema = schema.taxi
      val (taxiQuery, constructedQueryDataSource) = parameters[0].second.let { it.value as String to it.source as ConstructedQueryDataSource }
      val query = Compiler(taxiQuery, importSources = listOf(taxiSchema)).queries().first()
      val (sql, paramList) = TaxiQlToSqlConverter(taxiSchema, quoteColumns = true).toSql(query) { type -> type.toQualifiedName().typeName.toUpperCase()}
      val paramMap = paramList.associate { param -> param.nameUsedInTemplate to param.value }
      val resultTypeQualifiedName = query.resultType()
      val resultType = schema.type(resultTypeQualifiedName.toVyneQualifiedName())
      val parametrisedType =  resultType.collectionType ?: resultType
      val dataSource = VyneCalciteDataSource(
         schema,
         resultTypeQualifiedName.toVyneQualifiedName(),
         fetchAsStream(s3ConnectionConfiguration, parametrisedType, null)
      )

      val stopwatch = Stopwatch.createStarted()
      val result = NamedParameterJdbcTemplate(dataSource).queryForList(sql, paramMap)
      val elapsed = stopwatch.elapsed()
      val datasource = buildDataSource(
         service,
         operation,
         constructedQueryDataSource.inputs,
         sql,
         s3ConnectionConfiguration.connectionName,
         elapsed
      )
      return result.convertToTypedInstances(schema, datasource, resultTypeQualifiedName, dispatcher)
   }

   private fun buildDataSource(
      service: Service,
      operation: RemoteOperation,
      parameters: List<TypedInstance>,
      sql: String,
      jdbcUrl: String,
      elapsed: Duration,
   ): DataSource {

      val remoteCall = RemoteCall(
         service = service.name,
         address = jdbcUrl,
         operation = operation.name,
         responseTypeName = operation.returnType.name,
         method = "SELECT",
         requestBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
            mapOf("sql" to sql)
         ),
         resultCode = 200, // Using HTTP status codes here, because not sure what else to use.
         durationMs = elapsed.toMillis(),
         timestamp = Instant.now(),
         // If we implement streaming database queries, this will change
         responseMessageType = ResponseMessageType.FULL,
         // Feels like capturing the results are a bad idea.  Can revisit if there's a use-case
         response = "Not captured"
      )
      return OperationResult.fromTypedInstances(
         parameters,
         remoteCall
      )
   }

   private fun fetchAsStream(s3connectionConfig: AwsS3ConnectionConnectorConfiguration, messageType: Type, s3ObjectKey: String?): Stream<TypedInstance> {
      val schema = schemaProvider.schema
      return messageType
         .metadata.firstOrNull { metadata -> metadata.name == CsvAnnotationSpec.NAME }?.let {
            val csvModelFormatAnnotation =  formatDetector.getFormatType(messageType)?.let { if (it.second is CsvFormatSpec) CsvFormatSpecAnnotation.from(it.first) else null  }
            S3Connection(s3connectionConfig)
               .fetchAsCsv(s3ObjectKey, csvModelFormatAnnotation!!).flatMap { messageValue ->
                  messageValue.records.map { csvRecord ->
                     TypedInstance.from(
                        messageType,
                        csvRecord,
                        schema
                     )
                  }.stream()
               }
         }
         ?:  S3Connection(s3connectionConfig)
            .fetch(s3ObjectKey).map { messageValue ->
               TypedInstance.from(
                  type =  messageType,
                  value =  messageValue,
                  schema =  schema,
                  formatSpecs = listOf(CsvFormatSpec)
               )
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
