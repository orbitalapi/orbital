package io.vyne.connectors.jdbc

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import io.vyne.connectors.TaxiQlToSqlConverter
import io.vyne.connectors.TaxiQlToSqlConverter.Companion.QUERY_ID_COLUMN
import io.vyne.connectors.collectionTypeOrType
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
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.utils.Ids
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import lang.taxi.Compiler
import lang.taxi.query.TaxiQlQuery
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.Duration
import java.time.Instant

/**
 * An invoker capable of invoking VyneQL queries in a graph search
 * It's expected that the input to this (ie., the invoker), is the result of a QueryBuildingEvaluator,
 * where we're recieving a TypedInstance containing the VyneQL query, along with a datasource of ConstructedQuery.
 */
class JdbcInvoker(
   private val connectionFactory: JdbcConnectionFactory,
   private val schemaProvider: SchemaProvider,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper
) :
   OperationInvoker {
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(JdbcConnectorTaxi.Annotations.DatabaseOperation.NAME)
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String?
   ): Flow<TypedInstance> {
      val (connectionName, jdbcTemplate) = getConnectionNameAndTemplate(service)
      val schema = schemaProvider.schema
      val taxiSchema = schema.taxi
      val (taxiQuery, constructedQueryDataSource) = parameters[0].second.let { it.value as String to it.source as ConstructedQueryDataSource }

      val query = Compiler(taxiQuery, importSources = listOf(taxiSchema)).queries().first()
      val (sql, paramList) = TaxiQlToSqlConverter(taxiSchema)
         .toSql(query) { type -> SqlUtils.getTableName(type) }
      val paramMap = paramList.associate { param -> param.nameUsedInTemplate to param.value }

      val stopwatch = Stopwatch.createStarted()
      val resultList = jdbcTemplate.queryForList(sql, paramMap)
      val elapsed = stopwatch.elapsed()
      val datasource = buildDataSource(
         service,
         operation,
         constructedQueryDataSource.inputs,
         sql,
         connectionFactory.config(connectionName).address,
         elapsed
      )
      return convertToTypedInstances(resultList, query, schema, datasource).asFlow()
   }

   fun batchInvoke(operations: List<JdbcOperationBatchingStrategy.BatchedOperation>, batchTraceCollector: BatchTraceCollector) {
      if (operations.isNotEmpty()) {
         val service = operations.first().service
         val (connectionName, jdbcTemplate) = getConnectionNameAndTemplate(service)
         val schema = schemaProvider.schema
         val taxiSchema = schema.taxi
         val queryMap = mutableMapOf<String, SingleQueryExecutionData>()
         val baseQueryId = Ids.id("q-")
         operations.forEachIndexed { index, batchedOperation ->
            val parameters = batchedOperation.parameters
            val operation = batchedOperation.operation
            val (taxiQuery, constructedQueryDataSource) = parameters[0].second.let { it.value as String to it.source as ConstructedQueryDataSource }
            val queryId = "$baseQueryId$index"
            val query = Compiler(taxiQuery, importSources = listOf(taxiSchema)).queries().first()
            val (sql, paramList) = TaxiQlToSqlConverter(taxiSchema) { "$it$index" }
               .toSql(query, queryId) { type -> SqlUtils.getTableName(type) }
            val paramMap = paramList.associate { param -> param.nameUsedInTemplate to param.value }
            queryMap[queryId] = SingleQueryExecutionData(sql, query, paramMap, batchedOperation.consumer) {
               buildDataSource(
                  service,
                  operation,
                  constructedQueryDataSource.inputs,
                  sql,
                  connectionFactory.config(connectionName).address,
                  it
               )
            }
         }

         val batchSql = queryMap.values.joinToString(" UNION ALL ") { it.sqlQuery }
         val batchParamMap = mutableMapOf<String, Any>()
         queryMap.values.forEach { batchParamMap.putAll(it.queryParamNameValueMap) }
         val stopwatch = Stopwatch.createStarted()
         val batchResults = jdbcTemplate.queryForList(batchSql, batchParamMap)
         val elapsed = stopwatch.elapsed()
         batchTraceCollector.reportSqlBatchQuery(batchSql, batchParamMap)
         if (batchResults.isEmpty()) {
            operations.forEach { op -> op.consumer.onError(IllegalStateException("bulk select returned empty result set.")) }
         }

         batchResults.forEach { result ->
            val resultQueryId = result[QUERY_ID_COLUMN]?.toString() ?: ""
            val singleQueryData = queryMap.remove(resultQueryId)!!
            convertToTypedInstances(listOf(result), singleQueryData.taxiQlQuery, schema, singleQueryData.dataSourceSupplier(elapsed)).map {
               singleQueryData.supplier.onNextValue(it)
            }
            singleQueryData.supplier.onCompleted()
         }

         if (batchResults.isNotEmpty()) {
            queryMap.values.forEach { op -> op.supplier.onError(IllegalStateException("bulk select return empty result")) }
         }

      }

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

   private fun convertToTypedInstances(
      resultList: List<MutableMap<String, Any>>,
      query: TaxiQlQuery,
      schema: Schema,
      datasource: DataSource
   ): List<TypedInstance> {
      val resultTypeName = query.resultType()

      val resultTaxiType = collectionTypeOrType(schema.taxi.type(resultTypeName))

      val typedInstances = resultList
         .map { columnMap ->
            TypedInstance.from(
               schema.type(resultTaxiType),
               columnMap,
               schema,
               source = datasource,
               evaluateAccessors = false
            )
         }
      return typedInstances
   }

   private fun getConnectionNameAndTemplate(service: Service): Pair<String, NamedParameterJdbcTemplate> {
      val connectionName =
         service.metadata(JdbcConnectorTaxi.Annotations.DatabaseOperation.NAME).params["connection"] as String
      return connectionName to connectionFactory.jdbcTemplate(connectionName)
   }
}

class DatabaseQuerySource(
   val connectionName: String,
   val query: String,
   override val failedAttempts: List<DataSource> = emptyList()
) : DataSource {
   override val name: String = "DatabaseQuery"
   override val id: String = this.hashCode().toString()
}

data class SingleQueryExecutionData(
   val sqlQuery: String,
   val taxiQlQuery: TaxiQlQuery,
   val queryParamNameValueMap: Map<String, Any>,
   val supplier: JdbcOperationBatchingStrategy.TypedInstanceSupplier,
   val dataSourceSupplier: (Duration) -> DataSource)
