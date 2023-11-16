package com.orbitalhq.connectors.jdbc

import com.orbitalhq.connectors.collectionTypeOrType
import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.resultType
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.SqlExchange
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import lang.taxi.query.TaxiQlQuery
import org.jooq.DSLContext
import org.postgresql.jdbc.PgArray
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.util.LinkedCaseInsensitiveMap
import java.time.Duration
import java.time.Instant

abstract class BaseJdbcOperationInvoker(
   protected val connectionFactory: JdbcConnectionFactory,
) {

   protected fun jdbcTemplate(connectionName: String): NamedParameterJdbcTemplate {
      return connectionFactory.jdbcTemplate(connectionName)
   }

   protected fun sqlDsl(connectionName: String): DSLContext {
      val config = connectionFactory.config(connectionName)
      return connectionFactory.dsl(config)
   }

   abstract suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String
   ): Flow<TypedInstance>

   protected fun convertToTypedInstances(
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


   fun buildOperationResult(
      service: Service,
      operation: RemoteOperation,
      parameters: List<TypedInstance>,
      sql: String,
      jdbcUrl: String,
      elapsed: Duration,
      recordCount: Int,
      verb: String = "SELECT"
   ): OperationResult {

      val remoteCall = buildRemoteCall(service, jdbcUrl, operation, sql, elapsed, recordCount, verb)
      return OperationResult.fromTypedInstances(
         parameters,
         remoteCall
      )
   }

   private fun buildRemoteCall(
      service: Service,
      jdbcUrl: String,
      operation: RemoteOperation,
      sql: String,
      elapsed: Duration,
      recordCount: Int,
      verb: String
   ) = RemoteCall(
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
         recordCount = recordCount,
         verb = verb
      ),

   )

   protected fun getConnectionConfigAndTemplate(service: Service): Pair<JdbcConnectionConfiguration, NamedParameterJdbcTemplate> {
      val connectionName =
         service.metadata(JdbcConnectorTaxi.Annotations.DatabaseOperation.NAME).params["connection"] as String
      return connectionFactory.config(connectionName) to connectionFactory.jdbcTemplate(connectionName)
   }

}
