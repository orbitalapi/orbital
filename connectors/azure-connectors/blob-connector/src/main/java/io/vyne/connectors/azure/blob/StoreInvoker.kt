package io.vyne.connectors.azure.blob

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import io.vyne.connectors.TaxiQlToSqlConverter
import io.vyne.connectors.azure.blob.registry.AzureStorageConnectorConfiguration
import io.vyne.connectors.azure.blob.registry.AzureStoreConnectionRegistry
import io.vyne.connectors.calcite.VyneCalciteDataSource
import io.vyne.connectors.convertToTypedInstances
import io.vyne.connectors.resultType
import io.vyne.models.DataSource
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.models.json.Jackson
import io.vyne.query.ConstructedQueryDataSource
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.RemoteCall
import io.vyne.query.ResponseMessageType
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schemaApi.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import io.vyne.schemas.toVyneQualifiedName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import lang.taxi.Compiler
import mu.KotlinLogging
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {  }
class StoreInvoker(
   private val streamProvider: StreamProvider,
   private val connectionRegistry: AzureStoreConnectionRegistry,
   private val schemaProvider: SchemaProvider,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
): OperationInvoker {
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(AzureStoreConnectionTaxi.Annotations.AzureStoreService.NAME) &&
         operation.hasMetadata(AzureStoreConnectionTaxi.Annotations.StoreOperation.NAME)
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher, queryId: String?): Flow<TypedInstance> {
      val schema = schemaProvider.schema()
      val taxiSchema = schema.taxi
      val (taxiQuery, constructedQueryDataSource) = parameters[0].second.let { it.value as String to it.source as ConstructedQueryDataSource }
      val query = Compiler(taxiQuery, importSources = listOf(taxiSchema)).queries().first()
      val (sql, paramList) = TaxiQlToSqlConverter(taxiSchema, quoteColumns = true).toSql(query) {type -> type.toQualifiedName().typeName.toUpperCase()}
      val paramMap = paramList.associate { param -> param.nameUsedInTemplate to param.value }
      val azureStoreConnection = fetchConnection(service)
      val resultTypeQualifiedName = query.resultType()
      val resultType = schema.type(resultTypeQualifiedName.toVyneQualifiedName())
      val parametrisedType =  resultType.collectionType ?: resultType
      val dataSource = VyneCalciteDataSource(
         schema,
         resultTypeQualifiedName.toVyneQualifiedName(),
         streamProvider.stream(parametrisedType, schema, azureStoreConnection, fetchContainer(operation), null)
      )
      val stopwatch = Stopwatch.createStarted()
      val result = NamedParameterJdbcTemplate(dataSource).queryForList(sql, paramMap)
      val elapsed = stopwatch.elapsed()
      val datasource = buildDataSource(
         service,
         operation,
         constructedQueryDataSource.inputs,
         sql,
         azureStoreConnection.connectionName,
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

   private fun fetchConnection(service: Service): AzureStorageConnectorConfiguration {
      val connectionName = service.metadata(AzureStoreConnectionTaxi.Annotations.AzureStoreService.NAME).params["connectionName"] as String
      val azureStoreConnectionConfig = connectionRegistry.getConnection(connectionName)
      logger.info { "Azure Store connection ${azureStoreConnectionConfig.connectionName} found in configurations" }
      return azureStoreConnectionConfig
   }

   private fun fetchContainer(operation: RemoteOperation): String {
      return operation.metadata(AzureStoreConnectionTaxi.Annotations.StoreOperation.NAME).params[AzureStoreConnectionTaxi.Annotations.StoreOperation.containerMetadataName] as String
   }
}
