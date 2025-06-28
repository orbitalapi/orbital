package com.orbitalhq.connectors.azure.blob

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import com.orbitalhq.connectors.azure.blob.registry.AzureStorageConnectorConfiguration
import com.orbitalhq.connectors.azure.blob.registry.AzureStoreConnectionRegistry
import com.orbitalhq.connectors.calcite.VyneCalciteDataSource
import com.orbitalhq.connectors.convertToTypedInstances
import com.orbitalhq.connectors.jdbc.sql.dml.SelectStatementGenerator
import com.orbitalhq.connectors.resultType
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.EmptyExchangeData
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.ObjectStoreRequest
import com.orbitalhq.query.tracing.ObjectStoreResponse
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.toVyneQualifiedName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import lang.taxi.Compiler
import mu.KotlinLogging
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

class StoreInvoker(
   private val streamProvider: StreamProvider,
   private val connectionRegistry: AzureStoreConnectionRegistry,
   private val schemaProvider: SchemaProvider,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : OperationInvoker {
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(AzureStoreConnectionTaxi.Annotations.AzureStoreService.NAME) &&
         operation.hasMetadata(AzureStoreConnectionTaxi.Annotations.StoreOperation.NAME)
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val schema = schemaProvider.schema
      val taxiSchema = schema.taxi
      val (taxiQuery, constructedQueryDataSource) = parameters[0].second.let { it.value as String to it.source as ConstructedQueryDataSource }
      val query = Compiler(taxiQuery, importSources = listOf(taxiSchema)).queries().first()
      val (sql, paramList) = SelectStatementGenerator(taxiSchema).toSql(query) { type -> type.toQualifiedName().typeName.toUpperCase() }
      val paramMap = paramList.associate { param -> param.nameUsedInTemplate to param.value }
      val azureStoreConnection = fetchConnection(service)
      val containerName = fetchContainer(operation)
      val traceContext = eventDispatcher.createOperationTraceSpan(service, operation, containerName)

      traceContext.emitEvent(
         kind = TracingEventKind.OK,
         spanState = SpanState.ACTIVE,
         payloadType = null,
         exchangeMetadata = ObjectStoreRequest(azureStoreConnection.connectionName, containerName) { sql },
         verb = "Query",
         direction = TraceEventDirection.OUTBOUND
      )

      val resultTypeQualifiedName = query.resultType()
      val resultType = schema.type(resultTypeQualifiedName.toVyneQualifiedName())
      val parametrisedType = resultType.collectionType ?: resultType
      val dataSource = VyneCalciteDataSource(
         schema,
         resultTypeQualifiedName.toVyneQualifiedName(),
         streamProvider.stream(parametrisedType, schema, azureStoreConnection, containerName, null)
      )
      val stopwatch = Stopwatch.createStarted()
      val result = NamedParameterJdbcTemplate(dataSource).queryForList(sql, paramMap)
      val elapsed = stopwatch.elapsed()

      val resultEvent = traceContext.emitEvent(
         TracingEventKind.OK,
         SpanState.COMPLETE,
         operation.returnType,
         ObjectStoreResponse(errorMessage = null, result.size.toLong(), { "Retrieved ${result.size} records from Azure Store" }),
         "Query response",
         direction = TraceEventDirection.INBOUND
      )

      val operationResult = buildDataSource(
         service,
         operation,
         constructedQueryDataSource.inputs,
         sql,
         azureStoreConnection.connectionName,
         elapsed
      )
      eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
      return result.convertToTypedInstances(
         schema,
         operationResult.asOperationReferenceDataSource(resultEvent.idSet),
         resultTypeQualifiedName,
         dispatcher
      )
   }

   private fun buildDataSource(
      service: Service,
      operation: RemoteOperation,
      parameters: List<TypedInstance>,
      sql: String,
      jdbcUrl: String,
      elapsed: Duration,
   ): OperationResult {

      val remoteCall = RemoteCall(
         service = service.name,
         address = jdbcUrl,
         operation = operation.name,
         responseTypeName = operation.returnType.name,
         requestBody = sql,
         durationMs = elapsed.toMillis(),
         timestamp = Instant.now(),
         // If we implement streaming database queries, this will change
         responseMessageType = ResponseMessageType.FULL,
         // Feels like capturing the results are a bad idea.  Can revisit if there's a use-case
         response = null,
         exchange = EmptyExchangeData
      )
      return OperationResult.fromTypedInstances(
         parameters,
         remoteCall
      )
   }

   private fun fetchConnection(service: Service): AzureStorageConnectorConfiguration {
      val connectionName =
         service.firstMetadata(AzureStoreConnectionTaxi.Annotations.AzureStoreService.NAME).params["connectionName"] as String
      val azureStoreConnectionConfig = connectionRegistry.getConnection(connectionName)
      logger.info { "Azure Store connection ${azureStoreConnectionConfig.connectionName} found in configurations" }
      return azureStoreConnectionConfig
   }

   private fun fetchContainer(operation: RemoteOperation): String {
      return operation.firstMetadata(AzureStoreConnectionTaxi.Annotations.StoreOperation.NAME).params[AzureStoreConnectionTaxi.Annotations.StoreOperation.containerMetadataName] as String
   }
}
