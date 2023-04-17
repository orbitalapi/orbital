package io.vyne.connectors.jdbc

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import io.vyne.connectors.collectionTypeOrType
import io.vyne.connectors.config.jdbc.JdbcConnectionConfiguration
import io.vyne.connectors.jdbc.sql.dml.SelectStatementGenerator
import io.vyne.connectors.resultType
import io.vyne.models.DataSource
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.models.json.Jackson
import io.vyne.query.*
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import lang.taxi.query.TaxiQlQuery
import org.postgresql.jdbc.PgArray
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.util.LinkedCaseInsensitiveMap
import java.time.Duration
import java.time.Instant

/**
 * An invoker capable of invoking VyneQL queries in a graph search
 * It's expected that the input to this (i.e. the invoker), is the result of a QueryBuildingEvaluator,
 * where we're receiving a TypedInstance containing the VyneQL query, along with a datasource of ConstructedQuery.
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
      queryId: String
   ): Flow<TypedInstance> {
      val (connectionConfig, jdbcTemplate) = getConnectionConfigAndTemplate(service)
      val schema = schemaProvider.schema
      val taxiSchema = schema.taxi
      val (taxiQuery, constructedQueryDataSource) = parameters[0].second.let { it.value as String to it.source as ConstructedQueryDataSource }
      val (query, _) = schema.parseQuery(taxiQuery)
//      val query = Compiler(taxiQuery, importSources = listOf(taxiSchema)).queries().first()
      val (sql, paramList) = SelectStatementGenerator(taxiSchema).toSql(query, connectionConfig.sqlBuilder())
      val paramMap = paramList.associate { param -> param.nameUsedInTemplate to param.value }

      val stopwatch = Stopwatch.createStarted()
      val resultList = jdbcTemplate.queryForList(sql, paramMap)
      val elapsed = stopwatch.elapsed()
      val operationResult = buildOperationResult(
         service,
         operation,
         constructedQueryDataSource.inputs,
         sql,
         connectionConfig.address,
         elapsed,
         recordCount = resultList.size
      )
      eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
      return convertToTypedInstances(resultList, query, schema, operationResult.asOperationReferenceDataSource())
   }

   private fun buildOperationResult(
      service: Service,
      operation: RemoteOperation,
      parameters: List<TypedInstance>,
      sql: String,
      jdbcUrl: String,
      elapsed: Duration,
      recordCount: Int
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
         exchange = SqlExchange(
            sql = sql,
            recordCount = recordCount
         )
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
   ): Flow<TypedInstance> {
      val resultTypeName = query.resultType()

      val resultTaxiType = collectionTypeOrType(schema.taxi.type(resultTypeName))

      val typedInstances = resultList
         .map { columnMap ->
            TypedInstance.from(
               schema.type(resultTaxiType),
               convertColumnMapToGeneralPurposeTypes(columnMap),
               schema,
               source = datasource,
               evaluateAccessors = false
            )
         }
      return typedInstances.asFlow()
   }

   /**
    * TODO This shouldn't be needed..
    * The TypedInstance generation relies on the map being an insensitive one, so we need to utilize LinkedCaseInsensitiveMap.
    */
   private fun convertColumnMapToGeneralPurposeTypes(columnMap: Map<String, Any>): LinkedCaseInsensitiveMap<Any> {
      val result = LinkedCaseInsensitiveMap<Any>()
      columnMap.forEach {
         val value = when (it.value) {
            is PGobject -> it.value.toString()
            is PgArray -> ((it.value as PgArray).array as Array<String>).toList()
            else -> it.value
         }
         result[it.key] = value
      }
      return result
   }

   private fun getConnectionNameAndTemplate(service: Service): Pair<String, NamedParameterJdbcTemplate> {
      val connectionName =
         service.metadata(JdbcConnectorTaxi.Annotations.DatabaseOperation.NAME).params["connection"] as String
      return connectionName to connectionFactory.jdbcTemplate(connectionName)
   }

   private fun getConnectionConfigAndTemplate(service: Service): Pair<JdbcConnectionConfiguration, NamedParameterJdbcTemplate> {
      val connectionName =
         service.metadata(JdbcConnectorTaxi.Annotations.DatabaseOperation.NAME).params["connection"] as String
      return connectionFactory.config(connectionName) to connectionFactory.jdbcTemplate(connectionName)
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
